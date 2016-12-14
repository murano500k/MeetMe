package com.stc.meetme.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;

import com.stc.meetme.R;
import com.stc.meetme.model.Subscription;

import static com.stc.meetme.Constants.FIELD_DB_SUBSCRIPTION_OBSERVER_ID;
import static com.stc.meetme.Constants.FIELD_DB_SUBSCRIPTION_TARGET_ID;
import static com.stc.meetme.Constants.LIST_TYPE_MY_OBSERVERS;
import static com.stc.meetme.Constants.TABLE_DB_SUBSCRIPTIONS;

public class MyObserversListActivity extends BaseListActivity {


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TAG="MyObserversListActivity";
		listType=LIST_TYPE_MY_OBSERVERS;
		fabIcon=android.R.drawable.ic_menu_share;
		init(savedInstanceState);
	}


	private void sharePosition(String currentUserId) {
		String url = "https://f3x9u.app.goo.gl/observe/"+currentUserId;
		Log.w("SHARE", url + "");
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		shareIntent.putExtra(Intent.EXTRA_TEXT, url);
		startActivity(Intent.createChooser(shareIntent, "Share link using"));
	}
	@Override
	public void onClick(View view) {
		sharePosition(currentUserId);
	}

	@Override
	public void onListInteraction(final Subscription subscription) {
		new AlertDialog.Builder(this)
				.setMessage(getString(R.string.delete_observer_confirm))
				.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						removeObserver(subscription);
					}
				});
	}

	public void addObserver(String observerId){
		if(!isSharingMyself()) showToast(getString(R.string.error_sharing_disabled));
		else {
			Subscription subscription=new Subscription(isSharingMyself(), observerId,currentUserId);
			mFirebaseDatabaseReference.child(TABLE_DB_SUBSCRIPTIONS).setValue(subscription);
			showToast(observerId +" was added to observers");
		}
	}
	public void removeObserver(final Subscription subscription){
		mFirebaseDatabaseReference.child(TABLE_DB_SUBSCRIPTIONS).orderByChild(FIELD_DB_SUBSCRIPTION_TARGET_ID).equalTo(currentUserId)
				.orderByChild(FIELD_DB_SUBSCRIPTION_OBSERVER_ID).equalTo(subscription.getObserverUserId())
				.getRef().removeValue();
		Snackbar.make(mLayout,"Observer "+subscription.getObserverUserId()+" was removed", Snackbar.LENGTH_SHORT)
				.setAction("UNDO", new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						addObserver(subscription.getObserverUserId());
					}
				});
	}
}
