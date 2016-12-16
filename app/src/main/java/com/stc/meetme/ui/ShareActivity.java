package com.stc.meetme.ui;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
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
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.stc.meetme.Constants;
import com.stc.meetme.DetectedActivityIntentService;
import com.stc.meetme.DetectedLocationIntentService;
import com.stc.meetme.LocationHelper;
import com.stc.meetme.R;
import com.stc.meetme.SharingStatusNotifier;
import com.stc.meetme.model.ModelUserPosition;

import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.ButterKnife;
import timber.log.Timber;

import static com.google.android.gms.location.LocationServices.FusedLocationApi;
import static com.stc.meetme.Constants.ADDRESS_REQUESTED_KEY;
import static com.stc.meetme.Constants.FIELD_DB_USER_POSITIONS;
import static com.stc.meetme.Constants.INTENT_EXTRA_OBSERVE_UID;
import static com.stc.meetme.Constants.PERMISSION_REQUEST_FINE_LOCATION;
import static com.stc.meetme.Constants.SETTINGS_MY_UID;
import static com.stc.meetme.Constants.TABLE_DB_USER_STATUSES;
import static junit.framework.Assert.assertNotNull;


public class ShareActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener, SharingStatusNotifier.UIUpdateInterface {
	private static final String TAG = "ShareActivity";


	ProgressBar mProgressBar;

	TextView textStatusActivity;
	TextView textStatusLocation;

	Button buttonRegister;

	Button buttonRemove;

	Button buttonObserve;

	Button buttonShare;

	private GoogleApiClient mGoogleApiClient;

	protected Location mLastLocation;

	private String currentUserId;

	protected boolean mAddressRequested;

	protected String mAddressOutput;

	SharedPreferences prefs;

	private View mLayout;

