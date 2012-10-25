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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;

class FlurryProvider implements AnalyticsProvider {

	private String mApiKey;
	private Class<?> mAnalyticsClass;

	@Override
	public void init() {
		try {
			this.mAnalyticsClass = java.lang.Class.forName("com.flurry.android.FlurryAgent");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void setApiKey(String key) {
		this.mApiKey = key;
	}

	@Override
	public void setDebugEnabled(boolean b) {
		
	}

	@Override
	public void onStartSession(Context context) {
		if(mAnalyticsClass == null) {
			return;
		}
		
		try {
			Method m = mAnalyticsClass.getDeclaredMethod("onStartSession", Context.class, String.class);
			m.invoke(null, context, this.mApiKey);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		
		/*FlurryAgent.onStartSession(
				context, 
				apiKey
		);*/
	}

	@Override
	public void onEndSession(Context context) {
		if(mAnalyticsClass == null) {
			return;
		}
		
		try {
			Method m = mAnalyticsClass.getDeclaredMethod("onEndSession", Context.class);
			m.invoke(null, context);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		
		//FlurryAgent.onEndSession(context);
	}

	@Override
	public void onEvent(String event, String key, String value) {
		HashMap<String,String> params = new HashMap<String,String>();
		params.put(key, value);
		onEvent(event, params);
	}

	@Override
	public void onEvent(String event, Bundle parameters) {
		HashMap<String,String> params = new HashMap<String,String>();
		for(String key: parameters.keySet()) {
			Object val = parameters.get(key);
			params.put(key, val+"");
		}

		onEvent(event, params);
	}

	@Override
	public void onEvent(String event) {
		if(mAnalyticsClass == null) {
			return;
		}
		
		try {
			Method m = mAnalyticsClass.getDeclaredMethod("onEvent", String.class);
			m.invoke(null, event);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		//FlurryAgent.onEvent(event);
	}

	@Override
	public void onEvent(String event, Map<String, String> parameters) {
		if(mAnalyticsClass == null) {
			return;
		}
		
		try {
			Method m = mAnalyticsClass.getDeclaredMethod("onEvent", Map.class);
			m.invoke(null, parameters);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		//FlurryAgent.onEvent(event, parameters);
	}

	@Override
	public void onPageView() {
		if(mAnalyticsClass == null) {
			return;
		}
		
		try {
			Method m = mAnalyticsClass.getDeclaredMethod("onPageView");
			m.invoke(null);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		//FlurryAgent.onPageView();
	}
}
