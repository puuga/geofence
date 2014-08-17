/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.appspace.geofence.ex;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.appspace.geofence.ex.R;
import com.appspace.geofence.ex.GeofenceUtils.REMOVE_TYPE;
import com.appspace.geofence.ex.GeofenceUtils.REQUEST_TYPE;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.gson.Gson;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * UI handler for the Location Services Geofence sample app. Allow input of
 * latitude, longitude, and radius for two geofences. When registering
 * geofences, check input and then send the geofences to Location Services. Also
 * allow removing either one of or both of the geofences. The menu allows you to
 * clear the screen or delete the geofences stored in persistent memory.
 */
public class MainActivity extends FragmentActivity implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener {
	/*
	 * Use to set an expiration time for a geofence. After this amount of time
	 * Location Services will stop tracking the geofence. Remember to unregister
	 * a geofence when you're finished with it. Otherwise, your app will use up
	 * battery. To continue monitoring a geofence indefinitely, set the
	 * expiration time to Geofence#NEVER_EXPIRE.
	 */
	static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;
	static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = GEOFENCE_EXPIRATION_IN_HOURS
			* DateUtils.HOUR_IN_MILLIS;

	// Store the current request
	private REQUEST_TYPE mRequestType;

	// Store the current type of removal
	private REMOVE_TYPE mRemoveType;

	// Persistent storage for geofences
	private SimpleGeofenceStore mPrefs;

	// Store a list of geofences to add
	List<Geofence> mCurrentGeofences;

	// Add geofences handler
	private GeofenceRequester mGeofenceRequester;

	// Remove geofences handler
	private GeofenceRemover mGeofenceRemover;

	// decimal formats for latitude, longitude, and radius
	private DecimalFormat mLatLngFormat;
	private DecimalFormat mRadiusFormat;

	/*
	 * An instance of an inner class that receives broadcasts from listeners and
	 * from the IntentService that receives geofence transition events
	 */
	private GeofenceSampleReceiver mBroadcastReceiver;

	// An intent filter for the broadcast receiver
	private IntentFilter mIntentFilter;

	// Store the list of geofences to remove
	private List<String> mGeofenceIdsToRemove;

	private RoomlinkPlace roomlinkPlace[];

	private String webURL = "http://beta.roomlinksaas.com";

	private WebView webView;

	private String tagname = "roomlinksbeta";

	// Global variable to hold the current location
	private LocationClient mLocationClient;
	private Location mCurrentLocation;
	
	// wifi manager
	WifiManager wifi;
	int size = 0;
    List<ScanResult> results;
    String ITEM_KEY = "key";
    ArrayList<HashMap<String, String>> arraylist = new ArrayList<HashMap<String, String>>();
    

	Intent myService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set the pattern for the latitude and longitude format
		String latLngPattern = getString(R.string.lat_lng_pattern);

		// Set the format for latitude and longitude
		mLatLngFormat = new DecimalFormat(latLngPattern);

		// Localize the format
		mLatLngFormat.applyLocalizedPattern(mLatLngFormat.toLocalizedPattern());

		// Set the pattern for the radius format
		String radiusPattern = getString(R.string.radius_pattern);

		// Set the format for the radius
		mRadiusFormat = new DecimalFormat(radiusPattern);

		// Localize the pattern
		mRadiusFormat.applyLocalizedPattern(mRadiusFormat.toLocalizedPattern());

		// Create a new broadcast receiver to receive updates from the listeners
		// and service
		mBroadcastReceiver = new GeofenceSampleReceiver();

		// Create an intent filter for the broadcast receiver
		mIntentFilter = new IntentFilter();

