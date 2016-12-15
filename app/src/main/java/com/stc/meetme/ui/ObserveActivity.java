package com.stc.meetme.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.deeplinkdispatch.DeepLink;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.stc.meetme.MapsActivity;
import com.stc.meetme.R;
import com.stc.meetme.model.User;
import com.stc.meetme.model.UserActivity;
import com.stc.meetme.model.UserPosition;

import butterknife.ButterKnife;
import timber.log.Timber;

import static android.view.Menu.NONE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.stc.meetme.Constants.FIELD_DB_TOKEN;
import static com.stc.meetme.Constants.FIELD_DB_USER_ACTIVITY;
import static com.stc.meetme.Constants.FIELD_DB_USER_POSITION;
import static com.stc.meetme.Constants.INTENT_EXTRA_OBSERVE_LAT;
import static com.stc.meetme.Constants.INTENT_EXTRA_OBSERVE_LNG;
import static com.stc.meetme.Constants.INTENT_EXTRA_OBSERVE_UID;
import static com.stc.meetme.Constants.SETTINGS_DB_TOKEN;
import static com.stc.meetme.Constants.SETTINGS_MY_UID;
import static com.stc.meetme.Constants.SETTINGS_OBSERVE_UID;
import static com.stc.meetme.Constants.TABLE_DB_USERS;
import static com.stc.meetme.Constants.TABLE_DB_USER_STATUSES;
import static com.stc.meetme.R.id.map;
import static java.lang.System.currentTimeMillis;


