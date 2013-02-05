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

//Need the following import to get access to the app resources, since this
//class is in a sub-package.

import com.t2.R;
import com.t2.biofeedback.BioFeedbackService;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.widget.Toast;




public class BioZenPreferenceActivity extends PreferenceActivity 
		implements OnSharedPreferenceChangeListener, OnPreferenceChangeListener, OnPreferenceClickListener {
    public static final String KEY_PREFERENCE = "change_user_mode_preference";

	private static final String TAG = "BFDemo";    
    
    String existingSessionLength;    
    String existingAlphaGain;    
    
    // Shouldn't this point to settings_advanced_key in strings.xml
    public static final String settings_advanced_key = "advanced";
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        
        //  Read default values and it they haven't been set, set them
        // so they show up in the first preferences screen
        // Otherwise they show up as blank!
        boolean pref = SharedPref.getBoolean(this,
				BioZenConstants.PREF_HELP_ON_STARTUP,
				BioZenConstants.PREF_HELP_ON_STARTUP_DEFAULT);
        if (pref) {
            SharedPref.putBoolean(this,
    				BioZenConstants.PREF_HELP_ON_STARTUP,BioZenConstants.PREF_HELP_ON_STARTUP_DEFAULT);
        }
       	pref = SharedPref.getBoolean(this,
				BioZenConstants.PREF_HELP_ON_VIEW,
				BioZenConstants.PREF_HELP_ON_VIEW_DEFAULT);
        if (pref) {
            SharedPref.putBoolean(this,
    				BioZenConstants.PREF_HELP_ON_VIEW,BioZenConstants.PREF_HELP_ON_VIEW_DEFAULT);
        }
       	pref = SharedPref.getBoolean(this,
				BioZenConstants.PREF_HELP_ON_REVIEW,
				BioZenConstants.PREF_HELP_ON_REVIEW_DEFAULT);
        if (pref) {
            SharedPref.putBoolean(this,
    				BioZenConstants.PREF_HELP_ON_REVIEW,BioZenConstants.PREF_HELP_ON_REVIEW_DEFAULT);
        }
       	pref = SharedPref.getBoolean(this,
				BioZenConstants.PREF_HELP_ON_NEWSESSION,
				BioZenConstants.PREF_HELP_ON_NEWSESSION_DEFAULT);
        if (pref) {
            SharedPref.putBoolean(this,
    				BioZenConstants.PREF_HELP_ON_NEWSESSION,BioZenConstants.PREF_HELP_ON_NEWSESSION_DEFAULT);
        }
        
        //  Read default values and it they haven't been set, set them
        // so they show up in the first preferences screen
        // Otherwise they show up as blank!
        String value = SharedPref.getString(this, "audio_track", "None");
        if (value.equalsIgnoreCase("None")) {
        	SharedPref.putString(this, "audio_track", "None");
        }

        value = SharedPref.getString(this, "background_images", "-1");
        if (value.equalsIgnoreCase("-1")) {
        	SharedPref.putString(this, "background_images", "Sunset");
        }

        value = SharedPref.getString(this, "band_of_interest", "-1");
        if (value.equalsIgnoreCase("-1")) {
        	SharedPref.putString(this, "band_of_interest", "9");  // 9 = eMeditation)
        }

        value = SharedPref.getString(this, "parameter_of_interest", "-1");
        if (value.equalsIgnoreCase("-1")) {
        	SharedPref.putString(this, "parameter_of_interest", "12");  // 12 = skintemp)
        }

        value = SharedPref.getString(this, "alpha_gain", "-1");
        if (value.equalsIgnoreCase("-1")) {
        	SharedPref.putString(this, "alpha_gain", "5"); 
        }

        value = SharedPref.getString(this, "sensor_sample_rate", "-1");
        if (value.equalsIgnoreCase("-1")) {
        	SharedPref.putString(this, "sensor_sample_rate", "4");  
        }
        
        value = SharedPref.getString(this, "display_sample_rate", "-1");
        if (value.equalsIgnoreCase("-1")) {
        	SharedPref.putString(this, "display_sample_rate", "1");  
        }
        
        
        existingSessionLength = SharedPref.getString(this, "session_length", "-1");    
        if (existingSessionLength.equalsIgnoreCase("-1")) {
        	existingSessionLength = "20";
        	SharedPref.putString(this, "session_length", existingSessionLength);
        }

        
        
        existingAlphaGain = SharedPref.getString(this, "alpha_gain", "-1");    
        
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.bio_zen_preferences);
        

    // Take care, the advance pref preference might be commented out in bio_sen_preferences.xml    
    PreferenceScreen screen = this.getPreferenceScreen();
	Preference preference = screen.findPreference( settings_advanced_key);
	
	if (preference != null) {
		preference.setOnPreferenceClickListener(this);
	}
        
    }

    
    @Override
    protected void onResume() {
        super.onResume();

        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }    

    
    
    
	@Override
	protected void onStop() {
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
//		Intent intent = new Intent(BioFeedbackService.ACTION_SERVICE_BTNAMES);
//		intent.putExtra("BTNAMES", "fred is a wart");
//		this.sendBroadcast(intent);
        
        
        // We need to transfer these names to a specific shared memory for the service to see
        
    	String shimmerName = SharedPref.getString(this, "shimmer_sensor", "");
    	String zephyrName = SharedPref.getString(this, "zephyr_sensor", "");
    	String neuroskyName =SharedPref.getString(this, "neurosky_sensor", "");
    	String mobiName = SharedPref.getString(this, "mobi_sensor", "");
    	String spineName = SharedPref.getString(this, "spine_sensor", "");        
        

    	try {

//            String APP_SHARED_PREFS = "com.t2.compassionMeditation.BTNAMES";         
//            SharedPreferences myPrefs = this.getSharedPreferences(APP_SHARED_PREFS, MODE_WORLD_READABLE);

    		Context otherAppsContext = this.createPackageContext("com.t2",0);
    		SharedPreferences myPrefs = otherAppsContext.getSharedPreferences("com.t2.compassionMeditation.BTNAMES", Context.MODE_WORLD_READABLE);
    		
            
            SharedPreferences.Editor prefsEditor = myPrefs.edit();
            prefsEditor.putString("shimmer_sensor", shimmerName);
            prefsEditor.putString("zephyr_sensor", zephyrName);
            prefsEditor.putString("neurosky_sensor", neuroskyName);
            prefsEditor.putString("mobi_sensor", mobiName);
            prefsEditor.putString("spine_sensor", spineName);
            prefsEditor.commit();            
    	} 
    	catch (Exception e) {
    		Log.e(TAG, e.toString());
    	}          
        
        
		super.onStop();
	}


	@Override
	public void onSharedPreferenceChanged(SharedPreferences arg0, String key) {
      if (key.endsWith("session_length")) {
    	  String stringValue = arg0.getString(key, "-1");
    	  int value = 0;
    	  try {
    		  value = Integer.parseInt(stringValue);
			} 
    	  catch (NumberFormatException e) {
    		  value = -1;
    	  }
    	  finally {
        	  if (value < 1 || value > 60) {
        	      Toast.makeText(this, " *** " + stringValue + " is an invalid value, try again ***\n Valid values are 1 - 60", Toast.LENGTH_LONG).show();
        	      SharedPref.putString(this, "session_length", existingSessionLength);        	      
        	      
        	  }
        	  else {
        	      Toast.makeText(this, key + " changed to " + value, Toast.LENGTH_LONG).show();
        	  }
    	  }
      }
      else if (key.endsWith("alpha_gain")) {
    	  String stringValue = arg0.getString(key, "-1");
    	  double value = 0;
    	  try {
//    		  value = Integer.parseInt(stringValue);
    		  value = Double.parseDouble(stringValue);
			} 
    	  catch (NumberFormatException e) {
    		  value = -1;
    	  }
    	  finally {
        	  if (value < 0 || value > 10) {
        	      Toast.makeText(this, " *** " + stringValue + " is an invalid value, try again ***\n Valid values are 0 - 10", Toast.LENGTH_LONG).show();
        	      SharedPref.putString(this, "alpha_gain", existingAlphaGain);        	      
        	      
        	  }
        	  else {
        	      Toast.makeText(this, "Image Intensity changed to " + value, Toast.LENGTH_LONG).show();
        	  }
    	  }
      }
      else if (key.endsWith("band_of_interest")) {
    	  String stringValue = arg0.getString(key, "");
    	  String[] bands = getResources().getStringArray(R.array.bands_of_interest_array);
    	  int value = 0;
    	  try {
    		  value = Integer.parseInt(stringValue);
    	      Toast.makeText(this, key + " changed to " + bands[value], Toast.LENGTH_LONG).show();
    		  
			} 
    	  catch (NumberFormatException e) {
    		  value = -1;
    	  }    	  
      }      
      else if (key.endsWith("parameter_of_interest")) {
    	  String stringValue = arg0.getString(key, "");
    	  String[] bands = getResources().getStringArray(R.array.bands_of_interest_array);
    	  int value = 0;
    	  try {
    		  value = Integer.parseInt(stringValue);
    	      Toast.makeText(this, key + " changed to " + bands[value], Toast.LENGTH_LONG).show();
    		  
			} 
    	  catch (NumberFormatException e) {
    		  value = -1;
    	  }    	  
      }      
      else if (key.endsWith("background_images")) {
    	  String stringValue = arg0.getString(key, "");
          Toast.makeText(this, key + " changed to " + stringValue, Toast.LENGTH_LONG).show();
      }      
      else if (key.endsWith("audio_track")) {
    	  String stringValue = arg0.getString(key, "");
          Toast.makeText(this, key + " changed to " + stringValue, Toast.LENGTH_LONG).show();
      }      
      
      
      
	}


	@Override
	public boolean onPreferenceChange(Preference arg0, Object arg1) {
		// TODO Auto-generated method stub
		return false;
	}


	@Override
	public boolean onPreferenceClick(Preference preference) {
		Intent userIntent = null;		
		boolean handledClick = true;		
		if (preference.getKey().equals( settings_advanced_key))
		{
			userIntent = new Intent(this, BigBrotherPreferenceActivity.class);
		}
		
		if(userIntent != null) {
			startActivity(userIntent);
		}		
		
		return handledClick;
	}
}
