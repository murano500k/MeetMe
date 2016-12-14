package com.stc.meetme;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.stc.meetme.model.UserActivity;

import java.util.List;
import java.util.Set;

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
		currentUserId = prefs.getString(Constants.SETTINGS_MY_UID, null);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
		if(result!=null) {

			List<DetectedActivity> detectedActivities = result.getProbableActivities();
			mDetectedActivity = getDbUserActivity(detectedActivities, this);
			if(mDetectedActivity == null) {
				Log.w("DETECTED ACTIVITY: ", "null");
				mFirebaseDatabaseReference.child(TABLE_DB_USER_STATUSES).child(currentUserId)
						.child(FIELD_DB_USER_ACTIVITY).setValue(null);
			}else {
				Log.w("DETECTED ACTIVITY: ", mDetectedActivity.toString());
				if (currentUserId != null && mDetectedActivity != null) {
					mDetectedActivity.setTimestamp(result.getTime());
					mFirebaseDatabaseReference.child(TABLE_DB_USER_STATUSES).child(currentUserId)
						.child(FIELD_DB_USER_ACTIVITY).setValue(mDetectedActivity);
				}
			}
		}else {

			Set<String> keys=null;
			if(intent.getExtras()!=null ) keys=intent.getExtras().keySet();
			if(keys!=null){
				for(String key: keys) Log.e(TAG, "key: "+key+" value: "+intent.getExtras().get(key) );
			}
		}
	}

	public static UserActivity getDbUserActivity(List<DetectedActivity> detectedActivities, Context context) {
		Log.i(TAG, "activities detected");
		if (detectedActivities == null || detectedActivities.isEmpty()){
			Log.e("ERROR", "activities empty");
		return null;
		}
		else {
			UserActivity userActivity = new UserActivity();
			DetectedActivity da=null;
			if(detectedActivities.size()>0 && detectedActivities.get(0)!=null){
				da = detectedActivities.get(0);
				Log.w(TAG, "DetectedActivity0"+Constants.getActivityString(
						context,
						da.getType()) + " " + da.getConfidence() + "%");
				userActivity.setActivity0(Constants.getActivityString(context, da.getType())+" "+da.getConfidence()+"%");
			}
			if(detectedActivities.size()>1 && detectedActivities.get(1)!=null){
				da = detectedActivities.get(1);
				Log.w(TAG, "DetectedActivity1"+Constants.getActivityString(
						context,
						da.getType()) + " " + da.getConfidence() + "%");
				userActivity.setActivity1(Constants.getActivityString(context, da.getType())+" "+da.getConfidence()+"%");
			}
			if(detectedActivities.size()>2 && detectedActivities.get(2)!=null){
				da = detectedActivities.get(2);
				Log.w(TAG, "DetectedActivity2"+Constants.getActivityString(
						context,
						da.getType()) + " " + da.getConfidence() + "%");
				userActivity.setActivity2(Constants.getActivityString(context, da.getType())+" "+da.getConfidence()+"%");
			}
			return userActivity;
		}
	}
}
