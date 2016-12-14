package com.stc.meetme;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationResult;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.stc.meetme.model.UserPosition;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.stc.meetme.Constants.FIELD_DB_USER_POSITION;
import static com.stc.meetme.Constants.TABLE_DB_USER_STATUSES;

/**
 * Asynchronously handles an intent using a worker thread. Receives a ResultReceiver object and a
 * location through an intent. Tries to fetch the address for the location using a Geocoder, and
 * sends the result to the ResultReceiver.
 */
public class DetectedLocationIntentService extends IntentService {
    private static final String TAG = "FetchAddressIS";

    /**
     * The receiver where results are forwarded from this service.
     */
    protected ResultReceiver mReceiver;
	private SharedPreferences prefs;

	private DatabaseReference mFirebaseDatabaseReference;

	private String currentUserId;
    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public DetectedLocationIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }

	@Override
	public void onCreate() {
		super.onCreate();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
		currentUserId = prefs.getString(Constants.SETTINGS_MY_UID, null);
	}


	@Override
    protected void onHandleIntent(Intent intent) {
		Location location;

		LocationResult locationResult = LocationResult
				.extractResult(intent);

		LocationAvailability locationAvailability=LocationAvailability
				.extractLocationAvailability(intent);

		if(locationResult==null) {
			Set<String> keys=null;
			if(intent.getExtras()!=null ) keys=intent.getExtras().keySet();
			if(keys!=null){
				for(String key: keys) Log.e(TAG, "key: "+key+" value: "+intent.getExtras().get(key) );
			}
		}else {
			//Log.w(TAG, "isLocationAvailable " + locationAvailability.isLocationAvailable());
			Log.w(TAG, "hasResult " + LocationResult.hasResult(intent));

			if (LocationResult.hasResult(intent)) {
				location = locationResult.getLastLocation();
				if (location != null) {
					Log.d("locationtesting", "accuracy: " + location.getAccuracy() + " lat: " + location.getLatitude() + " lon: " + location.getLongitude());

					UserPosition userPosition = new UserPosition();
					userPosition.setLat(location.getLatitude());
					userPosition.setLng(location.getLongitude());
					userPosition.setTimestamp(location.getTime());
					userPosition.setAccuracy(location.getAccuracy());

					Geocoder geocoder = new Geocoder(this, Locale.getDefault());
					String errorMessage = "";
					List<Address> addresses = null;
					try {
						addresses = geocoder.getFromLocation(
								location.getLatitude(),
								location.getLongitude(),
								1);
					} catch (IOException ioException) {
						errorMessage = getString(R.string.service_not_available);
						Log.e(TAG, errorMessage, ioException);
					} catch (IllegalArgumentException illegalArgumentException) {
						errorMessage = getString(R.string.invalid_lat_long_used);
						Log.e(TAG, errorMessage + ". " +
								"Latitude = " + location.getLatitude() +
								", Longitude = " + location.getLongitude(), illegalArgumentException);
					}
					if (addresses == null || addresses.size() == 0) {
						if (errorMessage.isEmpty()) {
							errorMessage = getString(R.string.no_address_found);
							Log.e(TAG, errorMessage);
						}
					} else {
						Address address = addresses.get(0);
						String addressLines = "";
						for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
							addressLines += address.getAddressLine(i) + " ";
						}
						userPosition.setPlaceAddress(addressLines);
						//userPosition.setPlaceName(address.getPremises());
					}
					mFirebaseDatabaseReference.child(TABLE_DB_USER_STATUSES).child(currentUserId)
							.child(FIELD_DB_USER_POSITION).setValue(userPosition);
					return;
				} else Log.e(TAG, "locationResult ERROR no location ");
			}
		}

		/*if (location == null) {
            errorMessage = getString(R.string.no_location_data_provided);
            Log.wtf(TAG, errorMessage);
	        mFirebaseDatabaseReference.child(TABLE_DB_USER_STATUSES).child(currentUserId)
			        .child(FIELD_DB_USER_POSITION).setValue(null);
	        return;
        }*/





    }

}