	NotificationManager notificationManager;
	private PendingResult<Status> requestLocationResult;
	private PendingResult<Status> requestActivityResult;
	public SharingStatusNotifier notifier;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_share);
		ButterKnife.bind(this);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setTitle("Share your position");
		}
		mLayout=(LinearLayout)findViewById(R.id.activity_share);
		mProgressBar=(ProgressBar)findViewById(R.id.progress);
		buttonShare=(Button) findViewById(R.id.buttonShare);
		buttonObserve=(Button) findViewById(R.id.buttonObserve);
		buttonRemove =(Button) findViewById(R.id.buttonRemove);
		buttonRegister =(Button) findViewById(R.id.buttonRegister);
		textStatusLocation=(TextView) findViewById(R.id.textStatusLocation);
		textStatusActivity=(TextView) findViewById(R.id.textStatusActivity);

		mAddressRequested = false;
		mAddressOutput = "";

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		currentUserId = PreferenceManager.getDefaultSharedPreferences(this).getString(SETTINGS_MY_UID, null);
		assertNotNull(currentUserId);

		buttonRegister.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				setupGoogleApiClient();
			}
		});
		buttonRemove.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				removeUpdates();
			}
		});
		buttonObserve.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startObserve(currentUserId);
			}
		});

		buttonShare.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				sharePosition(currentUserId);
			}
		});
		if (savedInstanceState != null) {
			if (savedInstanceState.keySet().contains(ADDRESS_REQUESTED_KEY)) {
				mAddressRequested = savedInstanceState.getBoolean(ADDRESS_REQUESTED_KEY);
			}
		}
		notifier=new SharingStatusNotifier(this);

	}

	@Override
	protected void onNewIntent(Intent intent) {
		//if(intent.getAction().equals(INTENT_ACTION_STOP_UPDATES)) removeUpdates();
	}
	private void sharePosition(String currentUserId) {
		String url = "https://f3x9u.app.goo.gl/observe/"+currentUserId;
		Log.w("SHARE", url + "");
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		shareIntent.putExtra(Intent.EXTRA_TEXT, url);
		startActivity(Intent.createChooser(shareIntent, "Share link using"));
	}


	private void startObserve(String currentUserId) {
		if(currentUserId==null) showToast("NULL USER. OBSERVE FAILED");
		else{
			Timber.w("startObserve uid=%s", currentUserId);
			Intent intent=new Intent(this, ObserveActivity.class);
			intent.putExtra(INTENT_EXTRA_OBSERVE_UID, currentUserId);
			startActivity(intent);
		}
	}

	private void showToast(String s) {
		Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
	}

	private void setupGoogleApiClient() {
		if(mGoogleApiClient==null){
			mGoogleApiClient = new GoogleApiClient.Builder(this)
					.addConnectionCallbacks(this)
					.addOnConnectionFailedListener(this)
					.addApi(Awareness.API)
					.addApi(LocationServices.API)
					.addApi(ActivityRecognition.API)
					.build();
		}
		if(!mGoogleApiClient.isConnected()) mGoogleApiClient.connect();
		else requestUpdates();
	}


	@Override
	protected void onStart() {
		super.onStart();
	}


	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mGoogleApiClient!=null &&  mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}

	}

	public void requestUpdates() {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
				PackageManager.PERMISSION_GRANTED) {
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
					PERMISSION_REQUEST_FINE_LOCATION);
		} else {
			if (!mGoogleApiClient.isConnected()) {
				Toast.makeText(this, getString(R.string.not_connected),
						Toast.LENGTH_SHORT).show();
				return;
			}

			requestActivityResult =ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
					mGoogleApiClient,
					Constants.UPDATE_INTERVAL,
					getActivityDetectionPendingIntent()
			);
			//requestActivityResult.setResultCallback(notifier.getCallback(true,CALLBACK_ACTIVITY));


			checkOutdatedDbData();
			LocationRequest mLocationRequest = LocationHelper.getLocationRequest();
			requestLocationResult = LocationServices.FusedLocationApi.requestLocationUpdates(
					mGoogleApiClient,
					mLocationRequest,
					getLocationDetectionPendingIntent()
			);
			//requestLocationResult.setResultCallback(notifier.getCallback(true,CALLBACK_LOCATION));
		}
	}
	public void checkOutdatedDbData(){
		FirebaseDatabase.getInstance().getReference().child(TABLE_DB_USER_STATUSES).child(currentUserId).child(FIELD_DB_USER_POSITIONS).addChildEventListener(
				new ChildEventListener() {
					@Override
					public void onChildAdded(DataSnapshot dataSnapshot, String s) {
						ModelUserPosition modelUserPosition = dataSnapshot.getValue(ModelUserPosition.class);
						Date dateNow = new Date();
						Date dateDb = new Date(modelUserPosition.getTimestamp());
						SimpleDateFormat dateFormat=new SimpleDateFormat("DD");
						if(!TextUtils.equals(dateFormat.format(dateNow), dateFormat.format(dateDb))) {
							FirebaseDatabase.getInstance().getReference().child(TABLE_DB_USER_STATUSES).child(currentUserId).child(FIELD_DB_USER_POSITIONS).updateChildren(null);
						}
					}

					@Override
					public void onChildChanged(DataSnapshot dataSnapshot, String s) {

					}

					@Override
					public void onChildRemoved(DataSnapshot dataSnapshot) {
					}

					@Override
					public void onChildMoved(DataSnapshot dataSnapshot, String s) {

					}

					@Override
					public void onCancelled(DatabaseError databaseError) {
					}
				});
	}

	public void removeUpdates() {
		if (mGoogleApiClient==null || !mGoogleApiClient.isConnected()) {
			Log.e(TAG, "removeUpdates: notConnected");
			//return;
		}
		ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(
				mGoogleApiClient,
				getActivityDetectionPendingIntent()
		);//.setResultCallback(notifier.getCallback(false,CALLBACK_ACTIVITY));
		FusedLocationApi.removeLocationUpdates(
				mGoogleApiClient,
				getLocationDetectionPendingIntent()
		);//.setResultCallback(notifier.getCallback(false,CALLBACK_LOCATION));
	}


	private PendingIntent getActivityDetectionPendingIntent() {
		Intent intent = new Intent(this, DetectedActivityIntentService.class);
		return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}
	private PendingIntent getLocationDetectionPendingIntent() {
		Intent intent = new Intent(this, DetectedLocationIntentService.class);
		return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}



	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	public void updateUi(boolean status) {
		if (status) {
			buttonObserve.setEnabled(true);
			buttonShare.setEnabled(true);
			buttonRemove.setEnabled(true);
			buttonRegister.setEnabled(false);

			buttonObserve.setVisibility(View.VISIBLE);
			buttonShare.setVisibility(View.VISIBLE);
			buttonRemove.setVisibility(View.VISIBLE);
			buttonRegister.setVisibility(View.GONE);
		} else {
			buttonObserve.setEnabled(false);
			buttonShare.setEnabled(false);
			buttonRemove.setEnabled(false);
			buttonRegister.setEnabled(true);

			buttonObserve.setVisibility(View.GONE);
			buttonShare.setVisibility(View.GONE);
			buttonRemove.setVisibility(View.GONE);
			buttonRegister.setVisibility(View.VISIBLE);
		}
		textStatusLocation.setText("location updates enabled: "+status);
		textStatusActivity.setText("activity updates enabled: "+status);
	}


	@Override
	public void onRequestPermissionsResult(int requestCode,
	                                       String permissions[], int[] grantResults) {
		switch (requestCode) {
			case PERMISSION_REQUEST_FINE_LOCATION: {
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					requestUpdates();
				} else {
					mProgressBar.setVisibility(View.GONE);
					Snackbar.make(mLayout,
							getString(R.string.error_loading_places),
							Snackbar.LENGTH_LONG).show();
				}
			}
		}
	}



	@Override
	public void onConnected(@Nullable Bundle bundle) {
		Timber.w("onConnected");
		Snackbar.make(mLayout,
				"Connected",
				Snackbar.LENGTH_SHORT).show();
		requestUpdates();
	}

	@Override
	public void onConnectionSuspended(int i) {
		Timber.w("onConnectionSuspended");
		Snackbar.make(mLayout,
				"Connection Suspended. Reconnecting",
				Snackbar.LENGTH_SHORT).show();

		notifier.stopNotification();
		mGoogleApiClient.connect();
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
		Timber.w("onConnectionFailed");
		Snackbar.make(mLayout,
				"Connection Failed",
				Snackbar.LENGTH_SHORT).show();
		notifier.stopNotification();
	}


}
