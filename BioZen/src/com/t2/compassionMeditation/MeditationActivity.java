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

import java.io.File;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.achartengine.model.XYSeries;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.t2health.lib1.BioParameter;
import org.t2health.lib1.BioSensor;
import org.t2health.lib1.dsp.T2MovingAverageFilter;

import bz.org.t2health.lib.activity.BaseActivity;
import bz.org.t2health.lib.analytics.Analytics;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.oregondsp.signalProcessing.filter.iir.ChebyshevI;

import com.t2.SpineReceiver;
import com.t2.SpineReceiver.BioFeedbackStatus;
import com.t2.SpineReceiver.OnBioFeedbackMessageRecievedListener;
import com.t2.antlib.ANTPlusService;
import com.t2.antlib.AntPlusManager;
import com.t2.biofeedback.activity.BTServiceManager;
import com.t2.biofeedback.device.shimmer.ShimmerDevice;
import com.t2.compassionDB.BioSession;
import com.t2.compassionDB.BioUser;
import com.t2.compassionUtils.MathExtra;
import com.t2.compassionUtils.TMovingAverageFilter;
import com.t2.compassionUtils.RateOfChange;
import com.t2.compassionUtils.Util;
import com.t2.dataouthandler.DataOutHandler;
import com.t2.dataouthandler.DataOutHandlerException;
import com.t2.dataouthandler.DataOutHandlerTags;
import com.t2.dataouthandler.DataOutPacket;
import com.t2.t2sensorlib.BigBrotherService;

import com.t2.Constants;

import spine.datamodel.Node;
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
import spine.datamodel.ServiceMessage;
import spine.datamodel.ShimmerData;
import spine.datamodel.ZephyrData;
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
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

//Need the following import to get access to the app resources, since this
//class is in a sub-package.
import com.t2.R;

