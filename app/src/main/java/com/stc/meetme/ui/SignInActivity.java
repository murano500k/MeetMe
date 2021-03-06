/**
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stc.meetme.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.stc.meetme.R;
import com.stc.meetme.model.User;

import butterknife.BindView;

import static com.stc.meetme.Constants.FIELD_DB_TOKEN;
import static com.stc.meetme.Constants.SETTINGS_DB_TOKEN;
import static com.stc.meetme.Constants.SETTINGS_MY_UID;
import static com.stc.meetme.Constants.TABLE_DB_USERS;

public class SignInActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;
    private SignInButton mSignInButton;
	private ProgressBar mProgressBar;

    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mFirebaseAuth;
	SharedPreferences prefs;
	private DatabaseReference mFirebaseDatabaseReference;
	@BindView(R.id.buttonAnon)
	Button buttonAnon;
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
		mSignInButton = (SignInButton) findViewById(R.id.sign_in_button);

		mProgressBar.setVisibility(View.VISIBLE);
		mSignInButton.setVisibility(View.GONE);

        mSignInButton.setOnClickListener(this);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this )
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        mFirebaseAuth = FirebaseAuth.getInstance();
		mProgressBar.setVisibility(View.GONE);
		mSignInButton.setVisibility(View.VISIBLE);
		buttonAnon.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				signInAnonymously();
			}
		});
    }



    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                signInGoogle();
                break;
        }
    }
	private void signInAnonymously() {

		mProgressBar.setVisibility(View.VISIBLE);
		mSignInButton.setVisibility(View.GONE);
		buttonAnon.setVisibility(View.GONE);
		// [START signin_anonymously]
		mFirebaseAuth.signInAnonymously()
				.addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
					@Override
					public void onComplete(@NonNull Task<AuthResult> task) {
						Log.d(TAG, "signInAnonymously:onComplete:" + task.isSuccessful());

						if (!task.isSuccessful()) {
							Log.w(TAG, "signInAnonymously", task.getException());
							Toast.makeText(SignInActivity.this, "Authentication failed.",
									Toast.LENGTH_SHORT).show();
						} else {
							addUserToDb();

						}
						mProgressBar.setVisibility(View.GONE);
						mSignInButton.setVisibility(View.VISIBLE);
						buttonAnon.setVisibility(View.VISIBLE);
					}
				});
	}

    private void signInGoogle() {
	    mProgressBar.setVisibility(View.VISIBLE);
	    mSignInButton.setVisibility(View.GONE);
	    Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);

            } else {
                // Google Sign In failed
                Log.e(TAG, "Google Sign In failed.");
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());
        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mFirebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(SignInActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
	                        addUserToDb();

                        }
                    }
                });
    }
	public void addUserToDb(){
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
					prefs.edit().putString(SETTINGS_MY_UID, null).apply();
					Toast.makeText(SignInActivity.this, "CANCELLED", Toast.LENGTH_SHORT).show();
					mProgressBar.setVisibility(View.GONE);
					mSignInButton.setVisibility(View.VISIBLE);
					Log.e(TAG, "onCancelled");
				}
			});
		}



	}
	private void saveUidToPrefs(String uId, String username) {
		Log.w("TAG", "new uid to save: "+uId);
		prefs.edit().putString(SETTINGS_MY_UID, uId).apply();
		if(prefs.getString(SETTINGS_DB_TOKEN, null)!=null) {
			Log.w("TAG", "SETTINGS_DB_TOKEN: "+prefs.getString(SETTINGS_DB_TOKEN, null));
			mFirebaseDatabaseReference.child(TABLE_DB_USERS).child(uId).child(FIELD_DB_TOKEN).setValue(prefs.getString(SETTINGS_DB_TOKEN, null));
		}else Log.e("SignIn", "TOKEN NOT FOUND");
		Toast.makeText(this, "Welcome " + username, Toast.LENGTH_SHORT).show();
		mProgressBar.setVisibility(View.GONE);
		mSignInButton.setVisibility(View.VISIBLE);
		startActivity(new Intent(SignInActivity.this, ShareActivity.class));
		finish();
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
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(this, "Google Play Services error.", Toast.LENGTH_SHORT).show();
		mProgressBar.setVisibility(View.GONE);
		mSignInButton.setVisibility(View.VISIBLE);
    }

}