@DeepLink("https://f3x9u.app.goo.gl/observe/{id}")
public class ObserveActivity extends AppCompatActivity implements
		GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener,
		OnMapReadyCallback {
	protected static final String TAG = "ObserveActivity";
	public static final String ACTION_DEEP_LINK_COMPLEX = "deep_link_complex";


	LinearLayout mLayoutNoTarget;

	ProgressBar mProgressBar;

	TextView textPosition;

	TextView textTimeActivity;

	TextView textTimePlaces;

	TextView textActivity;

	String observableUserId;

	String positionString;

	String activityString;

	protected SharedPreferences prefs;

	protected DatabaseReference mFirebaseDatabaseReference;

	protected ValueEventListener statusUpdatesListener;
	protected FirebaseAuth mFirebaseAuth;

	protected String currentUserId;
	protected MenuItem menuSharePosition, menuRefresh;
	protected GoogleApiClient mGoogleApiClient;
	protected ActionBar actionBar;
	protected CountDownTimer timer;
	protected MenuItem menuMap;
	protected static final int MENU_MAP = 312;
	protected static final int MENU_SHARE = 959;
	protected static final int MENU_REFRESH = 958;
	protected double observeLat;
	protected double observeLng;
	protected boolean mapReady=false;
	protected GoogleMap mMap;
	protected Marker marker;
	protected FloatingActionButton fab;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_observe);
		ButterKnife.bind(this);
		observeLat=observeLng=0;
		textPosition=(TextView) findViewById(R.id.textViewPosition);
		textTimeActivity =(TextView) findViewById(R.id.textViewTimeActivity);
		textTimePlaces =(TextView) findViewById(R.id.textViewTimePlaces);
		textActivity=(TextView) findViewById(R.id.textViewActivity);
		fab=(FloatingActionButton) findViewById(R.id.floatingActionButton);
		mProgressBar=(ProgressBar) findViewById(R.id.progressBar);
		mLayoutNoTarget=(LinearLayout) findViewById(R.id.layoutNoTarget);
		actionBar = getSupportActionBar();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
		mFirebaseAuth = FirebaseAuth.getInstance();
		//setupGoogleApiClient();
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
				.findFragmentById(map);
		mapFragment.getMapAsync(this);
	}
	@Override
	public void onMapReady(GoogleMap googleMap) {
		mMap = googleMap;
		setupGoogleApiClient();
	}

	private void setupGoogleApiClient() {
		Log.d(TAG, "setupGoogleApiClient");
		mProgressBar.setVisibility(VISIBLE);
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(Awareness.API)
				.addApi(LocationServices.API)
				.addApi(ActivityRecognition.API)
				.build();
		mGoogleApiClient.connect();
	}

	@Override
	public void onConnected(@Nullable Bundle bundle) {
		Log.w(TAG, "onConnected");
		currentUserId = PreferenceManager.getDefaultSharedPreferences(this).getString(SETTINGS_MY_UID, null);
			if(currentUserId==null) signInAnonymously();
			else checkIntent();
	}

	@Override
	public void onConnectionSuspended(int i) {
		Log.e(TAG, "onConnectionSuspended");
		mProgressBar.setVisibility(GONE);
		mLayoutNoTarget.setVisibility(VISIBLE);
		removeDbUpdates();
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
		Log.e(TAG, "onConnectionFailed "+connectionResult.toString());
		mProgressBar.setVisibility(GONE);
		mLayoutNoTarget.setVisibility(VISIBLE);
		removeDbUpdates();

	}

	private void signInAnonymously() {
		Log.w(TAG, "signInAnonymously");
		mProgressBar.setVisibility(VISIBLE);
		mFirebaseAuth.signInAnonymously()
				.addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
					@Override
					public void onComplete(@NonNull Task<AuthResult> task) {
						Log.w(TAG, "signInAnonymously:onComplete:" + task.isSuccessful());

						if (!task.isSuccessful()) {
							Log.e(TAG, "signInAnonymously", task.getException());
							Toast.makeText(ObserveActivity.this, "Authentication failed.",
									Toast.LENGTH_SHORT).show();
							mProgressBar.setVisibility(GONE);
							mLayoutNoTarget.setVisibility(VISIBLE);
						} else initCurrentUser();
					}
				});
	}

	private void initCurrentUser() {
		final FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();
		mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
		if(firebaseUser!=null){
			Log.e(TAG, "firebaseUser!=null");

			mFirebaseDatabaseReference.child(TABLE_DB_USERS).addListenerForSingleValueEvent(new ValueEventListener() {
				@Override
				public void onDataChange(DataSnapshot dataSnapshot) {
					User dbUser=null;
					boolean exists=false;
					String key=null;
					for(DataSnapshot child : dataSnapshot.getChildren()){
						dbUser = child.getValue(User.class);
						if(TextUtils.equals(dbUser.getUserId(), firebaseUser.getUid())) {
							exists=true;
							key=child.getKey();
							break;
						}
					}
					if(!exists) {
						Log.w(TAG, "!exists");

						String photoUrl=null;
						if(firebaseUser.getPhotoUrl()!=null)
							photoUrl=firebaseUser.getPhotoUrl().toString();
						key=mFirebaseDatabaseReference.child(TABLE_DB_USERS).push().getKey();
						User user=new User(firebaseUser.getUid(),firebaseUser.isAnonymous() ? "Anonymous": firebaseUser.getDisplayName(), photoUrl,key);
						mFirebaseDatabaseReference.child(TABLE_DB_USERS).child(key).setValue(user);

					}else {
						Log.w(TAG, "exists");

						prefs.edit().putString(SETTINGS_MY_UID, key).apply();
					}
					Log.w(TAG, "ID="+key);
					Log.w(TAG, "ID="+firebaseUser.getUid());

					if(prefs.getString(SETTINGS_DB_TOKEN, null)!=null) {
						Log.w("TAG", "SETTINGS_DB_TOKEN: "+prefs.getString(SETTINGS_DB_TOKEN, null));
						mFirebaseDatabaseReference.child(TABLE_DB_USERS).child(key).child(FIELD_DB_TOKEN).setValue(prefs.getString(SETTINGS_DB_TOKEN, null));
					}else Log.e("SignIn", "TOKEN NOT FOUND");
					checkIntent();
				}

				@Override
				public void onCancelled(DatabaseError databaseError) {
					prefs.edit().putString(SETTINGS_MY_UID, null).apply();
					Toast.makeText(ObserveActivity.this, "CANCELLED", Toast.LENGTH_SHORT).show();
					mProgressBar.setVisibility(GONE);
					mLayoutNoTarget.setVisibility(VISIBLE);
					Log.e(TAG, "onCancelled");

				}
			});
		}else {
			mProgressBar.setVisibility(GONE);
			mLayoutNoTarget.setVisibility(VISIBLE);
			Toast.makeText(ObserveActivity.this, "LOGIN ERROR", Toast.LENGTH_SHORT).show();
			Log.e(TAG,"LOGIN ERROR");
		}
	}



	@Override
	protected void onResume() {
		if(mGoogleApiClient!=null && mGoogleApiClient.isConnected() && observableUserId!=null){
			Log.d(TAG, "onResume: registerDbUpdates");
			registerDbUpdates(observableUserId);
		}else {
			Log.e(TAG, "onResume: mGoogleApiClient "+mGoogleApiClient+"");
			if(mGoogleApiClient!=null)Log.e(TAG, "onResume: mGoogleApiClient.isConnected() "+mGoogleApiClient.isConnected());
			Log.e(TAG, "onResume: observableUserId "+observableUserId);
		}
		super.onResume();


	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onRonPauseesume: removeDbUpdates");
		removeDbUpdates();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}
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
			registerDbUpdates(observableUserId);
			//registerDbUpdates("-KWnMwtbJMYSokwxx5D2");

		}
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
		//Log.w("currentValue", currentValue+"");
		if (currentValue != -1) {
			Integer newValue = currentValue + 1;
			newValueStr = newValue.toString();
		}
		//Log.w(TAG, "updateValue: "+newValueStr );
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
		if(statusUpdatesListener==null) {
			statusUpdatesListener = getDbListener();
		}
		scheduleElapsedTimer();
		mProgressBar.setVisibility(VISIBLE);
		mFirebaseDatabaseReference.child(TABLE_DB_USER_STATUSES)
				.child(observableUserId).addValueEventListener(statusUpdatesListener);
	}

	public void removeDbUpdates(){
		Timber.w("removeEventListener");
		if(statusUpdatesListener!=null  && observableUserId!=null)
			mFirebaseDatabaseReference.child(TABLE_DB_USER_STATUSES)
				.child(observableUserId).removeEventListener(statusUpdatesListener);
		cancelElapsedTimer();
		mProgressBar.setVisibility(GONE);
	}
	public void addMarker(double observeLat, double observeLng){
		if(observeLng!=0 && observeLat!=0) {
			Log.d(TAG, "addMarker: lat:"+observeLat+" lng:"+observeLng);
			LatLng observeLatLng= new LatLng(observeLat,observeLng);
			if(marker==null) marker = mMap.addMarker(new MarkerOptions()
					.position(observeLatLng)
					.title(observableUserId)
					.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_account_circle_black_36dp))
					.snippet(observableUserId));
			else marker.setPosition(observeLatLng);
			mMap.moveCamera(CameraUpdateFactory.newLatLng(observeLatLng));
			mMap.animateCamera(CameraUpdateFactory.zoomTo(15), 2000, null);
		}else Log.e(TAG, "addMarker: ERROR");
	}
