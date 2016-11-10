package com.stc.meetme.notification;

import com.google.gson.annotations.SerializedName;
import com.stc.meetme.model.Data;
import com.stc.meetme.model.Notification;

/**
 * Created by artem on 10/26/16.
 */

public class SendMsgRequest {
	@SerializedName("to")
	String to;

	@SerializedName("data")
	Data data;
	@SerializedName("notification")
	Notification notification;

	public SendMsgRequest(String to, Data data, Notification notification) {
		this.data = data;
		this.notification = notification;
		this.to = to;
	}



}
