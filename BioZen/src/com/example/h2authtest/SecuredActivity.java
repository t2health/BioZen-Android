
package com.example.h2authtest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.t2auth.AuthUtils;
import com.t2auth.AuthUtils.T2LogoutTask;
import com.t2auth.AuthUtils.T2ServiceTicketTask;

import com.t2.R;


public class SecuredActivity extends ListActivity {

    public static final String H2_HOST = "ec2-54-245-170-242.us-west-2.compute.amazonaws.com:8081";
    public static final String H2_QUERY = H2_HOST + "/query?dbname=test&colname=h2_test&limit=20";
    public static final String H2_INSERT = H2_HOST + "/write?dbname=test&colname=h2_test";

    private T2ServiceTicketTask mServiceTicketTask = null;
    private H2ListTask mListTask = null;
    private H2PostTask mPostTask = null;
    private T2LogoutTask mLogoutTask = null;

    private H2ResultsAdapter mAdapter;
    private String mPostMessage = null;
    private Context myContext = null;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myContext = this;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.activity_secured, menu);
        return true;
    }

    private void logout() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(SecuredActivity.this);
        prefs.edit().remove(getString(R.string.pref_tgt)).commit();

        finish();
 //       startActivity(new Intent(this, LoginActivity.class));
    }

    private void updateList(JSONArray source) {
        final List<H2Result> results = new ArrayList<SecuredActivity.H2Result>();

        for (int i = 0; i < source.length(); i++) {
            try {
                H2Result result = new H2Result(source.getJSONObject(i));
                results.add(result);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        mAdapter = new H2ResultsAdapter(this, results);
        setListAdapter(mAdapter);
    }

    private void refreshList() {
        if (mListTask == null) {
            mListTask = new H2ListTask(AuthUtils.getServiceTicket(this));
            mListTask.execute((Void) null);
        }
    }

    private void createEntry() {
    	mPostMessage = null;
		AlertDialog.Builder alert1 = new AlertDialog.Builder(this);

		alert1.setMessage("Enter message to add");

		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert1.setView(input);

		alert1.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			mPostMessage = input.getText().toString();
	        if (mPostTask == null) {
	            mPostTask = new H2PostTask(AuthUtils.getServiceTicket(myContext), AuthUtils.getUsername(myContext));
	            mPostTask.execute((Void) null);
	        }

		  }
		});

		alert1.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		        if (mPostTask == null) {
		            mPostTask = new H2PostTask(AuthUtils.getServiceTicket(myContext), AuthUtils.getUsername(myContext));
		            mPostTask.execute((Void) null);
		        }
			  
		  }
		});

		alert1.show();	  

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        switch (item.getItemId()) {
            case R.id.menu_logout:
                if (mLogoutTask == null) {
                    mLogoutTask = new T2LogoutTask(AuthUtils.getSslContext(this).getSocketFactory(),
                            AuthUtils.getTicketGrantingTicket(this)) {

                        @Override
                        protected void onLogoutSuccess() {
                            logout();
                        }

                        @Override
                        protected void onLogoutFailed() {
                            logout();
                        }
                    };
                    mLogoutTask.execute((Void) null);
                }
                return true;
            case R.id.menu_delete_tgt:
                prefs.edit().remove(getString(R.string.pref_tgt)).remove(getString(R.string.pref_st)).commit();
                return true;
            case R.id.menu_post_data:
                createEntry();
                return true;
            case R.id.menu_request_data:
                refreshList();
                return true;
            case R.id.menu_request_st:
                if (mServiceTicketTask == null) {
                    prefs.edit().remove(getString(R.string.pref_st)).commit();
                    mServiceTicketTask = new T2ServiceTicketTask(AuthUtils.APPLICATION_NAME,
                            AuthUtils.getTicketGrantingTicket(this),
                            AuthUtils.getSslContext(this).getSocketFactory()) {
                        @Override
                        protected void onTicketRequestSuccess(String serviceTicket) {
                            Toast.makeText(SecuredActivity.this, "Service Ticket Granted - " + serviceTicket,
                                    Toast.LENGTH_SHORT).show();
                            SharedPreferences prefs = PreferenceManager
                                    .getDefaultSharedPreferences(SecuredActivity.this);
                            prefs.edit().putString(getString(R.string.pref_st), serviceTicket).commit();
                            mServiceTicketTask = null;
                        }

                        @Override
                        protected void onTicketRequestFailed() {
                            logout();
                        }
                    };

                    mServiceTicketTask.execute((Void) null);
                }
                return true;
        }
        return super.onOptionsItemSelected(item);

    }

    private static class H2Result {
        private String mId;
        private String mUserId;
        private long mTime;
        private String mPostData;

        public H2Result(JSONObject source) throws JSONException {
            if (source.has("RECORDED_TIME")) {
                mTime = source.getLong("RECORDED_TIME");
            } else {
                mTime = 0;
            }

            JSONObject data = source.getJSONObject("APPDATA");
            if (data.has("_id")) {
                mId = data.getString("_id");
            } else {
                mId = "N/A";
            }

            if (data.has("user_id")) {
                mUserId = data.getString("user_id");
            } else {
                mUserId = "Unknown";
            }
            
            if (data.has("post_data")) {
                mPostData = data.getString("post_data");
            } else {
            	mPostData = "";
            }            
        }

        public String getPostData() {
            return mPostData;
        }

        public String getId() {
            return mId;
        }

        public String getUserId() {
            return mUserId;
        }

        public long getTime() {
            return mTime;
        }

    }

    private static class H2ResultsAdapter extends ArrayAdapter<H2Result> {
        private LayoutInflater mInf;

        public H2ResultsAdapter(Context context, List<H2Result> results) {
            super(context, 0, results);
            mInf = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;

            if (v == null) {
                v = mInf.inflate(R.layout.h2_result_row, null);
            }

            final TextView postData = (TextView) v.findViewById(R.id.post_data);
            final TextView result = (TextView) v.findViewById(R.id.result);
            final TextView time = (TextView) v.findViewById(R.id.time);
            final H2Result item = getItem(position);
            postData.setText(item.getPostData());
            result.setText(item.getId());
            time.setText(item.getUserId() + " at " + new Date(item.getTime()).toString());

            return v;
        }

    }

    private class H2PostTask extends AsyncTask<Void, Void, JSONArray> {

        private static final String TAG = "H2PostTask";

        private String mServiceTicket;
        private String mUsername;

        public H2PostTask(String serviceTicket, String username) {
            super();
            mServiceTicket = serviceTicket;
            mUsername = username;
        }

        @Override
        protected JSONArray doInBackground(Void... vals) {

            HttpURLConnection conn = null;

            try {
                if (mServiceTicket == null) {
                    Log.d(TAG, "::doInBackground:" + "No Service Ticket");
                    return null;
                }
                
                URL url = new URL("http://" + H2_INSERT + "&st=" + mServiceTicket);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PUT");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                OutputStream out = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));

                if (mPostMessage != null) {
                    writer.write("{'RECORDED_TIME':" + System.currentTimeMillis() + ", 'APPDATA':{" +
                    		"'post_data': '" + mPostMessage  + "', " +
                    		"'user_id': '" + mUsername  + "', " +
                    		"'_id':'" + UUID.randomUUID().toString() + "'}}");
                }
                else {
                    writer.write("{'RECORDED_TIME':" + System.currentTimeMillis() + ", 'APPDATA':{" +
                    		"'user_id': '" + mUsername  + "', " +
                    		"'_id':'" + UUID.randomUUID().toString() + "'}}");
                }
                writer.close();
                out.close();

                if (conn.getResponseCode() == 201) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String response = reader.readLine();
                    Log.d(TAG, "::doInBackground:" + response);

                    return null;
                } else {
                    Log.d(TAG, "::doInBackground:" + "Insert Failed");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(final JSONArray response) {
            mPostTask = null;
            refreshList();
        }
    }

    private class H2ListTask extends AsyncTask<Void, Void, JSONArray> {

        private static final String TAG = "H2ListTask";

        private String mServiceTicket;

        public H2ListTask(String serviceTicket) {
            super();
            mServiceTicket = serviceTicket;
        }

        @Override
        protected JSONArray doInBackground(Void... vals) {

            HttpURLConnection conn = null;

            try {
                if (mServiceTicket == null) {
                    Log.d(TAG, "::doInBackground:" + "No Service Ticket");
                    return null;
                }

                URL url = new URL("http://" + H2_QUERY + "&st=" + mServiceTicket);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setDoInput(true);

                OutputStream out = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                writer.write("{}\n{'RECORDED_TIME':-1}");
                writer.close();
                out.close();

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                    String response = reader.readLine();

                    return new JSONArray(response);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(final JSONArray response) {

            if (response != null) {
                AuthUtils.clearServiceTicket(SecuredActivity.this);
            }

            if (response == null) {
                Log.d(TAG, "::onPostExecute:" + "H2 Query Failed\n");
            } else {
                updateList(response);
            }

            mListTask = null;
        }
    }

    
    
    
}
