package com.stc.meetme;

import android.content.Context;
import android.content.res.Resources;

import com.google.android.gms.location.DetectedActivity;

/**
 * Created by artem on 10/27/16.
 */
public final class Constants {

	private Constants() {
	}

	public static final  long UPDATE_INTERVAL = 10 * 1000;  /* 10 secs */
	public static final  long FASTEST_INTERVAL = 2000; /* 2 sec */

	public static final int PERMISSION_REQUEST_FINE_LOCATION = 941;

	public static final int CALLBACK_ACTIVITY = 0;
	public static final int CALLBACK_LOCATION = 1;

	public static final String PACKAGE_NAME = "com.google.android.gms.location.activityrecognition";

	public static final String BROADCAST_ACTION = PACKAGE_NAME + ".BROADCAST_ACTION";

	public static final String ACTIVITY_EXTRA = PACKAGE_NAME + ".ACTIVITY_EXTRA";

	public static final String SHARED_PREFERENCES_NAME = PACKAGE_NAME + ".SHARED_PREFERENCES";

	public static final String ACTIVITY_UPDATES_REQUESTED_KEY = PACKAGE_NAME +
			".ACTIVITY_UPDATES_REQUESTED";
	public static final String LOCATION_UPDATES_REQUESTED_KEY = PACKAGE_NAME +
			".LOCATION_UPDATES_REQUESTED";

	public static final String DETECTED_ACTIVITY = PACKAGE_NAME + ".DETECTED_ACTIVITY";

	/**
	 * The desired time between activity detections. Larger values result in fewer activity
	 * detections while improving battery life. A value of 0 results in activity detections at the
	 * fastest possible rate. Getting frequent updates negatively impact battery life and a real
	 * app may prefer to request less frequent updates.
	 */
	public static final long DETECTION_INTERVAL_IN_MILLISECONDS = 0;

	/**
	 * List of DetectedActivity types that we monitor in this sample.
	 */
	protected static final int[] MONITORED_ACTIVITIES = {
			DetectedActivity.STILL,
			DetectedActivity.ON_FOOT,
			DetectedActivity.WALKING,
			DetectedActivity.RUNNING,
			DetectedActivity.ON_BICYCLE,
			DetectedActivity.IN_VEHICLE,
			DetectedActivity.TILTING,
			DetectedActivity.UNKNOWN
	};
	public static final String ARG_OBSERVE_ID = "com.stc.firebase.android.chat.ARG_OBSERVE_ID";
	public static final String ARG_COLUMN_COUNT = "column-count";
	public static final String ARG_LIST_TYPE = "ARG_LIST_TYPE";
	public static final String ARG_LIST_SUBSCRIPTIONS = "ARG_LIST_SUBSCRIPTIONS";
	public static final String SETTINGS_STATUS_ACTIVE = "SETTINGS_STATUS_ACTIVE";

	public static final int LIST_TYPE_MY_TARGETS = 623;
	public static final int LIST_TYPE_MY_OBSERVERS = 622;
	public static final String INTENT_EXTRA_OBSERVE_UID = "com.stc.firebase.android.chat.INTENT_EXTRA_OBSERVE_UID";
	public static final String INTENT_EXTRA_OBSERVE_LAT = "com.stc.firebase.android.chat.INTENT_EXTRA_OBSERVE_LAT";
	public static final String INTENT_EXTRA_OBSERVE_LNG = "com.stc.firebase.android.chat.INTENT_EXTRA_OBSERVE_LNG";


	public static final String SETTINGS_DB_TOKEN = "com.stc.firebase.android.chat.SETTINGS_DB_TOKEN";
	public static final String SETTINGS_IS_ACTIVE = "com.stc.firebase.android.chat.ui.SETTINGS_IS_ACTIVE";
	public static final String SETTINGS_CHATTING_WITH = "com.stc.firebase.android.chat.ui.SETTINGS_CHATTING_WITH";

	public static final String SETTINGS_MY_UID = "com.stc.firebase.android.chat.SETTINGS_MY_UID";
	public static final String SETTINGS_OBSERVE_UID = "com.stc.firebase.android.chat.SETTINGS_OBSERVE_UID";

