package com.t2.compassionMeditation;

import org.t2health.lib1.DataOutHandler;
import org.t2health.lib1.DataOutHandler.DataOutPacket;
import org.t2health.lib1.dsp.T2FPeriodAverageFilter;
import org.t2health.lib1.dsp.T2MovingAverageFilter;
import org.t2health.lib1.dsp.T2PeriodAverageFilter;

import android.content.Context;

import com.oregondsp.signalProcessing.filter.iir.ChebyshevI;
import com.oregondsp.signalProcessing.filter.iir.PassbandType;
import com.t2.Constants;
import com.t2.compassionUtils.Util;

import spine.datamodel.Data;
import spine.datamodel.Feature;
import spine.datamodel.FeatureData;
import spine.datamodel.MindsetData;
import spine.datamodel.Node;
import spine.datamodel.ShimmerData;

/**
 * Does all necessary processing for raw rdata received from various biosensors
 * Also handles logging of data to using DataOutHandler
 * 
 * By having the individual activities use this class to process bio data the seperation
 * is maintained between the model (biodata), and the view (activity).
 * 
 * @author scott.coleman
 *
 */
public class BioDataProcessor {
	private final int GSR_PERIOD_SAMPLE_SECONDS = 1; 
	private final int HEARTRATE_PERIOD_SAMPLE_SECONDS = 3; 
	private final int RESPRATE_PERIOD_SAMPLE_SECONDS = 10; 

	Context mContext;

	private T2FPeriodAverageFilter mGsrPeriodAverage;	
	private T2PeriodAverageFilter mHeartRatePeriodAverage;
	private T2HeartRateDetector mHeartRateDetector = new T2HeartRateDetector();
	private T2MovingAverageFilter mGroundLeadFilter = new T2MovingAverageFilter(64);	
	private ChebyshevI mEcgBaselineFilter;	
	private ChebyshevI mEcgNoiseFilter;	
	private DataOutHandler mDataOutHandler;

	public int mGsrResistance;
	public double mGsrConductance;
	public double mGgsrAvg;
	public int mRawEcg;
	public double mBaselineFiltered;
	public int mShimmerHeartRate = 0;
	public int mZephyrHeartRate;
	public double mRespRate;
	public double mSkinTempF;		
	
	/**
	 * @param context - Context of calling activity
	 */
	public BioDataProcessor(Context context) {
		mContext = context;
	}

	/**
	 * Initializes all filters and processors necessary to process the biometeric data
	 * 
	 * @param dataOutHandler - data output handler used to send data to output sinks
	 */
	public void initialize(DataOutHandler dataOutHandler) {
		mDataOutHandler = dataOutHandler;
        int shimmerSensorSampleRate  = Integer.parseInt(SharedPref.getString(mContext, "sensor_sample_rate" ,"4"));
        mGsrPeriodAverage = new T2FPeriodAverageFilter(GSR_PERIOD_SAMPLE_SECONDS * shimmerSensorSampleRate);	
        mHeartRatePeriodAverage = new T2PeriodAverageFilter(HEARTRATE_PERIOD_SAMPLE_SECONDS * shimmerSensorSampleRate);
    	mEcgBaselineFilter = new ChebyshevI( 4, 0.50885, PassbandType.HIGHPASS, 0.5, 0.5, 0.015625 );	// Fs = 64Hz, corner = .5Hz)
    	mEcgNoiseFilter = new ChebyshevI( 4, 0.50885, PassbandType.LOWPASS, 20, 20, 0.015625 );			// // Fs = 64Hz, corner = 20Hz)
        
	}
	
