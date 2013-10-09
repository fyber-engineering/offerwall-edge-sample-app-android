/**
 * SponsorPay Android SDK
 *
 * Copyright 2011 - 2013 SponsorPay. All rights reserved.
 */

package com.sponsorpay.sdk.mbe.mediation;

import android.app.Activity;
import android.content.Context;

import com.sponsorpay.sdk.android.publisher.mbe.mediation.SPMediationAdaptor;
import com.sponsorpay.sdk.android.publisher.mbe.mediation.SPMediationConfigurator;
import com.sponsorpay.sdk.android.publisher.mbe.mediation.SPTPNValidationResult;
import com.sponsorpay.sdk.android.publisher.mbe.mediation.SPTPNVideoEvent;
import com.sponsorpay.sdk.android.utils.SponsorPayLogger;
import com.sponsorpay.sdk.android.utils.StringUtils;
import com.vungle.sdk.VunglePub;
import com.vungle.sdk.VunglePub.EventListener;

public class VungleMediationAdaptor extends SPMediationAdaptor implements EventListener {
			
	private static final String TAG = "VungleAdaptor";

	private static final String ADAPTOR_VERSION = "1.0.0";
	public static final String ADAPTOR_NAME = "MockMediatedNetwork";
//	private static final String ADAPTOR_NAME = "vungle";
	
	private static final String APP_ID = "app.id";
	private static final String SHOW_CLOSE_BUTTON = "show.close.button";
	private static final String SOUND_ENABLED = "sound.enabled";
	private static final String AUTO_ROTATION_ENABLED = "auto.rotation.enabled";
	private static final String BACK_BUTTON_ENABLED = "back.button.enabled";
	

	@Override
	public boolean startAdaptor(Activity activity) {
		SponsorPayLogger.d(TAG, "Starting Vungle adaptor - SDK version " + VunglePub.getVersionString());
		String appId = SPMediationConfigurator.getConfiguration(ADAPTOR_NAME, APP_ID, String.class);
		if (StringUtils.notNullNorEmpty(appId)) {
			SponsorPayLogger.i(TAG, "Using App ID = " + appId);
			VunglePub.setSoundEnabled(SPMediationConfigurator.getConfiguration(
					ADAPTOR_NAME, SOUND_ENABLED, Boolean.TRUE, Boolean.class));
			VunglePub.setAutoRotation(SPMediationConfigurator.getConfiguration(
					ADAPTOR_NAME, AUTO_ROTATION_ENABLED, Boolean.FALSE,	Boolean.class));
			VunglePub.setBackButtonEnabled(SPMediationConfigurator.getConfiguration(
					ADAPTOR_NAME, BACK_BUTTON_ENABLED, Boolean.FALSE, Boolean.class));
			VunglePub.init(activity, appId);
			VunglePub.setEventListener(this);
			return true;
		}
		SponsorPayLogger.d(TAG, "App Id  must have a valid value!");
		return false;
	}

	@Override
	public String getName() {
		return ADAPTOR_NAME;
	}

	@Override
	public String getVersion() {
		return ADAPTOR_VERSION;
	}

	@Override
	public void videosAvailable(Context context) {
		sendValidationEvent(VunglePub.isVideoAvailable() ? SPTPNValidationResult.SPTPNValidationSuccess
				: SPTPNValidationResult.SPTPNValidationNoVideoAvailable);
	}

	@Override
	public void startVideo(final Activity parentActivity) {
		if (VunglePub.isVideoAvailable()) {
			Boolean showClose = SPMediationConfigurator.getConfiguration(
					ADAPTOR_NAME, SHOW_CLOSE_BUTTON, Boolean.FALSE,	Boolean.class);
			VunglePub.displayIncentivizedAdvert(showClose);
		} else {
			sendVideoEvent(SPTPNVideoEvent.SPTPNVideoEventNoVideo);
			clearVideoEvent();
		}
	}

	
	// Vungle EventListener interface 
	@Override
	public void onVungleAdEnd() {
//		notifyCloseEngagement();
	}

	@Override
	public void onVungleAdStart() {
		notifyVideoStarted();
	}

	@Override
	public void onVungleView(double watchedSeconds, double totalAdSeconds) {
        final double watchedPercent = watchedSeconds / totalAdSeconds;
        if (watchedPercent >= 0.8) {
        	setVideoPlayed();
        }
		notifyCloseEngagement();
	}
	
}
