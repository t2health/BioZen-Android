package com.t2.compassionMeditation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.achartengine.ChartFactory;
import org.achartengine.chart.PointStyle;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;




import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import bz.org.t2health.lib.activity.BaseActivity;


import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

import com.t2.R;

import com.t2.compassionDB.BioSession;
import com.t2.compassionDB.BioUser;
import com.t2.compassionUtils.MathExtra;


public class ViewSessionsActivity extends BaseActivity
				implements OnItemLongClickListener, OnClickListener{
	private static final String TAG = "BFDemo";
	private static final String mActivityVersion = "1.0";
	public static final String EXTRA_TIME_START = "timeStart";
	public static final String EXTRA_CALENDAR_FIELD = "calendarField";
	public static final String EXTRA_CALENDAR_FIELD_INCREMENT = "calendarFieldIncrement";
	public static final String EXTRA_REVERSE_DATA = "reverseData";
	private static final String KEY_NAME = "categories_";	

	private static final int EXPORT_SUCCESS = 1;
	private static final int EXPORT_FAILED = 0;
	
	
	
	private ProgressDialog mProgressDialog;	
	
	
	private static ViewSessionsActivity instance;
	private DisplayMetrics displayMetrics = new DisplayMetrics();
	
	private View mDeviceChartView;
	
	

	private static final int DIRECTION_PREVIOUS = -1;
	private static final int DIRECTION_NONE = 0;
	private static final int DIRECTION_NEXT = 1;
	
	
	/**
	 * Currently selected user name (as selected at the start of the session)
	 */
	private String mCurrentBioUserName;
	
	/**
	 * BioUser associated with currently selected user name (as selected at the start of the session)
	 */
	private BioUser mCurrentBioUser = null;

	private Dao<BioUser, Integer> mBioUserDao;
	private Dao<BioSession, Integer> mBioSessionDao;
	
	/**
	 * UI ListView for sessions list
	 */
	private ListView sessionKeysList;

	/**
	 * Ordered list of session keys associated with the currently selected user
	 * 
	 */
	private ArrayList<SessionsKeyItem> sessionKeyItems = new ArrayList<SessionsKeyItem>();
	
	/**
	 * Ordered list of BioSessions associated with the currently selected user
	 * 
	 * note that we keep this list only so we can reference the currently selected session for deletion
	 */
	private ArrayList<BioSession> sessionItems = new ArrayList<BioSession>();
	
	

	/**
	 * Index of currently selected session
	 * @see sessionItems
	 */
	private int mSelectedId;		
	
	protected Calendar startCal;
	protected Calendar endCal;
	protected int calendarField;				// index of calandar parameter (Defaults to day of month)
	protected int calendarFieldIncrement;				
	
	
	private TextView monthNameTextView;
	SimpleDateFormat monthNameFormatter;
	
	Spinner mBandOfInterestSpinner;
	Spinner mCategorySpinner;
	private ArrayList<String> mCurrentCategories;	
	/**
	 * Currently selected category
	 */
	private String mSelectedCategoryName = "";

	/**
	 * Index of currently selected category
	 */	
	private int mSelectedCategory;

	
	
	
	private ArrayList<KeyItem> keyItems = new ArrayList<KeyItem>();
	protected SharedPreferences sharedPref;	
	protected int mBandOfInterest = 0;
	
	public class MyOnItemSelectedListener implements OnItemSelectedListener {

	    public void onItemSelected(AdapterView<?> parent,
	        View view, int pos, long id) {
			SharedPref.putInt(instance, BioZenConstants.PREF_BAND_OF_INTEREST_REVIEW , pos);	   
			mBandOfInterest = pos;
    		generateChart(DIRECTION_NEXT); 
			
			
	    }

	    public void onNothingSelected(AdapterView parent) {
	      // Do nothing.
	    }
	}	
	
	public class MyCategorySelectedListener implements OnItemSelectedListener {

	    public void onItemSelected(AdapterView<?> parent,
	        View view, int pos, long id) {
	    	
	    	mSelectedCategoryName = mCurrentCategories.get(pos);
	    	
	        updateListView();	    	
    		generateChart(DIRECTION_NEXT); 
			
			
	    }

	    public void onNothingSelected(AdapterView parent) {
	      // Do nothing.
	    }
	}	
	
	/**
	 * Adapter used to provide list of views for the sessionKeyItems list
	 * @see sessionKeyItems
	 */
	private SessionsKeyItemAdapter sessionKeysAdapter;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);       		
		
        instance = this;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());   
        
		requestWindowFeature(Window.FEATURE_NO_TITLE);		
        
        setContentView(R.layout.view_sessions_layout); 
        monthNameTextView = (TextView) this.findViewById(R.id.monthName);

        
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setMessage("Exporting data");
        mProgressDialog.setCancelable(false);        
        
        
        
        
        mCurrentBioUserName = SharedPref.getString(this, "SelectedUser", 	"");
		sessionKeysList = (ListView) this.findViewById(R.id.listViewSessionKeys);
		sessionKeysList.setOnItemLongClickListener(this);

		Intent intent = this.getIntent();
