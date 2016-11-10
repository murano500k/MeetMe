package com.stc.meetme.model;

/**
 * Created by artem on 11/10/16.
 */

public class UserStatus {

	private UserPosition userPosition;
	private UserActivity userActivity;

	public UserStatus() {
	}

	public UserPosition getUserPosition() {
		return userPosition;
	}

	public void setUserPosition(UserPosition userPosition) {
		this.userPosition = userPosition;
	}

	public UserActivity getUserActivity() {

		return userActivity;
	}

	public void setUserActivity(UserActivity userActivity) {
		this.userActivity = userActivity;
	}
}