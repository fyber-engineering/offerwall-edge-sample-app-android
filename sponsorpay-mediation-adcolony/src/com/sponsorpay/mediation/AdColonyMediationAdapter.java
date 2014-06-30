/**
 * SponsorPay Android SDK
 *
 * Copyright 2011 - 2014 SponsorPay. All rights reserved.
 */

package com.sponsorpay.mediation;

import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.os.Build;

import com.jirbo.adcolony.AdColony;
import com.sponsorpay.mediation.mbe.AdColonyVideoMediationAdapter;
import com.sponsorpay.publisher.interstitial.mediation.SPInterstitialMediationAdapter;
import com.sponsorpay.utils.SponsorPayLogger;
import com.sponsorpay.utils.StringUtils;

public class AdColonyMediationAdapter extends SPMediationAdapter {
			
	private static final String TAG = "AdColonyAdapter";

	private static final String ADAPTER_VERSION = "1.0.0";

	private static final String ADAPTER_NAME = "AdColony";
	
	private static final String APP_ID = "app.id";
	private static final String ZONE_IDS = "zone.ids";
	private static final String CLIENT_OPTIONS = "client.options";
	
	private static final String DEVICE_ID = "device.id";
	private static final String CUSTOM_ID = "custom.id";

	private AdColonyVideoMediationAdapter mVideoMediationAdapter;

	@Override
	public boolean startAdapter(final Activity activity) {
		if (Build.VERSION.SDK_INT >= 10 ){
			SponsorPayLogger.d(TAG, "Starting AdColony adapter" );
			final String appId = SPMediationConfigurator.getConfiguration(ADAPTER_NAME, APP_ID, String.class);
			final String[] zoneIds = getZoneIds();
			final String clientOptions = SPMediationConfigurator.getConfiguration(ADAPTER_NAME, CLIENT_OPTIONS, String.class);
			if (StringUtils.notNullNorEmpty(appId) && zoneIds != null) {
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						setCustomId();
						setDeviceId();
						AdColony.configure(activity, clientOptions, appId, zoneIds);
						mVideoMediationAdapter = new AdColonyVideoMediationAdapter(AdColonyMediationAdapter.this);
					}
				});
				return true;
			}
			SponsorPayLogger.d(TAG, "App Id  must have a valid value!");
		} else {
			SponsorPayLogger.d(TAG, "AdColony requires Android Version 2.3.3\nThe mediation adapter will not be started" );
		}
		return false;
	}

	@Override
	public String getName() {
		return ADAPTER_NAME;
	}

	@Override
	public String getVersion() {
		return ADAPTER_VERSION;
	}

	@Override
	public AdColonyVideoMediationAdapter getVideoMediationAdapter() {
		return mVideoMediationAdapter;
	}

	@Override
	public SPInterstitialMediationAdapter<? extends SPMediationAdapter> getInterstitialMediationAdapter() {
		return null;
	}

	@Override
	protected Set<? extends Object> getListeners() {
		return null;
	}
	
	// Helper methods
	private String[] getZoneIds() {
		JSONArray jsonZoneIds = SPMediationConfigurator.getConfiguration(ADAPTER_NAME, ZONE_IDS, JSONArray.class);
		if (jsonZoneIds != null && jsonZoneIds.length() > 0) {
			String[] zoneIds = new String[jsonZoneIds.length()];
			for (int i = 0; i < jsonZoneIds.length(); i++) {
				try {
					zoneIds[i] = jsonZoneIds.getString(i);
				} catch (JSONException exception) {
					SponsorPayLogger.e(TAG, "Error on parsing Zone Id.");
					return null;
				}
			}
			return zoneIds;
		}
		return null;
	}
	
	private void setDeviceId() {
		String deviceId = SPMediationConfigurator.getConfiguration(ADAPTER_NAME, DEVICE_ID, String.class);
		if (StringUtils.notNullNorEmpty(deviceId)) {
			AdColony.setDeviceID(deviceId);
		}
	}
	
	private void setCustomId() {
		String cutsomId = SPMediationConfigurator.getConfiguration(ADAPTER_NAME, CUSTOM_ID, String.class);
		if (StringUtils.notNullNorEmpty(cutsomId)) {
			AdColony.setDeviceID(cutsomId);
		}
	}
	
	
}