/*****************************************************************
BioZen

Copyright (C) 2011 The National Center for Telehealth and 
Technology

Eclipse Public License 1.0 (EPL-1.0)

This library is free software; you can redistribute it and/or
modify it under the terms of the Eclipse Public License as
published by the Free Software Foundation, version 1.0 of the 
License.

The Eclipse Public License is a reciprocal license, under 
Section 3. REQUIREMENTS iv) states that source code for the 
Program is available from such Contributor, and informs licensees 
how to obtain it in a reasonable manner on or through a medium 
customarily used for software exchange.

Post your updates and modifications to our GitHub or email to 
t2@tee2.org.

This library is distributed WITHOUT ANY WARRANTY; without 
the implied warranty of MERCHANTABILITY or FITNESS FOR A 
PARTICULAR PURPOSE.  See the Eclipse Public License 1.0 (EPL-1.0)
for more details.
 
You should have received a copy of the Eclipse Public License
along with this library; if not, 
visit http://www.opensource.org/licenses/EPL-1.0

*****************************************************************/
package com.t2.compassionMeditation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.t2health.lib1.BioParameter;
import org.t2health.lib1.BioSensor;
import org.t2health.lib1.DataOutHandler;
import org.t2health.lib1.DataOutHandler.DataOutPacket;
import org.t2health.lib1.DataOutHandlerException;
import org.t2health.lib1.DataOutHandlerTags;

import spine.SPINEFactory;
import spine.SPINEFunctionConstants;
import spine.SPINEListener;
import spine.SPINEManager;
import spine.SPINESensorConstants;
import spine.datamodel.Address;
import spine.datamodel.Data;
import spine.datamodel.Feature;
import spine.datamodel.FeatureData;
import spine.datamodel.HeartBeatData;
import spine.datamodel.MindsetData;
import spine.datamodel.Node;
import spine.datamodel.ServiceMessage;
import spine.datamodel.ShimmerData;
import spine.datamodel.functions.ShimmerNonSpineSetupSensor;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import bz.org.t2health.lib.activity.BaseActivity;

import com.t2.Constants;
import com.t2.R;
import com.t2.SpineReceiver;
import com.t2.SpineReceiver.BioFeedbackStatus;
import com.t2.SpineReceiver.OnBioFeedbackMessageRecievedListener;
import com.t2.antlib.ANTPlusService;
import com.t2.antlib.AntPlusManager;
import com.t2.biofeedback.device.shimmer.ShimmerDevice;
import com.t2.compassionUtils.MathExtra;
import com.t2.compassionUtils.Util;
import com.t2.t2sensorlib.BigBrotherService;


