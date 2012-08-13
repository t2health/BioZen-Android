package com.t2.compassionMeditation;

import java.util.Arrays;





import org.t2health.lib1.dsp.T2Filter;

import android.util.Log;

/**
 * Detects heart rate given an ECG waveform
 * 
 * @author scott.coleman
 * 
 * min bpm = 40    	1500 ms
 * Max bpm = 180	333.3 ms
 *
 */
public class T2HeartRateDetector extends T2Filter {
	private static final String TAG = "BFDemo";
	private static int DEFAULT_SIZE = 100;

	private int circularBuffer[];
	
    public int mean;
    private int total;
	private int circularIndex;
	private int totalSamples;
	private int size;
	private int min;
	/**
	 * maximum sample value in circular buffer
	 * Note that this will immediately follow all new values that are higher than the old max.
	 * and it will fall back to previous maximums after the buffer goes through all values in it's queue 
	 */
	private int max;
	
	private double hrPeriodCounterMs;
	private int hrBpm;	
	private int previousTimeStamp;
	private int previousSampleValue;
	private int numHRReports;
	private double sampleSlope = 0;
	
	public int getMin() {
		return min;
	}
	
	public int getMax() {
		return max;
	}
	
	public T2HeartRateDetector() {
		circularBuffer = new int[DEFAULT_SIZE];
	    reset();
	}
	
	/**
	 * @param dummy
	 */
	void dumpBuffer(int dummy) {
		String s = "circularIndex= " + circularIndex + "; ";
		for (int i = 0; i < circularBuffer.length; i++) {
			s += circularBuffer[i] + "; ";
		}
		Log.e(TAG, s);
	}
	
	/* (non-Javadoc)
	 * @see org.t2health.lib1.dsp.T2Filter#filter(int, int)
	 * @param sampleValue	    ADC sample value 
	 * @param sampleTimeStamp   Shimmer time stamp (note 640 = 20 ms)
	 * 
	 * @return HR in beats per minute (or -1 if unreasonable)
	 */
	@Override
	public int filter( int sampleValue, int sampleTimeStamp ) {
		int deltaTimeStamp;

		// Account for the fact that the timestamp rolls over at 65535
		if (sampleTimeStamp < previousTimeStamp) {
			deltaTimeStamp = 65536 - previousTimeStamp + sampleTimeStamp;
		}
		else {
			deltaTimeStamp = sampleTimeStamp - previousTimeStamp; 			
		}
	    previousTimeStamp = sampleTimeStamp;
		
	    // Convert timestamp to milliseconds
	    double deltaTimeStampMs = deltaTimeStamp / 32;
		
		if (totalSamples++ == 0) {
	        primeBuffer(sampleValue);
	    }
	    int lastBufferValue = circularBuffer[circularIndex];
	    
	    total -= lastBufferValue;
	    total += sampleValue;
	    mean = total / circularBuffer.length;
	    circularBuffer[circularIndex] = sampleValue;
	    circularIndex = nextIndex(circularIndex);
	    
	    // Set min and max
	    int[] tmp = circularBuffer.clone();
	    Arrays.sort(tmp);
	    min = tmp[0];
	    max = tmp[tmp.length - 1];
	    float threshold = (float) ((float) max * 0.6);
	    int ithreshold = (int) threshold;
	    
	    // Now do HR calculation
	    hrPeriodCounterMs += deltaTimeStampMs;
		sampleSlope = (sampleValue - previousSampleValue) / 20F;
    	
	    boolean rDetected = false;
	    // Per research from HR detection algorithms we set the threshold for the R-wave detection
	    // at 60% of the most recent maximum.
	    if (sampleValue > ithreshold) {
	    	// We're above the threshold so ostensibly we've detected an R-wave
	    	
	    	// Don't report the very first one since we might have started counting after the start of
	    	// the R wave.
	    	if (numHRReports++ == 0) {
	    	    // Print header
//	    	    Log.e(TAG, ",sampleValue, sampleSlope, max, ithreshold, mean, marker, hrBpm");
	    	    Log.e(TAG, ",sampleValue, hrPeriodCounterMs, marker, hrBpm");
	    		
		    	hrPeriodCounterMs = 0;		// Restart the period counter and do nothing else
	    	}
	    	else {
	    		
	    		// Do limit checking
	    		if (hrPeriodCounterMs > 200F) {
		    		
	    			if (hrPeriodCounterMs > 360F) {
		    			rDetected = true;
		    			
		    			double fBpm = (1F / (hrPeriodCounterMs - deltaTimeStampMs)) * 60000F; // Need to subtract the delta time stamp back out
			    		int bpm = (int) Math.round(fBpm);
		    			if (hrPeriodCounterMs <= 1500F) {
				    		hrBpm = bpm;				// Limit the count to 40 BPM
				    									// This is in case we miss a beat
				    									// So we won't contaminate a good heart rate with one with a missed beat
		    			}
				    	hrPeriodCounterMs = 0;		// Restart the period counter and do nothing else
	    			}
	    			else {
	    				// If the QRS was < 360 ms We need to make sure we have the R wave instead of the T wave
	    				if (sampleSlope > 2) {
			    			rDetected = true;
			    			double fBpm = (1F / (hrPeriodCounterMs - deltaTimeStampMs)) * 60000F; // Need to subtract the delta time stamp back out
				    		int bpm = (int) Math.round(fBpm);
				    		hrBpm = bpm;
					    	hrPeriodCounterMs = 0;		// Restart the period counter and do nothing else
	    					
	    				}
	    			}
	    		}
	    	}
	    }

	    // Do overall sanity checks
	    
    	// Don't report unless the mean is < 50 (mean is an indicator of artifact
    	if (mean > 50  || mean < -50) {
    		hrBpm = -1;
    	}
	    
//	    Log.e(TAG, String.format(",%d, %2.2f, %d, %d, %d, %d, %d", sampleValue, (float) sampleSlope, max, ithreshold, mean, rDetected ? 150:200, hrBpm));
//	    Log.e(TAG, String.format(",%d, %f, %d, %d", sampleValue, hrPeriodCounterMs, rDetected ? 150:200, hrBpm));
	    
	    previousSampleValue = sampleValue;
    	return hrBpm;
	}       
	   
