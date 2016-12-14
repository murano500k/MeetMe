package com.stc.meetme.model;

import android.text.format.DateFormat;

/**
 * Created by artem on 11/10/16.
 */

public class UserActivity {

	private long timestamp;
	private String activity0;
	private String activity1;
	private String activity2;


	public UserActivity() {
	}

	public String getActivity0() {
		return activity0;
	}

	public void setActivity0(String activity0) {
		this.activity0 = activity0;
	}

	public String getActivity1() {
		return activity1;
	}

	public void setActivity1(String activity1) {
		this.activity1 = activity1;
	}

	public String getActivity2() {
		return activity2;
	}

	public void setActivity2(String activity2) {
		this.activity2 = activity2;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return "UserActivity{" +
				"activity0='" + activity0 + '\'' +
				", timestamp=" + timestamp +
				", activity1='" + activity1 + '\'' +
				", activity2='" + activity2 + '\'' +
				'}';
	}
	public String formatDateTime(){
		if(timestamp<=0) return "ERROR";
		String date = (DateFormat.format("dd/MM/yyyy HH:mm:ss", timestamp).toString());

		//String time = DateUtils.formatDateTime(context, timestamp, DateUtils.FORMAT_SHOW_TIME);
		//String date =  DateUtils.formatDateTime(context, timestamp, DateUtils.FORMAT_SHOW_DATE);
		return date;
	}
}
