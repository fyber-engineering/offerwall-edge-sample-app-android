package com.sponsorpay.sdk.android.advertiser;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

/**
 * Persists and retrieves the state of the Advertiser part of the SDK.
 */
public class SponsorPayAdvertiserState {
	/**
	 * Shared preferences file name. We store a flag into the shared preferences which is checked on
	 * each consecutive invocation of {@link #register()}, to keep track of whether we have already
	 * successfully contacted the Advertiser API.
	 */
	private static final String PREFERENCES_FILE_NAME = "SponsorPayAdvertiserState";

	/**
	 * The key to store in the preferences file the flag which determines if we have already
	 * successfully contacted the Advertiser API.
	 */
	private static final String STATE_GOT_SUCCESSFUL_RESPONSE_KEY = "SponsorPayAdvertiserState"; // TODO

	/**
	 * The key to store the install subID in the preferences file
	 */
	private static final String STATE_INSTALL_SUBID_KEY = "InstallSubId";

	/**
	 * The key to store the install referrer in the preferences file
	 */
	private static final String STATE_INSTALL_REFERRER_KEY = "InstallReferrer";

	/**
	 * The shared preferences encoded in the {@link #PREFERENCES_FILE_NAME} file.
	 */
	private SharedPreferences mPrefs;

	/**
	 * Constructor.
	 * 
	 * @param context
	 *            Android application context, used to gain access to the preferences file.
	 */
	public SponsorPayAdvertiserState(Context context) {
		mPrefs = context.getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
	}

	/**
	 * Persists the flag which determines if we have already successfully contacted the Advertiser
	 * API.
	 */
	public void setHasAdvertiserCallbackReceivedSuccessfulResponse(boolean value) {
		Editor prefsEditor = mPrefs.edit();
		prefsEditor.putBoolean(STATE_GOT_SUCCESSFUL_RESPONSE_KEY, value);
		prefsEditor.commit();
	}

	/**
	 * Retrieves the flag which determines if we have already successfully contacted the Advertiser
	 * API.
	 */
	public boolean getHasAdvertiserCallbackReceivedSuccessfulResponse() {
		return mPrefs.getBoolean(STATE_GOT_SUCCESSFUL_RESPONSE_KEY, false);
	}

	/**
	 * Persists the value of the install subID.
	 */
	public void setInstallSubId(String subIdValue) {
		Editor prefsEditor = mPrefs.edit();
		prefsEditor.putString(STATE_INSTALL_SUBID_KEY, subIdValue);
		prefsEditor.commit();
	}

	/**
	 * Retrieves the value of the install subID.
	 */
	public String getInstallSubId() {
		return mPrefs.getString(STATE_INSTALL_SUBID_KEY, "");
	}

	/**
	 * Persists the value of the whole install referrer.
	 */
	public void setInstallReferrer(String value) {
		Editor prefsEditor = mPrefs.edit();
		prefsEditor.putString(STATE_INSTALL_REFERRER_KEY, value);
		prefsEditor.commit();
	}

	/**
	 * Retrieves the values of the install referrer, typically set when the host app was installed.
	 * 
	 * @return
	 */
	public String getInstallReferrer() {
		return mPrefs.getString(STATE_INSTALL_REFERRER_KEY, "");
	}
}
