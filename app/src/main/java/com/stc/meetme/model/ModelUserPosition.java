package com.stc.meetme.model;

import android.text.format.DateFormat;

import java.io.Serializable;

/**
 * Created by artem on 11/10/16.
 */

public class ModelUserPosition implements Serializable {
	private String placeName;
	private String placeAddress;
	private double mLng;
	private double mLat;
	private long timestamp;
	private float accuracy;

	public float getAccuracy() {
		return accuracy;
	}

	public void setAccuracy(float accuracy) {
		this.accuracy = accuracy;
	}

	public double getLng() {
		return mLng;
	}

	public void setLng(double mLng) {
		this.mLng = mLng;
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

	public ModelUserPosition() {

	}

	public String formatDateTime(){
		if(timestamp<=0) return "ERROR";
		String date = (DateFormat.format("dd/MM/yyyy HH:mm:ss", timestamp).toString());

		//String time = DateUtils.formatDateTime(context, timestamp, DateUtils.FORMAT_SHOW_TIME);
		//String date =  DateUtils.formatDateTime(context, timestamp, DateUtils.FORMAT_SHOW_DATE);
		return date;
	}

	@Override
	public String toString() {
		return "UserPosition{" +
				"accuracy=" + accuracy +
				", placeName='" + placeName + '\'' +
				", placeAddress='" + placeAddress + '\'' +
				", mLng=" + mLng +
				", mLat=" + mLat +
				", timestamp=" + timestamp +
				'}';
	}
}
