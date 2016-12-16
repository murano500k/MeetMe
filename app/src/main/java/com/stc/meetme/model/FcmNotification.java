package com.stc.meetme.model;

import com.google.gson.annotations.SerializedName;

/**
 * Created by artem on 10/26/16.
 */

public class FcmNotification {
	@SerializedName("body")
	String body;

	public FcmNotification(String body) {
		this.body = body;
	}
}