		// Action for broadcast Intents that report successful addition of
		// geofences
		mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_ADDED);

		// Action for broadcast Intents that report successful removal of
		// geofences
		mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_REMOVED);

		// Action for broadcast Intents containing various types of geofencing
		// errors
		mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_ERROR);

		// All Location Services sample apps use this category
		mIntentFilter.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);

		// Instantiate a new geofence storage area
		mPrefs = new SimpleGeofenceStore(this);

		// Instantiate the current List of geofences
		mCurrentGeofences = new ArrayList<Geofence>();

		// Instantiate a Geofence requester
		mGeofenceRequester = new GeofenceRequester(this);

		// Instantiate a Geofence remover
		mGeofenceRemover = new GeofenceRemover(this);

		// Attach to the main UI
		setContentView(R.layout.activity_main);

		// Get handles to the Geofence editor fields in the UI
		bindWidget();

		// read place
		roomlinkPlace = ApiManager.getRoomLinkPlace(getApplicationContext());
		Log.d("roomlink place", "count = " + roomlinkPlace.length);

		//
		setMPrefs();

		// start mornitor auto
		if (getIntent().getBooleanExtra("startFromNoti", false)) {

		} else {
			startGeofencesMonitor();
		}

		/*
		 * Create a new location client, using the enclosing class to handle
		 * callbacks.
		 */
		mLocationClient = new LocationClient(this, this, this);

		// create service
		/*
		if (myService == null) {
			myService = new Intent(this, MyService.class);
		}
		startService(myService);
		*/

		if (getIntent().getBooleanExtra("EXIT", false)) {
			finish();
		}
		
		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled() == false) {
            Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled", Toast.LENGTH_LONG).show();
            wifi.setWifiEnabled(true);
        }
        
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent intent) 
            {
               results = wifi.getScanResults();
               size = results.size();
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

	}

	private void setMPrefs() {
		for (int i = 0; i < roomlinkPlace.length; i++) {
			SimpleGeofence sGeofence = new SimpleGeofence("" + (i + 1),
					roomlinkPlace[i].getLatitude(),
					roomlinkPlace[i].getLongitude(),
					roomlinkPlace[i].getRadius(),
					// Set the expiration time
					GEOFENCE_EXPIRATION_IN_MILLISECONDS,
					// Only detect entry transitions
					Geofence.GEOFENCE_TRANSITION_ENTER
							| Geofence.GEOFENCE_TRANSITION_EXIT);

			// Store this flat version in SharedPreferences
			mPrefs.setGeofence("" + (i + 1), sGeofence);
			Log.d("setMPrefs", "add roomlinkPlace to mPrefs id" + (1 + i));
		}

	}

	@SuppressLint("SetJavaScriptEnabled")
	private void bindWidget() {

		// bind webview
		webView = (WebView) findViewById(R.id.webView1);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.setWebViewClient(new myWebClient());
		webView.addJavascriptInterface((this), "Android");
		webView.loadUrl(webURL);
	}

	/*
	 * Handle results returned to this Activity by other Activities started with
	 * startActivityForResult(). In particular, the method onConnectionFailed()
	 * in GeofenceRemover and GeofenceRequester may call
	 * startResolutionForResult() to start an Activity that handles Google Play
	 * services problems. The result of this call returns here, to
	 * onActivityResult. calls
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		// Choose what to do based on the request code
		switch (requestCode) {

		// If the request code matches the code sent in onConnectionFailed
		case GeofenceUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST:

			switch (resultCode) {
			// If Google Play services resolved the problem
			case Activity.RESULT_OK:

				// If the request was to add geofences
				if (GeofenceUtils.REQUEST_TYPE.ADD == mRequestType) {

					// Toggle the request flag and send a new request
					mGeofenceRequester.setInProgressFlag(false);

					// Restart the process of adding the current geofences
					mGeofenceRequester.addGeofences(mCurrentGeofences);

					// If the request was to remove geofences
				} else if (GeofenceUtils.REQUEST_TYPE.REMOVE == mRequestType) {

					// Toggle the removal flag and send a new removal request
					mGeofenceRemover.setInProgressFlag(false);

					// If the removal was by Intent
					if (GeofenceUtils.REMOVE_TYPE.INTENT == mRemoveType) {

						// Restart the removal of all geofences for the
						// PendingIntent
						mGeofenceRemover
								.removeGeofencesByIntent(mGeofenceRequester
										.getRequestPendingIntent());

						// If the removal was by a List of geofence IDs
					} else {

						// Restart the removal of the geofence list
						mGeofenceRemover
								.removeGeofencesById(mGeofenceIdsToRemove);
					}
				}
				break;

			// If any other result was returned by Google Play services
			default:

				// Report that Google Play services was unable to resolve the
				// problem.
				Log.d(GeofenceUtils.APPTAG, getString(R.string.no_resolution));
			}

			// If any other request code was received
		default:
			// Report that this Activity received an unknown requestCode
			Log.d(GeofenceUtils.APPTAG,
					getString(R.string.unknown_activity_request_code,
							requestCode));

			break;
		}
	}

	/*
	 * Whenever the Activity resumes, reconnect the client to Location Services
	 * and reload the last geofences that were set
	 */
	@Override
	protected void onResume() {
		super.onResume();
		// Register the broadcast receiver to receive status updates
		LocalBroadcastManager.getInstance(this).registerReceiver(
				mBroadcastReceiver, mIntentFilter);
		/*
		 * Get existing geofences from the latitude, longitude, and radius
		 * values stored in SharedPreferences. If no values exist, null is
		 * returned.
		 */
		// mUIGeofence1 = mPrefs.getGeofence("1");
		// mUIGeofence2 = mPrefs.getGeofence("2");
		/*
		 * If the returned geofences have values, use them to set values in the
		 * UI, using the previously-defined number formats.
		 */
		// if (mUIGeofence1 != null) {
		// mLatitude1
		// .setText(mLatLngFormat.format(mUIGeofence1.getLatitude()));
		// mLongitude1.setText(mLatLngFormat.format(mUIGeofence1
		// .getLongitude()));
		// mRadius1.setText(mRadiusFormat.format(mUIGeofence1.getRadius()));
		// }
		// if (mUIGeofence2 != null) {
		// mLatitude2
		// .setText(mLatLngFormat.format(mUIGeofence2.getLatitude()));
		// mLongitude2.setText(mLatLngFormat.format(mUIGeofence2
		// .getLongitude()));
		// mRadius2.setText(mRadiusFormat.format(mUIGeofence2.getRadius()));
		// }
	}

	/*
	 * Inflate the app menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu, menu);
		return true;

	}

	/*
	 * Respond to menu item selections
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {

		// Remove all geofences from storage
		case R.id.menu_item_clear_geofence_history:
			if (getNumberOfMPrefs() == 0) {
				return true;
			}
			// start at 1
			for (int i = 1; i <= getNumberOfMPrefs(); i++) {
				mPrefs.clearGeofence("" + i);
			}
			// mPrefs.clearGeofence("1");
			// mPrefs.clearGeofence("2");

			return true;

			// view all geofences from storage limit:100
		case R.id.menu_item_view_geofences:
			Log.d("count mPrefs", "mPrefs count = " + getNumberOfMPrefs());
			Log.d("count mCurrentGeofences", "mCurrentGeofences count = "
					+ mCurrentGeofences.size());

			return true;

			// view all geofences from storage limit:100
		case R.id.menu_item_start_geofences_monitor:
			startGeofencesMonitor();
			return true;

			// Pass through any other request

		case R.id.menu_item_stop_MyService:
			//stopService(myService);
			return true;

		case R.id.menu_item_start_MyService:
			/*
			// create service
			if (myService == null) {
				myService = new Intent(this, MyService.class);
			}
			startService(myService);
			*/
			return true;
			
		case R.id.menu_item_scanWifi:
			wifi.startScan();

	        Toast.makeText(this, "Scanning...." + size, Toast.LENGTH_SHORT).show();
	        ArrayList<WifiData> wifiData = new ArrayList<WifiData>();
	        try {
	            size = size - 1;
	            while (size >= 0) {   
	                HashMap<String, String> item1 = new HashMap<String, String>();                       
	                item1.put(ITEM_KEY, results.get(size).SSID + "  " + results.get(size).capabilities);
	                
	                String temp = "SSID:" + results.get(size).SSID;
	                temp += " BSSID:" + results.get(size).BSSID;
	                temp += " capabilities:" + results.get(size).capabilities;
	                temp += " level:" + results.get(size).level;
	                temp += " frequency:" + results.get(size).frequency;
	                
	                Log.d("wifi result", temp);
	                
	                WifiData temp2 = new WifiData();
	                temp2.setSsid(results.get(size).SSID);
	                temp2.setBSsid(results.get(size).BSSID);
	                temp2.setCapabilities(results.get(size).capabilities);
	                temp2.setLevel(results.get(size).level);
	                temp2.setFrequency(results.get(size).frequency);
	                wifiData.add(temp2);
	                
	                arraylist.add(item1);
	                size--;                
	            } 
	        }
	        catch (Exception e) {
	        	e.printStackTrace();
	        }
	        
	        Gson gson = new Gson();
	        final String temp = gson.toJson(wifiData);
	        Log.d("wifi json", temp);
	        
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	int getNumberOfMPrefs() {
		int i = 1;
		for (i = 0; i < 100; i++) {
			SimpleGeofence temp = mPrefs.getGeofence("" + (i + 1));
			if (temp == null) {
				break;
			}
		}
		// Log.d("count mPrefs", "mPrefs count = " + i);
		return i;
	}

	/*
	 * Save the current geofence settings in SharedPreferences.
	 */
	@Override
	protected void onPause() {
		super.onPause();
		// mPrefs.setGeofence("1", mUIGeofence1);
		// mPrefs.setGeofence("2", mUIGeofence2);
	}

	/**
	 * Verify that Google Play services is available before making a request.
	 * 
	 * @return true if Google Play services is available, otherwise false
	 */
	private boolean servicesConnected() {

		// Check that Google Play services is available
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);

		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {

			// In debug mode, log the status
			Log.d(GeofenceUtils.APPTAG,
					getString(R.string.play_services_available));

			// Continue
			return true;

			// Google Play services was not available for some reason
		} else {

			// Display an error dialog
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode,
					this, 0);
			if (dialog != null) {
				ErrorDialogFragment errorFragment = new ErrorDialogFragment();
				errorFragment.setDialog(dialog);
				errorFragment.show(getSupportFragmentManager(),
						GeofenceUtils.APPTAG);
			}
			return false;
		}
	}

	/**
	 * Called when the user clicks the "Remove geofences" button
	 * 
	 * @param view
	 *            The view that triggered this callback
	 */
	public void onUnregisterByPendingIntentClicked(View view) {
		removeAllGeofences();

	}

	void removeAllGeofences() {
		/*
		 * Remove all geofences set by this app. To do this, get the
		 * PendingIntent that was added when the geofences were added and use it
		 * as an argument to removeGeofences(). The removal happens
		 * asynchronously; Location Services calls
		 * onRemoveGeofencesByPendingIntentResult() (implemented in the current
		 * Activity) when the removal is done
		 */

		/*
		 * Record the removal as remove by Intent. If a connection error occurs,
		 * the app can automatically restart the removal if Google Play services
		 * can fix the error
		 */
		// Record the type of removal
		mRemoveType = GeofenceUtils.REMOVE_TYPE.INTENT;

		/*
		 * Check for Google Play services. Do this after setting the request
		 * type. If connecting to Google Play services fails, onActivityResult
		 * is eventually called, and it needs to know what type of request was
		 * in progress.
		 */
		if (!servicesConnected()) {

			return;
		}

		// Try to make a removal request
		try {
			/*
			 * Remove the geofences represented by the currently-active
			 * PendingIntent. If the PendingIntent was removed for some reason,
			 * re-create it; since it's always created with FLAG_UPDATE_CURRENT,
			 * an identical PendingIntent is always created.
			 */
			mGeofenceRemover.removeGeofencesByIntent(mGeofenceRequester
					.getRequestPendingIntent());

		} catch (UnsupportedOperationException e) {
			// Notify user that previous request hasn't finished.
			Toast.makeText(this,
					R.string.remove_geofences_already_requested_error,
					Toast.LENGTH_LONG).show();
		}
	}

	void startGeofencesMonitor() {
		if (getNumberOfMPrefs() == 0) {
			Log.d("message", "getNumberOfMPrefs() == 0, setMPrefs()");
			setMPrefs();
		}
		if (mCurrentGeofences.size() == 0) {
			Log.d("message",
					"mCurrentGeofences.size() == 0, add mPrefs to mCurrentGeofences");
			for (int i = 1; i <= getNumberOfMPrefs(); i++) {
				// mPrefs.getGeofence(""+i);
				SimpleGeofence temp = mPrefs.getGeofence("" + i);
				mCurrentGeofences.add(temp.toGeofence());
				Log.d("message", "mPrefs to mCurrentGeofences id" + i);
			}
		}

		// Start the request. Fail if there's already a request in progress
		try {
			// Try to add geofences
			mGeofenceRequester.addGeofences(mCurrentGeofences);
			Log.d("message", "Monitor Success");
		} catch (UnsupportedOperationException e) {
			// Notify user that previous request hasn't finished.
			Toast.makeText(this,
					R.string.add_geofences_already_requested_error,
					Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Define a Broadcast receiver that receives updates from connection
	 * listeners and the geofence transition service.
	 */
	public class GeofenceSampleReceiver extends BroadcastReceiver {
		/*
		 * Define the required method for broadcast receivers This method is
		 * invoked when a broadcast Intent triggers the receiver
		 */
		@Override
		public void onReceive(Context context, Intent intent) {

			// Check the action code and determine what to do
			String action = intent.getAction();

			// Intent contains information about errors in adding or removing
			// geofences
			if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_ERROR)) {

				handleGeofenceError(context, intent);

				// Intent contains information about successful addition or
				// removal of geofences
			} else if (TextUtils.equals(action,
					GeofenceUtils.ACTION_GEOFENCES_ADDED)
					|| TextUtils.equals(action,
							GeofenceUtils.ACTION_GEOFENCES_REMOVED)) {

				handleGeofenceStatus(context, intent);

				// Intent contains information about a geofence transition
			} else if (TextUtils.equals(action,
					GeofenceUtils.ACTION_GEOFENCE_TRANSITION)) {

				handleGeofenceTransition(context, intent);

				// The Intent contained an invalid action
			} else {
				Log.e(GeofenceUtils.APPTAG,
						getString(R.string.invalid_action_detail, action));
				Toast.makeText(context, R.string.invalid_action,
						Toast.LENGTH_LONG).show();
			}
		}

		/**
		 * If you want to display a UI message about adding or removing
		 * geofences, put it here.
		 * 
		 * @param context
		 *            A Context for this component
		 * @param intent
		 *            The received broadcast Intent
		 */
		private void handleGeofenceStatus(Context context, Intent intent) {

		}

		/**
		 * Report geofence transitions to the UI
		 * 
		 * @param context
		 *            A Context for this component
		 * @param intent
		 *            The Intent containing the transition
		 */
		private void handleGeofenceTransition(Context context, Intent intent) {
			/*
			 * If you want to change the UI when a transition occurs, put the
			 * code here. The current design of the app uses a notification to
			 * inform the user that a transition has occurred.
			 */
		}

		/**
		 * Report addition or removal errors to the UI, using a Toast
		 * 
		 * @param intent
		 *            A broadcast Intent sent by ReceiveTransitionsIntentService
		 */
		private void handleGeofenceError(Context context, Intent intent) {
			String msg = intent
					.getStringExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS);
			Log.e(GeofenceUtils.APPTAG, msg);
			Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
		}
	}

	/**
	 * Define a DialogFragment to display the error dialog generated in
	 * showErrorDialog.
	 */
	public static class ErrorDialogFragment extends DialogFragment {

		// Global field to contain the error dialog
		private Dialog mDialog;

		/**
		 * Default constructor. Sets the dialog field to null
		 */
		public ErrorDialogFragment() {
			super();
			mDialog = null;
		}

		/**
		 * Set the dialog to display
		 * 
		 * @param dialog
		 *            An error dialog
		 */
		public void setDialog(Dialog dialog) {
			mDialog = dialog;
		}

		/*
		 * This method must return a Dialog to the DialogFragment.
		 */
		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return mDialog;
		}
	}

	private class myWebClient extends WebViewClient {
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			super.onPageStarted(view, url, favicon);
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			if (url.startsWith("tel:")) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				startActivity(intent);
				return true;
			}
			view.loadUrl(url);
			return true;

		}

		@Override
		public void onPageFinished(WebView view, String url) {
			Log.d(tagname, "urlnow=" + view.getUrl());
			Log.d(tagname, "getOriginalUrl=" + view.getOriginalUrl());
			super.onPageFinished(view, url);
		}
	}

	@JavascriptInterface
	public void getLocationFromPlayService() {

		// Get the current location's latitude & longitude
		mCurrentLocation = mLocationClient.getLastLocation();

		String msg = "Current Location: "
				+ Double.toString(mCurrentLocation.getLatitude()) + ","
				+ Double.toString(mCurrentLocation.getLongitude());
		Log.d(tagname, msg);

		Gson gson = new Gson();
		CustomLocation cLocation = new CustomLocation();
		cLocation.setAltitude(mCurrentLocation.getAltitude());
		cLocation.setLatitude(mCurrentLocation.getLatitude());
		cLocation.setLongitude(mCurrentLocation.getLongitude());
		final String temp = gson.toJson(cLocation);
		Log.d("Location Updates", "temp=" + temp);

		MainActivity.this.runOnUiThread(new Runnable() {
			public void run() {
				webView.loadUrl("javascript:getAndroidLocationFromPlayService('"
						+ temp + "')");
			}
		});

	}
	
	@JavascriptInterface
	public void getWifiData() {
		wifi.startScan();
		
        Log.d("wifi result", "Scanning...." + size);
        ArrayList<WifiData> wifiData = new ArrayList<WifiData>();
        try {
            size = size - 1;
            while (size >= 0) {   
                HashMap<String, String> item1 = new HashMap<String, String>();                       
                item1.put(ITEM_KEY, results.get(size).SSID + "  " + results.get(size).capabilities);
                
                WifiData temp = new WifiData();
                temp.setSsid(results.get(size).SSID);
                temp.setBSsid(results.get(size).BSSID);
                temp.setCapabilities(results.get(size).capabilities);
                temp.setLevel(results.get(size).level);
                temp.setFrequency(results.get(size).frequency);
                
                Log.d("wifi result", temp.toString());
                wifiData.add(temp);
                
                arraylist.add(item1);
                size--;                
            } 
        }
        catch (Exception e) {
        	e.printStackTrace();
        }
        
        // return to javascript
        Gson gson = new Gson();
        final String temp = gson.toJson(wifiData);
        Log.d("wifi json", temp);
        MainActivity.this.runOnUiThread(new Runnable() {
			public void run() {
				webView.loadUrl("javascript:getWifiDataFromAndroid('"
						+ temp + "')");
			}
		});
	}

	@JavascriptInterface
	public void showNoti(String title, String content) {
		Log.d("show noti", "show noti:" + title + "," + content);
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
				this).setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(title).setContentText(content);
		Notification notification = mBuilder.build();
		notification.defaults = Notification.DEFAULT_ALL;
		// Sets an ID for the notification
		int mNotificationId = 001;
		// Gets an instance of the NotificationManager service
		NotificationManager mNotifyMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		// Builds the notification and issues it.
		mNotifyMgr.notify(mNotificationId, notification);
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		Log.d(tagname, "onConnectionFailed");

	}

	@Override
	public void onConnected(Bundle connectionHint) {
		Log.d(tagname, "onConnected");

	}

	@Override
	public void onDisconnected() {
		Log.d(tagname, "onDisconnected");

	}

	/*
	 * Called when the Activity becomes visible.
	 */
	@Override
	protected void onStart() {
		super.onStart();
		// Connect the client.
		mLocationClient.connect();
		Log.d(tagname, "mLocationClient.connect()");
	}

	/*
	 * Called when the Activity is no longer visible.
	 */
	@Override
	protected void onStop() {
		// Disconnecting the client invalidates it.
		mLocationClient.disconnect();
		Log.d(tagname, "mLocationClient.disconnect()");
		super.onStop();
	}

}
