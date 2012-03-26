/**
 * SponsorPay Android Publisher SDK
 *
 * Copyright 2011 SponsorPay. All rights reserved.
 */

package com.sponsorpay.sdk.android;

import java.lang.reflect.Field;
import java.util.Locale;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.provider.Settings.Secure;

/**
 * Extracts device information from the host device in which the SDK runs and SponsorPay App ID
 * contained in the Android Application Manifest of the host app.
 */
public class HostInfo {

	/**
	 * Prefix appended to the OS version to identify the Android platform.
	 */
	private static final String ANDROID_OS_PREFIX = "Android OS ";

	private static boolean sSimulateNoReadPhoneStatePermission = false;
	private static boolean sSimulateNoAccessWifiStatePermission = false;
	private static boolean sSimulateInvalidAndroidId = false;
	private static boolean sSimulateNoHardwareSerialNumber = false;

	public static void setSimulateNoReadPhoneStatePermission(boolean value) {
		sSimulateNoReadPhoneStatePermission = value;
	}

	public static void setSimulateNoAccessWifiStatePermission(boolean value) {
		sSimulateNoAccessWifiStatePermission = value;
	}

	public static void setSimulateInvalidAndroidId(boolean value) {
		sSimulateInvalidAndroidId = value;
	}

	public static void setSimulateNoHardwareSerialNumber(boolean value) {
		sSimulateNoHardwareSerialNumber = value;
	}

	/**
	 * The unique device ID.
	 */
	private String mUDID;

	/**
	 * The running Android OS version (e.g. "2.1" for Android 2.1).
	 */
	private String mOsVersion;

	/**
	 * The device version (e.g. "HTC Nexus One").
	 */
	private String mPhoneVersion;

	/**
	 * Language settings (the default locale).
	 */
	private String mLanguageSetting;

	/**
	 * Android ID as reported by Settings.Secure
	 */
	private String mAndroidId;

	/**
	 * MAC Address of the WiFi Adapter
	 */
	private String mWifiMacAddress;

	/**
	 * Device's hardware serial number, reported by versions of Android >=2.3
	 */
	private String mHardwareSerialNumber;

	/**
	 * The SponsorPay's App ID Key that is used in the AndroidManifest.xml file.
	 */
	private static final String SPONSORPAY_APP_ID_KEY = "SPONSORPAY_APP_ID";

	/**
	 * The App ID value.
	 */
	private String mAppId;

	/**
	 * Android application context, used to retrieve the rest of the properties.
	 */
	private Context mContext;

	/**
	 * Get the unique device ID
	 * 
	 * @return the unique device id
	 */
	public String getUDID() {
		return mUDID;
	}

	/**
	 * Get the running OS version
	 * 
	 * @return the OS version
	 */
	public String getOsVersion() {
		return mOsVersion;
	}

	/**
	 * Get the current phone model
	 * 
	 * @return the phone model
	 */
	public String getPhoneVersion() {
		return mPhoneVersion;
	}

	/**
	 * Fetches the device's hardware serial number, reported by versions of Android >=2.3
	 */
	public String getHardwareSerialNumber() {
		if (mHardwareSerialNumber == null) {
			if (!sSimulateNoHardwareSerialNumber) {
				Field serialField = null;
				try {
					serialField = android.os.Build.class.getField("SERIAL");
					Object serialValue = serialField.get(null);
					if (serialValue != null && serialValue.getClass().equals(String.class)) {
						mHardwareSerialNumber = (String) serialValue;
					}
				} catch (Exception e) {
					// Probably running on an older version of Android which doesn't include this
					// field
					mHardwareSerialNumber = "";
				}
			} else {
				mHardwareSerialNumber = "";
			}
		}
		return mHardwareSerialNumber;
	}

	/**
	 * Get the default locale set by the user
	 * 
	 * @return the default language setting
	 */
	public String getLanguageSetting() {
		return mLanguageSetting;
	}

	/**
	 * Returns the device's Android ID.
	 */
	public String getAndroidId() {
		return mAndroidId;
	}

