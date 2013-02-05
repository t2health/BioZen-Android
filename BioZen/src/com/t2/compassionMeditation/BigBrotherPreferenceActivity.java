package com.t2.compassionMeditation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.t2.R;


public class BigBrotherPreferenceActivity extends PreferenceActivity 
		implements OnSharedPreferenceChangeListener, 
		OnPreferenceChangeListener,
		OnPreferenceClickListener	{
	private static final String TAG = BigBrotherPreferenceActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, this.getClass().getSimpleName() + ".onCreate()"); 
        
        // Load the preferences from an XML resource 
        addPreferencesFromResource(R.xml.bigbrother_preferences);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, this.getClass().getSimpleName() + ".onResume()");     	

        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        getPreferenceScreen().setOnPreferenceChangeListener(this);    
    }    

    @Override
	protected void onPause() {
        Log.d(TAG, this.getClass().getSimpleName() + ".onPause()");     
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);

		super.onPause();
	}

	@Override
	protected void onDestroy() {
        Log.d(TAG, this.getClass().getSimpleName() + ".onDestroy()"); 
		super.onDestroy();
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences arg0, String key) {
		
		Log.d(TAG, this.getClass().getSimpleName() + ".onSharedPreferenceChanged(), this = " + this); 	
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		super.onListItemClick(l, v, position, id);
	}

	@Override
	public boolean onPreferenceChange(Preference arg0, Object arg1) {
		return false;
	}
	
	@Override
	public boolean onPreferenceClick(Preference preference) {
		return true;
	}	
	
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		
	}
	
}
