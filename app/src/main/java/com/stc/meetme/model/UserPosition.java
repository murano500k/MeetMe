package com.stc.meetme.model;

import android.content.Context;
import android.text.format.DateFormat;

import java.sql.Date;

/**
 * Created by artem on 11/10/16.
 */

public class UserPosition {
	private String placeName;
	private String placeAddress;
	private double mLong;
	private double mLat;
	private long timestamp;
	private float accuracy;

	public float getAccuracy() {
		return accuracy;
	}

	public void setAccuracy(float accuracy) {
		this.accuracy = accuracy;
	}

	public double getLong() {
		return mLong;
	}

	public void setLong(double mLong) {
		this.mLong = mLong;
	}

	public double getLat() {
		return mLat;
	}

	public void setLat(double mLat) {
		this.mLat = mLat;
	}

	public String getPlaceAddress() {
		return placeAddress;
	}

	public void setPlaceAddress(String placeAddress) {
		this.placeAddress = placeAddress;
	}

	public String getPlaceName() {
		return placeName;
	}

	public void setPlaceName(String placeName) {
		this.placeName = placeName;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public UserPosition() {

	}
	public String getFormattedDateTime(Context context){
		if(timestamp<=0) return null;
		Date date = new Date(timestamp);
		return DateFormat.getDateFormat(context).format(date);
	}

}
