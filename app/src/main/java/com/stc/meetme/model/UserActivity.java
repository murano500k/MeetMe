package com.stc.meetme.model;

import android.content.Context;
import android.text.format.DateFormat;

import java.sql.Date;

/**
 * Created by artem on 11/10/16.
 */

public class UserActivity {

	private long timestamp;
	private String activity;
	private int confidence;

	public String getFormattedDateTime(Context context){
		if(timestamp<=0) return null;
		Date date = new Date(timestamp);
		return DateFormat.getDateFormat(context).format(date);
	}

	public UserActivity() {
	}

	public String getActivity() {
		return activity;
	}

	public void setActivity(String activity) {
		this.activity = activity;
	}

	public int getConfidence() {
		return confidence;
	}

	public void setConfidence(int confidence) {
		this.confidence = confidence;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}
