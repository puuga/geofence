package com.appspace.geofence.ex;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class autostart extends BroadcastReceiver {

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		Log.i("Autostart", "starting");

		// Intent intent = new Intent(arg0, MyService.class);
		// arg0.startService(intent);

		Intent app = new Intent(arg0, MainActivity.class);
		app.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		app.putExtra("EXIT", true);
		arg0.startActivity(app);

		Log.i("Autostart", "started");
	}

}
