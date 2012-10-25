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
package bz.org.t2health.lib.activity;


//import org.t2health.lib.db.DatabaseOpenHelper;
//import org.t2health.lib.db.ManifestSqliteOpenHelperFactory;

import android.content.Intent;
import android.os.Bundle;
import bz.org.t2health.lib.ManifestMetaData;
import bz.org.t2health.lib.SharedPref;
import bz.org.t2health.lib.analytics.Analytics;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
//import com.nullwire.trace.ExceptionHandler;
import com.t2.compassionDB.DatabaseHelper;

/**
 * The base class to use when creating an activity. This class will read
 * meta-data from the manifest.xml file in order to configure ORM, Analytics,
 * Remote Stack Trace and other code.
 * The code in this activity is the exact same as the code used in
 * BasePreferenceActivity and BaseService, be sure any changes you make to this
 * source code is copied to those as well.
 * @author robbiev
 */
//public abstract class BaseActivity extends OrmLiteBaseActivity<DatabaseOpenHelper> {
	public abstract class BaseActivity extends OrmLiteBaseActivity<DatabaseHelper>{
	private boolean isORMConfigured = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

//		// register remote callbacks for analytics and stack trace collection.
//		// configure remote stack trace collector
//        if(ManifestMetaData.RemoteStackTrace.isEnabled(this) && SharedPref.RemoteStackTrace.isEnabled(this)) {
//        	ExceptionHandler.register(this, 
//        			ManifestMetaData.RemoteStackTrace.getURL(this)
//			);
//        }
//        
//		// configure the database.
//        isORMConfigured = ManifestMetaData.Database.isConfigured(this) && ManifestSqliteOpenHelperFactory.isClassesConfigured(this);
//        if(isORMConfigured) {
//			OpenHelperManager.setOpenHelperFactory(
//					ManifestSqliteOpenHelperFactory.getInstance()
//			);
//		}
//        
		// configure and make analytics event call.
		if(ManifestMetaData.Analytics.isEnabled(this)) {
			Analytics.init(
					Analytics.providerFromString(ManifestMetaData.Analytics.getProvider(this)),
					ManifestMetaData.Analytics.getProviderKey(this), 
					SharedPref.Analytics.isEnabled(this)
			);
			Analytics.setDebugEnabled(ManifestMetaData.isDebugEnabled(this));
			Analytics.onPageView();
			String event = getAnalyticsActivityEvent();
			if(event != null) {
				Analytics.onEvent(event);
			}
		}
	}
	
	/**
	 * When the activity loads, this string will be sent to Analytics.onEvent
	 * @return (default: this.getClass().getSimpleName())
	 */
	protected String getAnalyticsActivityEvent() {
		return this.getClass().getSimpleName();
	}
	
	/**
	 * Retrieve a string value from an intent. This will handle both resource id
	 * and string values.
	 * @param intent
	 * @param extraKey
	 * @return
	 */
	protected final String getIntentText(Intent intent, String extraKey) {
		String text = intent.getStringExtra(extraKey);
		
		if(text != null && text.matches("[0-9]+")) {
			int resId = Integer.parseInt(text);
			String resourceText = getString(resId);
			if(resourceText != null) {
				text = resourceText;
			}
		}
		
		return text;
	}

//	@Override
//	public final synchronized DatabaseOpenHelper getHelper() {
//		if(isORMConfigured) {
//			return super.getHelper();
//		}
//		
//		throw new RuntimeException("The ORM has not been properly configured. Look at the Library's AndroidManifest.xml file for setup instructions.");
//	}
	
	@Override
	protected void onStart() {
		super.onStart();
		Analytics.onStartSession(this);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		Analytics.onEndSession(this);
	}
}