	/**
	 * Returns the MAC address of the device's WiFi adapter.
	 */
	public String getWifiMacAddress() {
		return mWifiMacAddress;
	}

	/**
	 * Constructor. Requires an Android application context which will be used to retrieve
	 * information from the device and the host application's Android Manifest.
	 * 
	 * @param context
	 *            Android application context
	 */
	public HostInfo(Context context) {
		mContext = context;

		if (!sSimulateNoReadPhoneStatePermission) {
			// Get access to the Telephony Services
			TelephonyManager tManager = (TelephonyManager) context
					.getSystemService(Context.TELEPHONY_SERVICE);
			try {
				mUDID = tManager.getDeviceId();
			} catch (SecurityException e) {
				mUDID = "";
			}
		} else {
			mUDID = "";
		}

		// Get the default locale
		mLanguageSetting = Locale.getDefault().toString();

		// Get the Android version
		mOsVersion = ANDROID_OS_PREFIX + android.os.Build.VERSION.RELEASE;

		// Get the phone model
		mPhoneVersion = android.os.Build.MANUFACTURER + "_" + android.os.Build.MODEL;

		// Android ID
		if (!sSimulateInvalidAndroidId) {
			mAndroidId = Secure.getString(mContext.getContentResolver(), Secure.ANDROID_ID);

			if (mAndroidId == null) {
				mAndroidId = "";
			}
		} else {
			mAndroidId = "";
		}

		if (!sSimulateNoAccessWifiStatePermission) {
			try {
				// MAC address of WiFi adapter
				WifiManager wifiMan = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
				WifiInfo wifiInf = wifiMan.getConnectionInfo();
				mWifiMacAddress = wifiInf.getMacAddress();
			} catch (RuntimeException re) {
				mWifiMacAddress = "";
			}
		} else {
			mWifiMacAddress = "";
		}
	}

	/**
	 * Extracts a numeric or alphanumeric value from the meta-data configured in the application
	 * manifest XML file and returns it as a String.
	 * 
	 * @param key
	 *            key to identify the piece of meta-data to return.
	 * @return the value for the given key, or null on failure.
	 */
	private String getValueFromAppMetadata(String key) {
		Object retrievedValue;

		ApplicationInfo ai = null;
		Bundle appMetadata = null;

		// Extract the meta data from the package manager
		try {
			ai = mContext.getPackageManager().getApplicationInfo(mContext.getPackageName(),
					PackageManager.GET_META_DATA);
		} catch (NameNotFoundException e) {
			return null;
		}

		appMetadata = ai.metaData;

		if (appMetadata == null) {
			return null;
		}

		retrievedValue = appMetadata.get(key);

		return retrievedValue == null ? null : retrievedValue.toString();
	}

	/**
	 * <p>
	 * Extracts the App ID from the host application's Android Manifest XML file.
	 * </p>
	 * 
	 * <p>
	 * If the App Id has already been set (i.e. by calling the {@link #setOverriddenAppId(String)}),
	 * this method will just return the id which has been set without trying to retrieve it from the
	 * manifest.
	 * </p>
	 * 
	 * <p>
	 * If no App ID is present in the manifest and no non-empty App ID has been set by calling the
	 * mentioned method, this method will throw a RuntimeException.
	 * </p>
	 * 
	 * @return The offer id previously set or defined in the manifest, or 0.
	 */
	public String getAppId() {
		if (mAppId == null || mAppId.equals("")) {
			mAppId = getValueFromAppMetadata(SPONSORPAY_APP_ID_KEY);
			if (mAppId == null || mAppId.equals("")) {
				throw new RuntimeException(
						"SponsorPay SDK: no valid App ID has been provided. "
								+ "Please set a valid App ID in your application manifest or provide one at runtime. "
								+ "See the integration guide or the SDK javadoc for more information.");
			}
		}
		return mAppId;
	}

	/**
	 * Set the offerId, overriding the one which would be read from the manifest.
	 * 
	 * @param offerId
	 */
	public void setOverriddenAppId(String appId) {
		mAppId = appId;
	}
}