public class MeditationActivity extends BaseActivity
		implements 	OnBioFeedbackMessageRecievedListener, SPINEListener, 
					View.OnTouchListener, SeekBar.OnSeekBarChangeListener,
					AntPlusManager.Callbacks {
	private static final String TAG = "BFDemo";
	private static final String mActivityVersion = "2.4";
	private static final int BLUETOOTH_SETTINGS_ID = 987;	
	
	private static final int HEARTRATE_SHIMMER = 1;	
	private static final int HEARTRATE_ZEPHYR = 2;	
	private static final int HEARTRATE_ANT = 3;	
	
	
//	int mHeartRateSource = HEARTRATE_SHIMMER;	int mHeartRateSource = HEARTRATE_ZEPHYR;
	int mHeartRateSource = HEARTRATE_ZEPHYR;
	
	private String mAppId = "bioZenMeditation";
	
	/**
	 * Flag to set for manual debugging of activity (Shows values on screen
	 */
	private boolean mDebug = false;
	
	private int mIntroFade = 255;
	private int mSubTimerClick = 100;
	
	private String mUserId;
	private String mSessionId;	
	
	
	Dao<BioUser, Integer> mBioUserDao;
	Dao<BioSession, Integer> mBioSessionDao;

	BioUser mCurrentBioUser = null;
	BioSession mCurrentBioSession = null;
	List<BioUser> currentUsers;	
	
	File mLogFile;

	/**
	 * Number of seconds remaining in the session
	 *   This is set initially from SharedPref.PREF_SESSION_LENGTH
	 */
	private int mSecondsRemaining = 0;
	private int mSecondsTotal = 0;
	
	/**
	 * Determines state of on screen button
	 *   true = this is a start button
	 *   false = this is a quit button
	 */
	private boolean mButtonIsStart = true;
	
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
	 * Static mInstance of this activity
	 */
	private static MeditationActivity mInstance;
	
    /**
     * Toggled by screen press, indicates whether or not to show buttons/tools on screen
     */
    private boolean mShowingControls = true; 
	
    /**
     * Signal quality as reported by the mindset headset
     * Value 0 - 200 0 is best, 199 is worst, 200 is no connection 
     */
    private int mSigQuality = 200;

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
	 * Timer for updating the UI
	 */
	private static Timer mDataUpdateTimer;	
	
	/**
	 * Class to help in saving received data to file
	 */
	private DataOutHandler mDataOutHandler;	

	/**
	 * Class to help in processing biometeric data
	 */
	private BioDataProcessor mBioDataProcessor = new BioDataProcessor(this);

	private boolean mLoggingEnabled = true;
	private boolean mLogCatEnabled = true;
	private boolean mPaused = true;
	
    private Boolean mBluetoothEnabled = false;
	
	
	// UI Elements
	private Button mToggleLogButton;
    private Button mLlogMarkerButton;
    private ImageButton mPauseButton;
    private TextView mTextInfoView;
    private TextView mTextViewInstructions;
    private TextView mTextBioHarnessView;
    private ImageView mBackgroundImage; 
    private ImageView mForegroundImage; 
    private ImageView mBaseImage; 

    
    private SeekBar mSeekBar;
    private ImageView mSignalImage;    
    private ImageView mCountdownImageView; 
    private TextView mCountdownTextView;
    
    
	private T2HeartRateDetector mHeartRateDetector = new T2HeartRateDetector();
	private T2MovingAverageFilter mGroundLeadFilter = new T2MovingAverageFilter(64);	
	ChebyshevI mEcgBaselineFilter;	
	ChebyshevI mEcgNoiseFilter;	
    
    
    /**
     * Moving average used to smooth the display of the band of interest
     */
    private TMovingAverageFilter mMovingAverage;
    private int mMovingAverageSize = 10;

    private TMovingAverageFilter mMovingAverageROC;
    private int mMovingAverageSizeROC = 6;
    
    float maxMindsetValue = 0;
    float minMindsetValue = 0;
    float AverageMindsetValue = 0;

    /**
     * Gain used to determine how band of interest affects the background image 
     */
    private double mAlphaGain = 1;
    
	protected SharedPreferences sharedPref;
	
	private int mConfiguredGSRRange = ShimmerDevice.GSR_RANGE_HW_RES_3M3;
	
	/**
	 * List of all BioParameters used in this activity
	 */
	private ArrayList<GraphBioParameter> mBioParameters = new ArrayList<GraphBioParameter>();

	/**
	 * List of all currently PAIRED BioSensors
	 */
	private ArrayList<BioSensor> mBioSensors = new ArrayList<BioSensor>();	
	boolean mIsActive = false;
	
	
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
	
	
	MindsetData currentMindsetData;
	ZephyrData currentZephyrData = new ZephyrData();
	
	
	private int mBackgroundControlParameter = MindsetData.THETA_ID; // Default to theta
	private int mForegroundControlParameter = MindsetData.THETA_ID; // Default to theta
	private int numSecsWithoutData = 0;
	
	private String mAudioTrackResourceName;
	private String mBaseImageResourceName;
	
	private static Object mKeysLock = new Object();
    private RateOfChange mRateOfChange;
    private int mRateOfChangeSize = 6;

	int mForegroundRawValue = 0;;     
	double mForegroundScaledValue = 0;;     
	int mForegroundFilteredValue = 0;;     
	
	
    private MediaPlayer mMediaPlayer;
    private ToneGenerator mToneGenerator; 
    
    private boolean mShowForeground;
    private boolean mShowToast;
	/**
	 * Temp variable used in SelectUser() to indicate which user was selected
	 *  Note that this needed to be a member variable because of error: 
	 *  	"Cannot refer to a non-final variable mSelection inside an inner 
	 *      class defined in a different method" 
	 */
	private int mSelection = 0;

	
	boolean mSaveRawWave;
	boolean mAllowComments;
	boolean mShowAGain;
	String[] mBioHarnessParameters;	
	String mLogFileName = "";
	
	private Node mShimmerNode = null;
	
	/**
	 * Node object for shimmer device as returned by spine
	 */
	public Node mSpineNode = null;	
	
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
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		Log.i(TAG, this.getClass().getSimpleName() + ".onCreate()"); 
		
		mInstance = this;
        mRateOfChange = new RateOfChange(mRateOfChangeSize);
		
		mIntroFade = 255;
        
        // We don't want the screen to timeout in this activity
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);		// This needs to happen BEFORE setContentView
        
        
        setContentView(R.layout.buddah_activity_layout);
        
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());   

    	currentMindsetData = new MindsetData(this);
		mSaveRawWave = SharedPref.getBoolean(this, 
				BioZenConstants.PREF_SAVE_RAW_WAVE, 
				BioZenConstants.PREF_SAVE_RAW_WAVE_DEFAULT);
		
		mShowAGain = SharedPref.getBoolean(this, 
				BioZenConstants.PREF_SHOW_A_GAIN, 
				BioZenConstants.PREF_SHOW_A_GAIN_DEFAULT);

		mAllowComments = SharedPref.getBoolean(this, 
				BioZenConstants.PREF_COMMENTS, 
				BioZenConstants.PREF_COMMENTS_DEFAULT);
		
		mShowForeground = SharedPref.getBoolean(this,"show_lotus", true);
		mShowToast = SharedPref.getBoolean(this,"show_toast", true);
		
				
		mAudioTrackResourceName =SharedPref.getString(this, "audio_track" ,"None");
		mBaseImageResourceName =SharedPref.getString(this, "background_images" ,"Sunset");
		
		mBioHarnessParameters = getResources().getStringArray(R.array.bioharness_parameters_array);
		
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);       
        
        String s = SharedPref.getString(this, BioZenConstants.PREF_SESSION_LENGTH, 	"10");  
        mSecondsRemaining = Integer.parseInt(s) * 60;
        mSecondsTotal = mSecondsRemaining; 

		s = SharedPref.getString(this, BioZenConstants.PREF_ALPHA_GAIN, "5");
		mAlphaGain = Float.parseFloat(s);

        
        mMovingAverage = new TMovingAverageFilter(mMovingAverageSize);
        mMovingAverageROC = new TMovingAverageFilter(mMovingAverageSizeROC);
        
        View v1 = findViewById (R.id.buddahView); 
        v1.setOnTouchListener (this);        
        
        Resources resources = this.getResources();
        AssetManager assetManager = resources.getAssets();
        
        // Set up member variables to UI Elements
        mTextInfoView = (TextView) findViewById(R.id.textViewInfo);
        mTextBioHarnessView = (TextView) findViewById(R.id.textViewBioHarness);
        mCountdownTextView = (TextView) findViewById(R.id.countdownTextView);
        mCountdownImageView = (ImageView) findViewById(R.id.imageViewCountdown);  
        
        mPauseButton = (ImageButton) findViewById(R.id.buttonPause);
        mSignalImage = (ImageView) findViewById(R.id.imageView1);  
        mTextViewInstructions = (TextView) findViewById(R.id.textViewInstructions);

        // Note that the seek bar is a debug thing - used only to set the
        // alpha of the buddah image manually for visual testing
        mSeekBar = (SeekBar)findViewById(R.id.seekBar1);
		mSeekBar.setOnSeekBarChangeListener(this);

		// Scale such that values to the right of center are scaled 1 - 10
		// and values to the left of center are scaled 0 = .99999
		if (mAlphaGain > 1.0) {
			mSeekBar.setProgress(50 + (int) (mAlphaGain * 5));      
		}
		else {
			int i = (int) (mAlphaGain * 50);
			mSeekBar.setProgress(i);      
		}
