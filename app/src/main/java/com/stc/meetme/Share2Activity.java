package com.stc.meetme;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.ui.IconGenerator;
import com.stc.meetme.model.ModelUserActivity;
import com.stc.meetme.model.ModelUserPosition;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static com.stc.meetme.Constants.INTENT_ACTION_START_MY_UPDATES;
import static com.stc.meetme.Constants.INTENT_ACTION_STOP_MY_UPDATES;
import static com.stc.meetme.Constants.INTENT_EXTRA_GAPI_STATUS;
import static com.stc.meetme.Constants.INTENT_EXTRA_MY_ACTIVITY;
import static com.stc.meetme.Constants.INTENT_EXTRA_MY_POSITION;
import static com.stc.meetme.Constants.INTENT_EXTRA_MY_UPDATES_STATUS;
import static com.stc.meetme.Constants.INTENT_EXTRA_OBSERVE_UID;
import static com.stc.meetme.Constants.PERMISSION_REQUEST_FINE_LOCATION;

public class Share2Activity extends AppCompatActivity implements OnMapReadyCallback{
	public UpdatesReciever receiverForTest;
	boolean gApiStatus;
	boolean myUpdatesStatus;
	@BindView(R.id.status)
	TextView status;
	private String  myUid;
	private Toast toast;
	private int activityResId;
	private int statusColorId;

	protected GoogleMap mMap;
	private PolylineOptions polylineOptions;
	Polyline polyline;
	private Marker marker;
	private Circle circle;

