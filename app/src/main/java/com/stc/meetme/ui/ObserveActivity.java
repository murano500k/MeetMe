package com.stc.meetme.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.deeplinkdispatch.DeepLink;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.google.maps.android.ui.IconGenerator;
import com.stc.meetme.R;
import com.stc.meetme.model.ModelUserActivity;
import com.stc.meetme.model.ModelUserPosition;

import java.util.ArrayList;

import butterknife.ButterKnife;
import timber.log.Timber;

import static android.view.Menu.NONE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.stc.meetme.Constants.FIELD_DB_USER_ACTIVITY;
import static com.stc.meetme.Constants.INTENT_EXTRA_OBSERVE_LAT;
import static com.stc.meetme.Constants.INTENT_EXTRA_OBSERVE_UID;
import static com.stc.meetme.Constants.SETTINGS_MY_UID;
import static com.stc.meetme.Constants.SETTINGS_OBSERVE_UID;
import static java.lang.System.currentTimeMillis;


@DeepLink("https://f3x9u.app.goo.gl/observe/{id}")
public class ObserveActivity extends AppCompatActivity implements OnMapReadyCallback, ObserveInfoProvider {
	protected static final String TAG = "ObserveActivity";
	public static final String ACTION_DEEP_LINK_COMPLEX = "deep_link_complex";


	LinearLayout mLayoutNoTarget;

	ProgressBar mProgressBar;

	TextView textPosition;

	TextView textTimeActivity;

	TextView textTimePlaces;

	TextView textActivity;

	protected MenuItem menuSharePosition, menuRefresh;

	protected MenuItem menuMap;

	protected ActionBar actionBar;

	protected GoogleMap mMap;

	protected Marker marker;

	protected FloatingActionButton fab;

	String observableUserId;

	String positionString;

	String activityString;

	private SharedPreferences prefs;
	private ChildEventListener positionChildUpdateListener;
	private String currentUserId;
	private CountDownTimer timer;

