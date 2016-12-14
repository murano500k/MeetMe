package com.stc.meetme.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.stc.meetme.R;
import com.stc.meetme.model.Subscription;

import static com.stc.meetme.Constants.INTENT_EXTRA_OBSERVE_UID;
import static com.stc.meetme.Constants.LIST_TYPE_MY_TARGETS;

public class MyTargetsListActivity extends BaseListActivity {


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TAG="MyObserversListActivity";
		listType=LIST_TYPE_MY_TARGETS;
		fabIcon=android.R.drawable.ic_menu_add;
		init(savedInstanceState);
	}
	@Override
	public void onClick(View view) {
		showToast("NOT IMPLEMENTED");
	}


	@Override
	public void onListInteraction(final Subscription subscription) {
		if(subscription.isActive()){
			Intent intent = new Intent(MyTargetsListActivity.this, ObserveActivity.class);
			intent.putExtra(INTENT_EXTRA_OBSERVE_UID, subscription.getTargetUserId());
			startActivity(intent);
		}else showToast(getString(R.string.subscription_is_not_active));
	}


}
