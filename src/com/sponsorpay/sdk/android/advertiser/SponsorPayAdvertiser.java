/**
 * SponsorPay Android SDK
 *
 * Copyright 2011 - 2013 SponsorPay. All rights reserved.
 */

package com.sponsorpay.sdk.android.advertiser;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.sponsorpay.sdk.android.SponsorPay;
import com.sponsorpay.sdk.android.credentials.SPCredentials;
import com.sponsorpay.sdk.android.utils.SPIdException;
import com.sponsorpay.sdk.android.utils.SPIdValidator;
import com.sponsorpay.sdk.android.utils.SponsorPayParametersProvider;

/**
 * <p>
 * Provides convenience calls to run the Advertiser callback request. Manages the state of the SDK
 * determining whether a successful response to the callback request has been already received since
 * the application was installed in the host device.
 * </p>
 * 
 * <p>
 * It's implemented as a singleton, and its public methods are static.
 * </p>
 */
public class SponsorPayAdvertiser {

//	/**
//	 * Map of custom key/values to add to the parameters on the requests to the REST API.
//	 */
//	private static Map<String, String> sCustomParameters;
//
//	/**
//	 * Sets a map of custom key/values to add to the parameters on the requests to the REST API.
//	 * 
//	 * @param params
//	 */
//	public static void setCustomParameters(Map<String, String> params) {
//		sCustomParameters = params;
//	}
//
//	/**
//	 * Sets a map of custom key/values to add to the parameters on the requests to the REST API.
//	 * 
//	 * @param keys
//	 * @param values
//	 */
//	public static void setCustomParameters(String[] keys, String[] values) {
//		sCustomParameters = UrlBuilder.mapKeysToValues(keys, values);
//	}
//
//	/**
//	 * Clears the map of custom key/values to add to the parameters on the requests to the REST API.
//	 */
//	public static void clearCustomParameters() {
//		sCustomParameters = null;
//	}

	/**
	 * Keep track of the persisted state of the Advertiser part of the SDK
	 */
	private SponsorPayAdvertiserState mPersistedState;

	/**
	 * Singleton instance.
	 */
	private static SponsorPayAdvertiser mInstance;

	/**
	 * Returns the map of custom key/values to add to the parameters on the requests to the REST
	 * API.
	 * 
	 * @param
	 * @return If passedParameters is not null, a copy of it is returned. Otherwise if the
	 *         parameters set with {@link #setCustomParameters(Map)} or
	 *         {@link #setCustomParameters(String[], String[])} are not null, a copy of that map is
	 *         returned. Otherwise null is returned.
	 */
	private static HashMap<String, String> getCustomParameters(Map<String, String> passedParameters) {
		HashMap<String, String> retval = null;

		if (passedParameters != null){
			retval = new HashMap<String, String>(passedParameters);
		}
		Map<String, String> parameters = SponsorPayParametersProvider.getParameters();
		if (parameters != null) {
			if (retval != null) {
				retval.putAll(parameters);
			} else {
				retval = new HashMap<String, String>(parameters);
			}
		}
		return retval;
	}

	/**
	 * Constructor. Stores the received application context and loads up the shared preferences.
	 * 
	 * @param context
	 *            The host application context.
	 */
	private SponsorPayAdvertiser(Context context) {
		if (context == null) {
			throw new RuntimeException("The SDK was not initialized yet. You should call SponsorPay.start method");
		}
		mPersistedState = new SponsorPayAdvertiserState(context);
	}
	
	private static SponsorPayAdvertiser getInstance(Context context) {
		if (mInstance == null) {
			mInstance = new SponsorPayAdvertiser(context);
		}
		return mInstance;
	}
	
	
	/**
	 * This method does the actual registration at the SponsorPay backend, performing the advertiser
	 * callback, and including in it a parameter to signal if a successful response has been
	 * received yet.
	 * 
	 * @param credentialsToken
	 *            The token id of the credentials to be used.
	 * @param customParams
	 *            A map of extra key/value pairs to add to the request URL.
	 */
	private void register(String credentialsToken, Map<String, String> customParams) {
		SPCredentials credentials = SponsorPay.getCredentials(credentialsToken);
		
		/* Send asynchronous call to SponsorPay's API */
		InstallCallbackSender callback = new InstallCallbackSender(credentials, mPersistedState);
		callback.setCustomParams(customParams);
		callback.trigger();
	}
	
	private void notitfyActionCompletion(String credentialsToken, String actionId) {
		
		SPCredentials credentials = SponsorPay.getCredentials(credentialsToken);
		
		/* Send asynchronous call to SponsorPay's API */
		ActionCallbackSender callback = new ActionCallbackSender(
				actionId, credentials, mPersistedState);
		callback.trigger();
	}
	
	//================================================================================
	// Actions
	//================================================================================
	
	
	/**
	 * Report an Action completion. It will use the values hold on the current credentials.
	 * 
	 * @param actionId
	 *            the id of the action
	 */
	public static void reportActionCompletion(String actionId) {
		String credentialsToken = SponsorPay.getCurrentCredentials().getCredentialsToken();
		reportActionCompletion(credentialsToken, actionId);
	}
	

