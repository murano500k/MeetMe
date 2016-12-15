package com.stc.meetme.ui;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by artem on 12/15/16.
 */
public interface ObserveInfoProvider {
	ChildEventListener getChildPositionListener();
	ValueEventListener getActivityListener();
	String  getObservableUid();
	String  getCurrentUid();
}
