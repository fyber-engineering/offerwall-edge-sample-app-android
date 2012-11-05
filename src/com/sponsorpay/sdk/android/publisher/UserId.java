/**
 * SponsorPay Android SDK
 *
 * Copyright 2012 SponsorPay. All rights reserved.
 */

package com.sponsorpay.sdk.android.publisher;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.sponsorpay.sdk.android.HostInfo;
import com.sponsorpay.sdk.android.SignatureTools;
import com.sponsorpay.sdk.android.utils.StringUtils;

public class UserId {
	public static final String STATE_GENERATED_USERID_KEY = "STATE_GENERATED_USERID_KEY";
	
	private String mUserIdValue;

	public static synchronized String getAutoGenerated(Context context) {
		SharedPreferences state = context.getSharedPreferences(
				SponsorPayPublisher.PREFERENCES_FILENAME, Context.MODE_PRIVATE);

		String userIdValue = state.getString(STATE_GENERATED_USERID_KEY, null);

		if (userIdValue == null) {
			HostInfo hostInfo = new HostInfo(context);
			UserIdGenerator userIdGenerator = new UserIdGenerator();

			userIdGenerator.setTelephonyDeviceId(hostInfo.getUDID());
			userIdGenerator.setAndroidId(hostInfo.getAndroidId());
			userIdGenerator.setHardwareSerialNumber(hostInfo.getHardwareSerialNumber());

			userIdValue = userIdGenerator.generateUserId();

			Editor stateEditor = state.edit();
			stateEditor.putString(STATE_GENERATED_USERID_KEY, userIdValue);
			stateEditor.commit();
		}
				
		return userIdValue;
	}

	public static UserId make(Context context, String userIdValue) {
		UserId instance;
		if (StringUtils.notNullNorEmpty(userIdValue)) {
			instance = new UserId(userIdValue);
		} else {
			instance = new UserId(context);
		}
		
		return instance;
	}
	
	public UserId(Context context) {
		mUserIdValue = getAutoGenerated(context);
	}
	
	public UserId(String value) {
		mUserIdValue = value;
	}
	
	@Override
	public String toString() {
		return mUserIdValue;
	}

	public static class UserIdGenerator {
		private String mTelephonyDeviceId;
		private String mAndroidId;
		private String mHardwareSerialNumber;

		private Set<String> mKnownInvalidAndroidIds;

		public boolean setTelephonyDeviceId(String deviceId) {
			if (isValidId(deviceId)) {
				mTelephonyDeviceId = deviceId;
				return true;
			}
			mTelephonyDeviceId = null;
			return false;
		}

		public boolean setAndroidId(String androidId) {
			if (isValidAndroidId(androidId)) {
				mAndroidId = androidId;
				return true;
			}
			mAndroidId = null;
			return false;
		}

		public boolean setHardwareSerialNumber(String value) {
			if (isValidId(value)) {
				mHardwareSerialNumber = value;
				return true;
			}
			mHardwareSerialNumber = null;
			return false;
		}

		public boolean isValidAndroidId(String androidId) {
			// Check for general ID validness
			if (!isValidId(androidId))
				return false;

			// Check for equality with any of the known invalid android IDs
			if (mKnownInvalidAndroidIds == null) {
				mKnownInvalidAndroidIds = new HashSet<String>();
				mKnownInvalidAndroidIds.add("9774d56d682e549c");
			}
			if (mKnownInvalidAndroidIds.contains(androidId))
				return false;

			return true;
		}

		public static boolean isValidId(String id) {
			// Check for null string
			// Check for empty or whitespace only string
			if (StringUtils.nullOrEmpty(id)) {
				return false;
			}

			// Check for the integer number 0
			Integer androidIdAsInteger = null;
			try {
				androidIdAsInteger = Integer.parseInt(id);
			} catch (NumberFormatException e) {
				// The string didn't have a valid integer format. Test for zero doesn't apply.
			}
			if (androidIdAsInteger != null && androidIdAsInteger.intValue() == 0) {
				return false;
			}

			return true;
		}

		public String generateUserId() {
			StringBuilder builder = new StringBuilder();

			if (mTelephonyDeviceId != null || mAndroidId != null || mHardwareSerialNumber != null) {
				if (mTelephonyDeviceId != null) {
					builder.append(mTelephonyDeviceId);
				}
				if (mAndroidId != null) {
					builder.append(mAndroidId);
				}
				if (mHardwareSerialNumber != null) {
					builder.append(mHardwareSerialNumber);
				}
			} else {
				builder.append(UUID.randomUUID());
			}

			String baseText = builder.toString();
			String generatedId = SignatureTools.generateSHA1ForString(baseText);

			if (generatedId == null || generatedId.equals(SignatureTools.NO_SHA1_RESULT)) {
				generatedId = baseText;
			}

			return generatedId;
		}
	}
}
