package com.t2.compassionMeditation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import spine.SPINEFactory;
import spine.SPINEListener;
import spine.SPINEManager;
import spine.datamodel.Data;
import spine.datamodel.Node;
import spine.datamodel.ServiceMessage;
import spine.datamodel.functions.SpineServiceCommand;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Messenger;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.t2.SpineReceiver;
import com.t2.SpineReceiver.BioFeedbackStatus;
import com.t2.SpineReceiver.OnBioFeedbackMessageRecievedListener;
import com.t2.biofeedback.BioFeedbackService;
import com.t2.biofeedback.R;
import com.t2.compassionUtils.BioSensor;
import com.t2.compassionUtils.Util;

/**
 * Activity used to manually administer list of available devices.
 * This is instantiated in two conditions
 *  1. When the user starts the service manually (without, or more accurately before the Spine server).
 *  2. When the user selects "Manage Devices" from the Spine Server main menu.
 *  
 * @author scott.coleman
 *
 */
public class DeviceManagerActivity extends Activity 
	implements OnClickListener, SPINEListener, OnBioFeedbackMessageRecievedListener {
	private static final String TAG = "BFDemo";

	private static final int BLUETOOTH_SETTINGS = 987;
	
	private ListView deviceList;
	private ManagerItemAdapter deviceListAdapter;

	private AlertDialog bluetoothDisabledDialog;
    ArrayList<Messenger> mServerListeners = new ArrayList<Messenger>();
	String mVersionName = "";    
	Boolean mBluetoothEnabled;
	
	protected SharedPreferences sharedPref;
	
	
	/**
	 * List of all currently PAIRED BioSensors
	 */
	private ArrayList<BioSensor> mBioSensors = new ArrayList<BioSensor>();
	
	
    /**
     * The Spine manager contains the bulk of the Spine server. 
     */
    private static SPINEManager mSpineManager;
    
    /**
	 * This is a broadcast receiver. Note that this is used ONLY for command/status messages from the AndroidBTService
	 * All data from the service goes through the mail SPINE mechanism (received(Data data)).
	 */
	private SpineReceiver mCommandReceiver;        
    
    private DeviceManagerActivity mInstance;
    
	String[] measureNames = {"OPne", "two"};
	boolean toggleArray[] = new boolean[2]; 

	AlertDialog mParamChooserAlert;
	
	
	/**
	 * Handles UI button clicks
	 * @param v
	 */
	public void onButtonClick(final View v)
	{
		 final int id = v.getId();
		    switch (id) {
		    
		    // Present a list of all parameter types to allow the user
		    // to associate a specific device (address) to a specific parameter
		    // This association will be used in the main activities so that they
		    // know the BT address of particular sensors to tell them to start/stop
		    
		    // The associations are stored in Shared pref
		    //	key = param name (See R.array.parameter_names)
		    //	value = BT address
		    
		    case R.id.parameter:
		    	final String address = (String)v.getTag();	
			    AlertDialog.Builder alertParamChooser = new AlertDialog.Builder(mInstance);
			    alertParamChooser.setTitle("Select parameter for this device");
//			    mParamChooserAlert = alertParamChooser.create();
			    
			    
			    alertParamChooser.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int id) {
			    }
			    });				    
			    alertParamChooser.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int id) {
			    }
			    });				    
			    alertParamChooser.setSingleChoiceItems(R.array.parameter_names,-1, new DialogInterface.OnClickListener()
			    {
			        @Override
			        public void onClick(DialogInterface dialog, final int which) {
			        	String[] paramNamesStringArray = getResources().getStringArray(R.array.parameter_names);
			        	final String name = paramNamesStringArray[which];
			        	
			        	// See which pamrameter is associated with this name
			        	String device = SharedPref.getDeviceForParam(mInstance, name);			        	
			        	if (device != null && which != 0) {
							AlertDialog.Builder alertWarning = new AlertDialog.Builder(mInstance);
							String message = String.format("Another sensor (%s) currently feeds this parameter. Please change this previous mapping before trying again", device );
							alertWarning.setMessage(message);


							alertWarning.setPositiveButton("OK", new DialogInterface.OnClickListener() {
								  public void onClick(DialogInterface dialog, int whichButton) {
									  mParamChooserAlert.dismiss();
								  }

								});
							alertWarning.setNegativeButton("Ignore", new DialogInterface.OnClickListener() {
								  public void onClick(DialogInterface dialog, int whichButton) {
							        	Log.d(TAG, "param name = " + name + ", address = " + address);
							        	if (which != 0) {
							        		SharedPref.setParamForDevice(mInstance, name, address);
									    	v.getBackground().setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);		    	
											((Button)v.findViewById(R.id.parameter)).setText(name);
							        	}
							        	else {
							        		SharedPref.setParamForDevice(mInstance, name, address);
									    	v.getBackground().setColorFilter(Color.LTGRAY, PorterDuff.Mode.MULTIPLY);
											((Button)v.findViewById(R.id.parameter)).setText("Parameter");
									    	
							        	}
								  
								  
								  
								  
								  }

								});

							alertWarning.show();
			        		
			        		
			        	}
			        	else {
			        		// The parameter is currently not mapped to a sensor, so we can go ahead and map it
				        	Log.d(TAG, "param name = " + name + ", address = " + address);
				        	if (which != 0) {
				        		SharedPref.setParamForDevice(mInstance, name, address);
						    	v.getBackground().setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);		    	
								((Button)v.findViewById(R.id.parameter)).setText(name);
				        	}
				        	else {
				        		SharedPref.setParamForDevice(mInstance, name, address);
						    	v.getBackground().setColorFilter(Color.LTGRAY, PorterDuff.Mode.MULTIPLY);
								((Button)v.findViewById(R.id.parameter)).setText("Parameter");
						    	
				        	}
			        	}
			        }
			    });	
