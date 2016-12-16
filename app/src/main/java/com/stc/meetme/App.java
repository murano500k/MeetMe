package com.stc.meetme;

import android.app.Application;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.airbnb.deeplinkdispatch.DeepLinkHandler;

import timber.log.BuildConfig;
import timber.log.Timber;

/**
 * Created by artem on 11/10/16.
 */

public class App extends Application {

	@Override
	public void onTerminate() {
		super.onTerminate();
	}

	@Override
	public void onCreate() {
		super.onCreate();
		if (BuildConfig.DEBUG) {

		}
		Timber.plant(new Timber.DebugTree(){
			@Override
			protected String createStackElementTag(StackTraceElement element) {
				return super.createStackElementTag(element)
						+'.'+element.getMethodName()
						+':'+element.getLineNumber();
			}
		});
		IntentFilter intentFilter = new IntentFilter(DeepLinkHandler.ACTION);
		LocalBroadcastManager.getInstance(this).registerReceiver(new DeepLinkReceiver(), intentFilter);
	}

}
