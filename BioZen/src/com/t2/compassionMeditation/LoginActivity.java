
package com.t2.compassionMeditation;

//import android.animation.Animator;
//import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.t2auth.AuthUtils;
import com.t2auth.AuthUtils.T2AuthenticateTask;
import com.t2auth.AuthUtils.T2ServiceTicketTask;

import com.t2.R;


/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends Activity {

    /**
     * The default email to populate the email field with.
     */
    public static final String EXTRA_EMAIL = "com.example.android.authenticatordemo.extra.EMAIL";

	/**
	 * Static instance of this activity
	 */
	private static LoginActivity mInstance;
    
    
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private T2AuthenticateTask mT2AuthTask = null;

    private T2ServiceTicketTask mServiceTicketTask = null;

    // Values for email and password at the time of the login attempt.
    private String mEmail;
    private String mPassword;

    // UI references.
    private EditText mEmailView;
    private EditText mPasswordView;
    private View mLoginFormView;
    private View mLoginStatusView;
    private TextView mLoginStatusMessageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mInstance = this;
        
        // Start out clean with no ticket granting ticket - ensures we always do login
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
        prefs.edit().remove(getString(R.string.pref_tgt)).commit();
        
        // Set up the login form.
        mEmail = getIntent().getStringExtra(EXTRA_EMAIL);
        mEmailView = (EditText) findViewById(R.id.email);
        mEmailView.setText(mEmail);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (savedInstanceState == null) {
            mEmailView.setText(prefs.getString(getString(R.string.pref_last_user), ""));
            if (mEmailView.getText().length() > 0) {
                mPasswordView.requestFocus();
            }
        }

        mLoginFormView = findViewById(R.id.login_form);
        mLoginStatusView = findViewById(R.id.login_status);
        mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

        if (AuthUtils.getTicketGrantingTicket(this) != null) {
            attemptServiceTicketRequest();
        } else {
            mLoginFormView.setVisibility(View.VISIBLE);
        }

        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
//        getMenuInflater().inflate(R.menu.activity_login, menu);
        return true;
    }

    public void attemptServiceTicketRequest() {
        if (mServiceTicketTask != null) {
            return;
        }

        mLoginStatusMessageView.setText("Requesting a Service Ticket...");
        showProgress(true);
        mServiceTicketTask = new T2ServiceTicketTask(AuthUtils.APPLICATION_NAME,
                AuthUtils.getTicketGrantingTicket(LoginActivity.this),
                AuthUtils.getSslContext(LoginActivity.this).getSocketFactory()) {
            @Override
            protected void onTicketRequestSuccess(String serviceTicket) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
                prefs.edit().putString(getString(R.string.pref_st), serviceTicket).commit();

                
                finish();
//                startActivity(new Intent(LoginActivity.this, SecuredActivity.class));
                
                
                
            }

            @Override
            protected void onTicketRequestFailed() {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
                prefs.edit().remove(getString(R.string.pref_tgt)).commit();
                
                mServiceTicketTask = null;
                showProgress(false);
                mLoginFormView.setVisibility(View.VISIBLE);
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                mServiceTicketTask = null;
                showProgress(false);
                mLoginFormView.setVisibility(View.VISIBLE);
            }
        };
        mServiceTicketTask.execute((Void) null);
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mT2AuthTask != null) {
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
        prefs.edit().remove(getString(R.string.pref_tgt)).commit();

        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        mEmail = mEmailView.getText().toString();
        mPassword = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(mPassword)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        } else if (mPassword.length() < 4) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
            showProgress(true);
            mT2AuthTask = new T2AuthenticateTask(AuthUtils.getSslContext(LoginActivity.this).getSocketFactory(),
                    mEmail, mPassword) {
                @Override
                protected void onAuthenticationFailed() {
                    mT2AuthTask = null;
                    showProgress(false);
//                    Toast.makeText(LoginActivity.this, "Invalid username or password.", Toast.LENGTH_SHORT).show();
            		AlertDialog.Builder alert1 = new AlertDialog.Builder(mInstance);

            		alert1.setTitle("Invalid username or password");
            		
            		alert1.setPositiveButton("Retry", new DialogInterface.OnClickListener() {
            		public void onClick(DialogInterface dialog, int whichButton) {
            		  }
            		});

            		alert1.show();	    	
                    
                }

                @Override
                protected void onAuthenticationSuccess(String ticketGrantingTicket) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(LoginActivity.this);
                    prefs.edit()
                            .putString(getString(R.string.pref_tgt), ticketGrantingTicket)
                            .putString(getString(R.string.pref_last_user), mEmail)
                            .commit();
//                    attemptServiceTicketRequest();
                    
                    
                    loginSuccess();
                    
                    
                }

                @Override
                protected void onCancelled() {
                    super.onCancelled();
                    mT2AuthTask = null;
                    showProgress(false);
                }
            };
            mT2AuthTask.execute((Void) null);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
//    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
//    private void showProgress(final boolean show) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
//            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
//
//            // mLoginStatusView.setVisibility(View.VISIBLE);
//            mLoginStatusView.animate()
//                    .setDuration(shortAnimTime)
//                    .alpha(show ? 1 : 0)
//                    .setListener(new AnimatorListenerAdapter() {
//                        @Override
//                        public void onAnimationEnd(Animator animation) {
//                            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
//                        }
//                    });
//
//            // mLoginFormView.setVisibility(View.VISIBLE);
//            mLoginFormView.animate()
//                    .setDuration(shortAnimTime)
//                    .alpha(show ? 0 : 1)
//                    .setListener(new AnimatorListenerAdapter() {
//                        @Override
//                        public void onAnimationEnd(Animator animation) {
//                            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
//                        }
//                    });
//        } else {
//            // The ViewPropertyAnimator APIs are not available, so simply show
//            // and hide the relevant UI components.
//            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
//            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
//        }
//    }

    /**
     * Shows the progress UI and hides the login form.
     */
//    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // The ViewPropertyAnimator APIs are not available, so simply show
        // and hide the relevant UI components.
        mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
        mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
    }
    
    void loginSuccess() {
		AlertDialog.Builder alert1 = new AlertDialog.Builder(this);

		alert1.setTitle("Successfully loged in\n" + 
		"Data will now be logged to H2 Server");
		
		alert1.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			finish();
		  }
		});

		alert1.show();	    	
    }
    
    
}