	/**
	 * Processes GSR biometeric data received from the Shimmer sensor
	 *  
	 * @param shimmerData - Shimmer sensor data
	 * @param configuredGSRRange - GSR range configured into the sensor
	 */
	public void processShimmerGSRData(ShimmerData shimmerData, int configuredGSRRange) {
		mGsrResistance = Util.GsrResistance(shimmerData.gsr, shimmerData.gsrRange, configuredGSRRange); 
		mGsrConductance = (1F / (float) mGsrResistance) * 1000000;
		
		// Send data to output
		DataOutPacket packet = mDataOutHandler.new DataOutPacket();
		packet.add(DataOutHandler.SENSOR_TIME_STAMP, shimmerData.timestamp);
		packet.add(DataOutHandler.RAW_GSR, mGsrConductance, "%2.2f");
		mDataOutHandler.handleDataOut(packet);
		
		// Handle logging of average GSR every 1 second
		double mGgsrAvg = mGsrPeriodAverage.filter(mGsrConductance);
		if (mGgsrAvg != T2FPeriodAverageFilter.AVERAGING) {
			// We get here once every second
	        // Send data to output
			packet = mDataOutHandler.new DataOutPacket();
			packet.add(DataOutHandler.AVERAGE_GSR, mGgsrAvg, "%2.2f");
			mDataOutHandler.handleDataOut(packet);
		}
	} // End processShimmerGSR(ShimmerData shimmerData, int configuredGSRRange)
	
	/**
	 * Processes EMG biometeric data received from the Shimmer sensor
	 *  
	 * @param shimmerData - Shimmer sensor data
	 * 
	 */	
	public void processShimmerEMGData(ShimmerData shimmerData) {
        // Send data to output
		DataOutPacket packet = mDataOutHandler.new DataOutPacket();
		packet.add(DataOutHandler.SENSOR_TIME_STAMP, shimmerData.timestamp);
		packet.add(DataOutHandler.RAW_EMG, shimmerData.emg);
		mDataOutHandler.handleDataOut(packet);						
	}
	
	/**
	 * Processes ECG biometeric data received from the Shimmer sensor
	 *   Note that this class filters the data detects heart rate using a   
	 *   Pan-Tompkins QRS detector
	 *   
	 * @param shimmerData - Shimmer sensor data
	 * 
	 */	
	public void processShimmerECGData(ShimmerData shimmerData) {
		// Account for the fact that the leads are switched in the service
		int tmp = shimmerData.ecgLaLL ;
		shimmerData.ecgLaLL = shimmerData.ecgRaLL;
		shimmerData.ecgRaLL = tmp;

		// First limit the ecg voltages to the max
		if (shimmerData.ecgLaLL > 4095) shimmerData.ecgLaLL = 4096; 
		if (shimmerData.ecgRaLL > 4095) shimmerData.ecgRaLL = 4096; 		
	
		int lead3 = mGroundLeadFilter.filter(shimmerData.ecgLaLL); 
		int mRawEcg = shimmerData.ecgRaLL - lead3; 
		double mBaselineFiltered = mEcgBaselineFilter.filter(mRawEcg);	
		
		int heartRate = mHeartRateDetector.filter((int) mBaselineFiltered, shimmerData.timestamp);

		if (heartRate == -1) {
			// just leave the heartrate at the last good calculated value
		}
		else {
			// Limit the rate at which heartrate can change
			int deltaHeartRate = heartRate - mShimmerHeartRate; 
			if (Math.abs(deltaHeartRate) < mShimmerHeartRate / 10) {
				mShimmerHeartRate = heartRate;
			}
			else {
				if (deltaHeartRate >= 0) {
					mShimmerHeartRate += 1;
				}
				else {
					mShimmerHeartRate -= 1;
				}
			}
		}		
		
        // Send data to output
		DataOutPacket packet = mDataOutHandler.new DataOutPacket();
		packet.add(DataOutHandler.SENSOR_TIME_STAMP, shimmerData.timestamp);
		packet.add(DataOutHandler.RAW_ECG, mRawEcg);
		packet.add(DataOutHandler.FILTERED_ECG, (int) mBaselineFiltered);
		packet.add(DataOutHandler.RAW_HEARTRATE, mShimmerHeartRate);
		mDataOutHandler.handleDataOut(packet);						
		
		int hrAvg = mHeartRatePeriodAverage.filter(mShimmerHeartRate);
		if (hrAvg != T2PeriodAverageFilter.AVERAGING) {
			// We get here once every 3 seconds
	        // Send data to output
			packet = mDataOutHandler.new DataOutPacket();
			packet.add(DataOutHandler.AVERAGE_HEARTRATE, hrAvg);
			mDataOutHandler.handleDataOut(packet);
		}		
	
	} // End processShimmerECG(ShimmerData shimmerData)

