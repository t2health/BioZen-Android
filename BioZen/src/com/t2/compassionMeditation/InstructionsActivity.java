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

import com.t2.R;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;

public class InstructionsActivity extends Activity implements View.OnTouchListener {
	private static final String mActivityVersion = "1.0";
	CheckBox mShowInstructionsCheckbox;	
    private WebView mWebView;	

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);		
//        setContentView(R.layout.instructions_activity_layout);        
        setContentView(R.layout.new_instructions_layout);        
		mShowInstructionsCheckbox = (CheckBox) findViewById(R.id.checkBox1);
		
		boolean instructionsOnStart = SharedPref.getBoolean(this, BioZenConstants.PREF_INSTRUCTIONS_ON_START, BioZenConstants.PREF_INSTRUCTIONS_ON_START_DEFAULT);
		mShowInstructionsCheckbox.setChecked(instructionsOnStart);

		final LinearLayout parent = (LinearLayout) findViewById(R.id.instructionslayout);
//		final RelativeLayout parent = (RelativeLayout) findViewById(R.id.instructionsLayout	);
		parent.setOnTouchListener (this);
		
        mWebView = (WebView) findViewById(R.id.webview);

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(false);
        mWebView.setBackgroundColor(Color.parseColor("#C0C0C0"));      
        mShowInstructionsCheckbox.setBackgroundColor(Color.parseColor("#C0C0C0"));

        Button backButton = (Button) findViewById(R.id.buttonBack);
        backButton.setBackgroundColor(Color.parseColor("#C0C0C0")); 

		final LinearLayout layout = (LinearLayout) findViewById(R.id.linearLayout1);
		layout.setBackgroundColor(Color.parseColor("#C0C0C0"));  
        
        
        mWebView.setWebChromeClient(new MyWebChromeClient());

//        mWebView.addJavascriptInterface(new DemoJavaScriptInterface(), "demo");

        mWebView.loadUrl("file:///android_asset/index.html");	

	}
	
    /**
     * Provides a hook for calling "alert" from javascript. Useful for
     * debugging your javascript.
     */
    final class MyWebChromeClient extends WebChromeClient {
        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
//            Log.d(LOG_TAG, message);
            result.confirm();
            return true;
        }
    }
    
    
	public void onButtonClick(View v)
	{
		 final int id = v.getId();
		    switch (id) {
		    case R.id.buttonBack:
		    	finish();
		    	
		    	break;
		    		    
		    }
	}
	
	@Override
	protected void onDestroy() {
		boolean isChecked = mShowInstructionsCheckbox.isChecked();
		SharedPref.putBoolean(this, BioZenConstants.PREF_INSTRUCTIONS_ON_START, isChecked );
		
		Intent resultIntent;
		resultIntent = new Intent();
		resultIntent.putExtra(BioZenConstants.INSTRUCTIONS_USER_ACTIVITY_RESULT, "");
		setResult(RESULT_OK, resultIntent);
		super.onDestroy();
		
		
	}
	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		finish();
		return false;
	}
	

}
