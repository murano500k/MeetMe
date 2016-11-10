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

import java.util.ArrayList;

import static com.stc.meetme.Constants.TABLE_DB_DATA;

public class DetectedActivityIntentService extends IntentService {

	protected static final String TAG = "DetectedActivitiesIS";

	private SharedPreferences prefs;

	String mDetectedActivity;

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

		// Get the list of the probable activities associated with the current state of the
		// device. Each activity is associated with a confidence level, which is an int between
		// 0 and 100.
		ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();

		// Log each activity.
		mDetectedActivity = getActString(detectedActivities, this);
		if(currentUserId!=null && mDetectedActivity!=null)
			mFirebaseDatabaseReference.child(TABLE_DB_DATA).child(currentUserId).setValue(mDetectedActivity);

			// Broadcast the list of detected activities.
			localIntent.putExtra(Constants.ACTIVITY_EXTRA, detectedActivities);
			LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
	}

	public static String getActString(ArrayList<DetectedActivity> detectedActivities, Context context) {
		Log.i(TAG, "activities detected");
		if (detectedActivities == null || detectedActivities.isEmpty()){
			Log.e("ERROR", "activities empty");
		return "activities empty";
		}
		else {
			DetectedActivity da = detectedActivities.get(0);
			Log.w(TAG, Constants.getActivityString(
					context,
					da.getType()) + " " + da.getConfidence() + "%"
			);
			return Constants.getActivityString(
					context,
					da.getType()) + " " + da.getConfidence() + "%";
		}
	}

}