	protected static final int MENU_MAP = 312;
	protected static final int MENU_SHARE = 959;
	protected static final int MENU_REFRESH = 958;
	private double observeLat;
	private double observeLng;
	private boolean mapReady=false;
	private ArrayList<Marker> markers;
	private ValueEventListener activityUpdatesListener;
	private PolylineOptions polylineOptions;
	Polyline polyline;
	private Marker prevMarker;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_observe);
		ButterKnife.bind(this);
		textPosition=(TextView) findViewById(R.id.textViewPosition);
		textTimeActivity =(TextView) findViewById(R.id.textViewTimeActivity);
		textTimePlaces =(TextView) findViewById(R.id.textViewTimePlaces);
		textActivity=(TextView) findViewById(R.id.textViewActivity);
		fab=(FloatingActionButton) findViewById(R.id.floatingActionButton);
		mProgressBar=(ProgressBar) findViewById(R.id.progressBar);
		mLayoutNoTarget=(LinearLayout) findViewById(R.id.layoutNoTarget);
		observeLat=observeLng=0;
		markers=new ArrayList<>();
		actionBar = getSupportActionBar();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		currentUserId=prefs.getString(SETTINGS_MY_UID, null);
		checkIntent();
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(R.id.map);
		mapFragment.getMapAsync(this);

		fab.setVisibility(VISIBLE);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				removeDbUpdates();
			}
		});

	}
	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;
		mMap.setIndoorEnabled(true);
		polylineOptions=new PolylineOptions();
		polyline=mMap.addPolyline(polylineOptions);
	}


	private void checkIntent(){
		Intent intent=getIntent();
		if (intent!=null && intent.getExtras()!=null) {
			if (intent.getBooleanExtra(DeepLink.IS_DEEP_LINK, false)) {
				Bundle parameters = intent.getExtras();
				observableUserId = parameters.getString("id");
				Log.w(TAG, "checkIntent: isDeepLink");
			} else {
				Bundle parameters = intent.getExtras();
				observableUserId = parameters.getString(INTENT_EXTRA_OBSERVE_UID);
				Log.w(TAG, "checkIntent: isRegularIntent");
			}
		}

		if(observableUserId==null)
			observableUserId=PreferenceManager.getDefaultSharedPreferences(this)
					.getString(SETTINGS_OBSERVE_UID, null);


		if(observableUserId==null) {
			showToast("NO OBSERVABLE USERID");
			mLayoutNoTarget.setVisibility(VISIBLE);
			mProgressBar.setVisibility(GONE);
		}else {
			PreferenceManager.getDefaultSharedPreferences(this)
					.edit().putString(SETTINGS_OBSERVE_UID, observableUserId).apply();
		}
	}


	@Override
	protected void onStart() {
		super.onStart();


	}

	@Override
	protected void onStop() {
		super.onStop();
	}


	@Override
	protected void onPause() {
		Log.d(TAG, "onPause: removeDbUpdates");
		super.onPause();
	}
	/*
		@Override
		protected void onDestroy() {
			apiHelper.disconnect();
			super.onDestroy();

		}
	*/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(1,MENU_MAP, NONE,"Show map");
		menu.add(1,MENU_SHARE, NONE,"Share position");
		menu.add(1,MENU_REFRESH, NONE,"Refresh");
		return super.onCreateOptionsMenu(menu);

	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG, "onOptionsItemSelected: "+item.getItemId());
		if(MENU_MAP==item.getItemId()) {
			if(observeLat!=-1 && observeLng!=-1) {
				removeDbUpdates();
				Intent intent=new Intent(this, ObserveActivity.class);
				intent.putExtra(INTENT_EXTRA_OBSERVE_LAT,observeLat);
				//intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}else {
				showToast("ERROR lat:"+observeLat+", lng:"+ observeLng);
			}
		}else if(MENU_REFRESH==item.getItemId()) {
			removeDbUpdates();
			if (observableUserId != null && currentUserId != null)
				registerDbUpdates(observableUserId);
		}else if(MENU_SHARE==item.getItemId()) {
			Intent intent=new Intent(this, ShareActivity.class);
			startActivity(intent);
		}
		return super.onOptionsItemSelected(item);
	}

	protected void showToast(String text) {
		Log.w("OBSERVE", text);
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}

	public void updateValue(TextView text){
		String newValueStr="no data";
		String currentValueStr = text.getText().toString();
		int currentValue=-1;
		try {
			currentValue = Integer.valueOf(currentValueStr);
		}catch (NumberFormatException e){

		}
		if (currentValue != -1) {
			Integer newValue = currentValue + 1;
			newValueStr = newValue.toString();
		}
		text.setText(newValueStr);
	}
	private void scheduleElapsedTimer(){
		Log.d(TAG, "scheduleElapsedTimer: "+timer );
		final long totalMilliseconds = 100000000000L;
		long intervalMilliseconds = 1000;
		if(timer==null) {
			timer = new CountDownTimer(totalMilliseconds, intervalMilliseconds) {
				public void onTick(long millisUntilFinished) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							updateValue(textTimeActivity);
							updateValue(textTimePlaces);
						}
					});
				}

				public void onFinish() {
				}
			};
		}
		timer.start();

	}



	private void cancelElapsedTimer(){
		Log.d(TAG, "cancelElapsedTimer "+timer);
		if(timer!=null) timer.cancel();
		textTimeActivity.setText("elapsed: no info");
		textTimePlaces.setText("elapsed: no info");
	}

	public void registerDbUpdates(String observableUserId){
		Timber.w("registerDbUpdates uid: %s", observableUserId);
		if (actionBar != null) {
			actionBar.setHomeButtonEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle("Observing "+observableUserId);
		}
		scheduleElapsedTimer();
		mProgressBar.setVisibility(VISIBLE);
	}

	public void removeDbUpdates(){
		Timber.w("removeEventListener");
		cancelElapsedTimer();
		mProgressBar.setVisibility(GONE);
	}

	public void highlightTrace(){
		if(markers==null || markers.isEmpty()) return;
		  polylineOptions=new PolylineOptions();
		for(Marker marker1 : markers) {
			polylineOptions.add(marker1.getPosition());
			marker1.remove();
		}
		markers=new ArrayList<>();
		polylineOptions.color(R.color.wallet_hint_foreground_holo_dark);
		polylineOptions.geodesic(true);
		mMap.addPolyline(polylineOptions);

	}
	BitmapDescriptor createTextIcon(String title){
		IconGenerator iconGenerator = new IconGenerator(this);

// Possible color options:
// STYLE_WHITE, STYLE_RED, STYLE_BLUE, STYLE_GREEN, STYLE_PURPLE, STYLE_ORANGE
		iconGenerator.setStyle(IconGenerator.STYLE_GREEN);
// Swap text here to live inside speech bubble
		Bitmap bitmap = iconGenerator.makeIcon(title);
// Use BitmapDescriptorFactory to create the marker
		return BitmapDescriptorFactory.fromBitmap(bitmap);
	}


	public void addMarker(double observeLat, double observeLng, String timestamp){
		if(observeLng!=0 && observeLat!=0 && timestamp!=null) {
			if(prevMarker!=null) {
				polylineOptions.add(prevMarker.getPosition());
				if(polyline!=null) polyline.remove();
				polyline=mMap.addPolyline(polylineOptions);
				prevMarker.remove();
			}

			Log.d(TAG, "addMarker: lat:"+observeLat+" lng:"+observeLng);
			LatLng observeLatLng= new LatLng(observeLat,observeLng);
			prevMarker = mMap.addMarker(new MarkerOptions()
					.position(observeLatLng)
					//.title(timestamp)
					.icon(createTextIcon(timestamp))
					.snippet(observableUserId));
			mMap.moveCamera(CameraUpdateFactory.newLatLng(observeLatLng));
			mMap.animateCamera(CameraUpdateFactory.zoomTo(17), 2000, null);
		}else Log.e(TAG, "addMarker: ERROR");
	}

	@Override
	public ChildEventListener getChildPositionListener(){
	if(positionChildUpdateListener==null) positionChildUpdateListener=new ChildEventListener() {
		@Override
		public void onChildAdded(DataSnapshot dataSnapshot, String s) {
			ModelUserPosition modelUserPosition = dataSnapshot.getValue(ModelUserPosition.class);
			if (modelUserPosition != null) {
				Timber.w("marker: %s", modelUserPosition.formatDateTime());
				positionString = modelUserPosition.getPlaceAddress() + ", ";
				positionString += modelUserPosition.getAccuracy() + "m";
				if(modelUserPosition.getTimestamp()>1) {
					textTimePlaces.setText( modelUserPosition.formatDateTime());
				}else textTimePlaces.setText("no data");
				addMarker(modelUserPosition.getLat(), modelUserPosition.getLng(), modelUserPosition.formatDateTime());
			}
			textPosition.setText(positionString);
		}
		@Override
		public void onChildChanged(DataSnapshot dataSnapshot, String s) {

		}

		@Override
		public void onChildRemoved(DataSnapshot dataSnapshot) {
			ModelUserPosition modelUserPosition = dataSnapshot.getValue(ModelUserPosition.class);
			if (modelUserPosition != null) {
				if(modelUserPosition.getTimestamp()>1) {
					for(Marker marker: markers){
						if(marker.getPosition().latitude== modelUserPosition.getLat()
								&& marker.getPosition().longitude== modelUserPosition.getLng() ){
							marker.remove();
						}
					}
				}
			}
		}

		@Override
		public void onChildMoved(DataSnapshot dataSnapshot, String s) {

		}

		@Override
		public void onCancelled(DatabaseError databaseError) {
		}
	};
			return positionChildUpdateListener;
}

	@Override
	public ValueEventListener getActivityListener() {
		if(activityUpdatesListener==null)activityUpdatesListener=new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				ModelUserActivity modelUserActivity = dataSnapshot.child(FIELD_DB_USER_ACTIVITY).getValue(ModelUserActivity.class);
				//UserPosition userPosition = dataSnapshot.child(FIELD_DB_USER_POSITIONS).getValue(UserPosition.class);
				//positionString = "";
				activityString = "activity: ";
				if (modelUserActivity != null) {
					boolean hasData=false;

					if (modelUserActivity.getActivity0() != null){
						hasData=true;
						activityString += modelUserActivity.getActivity0()+";";
					}
					if (modelUserActivity.getActivity1() != null){
						activityString += modelUserActivity.getActivity1()+";";
					}
					if (modelUserActivity.getActivity2() != null){
						activityString += modelUserActivity.getActivity2()+";";
					}
					if(modelUserActivity.getTimestamp()>1) {
						//activityString += userActivity.formatDateTime()+"\n";
						String s=(currentTimeMillis()- modelUserActivity.getTimestamp())/1000+"";
						//Log.w(TAG, "onDataChange activity: "+s);
						textTimeActivity.setText(s);
					}else textTimeActivity.setText("no data");
					if (!hasData){
						activityString += "no data";
					}
				}
				/*if (userPosition != null) {
					//if(userPosition.getPlaceName()!=null)positionString += "current place: " + userPosition.getPlaceName() + "\n";
					positionString += userPosition.getPlaceAddress() + ", ";
					positionString += userPosition.getAccuracy() + "m";
					//positionString += "lat: " + userPosition.getLat() + "\n";
					//positionString += "long: " + userPosition.getLng()+ "\n";
					if(userPosition.getTimestamp()>1) {
						//positionString +="time: " +  userPosition.formatDateTime();
						String s=(currentTimeMillis()-userPosition.getTimestamp())/1000+"";
						//Log.w(TAG, "onDataChange places: "+s);
						textTimePlaces.setText(s);
					}else textTimePlaces.setText("no data");
					addMarker(userPosition.getLat(), userPosition.getLng(), userPosition.formatDateTime());
				}*/
				//Timber.w("activityString: %s",activityString);
				//Timber.w("positionString: %s",positionString);
				//textPosition.setText(positionString);
				textActivity.setText(activityString);
				mProgressBar.setVisibility(GONE);
				mLayoutNoTarget.setVisibility(GONE);
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {
				removeDbUpdates();
				textActivity.setText("cancelled");
				//textPosition.setText("cancelled");
				mProgressBar.setVisibility(GONE);
				mLayoutNoTarget.setVisibility(VISIBLE);
				cancelElapsedTimer();

			}
		};
		return activityUpdatesListener;
	}

	@Override
	public String getObservableUid() {
		return observableUserId;
	}

	@Override
	public String getCurrentUid() {
		return currentUserId;
	}





}