//		mSeekBar.setProgress((int) mAlphaGain * 10);      
		
        // Controls start as invisible, need to touch screen to activate them
		mCountdownTextView.setVisibility(View.INVISIBLE);
		mCountdownImageView.setVisibility(View.INVISIBLE);
		mTextInfoView.setVisibility(View.INVISIBLE);
		mTextBioHarnessView.setVisibility(View.INVISIBLE);		
		mPauseButton.setVisibility(View.INVISIBLE);
		mPauseButton.setVisibility(View.VISIBLE);
		mSeekBar.setVisibility(View.INVISIBLE);
		
        
        mBackgroundImage = (ImageView) findViewById(R.id.buddahView);
        mForegroundImage = (ImageView) findViewById(R.id.lotusView);
        mBaseImage = (ImageView) findViewById(R.id.backgroundView);    
        
        if (!mShowForeground) {
        	mForegroundImage.setVisibility(View.INVISIBLE);
        }
        

        int resource = 0;
		if (mBaseImageResourceName.equalsIgnoreCase("Buddah")) {
	        mBackgroundImage.setImageResource(R.drawable.buddha);
	        mForegroundImage.setImageResource(R.drawable.lotus_flower);
	        mBaseImage.setImageResource(R.drawable.none_bg);	        
		}
		else if (mBaseImageResourceName.equalsIgnoreCase("Bob")) {
	        mBackgroundImage.setImageResource(R.drawable.bigbob);
	        mForegroundImage.setImageResource(R.drawable.red_nose);
	        mBaseImage.setImageResource(R.drawable.none_bg);	        
		}
		else if (mBaseImageResourceName.equalsIgnoreCase("Sunset")) {
	        mBackgroundImage.setImageResource(R.drawable.eeg_layer);
	        mForegroundImage.setImageResource(R.drawable.breathing_rate);
	        mBaseImage.setImageResource(R.drawable.meditation_bg);	        
		}
        
        

		// Initialize SPINE by passing the fileName with the configuration properties
		try {
			mManager = SPINEFactory.createSPINEManager("SPINETestApp.properties", resources);
		} catch (InstantiationException e) {
			Log.e(TAG, "Exception creating SPINE manager: " + e.toString());
			e.printStackTrace();
		}        
		
		// Since Mindset is a static node we have to manually put it in the active node list
		// Note that the sensor id 0xfff1 (-15) is a reserved id for this particular sensor
		Node mindsetNode = null;
		mindsetNode = new Node(new Address("" + Constants.RESERVED_ADDRESS_MINDSET));
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
				
                
		// Create a broadcast receiver. Note that this is used ONLY for command messages from the service
		// All data from the service goes through the mail SPINE mechanism (received(Data data)).
		// See public void received(Data data)
        this.mCommandReceiver = new SpineReceiver(this);
        
		try {
			PackageManager packageManager = this.getPackageManager();
			PackageInfo info = packageManager.getPackageInfo(this.getPackageName(), 0);			
			mApplicationVersion = info.versionName;
			Log.i(TAG, "BioZen Application Version: " + mApplicationVersion + ", Activity Version: " + mActivityVersion);
		} 
		catch (NameNotFoundException e) {
			   	Log.e(TAG, e.toString());
		}
		
		
        // First create GraphBioParameters for each of the ECG static params (ie mindset)		
        int itemId = 0;
        eegPos = itemId; // eeg always comes first
        mBioParameters.clear();
        
        for (itemId = 0; itemId < MindsetData.NUM_BANDS + 2; itemId++) {		// 2 extra, for attention and meditation
        	GraphBioParameter param = new GraphBioParameter(itemId, MindsetData.spectralNames[itemId], "", true);
        	param.isShimmer = false;
            mBioParameters.add(param);
        }
        
        // Now create all of the potential dynamic GBraphBioParameters (GSR, EMG, ECG, HR, Skin Temp, Resp Rate
    	String[] paramNamesStringArray = getResources().getStringArray(R.array.parameter_names);
    	
    	for (String paramName: paramNamesStringArray) {
        	if (paramName.equalsIgnoreCase("not assigned"))
        		continue;

        	if (paramName.equalsIgnoreCase("EEG"))
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
        
		// The session start time will be used as session id
		// Note this also sets session start time
		// **** This session ID will be prepended to all JSON data stored
		//      in the external database until it's changed (by the start
		//		of a new session.
		Calendar cal = Calendar.getInstance();						
		SharedPref.setBioSessionId(sharedPref, cal.getTimeInMillis());		

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US);
		String sessionDate = sdf.format(new Date());    	
		long sessionId = SharedPref.getLong(this, "bio_session_start_time", 0);    	
    	
    	mUserId  = SharedPref.getString(this, "SelectedUser", 	"");
		
		// Now get the database object associated with this user
		
		try {
			mBioUserDao = getHelper().getBioUserDao();
			mBioSessionDao = getHelper().getBioSessionDao();
			
			QueryBuilder<BioUser, Integer> builder = mBioUserDao.queryBuilder();
			builder.where().eq(BioUser.NAME_FIELD_NAME, mUserId);
			builder.limit(1);
//			builder.orderBy(ClickCount.DATE_FIELD_NAME, false).limit(30);
			List<BioUser> list = mBioUserDao.query(builder.prepare());	
			
			if (list.size() == 1) {
				mCurrentBioUser = list.get(0);
			}
			else if (list.size() == 0)
			{
				try {
					mCurrentBioUser = new BioUser(mUserId, System.currentTimeMillis());
					mBioUserDao.create(mCurrentBioUser);
				} catch (SQLException e1) {
					Log.e(TAG, "Error creating user " + mUserId , e1);
				}		
			}
			else {
				Log.e(TAG, "General Database error" + mUserId);
			}
			
		} catch (SQLException e) {
			Log.e(TAG, "Can't find user: " + mUserId , e);

		}
		
		mSignalImage.setImageResource(R.drawable.signal_bars0);

		// Check to see of there a device configured for EEG, if so then show the skin conductance meter
		String tmp = SharedPref.getString(this, "EEG" ,null);		
		
		if (tmp != null) {
			mSignalImage.setVisibility(View.VISIBLE);
    		mTextViewInstructions.setVisibility(View.VISIBLE);
			
		}
		else {
			mSignalImage.setVisibility(View.INVISIBLE);
    		mTextViewInstructions.setVisibility(View.INVISIBLE);
		}
		
		
		mDataOutHandler = new DataOutHandler(this, mUserId,sessionDate, mAppId, DataOutHandler.DATA_TYPE_EXTERNAL_SENSOR, sessionId );
		
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

			DataOutPacket packet = new DataOutPacket();
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
		
    	
		if (mInternalSensorMonitoring) {
	        // IntentSender Launches our service scheduled with with the alarm manager 
	        mBigBrotherService = PendingIntent.getService(MeditationActivity.this,
	                0, new Intent(MeditationActivity.this, BigBrotherService.class), 0);        
			
	        
            long firstTime = SystemClock.elapsedRealtime();	        
            // Schedule the alarm!
            AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
            am.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                            firstTime, mPollingPeriod * 1000, mBigBrotherService);

            // Tell the user about what we did.
            Toast.makeText(MeditationActivity.this, R.string.service_scheduled,
                    Toast.LENGTH_LONG).show();		    	
	        
		}		
		
    } // End onCreate(Bundle savedInstanceState)
    
    @Override
	public void onBackPressed() {
    	handlePause("Session Complete"); // Allow opportinuty for a note
	}

    @Override
	protected void onDestroy() {
    	super.onDestroy();
    	
		Log.i(TAG, this.getClass().getSimpleName() + ".onDestroy()"); 
		
		if (mInternalSensorMonitoring) {
            // And cancel the alarm.
			AlarmManager am = (AlarmManager)getSystemService(ALARM_SERVICE);
            am.cancel(mBigBrotherService);

            
			Intent intent = new Intent();
			intent.setAction(BigBrotherConstants.ACTION_COMMAND_BROADCAST);
			intent.putExtra("message", BigBrotherConstants.SERVICE_OFF);
			sendBroadcast(intent);
            
            
            // Tell the user about what we did.
            Toast.makeText(MeditationActivity.this, R.string.service_unscheduled,
                    Toast.LENGTH_LONG).show();		
		}   
		
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
					
					Log.i(TAG, String.format("Setting up Shimmer sensor: %s (%s) (%d) SHIMMER_COMMAND_STOPPED", 
							param.shimmerNode.getPhysicalID(), deviceAddress, param.shimmerSensorConstant));
					mManager.setup(param.shimmerNode, setup);
				}
			}
		}		
		
    	if (mMediaPlayer != null) {
    		mMediaPlayer.stop();
    		mMediaPlayer.release();
    		mMediaPlayer = null;
    	}
    	
    	this.unregisterReceiver(this.mCommandReceiver);
    	
		mDataOutHandler.close();
    	
	} // End onDestroy()
    
	@Override
	protected void onStart() {
		super.onStart();
		mIsActive = true;
		Log.i(TAG, this.getClass().getSimpleName() + ".onStart()"); 
		
		
//		mPauseButton.setText("Start");
		mPauseButton.setImageResource(R.drawable.start); 		    		
		
		// Set up filter intents so we can receive broadcasts
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.t2.biofeedback.service.status.BROADCAST");
		this.registerReceiver(this.mCommandReceiver,filter);
		
		// Set up a timer to do graphical updates
		mDataUpdateTimer = new Timer();
		mDataUpdateTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				TimerMethod();
			}

		}, 0, 10);		
		

		if (mMediaPlayer != null) {
	        mMediaPlayer.stop();
		}

		int resource = 0;
		if (mAudioTrackResourceName.contains("Air Synth")) resource = R.raw.dave_luxton_air_synth_meditation; 
		if (mAudioTrackResourceName.contains("Entity and Echo")) resource = R.raw.dave_luxton_entity_and_echo_meditation; 
		if (mAudioTrackResourceName.contains("Starlit Lake")) resource = R.raw.dave_luxton_starlit_lake_meditation; 
		
		if (resource != 0) {
			mMediaPlayer = MediaPlayer.create(this, resource);
			if (mMediaPlayer != null) {
		        mMediaPlayer.start();
		        mMediaPlayer.setLooping(true);
			}
			
		}
		
		if (mAntHrmEnabled) {
	        mAntServiceBound = bindService(new Intent(this, ANTPlusService.class), mConnection, BIND_AUTO_CREATE);
		}		
	}
    
	/**
	 * Convert seconds to string display of hours:minutes:seconds 
	 * @param time Total number of seconds to display
	 * @return String formated to hours:minutes:seconds
	 */
	String secsToHMS(long time) {
		
		long secs = time;
		long hours = secs / 3600;
		secs = secs % 3600;
		long mins = secs / 60;
		secs = secs % 60;
		
		return hours + ":" + mins + ":" + secs;
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	//	this.getMenuInflater().inflate(R.menu.menu_compassion_meditation, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.settings:
			Intent intent2 = new Intent(this, BTServiceManager.class);
			this.startActivity(intent2);			
			return true;
			
		case R.id.discover:
			mManager.discoveryWsn();

			return true;
			
		case R.id.about:
			String content = "National Center for Telehealth and Technology (T2)\n\n";
			content += "BioZen Application\n";
			content += "Application Version: " + mApplicationVersion + "\n";
			content += "Activity Version: " + mActivityVersion;
			
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			
			alert.setTitle("About");
			alert.setMessage(content);	
			alert.show();			
			return true;
			
		default:
			return super.onOptionsItemSelected(item);
		}
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
			Log.i(TAG, "Received command : " + bfs.messageId + " to "  + name );
			if (mShowToast) 
				Toast.makeText (getApplicationContext(), "**** Connecting to Sensor Node ****", Toast.LENGTH_SHORT).show ();
		} 
		else if(bfs.messageId.equals("CONN_ANY_CONNECTED")) {
			Log.i(TAG, "Received command : " + bfs.messageId + " to "  + name );
			// Something has connected - discover what it was
			mManager.discoveryWsn();
			if (mShowToast) 
				Toast.makeText (getApplicationContext(), "**** Sensor Node Connected ****", Toast.LENGTH_SHORT).show ();
		} 
		else if(bfs.messageId.equals("CONN_CONNECTION_LOST")) {
			Log.i(TAG, "Received command : " + bfs.messageId + " to "  + name );		
			if (mShowToast) 
				Toast.makeText (getApplicationContext(), "**** Sensor Node Connection lost ****", Toast.LENGTH_SHORT).show ();
		}
		else if(bfs.messageId.equals("STATUS_PAIRED_DEVICES")) {
			Log.i(TAG, "Received command : " + bfs.messageId + " to "  + name );
			Log.i(TAG, bfs.address );
			
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
//						byte startShimmercommand = ShimmerNonSpineSetupSensor.SHIMMER_COMMAND_RUNNING_4HZ_3M3;
						byte startShimmercommand = ShimmerNonSpineSetupSensor.SHIMMER_COMMAND_RUNNING_4HZ_AUTORANGE;
						setup.setCommand(startShimmercommand);
						
						
						mConfiguredGSRRange = Util.getGsrRangeFromShimmerCommand(startShimmercommand);				
											
						
						Log.i(TAG, String.format("Setting up Shimmer sensor: %s (%s) (%d) SHIMMER_COMMAND_RUNNING", 
								param.shimmerNode.getPhysicalID(), deviceAddress, param.shimmerSensorConstant));
						mManager.setup(param.shimmerNode, setup);
						
					}
					else {

					}
				}
			}
		}		
	}

	@Override
	public void newNodeDiscovered(Node newNode) {
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
					mBioParameters.get(eHealthAirFlowPos).setScaledValue((int)((double)  map(airFlow,0,360,0,255) * mAlphaGain));
					
					mBioParameters.get(eHealthTempPos).rawValue = (int) temp;
					mBioParameters.get(eHealthTempPos).setScaledValue((int)((double)  map(temp,29,40,0,255) * mAlphaGain));
					
					mBioParameters.get(eHealthHeartRatePos).rawValue = BPM;
					mBioParameters.get(eHealthHeartRatePos).setScaledValue((int)((double)  map(BPM,30,220,0,255) * mAlphaGain));
					
					mBioParameters.get(eHealthSpO2Pos).rawValue = SPO2;
					mBioParameters.get(eHealthSpO2Pos).setScaledValue((int)((double)  map(SPO2,0,100,0,255) * mAlphaGain));
					
					mBioParameters.get(eHealthGSRPos).rawValue = (int) map(scaledConductance,0,65535,0,100);
					mBioParameters.get(eHealthGSRPos).setScaledValue((int)((double)  map(scaledConductance,0,65535,0,255) * mAlphaGain));
				
					DataOutPacket packet = new DataOutPacket();
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
					DataOutPacket packet = new DataOutPacket();
					packet.add(DataOutHandlerTags.RAW_HEARTRATE, thisData.getBPM());
					try {
						mDataOutHandler.handleDataOut(packet);
					} catch (DataOutHandlerException e) {
						Log.e(TAG, e.toString());
						e.printStackTrace();
					}					

				}
				
				break;
			}			
			case SPINEFunctionConstants.SHIMMER: {
				Node node = data.getNode();
				numSecsWithoutData = 0;		
				
				Node source = data.getNode();
				ShimmerData shimmerData = (ShimmerData) data;
				
				switch (shimmerData.sensorCode) {
				case SPINESensorConstants.SHIMMER_GSR_SENSOR:
					
					mBioDataProcessor.processShimmerGSRData(shimmerData, mConfiguredGSRRange);
					
					double scaled  = MathExtra.scaleData((float)mBioDataProcessor.mGsrResistance, 2000000F, 0F, 255) * mAlphaGain;
    	        	synchronized(mKeysLock) {
    	        		mBioParameters.get(gsrPos).rawValue = (int) scaled;
    	        		mBioParameters.get(gsrPos).setScaledValue((int) scaled);
    	        	}
					break;
					
				case SPINESensorConstants.SHIMMER_EMG_SENSOR:
					
					mBioDataProcessor.processShimmerEMGData(shimmerData); 

    				scaled  = MathExtra.scaleData((float)shimmerData.emg, 4000F, 0F, 255) * mAlphaGain;
    	        	synchronized(mKeysLock) {
    	        		mBioParameters.get(emgPos).rawValue = (int) scaled;
    	        		mBioParameters.get(emgPos).setScaledValue((int) scaled);
    	        	}
					break;
				case SPINESensorConstants.SHIMMER_ECG_SENSOR:
					// If we're receiving packets from shimmer egg then swith the heartrate to shimmer
					// Otherwise we'll leave it at the default which is zephyr
					mHeartRateSource = HEARTRATE_SHIMMER;		
					
					mBioDataProcessor.processShimmerECGData(shimmerData); 						
					
    				scaled  = MathExtra.scaleData((float)shimmerData.ecg, 4000F, 0F, 255) * mAlphaGain;
    	        	synchronized(mKeysLock) {
    	        		mBioParameters.get(ecgPos).rawValue = (int) scaled;
    	        		mBioParameters.get(ecgPos).setScaledValue((int) scaled);
    	        	}
					break;

				} // End switch (shimmerData.sensorCode)

				break;
			}			

			case SPINEFunctionConstants.ZEPHYR: {
				
				mBioDataProcessor.processZephyrData(data);	
				
				Node source = data.getNode();
				Feature[] feats = ((FeatureData)data).getFeatures();
				Feature firsFeat = feats[0];

				currentZephyrData.heartRate = mBioDataProcessor.mZephyrHeartRate;
				currentZephyrData.respRate = (int) mBioDataProcessor.mRespRate;
				currentZephyrData.skinTemp = (int) mBioDataProcessor.mSkinTempF;
				
	        	synchronized(mKeysLock) {				
    				double scaled  = MathExtra.scaleData((float)mBioDataProcessor.mSkinTempF, 110F, 70F, 255) * mAlphaGain;
					mBioParameters.get(skinTempPos).rawValue = (int) mBioDataProcessor.mSkinTempF;
					mBioParameters.get(skinTempPos).setScaledValue((int) scaled);
					
    				scaled = MathExtra.scaleData((float)mBioDataProcessor.mZephyrHeartRate, 250F, 20F, 255) * mAlphaGain;
					mBioParameters.get(heartRatePos).rawValue = mBioDataProcessor.mZephyrHeartRate;
					mBioParameters.get(heartRatePos).setScaledValue((int) scaled);

    				scaled = MathExtra.scaleData((float)mBioDataProcessor.mRespRate, 120F, 5F, 255) * mAlphaGain;					
					mBioParameters.get(respRatePos).rawValue = (int) mBioDataProcessor.mRespRate;
					mBioParameters.get(respRatePos).setScaledValue((int) scaled);
	        	}				
				
				numSecsWithoutData = 0;		

				break;
			} // End case SPINEFunctionConstants.ZEPHYR:			
			

			case SPINEFunctionConstants.MINDSET: {
					Node source = data.getNode();
				
					MindsetData mindsetData = (MindsetData) data;
					
					mBioDataProcessor.processMindsetData(data, currentMindsetData);					
					
					if (mindsetData.exeCode == Constants.EXECODE_POOR_SIG_QUALITY) {
						
			        	synchronized(mKeysLock) {	
			        		currentMindsetData.poorSignalStrength = mindsetData.poorSignalStrength;
			        	}

						mSigQuality = mindsetData.poorSignalStrength & 0xff;

						if (mShowingControls || mSigQuality == 200)
							mSignalImage.setVisibility(View.VISIBLE);
						else
							mSignalImage.setVisibility(View.INVISIBLE);
						
						if (mSigQuality == 200)
							mSignalImage.setImageResource(R.drawable.signal_bars0);
						else if (mSigQuality > 150)
							mSignalImage.setImageResource(R.drawable.signal_bars1);
						else if (mSigQuality > 100)
							mSignalImage.setImageResource(R.drawable.signal_bars2);
						else if (mSigQuality > 50)
							mSignalImage.setImageResource(R.drawable.signal_bars3);
						else if (mSigQuality > 25)
							mSignalImage.setImageResource(R.drawable.signal_bars4);
						else 
							mSignalImage.setImageResource(R.drawable.signal_bars5);

						if (mSigQuality == 200 && mPrevSigQuality != 200) {
							Toast.makeText (getApplicationContext(), "Headset not makeing good skin contact. Please Adjust", Toast.LENGTH_LONG).show ();
						}
						mPrevSigQuality = mSigQuality;						
						
					}
					
					if (mindsetData.exeCode == Constants.EXECODE_SPECTRAL || mindsetData.exeCode == Constants.EXECODE_RAW_ACCUM) {

						synchronized(mKeysLock) {	
							if (mPaused == false) {
								numSecsWithoutData = 0;				

								synchronized(mKeysLock) {				
							        for (int i = 0; i < MindsetData.NUM_BANDS + 2; i++) {		// 2 extra, for attention and meditation
//							        	float scaled = MathExtra.scaleData((float)currentMindsetData.getFeatureValue(i), 100F, 20F, 255, (float)mAlphaGain);
							        	float scaled = MathExtra.scaleData((float)currentMindsetData.getFeatureValue(i), 100F, 20F, 255);
							        	scaled *= mAlphaGain;
							        	
							        	if (scaled > 255)
							        		scaled = 255;
							        	
							        	mBioParameters.get(i).rawValue = currentMindsetData.getFeatureValue(i);
										mBioParameters.get(i).setScaledValue((int) scaled);							        	
							        }
					        	}									
							} // End if (mPaused == false)
			        	}
					}
					break;
				} // End case SPINEFunctionConstants.MINDSET:
			} // End switch (data.getFunctionCode())
		} // End if (data != null)
	}
	
	@Override
	public void discoveryCompleted(Vector activeNodes) {
		Log.d(TAG, this.getClass().getSimpleName() + ".discoveryCompleted()");

		// Tell the bluetooth service to send us a list of bluetooth devices and system status
		// Response comes in public void onStatusReceived(BioFeedbackStatus bfs) STATUS_PAIRED_DEVICES
		mManager.pollBluetoothDevices();			
	}

	/**
	 * Converts a byte array to an integer
	 * @param bytes		Bytes to convert
	 * @return			Integer representaion of byte array
	 */
	public static int byteArrayToInt(byte[] bytes) {
		int val = 0;
		
		for(int i = 0; i < bytes.length; i++) {
			int n = (bytes[i] < 0 ? (int)bytes[i] + 256 : (int)bytes[i]) << (8 * i);
			val += n;
		}
		
		return val;
	}
	
	/**
	 * Hansles UI button clicks
	 * @param v
	 */
	public void onButtonClick(View v)
	{
		 final int id = v.getId();
		    switch (id) {
		    case R.id.buttonBack:
		    	finish();
		    	break;
		    		    
		    case R.id.buttonPause:
		    	if (mPaused) {
		    		mPaused = false;
		    		mPauseButton.setImageResource(R.drawable.quit); 		    		
		    		mTextViewInstructions.setVisibility(View.INVISIBLE);
		    		toggleControls();		    		
		    		Toast.makeText(mInstance, "You may toggle the screen controls back \non by pressing anywhere on the screen", Toast.LENGTH_SHORT).show();
		    		
		    	}
		    	else {
		    		handlePause(mUserId + mSessionId + " Paused");
		    	}
		    	break;
		        
		    } // End switch		
	}
	
	/**
	 * This method is called directly by the timer and runs in the same thread as the timer
	 * From here We call the method that will work with the UI through the runOnUiThread method.
	 */
	private void TimerMethod()
	{
		this.runOnUiThread(Timer_Tick);
	}

	/**
	 * This method runs in the same thread as the UI.
	 */
	private Runnable Timer_Tick = new Runnable() {
		public void run() {

			// We get here every .01 second
			if (mPaused == true || currentMindsetData == null || currentZephyrData == null) {
				return;
			}

			if (mSubTimerClick-- > 0) {
				if (mIntroFade > 0) {

					mBackgroundImage.setAlpha(mIntroFade--);
					mForegroundImage.setAlpha(mIntroFade--);
					
				}
				return;
			}
			else {
				mSubTimerClick = 100;
				
			}
			
			// We get here every 1 second
			numSecsWithoutData++;
			
			if (mLoggingEnabled == true && numSecsWithoutData < 2) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
				String currentDateTimeString = DateFormat.getDateInstance().format(new Date());				
				currentDateTimeString = sdf.format(new Date());
				
				String logData = currentDateTimeString + ", " + currentZephyrData.getLogDataLine();
				logData += currentMindsetData.getLogDataLine(currentMindsetData.exeCode, mSaveRawWave) + "\n";
				

			}			


			// Background parameters
			// NOTE that this is a huge hack (and needs to be fixed.
			// mBackgroundControlParameter (which comes from preferences, is based on the list R.array.bands_of_interest_array
			// which FOR NOW, matches the assignments here when assigning mBioParameters itemID's.
			// These all must be changed to use the same list
			// To further complicate the matter mBioParameters itemID's are assigned based on MindsetData.NUM_BANDS
			// AND R.array.parameter_names
			GraphBioParameter backgroundParam = mBioParameters.get(mBackgroundControlParameter);
			String backgroundLogText = String.format("Background: %s (raw, scaled, filtered): %d, %d, %d", backgroundParam.title1, backgroundParam.rawValue, backgroundParam.scaledValue, backgroundParam.getFilteredScaledValue());
			mTextInfoView.setText(backgroundLogText);		
			Log.d(TAG, backgroundLogText);

			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
			String currentDateTimeString = DateFormat.getDateInstance().format(new Date());				
			currentDateTimeString = sdf.format(new Date());
	    	try {
				mDataOutHandler.logNote(currentDateTimeString + ", " + currentDateTimeString);
			} catch (DataOutHandlerException e) {
				Log.e(TAG, e.toString());
				e.printStackTrace();
			}			
			

			// Foreground parameters
			GraphBioParameter foregroundParam = mBioParameters.get(mForegroundControlParameter);			
    		mForegroundRawValue = foregroundParam.rawValue;   
//    		int iBackgroundAlphaValue = (int) ((double) backgroundParam.getFilteredScaledValue() * mAlphaGain);
    		int iBackgroundAlphaValue = backgroundParam.getFilteredScaledValue();
			
    		// We want to update the rate of change once every second
    		foregroundParam.updateRateOfChange();
			int iForegroundAlphaValue = 255 - foregroundParam.getRateOfChangeScaledValue();
			String foregroundLogText = String.format("Foreground: %s (scaled, ROC, ALPHA): %d, %d %d", foregroundParam.title1, backgroundParam.getFilteredScaledValue(), foregroundParam.getRateOfChangeScaledValue(), iForegroundAlphaValue);
			mTextBioHarnessView.setText(foregroundLogText);		
//			Log.d(TAG, foregroundLogText);
			
			
			if (mIntroFade <= 0) {
				mBackgroundImage.setAlpha(iBackgroundAlphaValue);
				
				if (mShowForeground) {
					mForegroundImage.setAlpha(iForegroundAlphaValue);
				}
				else {
					mForegroundImage.setAlpha(0);
				}
			}
			
			if (mSecondsRemaining-- > 0) {
				mCountdownTextView.setText(secsToHMS(mSecondsRemaining));	
			}
			else {
				if (mMediaPlayer != null) {
			        mMediaPlayer.stop();
				}
				
				mMediaPlayer = MediaPlayer.create(mInstance, R.raw.wind_chime_1);
				if (mMediaPlayer != null) {
			        mMediaPlayer.start();
			        mMediaPlayer.setLooping(true);			        
				}

		    	handlePause("Session Complete"); // Allow opportinuty for a note
			}
		}
	};

	@Override
	protected void onPause() {
		Log.i(TAG, this.getClass().getSimpleName() + ".onPause()"); 
		
		mIsActive = false;
		
		// *******************
    	// Make sure to to this or else you will get more and more notifications from Spine as you 
    	// go into and out of activities!
		// Also make sure to do this in on pause (as opposed to onStop or ondestroy.
		// This will prevent you from receiving messages possibly requested by another activity    	
		mManager.removeListener(this);	

		
		mDataUpdateTimer.purge();
    	mDataUpdateTimer.cancel();
    	currentMindsetData.saveScaleData();	


    	
    	

		super.onPause();
	}

	@Override
	protected void onStop() {
		Log.i(TAG, this.getClass().getSimpleName() + ".onStop()"); 
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
				
		super.onStop();
	}	
	
	@Override
	protected void onRestart() {
		Log.i(TAG, this.getClass().getSimpleName() + ".onRestart()"); 
		
		super.onRestart();
	}

	@Override
	protected void onResume() {
		Log.i(TAG, this.getClass().getSimpleName() + ".onResume()"); 
		
		restoreState();
		
		// ... then we need to register a SPINEListener implementation to the SPINE manager mInstance
		// to receive sensor node data from the Spine server
		// (I register myself since I'm a SPINEListener implementation!)
		mManager.addListener(this);	   
		
		mManager.discoveryWsn(); // discoveryCompleted() is called after this is done
		
		
		super.onResume();
	}


	void restoreState()
	{
	    String s = SharedPref.getString(this, BioZenConstants.PREF_BAND_OF_INTEREST ,"0");
        mBackgroundControlParameter = Integer.parseInt(s);
		
		
		s = SharedPref.getString(this, 
				BioZenConstants.PREF_BIOHARNESS_PARAMETER_OF_INTEREST ,
				BioZenConstants.PREF_BIOHARNESS_PARAMETER_OF_INTEREST_DEFAULT);

		mForegroundControlParameter = Integer.parseInt(s); 

	}

	void toggleControls() {
		// Toggle showing screen buttons/controls
		if (mShowingControls) {
			mShowingControls = false;
			mCountdownImageView.setVisibility(View.INVISIBLE);
			mCountdownTextView.setVisibility(View.INVISIBLE);
			mTextInfoView.setVisibility(View.INVISIBLE);
			mTextBioHarnessView.setVisibility(View.INVISIBLE);
			mPauseButton.setVisibility(View.INVISIBLE);
			mSeekBar.setVisibility(View.INVISIBLE);
			
		}
		else {
			mShowingControls = true;
			mCountdownImageView.setVisibility(View.VISIBLE);
			mCountdownTextView.setVisibility(View.VISIBLE);
			if (mDebug) mTextInfoView.setVisibility(View.VISIBLE);
			if (mDebug) mTextBioHarnessView.setVisibility(View.VISIBLE);
			mPauseButton.setVisibility(View.VISIBLE);
			mSeekBar.setVisibility(mShowAGain ? View.VISIBLE :View.INVISIBLE);

		}		
	}
	
	@Override
	public boolean onTouch(View arg0, MotionEvent arg1) {
		if (!mPaused) toggleControls();

		return false;
	}

	@Override
	public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