	FloatingActionButton fab;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_share2);
		ButterKnife.bind(this);

		fab=(FloatingActionButton) findViewById(R.id.fab2);
		fab.setEnabled(true);
		status=(TextView) findViewById(R.id.status);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Timber.w(" ");
				if(myUpdatesStatus) registerUpdates();
				else onStartService();
			}
		});
		setupServiceReceiver();
		activityResId=R.drawable.ic_not_available;
		statusColorId=IconGenerator.STYLE_DEFAULT;
		onStartService();
	}
	public void registerUpdates(){
		Timber.w(" ");
		if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
				PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
					PERMISSION_REQUEST_FINE_LOCATION);
		}else {
			Intent i = new Intent(this, MeetMeService.class);
			i.setAction(INTENT_ACTION_START_MY_UPDATES);
			startService(i);
		}
	}
	public  void removeUpdates(){
		Timber.w(" ");
		Intent i = new Intent(this, MeetMeService.class);
		i.setAction(INTENT_ACTION_STOP_MY_UPDATES);
		startService(i);
	}
	public void onStartService() {
		Timber.w(" ");
		setupServiceReceiver();
		Intent i = new Intent(this, MeetMeService.class);
		i.putExtra("receiver", receiverForTest);
		startService(i);
	}

	// Setup the callback for when data is received from the service
	public void setupServiceReceiver() {
		receiverForTest = new UpdatesReciever(new Handler());
		// This is where we specify what happens when data is received from the service
		receiverForTest.setReceiver(new UpdatesReciever.Receiver() {
			@Override
			public void onReceiveResult(int resultCode, Bundle resultData) {
				if (resultCode != RESULT_OK) {
					Toast.makeText(Share2Activity.this, "ERROR", Toast.LENGTH_SHORT).show();
				}
				if(resultData!=null){
					updateStatus(resultData);
					if(resultData.getBoolean(INTENT_EXTRA_GAPI_STATUS)){
						if(mMap==null) initMap();
						else updateUiData(resultData);
					}
				}
			}
		});
	}

	private void initMap() {
		Timber.w(" ");
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);

	}

	@Override
	public void onMapReady(GoogleMap googleMap) {
		Timber.w(" ");

		mMap=googleMap;
		polylineOptions=new PolylineOptions();
		polylineOptions.color(getColor(android.R.color.holo_blue_light));
		fab.setEnabled(true);
		fab.setVisibility(View.VISIBLE);

	}

	private void updateUiData(Bundle resultData) {
		Timber.w(" ");
		if(resultData.containsKey(INTENT_EXTRA_MY_ACTIVITY)){
			ModelUserActivity activity= (ModelUserActivity) resultData.getSerializable(INTENT_EXTRA_MY_ACTIVITY);
			if(activity!=null) {
				switch (activity.getFirstActivityCode()){
					case DetectedActivity.ON_FOOT:
						activityResId=R.drawable.ic_activity_onfoot;
						break;
					case DetectedActivity.ON_BICYCLE:
						activityResId=R.drawable.ic_activity_bike;
						break;
					case DetectedActivity.RUNNING:
						activityResId=R.drawable.ic_activity_run;
						break;
					case DetectedActivity.IN_VEHICLE:
						activityResId=R.drawable.ic_activity_vehicle;
						break;
					case DetectedActivity.TILTING:
						activityResId=R.drawable.ic_activity_tilt;
						break;
					case DetectedActivity.UNKNOWN:
						activityResId=R.drawable.ic_unknown;
						break;
					case DetectedActivity.STILL:
						activityResId=R.drawable.ic_activity_idle;
						break;
					default:
						activityResId=R.drawable.ic_not_available;
				}
			}
		}
		if(resultData.containsKey(INTENT_EXTRA_MY_POSITION)){
			ModelUserPosition position= (ModelUserPosition) resultData.getSerializable(INTENT_EXTRA_MY_POSITION);

			if(position!=null) {
				LatLng ll=new LatLng(position.getLat(), position.getLng());
				String address=position.getPlaceAddress();
				String time=position.formatDateTime();
				updateMap(ll, address, time);
			}
		}
	}

	BitmapDescriptor createIcon(){
		IconGenerator iconGenerator = new IconGenerator(this);
		iconGenerator.setStyle(statusColorId);
		iconGenerator.setBackground(getDrawable(activityResId));
		return BitmapDescriptorFactory.fromBitmap(iconGenerator.makeIcon());
	}
	public void updateMap(LatLng pos, String address, String time){
		Timber.w("position: %s, %s, %s", pos.toString(), address, time);
		polylineOptions.add(pos);
		CircleOptions circleOptions=new CircleOptions();
		circleOptions.center(pos);
		MarkerOptions markerOptions = new MarkerOptions();
		markerOptions.icon(createIcon());
		markerOptions.title(time);
		markerOptions.snippet(address);
		markerOptions.position(pos);
		markerOptions.flat(false);

		if(marker!=null) marker.remove();
		if(polyline!=null) polyline.remove();
		if(circle!=null) circle.remove();

		marker = mMap.addMarker(markerOptions);
		polyline=mMap.addPolyline(polylineOptions);
		circle=mMap.addCircle(circleOptions);
	}

	protected void showToast(String text) {
		Log.w("OBSERVE", text);
		if(toast!=null )toast.cancel();
		toast=Toast.makeText(this, text, Toast.LENGTH_SHORT);
		toast.show();
	}
	private void updateStatus(Bundle resultData) {
		boolean lastGAPIStatus=gApiStatus;
		boolean lastUpdatesStatus=myUpdatesStatus;
		String lastUid=myUid;

		gApiStatus=resultData.getBoolean(INTENT_EXTRA_GAPI_STATUS);
		myUpdatesStatus=resultData.getBoolean(INTENT_EXTRA_MY_UPDATES_STATUS);
		myUid=resultData.getString(INTENT_EXTRA_OBSERVE_UID);
		if(gApiStatus!=lastGAPIStatus)	{
			showToast("Gapi status = "+gApiStatus);
		}
		if(myUpdatesStatus!=lastUpdatesStatus)	showToast("myUpdatesStatus = "+myUpdatesStatus);
		if(myUid!=null && !myUid.equals(lastUid))	showToast( "UiD updated: "+myUid);
		//fab.setEnabled(gApiStatus);

		if(gApiStatus) {
			fab.setEnabled(true);
			fab.setImageResource(myUpdatesStatus ? R.drawable.ic_deactivate :R.drawable.ic_activate );
			statusColorId=myUpdatesStatus ? IconGenerator.STYLE_GREEN : IconGenerator.STYLE_RED;
			fab.setBackgroundColor(getColor(myUpdatesStatus ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));
		}else {
			statusColorId=IconGenerator.STYLE_DEFAULT;
			status.setTextColor(getColor(android.R.color.tertiary_text_dark));
		}
	}


	@Override
	public void onRequestPermissionsResult(int requestCode,
	                                       String permissions[], int[] grantResults) {
		switch (requestCode) {
			case PERMISSION_REQUEST_FINE_LOCATION: {
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					registerUpdates();

					Timber.w(" GRANTED");
				} else {
					Timber.e("NOT GRANTED");
				}
			}
		}
	}



}
