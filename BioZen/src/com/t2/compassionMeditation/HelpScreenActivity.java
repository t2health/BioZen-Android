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


import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import bz.org.t2health.lib.activity.BaseActivity;


//Need the following import to get access to the app resources, since this
//class is in a sub-package.
import com.t2.R;


public class HelpScreenActivity extends BaseActivity implements OnClickListener {
	private static final String TAG = "BFDemo";
    private DisplayMetrics mDisplayMetrics = new DisplayMetrics();	
	

	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        Log.i(TAG, this.getClass().getSimpleName() + ".onCreate()");
        getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);		// This needs to happen BEFORE setContentView
        
        
        // If we were called from the Biomap activity then it will have
        // sent us a target to focus on
        try {
			// Get target name if one was supplied
			Bundle bundle = getIntent().getExtras();
			String sessionType = bundle.getString("SESSION_TYPE");
			if (sessionType.equalsIgnoreCase("newsession")) {
				setContentView(R.layout.help_screen_layout);        
			}
			else if (sessionType.equalsIgnoreCase("review")) {
				setContentView(R.layout.help_screen_layout_review);
			}
			else if (sessionType.equalsIgnoreCase("view")) {
				setContentView(R.layout.help_screen_layout_view);
			}
			
		} catch (Exception e1) {
		}        
        
        
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
        Log.i(TAG, this.getClass().getSimpleName() + ".onDestroy()");
		
		Intent resultIntent;
		resultIntent = new Intent();
		resultIntent.putExtra(BioZenConstants.NEW_SESSION_ACTIVITY_RESULT, "");
		setResult(RESULT_OK, resultIntent);
		
		
	}	
	
	@Override
	public void onClick(View v) {
	}

	
	/**
	 * Handles UI button clicks
	 * @param v
	 */
	public void onButtonClick(final View v)
	{
		finish();
		 final int id = v.getId();
		    switch (id) {
		    
		    }
	}
	
}
