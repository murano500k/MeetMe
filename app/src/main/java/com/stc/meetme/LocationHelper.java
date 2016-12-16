package com.stc.meetme;

import com.google.android.gms.location.LocationRequest;

import static com.stc.meetme.Constants.FASTEST_INTERVAL;
import static com.stc.meetme.Constants.UPDATE_INTERVAL;

/**
 * Created by artem on 12/16/16.
 */

public class LocationHelper {
	public static LocationRequest getLocationRequest(){
		return LocationRequest.create()
				.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
				.setInterval(UPDATE_INTERVAL)
				.setFastestInterval(FASTEST_INTERVAL)
				.setSmallestDisplacement(10);
	}
}