//		calendarField = intent.getIntExtra(EXTRA_CALENDAR_FIELD, Calendar.HOUR_OF_DAY);
		calendarField = intent.getIntExtra(EXTRA_CALENDAR_FIELD, Calendar.DAY_OF_MONTH);

		// Set up current categories and specifically the selected category
		// BEFORE updateListView and generateChart so the specified category(s) can 
		// be filtered
		mCurrentCategories = getCategories("categories");
		mCurrentCategories.add("All");
		
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, mCurrentCategories);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mCategorySpinner = (Spinner) findViewById(R.id.spinnerCategorySpinner);		
		mCategorySpinner.setAdapter(adapter)	;	

		mCategorySpinner.setOnItemSelectedListener(new MyCategorySelectedListener());    
		mSelectedCategory = mCurrentCategories.size() - 1; // all
		mCategorySpinner.setSelection(mSelectedCategory);
		mSelectedCategoryName = mCurrentCategories.get(mSelectedCategory);
		
		
		
		setCalendarResolution();
    	setupCalendars();            	
        updateListView();

        this.findViewById(R.id.monthMinusButton).setOnClickListener(this);
		this.findViewById(R.id.monthPlusButton).setOnClickListener(this);
        this.monthNameTextView.setOnClickListener(this);
        
        // Get the list of band names from the first session (All of the session key names will be the same)
		if (sessionItems.size() >= 1) {
			BioSession session = sessionItems.get(0);
			ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, session.keyItemNames);
			adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			mBandOfInterestSpinner = (Spinner) findViewById(R.id.spinnerBandOfInterest);		
			mBandOfInterestSpinner.setAdapter(adapter1)	;	

			mBandOfInterestSpinner.setOnItemSelectedListener(new MyOnItemSelectedListener());    
			mBandOfInterestSpinner.setSelection(SharedPref.getInt(this, BioZenConstants.PREF_BAND_OF_INTEREST_REVIEW , 	
					mBandOfInterest));
		}
		

		
		generateChart(DIRECTION_NEXT);        
	}
	
	
	private Handler fileExportCompleteHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			// Hide the progress dialog.
			hideProgressDialog();

			if(msg.what == EXPORT_SUCCESS) {
				//onDataExported(exportFileUris);
			} else if(msg.what == EXPORT_FAILED) {
				//onDataExportFailed();
			}
		}
	};
	
	
	protected void showProgressDialog() {
		this.mProgressDialog.show();
	}

	protected void hideProgressDialog() {
		this.mProgressDialog.hide();
	}	

	
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		this.getMenuInflater().inflate(R.menu.menu_review, menu);
		return true;
	}    
    	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.itemCreatePdf) {
		
			if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
				showProgressDialog();
				CreatePdf();
			}
			else {
				AlertDialog.Builder alertWarning = new AlertDialog.Builder(this);
				alertWarning.setMessage("There is no SD card mounted, please insert SD card and try again");
				alertWarning.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				  public void onClick(DialogInterface dialog, int whichButton) {

				  }

				});

				alertWarning.show();			
				
			}
			return true;
		} 
		return true;

	}	
	
	static class SessionsKeyItem {
		public long id;
		public String title1;
		public String title2;
		public int color;
		public boolean visible;
		public boolean reverseData = false; 
		
		public SessionsKeyItem(long id, String title1, String title2) {
			this.id = id;
			this.title1 = title1;
			this.title2 = title2;
		}
		
		public SessionsKeyItem(long id, String title1, String title2, int color) {
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
	
	class SessionsKeyItemAdapter extends ArrayAdapter<SessionsKeyItem> {
		public static final int VIEW_TYPE_ONE_LINE = 1;
		public static final int VIEW_TYPE_TWO_LINE = 2;
		
		private LayoutInflater layoutInflater;
		private int layoutId;

		public SessionsKeyItemAdapter(Context context, int viewType,
				List<SessionsKeyItem> objects) {
			super(context, viewType, objects);
			
			layoutInflater = (LayoutInflater)context.getSystemService(LAYOUT_INFLATER_SERVICE);
			if(viewType == VIEW_TYPE_TWO_LINE) {
				layoutId = R.layout.list_item_result_key_2;
			} else {
				layoutId = R.layout.list_item_result_key_1;
			}
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if(convertView == null) {
				convertView = layoutInflater.inflate(layoutId, null);
			}
			final int buttonposition = position;			

			final SessionsKeyItem item = this.getItem(position);
			TextView tv1 = (TextView)convertView.findViewById(R.id.text1);
			TextView tv2 = (TextView)convertView.findViewById(R.id.text2);
			Button button = (Button) convertView.findViewById(R.id.buttonViewDetails);
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
	            	try {
	            		sessionKeyItems.get(mSelectedId);
	            		mBioSessionDao.delete(sessionItems.get(mSelectedId));	
	                	setupCalendars();            	
	            		updateListView();
	            		generateChart(DIRECTION_NEXT); 
						
					} catch (SQLException e) {
						Log.e(TAG, "Error deleting user" + e.toString());
					}
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
	 * Populates sessionItems and sessionKeyItems with session from the currently selected user
	 * then uses the adapter to populate the list view with that data
	 */
	private void updateListView() {

		// Retrieve the BuiUser object associated with object mSelectedUserName
		try {
			mBioUserDao = getHelper().getBioUserDao();
			mBioSessionDao = getHelper().getBioSessionDao();
			
			QueryBuilder<BioUser, Integer> builder = mBioUserDao.queryBuilder();
			builder.where().eq(BioUser.NAME_FIELD_NAME, mCurrentBioUserName);
			builder.limit(1);
			List<BioUser> list = mBioUserDao.query(builder.prepare());	
			
			if (list.size() >= 1) {
				mCurrentBioUser = list.get(0);
			}
			else {
				Log.e(TAG, "General Database error" + mCurrentBioUserName);
			}
			
		} catch (SQLException e) {
			Log.e(TAG, "Can't find user: " + mCurrentBioUserName , e);

		}        
        	
		// Fill the collections sessionItems, and sessionKeyItems with session data from the current user
		sessionItems.clear();
		sessionKeyItems.clear();
		long startTime = startCal.getTimeInMillis();
		long endTime = endCal.getTimeInMillis();	
		
		
		if (mCurrentBioUser != null) {
		
			for (BioSession session: mCurrentBioUser.getSessions()) {
				
				if (session.time >= startTime && session.time <= endTime ) {
					
					if (!mSelectedCategoryName.equalsIgnoreCase("all")) {
						// See if this item should be filtered out
						if (!session.category.equalsIgnoreCase(mSelectedCategoryName)) continue;
						
					}

					SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yy HH:mm:ss", Locale.US);
					String title = sdf.format(new Date(session.time));			
					try {
						String categoryInitial = "(" + session.category.charAt(0) + ")";
						title += categoryInitial;
					} catch (IndexOutOfBoundsException e) {
					}
					
					
					int color;
					if (session.precentComplete >= 100) {
						color = Color.GREEN;
					}
					else {
						color = Color.YELLOW;
					}
					
					SessionsKeyItem item = new SessionsKeyItem(1,title , "", color);
					sessionKeyItems.add(item);
					sessionItems.add(session);
					
				}
			}
		}				
	
		sessionKeysAdapter = new SessionsKeyItemAdapter(this, 1, sessionKeyItems);		
		sessionKeysList.setAdapter(sessionKeysAdapter);		
	}
	
	private void showSessionDetails() {
		AlertDialog.Builder alert2 = new AlertDialog.Builder(instance);
		
		BioSession session = sessionItems.get(mSelectedId);
		
		String sessionDetails = "";
		sessionDetails += "Completion: " + session.precentComplete + "%\n";
		sessionDetails += "Length: " + secsToHMS(session.secondsCompleted) + "\n";

		sessionDetails += "Background Parameter:   " + session.keyItemNames[mBandOfInterest] + "\n";
		sessionDetails += "   Min: " + session.minFilteredValue[mBandOfInterest] + "\n";
		sessionDetails += "   Max: " + session.maxFilteredValue[mBandOfInterest] + "\n";
		sessionDetails += "   Avg: " + session.avgFilteredValue[mBandOfInterest] + "\n";
		
		sessionDetails += "Comments: " + session.comments + "\n";
		sessionDetails += "Category: " + session.category+ "\n";
		sessionDetails += "Log file: " + session.logFileName+ "\n";
		
		alert2.setMessage(sessionDetails);
		alert2.show();
		
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
		case R.id.monthMinusButton:
			monthMinusButtonPressed();
			break;
			
		case R.id.monthPlusButton:
			monthPlusButtonPressed();
			break;

		case R.id.monthName:
			calendarResolutionButtonPressed();
			break;
		}
		
		
		
	}

	double getXValueBasedOnResolution(long sessionTime) {
		int i;
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(sessionTime);
		
		i = cal.get(calendarField);
		int day = cal.get(Calendar.DAY_OF_MONTH);
		int dayofWeek = cal.get(Calendar.DAY_OF_WEEK);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int minute = cal.get(Calendar.MINUTE);
		
		float value = 0;;

		switch (calendarField) {
		case Calendar.DAY_OF_MONTH:
			value = day + (float) hour/24 + (float) minute/60;
			break;

		case Calendar.HOUR_OF_DAY:
			value = (float) hour + (float) minute/60;
			break;

		default:
			break;
		}

		
		return (double) value;
		
	}
	
	
	private void generateChart(int direction) {
		XYMultipleSeriesDataset dataSet = new XYMultipleSeriesDataset();
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		
//		XYSeries minSeries = new XYSeries("minSeries");
		XYSeries avgSeries = new XYSeries("avgSeries");
//		XYSeries maxSeries = new XYSeries("maxSeries");
		
        LinearLayout layout = (LinearLayout) findViewById(R.id.deviceChart);    	
    	if (mDeviceChartView != null) {
    		layout.removeView(mDeviceChartView);
    	}

//		LineChart chart = new LineChart(dataSet, renderer);
//		RangeBarChart chart = new RangeBarChart(dataSet, renderer, BarChart.Type.DEFAULT);

    	
  //  	mDeviceChartView = new OffsetGraphicalChartView(this, chart);
     	
//     	mDeviceChartView = ChartFactory.getRangeBarChartView(this, dataSet, renderer, BarChart.Type.DEFAULT );
     	mDeviceChartView = ChartFactory.getLineChartView(this, dataSet, renderer);
     	layout.addView(mDeviceChartView, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
         	
		long startTime = startCal.getTimeInMillis();
		long endTime = endCal.getTimeInMillis();	
		
		double maxChartValue = 0;


		// Get the data points
		for (BioSession session : sessionItems) {
			if (session.time >= startTime && session.time <= endTime ) {
				double chartYMin = session.minFilteredValue[mBandOfInterest];
				double chartYAvg = session.avgFilteredValue[mBandOfInterest];
				double chartYMax = session.maxFilteredValue[mBandOfInterest];
				if (chartYMin > maxChartValue)  maxChartValue = chartYMin;
				if (chartYAvg > maxChartValue)  maxChartValue = chartYAvg;
				if (chartYMax > maxChartValue)  maxChartValue = chartYMax;
				
				double chartXValue = getXValueBasedOnResolution(session.time);
				
//				minSeries.add(chartXValue, chartYMin );
				avgSeries.add(chartXValue, chartYAvg );
//				maxSeries.add(chartXValue, chartYMax );
				
			}
		}
		
		
		
//		XYSeriesRenderer minSeriesRenderer = new XYSeriesRenderer();
//		minSeriesRenderer.setColor(Color.YELLOW);
//		minSeriesRenderer.setPointStyle(PointStyle.CIRCLE);
//		minSeriesRenderer.setFillPoints(false);
//		minSeriesRenderer.setLineWidth(0 * displayMetrics.density);
//		renderer.addSeriesRenderer(minSeriesRenderer);
//		dataSet.addSeries(minSeries);		
//		
		XYSeriesRenderer avgSeriesRenderer = new XYSeriesRenderer();
		avgSeriesRenderer.setColor(Color.RED);
		avgSeriesRenderer.setPointStyle(PointStyle.CIRCLE);
		avgSeriesRenderer.setFillPoints(true);
		avgSeriesRenderer.setLineWidth(2 * displayMetrics.density);
		renderer.addSeriesRenderer(avgSeriesRenderer);
		dataSet.addSeries(avgSeries);		

//		XYSeriesRenderer maxSeriesRenderer = new XYSeriesRenderer();
//		maxSeriesRenderer.setColor(Color.YELLOW);
//		maxSeriesRenderer.setPointStyle(PointStyle.CIRCLE);
//		maxSeriesRenderer.setFillPoints(false);
//		maxSeriesRenderer.setLineWidth(0 * displayMetrics.density);
//		renderer.addSeriesRenderer(maxSeriesRenderer);
//		dataSet.addSeries(maxSeries);		
		
		
		
		// only contine making the chart if there is data in the series.
		if(dataSet.getSeriesCount() > 0) {
			
			// Make the renderer for the weekend blocks
			Calendar weekendCal = Calendar.getInstance();
			weekendCal.setTimeInMillis(System.currentTimeMillis());
			
			
			int lastDayOfMonth = weekendCal.getActualMaximum(calendarField);
//			int lastDayOfMonth = weekendCal.getActualMaximum(Calendar.DAY_OF_MONTH);
			
			renderer.setShowGrid(false);
			renderer.setAxesColor(Color.WHITE);
			renderer.setLabelsColor(Color.WHITE);
			renderer.setAntialiasing(true);
			renderer.setShowLegend(false);
			renderer.setYLabels(0);
			renderer.setXLabels(15);
			renderer.setYAxisMax(maxChartValue);
			
			renderer.setYAxisMin(0.00);
			renderer.setXAxisMin(1.00);
			renderer.setXAxisMax(lastDayOfMonth);
			
			renderer.setZoomEnabled(true, false);
			renderer.setPanEnabled(true, false);
			renderer.setLegendHeight(10);
			
			renderer.setMargins(new int[] {0,5,10, 0});			
		}		
		
		
		
	} // End generateChart

	protected void monthMinusButtonPressed() {
		startCal.add(calendarFieldIncrement, -1);
		endCal.add(calendarFieldIncrement, -1);
		this.monthNameTextView.setText(monthNameFormatter.format(startCal.getTime()));
        updateListView();
		generateChart(DIRECTION_PREVIOUS);
	}
	
	protected void monthPlusButtonPressed() {
		startCal.add(calendarFieldIncrement, 1);
		endCal.add(calendarFieldIncrement, 1);
		this.monthNameTextView.setText(monthNameFormatter.format(startCal.getTime()));
        updateListView();
		generateChart(DIRECTION_NEXT);
	}

	void setupCalendars() {
    	long startTime = Calendar.getInstance().getTimeInMillis();
		
		startCal = Calendar.getInstance();
		startCal.setTimeInMillis(MathExtra.roundTime(startTime, calendarField));
		startCal.set(calendarField, startCal.getMinimum(calendarField));
		
		endCal = Calendar.getInstance();
		endCal.setTimeInMillis(startCal.getTimeInMillis());
		endCal.add(calendarFieldIncrement, 1);
    	
		monthNameTextView.setText(monthNameFormatter.format(startCal.getTime()));
		
	}
	
	void calendarResolutionButtonPressed() {
	    String[] items = {"Day of Month", "Hour of Day" };
		AlertDialog.Builder alert = new AlertDialog.Builder(instance);
		alert.setTitle("Set Calandar Resolution");
		alert.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

            	switch (which) {
            	case 0:
            		calendarField = Calendar.DAY_OF_MONTH;
            		break;
            	case 1:
            		calendarField = Calendar.HOUR_OF_DAY;
            		break;
            	}
        		
            	setCalendarResolution();
        		// We must reset the start and end calendars
            	setupCalendars();           
                updateListView();
            	
        		generateChart(DIRECTION_NEXT);        
            }		
        });				
		
		alert.show();		
	}
	
	void setCalendarResolution() {

		switch (calendarField) {
		case Calendar.DAY_OF_MONTH:
			calendarFieldIncrement = Calendar.MONTH; 
			monthNameFormatter = new SimpleDateFormat("MMMM, yyyy");
			
			break;
	
		case Calendar.HOUR_OF_DAY:
			monthNameFormatter = new SimpleDateFormat("dd-MMMM-yyyy");
			calendarFieldIncrement = Calendar.DATE; 
			break;
	
		default:
			break;
		}
		
		
	}
	



	private ArrayList<String> getCategories(String keySuffix) {
		String[] idsStrArr = SharedPref.getValues(
				sharedPref, 
				KEY_NAME+keySuffix, 
				",",
				new String[0]
		);
		
		return new ArrayList<String>(Arrays.asList(idsStrArr));
		
	}	
	
	public RegressionResult calculateRegression(ArrayList<RegressionItem> inArray)
	{
		RegressionResult result = new RegressionResult();

		int count = inArray.size();
		double sumY = 0.0;
		double sumX = 0.0;
		double sumXY = 0.0;
		double sumX2 = 0.0;
		double sumY2 = 0.0;

		for(int l=0;l<count;l++)
		{
			RegressionItem item = inArray.get(l);

			sumX += item.xValue;
			sumY += item.yValue;
			sumXY += (item.xValue * item.yValue);
			sumX2 += (item.xValue * item.xValue);
			sumY2 += (item.yValue * item.yValue);
		}

		result.slope = ((count * sumXY) - sumX * sumY) / ((count * sumX2) - (sumX * sumX));
		result.intercept = ((sumY - (result.slope * sumX))/count);
		result.correlation = Math.abs((count * sumXY) - (sumX * sumY)) / (Math.sqrt((count * sumX2 - sumX * sumX) * (count * sumY2 - (sumY * sumY))));

		return result;
	}	
	
	
	/**
	 * Create a PDF file based on the contents of the graph
	 */
	void CreatePdf() {
		
		// Run the export on a separate thread.
		new Thread(new Runnable() {
			@Override
			public void run() {
		
		
	        Document document = new Document();
	        try {
				Date calendar = Calendar.getInstance().getTime();
				String filename = "BioZenResults_" ;
				filename += (calendar.getYear() + 1900) + "-" + (calendar.getMonth() + 1) + "-" + calendar.getDate() + "_"; 
				filename += calendar.getHours()+ "-" + calendar.getMinutes() + "-" + calendar.getSeconds() + ".pdf"; 

	        	PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(android.os.Environment.getExternalStorageDirectory() + java.io.File.separator + filename));
	            document.open();
				PdfContentByte contentByte = writer.getDirectContent();
				BaseFont baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
				// Note top of PDF = 900
				float chartWidth = 332;
				float chartHeight = 45;

				float spaceHeight = chartHeight + 30;
				int horizontalPos = 180;
				float verticalPos = 780;	            
				
				// Write document header
				contentByte.beginText();
				contentByte.setFontAndSize(baseFont, 20);
				contentByte.showTextAligned(PdfContentByte.ALIGN_CENTER, "T2 BioZen Report", 300, 800, 0);
				contentByte.showTextAligned(PdfContentByte.ALIGN_CENTER, "Generated on: " + calendar.toLocaleString(), 300, 770, 0);
				contentByte.endText();				
	            
				contentByte.setLineWidth(1f);
				verticalPos -= spaceHeight;				
				long startTime = startCal.getTimeInMillis();
				long endTime = endCal.getTimeInMillis();	
				
				float maxChartValue = 0;	            
				float chartYAvg;

				BioSession tmpSession = sessionItems.get(0);
				int maxKeys = tmpSession.keyItemNames.length; 
				
				// Loop through all of the the keys
				for (int key = 0; key < maxKeys; key++) {
					
					//Draw a border rect
					contentByte.setRGBColorStrokeF(0,0,0);
					contentByte.setLineWidth(1f);
					contentByte.rectangle(horizontalPos, verticalPos, chartWidth, chartHeight);
					contentByte.stroke();

					// Write band name
					contentByte.beginText();
					contentByte.setFontAndSize(baseFont, 12);
					BioSession tmpSession1 = sessionItems.get(0);
					contentByte.showTextAligned(PdfContentByte.ALIGN_RIGHT, tmpSession1.keyItemNames[key], 170, (verticalPos+(chartHeight/2))-5, 0);
					contentByte.endText();		
					
					maxChartValue = 0;				
					// First find the max Y
					for (BioSession session : sessionItems) {
						if (session.time >= startTime && session.time <= endTime ) {
							chartYAvg = session.avgFilteredValue[key];
							if (chartYAvg > maxChartValue)  
								maxChartValue = chartYAvg;
						}
					}						
					
					float lastY = -1;
					float xIncrement = 0;
					if (sessionItems.size() > 0) {
						xIncrement = chartWidth / sessionItems.size();
					}
					
					float yIncrement = 0;
					if (maxChartValue > 0) {
						yIncrement = chartHeight / maxChartValue;
					}

					
					float highValue= 0;
					int highTime = 0;
					float highY = 0;
					float highX = 0;
					int lowTime = 0;
					float lowY = 100;
					float lowX = chartWidth;
					float lowValue = maxChartValue;					
					
					int lCount = 0;	
					String keyName = "";
					
					ArrayList<RegressionItem> ritems = new ArrayList<RegressionItem>();					
					
					// Loop through the session points of this key
					String rawYValues = "";
					for (BioSession session : sessionItems) {
						keyName = session.keyItemNames[key];
						if (session.time >= startTime && session.time <= endTime ) {
							chartYAvg = session.avgFilteredValue[key];
							rawYValues += chartYAvg + ", ";
							if(lastY < 0) 
								lastY = (float) chartYAvg;

							contentByte.setLineWidth(3f);
							contentByte.setRGBColorStrokeF(255,0,0);
							
							float graphXFrom  = horizontalPos + (lCount * xIncrement);
							float graphYFrom = verticalPos + (lastY * yIncrement);
							float graphXTo  = (horizontalPos + ((lCount + 1) * xIncrement));
							float graphYTo =  verticalPos + (chartYAvg * yIncrement);
							//							Log.e(TAG, "[" + graphXFrom + ", " + graphYFrom + "] to [" + graphXTo + ", " + graphYTo + "]");
							// Draw the actual graph
							contentByte.moveTo(graphXFrom, graphYFrom);
							contentByte.lineTo(graphXTo, graphYTo);
							contentByte.stroke();
							
							//Add regression Item
							ritems.add(new RegressionItem(lCount, (chartYAvg*yIncrement)));							
							
							
							if(chartYAvg > highValue)
							{
								highValue = chartYAvg;
								highY = graphYTo;
								highX = graphXTo;
								highTime = (int) (session.time / 1000);
							}

							if(chartYAvg < lowValue)
							{
								lowValue = chartYAvg;
								lowY = graphYTo;
								lowX = graphXTo;
								lowTime = (int) (session.time / 1000);
							}							
							
							
							lCount++;
							lastY = (float) chartYAvg;
							
							
						} // End if (session.time >= startTime && session.time <= endTime )
					} // End for (BioSession session : sessionItems)				

					//Draw high low dates
					if (highY != 0 && lowY != 0) {
						SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy");
						String hDate = dateFormat.format(new Date((long) highTime * 1000L));
						String lDate = dateFormat.format(new Date((long) lowTime * 1000L));
						contentByte.beginText();
						contentByte.setFontAndSize(baseFont, 8);
						contentByte.showTextAligned(PdfContentByte.ALIGN_CENTER, hDate, highX, highY, 0);
						contentByte.showTextAligned(PdfContentByte.ALIGN_CENTER, lDate, lowX, lowY, 0);
						contentByte.endText();						
					}
					
					//Draw Regression Line
					RegressionResult regression = calculateRegression(ritems);
					contentByte.saveState();
					contentByte.setRGBColorStrokeF(0,0,250);
					contentByte.setLineDash(3, 3, 0);
					contentByte.moveTo(horizontalPos,verticalPos+(float)regression.intercept);
					contentByte.lineTo(horizontalPos+chartWidth,(float) ((verticalPos+regression.intercept)+(float) (regression.slope * (chartWidth/xIncrement))));
					contentByte.stroke();
					contentByte.restoreState();
					contentByte.setRGBColorStrokeF(0,0,0);					
					
					
					//					Log.e(TAG, keyName + ": [" + rawYValues + "]");
					// Get ready for the next key (and series of database points )
					verticalPos -= spaceHeight;		
					
					if (verticalPos < 30) {
						document.newPage();
						verticalPos = 780 - spaceHeight;				
					}
			
				
				} // End for (int key = 0; key < maxKeys; key++)
				
				
				
	            
	            //document.add(new Paragraph("You can also write stuff directly tot he document like this!"));
	        } catch (DocumentException de) {
	                System.err.println(de.getMessage());
	                Log.e(TAG, de.toString());
	        } catch (IOException ioe) {
	                System.err.println(ioe.getMessage());
	                Log.e(TAG, ioe.toString());
	        } catch (Exception e) {
	            System.err.println(e.getMessage());
	            Log.e(TAG, e.toString());
	        }

	        // step 5: we close the document
	        document.close();  	
	        fileExportCompleteHandler.sendEmptyMessage(EXPORT_SUCCESS);			
			
		}
		}).start();

     
		
	}
	
	
	public class RegressionItem
	{
		public double xValue =0;
		public double yValue =0;

		RegressionItem(double xv, double yv)
		{
			xValue = xv;
			yValue = yv;
		}

	}

	public class RegressionResult
	{
		public double slope =0;
		public double intercept =0;
		public double correlation = 0;
	}	

	
	
}
