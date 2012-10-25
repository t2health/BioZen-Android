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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.t2health.lib1.BioParameter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.t2.R;

public class ViewParametersActivity extends Activity
				implements OnItemLongClickListener, OnClickListener{

	private static final String TAG = "BFDemo";

	
	
	private static ViewParametersActivity instance;
	
	/**
	 * UI ListView for parameter list
	 */
	private ListView parameterKeysList;

	/**
	 * Ordered list of parameters keys 
	 */
	private ArrayList<ParametersKeyItem> parameterKeyItems = new ArrayList<ParametersKeyItem>();
	
	/**
	 * Ordered list of BioParameters associated with the currently selected user
	 * 
	 * note that we keep this list only so we can reference the currently selected session for deletion
	 */
	private ArrayList<BioParameter> parameterItems = new ArrayList<BioParameter>();
	
	

	/**
	 * Index of currently selected session
	 * @see parameterItems
	 */
	private int mSelectedId;		
	
	
	
	private ArrayList<KeyItem> keyItems = new ArrayList<KeyItem>();
	protected SharedPreferences sharedPref;	
	

	
	/**
	 * Adapter used to provide list of views for the parameterKeyItems list
	 * @see parameterKeyItems
	 */
	private ParametersKeyItemAdapter parameterKeysAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);       		
		
        instance = this;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());   
        
		requestWindowFeature(Window.FEATURE_NO_TITLE);		
        
        setContentView(R.layout.view_parameters_layout); 

        
        
		parameterKeysList = (ListView) this.findViewById(R.id.listViewSessionKeys);
		parameterKeysList.setOnItemLongClickListener(this);

		Intent intent = this.getIntent();
		
		
        updateListView();


		
	}
	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
	}

	static class ParametersKeyItem {
		public long id;
		public String title1;
		public String title2;
		public int color;
		public boolean visible;
		public boolean reverseData = false; 
		
		public ParametersKeyItem(long id, String title1, String title2) {
			this.id = id;
			this.title1 = title1;
			this.title2 = title2;
		}
		
		public ParametersKeyItem(long id, String title1, String title2, int color) {
			this.id = id;
			this.title1 = title1;
			this.title2 = title2;
			this.color = color;
		}
		
		public HashMap<String,Object> toHashMap() {
			HashMap<String,Object> data = new HashMap<String,Object>();
			data.put("id", id);
			data.put("title1", title1);
			data.put("title2", title2);
			data.put("color", color);
			data.put("visible", visible);
			return data;
		}
	}	
	
	class ParametersKeyItemAdapter extends ArrayAdapter<ParametersKeyItem> {
		public static final int VIEW_TYPE_ONE_LINE = 1;
		public static final int VIEW_TYPE_TWO_LINE = 2;
		
		private LayoutInflater layoutInflater;
		private int layoutId;

		public ParametersKeyItemAdapter(Context context, int viewType,
				List<ParametersKeyItem> objects) {
			super(context, viewType, objects);
			
			layoutInflater = (LayoutInflater)context.getSystemService(LAYOUT_INFLATER_SERVICE);
			layoutId = R.layout.list_item_result_key_v;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView == null) {
				convertView = layoutInflater.inflate(layoutId, null);
			}
			final int buttonposition = position;			

			final ParametersKeyItem item = this.getItem(position);
			TextView tv1 = (TextView)convertView.findViewById(R.id.text1);
			TextView tv2 = (TextView)convertView.findViewById(R.id.text2);
			ImageButton button = (ImageButton) convertView.findViewById(R.id.buttonViewDetails);
			View keyBox = convertView.findViewById(R.id.keyBox);
			
			if(tv1 != null) {
				tv1.setText(item.title1);
			}
			if(tv2 != null) {
				tv2.setText(item.title2);
			}
			if(button != null) {
				button.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						mSelectedId = buttonposition;
		            	showSessionDetails();
					}
				});				
			}
			
			if(keyBox != null) {
				keyBox.setBackgroundColor(item.color);
			}
			
			return convertView;
		}
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
			long arg3) {
		
		mSelectedId = arg2;		
		
		AlertDialog.Builder alert = new AlertDialog.Builder(instance);
		alert.setTitle("Choose Activity");
    	alert.setPositiveButton("View Details", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	showSessionDetails();
            	
            }
        });				
		
    	alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });				
		
    	alert.setNeutralButton("Delete Session", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	
				AlertDialog.Builder alert2 = new AlertDialog.Builder(instance);
				alert2.setMessage("Are you sure?");

				alert2.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
//	            	try {
//	            		parameterKeyItems.get(mSelectedId);
////	            		mBioSessionDao.delete(parameterItems.get(mSelectedId));	
//	            		updateListView();
//						
//					} catch (SQLException e) {
//						Log.e(TAG, "Error deleting user" + e.toString());
//					}
				}
				});

				alert2.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				  public void onClick(DialogInterface dialog, int whichButton) {
				  }
				});

				alert2.show();
            }
        });				
		
		alert.show();		
		return false;
	}

	/**
	 * Populates parameterItems and parameterKeyItems with session from the currently selected user
	 * then uses the adapter to populate the list view with that data
	 */
	private void updateListView() {

//		// Retrieve the BuiUser object associated with object mSelectedUserName
//		try {
//			mBioUserDao = getHelper().getBioUserDao();
//			mBioSessionDao = getHelper().getBioSessionDao();
//			
//			QueryBuilder<BioUser, Integer> builder = mBioUserDao.queryBuilder();
//			builder.where().eq(BioUser.NAME_FIELD_NAME, mCurrentBioUserName);
//			builder.limit(1);
//			List<BioUser> list = mBioUserDao.query(builder.prepare());	
//			
//			if (list.size() >= 1) {
//				mCurrentBioUser = list.get(0);
//			}
//			else {
//				Log.e(TAG, "General Database error" + mCurrentBioUserName);
//			}
//			
//		} catch (SQLException e) {
//			Log.e(TAG, "Can't find user: " + mCurrentBioUserName , e);
//
//		}        
        	
		// Fill the collections parameterItems, and parameterKeyItems with session data from the current user
		parameterItems.clear();
		parameterKeyItems.clear();
		
		
		int color;
		// Put some bogus data in for now
		BioParameter bp = new BioParameter(1, "Heart Rate", "Heart Rate", true);
		parameterItems.add(bp);		
		color = Color.GREEN;
		ParametersKeyItem item = new ParametersKeyItem(1,"Heart Rate", "Heart Rate", color);
		parameterKeyItems.add(item);

		bp = new BioParameter(1, "Skin Resistance", "Skin Resistance", true);
		parameterItems.add(bp);		
		color = Color.YELLOW;
		item = new ParametersKeyItem(1,"Skin Resistance", "Skin Resistance", color);
		parameterKeyItems.add(item);

		
		//		parameterItems.add(session);
		
//		parameterItems.add(bp);		
		
		
//		if (mCurrentBioUser != null) {
//		
//			for (BioSession session: mCurrentBioUser.getSessions()) {
//				
//				if (session.time >= startTime && session.time <= endTime ) {
//					
//
//					SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yy HH:mm:ss", Locale.US);
//					String title = sdf.format(new Date(session.time));			
//					try {
//						String categoryInitial = "(" + session.category.charAt(0) + ")";
//						title += categoryInitial;
//					} catch (IndexOutOfBoundsException e) {
//					}
//					
//					
//					int color;
////					if (session.precentComplete >= 100) {
//						color = Color.GREEN;
////					}
////					else {
////						color = Color.YELLOW;
////					}
//					
//					ParametersKeyItem item = new ParametersKeyItem(1,title , "", color);
//					parameterKeyItems.add(item);
//					parameterItems.add(session);
//					
//				}
//			}
//		}				
	
		parameterKeysAdapter = new ParametersKeyItemAdapter(this, 1, parameterKeyItems);		
		parameterKeysList.setAdapter(parameterKeysAdapter);		
	}
	
	private void showSessionDetails() {
//		AlertDialog.Builder alert2 = new AlertDialog.Builder(instance);
//		
//		
//		alert2.setMessage(sessionDetails);
//		alert2.show();
		
	}

	private String secsToHMS(int time) {
		long secs = time;
		long hours = secs / 3600;
		secs = secs % 3600;
		long mins = secs / 60;
		secs = secs % 60;
		
		return hours + ":" + mins + ":" + secs;
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
	}
	}
	
	
}
