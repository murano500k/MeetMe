package com.stc.meetme;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

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
import com.stc.meetme.model.User;
import com.stc.meetme.ui.ObserveInfoProvider;

import static com.stc.meetme.Constants.FIELD_DB_TOKEN;
import static com.stc.meetme.Constants.SETTINGS_DB_TOKEN;
import static com.stc.meetme.Constants.SETTINGS_MY_UID;
import static com.stc.meetme.Constants.TABLE_DB_USERS;

/**
 * Created by artem on 12/16/16.
 */

public class GoogleApiHelper implements
		GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener  {
	private static final String TAG = "GoogleApiHelper";
	private Context mContext;
	private ObserveInfoProvider provider;

	protected SharedPreferences prefs;

	protected DatabaseReference mFirebaseDatabaseReference;

	protected FirebaseAuth mFirebaseAuth;


	protected GoogleApiClient mGoogleApiClient;

	public GoogleApiHelper(Context context) {
		mContext=context;
		provider =(ObserveInfoProvider)context;
		prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
		mFirebaseAuth = FirebaseAuth.getInstance();
	}


	public void disconnect(){

		if (mGoogleApiClient!=null && mGoogleApiClient.isConnected())
			mGoogleApiClient.disconnect();
	}

	public void connect(String currentUserId){
		if(mGoogleApiClient==null) setupGoogleApiClient();
		else if(mGoogleApiClient.isConnected()){
			if(currentUserId==null) signInAnonymously();
		}
	}



	private void setupGoogleApiClient() {
		Log.d(TAG, "setupGoogleApiClient");
		mGoogleApiClient = new GoogleApiClient.Builder(mContext)
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
	}


	@Override
	public void onConnectionSuspended(int i) {
		Log.e(TAG, "onConnectionSuspended");
		disconnect();
	}

	@Override
	public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
		Log.e(TAG, "onConnectionFailed "+connectionResult.toString());
	}

	private void signInAnonymously() {
		Log.w(TAG, "signInAnonymously");
		mFirebaseAuth.signInAnonymously()
				.addOnCompleteListener((Activity)mContext, new OnCompleteListener<AuthResult>() {
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
		if(firebaseUser!=null) {
			Log.e(TAG, "firebaseUser!=null");

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
					if (prefs.getString(SETTINGS_DB_TOKEN, null) != null) {
						Log.w("TAG", "SETTINGS_DB_TOKEN: " + prefs.getString(SETTINGS_DB_TOKEN, null));
						mFirebaseDatabaseReference.child(TABLE_DB_USERS).child(key).child(FIELD_DB_TOKEN).setValue(prefs.getString(SETTINGS_DB_TOKEN, null));
					} else Log.e("SignIn", "TOKEN NOT FOUND");
				}

				@Override
				public void onCancelled(DatabaseError databaseError) {
					prefs.edit().putString(SETTINGS_MY_UID, null).apply();
					Log.e(TAG, "onCancelled");
					disconnect();
				}
			});
		}
		else {
			Log.e(TAG,"LOGIN ERROR");
		}
	}
}