public ValueEventListener getDbListener(){
	return new ValueEventListener() {
		@Override
		public void onDataChange(DataSnapshot dataSnapshot) {
			UserActivity userActivity = dataSnapshot.child(FIELD_DB_USER_ACTIVITY).getValue(UserActivity.class);
			UserPosition userPosition = dataSnapshot.child(FIELD_DB_USER_POSITION).getValue(UserPosition.class);
			positionString = "";
			activityString = "activity: ";
			if (userActivity != null) {
				boolean hasData=false;

				if (userActivity.getActivity0() != null){
					hasData=true;
					activityString += userActivity.getActivity0()+";";
				}
				if (userActivity.getActivity1() != null){
					activityString += userActivity.getActivity1()+";";
				}
				if (userActivity.getActivity2() != null){
					activityString += userActivity.getActivity2()+";";
				}
				if(userActivity.getTimestamp()>1) {
					//activityString += userActivity.formatDateTime()+"\n";
					String s=(currentTimeMillis()-userActivity.getTimestamp())/1000+"";
					//Log.w(TAG, "onDataChange activity: "+s);
					textTimeActivity.setText(s);
				}else textTimeActivity.setText("no data");
				if (!hasData){
					activityString += "no data";
				}
			}
			if (userPosition != null) {
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
				addMarker(userPosition.getLat(), userPosition.getLng());
			}
			//Timber.w("activityString: %s",activityString);
			//Timber.w("positionString: %s",positionString);
			textPosition.setText(positionString);
			textActivity.setText(activityString);
			mProgressBar.setVisibility(GONE);
			mLayoutNoTarget.setVisibility(GONE);
		}

		@Override
		public void onCancelled(DatabaseError databaseError) {
			removeDbUpdates();
			textActivity.setText("cancelled");
			textPosition.setText("cancelled");
			mProgressBar.setVisibility(GONE);
			mLayoutNoTarget.setVisibility(VISIBLE);
			cancelElapsedTimer();
			marker.remove();
		}
	};
}



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
				Intent intent=new Intent(this, MapsActivity.class);
				intent.putExtra(INTENT_EXTRA_OBSERVE_LAT,observeLat);
				intent.putExtra(INTENT_EXTRA_OBSERVE_LNG,observeLng);
				//intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}else {
				showToast("ERROR lat:"+observeLat+", lng:"+ observeLng);
			}
		}else if(MENU_REFRESH==item.getItemId()) {
			if (statusUpdatesListener != null) {
				Timber.w("removeEventListener");
				mFirebaseDatabaseReference.removeEventListener(statusUpdatesListener);
			}
			if (observableUserId != null && currentUserId != null)
				registerDbUpdates(observableUserId);
		}else if(MENU_SHARE==item.getItemId()) {
			Intent intent=new Intent(this, ShareActivity.class);
			startActivity(intent);
		}
		return super.onOptionsItemSelected(item);
	}
	public class GoogleAPIHelper  {

	}
}