public class Graphs1Activity extends BaseActivity implements OnBioFeedbackMessageRecievedListener, 
	SPINEListener, AntPlusManager.Callbacks {
	private static final String TAG = "BFDemo";
	
	private static final String KEY_NAME = "results_visible_ids_16";	
	private static final int BLUETOOTH_SETTINGS_ID = 987;	

	private static final int HEARTRATE_SHIMMER = 1;	
	private static final int HEARTRATE_ZEPHYR = 2;	
	private static final int HEARTRATE_ANT = 3;	

	private String mAppId = "bioZenGraphs";
	
	private boolean mLogCatEnabled = true;
	private boolean mLoggingEnabled = true;
	private int mPrevSigQuality = 0;
	private boolean mInternalSensorMonitoring = false;
	
	
    /**
     * Intent to start Big Brother service
     */
    private PendingIntent mBigBrotherService;	
    private int mPollingPeriod = 30;					// seconds
	private int mSecondsWithoutActivityThreshold = 5;	// seconds
	private double mAccelerationThreshold = 12.0;		// m/s^2	
    	
	
	/**
	 * Application version info determined by the package manager
	 */
	private String mApplicationVersion = "";

    /**
     * The Spine manager contains the bulk of the Spine server. 
     */
    private static SPINEManager mManager;

    /**
	 * This is a broadcast receiver. Note that this is used ONLY for command/status messages from the AndroidBTService
	 * All data from the service goes through the mail SPINE mechanism (received(Data data)).
	 */
	private SpineReceiver mCommandReceiver;
	
	/**
	 * Static instance of this activity
	 */
	private static Graphs1Activity mInstance;
	
	/**
	 * Timer for updating the UI
	 */
	private static Timer mDataUpdateTimer;	
	
	/**
	 * Timer for Resp Rate Average
	 */
	private static Timer mRespRateAverageTimer;	
	
	protected SharedPreferences sharedPref;

	private boolean mPaused = false;
	
	
    private Boolean mBluetoothEnabled = false;
	
	// UI Elements	
    private Button mAddMeasureButton;
    private Button mPauseButton;
    private TextView mTextInfoView;
    private TextView mMeasuresDisplayText;    
	private MindsetData currentMindsetData;
	private GraphicalView mDeviceChartView;
	
	private int mConfiguredGSRRange = ShimmerDevice.GSR_RANGE_HW_RES_3M3;
	/**
	 * List of all BioParameters used in this activity
	 */
	private ArrayList<GraphBioParameter> mBioParameters = new ArrayList<GraphBioParameter>();

	/**
	 * List of all currently PAIRED BioSensors
	 */
	private ArrayList<BioSensor> mBioSensors = new ArrayList<BioSensor>();	
	
	/**
	 * Class to help in saving received data to file
	 */
	private DataOutHandler mDataOutHandler;	

	/**
	 * Class to help in processing biometeric data
	 */
	private BioDataProcessor mBioDataProcessor = new BioDataProcessor(this);
	
	// Charting stuff
	private final static int SPINE_CHART_SIZE = 20;
	private int mSpineChartX = 0;
	
	private Node mShimmerNode = null;
	
	/**
	 * Node object for shimmer device as returned by spine
	 */
	public Node mSpineNode = null;	
	
	private int numTicsWithoutData = 0;
	private static Object mKeysLock = new Object();
	private static Object mRespRateAverageLock = new Object();

	
	// We'll use these to get easy access to parameters in the mBioParameters array
	private int eegPos;
	private int gsrPos;
	private int emgPos;
	private int ecgPos;
	private int heartRatePos;
	private int respRatePos;
	private int skinTempPos;

	private int eHealthAirFlowPos;
	private int eHealthTempPos;
	private int eHealthSpO2Pos;
	private int eHealthHeartRatePos;
	private int eHealthGSRPos;
	
	boolean mIsActive = false;
	
	int mDisplaySampleRate;
	
//	int mHeartRateSource = HEARTRATE_SHIMMER;
	int mHeartRateSource = HEARTRATE_ZEPHYR;
	
	long mLastRespRateTime;
	int mRespRateTotal;
	int mRespRateIndex;

	private boolean mDatabaseEnabled;
	private boolean mAntHrmEnabled;	

	/**
	 * Static names dealing with the external database
	 */
	public static final String dDatabaseName = "";
	public static final String dDesignDocName = "bigbrother-local";
	public static final String dDesignDocId = "_design/" + dDesignDocName;
	public static final String byDateViewName = "byDate";

	/** Class to manage all the ANT messaging and setup */
	private AntPlusManager mAntManager;
	   
	private boolean mAntServiceBound;
	   
	/** Shared preferences data filename. */
	public static final String PREFS_NAME = "ANTDemo1Prefs";	   
	   
	/** Pair to any device. */
	static final short ANT_WILDCARD = 0;
	   
	/** The default proximity search bin. */
	private static final byte ANT_DEFAULT_BIN = 7;
	   
	/** The default event buffering buffer threshold. */
	private static final short ANT_DEFAULT_BUFFER_THRESHOLD = 0;	
	
	
	
	/**
	 *  Right now we're using only one shimmer node for all shimmer devices
	 *  (since we address they by BT address)
	 * @return singleton for the shimmer node
	 */
	private Node getShimmerNode() {
		if (mShimmerNode == null) {
			mShimmerNode = new Node(new Address("" + Constants.RESERVED_ADDRESS_SHIMMER)); 
			mManager.getActiveNodes().add(mShimmerNode);			
		}
		return mShimmerNode; 
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(TAG, this.getClass().getSimpleName() + ".onCreate()"); 
		
        // We don't want the screen to timeout in this activity
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
        this.requestWindowFeature(Window.FEATURE_NO_TITLE); // This needs to happen BEFORE setContentView
        setContentView(R.layout.graphs_activity_layout);
        mInstance = this;		
        
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());   
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);        
                
		mLoggingEnabled = SharedPref.getBoolean(this, "enable_logging", 	true);
		mDatabaseEnabled = SharedPref.getBoolean(this, "database_enabled", false);        
		mAntHrmEnabled = SharedPref.getBoolean(this, "enable_ant_hrm", false);        
		
		mInternalSensorMonitoring = SharedPref.getBoolean(this, "inernal_sensor_monitoring_enabled", 	false);
		
		if (mAntHrmEnabled) {
			mHeartRateSource = HEARTRATE_ANT;
		}
		else {
			mHeartRateSource = HEARTRATE_ZEPHYR;
		}
		
		

		// The session start time will be used as session id
		// Note this also sets session start time
		// **** This session ID will be prepended to all JSON data stored
		//      in the external database until it's changed (by the start
		//		of a new session.
		Calendar cal = Calendar.getInstance();						
		SharedPref.setBioSessionId(sharedPref, cal.getTimeInMillis());		

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
		String sessionDate = sdf.format(new Date());
		String userId = SharedPref.getString(this, "SelectedUser", 	"");
		long sessionId = SharedPref.getLong(this, "bio_session_start_time", 0);

		
		
		mDataOutHandler = new DataOutHandler(this, userId,sessionDate, mAppId, DataOutHandler.DATA_TYPE_EXTERNAL_SENSOR, sessionId );

		if (mDatabaseEnabled) {
			TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
	   		String myNumber = telephonyManager.getLine1Number();
	   		
	   		String remoteDatabaseUri = SharedPref.getString(this, "database_sync_name", getString(R.string.database_uri));
//	   		remoteDatabaseUri += myNumber; 
	   		
			Log.d(TAG, "Initializing database at " + remoteDatabaseUri); // TODO: remove
			try {
				mDataOutHandler.initializeDatabase(dDatabaseName, dDesignDocName, dDesignDocId, byDateViewName, remoteDatabaseUri);
			} catch (DataOutHandlerException e) {
				Log.e(TAG, e.toString());
				e.printStackTrace();
			}
			mDataOutHandler.setRequiresAuthentication(false);
			
		}			
		
		
		mBioDataProcessor.initialize(mDataOutHandler);
		
		
		if (mLoggingEnabled) {
			mDataOutHandler.enableLogging(this);
		}   
		
		if (mLogCatEnabled) {
			mDataOutHandler.enableLogCat();
		}   
		
		// Log the version
		try {
			PackageManager packageManager = getPackageManager();
			PackageInfo info = packageManager.getPackageInfo(getPackageName(), 0);			
			mApplicationVersion = info.versionName;
			String versionString = mAppId + " application version: " + mApplicationVersion;

			DataOutPacket packet = mDataOutHandler.new DataOutPacket();
			packet.add(DataOutHandlerTags.version, versionString);
			try {
				mDataOutHandler.handleDataOut(packet);
			} catch (DataOutHandlerException e) {
				Log.e(TAG, e.toString());
				e.printStackTrace();
			}				

		}
		catch (NameNotFoundException e) {
		   	Log.e(TAG, e.toString());
		} 		
        
        // Set up UI elements
        Resources resources = this.getResources();
        AssetManager assetManager = resources.getAssets();
        
        mPauseButton = (Button) findViewById(R.id.buttonPause);
        mAddMeasureButton = (Button) findViewById(R.id.buttonAddMeasure);
        mTextInfoView = (TextView) findViewById(R.id.textViewInfo);
        mMeasuresDisplayText = (TextView) findViewById(R.id.measuresDisplayText);
        
		// Don't actually show skin conductance meter unless we get samples
        ImageView image = (ImageView) findViewById(R.id.imageView1);
        image.setImageResource(R.drawable.signal_bars0);  
        
		// Check to see of there a device configured for EEG, if so then show the skin conductance meter
		String tmp = SharedPref.getString(this, "EEG" ,null);		
		
		if (tmp != null) {
	        image.setVisibility(View.VISIBLE);
		}
		else {
	        image.setVisibility(View.INVISIBLE);
		}
		
		        
        
        
        
		// Initialize SPINE by passing the fileName with the configuration properties
		try {
			mManager = SPINEFactory.createSPINEManager("SPINETestApp.properties", resources);
		} catch (InstantiationException e) {
			Log.e(TAG, "Exception creating SPINE manager: " + e.toString());
			e.printStackTrace();
		}        
		
		try {
			currentMindsetData = new MindsetData(this);
		} catch (Exception e1) {
			Log.e(TAG, "Exception creating MindsetData: " + e1.toString());
			e1.printStackTrace();
		}        
        
		// Establish nodes for BSPAN
		
		// Create a broadcast receiver. Note that this is used ONLY for command messages from the service
		// All data from the service goes through the mail SPINE mechanism (received(Data data)).
		// See public void received(Data data)
        this.mCommandReceiver = new SpineReceiver(this);

        int itemId = 0;
        eegPos = itemId; // eeg always comes first
        mBioParameters.clear();
        
        // First create GraphBioParameters for each of the EEG static params (ie mindset)
        for (itemId = 0; itemId < MindsetData.NUM_BANDS + 2; itemId++) {		// 2 extra, for attention and meditation
        	GraphBioParameter param = new GraphBioParameter(itemId, MindsetData.spectralNames[itemId], "", true);
        	param.isShimmer = false;
        	mBioParameters.add(param);
        }
        
        // Now create all of the potential dynamic GBraphBioParameters (GSR, EMG, ECG, EEG, HR, Skin Temp, Resp Rate