//				alert.show();	
			    mParamChooserAlert = alertParamChooser.create();
			    
			    mParamChooserAlert.show();
		    	
		    	break;
		    		    
		    }
	}
	
	
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mInstance = this;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());   
		

		
		//this.sendBroadcast(new Intent(BioFeedbackService.ACTION_SERVICE_START));
		
		this.setContentView(R.layout.device_manager_activity_layout);

		this.findViewById(R.id.bluetoothSettingsButton).setOnClickListener(this);
		

        Resources resources = this.getResources();
        AssetManager assetManager = resources.getAssets();        
		try {
			mSpineManager = SPINEFactory.createSPINEManager("SPINETestApp.properties", resources);
		} catch (InstantiationException e) {
			Log.e(TAG, this.getClass().getSimpleName() + " Exception creating SPINE manager: " + e.toString());
			e.printStackTrace();
		}        
		// ... then we need to register a SPINEListener implementation to the SPINE manager instance
		// to receive sensor node data from the Spine server
		// (I register myself since I'm a SPINEListener implementation!)
		mSpineManager.addListener(this);			
		
		// Create a broadcast receiver. Note that this is used ONLY for command messages from the service
		// All data from the service goes through the mail SPINE mechanism (received(Data data)).
		// See public void received(Data data)
        this.mCommandReceiver = new SpineReceiver(this);    
		// Set up filter intents so we can receive broadcasts
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.t2.biofeedback.service.status.BROADCAST");
		filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
		filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
		filter.addAction(BioFeedbackService.ACTION_STATUS_BROADCAST);
		
		this.registerReceiver(this.mCommandReceiver,filter);        
		
		
		// Tell the bluetooth service to send us a list of bluetooth devices and system status
		// Response comes in public void onStatusReceived(BioFeedbackStatus bfs) STATUS_PAIRED_DEVICES		
		mSpineManager.pollBluetoothDevices();	
		
		//		this.deviceManager = DeviceManager.getInstance(this.getBaseContext(), null);
		this.deviceList = (ListView)this.findViewById(R.id.list);
		
		this.deviceListAdapter = new ManagerItemAdapter(
				this,
				R.layout.manager_item
		);
		this.deviceList.setAdapter(this.deviceListAdapter);
		
		
		
		this.bluetoothDisabledDialog = new AlertDialog.Builder(this)
			.setMessage("Bluetooth is not enabled.")
			.setOnCancelListener(new OnCancelListener(){
				@Override
				public void onCancel(DialogInterface dialog) {
					finish();
				}
			})
			.setPositiveButton("Setup Bluetooth", new DialogInterface.OnClickListener () {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					startBluetoothSettings();
				}
			})
			.create();
		
		try {
			PackageManager packageManager = this.getPackageManager();
			PackageInfo info = packageManager.getPackageInfo(this.getPackageName(), 0);			
			mVersionName = info.versionName;
			Log.i(TAG, this.getClass().getSimpleName() + " Spine server Test Application Version " + mVersionName);
		} 
		catch (NameNotFoundException e) {
			   	Log.e(TAG, this.getClass().getSimpleName() + e.toString());
		}			
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
		switch(requestCode) {
			case BLUETOOTH_SETTINGS:
				break;
		}
	}
	
	private void startBluetoothSettings() {
		this.startActivityForResult(new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS), BLUETOOTH_SETTINGS);
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		if(!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
			this.bluetoothDisabledDialog.show();
		}
		if (deviceListAdapter != null) {
			deviceListAdapter.reloadItems();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mSpineManager.removeListener(this);			
    	this.unregisterReceiver(this.mCommandReceiver);		
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.bluetoothSettingsButton:
			this.startBluetoothSettings();
			break;
		}
	}
	
	private class GeneralReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (deviceListAdapter != null)
				deviceListAdapter.reloadItems();
		}
	}
	
	/**
	 * Adapter class for sensor list
	 * @author scott.coleman
	 *
	 */
	private class ManagerItemAdapter extends ArrayAdapter<BioSensor> implements OnCheckedChangeListener {
		private int layoutId;
		private LayoutInflater layoutInflater;
		private HashMap<View,String> viewDeviceMap = new HashMap<View,String>();

		public ManagerItemAdapter(Context context, int textViewResourceId) {
			super(context, textViewResourceId, new ArrayList<BioSensor>());
			
			this.layoutId = textViewResourceId;
			layoutInflater = (LayoutInflater)this.getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
			
			this.setNotifyOnChange(false);
			
			this.reloadItems();
		}
		
		public void reloadItems() {
			this.reloadItems(true);
		}
		
		private void reloadItems(boolean notify) {
			this.clear();
			
			for (BioSensor bioSensor: mBioSensors) {
				this.add(bioSensor);
			}			
			
			if(notify) {
				this.notifyDataSetChanged();
			}
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			BioSensor bioSensor = this.getItem(position);
			View v = layoutInflater.inflate(this.layoutId, null);
			
			if(bioSensor == null) {
				return v;
			}
			
			String statusString = Util.connectionStatusToString(bioSensor.mConnectionStatus);

			
			((TextView)v.findViewById(R.id.text1)).setText(bioSensor.mBTName);
			((TextView)v.findViewById(R.id.status)).setText(statusString);
						
			((ToggleButton)v.findViewById(R.id.enabled)).setChecked(bioSensor.mEnabled);
			((ToggleButton)v.findViewById(R.id.enabled)).setOnCheckedChangeListener(this);
			((ToggleButton)v.findViewById(R.id.enabled)).setTag(bioSensor.mBTAddress);

			String param = SharedPref.getParamForDevice(mInstance, bioSensor.mBTAddress);
			
			if (param != null) {
				((Button)v.findViewById(R.id.parameter)).getBackground().setColorFilter(Color.GREEN, PorterDuff.Mode.MULTIPLY);
				((Button)v.findViewById(R.id.parameter)).setText(param);
			}
			else {
				((Button)v.findViewById(R.id.parameter)).getBackground().setColorFilter(Color.LTGRAY, PorterDuff.Mode.MULTIPLY);
				((Button)v.findViewById(R.id.parameter)).setText("Parameter");
				
			}
			
			((Button)v.findViewById(R.id.parameter)).setTag(bioSensor.mBTAddress);
			
			
			
			viewDeviceMap.put(v, bioSensor.mBTAddress);
			
			return v;
		}

		@Override
		public void onCheckedChanged(final CompoundButton buttonView,
				boolean isChecked) {
			
			
			if (buttonView.getId() == R.id.enabled) {
				String address = (String)buttonView.getTag();
				SpineServiceCommand serviceCommand = null;
				
				serviceCommand = new SpineServiceCommand();
				serviceCommand.setBtAddress(address);
				
				if(isChecked) {
					serviceCommand.setCommand(SpineServiceCommand.COMMAND_ENABLED);
					// We technically don't have do to this because we'll receive a STATUS_PAIRED_DEVICES
					// command from the service as a result of the service commena we send
					// but we do it so the button updates immediately
					// as opposed to waiting for a round trip to/from the service
					updateBioSensorEnabled(address, true);				
				} else {
					serviceCommand.setCommand(SpineServiceCommand.COMMAND_DISABLED);
					updateBioSensorEnabled(address, false);				
				}
				
				// We need to tell the service to enable or disable this sensor
				mSpineManager.serviceCommand(serviceCommand);
				this.reloadItems();				
			}
			else if (buttonView.getId() == R.id.parameter) {
			}

		}
	}
	
	

	@Override
	public void newNodeDiscovered(Node newNode) {
	}

	@Override
	public void received(ServiceMessage msg) {
	}

	@Override
	public void received(Data data) {
	}

	@Override
	public void discoveryCompleted(Vector activeNodes) {
	}

	@Override
	public void onStatusReceived(BioFeedbackStatus bfs) {
        Log.d(TAG, this.getClass().getSimpleName() + ".onStatusReceived(" + bfs.messageId + ")"); 
		
        
		String name = bfs.name;
		if (name == null ) name = "sensor node";
		if(bfs.messageId.equals("CONN_CONNECTING")) {
			Log.i(TAG, this.getClass().getSimpleName() + " Received command : " + bfs.messageId + " to "  + name );
			updateBioSensorConnectionStatus(name, BioSensor.CONN_CONNECTING);		
		} 
		else if(bfs.messageId.equals("CONN_ANY_CONNECTED")) {
			Log.i(TAG, this.getClass().getSimpleName() + " Received command : " + bfs.messageId + " to "  + name );
			updateBioSensorConnectionStatus(name, BioSensor.CONN_CONNECTED);		
			
		} 
		else if(bfs.messageId.equals("CONN_CONNECTION_LOST")) {
			Log.i(TAG, this.getClass().getSimpleName() + " Received command : " + bfs.messageId + " to "  + name );
			updateBioSensorConnectionStatus(name, BioSensor.CONN_PAIRED);			
			
		}
		else if(bfs.messageId.equals("STATUS_PAIRED_DEVICES")) {
			// bfs.address contains a json arrary containing status of all BT Devices
			Log.i(TAG, this.getClass().getSimpleName() + " Received command : " + bfs.messageId + " to "  + name );
			
			Log.i(TAG, this.getClass().getSimpleName() + bfs.address );
			// Populate the mSensors array with a list of all paired sensors
			populateBioSensors(bfs.address);
		}
		
		if (deviceListAdapter != null)
			deviceListAdapter.reloadItems();
	}
	
	/**
	 * Receives a json string containing data about all of the paired sensors
	 * the adds a new BioSensor for each one to the mBioSensors collection
	 * 
	 * @param jsonString String containing info on all paired devices
	 */
	private void populateBioSensors(String jsonString) {
		
		Log.d(TAG, this.getClass().getSimpleName() + " populateBioSensors");

		// Now clear it out and Re-populate it.
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
					Log.i(TAG, this.getClass().getSimpleName() + " Adding sensor " + name + ", " + address + (enabled ? ", enabled":", disabled"));
					BioSensor bioSensor = new BioSensor(name, address, enabled);
					
					bioSensor.mConnectionStatus = connectionStatus;				
					mBioSensors.add(bioSensor);
				}
			}			
		} catch (JSONException e) {
		   	Log.e(TAG, e.toString());
		}
	}
	
	void updateBioSensorConnectionStatus(String name, int status) {
		for (BioSensor bioSensor: mBioSensors) {
			if (bioSensor.mBTName.equalsIgnoreCase(name)) {
				bioSensor.mConnectionStatus = status;
			}
		}	
	}
	
	void updateBioSensorEnabled(String address,Boolean enabled) {
		for (BioSensor bioSensor: mBioSensors) {
			if (bioSensor.mBTAddress.equalsIgnoreCase(address)) {
				bioSensor.mEnabled = enabled;
			}
		}	
	}
	
	
	
	
}
