/**
 * SponsorPay Android SDK
 *
 * Copyright 2012 SponsorPay. All rights reserved.
 */

package com.sponsorpay.sdk.android.credentials;

import java.util.UUID;

import android.content.Context;

import com.sponsorpay.sdk.android.HostInfo;
import com.sponsorpay.sdk.android.publisher.UserId;
import com.sponsorpay.sdk.android.utils.StringUtils;

/**
 * <p>
 * Object that holds the information about the current user, application and device.
 * </p>
 * 
 * <p>
 * The application and user id are immutable. You'll need to create a new credentials object 
 * to change any of those.
 * </p>
 *
 */
public class SPCredentials{

	private final String mCredentialsToken;
	
	private final String mAppId;
	private final String mUserId;
	private String mSecurityToken;
	
	private final HostInfo mHostInfo;

	public SPCredentials(String appId, String userId, String securityToken, Context context) {
		mSecurityToken = StringUtils.trim(securityToken);
		mHostInfo = new HostInfo(context);
		// to be removed when we no longer support appid in manifest
		if (StringUtils.notNullNorEmpty(appId)) {
			mAppId = StringUtils.trim(appId);
			mHostInfo.setOverriddenAppId(mAppId);
	 	} else {
	 		mAppId = mHostInfo.getAppId();
	 	}
		mCredentialsToken = getCredentialsToken(mAppId, userId);
		if (StringUtils.nullOrEmpty(userId)) {
			mUserId = UserId.make(context, userId).toString();
		} else {
			mUserId = userId;
		}
	}
	
	/**
	 * Returns the credentials token ID.
	 * 
	 * @return
	 */
	public String getCredentialsToken() {
		return mCredentialsToken;
	}

	/**
	 * Returns the application ID
	 * 
	 * @return
	 */
	public String getAppId() {
		return mAppId;
	}

	/**
	 * Returns the user ID (can be a generated one, if none
	 * was provided at construction time)
	 * 
	 * @return
	 */
	public String getUserId() {
		return mUserId;
	}
	
	/**
	 * Returns the security token
	 * 
	 * @return
	 */
	public String getSecurityToken() {
		return mSecurityToken;
	}
	
	/**
	 * Sets a new security token for this credentials object
	 * 
	 * @param securityToken
	 * 			the new security token to be used in this credentials
	 */
	public void setSecurityToken(String securityToken) {
		mSecurityToken = securityToken;
	}
	
	/**
	 * Returns the {@link HostInfo} object containing information about the device.
	 * 
	 * @return
	 */
	public HostInfo getHostInfo() {
		return mHostInfo;
	}
	
	/**
	 * Convenience method to get a credentials token id for the appId-userId
	 * pair. Throws an {@link IllegalArgumentException} if AppId is null.
	 * 
	 * @param appId
	 * 			the application id
	 * @param userId
	 * 			the user id
	 * 
	 * @return the credentials token
	 */
	public static String getCredentialsToken(String appId, String userId) {
		if (StringUtils.nullOrEmpty(appId)) {
			throw new IllegalArgumentException("AppID cannot be null!");
		}
		if (StringUtils.nullOrEmpty(userId)) {
			userId = StringUtils.EMPTY_STRING;
		}
		String token = appId + "-" + userId;
		return UUID.nameUUIDFromBytes(token.getBytes()).toString();
	}
	
	@Override
	public String toString() {
		return String
				.format("Credentials token - %s\nAppId - %s\nUserId - %s\nSecurityToken - %s",
						mCredentialsToken,
						mAppId,
						StringUtils.notNullNorEmpty(mUserId) ? mUserId : "N/A",
						StringUtils.notNullNorEmpty(mSecurityToken) ? mSecurityToken : "N/A");
	}
	
}
