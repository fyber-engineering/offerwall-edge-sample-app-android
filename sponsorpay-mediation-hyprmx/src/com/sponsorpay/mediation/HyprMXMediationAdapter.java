package com.sponsorpay.mediation;

import java.util.Set;

import android.app.Activity;

import com.hyprmx.android.sdk.HyprMXHelper;
import com.sponsorpay.mediation.mbe.HyprMXVideoMediationAdapter;
import com.sponsorpay.publisher.interstitial.mediation.SPInterstitialMediationAdapter;
import com.sponsorpay.publisher.mbe.mediation.SPBrandEngageMediationAdapter;
import com.sponsorpay.utils.SponsorPayLogger;

public class HyprMXMediationAdapter extends SPMediationAdapter {

	private static final String DISTRIBUTOR_ID = "distributorId";
	private static final String PROPERTY_ID = "propertyId";
	private static final String USER_ID = "userId";

	public static final String TAG = HyprMXMediationAdapter.class.getSimpleName();
	public static final String ADAPTER_VERSION = "1.0.0"; // to verify what
															// version of
															// adapter is
															// supported
	public static final String ADAPTER_NAME = "HyprMX"; // user for getting
															// info from
															// adapters.config

	private HyprMXVideoMediationAdapter mVideoAdapter; // reference to video
														// adapter

	@Override
	public boolean startAdapter(final Activity activity) {
		// create video adapter instance
		SponsorPayLogger.d(TAG, "starting Applifier - HyprMX adapter");
		mVideoAdapter = new HyprMXVideoMediationAdapter(this);
		SponsorPayLogger.d(TAG, "adapter details - distributorID: " + getDistributorId() + ", propertyID: " + getPropertyId() + ", userID: " + getUserId());
		activity.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				HyprMXHelper.getInstance(activity, getDistributorId(), getPropertyId(), getUserId());
			}
		});
		return true; // indicates that adapter has been run successfully
	}

	@Override
	public String getName() {
		SponsorPayLogger.d(TAG, "get name");
		return ADAPTER_NAME;
	}

	@Override
	public String getVersion() {
		SponsorPayLogger.d(TAG, "get version");
		return ADAPTER_VERSION;
	}

	@Override
	public SPBrandEngageMediationAdapter<? extends SPMediationAdapter> getVideoMediationAdapter() {
		SponsorPayLogger.d(TAG, "get video adapter");
		return mVideoAdapter;
	}

	@Override
	public SPInterstitialMediationAdapter<? extends SPMediationAdapter> getInterstitialMediationAdapter() {
		// no interstitial adapter - return null
		SponsorPayLogger.d(TAG, "get interstitial adapter");
		return null;
	}

	@Override
	protected Set<? extends Object> getListeners() {
		SponsorPayLogger.d(TAG, "get listeners");
		return null;
	}

	public String getDistributorId() {
		return SPMediationConfigurator.getConfiguration(ADAPTER_NAME, DISTRIBUTOR_ID, String.class);
	}

	public String getUserId() {
		return SPMediationConfigurator.getConfiguration(ADAPTER_NAME, USER_ID, String.class);
	}

	public String getPropertyId() {
		return SPMediationConfigurator.getConfiguration(ADAPTER_NAME, PROPERTY_ID, String.class);
	}

}
