package com.stc.meetme;

import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import static com.stc.meetme.Constants.INTENT_EXTRA_OBSERVE_LAT;
import static com.stc.meetme.Constants.INTENT_EXTRA_OBSERVE_LNG;
import static com.stc.meetme.Constants.SETTINGS_OBSERVE_UID;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

	private static final String TAG = "MyMaps";
	private GoogleMap mMap;
	LatLng observeLatLng;
	String observeUid;
	Marker marker;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_maps);
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		Log.w(TAG, "onCreate:" );

		if(getIntent()!=null && getIntent().getExtras()!=null){
			Log.e(TAG, "onCreate: ERROR" );
			if( getIntent().getExtras().containsKey(INTENT_EXTRA_OBSERVE_LNG)
					&& getIntent().getExtras().containsKey(INTENT_EXTRA_OBSERVE_LAT)){
				double lat, lng;
				lat = getIntent().getExtras().getDouble(INTENT_EXTRA_OBSERVE_LAT);
				lng = getIntent().getExtras().getDouble(INTENT_EXTRA_OBSERVE_LNG);
				observeLatLng = new LatLng(lat,lng);
				observeUid= PreferenceManager.getDefaultSharedPreferences(this)
						.getString(SETTINGS_OBSERVE_UID, null);
				mapFragment.getMapAsync(this);
				return;
			}
		}
		Toast.makeText(this, "ERROR", Toast.LENGTH_SHORT).show();
		Log.e(TAG, "onCreate: ERROR" );
		//finish();
	}



	/**
	 * Manipulates the map once available.
	 * This callback is triggered when the map is ready to be used.
	 * This is where we can add markers or lines, add listeners or move the camera. In this case,
	 * we just add a marker near Sydney, Australia.
	 * If Google Play services is not installed on the device, the user will be prompted to install
	 * it inside the SupportMapFragment. This method will only be triggered once the user has
	 * installed Google Play services and returned to the app.
	 */
	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;
		if(observeLatLng!=null){
			marker=mMap.addMarker(new MarkerOptions().position(observeLatLng).title(observeUid));

			mMap.moveCamera(CameraUpdateFactory.newLatLng(observeLatLng));
			//mMap.moveCamera(CameraUpdateFactory.zoomIn());
		}

	}
}
