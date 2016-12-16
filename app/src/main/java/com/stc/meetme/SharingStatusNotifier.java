package com.stc.meetme;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.service.notification.StatusBarNotification;

import static com.stc.meetme.Constants.INTENT_ACTION_STOP_MY_UPDATES;
import static com.stc.meetme.Constants.NOTIFICATION_ID_STATUS;

/**
 * Created by artem on 12/16/16.
 */

public class SharingStatusNotifier {
	private static final String TAG = "SharingStatusNotifier";
	public interface UIUpdateInterface {
		public void updateUi(boolean status);
	}
	NotificationManager  notificationManager;
	Context mContext;
	Notification notification;
	UIUpdateInterface uiUpdateInterface;
	PendingIntent cancelIntent, openIntent;

	public SharingStatusNotifier(Context mContext) {
		this.mContext = mContext;
		notificationManager=(NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);

		cancelIntent =PendingIntent.getService(
				mContext, 0, new Intent(mContext, MeetMeService.class)
						.setAction(INTENT_ACTION_STOP_MY_UPDATES)
				,PendingIntent.FLAG_UPDATE_CURRENT);

		openIntent =PendingIntent.getActivity(
				mContext, 0, new Intent(mContext, Share2Activity.class)
				,PendingIntent.FLAG_UPDATE_CURRENT);
	}
	public boolean isShown(){
		for(StatusBarNotification n:notificationManager.getActiveNotifications()){
			if(n.getId()==NOTIFICATION_ID_STATUS) return true;
		}
		return false;
	}
	public void startNotification(){
		android.app.Notification.Builder builder = new android.app.Notification.Builder(mContext);
		builder.addAction(-1,"Cancel",cancelIntent);
		builder.setContentText("Updates are active");
		builder.setSmallIcon(android.support.design.R.drawable.abc_ic_menu_share_mtrl_alpha);
		builder.setDeleteIntent(cancelIntent);
		builder.setContentIntent(openIntent);
		notification= builder.build();
		notificationManager.notify(NOTIFICATION_ID_STATUS, notification);
	}
	public void stopNotification(){
		 notificationManager.cancel(NOTIFICATION_ID_STATUS);
	}



}