	public int getValue() {
		return mean;
	}
	
	/**
	 * Returns the variance of all samples in the circular buffer
	 * @return	 variance
	 */
	public double getVariance() {
		
		int sdIndex = circularIndex;		
		long n = 0;
		double mean = 0;
		double s = 0.0;
		
	    for (int i = 0; i < circularBuffer.length; ++i) {
	    	double val = circularBuffer[sdIndex];
			++n;
			double delta = val - mean;
			mean += delta / n;
			s += delta * (val - mean);
			
			sdIndex = nextIndex(sdIndex);
						
	    }		
		return (s / n);		
	}
	
	/**
	 * Returns the standard deviation of all samples in the circular buffer
	 * @return	 standard deviation
	 */
	public double getStdDev() {
		return Math.sqrt(getVariance());
	}	
	
	/**
	 * Resets the detector
	 */
	public void reset() {
	    totalSamples = 0;
	    circularIndex = 0;
	    mean = 0;
	    total = 0;
	    hrPeriodCounterMs = 0;
	    previousTimeStamp = 0;
	    numHRReports = 0;
	    hrBpm = -1;
	    previousSampleValue = -1;
	    
	    
	}
	
	/**
	 * Returns the total number of samples that have been fed to the detector
	 * @return	total number of samples
	 */
	public long getTotalSamples() {
	    return totalSamples;
	}
	
	/**
	 * Primes the circular buffer with a value
	 * @param val
	 */
	private void primeBuffer(int val) {
	    for (int i = 0; i < circularBuffer.length; ++i) {
	        circularBuffer[i] = val;
	        total += val;
	    }
	    mean = val;
	}
	
	/**
	 * Encapsulaes nagivating the circular buffer
	 * @param curIndex	current index into the 
	 * @return			next index into the circular buffer
	 */
	private int nextIndex(int curIndex) {
	    if (curIndex + 1 >= circularBuffer.length) {
	        return 0;
	    }
	    return curIndex + 1;
	}	
	
}