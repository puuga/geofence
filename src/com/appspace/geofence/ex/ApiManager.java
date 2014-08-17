package com.appspace.geofence.ex;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import com.google.gson.Gson;

public class ApiManager {
	public static RoomlinkPlace[] getRoomLinkPlace(Context context) {
		//AssetManager manager = context.getAssets();
		RoomlinkPlace roomlinkPlaces[];
		String temp = null;
		try {
			InputStream is = context.getAssets().open("locationData/placejson.json");
			int size = is.available();
			byte[] buffer = new byte[size];
			is.read(buffer);
			is.close();
			temp = new String(buffer, "UTF-8");
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}

		Gson gson = new Gson();
		roomlinkPlaces = gson.fromJson(temp, RoomlinkPlace[].class);
		//System.out.println("" + roomlinkPlaces.length);
		return roomlinkPlaces;
	}
}