	/**
	 * Report an Action completion.
	 * 
	 * @param credentialsToken
	 * 			  the token id of credentials
	 * @param actionId
	 *            the id of the action
	 */
	public static void reportActionCompletion(String credentialsToken, String actionId) {
		try {
			SPIdValidator.validate(actionId);
		} catch (SPIdException e) {
			throw new RuntimeException("The provided Action ID is not valid. "
					+ e.getLocalizedMessage());
		}
		// The actual work is performed by the notitfyActionCompletion() instance method.
		//mInstance has to exist so we can have a credentialsToken, anyway, shielding it
		if (mInstance == null) {
			throw new RuntimeException("No valid credentials object was created yet.\n" +
					"You have to execute SponsorPay.start method first.");
		}
		mInstance.notitfyActionCompletion(credentialsToken, actionId);
	}
	
	//================================================================================
	// Callbacks
	//================================================================================

	/**
	 * Triggers the Advertiser callback. It will use the values hold on the current credentials.
	 * 
	 * @param context
	 *            Host application context.
	 */
	public static void register(Context context) {
		register(context, (Map<String, String>)null);
	}
	
	/**
	 * Triggers the Advertiser callback. It will use the values hold on the current credentials..
	 * 
	 * @param context
	 *            Host application context.
	 * @param customParams
	 *            A map of extra key/value pairs to add to the request URL.
	 */
	public static void register(Context context, Map<String, String> customParams) {
		String credentialsToken = SponsorPay.getCurrentCredentials().getCredentialsToken();
		register(credentialsToken, context, customParams);
	}

	/**
	 * Triggers the Advertiser callback.
	 * 
	 * @param credentialsToken
	 * 			  the token id of credentials
	 * @param context
	 *            Host application context.
	 * @param customParams
	 *            A map of extra key/value pairs to add to the request URL.
	 */
	public static void register(String credentialsToken, Context context, Map<String, String> customParams) {
		getInstance(context);
		
		// The actual work is performed by the register() instance method.
		mInstance.register(credentialsToken, getCustomParameters(customParams));
	}
	

	//================================================================================
    // Deprecated Methods
	//================================================================================

	
	//================================================================================
	// Callbacks
	//================================================================================

	/**
	 * Triggers the Advertiser callback. If passed a non-null and non-empty Application ID, it will
	 * be used. Otherwise the Application ID will be retrieved from the value defined in the host
	 * application's Android Manifest XML file.
	 * 
	 * @param context
	 *            Host application context.
	 * @param overrideAppId
	 *            The App ID to use.
	 *            
	 * @deprecated This method will be removed from a future version of the SDK. 
	 * 				Use {@link SponsorPayAdvertiser#register(Context)} instead.
	 */
	public static void register(Context context, String overrideAppId) {
		register(context, overrideAppId, null);
	}

	/**
	 * Triggers the Advertiser callback. If passed a non-null and non-empty Application ID, it will
	 * be used. Otherwise the Application ID will be retrieved from the value defined in the host
	 * application's Android Manifest XML file.
	 * 
	 * @param context
	 *            Host application context.
	 * @param overrideAppId
	 *            The App ID to use.
	 * @param customParams
	 *            A map of extra key/value pairs to add to the request URL.
	 * @deprecated This method will be removed from a future version of the SDK. 
	 * 				Use {@link SponsorPayAdvertiser#register(Context, Map)} instead.
	 */
	public static void register(Context context, String overrideAppId,
			Map<String, String> customParams) {

		getInstance(context);

		String credentialsToken = SponsorPay.getCredentials(overrideAppId, null, null, context);
		
		// The actual work is performed by the register() instance method.
		mInstance.register(credentialsToken, getCustomParameters(customParams));
	}
	

	
	//================================================================================
	// Delayed callback
	//================================================================================
	
	/**
	 * Triggers the Advertiser callback after the specified delay has passed. Will retrieve the App
	 * ID from the value defined in the host application's Android Manifest XML file.
	 * 
	 * @param context
	 *            Host application context.
	 * @param delayMin
	 *            The delay in minutes for triggering the Advertiser callback.
	 *            
	 * @deprecated We no longer support delayed callbacks. This method will be 
	 * 			   removed from a future version of the SDK
	 */
	public static void registerWithDelay(Context context, int delayMin) {
		registerWithDelay(context, delayMin, null, null);
	}

	/**
	 * Triggers the Advertiser callback after the specified delay has passed. Will use the provided
	 * App ID instead of trying to retrieve the one defined in the host application's manifest.
	 * 
	 * @param context
	 *            Host application context.
	 * @param delayMin
	 *            The delay in minutes for triggering the Advertiser callback.
	 * @param overrideAppId
	 *            The App ID to use.
	 *            
	 * @deprecated We no longer support delayed callbacks. This method will be 
	 * 			   removed from a future version of the SDK
	 */
	public static void registerWithDelay(Context context, int delayMin, String overrideAppId) {
		registerWithDelay(context, delayMin, overrideAppId, null);
	}

	/**
	 * Triggers the Advertiser callback after the specified delay has passed. Will use the provided
	 * App ID instead of trying to retrieve the one defined in the host application's manifest.
	 * 
	 * @param context
	 *            Host application context.
	 * @param delayMin
	 *            The delay in minutes for triggering the Advertiser callback.
	 * @param overrideAppId
	 *            The App ID to use.
	 * @param customParams
	 *            Map of custom key/values to add to the parameters on the requests to the REST API.
	 *            
	 * @deprecated We no longer support delayed callbacks. This method will be 
	 * 			   removed from a future version of the SDK
	 */
	public static void registerWithDelay(Context context, int delayMin, String overrideAppId,
			Map<String, String> customParams) {

		SponsorPayCallbackDelayer.callWithDelay(context, overrideAppId, delayMin,
				getCustomParameters(customParams));
	}
	
	
	
}