//    	String[] paramNamesStringArray = getResources().getStringArray(R.array.parameter_names);
    	String[] paramNamesStringArray = getResources().getStringArray(R.array.parameter_names_less_eeg);

    	for (String paramName: paramNamesStringArray) {
        	if (paramName.equalsIgnoreCase("not assigned"))
        		continue;

    		GraphBioParameter param = new GraphBioParameter(itemId, paramName, "", true);
        	
        	if (paramName.equalsIgnoreCase("gsr")) {
        		gsrPos = itemId;
        		param.isShimmer = true;
        		param.shimmerSensorConstant = SPINESensorConstants.SHIMMER_GSR_SENSOR; 
            	param.shimmerNode = getShimmerNode();
        	}
        	
        	if (paramName.equalsIgnoreCase("emg")) {
        		emgPos = itemId;
        		param.isShimmer = true;
        		param.shimmerSensorConstant = SPINESensorConstants.SHIMMER_EMG_SENSOR; 
            	param.shimmerNode = getShimmerNode();
        	}
        	
        	if (paramName.equalsIgnoreCase("ecg")) {
        		ecgPos = itemId;
        		param.isShimmer = true;
        		param.shimmerSensorConstant = SPINESensorConstants.SHIMMER_ECG_SENSOR; 
            	param.shimmerNode = getShimmerNode();
        	}
        	
        	if (paramName.equalsIgnoreCase("heart rate")) {
        		heartRatePos = itemId;
        		param.isShimmer = false;
        	}
        	
        	if (paramName.equalsIgnoreCase("resp rate")) {
        		respRatePos = itemId;
        		param.isShimmer = false;
        	}
        	
        	if (paramName.equalsIgnoreCase("skin temp")) {
        		skinTempPos = itemId;
        		param.isShimmer = false;
        	}
        	
        	if (paramName.equalsIgnoreCase("EHealth Airflow")) {
        		eHealthAirFlowPos = itemId;
        		param.isShimmer = false;
        	}

        	if (paramName.equalsIgnoreCase("EHealth Temp")) {
        		eHealthTempPos = itemId;
        		param.isShimmer = false;
        	}

        	if (paramName.equalsIgnoreCase("EHealth SpO2")) {
        		eHealthSpO2Pos = itemId;
        		param.isShimmer = false;
        	}
        	
        	if (paramName.equalsIgnoreCase("EHealth Heartrate")) {
        		eHealthHeartRatePos = itemId;
        		param.isShimmer = false;
        	}

        	if (paramName.equalsIgnoreCase("EHealth GSR")) {
        		eHealthGSRPos = itemId;
        		param.isShimmer = false;
        	}
        	
        	itemId++;
        	mBioParameters.add(param);
    	}
		
		// Since These are static nodes (Non-spine) we have to manually put them in the active node list
		Node mindsetNode = null;
		mindsetNode = new Node(new Address("" + Constants.RESERVED_ADDRESS_MINDSET)); // Note that the sensor id 0xfff1 (-15) is a reserved id for this particular sensor
		mManager.getActiveNodes().add(mindsetNode);
				
		Node zepherNode = null;
		zepherNode = new Node(new Address("" + Constants.RESERVED_ADDRESS_ZEPHYR));
		mManager.getActiveNodes().add(zepherNode);
		
		
		// The arduino node is programmed to look like a static Spine node
		// Note that currently we don't have  to turn it on or off - it's always streaming
		// Since Spine (in this case) is a static node we have to manually put it in the active node list
		// Since the 
		final int RESERVED_ADDRESS_ARDUINO_SPINE = 1;   // 0x0001
		mSpineNode = new Node(new Address("" + RESERVED_ADDRESS_ARDUINO_SPINE));
		mManager.getActiveNodes().add(mSpineNode);		
		
    	
		final String sessionName;
    	
    	// Check to see if we were requested to play back a previous session
		try {
			Bundle bundle = getIntent().getExtras();
			
			if (bundle != null) {
				sessionName = bundle.getString(BioZenConstants.EXTRA_SESSION_NAME);

				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				alert.setTitle("Replay Session " + sessionName + "?");
				alert.setMessage("Make sure to turn off all Bluetooth Sensors!");	

				alert.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {

	                	
	                	try {
							mDataOutHandler.logNote("Replaying data from session " + sessionName);
						} catch (DataOutHandlerException e) {
							Log.e(TAG, e.toString());
							e.printStackTrace();
						}     
	                	
	        			
	        			replaySessionData(sessionName);
	        			AlertDialog.Builder alert1 = new AlertDialog.Builder(mInstance);
	        			alert1.setTitle("INFO");
	        			alert1.setMessage("Replay of session complete!");	
	        			alert1.show();			
	        	    				

	                }
	            });
				alert.show();			
			}
			
			
			
		} catch (Exception e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		}    	
    	
    	
		if (mInternalSensorMonitoring) {
	        // IntentSender Launches our service scheduled with with the alarm manager 
	        mBigBrotherService = PendingIntent.getService(Graphs1Activity.this,
	                0, new Intent(Graphs1Activity.this, BigBrotherService.class), 0);        
			
	        
            long firstTime = SystemClock.elapsedRealtime();	        
            // Schedule the alarm!
            AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
            am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            firstTime, mPollingPeriod * 1000, mBigBrotherService);

            // Tell the user about what we did.
            Toast.makeText(Graphs1Activity.this, R.string.service_scheduled,
                    Toast.LENGTH_LONG).show();		    	
	        
		}
		
        //testFIRFilter();
    	
