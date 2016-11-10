package com.stc.meetme.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.stc.meetme.Constants;
import com.stc.meetme.R;
import com.stc.meetme.model.UserActivity;
import com.stc.meetme.model.UserPosition;
import com.stc.meetme.model.UserStatus;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static com.stc.meetme.Constants.TABLE_DB_USER_STATUSES;

public class ObserveActivity extends AppCompatActivity {
/*

	@BindView(R.id.progress)
	ProgressBar mProgressBar;

	@BindView(R.id.content_observe)
	LinearLayout mLayout;
*/

	@BindView(R.id.fab)
	FloatingActionButton fab;

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




	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_observe);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		ButterKnife.bind(this);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setHomeButtonEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle("Observing realtime status");
		}

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
						.setAction("Action", null).show();
			}
		});

		checkIntent();
	}
	private void checkIntent(){
		Intent intent=getIntent();
		if(intent==null) {
			showToast("NO OBSERVABLE USERID");
			finish();
			return;
		}
			observableUserId=intent.getStringExtra(Constants.INTENT_EXTRA_OBSERVE_UID);
		if(observableUserId==null) {
			showToast("NO OBSERVABLE USERID");
			finish();
			return;
		}
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
		if(observableUserId!=null)registerDbUpdates(observableUserId);
		else showToast("observableUserId==null");
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				this.finish();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void registerDbUpdates(String observableUserId){
		Timber.w("registerDbUpdates uid: %s", observableUserId);
		if(statusUpdatesListener==null) {
			statusUpdatesListener = new ValueEventListener() {
				@Override
				public void onDataChange(DataSnapshot dataSnapshot) {
					UserStatus userStatus = null;
					if (dataSnapshot != null)
						userStatus = dataSnapshot.getValue(UserStatus.class);
					if (userStatus == null) {
						showToast("ERROR userStatus NULL");
						return;
					}
					UserActivity userActivity = userStatus.getUserActivity();
					if (userActivity != null) {
						if (userActivity.getActivity() != null)
							activityString = "current activity: " + userActivity.getActivity();
						else activityString = "current activity: ";
						if (userActivity.getConfidence() > 0)
							confidenceString = userActivity.getConfidence() + "%";
						else confidenceString = "NULL";
						timeString = userActivity.getFormattedDateTime(getApplicationContext());
						if (timeString == null) timeString = "NULL";
					}
					textActivity.setText(activityString);
					textConfidence.setText(confidenceString);
					textTime.setText(timeString);

					UserPosition userPosition = userStatus.getUserPosition();
					positionString = "";
					if (userPosition != null) {
						positionString += userPosition.getFormattedDateTime(getApplicationContext()) + "\n";
						positionString += "current place: " + userPosition.getPlaceName() + "\n";
						positionString += "address: " + userPosition.getPlaceAddress() + "\n";
						positionString += "accuracy: " + userPosition.getAccuracy() + "m\n";
						positionString += "lat: " + userPosition.getLat() + "\n";
						positionString += "long: " + userPosition.getLong();
					}
					textPosition.setText(positionString);
				}

				@Override
				public void onCancelled(DatabaseError databaseError) {
					textActivity.setText("cancelled");
					textConfidence.setText("cancelled");
					textTime.setText("cancelled");
					textPosition.setText("cancelled");
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
}
