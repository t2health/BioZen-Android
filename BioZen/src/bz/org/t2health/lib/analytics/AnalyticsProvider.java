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
package bz.org.t2health.lib.analytics;

import java.util.Map;

import android.content.Context;
import android.os.Bundle;

interface AnalyticsProvider {
	/**
	 * Called on first instantiation.
	 */
	public void init();
	
	/**
	 * Set the key the provider requires for collecting data.
	 * @param key
	 */
	public void setApiKey(String key);
	
	/**
	 * 
	 * @param b
	 */
	public void setDebugEnabled(boolean b);
	
	/**
	 * Fires when a new session begins, this typically occurs onStart
	 * in an Activity.
	 * @param context
	 */
	public void onStartSession(Context context);
	
	/**
	 * Fires when a session ends, this typically occurs onStop in an Activity.
	 * @param context
	 */
	public void onEndSession(Context context);
	
	/**
	 * Sends an event to the provider.
	 * @param event
	 * @param key
	 * @param value
	 */
	public void onEvent(String event, String key, String value);
	
	/**
	 * Sends an event to the provider.
	 * @param event
	 * @param parameters
	 */
	public void onEvent(String event, Bundle parameters);
	
	/**
	 * Sends and event to the provider.
	 * @param event
	 */
	public void onEvent(String event);
	
	/**
	 * Sends and event to the provider.
	 * @param event
	 * @param parameters
	 */
	public void onEvent(String event, Map<String,String> parameters);
	
	/**
	 * Signals to the provider that the whole screen has changed.
	 */
	public void onPageView();
}
