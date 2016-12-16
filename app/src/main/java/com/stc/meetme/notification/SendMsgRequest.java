package com.stc.meetme.notification;

import com.google.gson.annotations.SerializedName;
import com.stc.meetme.model.Data;
import com.stc.meetme.model.FcmNotification;

/**
 * Created by artem on 10/26/16.
 */

public class SendMsgRequest {
	@SerializedName("to")
	String to;

	@SerializedName("data")
	Data data;
	@SerializedName("notification")
	FcmNotification fcmNotification;

	public SendMsgRequest(String to, Data data, FcmNotification fcmNotification) {
		this.data = data;
		this.fcmNotification = fcmNotification;
		this.to = to;
	}



}