	/**
	 * Processes HR, Skin Temp, and Resp Rate  biometeric data received from the Zephyr sensor
	 * 
	 * @param data - Spine data containing Zephyr data
	 */
	public void processZephyrData(Data data) {
		Node source = data.getNode();
		Feature[] feats = ((FeatureData)data).getFeatures();
		Feature firsFeat = feats[0];
		
		byte sensor = firsFeat.getSensorCode();
		byte featCode = firsFeat.getFeatureCode();
		int batLevel = firsFeat.getCh1Value();
		mZephyrHeartRate = firsFeat.getCh2Value();
		mRespRate = firsFeat.getCh3Value() / 10;
		int skinTemp = firsFeat.getCh4Value() / 10;
		mSkinTempF = (skinTemp * 9 / 5) + 32;			
		
		
        // Send data to output
		DataOutPacket packet = mDataOutHandler.new DataOutPacket();
		packet.add(DataOutHandler.RAW_HEARTRATE, mZephyrHeartRate);
		packet.add(DataOutHandler.RAW_RESP_RATE, (int) mRespRate);
		packet.add(DataOutHandler.RAW_SKINTEMP, (int) mSkinTempF);
		mDataOutHandler.handleDataOut(packet);				
		
	} // End processZephyr(ShimmerData shimmerData)
	
	
	/**
	 * Processes Mindset (EEG) biometeric data received from the Mindset sensor
	 * 
	 * @param data - Spine data containing Mindset data
	 * @param currentMindsetData - Structure to hold individual spectral bands
	 */
	public void processMindsetData(Data data, MindsetData currentMindsetData) {
		
		Node source = data.getNode();
		MindsetData mindsetData = (MindsetData) data;		
		
		if (mindsetData.exeCode == Constants.EXECODE_SPECTRAL || mindsetData.exeCode == Constants.EXECODE_RAW_ACCUM) {
			
			if (mindsetData.exeCode == Constants.EXECODE_RAW_ACCUM)
				currentMindsetData.updateRawWave(mindsetData);
			
			currentMindsetData.updateSpectral(mindsetData);	// Updates currentMindsetData with spectral data
			String logDataLine = currentMindsetData.getLogDataLine();
			
	        // Send data to output
			DataOutPacket packet = mDataOutHandler.new DataOutPacket();
			packet.add(DataOutHandler.EEG_SPECTRAL, logDataLine);
			mDataOutHandler.handleDataOut(packet);				
		}
		if (mindsetData.exeCode == Constants.EXECODE_POOR_SIG_QUALITY) {
			
			currentMindsetData.poorSignalStrength = mindsetData.poorSignalStrength;
			
	        // Send data to output
    		DataOutPacket packet = mDataOutHandler.new DataOutPacket();
			packet.add(DataOutHandler.EEG_SIG_STRENGTH, mindsetData.poorSignalStrength);
			mDataOutHandler.handleDataOut(packet);
		}
		if (mindsetData.exeCode == Constants.EXECODE_ATTENTION) {

			currentMindsetData.attention= mindsetData.attention;
			
	        // Send data to output
    		DataOutPacket packet = mDataOutHandler.new DataOutPacket();
			packet.add(DataOutHandler.EEG_ATTENTION, mindsetData.attention);
			mDataOutHandler.handleDataOut(packet);
		}
		if (mindsetData.exeCode == Constants.EXECODE_MEDITATION) {						
			currentMindsetData.meditation= mindsetData.meditation;
	        // Send data to output
    		DataOutPacket packet = mDataOutHandler.new DataOutPacket();
			packet.add(DataOutHandler.EEG_MEDITATION, mindsetData.meditation);
			mDataOutHandler.handleDataOut(packet);
		}						
	} // End processMindsetData(Data data, MindsetData currentMindsetData)
}
