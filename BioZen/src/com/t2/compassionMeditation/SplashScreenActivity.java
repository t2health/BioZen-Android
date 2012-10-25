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

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.TextView;

//Need the following import to get access to the app resources, since this
//class is in a sub-package.
import com.t2.R;


public class SplashScreenActivity extends Activity implements OnClickListener {
	private static final String TAG = "BFDemo";
	private TextView startupTipsView;
	private Timer startTimer;
	private Handler startHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			startMainActivity();
		}
	};
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);       		
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);		// This needs to happen BEFORE setContentView
        
		int nextTimeout = 2500;
		this.setContentView(R.layout.splash_screen_activity);
		View v = this.findViewById(R.id.splashWrapper);
		this.findViewById(R.id.splashWrapper).setOnClickListener(this);		

		Log.i(TAG, this.getClass().getSimpleName() + ".onCreate()"); 			
		

		startTimer = new Timer();
		startTimer.schedule(new TimerTask(){
			@Override
			public void run() {
				startHandler.sendEmptyMessage(0);
			}
		}, nextTimeout);
	}
	
	@Override
	public void onBackPressed() {
		Log.i(TAG, this.getClass().getSimpleName() + ".onBackPressed()"); 			
		startTimer.cancel();
		startMainActivity();

	   return;
	}	
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.splashWrapper:
			startTimer.cancel();
			startMainActivity();
		}
	}

	private void startMainActivity() {
		Log.i(TAG, this.getClass().getSimpleName() + ".startMainActivity()"); 			
		
//		Intent i = new Intent(this, MainChooserActivity.class);		
		Intent i = new Intent(this, MainChooserActivityNew.class);		
		this.startActivity(i);
		this.finish();
	}
}
