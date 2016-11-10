package com.stc.meetme.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.airbnb.deeplinkdispatch.DeepLink;
import com.google.android.gms.awareness.Awareness;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.LocationServices;
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
import com.stc.meetme.R;
import com.stc.meetme.model.User;
import com.stc.meetme.model.UserActivity;
import com.stc.meetme.model.UserPosition;
import com.stc.meetme.model.UserStatus;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.stc.meetme.Constants.FIELD_DB_TOKEN;
import static com.stc.meetme.Constants.FIELD_DB_USER_ACTIVITY;
import static com.stc.meetme.Constants.FIELD_DB_USER_POSITION;
import static com.stc.meetme.Constants.INTENT_EXTRA_OBSERVE_UID;
import static com.stc.meetme.Constants.SETTINGS_DB_TOKEN;
import static com.stc.meetme.Constants.SETTINGS_DB_UID;
import static com.stc.meetme.Constants.TABLE_DB_USERS;
import static com.stc.meetme.Constants.TABLE_DB_USER_STATUSES;
import static java.lang.System.currentTimeMillis;

@DeepLink("https://f3x9u.app.goo.gl/observe/{id}")
public class ObserveActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

	private static final String TAG = "ObserveActivity";
	@BindView(R.id.progressBar)
	ProgressBar mProgressBar;
	@BindView(R.id.textViewPosition)
	TextView textPosition;

	@BindView(R.id.textViewTime)
	TextView textTime;

	@BindView(R.id.textViewConfidence)
	TextView textConfidence;

	@BindView(R.id.textViewActivity)
	TextView textActivity;

	String observableUserId;

	String positionString;

	String activityString;

	String timeString;

	String confidenceString;

	private SharedPreferences prefs;

	private DatabaseReference mFirebaseDatabaseReference;

	private ValueEventListener statusUpdatesListener;
	private FirebaseAuth mFirebaseAuth;

	@BindView(R.id.activity_layout)
	private View mLayout;
	private FirebaseUser mFirebaseUser;
	private String currentUserId;
	private MenuItem menuSharePosition, menuRefresh;
	private GoogleApiClient mGoogleApiClient;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_observe);
		ButterKnife.bind(this);
		mProgressBar=(ProgressBar) findViewById(R.id.progressBar);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setHomeButtonEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle("Observing realtime status");
		}
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
		mFirebaseAuth = FirebaseAuth.getInstance();
		mFirebaseUser = mFirebaseAuth.getCurrentUser();
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

	private void checkIntent(){
		Intent intent=getIntent();
		if(intent==null && intent.getExtras()==null) {
			showToast("NO OBSERVABLE USERID");
			return;
		}
		if (intent.getBooleanExtra(DeepLink.IS_DEEP_LINK, false)) {
			Bundle parameters = intent.getExtras();
			observableUserId = parameters.getString("id");
			Log.d(TAG, "checkIntent: isDeepLink");
		}else {
			Bundle parameters = intent.getExtras();
			observableUserId = parameters.getString(INTENT_EXTRA_OBSERVE_UID);
			Log.d(TAG, "checkIntent: isRegularIntent");

		}
		if(observableUserId==null) {
			showToast("NO OBSERVABLE USERID");
			return;
		}else {
			registerDbUpdates(observableUserId);
		}
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menuSharePosition=menu.add("share position");
		menuRefresh=menu.add("refresh");
		return super.onCreateOptionsMenu(menu);

	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(menuSharePosition.getItemId()==item.getItemId()) {
			Intent intent=new Intent(this, StartActivity.class);
			startActivity(intent);
		}else if(menuRefresh.getItemId()==item.getItemId()) {
			if (statusUpdatesListener != null) {
				Timber.w("removeEventListener");
				mFirebaseDatabaseReference.removeEventListener(statusUpdatesListener);
			}
			if (observableUserId != null && currentUserId != null)
				registerDbUpdates(observableUserId);
		}
		return super.onOptionsItemSelected(item);
	}


	private void signInAnonymously() {
		Log.d(TAG, "signInAnonymously");

		mProgressBar.setVisibility(VISIBLE);
		mFirebaseAuth.signInAnonymously()
				.addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
					@Override
					public void onComplete(@NonNull Task<AuthResult> task) {
						Log.d(TAG, "signInAnonymously:onComplete:" + task.isSuccessful());

						if (!task.isSuccessful()) {
							Log.w(TAG, "signInAnonymously", task.getException());
							Toast.makeText(ObserveActivity.this, "Authentication failed.",
									Toast.LENGTH_SHORT).show();
						} else {
							addUserToDb();

						}
						mProgressBar.setVisibility(GONE);
					}
				});
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(statusUpdatesListener!=null) {
			Timber.w("removeEventListener");
			mFirebaseDatabaseReference.removeEventListener(statusUpdatesListener);
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(observableUserId!=null && currentUserId!=null)registerDbUpdates(observableUserId);
		else showToast("observableUserId==null");
	}


	public void registerDbUpdates(String observableUserId){
		Timber.w("registerDbUpdates uid: %s", observableUserId);
		if(statusUpdatesListener==null) {
			statusUpdatesListener = new ValueEventListener() {
				@Override
				public void onDataChange(DataSnapshot dataSnapshot) {

					Timber.w("dataSnapshot: %S",dataSnapshot.toString());
					UserStatus userStatus = null;
					if (dataSnapshot == null) {
						showToast("ERROR userStatus NULL");
						return;
					}
					//UserActivity userActivity = userStatus.getUserActivity();
					UserActivity userActivity = dataSnapshot.child(FIELD_DB_USER_ACTIVITY).getValue(UserActivity.class);
					if (userActivity != null) {
						if (userActivity.getActivity() != null)
							activityString = "current activity: " + userActivity.getActivity();
						else activityString = "current activity: ";
						if (userActivity.getConfidence() > 0)
							confidenceString = userActivity.getConfidence() + "%";
						else confidenceString = "NULL";
						//timeString = userActivity.getFormattedDateTime(getApplicationContext());
						timeString=""+((currentTimeMillis()-userActivity.getTimestamp())/1000)+" seconds elapsed";

						if (timeString == null) timeString = "NULL";
					}
					Timber.w("activityString: "+activityString);
					Timber.w("confidenceString: "+confidenceString);
					Timber.w("timeString: "+timeString);

					textActivity.setText(activityString);
					textConfidence.setText(confidenceString);
					textTime.setText(timeString);

					//UserPosition userPosition = userStatus.getUserPosition();
					UserPosition userPosition = dataSnapshot.child(FIELD_DB_USER_POSITION).getValue(UserPosition.class);

					positionString = "";
					if (userPosition != null) {
						positionString += userPosition.getFormattedDateTime(getApplicationContext()) + "\n";
						positionString += "current place: " + userPosition.getPlaceName() + "\n";
						positionString += "address: " + userPosition.getPlaceAddress() + "\n";
						positionString += "accuracy: " + userPosition.getAccuracy() + "m\n";
						positionString += "lat: " + userPosition.getLat() + "\n";
						positionString += "long: " + userPosition.getLong();
					}
					Timber.w("position: "+positionString);
					textPosition.setText(positionString);
					mProgressBar.setVisibility(GONE);

				}

				@Override
				public void onCancelled(DatabaseError databaseError) {
					textActivity.setText("cancelled");
					textConfidence.setText("cancelled");
					textTime.setText("cancelled");
					textPosition.setText("cancelled");
					mProgressBar.setVisibility(GONE);

				}
			};
		}
		mFirebaseDatabaseReference.child(TABLE_DB_USER_STATUSES)
				.child(observableUserId).addValueEventListener(statusUpdatesListener);
	}


	protected void showToast(String text) {
		Log.w("OBSERVE", text);
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}


	public void addUserToDb(){
		Log.d(TAG, "addUserToDb");

		FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();
		mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
		if(firebaseUser!=null){
			final String uid = firebaseUser.getUid();
			mFirebaseDatabaseReference.child(TABLE_DB_USERS).addListenerForSingleValueEvent(new ValueEventListener() {
				@Override
				public void onDataChange(DataSnapshot dataSnapshot) {
					User user=null;
					boolean exists=false;
					String key=null;

					for(DataSnapshot child : dataSnapshot.getChildren()){
						user = child.getValue(User.class);
						if(TextUtils.equals(user.getUserId(), uid)) {
							exists=true;
							key=child.getKey();
							break;
						}
					}
					if(!exists) {
						createUser();
					}else
						saveUidToPrefs(key,user.getName());
				}

				@Override
				public void onCancelled(DatabaseError databaseError) {
					prefs.edit().putString(SETTINGS_DB_UID, null).apply();
					Toast.makeText(ObserveActivity.this, "CANCELLED", Toast.LENGTH_SHORT).show();
					mProgressBar.setVisibility(GONE);
					Log.e(TAG, "onCancelled");

				}
			});
		}



	}
	private void saveUidToPrefs(String uId, String username) {
		Log.w("TAG", "new uid to save: "+uId);
		prefs.edit().putString(SETTINGS_DB_UID, uId).apply();
		if(prefs.getString(SETTINGS_DB_TOKEN, null)!=null) {
			Log.w("TAG", "SETTINGS_DB_TOKEN: "+prefs.getString(SETTINGS_DB_TOKEN, null));
			mFirebaseDatabaseReference.child(TABLE_DB_USERS).child(uId).child(FIELD_DB_TOKEN).setValue(prefs.getString(SETTINGS_DB_TOKEN, null));
		}else Log.e("SignIn", "TOKEN NOT FOUND");
		Toast.makeText(this, "Welcome " + username, Toast.LENGTH_SHORT).show();
		checkIntent();
	}


	private void createUser() {
		FirebaseUser firebaseUser = mFirebaseAuth.getCurrentUser();
		String photoUrl=null;
		if(firebaseUser!=null){
			if(firebaseUser.getPhotoUrl()!=null)
				photoUrl=firebaseUser.getPhotoUrl().toString();
			String key=mFirebaseDatabaseReference.child(TABLE_DB_USERS).push().getKey();

			User user=new User(firebaseUser.getUid(),firebaseUser.isAnonymous() ? "Anonymous": firebaseUser.getDisplayName(), photoUrl,key);
			mFirebaseDatabaseReference.child(TABLE_DB_USERS).child(key).setValue(user);
			saveUidToPrefs(key, firebaseUser.getDisplayName());
		}else Log.e(TAG, "createUser: ERROR");
	}

	@Override
	public void onConnected(@Nullable Bundle bundle) {
		Log.d(TAG, "onConnected");

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
		} else {
			signInAnonymously();
			return;
		}

	}

	@Override
	public void onConnectionSuspended(int i) {
		Log.e(TAG, "onConnectionSuspended");
		mProgressBar.setVisibility(GONE);

	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
		Log.e(TAG, "onConnectionFailed "+connectionResult.toString());
		mProgressBar.setVisibility(GONE);

	}
}
