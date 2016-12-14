package com.stc.meetme.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.stc.meetme.R;
import com.stc.meetme.model.Subscription;

import static com.stc.meetme.Constants.FIELD_DB_SUBSCRIPTION_TARGET_ID;
import static com.stc.meetme.Constants.LIST_TYPE_MY_OBSERVERS;
import static com.stc.meetme.Constants.SETTINGS_MY_UID;
import static com.stc.meetme.Constants.SETTINGS_STATUS_ACTIVE;
import static com.stc.meetme.Constants.TABLE_DB_SUBSCRIPTIONS;

public abstract class BaseListActivity extends AppCompatActivity implements OnListInteractionListener, View.OnClickListener {
	protected String TAG;
	protected DatabaseReference mFirebaseDatabaseReference;
	protected RecyclerView recyclerView;
	protected BaseAdapter mAdapter;
	protected String currentUserId;
	protected ChildEventListener userListListener;
	protected SharedPreferences prefs;
	protected int listType;
	protected Query query;
	protected int fabIcon;
	protected View mLayout;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_base_list);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
	}



	public boolean isSharingMyself() {
		return prefs.getBoolean(SETTINGS_STATUS_ACTIVE, false);
	}


	protected void showToast(String text) {
		Log.w("OBSERVE", text);
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}

	public void init(Bundle savedInstanceState){
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		mLayout=(LinearLayout)findViewById(R.id.activity_share);
		currentUserId=prefs.getString(SETTINGS_MY_UID, null);
		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		fab.setOnClickListener(this);
		fab.setImageResource(fabIcon);
		recyclerView = (RecyclerView) findViewById(R.id.list);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));
		mAdapter = new BaseAdapter(listType, this,this);
		recyclerView.setAdapter(mAdapter);
		mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
		DatabaseReference subscriptionsReference=mFirebaseDatabaseReference.child(TABLE_DB_SUBSCRIPTIONS);
		if(listType==LIST_TYPE_MY_OBSERVERS) query=subscriptionsReference.orderByChild(FIELD_DB_SUBSCRIPTION_TARGET_ID);
		else query=subscriptionsReference.orderByChild(FIELD_DB_SUBSCRIPTION_TARGET_ID);
		userListListener=getChildListener();
		query.equalTo(currentUserId).addChildEventListener(userListListener);

	}




	 public ChildEventListener getChildListener(){
		 return new ChildEventListener() {
			 @Override
			 public void onChildAdded(DataSnapshot dataSnapshot, String s) {
				 Subscription subscription = dataSnapshot.getValue(Subscription.class);
				 if(subscription==null) Log.e("TAG", "NULL subscription");
				 Log.w("TAG", "subscription: "+subscription);
				 mAdapter.addItem(subscription);
			 }
			 @Override
			 public void onChildChanged(DataSnapshot dataSnapshot, String s) {
				 Subscription subscription = dataSnapshot.getValue(Subscription.class);
				 Log.w("onChildChanged", "subscription: "+subscription);
				 mAdapter.updateItem(subscription);
			 }
			 @Override
			 public void onChildRemoved(DataSnapshot dataSnapshot) {
				 Subscription subscription = dataSnapshot.getValue(Subscription.class);
				 Log.w("onChildRemoved", "subscription: "+subscription);
				 mAdapter.removeItem(subscription);
			 }

			 @Override
			 public void onChildMoved(DataSnapshot dataSnapshot, String s) {
				 Log.w("onChildMoved", "");

			 }

			 @Override
			 public void onCancelled(DatabaseError databaseError) {

				 Log.e("onCancelled", "databaseError: "+databaseError);

				 Toast.makeText(getApplicationContext(), "CANCELLED", Toast.LENGTH_SHORT).show();
			 }
		 };
	 }

}