	public static final String TABLE_DB_USERS = "users";
	public static final String TABLE_DB_USER_STATUSES = "user_statuses";
	public static final String TABLE_DB_SUBSCRIPTIONS = "subscriptions";

	public static final String FIELD_DB_SUBSCRIPTION_TARGET_ID = "target_id";
	public static final String FIELD_DB_SUBSCRIPTION_OBSERVER_ID = "observer_id";
	public static final String FIELD_DB_SUBSCRIPTION_ACTIVE = "active";


	public static final String FIELD_DB_USER_POSITION = "user_position";
	public static final String FIELD_DB_PLACE_NAME = "place_name";
	public static final String FIELD_DB_PLACE_ADDRESS = "place_address";
	public static final String FIELD_DB_LONG = "long";
	public static final String FIELD_DB_LAT = "lat";
	public static final String FIELD_DB_POSITION_TIMESTAMP= "position_timestamp";
	public static final String FIELD_DB_POSITION_ACCURACY = "position_accuracy";

	public static final String FIELD_DB_USER_ACTIVITY = "user_activity";
	public static final String FIELD_DB_ACTIVITY_TIMESTAMP= "activity_timestamp";
	public static final String FIELD_DB_ACTIVITY = "activity";
	public static final String FIELD_DB_ACTIVITY_CONFIDENCE = "activity_confidence";



	public static final String FIELD_DB_NAME = "name";
	public static final String FIELD_DB_TOKEN = "token";
	public static final String FIELD_DB_EMAIL = "email";
	public static final String FIELD_DB_PHOTOURL = "photoUrl";
	public static final String FIELD_DB_USERID = "userId";

	public static final String FIELD_DB_TEXT = "text";
	public static final String FIELD_DB_SENDERID = "senderId";
	public static final String FIELD_DB_RECIEVERID = "receiverId";


	public static final String TABLE_DB_MESSAGES = "messages";
	public static final int NOTIFICATION_ID_CHAT = 1465;
	public static final int NOTIFICATION_ID_MAIN = 889;
	public static final int NOTIFICATION_PENDING_INTENT_CHAT = 4456;
	public static final int NOTIFICATION_PENDING_INTENT_MAIN = 326;

	public static final String INSTANCE_ID_TOKEN_RETRIEVED = "iid_token_retrieved";
	public static final String FRIENDLY_MSG_LENGTH = "friendly_msg_length";

	public static final String INTENT_ACTION_CHAT = "com.stc.firebase.android.chat.INTENT_ACTION_CHAT";

	public static final int SUCCESS_RESULT = 0;

	public static final int FAILURE_RESULT = 1;


	public static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";

	public static final String RESULT_DATA_KEY = PACKAGE_NAME + ".RESULT_DATA_KEY";

	public static final String LOCATION_DATA_EXTRA = PACKAGE_NAME + ".LOCATION_DATA_EXTRA";


	public static final String ADDRESS_REQUESTED_KEY = "address-request-pending";

	public static final String LOCATION_ADDRESS_KEY = "location-address";

	public static String getActivityString(Context context, int detectedActivityType) {
		Resources resources = context.getResources();
		switch(detectedActivityType) {
			case DetectedActivity.IN_VEHICLE:
				return resources.getString(R.string.in_vehicle);
			case DetectedActivity.ON_BICYCLE:
				return resources.getString(R.string.on_bicycle);
			case DetectedActivity.ON_FOOT:
				return resources.getString(R.string.on_foot);
			case DetectedActivity.RUNNING:
				return resources.getString(R.string.running);
			case DetectedActivity.STILL:
				return resources.getString(R.string.still);
			case DetectedActivity.TILTING:
				return resources.getString(R.string.tilting);
			case DetectedActivity.UNKNOWN:
				return resources.getString(R.string.unknown);
			case DetectedActivity.WALKING:
				return resources.getString(R.string.walking);
			default:
				return resources.getString(R.string.unidentifiable_activity, detectedActivityType);
		}
	}
}