//		mAlphaGain = arg1/10;
//		if (mAlphaGain <= 0) mAlphaGain = 1;
		
		// Scale such that values to the right of center are scaled 1 - 10
		// and values to the left of center are scaled 0 = .99999
		if (arg1 > 50) {
			float farg1 = (float) arg1;
			mAlphaGain = (farg1 - 50) / 5;
		}
		else {
			mAlphaGain = (float) arg1 / (float) 50;
		}
		
		
		
	}

	@Override
	public void onStartTrackingTouch(SeekBar arg0) {
	}


	@Override
	public void onStopTrackingTouch(SeekBar arg0) {
		SharedPref.putString(this,
				BioZenConstants.PREF_ALPHA_GAIN, new Float(mAlphaGain).toString() );
		
	    Toast.makeText(this, " AlphaGain changed to " + mAlphaGain, Toast.LENGTH_SHORT).show();
		
	}
	


	/**
	 * Handles the pause button press
	 *   Brings up a dialog that allows the user to either restart, or quit
	 *   Note that in any case the text entered by the user is saved to the log file
	 */
	public void handlePause(String message) {
		
			mPaused = true;

//			if (mMediaPlayer != null) {
//				mMediaPlayer.pause();
//			}

			Intent intent1 = new Intent(mInstance, EndSessionActivity.class);
			mInstance.startActivityForResult(intent1, BioZenConstants.END_SESSION_ACTIVITY);		
	}

	/**
	 * Writes a specific note to the log - adding a time stamp
	 * @param note Note to save to log
	 */
	void addNoteToLog(String note) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		
		String currentDateTimeString = DateFormat.getDateInstance().format(new Date());				
		currentDateTimeString = sdf.format(new Date());
    	try {
			mDataOutHandler.logNote(currentDateTimeString + ", " + note + "\n");
		} catch (DataOutHandlerException e) {
			Log.e(TAG, e.toString());
			e.printStackTrace();
		}                	
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		switch(requestCode) {
			case BioZenConstants.END_SESSION_ACTIVITY:
				 if (data != null) {
					int action = data.getIntExtra(
							BioZenConstants.END_SESSION_ACTIVITY_RESULT,
							BioZenConstants.END_SESSION_RESTART);
					
					
					switch (action) {
					
					default:
					case BioZenConstants.END_SESSION_RESTART:
						break;

					case BioZenConstants.END_SESSION_SAVE:
						EndAndSaveSession(data);
						break;

					case BioZenConstants.END_SESSION_QUIT:
						if (mLogFile != null)
							mLogFile.delete();
						Analytics.onEndSession(this);						
						finish();					
						break;
					}					 
				 }
				 else {
						if (mLogFile != null)
							mLogFile.delete();
						Analytics.onEndSession(this);						
						finish();					
				 }

				break;
		}
	}
	
	void EndAndSaveSession(Intent data) {

		String notes = "";
		String categoryName = "";
		 if (data != null) {
				notes = data.getStringExtra(
						BioZenConstants.END_SESSION_ACTIVITY_NOTES);

				categoryName = data.getStringExtra(
						BioZenConstants.END_SESSION_ACTIVITY_CATEGORY);
				
				if (categoryName == null) categoryName = "";
				if (notes == null) notes = "";
		 }
		
		
		addNoteToLog(notes);
		
		// -----------------------------
		// Save stats for session
		// -----------------------------
		// Create a session data point for this session (to put in data
		mCurrentBioSession = new BioSession(mCurrentBioUser, System.currentTimeMillis());
		if (mCurrentBioSession != null) {
			mCurrentBioSession.comments += notes;
			mCurrentBioSession.category = categoryName;

	        for (int i = 0; i < BioZenConstants.MAX_KEY_ITEMS; i++) {		
	        	mCurrentBioSession.maxFilteredValue[i] = mBioParameters.get(i).getMaxFilteredValue();
	        	mCurrentBioSession.minFilteredValue[i] = 
	        		mBioParameters.get(i).getMinFilteredValue() != 9999 ? mBioParameters.get(i).getMinFilteredValue() : 0;
	        	mCurrentBioSession.avgFilteredValue[i] = mBioParameters.get(i).getAvgFilteredValue();
	        	mCurrentBioSession.keyItemNames[i] = mBioParameters.get(i).title1;
	        }
	        
	        int secondsCompleted =  mSecondsTotal -  mSecondsRemaining;
	        float precentComplete = (float) secondsCompleted / (float) mSecondsTotal;
	        mCurrentBioSession.precentComplete = (int) (precentComplete * 100);
	        mCurrentBioSession.secondsCompleted = secondsCompleted;
	        mCurrentBioSession.logFileName = mLogFileName; 
	        
	        mCurrentBioSession.mindsetBandOfInterestIndex = mBackgroundControlParameter;
	        mCurrentBioSession.bioHarnessParameterOfInterestIndex = mForegroundControlParameter;
	        

	        // Udpate the database with the current session
			try {
				mBioSessionDao.create(mCurrentBioSession);
			} catch (SQLException e1) {
				Log.e(TAG, "Error saving current session to database", e1);
			}			
			
		}
		

		
		finish();
		
		
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
					
					Log.i(TAG, "Adding sensor " + name + ", " + address + (enabled ? ", enabled":", disabled") + " : " + Util.connectionStatusToString(connectionStatus));
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


	@Override
	public void errorCallback() {
		// TODO Auto-generated method stub
		
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
	            mAntManager.setCallbacks(MeditationActivity.this);
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