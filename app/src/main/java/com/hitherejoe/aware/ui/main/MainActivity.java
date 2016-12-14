package com.hitherejoe.aware.ui.main;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.widget.ProgressBar;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hitherejoe.aware.ui.fence.beacon.BeaconFenceActivity;
import com.hitherejoe.aware.ui.fence.headphone.HeadphoneFenceActivity;
import com.hitherejoe.aware.ui.fence.location.LocationFenceActivity;
import com.hitherejoe.aware.ui.fence.time.TimeFenceActivity;
import com.hitherejoe.aware.ui.fence.user.DetectedActivityFenceActivity;
import com.hitherejoe.aware.ui.snapshot.beacons.BeaconActivity;
import com.hitherejoe.aware.ui.snapshot.headphone.HeadphoneActivity;
import com.hitherejoe.aware.ui.snapshot.location.LocationActivity;
import com.hitherejoe.aware.ui.snapshot.places.PlacesActivity;
import com.hitherejoe.aware.ui.snapshot.user.UserActivity;
import com.hitherejoe.aware.ui.snapshot.weather.WeatherActivity;
import com.stc.meetme.R;
import com.stc.meetme.model.User;
import com.stc.meetme.ui.SignInActivity;

import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.stc.meetme.Constants.FIELD_DB_TOKEN;
import static com.stc.meetme.Constants.NOTIFICATION_ID_CHAT;
import static com.stc.meetme.Constants.NOTIFICATION_ID_MAIN;
import static com.stc.meetme.Constants.SETTINGS_DB_TOKEN;
import static com.stc.meetme.Constants.SETTINGS_MY_UID;
import static com.stc.meetme.Constants.TABLE_DB_USERS;

public class MainActivity extends AppCompatActivity implements
		GoogleApiClient.OnConnectionFailedListener {
	private static final String TAG = "UsersListActivity";

	private static final int REQUEST_INVITE = 1;
	public static final String ANONYMOUS = "anonymous";
	private SharedPreferences prefs;
	private LinearLayoutManager mLinearLayoutManager;
	private ProgressBar mProgressBar;
	private DatabaseReference mFirebaseDatabaseReference;
	private FirebaseAuth mFirebaseAuth;
	private FirebaseUser mFirebaseUser;
	private FirebaseAnalytics mFirebaseAnalytics;
	private GoogleApiClient mGoogleApiClient;
	private String currentUserId;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
	    mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
	    NotificationManager notificationManager =
			    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
	    notificationManager.cancel(NOTIFICATION_ID_MAIN);
	    notificationManager.cancel(NOTIFICATION_ID_CHAT);
	    prefs = PreferenceManager.getDefaultSharedPreferences(this);
	    mFirebaseAuth = FirebaseAuth.getInstance();
	    mFirebaseUser = mFirebaseAuth.getCurrentUser();
	    if (mFirebaseUser == null) {
		    // Not signed in, launch the Sign In activity
		    startActivity(new Intent(this, SignInActivity.class));
		    finish();
		    return;
	    }
	    mGoogleApiClient = new GoogleApiClient.Builder(this)
			    .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
			    .addApi(Auth.GOOGLE_SIGN_IN_API)
			    .build();
	    mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
	    currentUserId = PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString(SETTINGS_MY_UID, null);

	    if( currentUserId!=null){
		    if(prefs.getString(SETTINGS_DB_TOKEN, null)!=null){
			    Log.w("SAVE SUCCESS", "SAVE SUCCESS");
			    mFirebaseDatabaseReference
					    .child(TABLE_DB_USERS)
					    .child(currentUserId)
					    .child(FIELD_DB_TOKEN)
					    .setValue(prefs.getString(SETTINGS_DB_TOKEN, null));
		    }else Log.e("SAVE ERROR", " token null");
	    }else Log.e("SAVE ERROR", "uid null");

	    mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
	    mFirebaseDatabaseReference.child(TABLE_DB_USERS).child(currentUserId).addListenerForSingleValueEvent(new ValueEventListener() {
		    @Override
		    public void onDataChange(DataSnapshot dataSnapshot) {
			    User user = dataSnapshot.getValue(User.class);
			    if(user!=null && user.getName()!=null)
				    setTitle(user.getName());
		    }

		    @Override
		    public void onCancelled(DatabaseError databaseError) {

		    }
	    });

    }

    @OnClick(R.id.text_headphone_state)
    public void onHeadphoneStateTextClick() {
        Intent intent = new Intent(this, HeadphoneActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.text_weather)
    public void onWeatherTextClick() {
        Intent intent = new Intent(this, WeatherActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.text_places)
    public void onPlacesTextClick() {
        Intent intent = new Intent(this, PlacesActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.text_user_activity)
    public void onUserActivityTextClick() {
        Intent intent = new Intent(this, UserActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.text_location)
    public void onLocationTextClick() {
        Intent intent = new Intent(this, LocationActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.text_beacons)
    public void onBeaconTextClick() {
        Intent intent = new Intent(this, BeaconActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.text_headphone_fence)
    public void onHeadphoneFenceTextClick() {
        Intent intent = new Intent(this, HeadphoneFenceActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.text_detected_activity_fence)
    public void onDetectedActivityFenceTextClick() {
        Intent intent = new Intent(this, DetectedActivityFenceActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.text_location_fence)
    public void onLocationFenceTextClick() {
        Intent intent = new Intent(this, LocationFenceActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.text_time_fence)
    public void onTimeFenceTextClick() {
        Intent intent = new Intent(this, TimeFenceActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.text_beacon_fence)
    public void onBeaconFenceTextClick() {
        Intent intent = new Intent(this, BeaconFenceActivity.class);
        startActivity(intent);
    }

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.d(TAG, "onConnectionFailed:" + connectionResult);
	}
}
