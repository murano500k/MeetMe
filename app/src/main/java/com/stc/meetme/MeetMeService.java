package com.stc.meetme;

import android.app.Activity;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.stc.meetme.model.ModelUserActivity;
import com.stc.meetme.model.ModelUserPosition;
import com.stc.meetme.model.User;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

import static com.google.android.gms.location.LocationServices.FusedLocationApi;
import static com.stc.meetme.Constants.FIELD_DB_TOKEN;
import static com.stc.meetme.Constants.FIELD_DB_USER_ACTIVITY;
import static com.stc.meetme.Constants.FIELD_DB_USER_POSITIONS;
import static com.stc.meetme.Constants.INTENT_ACTION_GET_STATUS;
import static com.stc.meetme.Constants.INTENT_ACTION_START_MY_UPDATES;
import static com.stc.meetme.Constants.INTENT_ACTION_STOP_MY_UPDATES;
import static com.stc.meetme.Constants.INTENT_EXTRA_GAPI_STATUS;
import static com.stc.meetme.Constants.INTENT_EXTRA_MY_ACTIVITY;
import static com.stc.meetme.Constants.INTENT_EXTRA_MY_DB_UID;
import static com.stc.meetme.Constants.INTENT_EXTRA_MY_UPDATES_STATUS;
import static com.stc.meetme.Constants.RECEIVER;
import static com.stc.meetme.Constants.SETTINGS_DB_TOKEN;
import static com.stc.meetme.Constants.SETTINGS_MY_UID;
import static com.stc.meetme.Constants.TABLE_DB_USERS;
import static com.stc.meetme.Constants.TABLE_DB_USER_STATUSES;
import static com.stc.meetme.GoogleApiInstance.Status.CONNECTED;
import static com.stc.meetme.GoogleApiInstance.Status.NONE;

/**
 * Asynchronously handles an intent using a worker thread. Receives a ResultReceiver object and a
 * location through an intent. Tries to fetch the address for the location using a Geocoder, and
 * sends the result to the ResultReceiver.
 */
public class MeetMeService extends IntentService {
	private static final String TAG = "FetchAddressIS";

	/**
	 * The receiver where results are forwarded from this service.
	 */
	protected ResultReceiver mReceiver;
	private SharedPreferences prefs;

	private DatabaseReference mFirebaseDatabaseReference;

	private String myUid;
	private ModelUserPosition lastModelUserPosition;
	private ModelUserActivity lastModelUserActivity;
	private boolean gApiStatus;
	private boolean myUpdatesStatus;


	List<ResultReceiver> recs;
	private FirebaseAuth mFirebaseAuth;
	GoogleApiInstance gApi;
	private String lastUserPositionKey;
	private GoogleApiInstance.Action<GoogleApiClient> disconnectAction, connectAction;

	/**
	 * This constructor is required, and calls the super IntentService(String)
	 * constructor with the name for a worker thread.
	 */
	public MeetMeService() {
		super(TAG);
		recs = new ArrayList<>();
		Timber.w(" ");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Timber.w(" ");
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
		myUid = prefs.getString(Constants.SETTINGS_MY_UID, null);
		mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
		mFirebaseAuth = FirebaseAuth.getInstance();
		disconnectAction=new GoogleApiInstance.Action<GoogleApiClient>() {
			@Override
			public void call(GoogleApiClient value) {
				ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(
						value,
						getActivityDetectionPendingIntent()
				);

				FusedLocationApi.removeLocationUpdates(
						value,
						getLocationDetectionPendingIntent()
				);
				myUpdatesStatus=false;
			}
		};

		gApi = GoogleApiInstance.get();
		gApi.init(this, Awareness.API, LocationServices.API, ActivityRecognition.API);
		gApi.connect(true, new GoogleApiInstance.Action<GoogleApiClient>() {
			@Override
			public void call(GoogleApiClient value) {
				signInAnonymously();
			}
		});
		connectAction=new GoogleApiInstance.Action<GoogleApiClient>() {
			@Override
			public void call(GoogleApiClient mGoogleApiClient) {
				ResultCallback<Status> callback = new ResultCallback<Status>() {
					@Override
					public void onResult(@NonNull Status status) {
						if (firstResult < 0) {
							firstResult = status.isSuccess() ? 1 : 0;
						} else {
							if (firstResult > 0) myUpdatesStatus = status.isSuccess();
							else if (firstResult == 0) myUpdatesStatus = false;
							notifyRecsStatus();
						}
					}

					int firstResult = -1;
				};
				ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
						mGoogleApiClient,
						Constants.UPDATE_INTERVAL,
						getActivityDetectionPendingIntent()
				).setResultCallback(callback);

				checkOutdatedDbData();
				LocationRequest mLocationRequest = LocationHelper.getLocationRequest();

				if (ActivityCompat.checkSelfPermission(MeetMeService.this,
						android.Manifest.permission.ACCESS_FINE_LOCATION) !=
						PackageManager.PERMISSION_GRANTED &&
						ActivityCompat.checkSelfPermission(
								MeetMeService.this,
								android.Manifest.permission.ACCESS_COARSE_LOCATION) !=
								PackageManager.PERMISSION_GRANTED) {

					myUpdatesStatus=false;
					notifyRecsStatus();
					return;

				}

				LocationServices.FusedLocationApi.requestLocationUpdates(
						mGoogleApiClient,
						mLocationRequest,
						getLocationDetectionPendingIntent()
				).setResultCallback(callback);
			}
		};
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		ActivityRecognitionResult resultA = ActivityRecognitionResult.extractResult(intent);
		LocationResult locationResult = LocationResult.extractResult(intent);
		Bundle bundle = intent.getExtras();
		String action = intent.getAction();

