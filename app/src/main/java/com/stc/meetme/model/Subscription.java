package com.stc.meetme.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by artem on 11/18/16.
 */

public class Subscription implements Parcelable{
	String targetUserId;
	String observerUserId;
	boolean active;

	public Subscription() {
	}

	public Subscription(boolean active, String observerUserId, String targetUserId) {
		this.active = active;
		this.observerUserId = observerUserId;
		this.targetUserId = targetUserId;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getObserverUserId() {
		return observerUserId;
	}

	public void setObserverUserId(String observerUserId) {
		this.observerUserId = observerUserId;
	}

	public String getTargetUserId() {
		return targetUserId;
	}

	public void setTargetUserId(String targetUserId) {
		this.targetUserId = targetUserId;
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel parcel, int i) {
		parcel.writeString(targetUserId);
		parcel.writeString(observerUserId);
		parcel.writeByte((byte) (active ? 1 : 0));     //if myBoolean == true, byte == 1
	}
	protected Subscription(Parcel in) {
		targetUserId = in.readString();
		observerUserId = in.readString();
		active = in.readByte() != 0;
	}
	public static final Creator<Subscription> CREATOR = new Creator<Subscription>() {
		@Override
		public Subscription createFromParcel(Parcel in) {

			return new Subscription(in);
		}

		@Override
		public Subscription[] newArray(int size) {
			return new Subscription[size];
		}
	};
}
