package com.stc.meetme.ui;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.stc.meetme.Constants;
import com.stc.meetme.DetectedActivityIntentService;
import com.stc.meetme.DetectedPlacesIntentService;
import com.stc.meetme.R;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static com.stc.meetme.Constants.ADDRESS_REQUESTED_KEY;
import static com.stc.meetme.Constants.FIELD_DB_TOKEN;
import static com.stc.meetme.Constants.LOCATION_ADDRESS_KEY;
import static com.stc.meetme.Constants.SETTINGS_DB_TOKEN;
import static com.stc.meetme.Constants.SETTINGS_DB_UID;
import static com.stc.meetme.Constants.TABLE_DB_DATA;
import static com.stc.meetme.Constants.TABLE_DB_USERS;


public class StartActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

	private static final int PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 940;
	private static final int PERMISSION_REQUEST_FINE_LOCATION = 941;
	private static final String TAG = "StartActivity";

	@BindView(R.id.progress)
	ProgressBar mProgressBar;


	@BindView(R.id.textActivity)
	TextView textActivity;

	@BindView(R.id.textPlaces)
	TextView textPlaces;

	@BindView(R.id.textLocation)
	TextView textLocation;

	@BindView(R.id.buttonRefreshLoc)
	Button buttonRefreshLoc;

	@BindView(R.id.activity_start)
	LinearLayout mLayout;

	@BindView(R.id.buttonRequestAct)
	Button buttonRequestActivityUpdates;

	@BindView(R.id.buttonRemoveAct)
	Button buttonRemoveActivityUpdates;

	protected ActivityDetectionBroadcastReceiver mBroadcastReceiver;

	private GoogleApiClient mGoogleApiClient;

	private SharedPreferences prefs;

	String mDetectedActivity;
	protected Location mLastLocation;

	private DatabaseReference mFirebaseDatabaseReference;

	private FirebaseAuth mFirebaseAuth;

	private FirebaseUser mFirebaseUser;

	private String currentUserId;

	private ValueEventListener activityValueEventListener;

	protected boolean mAddressRequested;

	protected String mAddressOutput;

	private PlacesResultReceiver mPlacesResultReceiver;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_start);
		ButterKnife.bind(this);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle("My current status");
		}
		mAddressRequested = false;
		mAddressOutput = "";

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mFirebaseAuth = FirebaseAuth.getInstance();
		mFirebaseUser = mFirebaseAuth.getCurrentUser();
		if (mFirebaseUser == null) {
			// Not signed in, launch the Sign In activity
			startActivity(new Intent(this, SignInActivity.class));
			finish();
			return;
		}
		mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
		currentUserId = PreferenceManager.getDefaultSharedPreferences(this).getString(SETTINGS_DB_UID, null);

		if (currentUserId != null) {
			if (prefs.getString(SETTINGS_DB_TOKEN, null) != null) {
				Log.w("SAVE SUCCESS", "SAVE SUCCESS");
				mFirebaseDatabaseReference
						.child(TABLE_DB_USERS)
						.child(currentUserId)
						.child(FIELD_DB_TOKEN)
						.setValue(prefs.getString(SETTINGS_DB_TOKEN, null));
			} else Log.e("SAVE ERROR", " token null");
		} else Log.e("SAVE ERROR", "uid null");

		buttonRefreshLoc.setOnClickListener(this::fetchPlaceButtonHandler);
		buttonRequestActivityUpdates.setOnClickListener(this::requestActivityUpdatesButtonHandler);
		buttonRemoveActivityUpdates.setOnClickListener(this::removeActivityUpdatesButtonHandler);

		mPlacesResultReceiver = new PlacesResultReceiver(new Handler());
		mBroadcastReceiver = new ActivityDetectionBroadcastReceiver();
		setButtonsEnabledState();
		if (savedInstanceState != null) {
			if (savedInstanceState.containsKey(Constants.DETECTED_ACTIVITY)) {
				mDetectedActivity = savedInstanceState.getString(
						Constants.DETECTED_ACTIVITY);
			}
			if (savedInstanceState.keySet().contains(ADDRESS_REQUESTED_KEY)) {
				mAddressRequested = savedInstanceState.getBoolean(ADDRESS_REQUESTED_KEY);
			}
			if (savedInstanceState.keySet().contains(LOCATION_ADDRESS_KEY)) {
				mAddressOutput = savedInstanceState.getString(LOCATION_ADDRESS_KEY);
				displayAddressOutput();
			}

		}
		updateUIWidgets();
		setupGoogleApiClient();
		registerActivityValueEventListener();
	}


	private void unRegisterActivityValueEventListener() {
		if (activityValueEventListener != null)
			mFirebaseDatabaseReference.child(TABLE_DB_DATA).child(currentUserId).removeEventListener(activityValueEventListener);
	}

	private void registerActivityValueEventListener() {
		if (activityValueEventListener == null)
			activityValueEventListener = new ValueEventListener() {
				@Override
				public void onDataChange(DataSnapshot dataSnapshot) {
					String result = dataSnapshot.getValue(String.class);
					Timber.w(result);
					if (result != null) textActivity.setText(result);
				}

				@Override
				public void onCancelled(DatabaseError databaseError) {
					showSnap("DB update cancelled");
				}
			};
		if (currentUserId != null && mDetectedActivity != null)
			mFirebaseDatabaseReference.child(TABLE_DB_DATA).child(currentUserId).addValueEventListener(activityValueEventListener);
	}

	private void setupGoogleApiClient() {
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
	protected void onStart() {
		super.onStart();
		mGoogleApiClient.connect();
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}
	}


	@Override
	protected void onResume() {
		super.onResume();
		LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
				new IntentFilter(Constants.BROADCAST_ACTION));
		registerActivityValueEventListener();
	}

	@Override
	protected void onPause() {
		super.onPause();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
		unRegisterActivityValueEventListener();
	}

	public void fetchPlaceButtonHandler(View view) {
		// We only start the service to fetch the address if GoogleApiClient is connected.
		if (mGoogleApiClient.isConnected() && mLastLocation != null) {
			startIntentService();
		}
		// If GoogleApiClient isn't connected, we process the user's request by setting
		// mAddressRequested to true. Later, when GoogleApiClient connects, we launch the service to
		// fetch the address. As far as the user is concerned, pressing the Fetch Address button
		// immediately kicks off the process of getting the address.
		mAddressRequested = true;
		updateUIWidgets();
	}

	protected void startIntentService() {
		// Create an intent for passing to the intent service responsible for fetching the address.
		Intent intent = new Intent(this, DetectedPlacesIntentService.class);

		// Pass the result receiver as an extra to the service.
		intent.putExtra(Constants.RECEIVER, mPlacesResultReceiver);

		// Pass the location data as an extra to the service.
		intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);

		// Start the service. If the service isn't already running, it is instantiated and started
		// (creating a process for it if needed); if it is running then it remains running. The
		// service kills itself automatically once all intents are processed.
		startService(intent);
	}

	public void requestActivityUpdatesButtonHandler(View view) {
		if (!mGoogleApiClient.isConnected()) {
			Toast.makeText(this, getString(R.string.not_connected),
					Toast.LENGTH_SHORT).show();
			return;
		}
		ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
				mGoogleApiClient,
				Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
				getActivityDetectionPendingIntent()
		).setResultCallback(this);
	}

	public void removeActivityUpdatesButtonHandler(View view) {
		if (!mGoogleApiClient.isConnected()) {
			Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
			return;
		}
		ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(
				mGoogleApiClient,
				getActivityDetectionPendingIntent()
		).setResultCallback(this);
	}

	public void onResult(Status status) {
		if (status.isSuccess()) {
			boolean requestingUpdates = !getUpdatesRequestedState();
			setUpdatesRequestedState(requestingUpdates);
			setButtonsEnabledState();

			Toast.makeText(
					this,
					getString(requestingUpdates ? R.string.activity_updates_added :
							R.string.activity_updates_removed),
					Toast.LENGTH_SHORT
			).show();
		} else {
			Log.e(TAG, "Error adding or removing activity detection: " + status.getStatusMessage());
		}
	}

	private PendingIntent getActivityDetectionPendingIntent() {
		Intent intent = new Intent(this, DetectedActivityIntentService.class);
		return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}

	private boolean getUpdatesRequestedState() {
		return getSharedPreferencesInstance()
				.getBoolean(Constants.ACTIVITY_UPDATES_REQUESTED_KEY, false);
	}

	private void setUpdatesRequestedState(boolean requestingUpdates) {
		getSharedPreferencesInstance()
				.edit()
				.putBoolean(Constants.ACTIVITY_UPDATES_REQUESTED_KEY, requestingUpdates)
				.apply();
	}

	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putString(Constants.DETECTED_ACTIVITY, mDetectedActivity);
		savedInstanceState.putBoolean(ADDRESS_REQUESTED_KEY, mAddressRequested);
		savedInstanceState.putString(LOCATION_ADDRESS_KEY, mAddressOutput);

		super.onSaveInstanceState(savedInstanceState);
	}

	private void setButtonsEnabledState() {
		if (getUpdatesRequestedState()) {
			buttonRequestActivityUpdates.setEnabled(false);
			buttonRemoveActivityUpdates.setEnabled(true);
		} else {
			buttonRequestActivityUpdates.setEnabled(true);
			buttonRemoveActivityUpdates.setEnabled(false);
		}
	}

	private void updateUIWidgets() {
		if (mAddressRequested) {
			mProgressBar.setVisibility(ProgressBar.VISIBLE);
			buttonRefreshLoc.setEnabled(false);
		} else {
			mProgressBar.setVisibility(ProgressBar.GONE);
			buttonRefreshLoc.setEnabled(true);
		}
	}

	protected void updateDetectedActivitiesList(ArrayList<DetectedActivity> detectedActivities) {
		if (detectedActivities != null && !detectedActivities.isEmpty())
			mDetectedActivity = DetectedActivityIntentService.getActString(detectedActivities, this);
		if (mDetectedActivity != null) textActivity.setText(mDetectedActivity);
	}


	protected void displayAddressOutput() {
		textPlaces.setText(mAddressOutput);
	}

	private SharedPreferences getSharedPreferencesInstance() {
		return getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
	                                       String permissions[], int[] grantResults) {
		switch (requestCode) {
			case PERMISSION_REQUEST_FINE_LOCATION: {
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					updateLocation();
				} else {
					mProgressBar.setVisibility(View.GONE);
					Snackbar.make(mLayout,
							getString(R.string.error_loading_places),
							Snackbar.LENGTH_LONG).show();
				}
			}
		}
	}

	private String getActivityString(int activity) {
		switch (activity) {
			case DetectedActivity.IN_VEHICLE:
				return getString(R.string.activity_in_vehicle);
			case DetectedActivity.ON_BICYCLE:
				return getString(R.string.activity_on_bicycle);
			case DetectedActivity.ON_FOOT:
				return getString(R.string.activity_on_foot);
			case DetectedActivity.RUNNING:
				return getString(R.string.activity_running);
			case DetectedActivity.STILL:
				return getString(R.string.activity_still);
			case DetectedActivity.TILTING:
				return getString(R.string.activity_tilting);
			case DetectedActivity.WALKING:
				return getString(R.string.activity_walking);
			default:
				return getString(R.string.activity_unknown);
		}
	}

	@Override
	public void onConnected(@Nullable Bundle bundle) {
		Timber.w("onConnected");
		Snackbar.make(mLayout,
				"Connected",
				Snackbar.LENGTH_SHORT).show();
		updateLocation();
	}

	private void updateLocation() {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
				PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
					PERMISSION_REQUEST_FINE_LOCATION);
		} else {
			mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
			if (mLastLocation != null) {
				// Determine whether a Geocoder is available.
				if (!Geocoder.isPresent()) {
					Toast.makeText(this, R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
					return;
				}
				// It is possible that the user presses the button to get the address before the
				// GoogleApiClient object successfully connects. In such a case, mAddressRequested
				// is set to true, but no attempt is made to fetch the address (see
				// fetchAddressButtonHandler()) . Instead, we start the intent service here if the
				// user has requested an address, since we now have a connection to GoogleApiClient.
				if (mAddressRequested) {
					startIntentService();
				}
			}
		}
	}

	@Override
	public void onConnectionSuspended(int i) {
		Timber.w("onConnectionSuspended");

		Snackbar.make(mLayout,
				"Connection Suspended. Reconnecting",
				Snackbar.LENGTH_SHORT).show();
		mGoogleApiClient.connect();

	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
		Timber.w("onConnectionFailed");

		Snackbar.make(mLayout,
				"Connection Failed",
				Snackbar.LENGTH_SHORT).show();
	}
	public void showSnap(String s){
		Snackbar.make(mLayout,
				s,
				Snackbar.LENGTH_SHORT).show();
		Timber.w(s);
	}
	public class ActivityDetectionBroadcastReceiver extends BroadcastReceiver {
		protected static final String TAG = "activity-detection-response-receiver";

		@Override
		public void onReceive(Context context, Intent intent) {
			ArrayList<DetectedActivity> updatedActivities =
					intent.getParcelableArrayListExtra(Constants.ACTIVITY_EXTRA);
			updateDetectedActivitiesList(updatedActivities);
		}
	}

	class PlacesResultReceiver extends ResultReceiver {
		public PlacesResultReceiver(Handler handler) {
			super(handler);
		}

		/**
		 *  Receives data sent from FetchAddressIntentService and updates the UI in MainActivity.
		 */
		@Override
		protected void onReceiveResult(int resultCode, Bundle resultData) {

			// Display the address string or an error message sent from the intent service.
			mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
			displayAddressOutput();

			// Show a toast message if an address was found.
			if (resultCode == Constants.SUCCESS_RESULT) {
				showSnap(getString(R.string.address_found));
			}

			// Reset. Enable the Fetch Address button and stop showing the progress bar.
			mAddressRequested = false;
			updateUIWidgets();
		}
	}
}