		if (bundle != null) {
			if (bundle.containsKey(RECEIVER)) {
				ResultReceiver rec = intent.getParcelableExtra(RECEIVER);
				if (rec != null) recs.add(rec);
			}

		}
		if (action != null) {
			if (action.equals(INTENT_ACTION_GET_STATUS)) {
				notifyRecsStatus();
				return;
			} else if (action.equals(INTENT_ACTION_START_MY_UPDATES)) {
				if (myUid == null) {
					notifyRecsStatus();
				}else requestUpdates();
			}else if (action.equals(INTENT_ACTION_STOP_MY_UPDATES)) {
				if (myUid == null) {
					notifyRecsStatus();
				}else removeUpdates();
			}
		}
		if (resultA != null) handleActivity(resultA);

		if (locationResult != null) handleLocation(locationResult);

		// Extract the receiver passed into the service

	}

	private void signInAnonymously() {
		Log.w(TAG, "signInAnonymously");
		mFirebaseAuth.signInAnonymously()
				.addOnCompleteListener(new OnCompleteListener<AuthResult>() {
					@Override
					public void onComplete(@NonNull Task<AuthResult> task) {
						Log.w(TAG, "signInAnonymously:onComplete:" + task.isSuccessful());

						if (!task.isSuccessful()) {
							Log.e(TAG, "signInAnonymously", task.getException());
						} else initCurrentUser();
					}
				});
	}

	private void initCurrentUser() {
		final FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();
		mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
		if (firebaseUser != null) {
			Log.w(TAG, "firebaseUser!=null");
			mFirebaseDatabaseReference.child(TABLE_DB_USERS).addListenerForSingleValueEvent(new ValueEventListener() {
				@Override
				public void onDataChange(DataSnapshot dataSnapshot) {
					User dbUser = null;
					boolean exists = false;
					String key = null;
					for (DataSnapshot child : dataSnapshot.getChildren()) {
						dbUser = child.getValue(User.class);
						if (TextUtils.equals(dbUser.getUserId(), firebaseUser.getUid())) {
							exists = true;
							key = child.getKey();
							break;
						}
					}
					if (!exists) {
						Log.w(TAG, "!exists");
						String photoUrl = null;
						if (firebaseUser.getPhotoUrl() != null)
							photoUrl = firebaseUser.getPhotoUrl().toString();
						key = mFirebaseDatabaseReference.child(TABLE_DB_USERS).push().getKey();
						User user = new User(firebaseUser.getUid(), firebaseUser.isAnonymous() ? "Anonymous" : firebaseUser.getDisplayName(), photoUrl, key);
						mFirebaseDatabaseReference.child(TABLE_DB_USERS).child(key).setValue(user);
					} else {
						Log.w(TAG, "exists");
					}
					Log.w(TAG, "ID=" + key);
					Log.w(TAG, "ID=" + firebaseUser.getUid());
					prefs.edit().putString(SETTINGS_MY_UID, key).apply();
					myUid = key;
					if (prefs.getString(SETTINGS_DB_TOKEN, null) != null) {
						Log.w("TAG", "SETTINGS_DB_TOKEN: " + prefs.getString(SETTINGS_DB_TOKEN, null));
						mFirebaseDatabaseReference.child(TABLE_DB_USERS).child(key).child(FIELD_DB_TOKEN).setValue(prefs.getString(SETTINGS_DB_TOKEN, null));
					} else Log.e("SignIn", "TOKEN NOT FOUND");
					notifyRecsStatus();
				}

				@Override
				public void onCancelled(DatabaseError databaseError) {
					prefs.edit().putString(SETTINGS_MY_UID, null).apply();
					myUid = null;
					notifyRecsStatus();
				}
			});
		} else {
			Log.e(TAG, "LOGIN ERROR");
		}
	}


	public void requestUpdates() throws SecurityException {
		if(gApi.status()!=NONE) gApi.connect(true, connectAction);

	}
	public void checkOutdatedDbData(){
		FirebaseDatabase.getInstance().getReference().child(TABLE_DB_USER_STATUSES).child(myUid).child(FIELD_DB_USER_POSITIONS).addChildEventListener(
				new ChildEventListener() {
					@Override
					public void onChildAdded(DataSnapshot dataSnapshot, String s) {
						ModelUserPosition modelUserPosition = dataSnapshot.getValue(ModelUserPosition.class);
						Date dateNow = new Date();
						Date dateDb = new Date(modelUserPosition.getTimestamp());
						SimpleDateFormat dateFormat=new SimpleDateFormat("DD");
						if(!TextUtils.equals(dateFormat.format(dateNow), dateFormat.format(dateDb))) {
							FirebaseDatabase.getInstance().getReference().child(TABLE_DB_USER_STATUSES).child(myUid).child(FIELD_DB_USER_POSITIONS).updateChildren(null);
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
		if(gApi.status()==CONNECTED) {
			gApi.removeConnect(disconnectAction);
		}
	}


	private PendingIntent getActivityDetectionPendingIntent() {
		Intent intent = new Intent(this, MeetMeService.class);
		return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}
	private PendingIntent getLocationDetectionPendingIntent() {
		Intent intent = new Intent(this, MeetMeService.class);
		return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
	}


	private void handleActivity(ActivityRecognitionResult resultA) {
		List<DetectedActivity> detectedActivities = resultA.getProbableActivities();
		lastModelUserActivity = getDbUserActivity(detectedActivities, this);
		if(lastModelUserActivity == null) {
			Log.w("DETECTED ACTIVITY: ", "null");
			mFirebaseDatabaseReference.child(TABLE_DB_USER_STATUSES).child(myUid)
					.child(FIELD_DB_USER_ACTIVITY).setValue(null);

		}else {
			Log.w("DETECTED ACTIVITY: ", lastModelUserActivity.toString());
			if (myUid != null && lastModelUserActivity != null) {
				lastModelUserActivity.setTimestamp(resultA.getTime());
				mFirebaseDatabaseReference.child(TABLE_DB_USER_STATUSES).child(myUid)
						.child(FIELD_DB_USER_ACTIVITY).setValue(lastModelUserActivity);
			}
			notifyRecsActivity(lastModelUserActivity);
		}
	}

	private void handleLocation(LocationResult locationResult) {
			Location location = locationResult.getLastLocation();
			if (location != null) {
				Log.d("locationtesting", "accuracy: " + location.getAccuracy() + " lat: " + location.getLatitude() + " lon: " + location.getLongitude());

				ModelUserPosition modelUserPosition = new ModelUserPosition();
				modelUserPosition.setLat(location.getLatitude());
				modelUserPosition.setLng(location.getLongitude());
				modelUserPosition.setTimestamp(location.getTime());
				modelUserPosition.setAccuracy(location.getAccuracy());

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
					modelUserPosition.setPlaceAddress(addressLines);
					//userPosition.setPlaceName(address.getPremises());
				}

				if(lastModelUserPosition !=null && lastUserPositionKey!=null){
					if(modelUserPosition.getLat()== lastModelUserPosition.getLat() && modelUserPosition.getLng()== lastModelUserPosition.getLng()){
						mFirebaseDatabaseReference.child(TABLE_DB_USER_STATUSES).child(myUid)
								.child(FIELD_DB_USER_POSITIONS).child(lastUserPositionKey).setValue(modelUserPosition);
					}
				}else {
					lastUserPositionKey = mFirebaseDatabaseReference.child(TABLE_DB_USER_STATUSES).child(myUid)
							.child(FIELD_DB_USER_POSITIONS).push().getKey();
					lastModelUserPosition=modelUserPosition;
					mFirebaseDatabaseReference.child(TABLE_DB_USER_STATUSES).child(myUid)
							.child(FIELD_DB_USER_POSITIONS).child(lastUserPositionKey).setValue(modelUserPosition);
				}
				notifyRecsLocation(modelUserPosition);
				return;
			} else Log.e(TAG, "locationResult ERROR no location ");
		}



	public static ModelUserActivity getDbUserActivity(List<DetectedActivity> detectedActivities, Context context) {
		Log.i(TAG, "activities detected");
		if (detectedActivities == null || detectedActivities.isEmpty()){
			Log.e("ERROR", "activities empty");
			return null;
		}
		else {
			ModelUserActivity modelUserActivity = new ModelUserActivity();
			DetectedActivity da=null;
			if(detectedActivities.size()>0 && detectedActivities.get(0)!=null){
				da = detectedActivities.get(0);
				Log.w(TAG, "DetectedActivity0"+Constants.getActivityString(
						context,
						da.getType()) + " " + da.getConfidence() + "%");
				modelUserActivity.setActivity0(Constants.getActivityString(context, da.getType())+" "+da.getConfidence()+"%");
			}
			if(detectedActivities.size()>1 && detectedActivities.get(1)!=null){
				da = detectedActivities.get(1);
				Log.w(TAG, "DetectedActivity1"+Constants.getActivityString(
						context,
						da.getType()) + " " + da.getConfidence() + "%");
				modelUserActivity.setActivity1(Constants.getActivityString(context, da.getType())+" "+da.getConfidence()+"%");
			}
			if(detectedActivities.size()>2 && detectedActivities.get(2)!=null){
				da = detectedActivities.get(2);
				Log.w(TAG, "DetectedActivity2"+Constants.getActivityString(
						context,
						da.getType()) + " " + da.getConfidence() + "%");
				modelUserActivity.setActivity2(Constants.getActivityString(context, da.getType())+" "+da.getConfidence()+"%");
			}
			return modelUserActivity;
		}
	}

	public boolean notifyRecsStatus() {
		Bundle bundle=new Bundle();
		bundle.putBoolean(INTENT_EXTRA_MY_UPDATES_STATUS, myUpdatesStatus);
		bundle.putBoolean(INTENT_EXTRA_GAPI_STATUS, gApiStatus);
		bundle.putString(INTENT_EXTRA_MY_DB_UID, myUid);
		return notifyRecs(gApiStatus ? Activity.RESULT_OK : Activity.RESULT_CANCELED, bundle);
	}

	public boolean notifyRecsActivity(ModelUserActivity activity) {
		Bundle bundle=new Bundle();
		bundle.putBoolean(INTENT_EXTRA_MY_UPDATES_STATUS, myUpdatesStatus);
		bundle.putBoolean(INTENT_EXTRA_GAPI_STATUS, gApiStatus);
		bundle.putString(INTENT_EXTRA_MY_DB_UID, myUid);
		bundle.putSerializable(INTENT_EXTRA_MY_ACTIVITY, activity);
		return notifyRecs(activity!=null ? Activity.RESULT_OK : Activity.RESULT_CANCELED, bundle);
	}
	public boolean notifyRecsLocation(ModelUserPosition position) {
		Bundle bundle=new Bundle();
		bundle.putSerializable(FIELD_DB_USER_POSITIONS, position);

		bundle.putBoolean(INTENT_EXTRA_MY_UPDATES_STATUS, myUpdatesStatus);
		bundle.putBoolean(INTENT_EXTRA_GAPI_STATUS, gApiStatus);
		bundle.putString(INTENT_EXTRA_MY_DB_UID, myUid);
		return notifyRecs(position!=null ? Activity.RESULT_OK : Activity.RESULT_CANCELED, bundle);
	}

	public boolean notifyRecs(int responceCode, Bundle bundle){
		boolean res=false;
		for(ResultReceiver resultReceiver : recs){
			resultReceiver.send(responceCode, bundle);
			res=true;
		}
		return res;
	}


}