//    	testHR();
        
       
        
		
	} // End onCreate(

	boolean replaySessionData(String sessionName)
	{
		BufferedReader logReader = null;
		
		// Open a file for saving data
		try {
		    File root = Environment.getExternalStorageDirectory();
		    if (root.canWrite()){
		        File gpxfile = new File(root, sessionName);
		        FileReader gpxreader = new FileReader(gpxfile); // open for append
		        logReader = new BufferedReader(gpxreader);
		        
		        String lineToParse;
		        while ((lineToParse = logReader.readLine()) != null) {
					Log.i("SensorData",lineToParse);
		        	
		        	if (lineToParse.contains("ECG,")) {
		        		
		        		
			        	try {
			            	String[] tokens = lineToParse.split(",");
			              	
			            	if (tokens.length == 7) { 
				            	ShimmerData shimmerData = new ShimmerData(SPINEFunctionConstants.SHIMMER, SPINESensorConstants.SHIMMER_ECG_SENSOR, (byte) 0);
				            	shimmerData.setFunctionCode(SPINEFunctionConstants.SHIMMER);
				            	shimmerData.sensorCode = SPINESensorConstants.SHIMMER_ECG_SENSOR;
				            	shimmerData.ecgLaLL = 2000 + (int) Float.parseFloat(tokens[5].trim());
				            	shimmerData.ecgRaLL = 2000;
				            	shimmerData.timestamp = Integer.parseInt(tokens[1].trim());
				            	this.received(shimmerData);				            	
			            	}
			            	if (tokens.length == 6) { 
				            	ShimmerData shimmerData = new ShimmerData(SPINEFunctionConstants.SHIMMER, SPINESensorConstants.SHIMMER_ECG_SENSOR, (byte) 0);
				            	shimmerData.setFunctionCode(SPINEFunctionConstants.SHIMMER);
				            	shimmerData.sensorCode = SPINESensorConstants.SHIMMER_ECG_SENSOR;
				            	shimmerData.ecgLaLL = 2000 + Integer.parseInt(tokens[4].trim());
				            	shimmerData.ecgRaLL = 2000;
				            	shimmerData.timestamp = Integer.parseInt(tokens[2].trim());
				            	this.received(shimmerData);				            	
			            	}
			            	if (tokens.length == 5) { 
				            	ShimmerData shimmerData = new ShimmerData(SPINEFunctionConstants.SHIMMER, SPINESensorConstants.SHIMMER_ECG_SENSOR, (byte) 0);
				            	shimmerData.setFunctionCode(SPINEFunctionConstants.SHIMMER);
				            	shimmerData.sensorCode = SPINESensorConstants.SHIMMER_ECG_SENSOR;
				            	shimmerData.ecgLaLL = 2000 + Integer.parseInt(tokens[3].trim());
				            	shimmerData.ecgRaLL = 2000;
				            	shimmerData.timestamp = Integer.parseInt(tokens[2].trim());
				            	this.received(shimmerData);				            	
			            	}
			        		
						} catch (Exception e) {
						}	        		
		        	}
	
		        }
		        
		    } 
		    else {
    		    Log.e(TAG, "Could not open file " );
    			AlertDialog.Builder alert = new AlertDialog.Builder(this);
    			
    			alert.setTitle("ERROR");
    			alert.setMessage("Cannot open to file");	
    			alert.show();			
		    	
		    }
		} catch (IOException e) {
		    Log.e(TAG, "Could not open file " + e.getMessage());
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			
			alert.setTitle("ERROR");
			alert.setMessage("Cannot write to file");	
			alert.show();			
		    
		}
		
		try {
			logReader.close();
		} catch (IOException e) {
		    Log.e(TAG, "Could not close file " + e.getMessage());
		}
		
		return true;
	}
    
    private void testHR() {
    	
    	byte[] input = {-22,-29,-33,-93,-48,27,-3,24,-20,-44,22,-40,-70,-82,33,-64,-23,4,-20,28,-78,3,7,23,92,43,14,19,-27,111,93,-38,93,-31,-29,-40,2,59,70,-76,-14,15,-44,-66,-17,-16,-19,-18,-10,-16,53,98,-23,-23,-15,-14,-8,-1,3,-20,-10,-26,-5,-23,-9,-6,18,18,17,16,33,56,46,10,7,9,20,0,12,10,8,8,-5,14,-5,6,-12,1,-9,22,-12,-7,-29,-18,59,47,-25,-31,-33,-39,-38,-27,-15,-8,-45,-42,-26,-15,-32,-13,8,4,21,31,17,22,8,17,5,16,2,0,3,34,1,-1,7,9,6,5,11,-9,20,7,-14,-17,29,99,15,-23,-32,-11,-27,-30,-24,-35,-18,-40,-22,-30,-34,-21,1,27,13,32,29,14,29,29,14,7,14,11,17,-5,9,-4,4,14,-1,8,11,6,12,8,-1,-40,50,98,-29,-19,-33,-22,-20,-30,-36,-5,-28,-28,-49,-45,-38,5,21,12,27,13,14,14,8,22,7,28,6,18,8,8,12,1,-7,-13,-22,19,-11,19,-7,4,-2,43,102,-17,-26,-33,-44,-15,-25,-36,-29,-19,-35,-24,-33,-10,-8,18,44,17,21,12,13,38,1,14,10,6,13,2,-2,12};
    	long timeStamp = 0;
    	
    	for (int i = 0; i < input.length; i++) {
        	ShimmerData shimmerData = new ShimmerData(SPINEFunctionConstants.SHIMMER, SPINESensorConstants.SHIMMER_ECG_SENSOR, (byte) 0);
        	shimmerData.setFunctionCode(SPINEFunctionConstants.SHIMMER);
        	shimmerData.sensorCode = SPINESensorConstants.SHIMMER_ECG_SENSOR;
        	shimmerData.ecgLaLL = 2000 + input[i];
        	shimmerData.ecgRaLL = 2000;
        	shimmerData.timestamp = (int) timeStamp;
        	timeStamp += 512;
        	
        	this.received(shimmerData);
    		
    	}
    	
    	
    	
    }
	
    @Override
	protected void onDestroy() {
    	super.onDestroy();

		if (mInternalSensorMonitoring) {
            // And cancel the alarm.
			AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
            am.cancel(mBigBrotherService);

            
			Intent intent = new Intent();
			intent.setAction(BigBrotherConstants.ACTION_COMMAND_BROADCAST);
			intent.putExtra("message", BigBrotherConstants.SERVICE_OFF);
			sendBroadcast(intent);
            
            
            // Tell the user about what we did.
            Toast.makeText(Graphs1Activity.this, R.string.service_unscheduled,
                    Toast.LENGTH_LONG).show();		
		}    	
    	
    	mDataOutHandler.close();
			
		if (mDataUpdateTimer != null) {
			mDataUpdateTimer.cancel();
			mDataUpdateTimer.purge();
		}

		if (mRespRateAverageTimer != null) {
			mRespRateAverageTimer.cancel();
			mRespRateAverageTimer.purge();
		}
	
    	Log.i(TAG, this.getClass().getSimpleName() + ".onDestroy()"); 
		
		// Send stop command to every shimmer device
		// You might think that it would be better to iterate through the mBioSensors table
		// instead of the mBioParameters table but it's actually easier this way
		for (GraphBioParameter param : mBioParameters) {
			if (param.isShimmer && param.shimmerNode != null) {
				ShimmerNonSpineSetupSensor setup = new ShimmerNonSpineSetupSensor();
				setup.setSensor(param.shimmerSensorConstant);

				String deviceAddress  = SharedPref.getDeviceForParam(this, param.title1);
				if (deviceAddress != null) {
					
					setup.setBtAddress(Util.AsciiBTAddressToBytes(deviceAddress));
					setup.setCommand(ShimmerNonSpineSetupSensor.SHIMMER_COMMAND_STOPPED);
					
					Log.d(TAG, String.format("Setting up Shimmer sensor: %s (%s) (%d) SHIMMER_COMMAND_STOPPED", 
							param.shimmerNode.getPhysicalID(), deviceAddress, param.shimmerSensorConstant));
					mManager.setup(param.shimmerNode, setup);
					
				}
			}
		}
    	this.unregisterReceiver(this.mCommandReceiver);		
    }	
	
	@Override
	protected void onStart() {
		super.onStart();
		
		mIsActive = true;
		
		// we need to register a SPINEListener implementation to the SPINE manager instance
		// to receive sensor node data from the Spine server
		// (I register myself since I'm a SPINEListener implementation!)
		mManager.addListener(this);	        
		
		Log.i(TAG, this.getClass().getSimpleName() + ".onStart()"); 
		
		// Set up filter intents so we can receive broadcasts
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.t2.biofeedback.service.status.BROADCAST");
		this.registerReceiver(this.mCommandReceiver,filter);
		
		int displaySampleTime = 1000;
        String s = SharedPref.getString(this, "display_sample_rate" ,"1");
        mDisplaySampleRate  = Integer.parseInt(s);
		
		
		switch (mDisplaySampleRate) {
			default:
			case 1:
				displaySampleTime = 1000;
				Log.d(TAG, "Setting display sample rate to " + mDisplaySampleRate + " Hz");
				break;
			case 10:
				displaySampleTime = 100;
				Log.d(TAG, "Setting display sample rate to " + mDisplaySampleRate + " Hz");
				break;
			case 100:
				displaySampleTime = 10;
				Log.d(TAG, "Setting display sample rate to " + mDisplaySampleRate + " Hz");
				break;
			case 9999:
				displaySampleTime = 9999;
				Log.d(TAG, "Setting display sample rate to match sensor sample rate");
				break;
		}
		
		if (mDisplaySampleRate != 9999) {
			// Set up a timer to do graphical updates
			mDataUpdateTimer = new Timer();
			mDataUpdateTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					TimerMethod();
				}

			}, 0, displaySampleTime);		
		}
		
		// Set up a timer for GSR average reporting (10 seconds
				
		mRespRateAverageTimer = new Timer();
		mRespRateAverageTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				respRateAverageMethod();
			}

		}, 0, 10000);		
		
		
		if (mAntHrmEnabled) {
	        mAntServiceBound = bindService(new Intent(this, ANTPlusService.class), mConnection, BIND_AUTO_CREATE);
		}
		
		
		
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mDataUpdateTimer != null)
			mDataUpdateTimer.cancel();
		
        if(mAntManager != null)
        {
            saveAntState();
            mAntManager.setCallbacks(null);
            
            if (mAntManager.isChannelOpen(AntPlusManager.HRM_CHANNEL))
            {			
           	 	Log.d(TAG, "onClick (HRM): Close channel");
           	 	mAntManager.closeChannel(AntPlusManager.HRM_CHANNEL);                	
            }            
            
            
        }
        if(mAntServiceBound)
        {
            unbindService(mConnection);
        }		
		
		Log.i(TAG, this.getClass().getSimpleName() + ".onStop()"); 
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		mIsActive = false;
		
		// *******************
    	// Make sure to to this or else you will get more and more notifications from Spine as you 
    	// go into and out of activities!
		// Also make sure to do this in on pause (as opposed to onStop or ondestroy.
		// This will prevent you from receiving messages possibly requested by another activity
    	mManager.removeListener(this);	        
    	
		
		Log.i(TAG, this.getClass().getSimpleName() + ".onPause()"); 
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(TAG, this.getClass().getSimpleName() + ".onResume()"); 
		mLastRespRateTime = System.currentTimeMillis();
		int mRespRateTotal = 0;
		int mRespRateIndex = 0;
		
		// Set up Device data chart
        generateChart();
		
		mManager.discoveryWsn();		// discoveryCompleted() is called after this is done		
	}

	@Override
	public void newNodeDiscovered(Node newNode) {
		Log.d(TAG, this.getClass().getSimpleName() + ".newNodeDiscovered()"  + newNode.toString()); 
	}

	@Override
	public void received(ServiceMessage msg) {
		
	}

	/**
	 * This is where we receive sensor data that comes through the actual
	 * Spine channel. 
	 * @param data		Generic Spine data packet. Should be cast to specifid data type indicated by data.getFunctionCode()
	 *
	 * @see spine.SPINEListener#received(spine.datamodel.Data)
	 */
	@Override
	public void received(Data data) {
		//Log.d(TAG, this.getClass().getSimpleName() + ".received()"); 
		
		if (data != null) {
			switch (data.getFunctionCode()) {

			// E-Health board
			case SPINEFunctionConstants.FEATURE: {
				FeatureData featureData = (FeatureData) data;
				
				Feature[] feats = featureData.getFeatures();
				
				if (feats.length < 2) {
					break;
				}
				Feature firsFeat = feats[0];
				Feature Feat2 = feats[1];
				int airFlow = firsFeat.getCh1Value();
				int scaledTemp = firsFeat.getCh2Value();
				float temp = (float)scaledTemp/(65535F/9F) + 29F;
				int BPM = firsFeat.getCh3Value();
				int SPO2 = firsFeat.getCh4Value();
				int scaledConductance = Feat2.getCh1Value();
				float conductance = (float) scaledConductance / (65535F/4F);
				Log.d(TAG, "E-health Values = " + airFlow + ", " + temp + ", " + BPM + ", " + SPO2 + ", " + conductance + ", " );
				synchronized(mKeysLock) {
					mBioParameters.get(eHealthAirFlowPos).rawValue = airFlow;
					mBioParameters.get(eHealthAirFlowPos).scaledValue = (int) map(airFlow,0,360,0,100);
					
					mBioParameters.get(eHealthTempPos).rawValue = (int) temp;
					mBioParameters.get(eHealthTempPos).scaledValue = (int) map(temp,29,40,0,100);
					
					mBioParameters.get(eHealthHeartRatePos).rawValue = BPM;
					mBioParameters.get(eHealthHeartRatePos).scaledValue = (int) map(BPM,30,220,0,100);
					
					mBioParameters.get(eHealthSpO2Pos).rawValue = SPO2;
					mBioParameters.get(eHealthSpO2Pos).scaledValue = SPO2;
					
					mBioParameters.get(eHealthGSRPos).rawValue = (int) map(scaledConductance,0,65535,0,100);
					mBioParameters.get(eHealthGSRPos).scaledValue = (int) map(scaledConductance,0,65535,0,100);;
				
					DataOutPacket packet = mDataOutHandler.new DataOutPacket();
					packet.add(DataOutHandlerTags.RAW_HEARTRATE, BPM);
					packet.add(DataOutHandlerTags.RAW_GSR, conductance);
					packet.add(DataOutHandlerTags.RAW_SKINTEMP, temp);
					packet.add(DataOutHandlerTags.SPO2, SPO2);
					packet.add(DataOutHandlerTags.AIRFLOW, airFlow);
					try {
						mDataOutHandler.handleDataOut(packet);
					} catch (DataOutHandlerException e) {
						Log.e(TAG, e.toString());
						e.printStackTrace();
					}						
					
					
				}
				
				
				
				break;
			}
			
			
			case SPINEFunctionConstants.HEARTBEAT: {
				
				synchronized(mKeysLock) {
					
					HeartBeatData thisData = (HeartBeatData) data; 
					
					int scaled  = (thisData.getBPM() )/2 ;
					
					if (mHeartRateSource == HEARTRATE_ANT) {
						mBioParameters.get(heartRatePos).rawValue = thisData.getBPM();
						mBioParameters.get(heartRatePos).scaledValue = scaled;	
					}
					
			        // Send data to output
					DataOutPacket packet = mDataOutHandler.new DataOutPacket();
					packet.add(DataOutHandlerTags.RAW_HEARTRATE, thisData.getBPM());
					try {
						mDataOutHandler.handleDataOut(packet);
					} catch (DataOutHandlerException e) {
						Log.e(TAG, e.toString());
						e.printStackTrace();
					}					
					
    				// See if we are configured to update display every time we get sensor data
    				if (mDisplaySampleRate == 9999 && mIsActive) {
    					this.runOnUiThread(Timer_Tick);	    					
    				}					
					
				}
				
				break;
			}
			
			case SPINEFunctionConstants.SHIMMER: {
				Node node = data.getNode();
				numTicsWithoutData = 0;		
				
				Node source = data.getNode();
				ShimmerData shimmerData = (ShimmerData) data;
				
	        	synchronized(mKeysLock) {
					switch (shimmerData.sensorCode) {
					case SPINESensorConstants.SHIMMER_GSR_SENSOR:
						
						mBioDataProcessor.processShimmerGSRData(shimmerData, mConfiguredGSRRange);
						
						mBioParameters.get(gsrPos).rawValue = (int) (mBioDataProcessor.mGsrConductance * 1000);		// scale by 1000 to fit a float into an int
						double scaled  = mBioDataProcessor.mGsrConductance * 10;
	    				mBioParameters.get(gsrPos).scaledValue = (int) scaled;
	    				
	    				// See if we are configured to update display every time we get sensor data
	    				if (mDisplaySampleRate == 9999 && mIsActive) {
	    					this.runOnUiThread(Timer_Tick);	    					
	    				}
	    				
						break;
					case SPINESensorConstants.SHIMMER_EMG_SENSOR:
						
						mBioDataProcessor.processShimmerEMGData(shimmerData); 
						
	    				scaled  = MathExtra.scaleData((float)shimmerData.emg, 4000F, 0F, 100);
	    				mBioParameters.get(emgPos).rawValue = (int) scaled;
	    				mBioParameters.get(emgPos).scaledValue = (int) scaled;
	    				
	    				// See if we are configured to update display every time we get sensor data
	    				if (mDisplaySampleRate == 9999) {
	    					this.runOnUiThread(Timer_Tick);	    					
	    				}
	    				
						break;
					case SPINESensorConstants.SHIMMER_ECG_SENSOR:
						
						// If we're receiving packets from shimmer egg then swith the heartrate to shimmer
						// Otherwise we'll leave it at the default which is zephyr
						mHeartRateSource = HEARTRATE_SHIMMER;						

						mBioDataProcessor.processShimmerECGData(shimmerData); 						
						
	    				scaled  = (mBioDataProcessor.mRawEcg + 50 )/2 ;
	    				mBioParameters.get(ecgPos).rawValue = (int) scaled;
	    				mBioParameters.get(ecgPos).scaledValue = (int) scaled;
						
						if (mHeartRateSource == HEARTRATE_SHIMMER) {
							mBioParameters.get(heartRatePos).rawValue = mBioDataProcessor.mShimmerHeartRate;
							mBioParameters.get(heartRatePos).scaledValue = mBioDataProcessor.mShimmerHeartRate;
						}
	    				
	    				// See if we are configured to update display every time we get sensor data
	    				if (mDisplaySampleRate == 9999) {
	    					this.runOnUiThread(Timer_Tick);	    					
	    				}
	    				
						break;
					}
	        	}
				break;
			}			
			
			case SPINEFunctionConstants.ZEPHYR: {

				numTicsWithoutData = 0;		
				mBioDataProcessor.processZephyrData(data);	

	        	synchronized(mKeysLock) {				

					if (mHeartRateSource == HEARTRATE_ZEPHYR) {
		        		mBioParameters.get(heartRatePos).scaledValue = mBioDataProcessor.mZephyrHeartRate/3;
		        		mBioParameters.get(heartRatePos).rawValue = mBioDataProcessor.mZephyrHeartRate;
					}	        		

	        		mBioParameters.get(respRatePos).scaledValue = (int) mBioDataProcessor.mRespRate * 5;
	        		mBioParameters.get(respRatePos).rawValue = (int) mBioDataProcessor.mRespRate;

	        		mBioParameters.get(skinTempPos).scaledValue = (int) mBioDataProcessor.mSkinTempF;
	        		mBioParameters.get(skinTempPos).rawValue = (int) mBioDataProcessor.mSkinTempF;
	        	}
				
	        	synchronized(mRespRateAverageLock) {				
	        		mRespRateTotal += mBioDataProcessor.mRespRate;
	        		mRespRateIndex++;
	        	}
				
				// See if we are configured to update display every time we get sensor data
				if (mDisplaySampleRate == 9999) {
					this.runOnUiThread(Timer_Tick);	    					
				}
				break;
			} // End case SPINEFunctionConstants.ZEPHYR:			

			case SPINEFunctionConstants.MINDSET: {
				Node source = data.getNode();
				MindsetData mindsetData = (MindsetData) data;

				mBioDataProcessor.processMindsetData(data, currentMindsetData);
					
				if (mindsetData.exeCode == Constants.EXECODE_SPECTRAL || mindsetData.exeCode == Constants.EXECODE_RAW_ACCUM) {

					numTicsWithoutData = 0;
					
					synchronized(mKeysLock) {				
				        for (int i = 0; i < MindsetData.NUM_BANDS + 2; i++) {		// 2 extra, for attention and meditation
				        	mBioParameters.get(i).scaledValue = currentMindsetData.getFeatureValue(i);
				        	mBioParameters.get(i).rawValue = currentMindsetData.getFeatureValue(i);
				        }
		        	}	
    				// See if we are configured to update display every time we get sensor data
    				if (mDisplaySampleRate == 9999) {
    					this.runOnUiThread(Timer_Tick);	    					
    				}
				}				
					
				if (mindsetData.exeCode == Constants.EXECODE_POOR_SIG_QUALITY) {
					
					// Now show signal strength as bars
					int sigQuality = mindsetData.poorSignalStrength & 0xff;
					ImageView image = (ImageView) findViewById(R.id.imageView1);
					if (sigQuality == 200)
						image.setImageResource(R.drawable.signal_bars0);
					else if (sigQuality > 150)
						image.setImageResource(R.drawable.signal_bars1);
					else if (sigQuality > 100)
						image.setImageResource(R.drawable.signal_bars2);
					else if (sigQuality > 50)
						image.setImageResource(R.drawable.signal_bars3);
					else if (sigQuality > 25)
						image.setImageResource(R.drawable.signal_bars4);
					else 
						image.setImageResource(R.drawable.signal_bars5);
					
					if (sigQuality == 200 && mPrevSigQuality != 200) {
						Toast.makeText (getApplicationContext(), "Headset not makeing good skin contact. Please Adjust", Toast.LENGTH_LONG).show ();
					}
					mPrevSigQuality = sigQuality;
				}
					
				break;
			} // End case SPINEFunctionConstants.MINDSET:
			} // End switch (data.getFunctionCode())
		} // End if (data != null)
	}

	// Note that this is really inaptly named. This simply gets called a certain time period after
	// discovery is initiated (2 sec?)
	@Override
	public void discoveryCompleted(Vector activeNodes) {
		Log.d(TAG, this.getClass().getSimpleName() + ".discoveryCompleted()");
		
		// Tell the bluetooth service to send us a list of bluetooth devices and system status
		// Response comes in public void onStatusReceived(BioFeedbackStatus bfs) STATUS_PAIRED_DEVICES
		mManager.pollBluetoothDevices();		
	}

	/**
	 * This callback is called whenever the AndroidBTService sends us an indication that
	 * it is actively trying to establish a BT connection to one of the nodes.
	 * 
	 * @see com.t2.SpineReceiver.OnBioFeedbackMessageRecievedListener#onStatusReceived(com.t2.SpineReceiver.BioFeedbackStatus)
	 */
	@Override
	public void onStatusReceived(BioFeedbackStatus bfs) {
		String name = bfs.name;
		if (name == null ) name = "sensor node";
		if(bfs.messageId.equals("CONN_CONNECTING")) {
			Log.d(TAG, "Received command : " + bfs.messageId + " to "  + name );
			Toast.makeText (getApplicationContext(), "Connecting to " + name, Toast.LENGTH_SHORT).show ();
		} 
		else if(bfs.messageId.equals("CONN_ANY_CONNECTED")) {
			Log.d(TAG, "Received command : " + bfs.messageId + " to "  + name );
			// Something has connected - discover what it was
			mManager.discoveryWsn();
			Toast.makeText (getApplicationContext(), name + " Connected", Toast.LENGTH_SHORT).show ();
		} 
		else if(bfs.messageId.equals("CONN_CONNECTION_LOST")) {
			Log.d(TAG, "Received command : " + bfs.messageId + " to "  + name );
			Toast.makeText (getApplicationContext(), name + " Connection lost ****", Toast.LENGTH_SHORT).show ();
		}
		else if(bfs.messageId.equals("STATUS_PAIRED_DEVICES")) {
			Log.d(TAG, "Received command : " + bfs.messageId + " to "  + name );
			Log.d(TAG, bfs.address );
			
			// We don't want to take any action unless we're ready to go 
			if (!mIsActive)
				return;
			populateBioSensors(bfs.address);		
			validateBioSensors();
	
			// Send startup command to every shimmer device
			for (GraphBioParameter param : mBioParameters) {
				if (param.isShimmer && param.shimmerNode != null) {
					ShimmerNonSpineSetupSensor setup = new ShimmerNonSpineSetupSensor();
					setup.setSensor(param.shimmerSensorConstant);

					String deviceAddress  = SharedPref.getDeviceForParam(this, param.title1);
					if (deviceAddress != null) {
						
						setup.setBtAddress(Util.AsciiBTAddressToBytes(deviceAddress));
						
						byte startShimmercommand;
				        String s = SharedPref.getString(this, "sensor_sample_rate" ,"4");
				        int sensorSampleRate  = Integer.parseInt(s);
						
						Log.d(TAG, "Initializing sensor sample rate to " + sensorSampleRate);
						switch (sensorSampleRate) {
							default:
							case 4:
								startShimmercommand = ShimmerNonSpineSetupSensor.SHIMMER_COMMAND_RUNNING_4HZ_AUTORANGE;
								break;
							case 10:
								startShimmercommand = ShimmerNonSpineSetupSensor.SHIMMER_COMMAND_RUNNING_10HZ_AUTORANGE;
								break;
							case 32:
								startShimmercommand = ShimmerNonSpineSetupSensor.SHIMMER_COMMAND_RUNNING_32HZ_AUTORANGE;
								break;
							case 50:
								startShimmercommand = ShimmerNonSpineSetupSensor.SHIMMER_COMMAND_RUNNING_50HZ_AUTORANGE;
								break;
							case 64:
								startShimmercommand = ShimmerNonSpineSetupSensor.SHIMMER_COMMAND_RUNNING_64HZ_AUTORANGE;
								break;
							case 100:
								startShimmercommand = ShimmerNonSpineSetupSensor.SHIMMER_COMMAND_RUNNING_100HZ_AUTORANGE;
								break;
							case 125:
								startShimmercommand = ShimmerNonSpineSetupSensor.SHIMMER_COMMAND_RUNNING_125HZ_AUTORANGE;
								break;
						
						}

						setup.setCommand(startShimmercommand);
						mConfiguredGSRRange = Util.getGsrRangeFromShimmerCommand(startShimmercommand);				
											
						
						Log.d(TAG, String.format("Setting up Shimmer sensor: %s (%s) (%d) SHIMMER_COMMAND_RUNNING", 
								param.shimmerNode.getPhysicalID(), deviceAddress, param.shimmerSensorConstant));
						mManager.setup(param.shimmerNode, setup);
						
					}
					else {

					}
				}
			}
		}		
	}	
	
    

	
	/**
	 * This method is called directly by the timer and runs in the same thread as the timer
	 * From here We call the method that will work with the UI through the runOnUiThread method.
	 */	
	private void TimerMethod() {
		this.runOnUiThread(Timer_Tick);
	}

	/**
	 * This method runs in the same thread as the UI.
	 */
	private Runnable Timer_Tick = new Runnable() {
		public void run() {
			numTicsWithoutData++;
			
			if (mPaused == true || currentMindsetData == null) {
//				if (mPaused == true || currentMindsetData == null || numTicsWithoutData > 2) {
				return;
			}

			String bandValuesString = "";
	        
			// Output a point for each visible key item
			int keyCount = mBioParameters.size();
			for(int i = 0; i < mBioParameters.size(); ++i) {
				GraphBioParameter item = mBioParameters.get(i);
				int rawValue = item.rawValue;
				int scaledValue = item.scaledValue;
				
				if(!item.visible) {
					continue;
				}

				// Special case for GSR since it's actually a float scaled to fit into an int
				if (gsrPos == i) {
					float conductance = (float) rawValue / 1000F;
					bandValuesString += item.title1 + ":" + conductance + ", ";				
				}
				else {
					bandValuesString += item.title1 + ":" + rawValue + ", ";				
				}
				
				
				
				
				item.series.add(mSpineChartX, scaledValue);
				if (item.series.getItemCount() > SPINE_CHART_SIZE) {
					item.series.remove(0);
				}
					
			} 			
			mSpineChartX++;
			
			if (mDeviceChartView != null) {
	            mDeviceChartView.repaint();
	        }   				
	        
	        mTextInfoView.setText(bandValuesString);
		}
	};
    
	private void respRateAverageMethod() {
		this.runOnUiThread(respRate_Average_Tick);
	}
    
	/**
	 * This method runs in the same thread as the UI.
	 */
	private Runnable respRate_Average_Tick = new Runnable() {
		public void run() {
	
			if (mRespRateIndex == 0)
				return;
			
			final int rrAvg;			
        	synchronized(mRespRateAverageLock) {				
				
				rrAvg = mRespRateTotal / mRespRateIndex;
				mRespRateTotal = 0;
				mRespRateIndex = 0;					
        	}			
			
	        // Send data to output
        	DataOutPacket packet = mDataOutHandler.new DataOutPacket();
			packet.add(DataOutHandlerTags.AVERAGE_RESP_RATE, rrAvg);
			try {
				mDataOutHandler.handleDataOut(packet);
			} catch (DataOutHandlerException e) {
				Log.e(TAG, e.toString());
				e.printStackTrace();
			}
		}
	};
    	
	
	
	/**
	 * Goes through all all parameters in "keyItems", it saves the visible ones to the long array toggledIds[]
	 * Then calls setVisibleIds to save this long list to a string list in SharedPref at
	 *  
	 * "results_visible_ids_measure2"
	 */
	private void saveVisibleKeyIds() {
		ArrayList<Long> toggledIds = new ArrayList<Long>();
		for(int i = 0; i < mBioParameters.size(); ++i) {
			GraphBioParameter item = mBioParameters.get(i);
			if(item.visible) {
				toggledIds.add(item.id);
			}
		}
		setVisibleIds(KEY_NAME, toggledIds);
	}
	
	/**
	 * Saves long array of ids to a string array at 
	 * "results_visible_ids_measure3"
	 *	
	 * @param keySuffix	Id of the array
	 * @return			a long array containing ids of parameters that are visible
	 */	
	private void setVisibleIds(String keyName, ArrayList<Long> ids) {
		SharedPref.setValues(
				sharedPref, 
				keyName, 
				",", 
				ArraysExtra.toStringArray(ids.toArray(new Long[ids.size()]))
		);
	}	
	
	/**
	 * 
	 * @param keySuffix	id of the array
	 * @param ids 		long array of ids to save to Shared Params
	 */
	private ArrayList<Long> getVisibleIds(String keyName) {
		String[] idsStrArr = SharedPref.getValues(
				sharedPref, 
				keyName, 
				",",
				new String[0]
		);
		
		return new ArrayList<Long>(
				Arrays.asList(
						ArraysExtra.toLongArray(idsStrArr)
				)
		);
	}	
	
	/**
	 * Returns a unique key color based on the current index and total count of parameters
	 * @param currentIndex		Index of current parameter
	 * @param totalCount		Total number of parameters
	 * @return					Unique color based in inputs
	 */
	protected int getKeyColor(int currentIndex, int totalCount) {
		float hue = currentIndex / (1.00f * totalCount) * 360.00f;
		
		return Color.HSVToColor(
    			255,
    			new float[]{
    				hue,
    				1.0f,
    				1.0f
    			}
    	);
	}	
	
	public void onButtonClick(View v)
	{
		 final int id = v.getId();
		    switch (id) {
//		    case R.id.buttonBack:
//		    	finish();
//		    	
//		    	break;
//		    		    
		    case R.id.buttonAddMeasure:
		    	
		    	boolean toggleArray[] = new boolean[mBioParameters.size()];
				for(int j = 0; j < mBioParameters.size(); ++j) {
					GraphBioParameter item = mBioParameters.get(j);
					if(item.visible)
						toggleArray[j] = true;
					else
						toggleArray[j] = false;
				}		    	
		    	
				String[] measureNames = new String[mBioParameters.size()];
				int i = 0;
				for (GraphBioParameter item: mBioParameters) {
					measureNames[i++] = item.title1;
				}
				
				// Present dialog to allow user to choose which parameters to view in this activity
		    	AlertDialog.Builder alert = new AlertDialog.Builder(this);
		    	alert.setTitle(R.string.alert_dialog_measure_selector);
//		    	alert.setMultiChoiceItems(R.array.measure_select_dialog_items,
				    	alert.setMultiChoiceItems(measureNames,
		    			toggleArray,
	                    new DialogInterface.OnMultiChoiceClickListener() {

		    			public void onClick(DialogInterface dialog, int whichButton,boolean isChecked) {

		    				GraphBioParameter item = mBioParameters.get(whichButton);
                			item.visible = item.visible ? false: true;
	                 		saveVisibleKeyIds();	
	                 		generateChart();	                 		
		    			}
	                    });
		    	alert.setPositiveButton(R.string.alert_dialog_ok, new DialogInterface.OnClickListener() {
	                public void onClick(DialogInterface dialog, int whichButton) {
                 		generateChart();	                 		
	                }
	            });
	
				alert.show();
		    	
		    	break;

		    case R.id.buttonPause:
				if (mPaused == true) {
					mPaused = false;
					mPauseButton.getBackground().setColorFilter(Color.LTGRAY, PorterDuff.Mode.MULTIPLY);
					mPauseButton.setText(R.string.button_running);
					if (mLoggingEnabled) {
						try {
							mDataOutHandler.logNote(getString(R.string.un_paused));
						} catch (DataOutHandlerException e) {
							Log.e(TAG, e.toString());
							e.printStackTrace();
						} // data header
					}        
					if (mLogCatEnabled) {
						Log.d(TAG, "Un-Paused" );
					}						
				}
				else {
					mPaused = true;
					mPauseButton.getBackground().setColorFilter(0xFFFF0000, PorterDuff.Mode.MULTIPLY);
					mPauseButton.setText(R.string.button_pause);
					if (mLoggingEnabled) {
						try {
							mDataOutHandler.logNote(getString(R.string.paused));
						} catch (DataOutHandlerException e) {
							Log.e(TAG, e.toString());
							e.printStackTrace();
						} // data header
					}
					if (mLogCatEnabled) {
						Log.d(TAG, "Paused" );
					}						
				}
		        break;
		    } // End switch		
	}	

	/**
	 * Sets up all parameters for display of both the chart on the screen 
	 * AND a color coded display of the parameters and their values 
	 */
	private void generateChart() {
        // Set up chart
    	XYMultipleSeriesDataset deviceDataset = new XYMultipleSeriesDataset();
    	XYMultipleSeriesRenderer deviceRenderer = new XYMultipleSeriesRenderer();        

        LinearLayout layout = (LinearLayout) findViewById(R.id.deviceChart);    	
    	if (mDeviceChartView != null) {
    		layout.removeView(mDeviceChartView);
    	}
       	if (true) {
          mDeviceChartView = ChartFactory.getLineChartView(this, deviceDataset, deviceRenderer);
          mDeviceChartView.setBackgroundColor(Color.BLACK);
          layout.addView(mDeviceChartView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        }    
    	
        deviceRenderer.setShowLabels(false);
        deviceRenderer.setMargins(new int[] {0,5,5,0});
        deviceRenderer.setShowAxes(true);
        deviceRenderer.setShowLegend(false);
        
        deviceRenderer.setZoomEnabled(false, false);
        deviceRenderer.setPanEnabled(false, false);
        deviceRenderer.setYAxisMin(0);
        deviceRenderer.setYAxisMax(100);

        SpannableStringBuilder sMeasuresText = new SpannableStringBuilder("Displaying: ");
        
		ArrayList<Long> visibleIds = getVisibleIds(KEY_NAME);
		
		int keyCount = mBioParameters.size();
        keyCount = mBioParameters.size();
        
		int lineNum = 0;
		for(int i = 0; i < mBioParameters.size(); ++i) {
			GraphBioParameter item = mBioParameters.get(i);
			
			item.visible = visibleIds.contains(item.id);
			if(!item.visible) {
				continue;
			}
			
			deviceDataset.addSeries(item.series);
			item.color = getKeyColor(i, keyCount);
			
			// Add name of the measure to the displayed text field
			ForegroundColorSpan fcs = new ForegroundColorSpan(item.color);
			int start = sMeasuresText.length();
			sMeasuresText.append(mBioParameters.get(i).title1 + ", ");
			int end = sMeasuresText.length();
			sMeasuresText.setSpan(fcs, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
			if (sMeasuresText.length() > 40 && lineNum == 0) {
				lineNum++;
			}
			
			XYSeriesRenderer seriesRenderer = new XYSeriesRenderer();
			seriesRenderer.setColor(item.color);
			seriesRenderer.setPointStyle(PointStyle.CIRCLE);
			
			deviceRenderer.addSeriesRenderer(seriesRenderer);
		}     
		
		mMeasuresDisplayText.setText(sMeasuresText) ;       
	}	
	
	/**
	 * Receives a json string containing data about all of the paired sensors
	 * the adds a new BioSensor for each one to the mBioSensors collection
	 * 
	 * @param jsonString String containing info on all paired devices
	 */
	private void populateBioSensors(String jsonString) {
		
		Log.d(TAG, this.getClass().getSimpleName() + " populateBioSensors");
		// Now clear it out and populate it. The only difference is that
		// if a sensor previously existed, then 
		mBioSensors.clear();
		try {
			JSONArray jsonArray = new JSONArray(jsonString);
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				Boolean enabled = jsonObject.getBoolean("enabled");
				String name = jsonObject.getString("name"); 
				String address = jsonObject.getString("address"); 
				int connectionStatus = jsonObject.getInt("connectionStatus");					
				
				if (name.equalsIgnoreCase("system")) {
					mBluetoothEnabled = enabled;
				}
				else {
					
					Log.d(TAG, "Adding sensor " + name + ", " + address + (enabled ? ", enabled":", disabled") + " : " + Util.connectionStatusToString(connectionStatus));
					BioSensor bioSensor = new BioSensor(name, address, enabled);
					bioSensor.mConnectionStatus = connectionStatus;	
					mBioSensors.add(bioSensor);
				}
			}			
		} catch (JSONException e) {
		   	Log.e(TAG, e.toString());
		}
	}
	
	/**
	 * Validates sensors, makes sure that bluetooth is on and each sensor has a parameter associated with it
	 */
	void validateBioSensors() {
		
		// First make sure that bluetooth is enabled
		if (!mBluetoothEnabled) {
			AlertDialog.Builder alert1 = new AlertDialog.Builder(this);

			alert1.setMessage("Bluetooth is not enabled on your device. Press OK to go to the wireless system" +
					"settings to turn it on");

			alert1.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					mInstance.startActivityForResult(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS), BLUETOOTH_SETTINGS_ID);
				}
				});
			alert1.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
				});
			alert1.show();		
		}
		String badSensorName = null;
		
		// Now make sure that every device has a parameter associated with it
		for (BioSensor sensor: mBioSensors) {
			if (sensor.mEnabled) {
				String param  = SharedPref.getParamForDevice(mInstance, sensor.mBTAddress);
				//Log.d(TAG, "sensor: " + sensor.mBTName + ", parameter: " + param);
				if (param == null) {
					badSensorName = sensor.mBTName;
					break;
				}
			}
		} // end for (BioSensor sensor: mBioSensors)
		
		if (badSensorName != null) {
			AlertDialog.Builder alert1 = new AlertDialog.Builder(this);

			alert1.setMessage("Sensor " + badSensorName + " is enabled but " + 
					" does not have a parameter associated with it." + 
					"Press Ok to associate one");

			alert1.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					Intent intent2 = new Intent(mInstance, DeviceManagerActivity.class);			
					mInstance.startActivity(intent2);				
				}
				});
			alert1.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
				}
				});
			alert1.show();	
	
		}
	}
	
	/**
	 * Wrapper for BioParameter that has a graph element
	 * 
	 * @author scott.coleman
	 *
	 */
	static class GraphBioParameter extends BioParameter{
		public XYSeries series;		
		public Boolean isShimmer;
		Node shimmerNode;		
		byte shimmerSensorConstant;
		
		public GraphBioParameter(long id, String title1, String title2,  Boolean enabled) {
			super(id, title1, title2, enabled);
			isShimmer = false;
			shimmerNode = null;		
			shimmerSensorConstant = 0;
			series = new XYSeries(title1);		
		}
		
	}

	
	// ----------------------------------------------------------
	// ANT specific stuff
	//-----------------------------------------------------------
	
	@Override
	public void errorCallback() {
		Log.e(TAG, "ANT error");
		
	}

	@Override
	public void notifyAntStateChanged() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyChannelStateChanged(byte channel) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void notifyChannelDataChanged(byte channel) {
//		Log.i(TAG, "notifyChannelDataChanged");
		
    	HeartBeatData thisData = new HeartBeatData();
    	thisData.setFunctionCode(SPINEFunctionConstants.HEARTBEAT);
    	thisData.setBPM(mAntManager.getBPM());
    	this.received(thisData);				            	
	}
	
	

	   private final ServiceConnection mConnection = new ServiceConnection()
	   {
	        @Override
	        public void onServiceDisconnected(ComponentName name)
	        {
	            //This is very unlikely to happen with a local service (ie. one in the same process)
	            mAntManager.setCallbacks(null);
	            mAntManager = null;
	        }
	        
	        @Override
	        public void onServiceConnected(ComponentName name, IBinder service)
	        {
	            mAntManager = ((ANTPlusService.LocalBinder)service).getManager();
	            mAntManager.setCallbacks(Graphs1Activity.this);
	            loadAntState();
	            notifyAntStateChanged();
	            
	            // Start ANT automatically
	            mAntManager.doEnable();
	    		Log.i(TAG, "Starting heart rate data");
	            mAntManager.openChannel(AntPlusManager.HRM_CHANNEL, true);	
	            mAntManager.requestReset(); 	            
	            
	        }
	   };
	
		
	   /**
	    * Store application persistent data.
	    */
	   private void saveAntState()
	   {
	      // Save current Channel Id in preferences
	      // We need an Editor object to make changes
	      SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	      SharedPreferences.Editor editor = settings.edit();
	      editor.putInt("DeviceNumberHRM", mAntManager.getDeviceNumberHRM());
	      editor.putInt("DeviceNumberSDM", mAntManager.getDeviceNumberSDM());
	      editor.putInt("DeviceNumberWGT", mAntManager.getDeviceNumberWGT());
	      editor.putInt("ProximityThreshold", mAntManager.getProximityThreshold());
	      editor.putInt("BufferThreshold", mAntManager.getBufferThreshold());
	      editor.commit();
	   }
	   
	   /**
	    * Retrieve application persistent data.
	    */
	   private void loadAntState()
	   {
	      // Restore preferences
	      SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	      mAntManager.setDeviceNumberHRM((short) settings.getInt("DeviceNumberHRM", ANT_WILDCARD));
	      mAntManager.setDeviceNumberSDM((short) settings.getInt("DeviceNumberSDM", ANT_WILDCARD));
	      mAntManager.setDeviceNumberWGT((short) settings.getInt("DeviceNumberWGT", ANT_WILDCARD));
	      mAntManager.setProximityThreshold((byte) settings.getInt("ProximityThreshold", ANT_DEFAULT_BIN));
	      mAntManager.setBufferThreshold((short) settings.getInt("BufferThreshold", ANT_DEFAULT_BUFFER_THRESHOLD));
	   }
	   		
	   private long map(long x, long in_min, long in_max, long out_min, long out_max)
	   {
	     return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
	   }	

	   private double map(double x, double in_min, double in_max, double out_min, double out_max)
	   {
	     return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
	   }	
}