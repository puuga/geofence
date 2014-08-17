package com.appspace.geofence.ex;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.gson.Gson;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MyService extends Service implements
GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener {

	private static final String TAG = "MyService";

	private RoomlinkPlace roomlinkPlace[];

	private WebView webView;
	
	private LocationClient mLocationClient;
	// Global variable to hold the current location
	private Location mCurrentLocation;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "MyService onDestroy");
		
		mLocationClient.disconnect();
		Log.d(TAG, "mLocationClient.disconnect()");

		// stopInterval();
		webView.loadUrl("javascript:myStopFunction()");
		webView.loadUrl("about:blank");
		webView.destroy();
		webView = null;
	}

	@SuppressLint("SetJavaScriptEnabled") @Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onStartCommand MyService");
		
		mLocationClient = new LocationClient(this, this, this);
		
		mLocationClient.connect();
		Log.d(TAG, "mLocationClient.connect()");

		// read place
		roomlinkPlace = ApiManager.getRoomLinkPlace(this);
		Log.d("roomlink place", "count = " + roomlinkPlace.length);

		// startInterval();
		webView = new WebView(this);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.addJavascriptInterface((this), "Android");
		webView.setWebViewClient(new myWebClient());
		webView.loadUrl("file:///android_asset/www/index.html");
		

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onStart(Intent intent, int startid) {
		Log.d(TAG, " MyService onStart");

		// read place
		roomlinkPlace = ApiManager.getRoomLinkPlace(this);
		Log.d("roomlink place", "count = " + roomlinkPlace.length);

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
			Log.d(TAG, "urlnow=" + view.getUrl());
			Log.d(TAG, "getOriginalUrl=" + view.getOriginalUrl());
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
		Log.d(TAG, msg);

		Gson gson = new Gson();
		CustomLocation cLocation = new CustomLocation();
		cLocation.setAltitude(mCurrentLocation.getAltitude());
		cLocation.setLatitude(mCurrentLocation.getLatitude());
		cLocation.setLongitude(mCurrentLocation.getLongitude());
		final String temp = gson.toJson(cLocation);
		Log.d("Location Updates", "temp=" + temp);
		webView.loadUrl("javascript:getAndroidLocationFromPlayService('" + temp + "')");
			

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
		Log.d(TAG, "GooglePlayServices in MyService onConnectionFailed");
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		Log.d(TAG, "GooglePlayServices in MyService onConnected");
	}

	@Override
	public void onDisconnected() {
		Log.d(TAG, "GooglePlayServices in MyService onDisconnected");
	}

}
