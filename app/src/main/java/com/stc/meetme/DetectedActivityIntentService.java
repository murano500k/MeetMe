package com.stc.meetme;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.stc.meetme.model.UserActivity;

import java.util.ArrayList;

import static com.stc.meetme.Constants.FIELD_DB_USER_ACTIVITY;
import static com.stc.meetme.Constants.TABLE_DB_USER_STATUSES;

public class DetectedActivityIntentService extends IntentService {

	protected static final String TAG = "DetectedActivitiesIS";

	private SharedPreferences prefs;

	UserActivity mDetectedActivity;

	private DatabaseReference mFirebaseDatabaseReference;

	private String currentUserId;

	public DetectedActivityIntentService() {
		// Use the TAG to name the worker thread.
		super(TAG);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
		currentUserId = prefs.getString(Constants.SETTINGS_DB_UID, null);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
		Intent localIntent = new Intent(Constants.BROADCAST_ACTION);
		ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();

		mDetectedActivity = getDbUserActivity(detectedActivities, this);

		if(currentUserId!=null && mDetectedActivity!=null){
			mDetectedActivity.setTimestamp(result.getTime());
			mFirebaseDatabaseReference.child(TABLE_DB_USER_STATUSES).child(currentUserId)
					.child(FIELD_DB_USER_ACTIVITY).setValue(mDetectedActivity);
		}

		localIntent.putExtra(Constants.ACTIVITY_EXTRA, detectedActivities);
		LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
	}

	public static UserActivity getDbUserActivity(ArrayList<DetectedActivity> detectedActivities, Context context) {
		Log.i(TAG, "activities detected");
		if (detectedActivities == null || detectedActivities.isEmpty()){
			Log.e("ERROR", "activities empty");
		return null;
		}
		else {
			DetectedActivity da = detectedActivities.get(0);
			Log.w(TAG, Constants.getActivityString(
					context,
					da.getType()) + " " + da.getConfidence() + "%"
			);
			UserActivity userActivity = new UserActivity();
			userActivity.setActivity(Constants.getActivityString(context, da.getType()));
			userActivity.setConfidence( da.getConfidence());
			return userActivity;
		}
	}
}
