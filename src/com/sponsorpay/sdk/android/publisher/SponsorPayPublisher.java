/**
 * SponsorPay Android Publisher SDK
 *
 * Copyright 2011 SponsorPay. All rights reserved.
 */

package com.sponsorpay.sdk.android.publisher;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.sponsorpay.sdk.android.UrlBuilder;
import com.sponsorpay.sdk.android.publisher.InterstitialLoader.InterstitialLoadingStatusListener;
import com.sponsorpay.sdk.android.publisher.OfferBanner.AdShape;
import com.sponsorpay.sdk.android.publisher.currency.SPCurrencyServerListener;
import com.sponsorpay.sdk.android.publisher.currency.VirtualCurrencyConnector;
import com.sponsorpay.sdk.android.publisher.unlock.ItemIdValidator;
import com.sponsorpay.sdk.android.publisher.unlock.SPUnlockResponseListener;
import com.sponsorpay.sdk.android.publisher.unlock.SponsorPayUnlockConnector;
import com.sponsorpay.sdk.android.session.SPSession;
import com.sponsorpay.sdk.android.session.SPSessionManager;
import com.sponsorpay.sdk.android.utils.SponsorPayLogger;
import com.sponsorpay.sdk.android.utils.StringUtils;

/**
 * Provides convenience calls to load and show the mobile Offer Wall and the mobile Interstitial.
 */
public class SponsorPayPublisher {
	public static final String PREFERENCES_FILENAME = "SponsorPayPublisherState";

	/**
	 * Enumeration identifying the different messages which can be displayed in the user interface.
	 */
	public enum UIStringIdentifier {
		ERROR_DIALOG_TITLE, DISMISS_ERROR_DIALOG, GENERIC_ERROR, ERROR_LOADING_OFFERWALL, ERROR_LOADING_OFFERWALL_NO_INTERNET_CONNECTION, LOADING_INTERSTITIAL, LOADING_OFFERWALL, ERROR_PLAY_STORE_UNAVAILABLE
	};

	/**
	 * Messages which can be displayed in the user interface.
	 */
	private static EnumMap<UIStringIdentifier, String> sUIStrings;

	/**
	 * Map of custom key/values to add to the parameters on the requests to the REST API.
	 */
	private static Map<String, String> sCustomKeysValues;

	/**
	 * Default {@link AdShape} used to request Offer Banners to the backend.
	 */
	private static OfferBanner.AdShape sDefaultOfferBannerAdShape = OfferBanner.SP_AD_SHAPE_320X50;

	/**
	 * Fills {@link #sUIStrings} with the default messages.
	 */
	private static void initUIStrings() {
		sUIStrings = new EnumMap<UIStringIdentifier, String>(UIStringIdentifier.class);
		sUIStrings.put(UIStringIdentifier.ERROR_DIALOG_TITLE, "Error");
		sUIStrings.put(UIStringIdentifier.DISMISS_ERROR_DIALOG, "Dismiss");
		sUIStrings.put(UIStringIdentifier.GENERIC_ERROR,
				"An error happened when performing this operation");
		sUIStrings.put(UIStringIdentifier.ERROR_LOADING_OFFERWALL,
				"An error happened when loading the offer wall");
		sUIStrings.put(UIStringIdentifier.ERROR_LOADING_OFFERWALL_NO_INTERNET_CONNECTION,
				"An error happened when loading the offer wall (no internet connection)");
		sUIStrings.put(UIStringIdentifier.LOADING_INTERSTITIAL, "Loading...");
		sUIStrings.put(UIStringIdentifier.LOADING_OFFERWALL, "Loading...");
		sUIStrings.put(UIStringIdentifier.ERROR_PLAY_STORE_UNAVAILABLE, "You don't have the Google Play Store application on your device to complete App Install offers.");
	}

	/**
	 * Gets a particular UI message identified by a {@link UIStringIdentifier}.
	 * 
	 * @param identifier
	 *            The identifier of the message to get.
	 * @return The message string.
	 */
	public static String getUIString(UIStringIdentifier identifier) {
		if (sUIStrings == null) {
			initUIStrings();
		}

		return sUIStrings.get(identifier);
	}

	/**
	 * Replaces one of the UI messages with a custom text.
	 * 
	 * @param identifier
	 *            The identifier of the message to set.
	 * @param message
	 *            Custom text for the message.
	 */
	public static void setCustomUIString(UIStringIdentifier identifier, String message) {
		if (sUIStrings == null) {
			initUIStrings();
		}

		sUIStrings.put(identifier, message);
	}

	/**
	 * Replaces one or several of the UI messages at once.
	 * 
	 * @param messages
	 *            An EnumMap mapping {@link UIStringIdentifier}s to the respective desired texts.
	 */
	public static void setCustomUIStrings(EnumMap<UIStringIdentifier, String> messages) {
		for (UIStringIdentifier condition : UIStringIdentifier.values()) {
			if (messages.containsKey(condition)) {
				setCustomUIString(condition, messages.get(condition));
			}
		}
	}

	/**
	 * Replaces one of the UI messages with the text identified by an Android String resource id.
	 * 
	 * @param identifier
	 *            The {@link UIStringIdentifier} of the message to replace.
	 * @param message
	 *            An Android String resource identifier.
	 * @param context
	 *            An Android context used to fetch the resource
	 */
	public static void setCustomUIString(UIStringIdentifier identifier, int message, Context context) {
		setCustomUIString(identifier, context.getString(message));
	}

	public static void setCustomUIStrings(EnumMap<UIStringIdentifier, Integer> messages,
			Context context) {
		for (UIStringIdentifier condition : UIStringIdentifier.values()) {
			if (messages.containsKey(condition)) {
				setCustomUIString(condition, messages.get(condition), context);
			}
		}
	}

	/**
	 * Sets a map of custom key/values to add to the parameters on the requests to the REST API.
	 */
	public static void setCustomParameters(Map<String, String> params) {
		sCustomKeysValues = params;
	}

	/**
	 * Sets a map of custom key/values to add to the parameters on the requests to the REST API.
	 * 
	 * @param keys
	 * @param values
	 */
	public static void setCustomParameters(String[] keys, String[] values) {
		sCustomKeysValues = UrlBuilder.mapKeysToValues(keys, values);
	}

	/**
	 * Clears the map of custom key/values to add to the parameters on the requests to the REST API.
	 */
	public static void clearCustomParameters() {
		sCustomKeysValues = null;
	}

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
		HashMap<String, String> retval;

		if (passedParameters != null)
			retval = new HashMap<String, String>(passedParameters);
		else if (sCustomKeysValues != null)
			retval = new HashMap<String, String>(sCustomKeysValues);
		else {
			retval = null;
		}

		return retval;
	}

	private static boolean sShouldUseStagingUrls = false;

	public static void setShouldUseStagingUrls(boolean value) {
		sShouldUseStagingUrls = value;
	}

	public static boolean shouldUseStagingUrls() {
		return sShouldUseStagingUrls;
	}

	private static String sOverridingWebViewUrl;
	
	public static void setOverridingWebViewUrl(String url) {
		sOverridingWebViewUrl = url;
	}
	
	/**
	 * The default request code needed for starting the Offer Wall activity.
	 */
	public static final int DEFAULT_OFFERWALL_REQUEST_CODE = 0xFF;

	/**
	 * The default request code needed for starting the Unlock Offer Wall activity.
	 */
	public static final int DEFAULT_UNLOCK_OFFERWALL_REQUEST_CODE = 0xFE;

	/**
	 * <p>
	 * Returns an {@link Intent} that can be used to launch the {@link OfferWallActivity}.
	 * </p>
	 * <p>
	 * Will retrieve the publisher application id from the application manifest.
	 * </p>
	 * 
	 * @param context
	 *            The publisher application context.
	 * @param userId
	 *            The current user ID of the host application.
	 * @return An Android {@link Intent} which can be used with the {@link Activity} method
	 *         startActivityForResult() to launch the {@link OfferWallActivity}.
	 * @deprecated bla bal bal
	 */
	public static Intent getIntentForOfferWallActivity(Context context, String userId) {
		return getIntentForOfferWallActivity(context, userId, null, null, null);
	}
	
	/**
	 * <p>
	 * Returns an {@link Intent} that can be used to launch the {@link OfferWallActivity}.
	 * </p>
	 * <p>
	 * Will retrieve the publisher application id from the application manifest.
	 * </p>
	 * 
	 * @param context
	 *            The publisher application context.
	 * @param userId
	 *            The current user ID of the host application.
	 * @param currencyName
	 *            The name of the currency employed by your application. Provide null if you don't
	 *            use a custom currency name.	 
	 * @return An Android {@link Intent} which can be used with the {@link Activity} method
	 *         startActivityForResult() to launch the {@link OfferWallActivity}.
	 * @deprecated bla bal bal
	 */
	public static Intent getIntentForOfferWallActivity(Context context, String userId, String currencyName) {
		return getIntentForOfferWallActivity(context, userId, null, currencyName, null, null);
	}

	/**
	 * <p>
	 * Returns an {@link Intent} that can be used to launch the {@link OfferWallActivity}. Lets the
	 * caller specify the behavior of the Offer Wall once the user gets redirected out of the
	 * application by clicking on an offer.
	 * </p>
	 * <p>
	 * Will retrieve the publisher application id from the application manifest.
	 * </p>
	 * 
	 * @param context
	 *            The publisher application context.
	 * @param userId
	 *            The current user ID of the host application.
	 * @param shouldStayOpen
	 *            True if the Offer Wall should stay open after the user clicks on an offer and gets
	 *            redirected out of the app. False to close the Offer Wall.
	 * @return An Android {@link Intent} which can be used with the {@link Activity} method
	 *         startActivityForResult() to launch the {@link OfferWallActivity}.
	 * @deprecated bla bal bal
	 */
	public static Intent getIntentForOfferWallActivity(Context context, String userId,
			boolean shouldStayOpen) {

		return getIntentForOfferWallActivity(context, userId, shouldStayOpen, null, null);
	}
	
	/**
	 * <p>
	 * Returns an {@link Intent} that can be used to launch the {@link OfferWallActivity}. Lets the
	 * caller specify the behavior of the Offer Wall once the user gets redirected out of the
	 * application by clicking on an offer.
	 * </p>
	 * <p>
	 * Will retrieve the publisher application id from the application manifest.
	 * </p>
	 * 
	 * @param context
	 *            The publisher application context.
	 * @param userId
	 *            The current user ID of the host application.
	 * @param currencyName
	 *            The name of the currency employed by your application. Provide null if you don't
	 *            use a custom currency name.	
	 * @param shouldStayOpen
	 *            True if the Offer Wall should stay open after the user clicks on an offer and gets
	 *            redirected out of the app. False to close the Offer Wall.
	 * @return An Android {@link Intent} which can be used with the {@link Activity} method
	 *         startActivityForResult() to launch the {@link OfferWallActivity}.
	 * @deprecated bla bal bal
	 */
	public static Intent getIntentForOfferWallActivity(Context context, String userId,
			 String currencyName,  boolean shouldStayOpen) {
		
		return getIntentForOfferWallActivity(context, userId, shouldStayOpen, currencyName, null, null);
	}

	/**
	 * <p>
	 * Returns an {@link Intent} that can be used to launch the {@link OfferWallActivity}. Lets the
	 * caller specify the behavior of the Offer Wall once the user gets redirected out of the
	 * application by clicking on an offer.
	 * </p>
	 * 
	 * <p>
	 * Will use the provided publisher application id instead of trying to retrieve it from the
	 * application manifest.
	 * </p>
	 * 
	 * @param context
	 *            The publisher application context.
	 * @param userId
	 *            The current user ID of the host application.
	 * @param shouldStayOpen
	 *            True if the Offer Wall should stay open after the user clicks on an offer and gets
	 *            redirected out of the app. False to close the Offer Wall.
	 * @param overrideAppId
	 *            An app ID which will override the one included in the manifest.
	 * @return An Android {@link Intent} which can be used with the {@link Activity} method
	 *         startActivityForResult() to launch the {@link OfferWallActivity}.
	 * @deprecated bla bal bal
	 */
	public static Intent getIntentForOfferWallActivity(Context context, String userId,
			boolean shouldStayOpen, String overrideAppId) {

		return getIntentForOfferWallActivity(context, userId, shouldStayOpen, null, overrideAppId, null);
	}
	
	/**
	 * <p>
	 * Returns an {@link Intent} that can be used to launch the {@link OfferWallActivity}. Lets the
	 * caller specify the behavior of the Offer Wall once the user gets redirected out of the
	 * application by clicking on an offer.
	 * </p>
	 * 
	 * <p>
	 * Will use the provided publisher application id instead of trying to retrieve it from the
	 * application manifest.
	 * </p>
	 * 
	 * @param context
	 *            The publisher application context.
	 * @param userId
	 *            The current user ID of the host application.
	 * @param shouldStayOpen
	 *            True if the Offer Wall should stay open after the user clicks on an offer and gets
	 *            redirected out of the app. False to close the Offer Wall.
	 * @param currencyName
	 *            The name of the currency employed by your application. Provide null if you don't
	 *            use a custom currency name.	
	 * @param overrideAppId
	 *            An app ID which will override the one included in the manifest.
	 * @return An Android {@link Intent} which can be used with the {@link Activity} method
	 *         startActivityForResult() to launch the {@link OfferWallActivity}.
	 * @deprecated bla bal bal
	 */
	public static Intent getIntentForOfferWallActivity(Context context, String userId,
			boolean shouldStayOpen, String currencyName, String overrideAppId) {
		
		return getIntentForOfferWallActivity(context, userId, shouldStayOpen, currencyName, overrideAppId, null);
	}

	/**
	 * <p>
	 * Returns an {@link Intent} that can be used to launch the {@link OfferWallActivity}. Lets the
	 * caller specify the behavior of the Offer Wall once the user gets redirected out of the
	 * application by clicking on an offer.
	 * </p>
	 * 
	 * <p>
	 * Will use the provided publisher application id instead of trying to retrieve it from the
	 * application manifest.
	 * </p>
	 * 
	 * @param context
	 *            The publisher application context.
	 * @param userId
	 *            The current user ID of the host application.
	 * @param shouldStayOpen
	 *            True if the Offer Wall should stay open after the user clicks on an offer and gets
	 *            redirected out of the app. False to close the Offer Wall.
	 * @param customParams
	 *            A map of extra key/value pairs to add to the request URL.
	 * 
	 * @return An Android {@link Intent} which can be used with the {@link Activity} method
	 *         startActivityForResult() to launch the {@link OfferWallActivity}.
	 * @deprecated bla bal bal
	 */
	public static Intent getIntentForOfferWallActivity(Context context,
			String userId, Boolean shouldStayOpen, String overridingAppId,
			HashMap<String, String> customParams) {
		return getIntentForOfferWallActivity(context, userId, shouldStayOpen,
				null, overridingAppId, customParams);
	}
	
	/**
	 * <p>
	 * Returns an {@link Intent} that can be used to launch the {@link OfferWallActivity}. Lets the
	 * caller specify the behavior of the Offer Wall once the user gets redirected out of the
	 * application by clicking on an offer.
	 * </p>
	 * 
	 * <p>
	 * Will use the provided publisher application id instead of trying to retrieve it from the
	 * application manifest.
	 * </p>
	 * 
	 * @param context
	 *            The publisher application context.
	 * @param userId
	 *            The current user ID of the host application.
	 * @param shouldStayOpen
	 *            True if the Offer Wall should stay open after the user clicks on an offer and gets
	 *            redirected out of the app. False to close the Offer Wall.
	 * @param currencyName
	 *            The name of the currency employed by your application. Provide null if you don't
	 *            use a custom currency name.	 
	 * @param overridingAppId
	 *            An app ID which will override the one included in the manifest.
	 * @param customParams
	 *            A map of extra key/value pairs to add to the request URL.
	 * 
	 * @return An Android {@link Intent} which can be used with the {@link Activity} method
	 *         startActivityForResult() to launch the {@link OfferWallActivity}.
	 * 
	 * @deprecated this method will disappear in a future release of the SDK.
	 */
	public static Intent getIntentForOfferWallActivity(Context context,
			String userId, Boolean shouldStayOpen, String currencyName,
			String overridingAppId, HashMap<String, String> customParams) {
		String sessionToken = SPSessionManager.INSTANCE.getSession(overridingAppId, userId, null, context);
		return getIntentForOfferWallActivity(context, shouldStayOpen, sessionToken, currencyName, customParams);
	}
	
	public static Intent getIntentForOfferWallActivity(Context context,
			Boolean shouldStayOpen) {
		String sessionToken =  SPSessionManager.INSTANCE.getCurrentSession().getSessionToken();
		return getIntentForOfferWallActivity(context, shouldStayOpen, sessionToken, null, null);
	}

	public static Intent getIntentForOfferWallActivity(Context context,
			Boolean shouldStayOpen, String currencyName,
			HashMap<String, String> customParams) {
		String sessionToken =  SPSessionManager.INSTANCE.getCurrentSession().getSessionToken();
		return getIntentForOfferWallActivity(context, shouldStayOpen, sessionToken, currencyName, customParams);
	}
	
	public static Intent getIntentForOfferWallActivity(Context context,
			Boolean shouldStayOpen, String sessionToken, String currencyName,
			HashMap<String, String> customParams) {

		SPSession session = SPSessionManager.INSTANCE.getSession(sessionToken);


		Intent intent = new Intent(context, OfferWallActivity.class);
		intent.putExtra(OfferWallActivity.EXTRA_SESSION_TOKEN_KEY, session.getSessionToken());
//		intent.putExtra(OfferWallActivity.EXTRA_USERID_KEY, session.getUserId());

		if (shouldStayOpen != null) {
			intent.putExtra(OfferWallActivity.EXTRA_SHOULD_STAY_OPEN_KEY, shouldStayOpen);
		}

//		String overridingAppId = session.getAppId();
//		if (StringUtils.notNullNorEmpty(overridingAppId )) {
//			intent.putExtra(OfferWallActivity.EXTRA_OVERRIDING_APP_ID_KEY, overridingAppId);
//		}
		
		if (StringUtils.notNullNorEmpty(currencyName)) {
			intent.putExtra(OfferWallActivity.EXTRA_CURRENCY_NAME_KEY, currencyName);
		}

		if (sOverridingWebViewUrl != null) {
			intent.putExtra(OfferWallActivity.EXTRA_OVERRIDING_URL_KEY, sOverridingWebViewUrl);
		}

		intent.putExtra(OfferWallActivity.EXTRA_KEYS_VALUES_MAP_KEY,
				getCustomParameters(customParams));

		return intent;
	}

	/**
	 * 
	 * @param context
	 * @param userId
	 * @param unlockItemId
	 * @param unlockItemName
	 * @return
	 * @deprecated bla bal bal
	 */
	public static Intent getIntentForUnlockOfferWallActivity(Context context, String userId,
			String unlockItemId, String unlockItemName) {
		return getIntentForUnlockOfferWallActivity(context, userId, unlockItemId, unlockItemName,
				null, null);
	}

	/**
	 * 
	 * @param context
	 * @param userId
	 * @param unlockItemId
	 * @param unlockItemName
	 * @param overrideAppId
	 * @param customParams
	 * @return
	 * @deprecated this method will be removed in a future release of the SDK. 
	 */
	public static Intent getIntentForUnlockOfferWallActivity(Context context, String userId,
			String unlockItemId, String unlockItemName, String overrideAppId,
			HashMap<String, String> customParams) {
		String sessionToken = SPSessionManager.INSTANCE.getSession(overrideAppId, userId, null, context);
		return getIntentForUnlockOfferWallActivity(context,
				sessionToken, unlockItemId, unlockItemName, customParams);
	}
	
	
	public static Intent getIntentForUnlockOfferWallActivity(Context context,
			String unlockItemId, String unlockItemName) {
		String sessionToken = SPSessionManager.INSTANCE.getCurrentSession().getSessionToken();
		return getIntentForUnlockOfferWallActivity(context,
				sessionToken, unlockItemId, unlockItemName, null);
	}
	
	public static Intent getIntentForUnlockOfferWallActivity(Context context, String sessionToken, 
			String unlockItemId, String unlockItemName,	HashMap<String, String> customParams) {
		
		ItemIdValidator itemIdValidator = new ItemIdValidator(unlockItemId);
		if (!itemIdValidator.validate()) {
			throw new RuntimeException("The provided Unlock Item ID is not valid. "
					+ itemIdValidator.getValidationDescription());
		}
		SPSession session = SPSessionManager.INSTANCE.getSession(sessionToken);

		Intent intent = new Intent(context, OfferWallActivity.class);
		intent.putExtra(OfferWallActivity.EXTRA_SESSION_TOKEN_KEY, session.getSessionToken());
//		intent.putExtra(OfferWallActivity.EXTRA_USERID_KEY, session.getUserId());

		intent.putExtra(OfferWallActivity.EXTRA_OFFERWALL_TYPE,
				OfferWallActivity.OFFERWALL_TYPE_UNLOCK);
		intent.putExtra(OfferWallActivity.UnlockOfferWallTemplate.EXTRA_UNLOCK_ITEM_ID_KEY,
				unlockItemId);
		intent.putExtra(OfferWallActivity.UnlockOfferWallTemplate.EXTRA_UNLOCK_ITEM_NAME_KEY,
				unlockItemName);
		
//		String overrideAppId = session.getAppId();
//		if (StringUtils.notNullNorEmpty(overrideAppId )) {
//			intent.putExtra(OfferWallActivity.EXTRA_OVERRIDING_APP_ID_KEY, overrideAppId);
//		}

		if (sOverridingWebViewUrl != null) {
			intent.putExtra(OfferWallActivity.EXTRA_OVERRIDING_URL_KEY, sOverridingWebViewUrl);
		}
		
		intent.putExtra(OfferWallActivity.EXTRA_KEYS_VALUES_MAP_KEY, getCustomParameters(customParams));

		return intent;
	}

	/**
	 * Starts the mobile interstitial request / loading / showing process.
	 * 
	 * @param callingActivity
	 *            The activity which requests the interstitial. A progress dialog will be shown on
	 *            top of it and if an ad is returned, the calling activity will be used to launch
	 *            the {@link InterstitialActivity} in order to show the ad.
	 * @param userId
	 *            The current user ID of the host application.
	 * @param loadingStatusListener
	 *            {@link InterstitialLoadingStatusListener} to register to be notified of events in
	 *            the interstitial lifecycle.
	 * @param shouldStayOpen
	 *            Used to specify the behavior of the interstitial once the user clicks on the
	 *            presented ad and is redirected outside the host publisher app. The default
	 *            behavior is to close the interstitial and let the user go back to the activity
	 *            that called the interstitial when they come back to the app. If you want the
	 *            interstitial not to close until the user does it explicitly, set this parameter to
	 *            true.
	 * @param backgroundUrl
	 *            Can be set to the absolute URL of an image to use as background graphic for the
	 *            interstitial. Must include the protocol scheme (http:// or https://) at the
	 *            beginning of the URL. Leave it null for no custom background.
	 * @param skinName
	 *            Used to specify the name of a custom skin or template for the requested
	 *            interstitial. Leaving it null will make the interstitial fall back to the DEFAULT
	 *            template.
	 * @param loadingTimeoutSecs
	 *            Sets the maximum amount of time the interstitial should take to load. If you set
	 *            it to 0 or a negative number, it will fall back to the default value of 5 seconds.
	 * @param overriddenAppId
	 *            An app ID which will override the one included in the manifest.
	 * @deprecated bla bal bal
	 */
	public static void loadShowInterstitial(Activity callingActivity, String userId,
			InterstitialLoadingStatusListener loadingStatusListener, Boolean shouldStayOpen,
			String backgroundUrl, String skinName, int loadingTimeoutSecs, String overriddenAppId) {

		loadShowInterstitial(callingActivity, userId, loadingStatusListener, shouldStayOpen,
				backgroundUrl, skinName, loadingTimeoutSecs, null, overriddenAppId, null);
	}
	
	/**
	 * Starts the mobile interstitial request / loading / showing process.
	 * 
	 * @param callingActivity
	 *            The activity which requests the interstitial. A progress dialog will be shown on
	 *            top of it and if an ad is returned, the calling activity will be used to launch
	 *            the {@link InterstitialActivity} in order to show the ad.
	 * @param userId
	 *            The current user ID of the host application.
	 * @param loadingStatusListener
	 *            {@link InterstitialLoadingStatusListener} to register to be notified of events in
	 *            the interstitial lifecycle.
	 * @param shouldStayOpen
	 *            Used to specify the behavior of the interstitial once the user clicks on the
	 *            presented ad and is redirected outside the host publisher app. The default
	 *            behavior is to close the interstitial and let the user go back to the activity
	 *            that called the interstitial when they come back to the app. If you want the
	 *            interstitial not to close until the user does it explicitly, set this parameter to
	 *            true.
	 * @param backgroundUrl
	 *            Can be set to the absolute URL of an image to use as background graphic for the
	 *            interstitial. Must include the protocol scheme (http:// or https://) at the
	 *            beginning of the URL. Leave it null for no custom background.
	 * @param skinName
	 *            Used to specify the name of a custom skin or template for the requested
	 *            interstitial. Leaving it null will make the interstitial fall back to the DEFAULT
	 *            template.
	 * @param loadingTimeoutSecs
	 *            Sets the maximum amount of time the interstitial should take to load. If you set
	 *            it to 0 or a negative number, it will fall back to the default value of 5 seconds.
	 * @param currencyName
	 *            The name of the currency employed by your application. Provide null if you don't
	 *            use a custom currency name.
	 * @param overriddenAppId
	 *            An app ID which will override the one included in the manifest.
	 * @deprecated bla bal bal
	 */
	public static void loadShowInterstitial(Activity callingActivity, String userId,
			InterstitialLoadingStatusListener loadingStatusListener, Boolean shouldStayOpen,
			String backgroundUrl, String skinName, int loadingTimeoutSecs, String currencyName, String overriddenAppId) {
		
		loadShowInterstitial(callingActivity, userId, loadingStatusListener, shouldStayOpen,
				backgroundUrl, skinName, loadingTimeoutSecs, currencyName, overriddenAppId, null);
	}

	/**
	 * Starts the mobile interstitial request / loading / showing process.
	 * 
	 * @param callingActivity
	 *            The activity which requests the interstitial. A progress dialog will be shown on
	 *            top of it and if an ad is returned, the calling activity will be used to launch
	 *            the {@link InterstitialActivity} in order to show the ad.
	 * @param userId
	 *            The current user ID of the host application.
	 * @param loadingStatusListener
	 *            {@link InterstitialLoadingStatusListener} to register to be notified of events in
	 *            the interstitial lifecycle.
	 * @param shouldStayOpen
	 *            Used to specify the behavior of the interstitial once the user clicks on the
	 *            presented ad and is redirected outside the host publisher app. The default
	 *            behavior is to close the interstitial and let the user go back to the activity
	 *            that called the interstitial when they come back to the app. If you want the
	 *            interstitial not to close until the user does it explicitly, set this parameter to
	 *            true.
	 * @param backgroundUrl
	 *            Can be set to the absolute URL of an image to use as background graphic for the
	 *            interstitial. Must include the protocol scheme (http:// or https://) at the
	 *            beginning of the URL. Leave it null for no custom background.
	 * @param skinName
	 *            Used to specify the name of a custom skin or template for the requested
	 *            interstitial. Leaving it null will make the interstitial fall back to the DEFAULT
	 *            template.
	 * @param loadingTimeoutSecs
	 *            Sets the maximum amount of time the interstitial should take to load. If you set
	 *            it to 0 or a negative number, it will fall back to the default value of 5 seconds.
	 * @param overriddenAppId
	 *            An app ID which will override the one included in the manifest.
	 * @param customParams
	 *            A map of extra key/value pairs to add to the request URL.
	 * @deprecated bla bal bal
	 */
	public static void loadShowInterstitial(Activity callingActivity,
			String userId,
			InterstitialLoadingStatusListener loadingStatusListener,
			Boolean shouldStayOpen, String backgroundUrl, String skinName,
			int loadingTimeoutSecs, String overriddenAppId,
			Map<String, String> customParams) {
		loadShowInterstitial(callingActivity, userId, loadingStatusListener,
				shouldStayOpen, backgroundUrl, skinName, loadingTimeoutSecs,
				null, overriddenAppId, customParams);
	}
	 
	/**
	 * Starts the mobile interstitial request / loading / showing process.
	 * 
	 * @param callingActivity
	 *            The activity which requests the interstitial. A progress dialog will be shown on
	 *            top of it and if an ad is returned, the calling activity will be used to launch
	 *            the {@link InterstitialActivity} in order to show the ad.
	 * @param userId
	 *            The current user ID of the host application.
	 * @param loadingStatusListener
	 *            {@link InterstitialLoadingStatusListener} to register to be notified of events in
	 *            the interstitial lifecycle.
	 * @param shouldStayOpen
	 *            Used to specify the behavior of the interstitial once the user clicks on the
	 *            presented ad and is redirected outside the host publisher app. The default
	 *            behavior is to close the interstitial and let the user go back to the activity
	 *            that called the interstitial when they come back to the app. If you want the
	 *            interstitial not to close until the user does it explicitly, set this parameter to
	 *            true.
	 * @param backgroundUrl
	 *            Can be set to the absolute URL of an image to use as background graphic for the
	 *            interstitial. Must include the protocol scheme (http:// or https://) at the
	 *            beginning of the URL. Leave it null for no custom background.
	 * @param skinName
	 *            Used to specify the name of a custom skin or template for the requested
	 *            interstitial. Leaving it null will make the interstitial fall back to the DEFAULT
	 *            template.
	 * @param loadingTimeoutSecs
	 *            Sets the maximum amount of time the interstitial should take to load. If you set
	 *            it to 0 or a negative number, it will fall back to the default value of 5 seconds.
	 * @param currencyName
	 *            The name of the currency employed by your application. Provide null if you don't
	 *            use a custom currency name.
	 * @param overriddenAppId
	 *            An app ID which will override the one included in the manifest.
	 * @param customParams
	 *            A map of extra key/value pairs to add to the request URL.
	 * @deprecated bla bal bal
	 */
	public static void loadShowInterstitial(Activity callingActivity, String userId,
			InterstitialLoadingStatusListener loadingStatusListener, Boolean shouldStayOpen,
			String backgroundUrl, String skinName, int loadingTimeoutSecs, String currencyName, 
			String overriddenAppId, Map<String, String> customParams) {
		String sessionToken = SPSessionManager.INSTANCE.getSession(
				overriddenAppId, userId, null, callingActivity.getApplication());
		loadShowInterstitial(sessionToken, callingActivity,
				loadingStatusListener, shouldStayOpen, backgroundUrl, skinName,
				loadingTimeoutSecs, currencyName, customParams);
	}
	
	
	public static void loadShowInterstitial(Activity callingActivity,  
			InterstitialLoadingStatusListener loadingStatusListener, Boolean shouldStayOpen) {
		String sessionToken = SPSessionManager.INSTANCE.getCurrentSession().getSessionToken();
		loadShowInterstitial(sessionToken , callingActivity, loadingStatusListener, shouldStayOpen,
				null, null,0, null, null);
	}
	
	public static void loadShowInterstitial(String sessionToken, Activity callingActivity,  
			InterstitialLoadingStatusListener loadingStatusListener, Boolean shouldStayOpen,
			String backgroundUrl, String skinName, int loadingTimeoutSecs, String currencyName,
			Map<String, String> customParams) {
		

//		HostInfo hostInfo = new HostInfo(callingActivity);

//		String overriddenAppId = session.getAppId();
//		if (StringUtils.notNullNorEmpty(overriddenAppId)) {
//			hostInfo.setOverriddenAppId(overriddenAppId);
//		}

		InterstitialLoader il = new InterstitialLoader(callingActivity, sessionToken,
				loadingStatusListener);

		if (shouldStayOpen != null) {
			il.setShouldStayOpen(shouldStayOpen);
		}
		if (StringUtils.notNullNorEmpty(backgroundUrl)) {
			il.setBackgroundUrl(backgroundUrl);
		}
		if (StringUtils.notNullNorEmpty(skinName)) {
			il.setSkinName(skinName);
		}
		if (loadingTimeoutSecs > 0) {
			il.setLoadingTimeoutSecs(loadingTimeoutSecs);
		}
		if (StringUtils.notNullNorEmpty(currencyName)) {
			il.setCurrencyName(currencyName);
		}
		Map<String, String> extraParams = getCustomParameters(customParams);

		if (extraParams != null) {
			il.setCustomParameters(extraParams);
		}

		if (sOverridingWebViewUrl != null) {
			il.setOverridingUrl(sOverridingWebViewUrl);
		}
		
		il.startLoading();
	}
	

	/**
	 * Starts the mobile interstitial request / loading / showing process retrieving the application
	 * id from the Android Manifest.
	 * 
	 * @param callingActivity
	 *            The activity which requests the interstitial. A progress dialog will be shown on
	 *            top of it and if an ad is returned, the calling activity will be used to launch
	 *            the {@link InterstitialActivity} in order to show the ad.
	 * @param userId
	 *            The current user ID of the host application.
	 * @param loadingStatusListener
	 *            {@link InterstitialLoadingStatusListener} to register to be notified of events in
	 *            the interstitial lifecycle.
	 * @param shouldStayOpen
	 *            Used to specify the behavior of the interstitial once the user clicks on the
	 *            presented ad and is redirected outside the host publisher app. The default
	 *            behavior is to close the interstitial and let the user go back to the activity
	 *            that called the interstitial when they come back to the app. If you want the
	 *            interstitial not to close until the user does it explicitly, set this parameter to
	 *            true.
	 * @param backgroundUrl
	 *            Can be set to the absolute URL of an image to use as background graphic for the
	 *            interstitial. Must include the protocol scheme (http:// or https://) at the
	 *            beginning of the URL. Leave it null for no custom background.
	 * @param skinName
	 *            Used to specify the name of a custom skin or template for the requested
	 *            interstitial. Leaving it null will make the interstitial fall back to the DEFAULT
	 *            template.
	 * @param loadingTimeoutSecs
	 *            Sets the maximum amount of time the interstitial should take to load. If you set
	 *            it to 0 or a negative number, it will fall back to the default value of 5 seconds.
	 * @deprecated bla bal bal
	 */
	public static void loadShowInterstitial(Activity callingActivity, String userId,
			InterstitialLoadingStatusListener loadingStatusListener, Boolean shouldStayOpen,
			String backgroundUrl, String skinName, int loadingTimeoutSecs) {

		loadShowInterstitial(callingActivity, userId, loadingStatusListener, shouldStayOpen,
				backgroundUrl, skinName, loadingTimeoutSecs, null, null, null);
	}

	/**
	 * Starts the mobile interstitial request / loading / showing process using a default value for
	 * loadingTimeoutSecs and retrieving the application id from the Android Manifest.
	 * 
	 * @param callingActivity
	 *            The activity which requests the interstitial. A progress dialog will be shown on
	 *            top of it and if an ad is returned, the calling activity will be used to launch
	 *            the {@link InterstitialActivity} in order to show the ad.
	 * @param userId
	 *            The current user ID of the host application.
	 * @param loadingStatusListener
	 *            {@link InterstitialLoadingStatusListener} to register to be notified of events in
	 *            the interstitial lifecycle.
	 * @param shouldStayOpen
	 *            Used to specify the behavior of the interstitial once the user clicks on the
	 *            presented ad and is redirected outside the host publisher app. The default
	 *            behavior is to close the interstitial and let the user go back to the activity
	 *            that called the interstitial when they come back to the app. If you want the
	 *            interstitial not to close until the user does it explicitly, set this parameter to
	 *            true.
	 * @param backgroundUrl
	 *            Can be set to the absolute URL of an image to use as background graphic for the
	 *            interstitial. Must include the protocol scheme (http:// or https://) at the
	 *            beginning of the URL. Leave it null for no custom background.
	 * @param skinName
	 *            Used to specify the name of a custom skin or template for the requested
	 *            interstitial. Leaving it null will make the interstitial fall back to the DEFAULT
	 *            template.
	 * @deprecated bla bal bal
	 */
	public static void loadShowInterstitial(Activity callingActivity, String userId,
			InterstitialLoadingStatusListener loadingStatusListener, Boolean shouldStayOpen,
			String backgroundUrl, String skinName) {

		loadShowInterstitial(callingActivity, userId, loadingStatusListener, shouldStayOpen,
				backgroundUrl, skinName, 0, null, null, null);
	}
	
	/**
	 * Starts the mobile interstitial request / loading / showing process using a default value for
	 * loadingTimeoutSecs and retrieving the application id from the Android Manifest.
	 * 
	 * @param callingActivity
	 *            The activity which requests the interstitial. A progress dialog will be shown on
	 *            top of it and if an ad is returned, the calling activity will be used to launch
	 *            the {@link InterstitialActivity} in order to show the ad.
	 * @param userId
	 *            The current user ID of the host application.
	 * @param loadingStatusListener
	 *            {@link InterstitialLoadingStatusListener} to register to be notified of events in
	 *            the interstitial lifecycle.
	 * @param shouldStayOpen
	 *            Used to specify the behavior of the interstitial once the user clicks on the
	 *            presented ad and is redirected outside the host publisher app. The default
	 *            behavior is to close the interstitial and let the user go back to the activity
	 *            that called the interstitial when they come back to the app. If you want the
	 *            interstitial not to close until the user does it explicitly, set this parameter to
	 *            true.
	 * @param backgroundUrl
	 *            Can be set to the absolute URL of an image to use as background graphic for the
	 *            interstitial. Must include the protocol scheme (http:// or https://) at the
	 *            beginning of the URL. Leave it null for no custom background.
	 * @param skinName
	 *            Used to specify the name of a custom skin or template for the requested
	 *            interstitial. Leaving it null will make the interstitial fall back to the DEFAULT
	 *            template.
	 * @param currencyName
	 *            The name of the currency employed by your application. Provide null if you don't
	 *            use a custom currency name.	
	 * @deprecated bla bal bal
	 */
	public static void loadShowInterstitial(Activity callingActivity, String userId,
			InterstitialLoadingStatusListener loadingStatusListener, Boolean shouldStayOpen,
			String backgroundUrl, String skinName, String currencyName) {
		
		loadShowInterstitial(callingActivity, userId, loadingStatusListener, shouldStayOpen,
				backgroundUrl, skinName, 0, currencyName, null, null);
	}

	/**
	 * Starts the mobile interstitial request / loading / showing process using default values for
	 * backgroundUrl, skinName, loadingTimeoutSecs and retrieving the application id from the
	 * Android Manifest.
	 * 
	 * @param callingActivity
	 *            The activity which requests the interstitial. A progress dialog will be shown on
	 *            top of it and if an ad is returned, the calling activity will be used to launch
	 *            the {@link InterstitialActivity} in order to show the ad.
	 * @param userId
	 *            The current user ID of the host application.
	 * @param loadingStatusListener
	 *            {@link InterstitialLoadingStatusListener} to register to be notified of events in
	 *            the interstitial lifecycle.
	 * @param shouldStayOpen
	 *            Used to specify the behavior of the interstitial once the user clicks on the
	 *            presented ad and is redirected outside the host publisher app. The default
	 *            behavior is to close the interstitial and let the user go back to the activity
	 *            that called the interstitial when they come back to the app. If you want the
	 *            interstitial not to close until the user does it explicitly, set this parameter to
	 *            true.
	 * @deprecated bla bal bal
	 */
	public static void loadShowInterstitial(Activity callingActivity, String userId,
			InterstitialLoadingStatusListener loadingStatusListener, Boolean shouldStayOpen) {

		loadShowInterstitial(callingActivity, userId, loadingStatusListener, shouldStayOpen, null,
				null, 0, null, null, null);
	}
	/**
	 * Starts the mobile interstitial request / loading / showing process using default values for
	 * backgroundUrl, skinName, loadingTimeoutSecs and retrieving the application id from the
	 * Android Manifest.
	 * 
	 * @param callingActivity
	 *            The activity which requests the interstitial. A progress dialog will be shown on
	 *            top of it and if an ad is returned, the calling activity will be used to launch
	 *            the {@link InterstitialActivity} in order to show the ad.
	 * @param userId
	 *            The current user ID of the host application.
	 * @param loadingStatusListener
	 *            {@link InterstitialLoadingStatusListener} to register to be notified of events in
	 *            the interstitial lifecycle.
	 * @param shouldStayOpen
	 *            Used to specify the behavior of the interstitial once the user clicks on the
	 *            presented ad and is redirected outside the host publisher app. The default
	 *            behavior is to close the interstitial and let the user go back to the activity
	 *            that called the interstitial when they come back to the app. If you want the
	 *            interstitial not to close until the user does it explicitly, set this parameter to
	 *            true.
	 * @param currencyName
	 *            The name of the currency employed by your application. Provide null if you don't
	 *            use a custom currency name.	
	 * @deprecated bla bal bal
	 */
	public static void loadShowInterstitial(Activity callingActivity, String userId,
			InterstitialLoadingStatusListener loadingStatusListener, Boolean shouldStayOpen, String currencyName) {
		
		loadShowInterstitial(callingActivity, userId, loadingStatusListener, shouldStayOpen, null,
				null, 0, currencyName,  null, null);
	}

	/**
	 * Starts the mobile interstitial request / loading / showing process using default values for
	 * shouldStayOpen, backgroundUrl, skinName, loadingTimeoutSecs and retrieving the application id
	 * from the Android Manifest.
	 * 
	 * @param callingActivity
	 *            The activity which requests the interstitial. A progress dialog will be shown on
	 *            top of it and if an ad is returned, the calling activity will be used to launch
	 *            the {@link InterstitialActivity} in order to show the ad.
	 * @param userId
	 *            The current user ID of the host application.
	 * @param loadingStatusListener
	 *            {@link InterstitialLoadingStatusListener} to register to be notified of events in
	 *            the interstitial lifecycle.
	 * @deprecated bla bal bal
	 */
	public static void loadShowInterstitial(Activity callingActivity, String userId,
			InterstitialLoadingStatusListener loadingStatusListener) {

		loadShowInterstitial(callingActivity, userId, loadingStatusListener, null, null, null, 0,
				null, null, null);
	}
	/**
	 * Starts the mobile interstitial request / loading / showing process using default values for
	 * shouldStayOpen, backgroundUrl, skinName, loadingTimeoutSecs and retrieving the application id
	 * from the Android Manifest.
	 * 
	 * @param callingActivity
	 *            The activity which requests the interstitial. A progress dialog will be shown on
	 *            top of it and if an ad is returned, the calling activity will be used to launch
	 *            the {@link InterstitialActivity} in order to show the ad.
	 * @param userId
	 *            The current user ID of the host application.
	 * @param loadingStatusListener
	 *            {@link InterstitialLoadingStatusListener} to register to be notified of events in
	 *            the interstitial lifecycle.
	 * @param currencyName
	 *            The name of the currency employed by your application. Provide null if you don't
	 *            use a custom currency name.	
	 * @deprecated bla bal bal
	 */
	public static void loadShowInterstitial(Activity callingActivity, String userId,
			InterstitialLoadingStatusListener loadingStatusListener, String currencyName) {
		
		loadShowInterstitial(callingActivity, userId, loadingStatusListener, null, null, null, 0,
				currencyName, null, null);
	}

	/**
	 * Sends a request to the SponsorPay currency server to obtain the variation in amount of
	 * virtual currency for a given user. Returns immediately, and the answer is delivered to one of
	 * the provided listener's callback methods. See {@link SPCurrencyServerListener}.
	 * 
	 * @param context
	 *            Android application context.
	 * @param userId
	 *            The ID of the user for which the delta of coins will be requested.
	 * @param listener
	 *            {@link SPCurrencyServerListener} which will be notified of the result of the
	 *            request.
	 * @param transactionId
	 *            Optionally, provide the ID of the latest known transaction. The delta of coins
	 *            will be calculated from this transaction (not included) up to the present. Leave
	 *            it to null to let the SDK use the latest transaction ID it kept track of.
	 * @param securityToken
	 *            Security Token associated with the provided Application ID. It's used to sign the
	 *            requests and verify the server responses.
	 * @param applicationId
	 *            Application ID assigned by SponsorPay. Provide null to read the Application ID
	 *            from the Application Manifest.
	 * @deprecated bla bal bal
	 */
	public static void requestNewCoins(Context context, String userId,
			SPCurrencyServerListener listener, String transactionId, String securityToken,
			String applicationId) {

		requestNewCoins(context, userId, listener, transactionId, securityToken, applicationId,
				null);
	}

	/**
	 * Sends a request to the SponsorPay currency server to obtain the variation in amount of
	 * virtual currency for a given user. Returns immediately, and the answer is delivered to one of
	 * the provided listener's callback methods. See {@link SPCurrencyServerListener}.
	 * 
	 * @param context
	 *            Android application context.
	 * @param userId
	 *            The ID of the user for which the delta of coins will be requested.
	 * @param listener
	 *            {@link SPCurrencyServerListener} which will be notified of the result of the
	 *            request.
	 * @param transactionId
	 *            Optionally, provide the ID of the latest known transaction. The delta of coins
	 *            will be calculated from this transaction (not included) up to the present. Leave
	 *            it to null to let the SDK use the latest transaction ID it kept track of.
	 * @param securityToken
	 *            Security Token associated with the provided Application ID. It's used to sign the
	 *            requests and verify the server responses.
	 * @param applicationId
	 *            Application ID assigned by SponsorPay. Provide null to read the Application ID
	 *            from the Application Manifest.
	 * @param customParams
	 *            A map of extra key/value pairs to add to the request URL.
	 * @deprecated bla bal bal
	 */
	public static void requestNewCoins(Context context, String userId,
			SPCurrencyServerListener listener, String transactionId, String securityToken,
			String applicationId, Map<String, String> customParams) {
		
		String sessionToken = SPSessionManager.INSTANCE.getSession(applicationId, userId, securityToken, context);
		requestNewCoins(context, sessionToken, listener, transactionId, customParams);
	}
	
	public static void requestNewCoins(Context context, SPCurrencyServerListener listener) {
		String sessionToken = SPSessionManager.INSTANCE.getCurrentSession().getSessionToken();
		requestNewCoins(context, sessionToken, listener, null, null);
	}
	
	public static void requestNewCoins(Context context, String sessionToken,
			SPCurrencyServerListener listener, String transactionId, Map<String, String> customParams) {
		
//		HostInfo hostInfo = new HostInfo(context);
		
//		SPSession session = SPSessionManager.INSTANCE.getSession(sessionToken);
		
		
//		String applicationId = session.getAppId();
//		if (StringUtils.notNullNorEmpty(applicationId )) {
//			hostInfo.setOverriddenAppId(applicationId);
//		}
		
		VirtualCurrencyConnector vcc = new VirtualCurrencyConnector(context, sessionToken, listener);
		
		vcc.setCustomParameters(getCustomParameters(customParams));
		
		vcc.fetchDeltaOfCoinsForCurrentUserSinceTransactionId(transactionId);
	}
	/**
	 * 
	 * @param context
	 * @param userId
	 * @param listener
	 * @param securityToken
	 * @deprecated bla bal bal
	 */
	public static void requestUnlockItemsStatus(Context context, String userId,
			SPUnlockResponseListener listener, String securityToken) {

		requestUnlockItemsStatus(context, userId, listener, securityToken, null, null);
	}
	
	/**
	 * 
	 * @param context
	 * @param userId
	 * @param listener
	 * @param securityToken
	 * @param applicationId
	 * @param customParams
	 * 
	 * @deprecated bla bal bal
	 */
	public static void requestUnlockItemsStatus(Context context, String userId,
			SPUnlockResponseListener listener, String securityToken, String applicationId,
			Map<String, String> customParams) {
		String sessionToken = SPSessionManager.INSTANCE.getSession(applicationId, userId, securityToken, context);
		requestUnlockItemsStatus(context, sessionToken, listener, customParams);
	}
	
	public static void requestUnlockItemsStatus(Context context, SPUnlockResponseListener listener) {
		String sessionToken = SPSessionManager.INSTANCE.getCurrentSession().getSessionToken();
		requestUnlockItemsStatus(context, sessionToken, listener, (Map<String, String>)null);
	}
	
	public static void requestUnlockItemsStatus(Context context, String sessionToken,
			SPUnlockResponseListener listener, Map<String, String> customParams) {
		
//		HostInfo hostInfo = new HostInfo(context);
		
//		SPSession session = SPSessionManager.INSTANCE.getSession(sessionToken);
		
//		String applicationId = session.getAppId();
//		if (StringUtils.notNullNorEmpty(applicationId )) {
//			hostInfo.setOverriddenAppId(applicationId);
//		}
		
		SponsorPayUnlockConnector uc = new SponsorPayUnlockConnector(context, sessionToken, listener);
		
		uc.setCustomParameters(getCustomParameters(customParams));
		
		uc.fetchItemsStatus();
	}

	/**
	 * Requests an Offer Banner to the SponsorPay servers and registers a listener which will be
	 * notified when a response is received.
	 * 
	 * @param context
	 *            Android application context.
	 * @param userId
	 *            The ID of the user for whom the banner will be requested.
	 * @param listener
	 *            {@link SPOfferBannerListener} which will be notified of the results of the
	 *            request.
	 * @param offerBannerAdShape
	 *            Provide null for this parameter to request a banner of the default dimensions (320
	 *            x 50).
	 * @param currencyName
	 *            The name of the currency employed by your application. Provide null if you don't
	 *            use a custom currency name.
	 * @param applicationId
	 *            Your Application ID, or null to retrieve it from your application manifest.
	 * @return An {@link OfferBannerRequest} instance which manages the request to the server on the
	 *         background.
	 * @deprecated bla bal bal
	 */
	public static OfferBannerRequest requestOfferBanner(Context context, String userId,
			SPOfferBannerListener listener, OfferBanner.AdShape offerBannerAdShape,
			String currencyName, String applicationId) {

		return requestOfferBanner(context, userId, listener, offerBannerAdShape, currencyName,
				applicationId, null);
	}

	/**
	 * Requests an Offer Banner to the SponsorPay servers and registers a listener which will be
	 * notified when a response is received.
	 * 
	 * @param context
	 *            Android application context.
	 * @param userId
	 *            The ID of the user for whom the banner will be requested.
	 * @param listener
	 *            {@link SPOfferBannerListener} which will be notified of the results of the
	 *            request.
	 * @param offerBannerAdShape
	 *            Provide null for this parameter to request a banner of the default dimensions (320
	 *            x 50).
	 * @param currencyName
	 *            The name of the currency employed by your application. Provide null if you don't
	 *            use a custom currency name.
	 * @param applicationId
	 *            Your Application ID, or null to retrieve it from your application manifest.
	 * @param customParams
	 *            A map of extra key/value pairs to add to the request URL.
	 * 
	 * @return An {@link OfferBannerRequest} instance which manages the request to the server on the
	 *         background.
	 * @deprecated bla bal bal
	 */
	public static OfferBannerRequest requestOfferBanner(Context context, String userId,
			SPOfferBannerListener listener, OfferBanner.AdShape offerBannerAdShape,
			String currencyName, String applicationId, Map<String, String> customParams) {
		String sessionToken = SPSessionManager.INSTANCE.getSession(applicationId, userId, null, context);
		return requestOfferBanner(context, sessionToken, listener, offerBannerAdShape, currencyName, customParams);
	}
	
	
	public static OfferBannerRequest requestOfferBanner(Context context, SPOfferBannerListener listener) {
		return requestOfferBanner(context, listener, null, null, (Map<String, String>)null);
	}
	
	
	public static OfferBannerRequest requestOfferBanner(Context context,
			SPOfferBannerListener listener,	OfferBanner.AdShape offerBannerAdShape, 
			String currencyName, Map<String, String> customParams) {
		String sessionToken = SPSessionManager.INSTANCE.getCurrentSession().getSessionToken();
		return requestOfferBanner(context, sessionToken, listener, null, null, (Map<String, String>)null);
	}
	
	public static OfferBannerRequest requestOfferBanner(Context context, String sessionToken,
			SPOfferBannerListener listener,	OfferBanner.AdShape offerBannerAdShape, 
			String currencyName, Map<String, String> customParams) {
		
//		SPSession session = SPSessionManager.INSTANCE.getSession(sessionToken);
		
//		HostInfo hostInfo = new HostInfo(context);
//		
//		String applicationId = session.getAppId();
//		if (StringUtils.notNullNorEmpty(applicationId)) {
//			hostInfo.setOverriddenAppId(applicationId);
//		}
		
		if (offerBannerAdShape == null) {
			offerBannerAdShape = sDefaultOfferBannerAdShape;
		}
		
		OfferBannerRequest bannerRequest = new OfferBannerRequest(context, sessionToken,
				listener, offerBannerAdShape, currencyName, getCustomParameters(customParams));
		
		if (sOverridingWebViewUrl != null) {
			bannerRequest.setOverridingUrl(sOverridingWebViewUrl);
		}
		
		bannerRequest.requestOfferBanner();
		
		return bannerRequest;
	}

	/**
	 * Sets the provided cookie strings into the application's cookie manager for the given base
	 * URL.
	 * 
	 * @param cookies
	 *            An array of cookie strings.
	 * @param baseUrl
	 *            The base URL to set the cookies for.
	 * @param context
	 *            Android application context.
	 */
	static void setCookiesIntoCookieManagerInstance(String[] cookies, String baseUrl,
			Context context) {
		if (cookies == null || cookies.length == 0) {
			return;
		}

		CookieManager instance;

		// CookieSyncManager.createInstance() has to be called before we get CookieManager's
		// instance.
		try {
			CookieSyncManager.getInstance();
		} catch (IllegalStateException e) {
			CookieSyncManager.createInstance(context);
		}

		instance = CookieManager.getInstance();

		SponsorPayLogger.v(AsyncRequest.LOG_TAG, "Setting the following cookies into CookieManager instance "
				+ instance + " for base URL " + baseUrl + ": ");

		for (String cookieString : cookies) {
			instance.setCookie(baseUrl, cookieString);
			SponsorPayLogger.v(AsyncRequest.LOG_TAG, cookieString);
		}
	}

	/**
	 * Converts device pixels into screen pixels.
	 */
	static int convertDevicePixelsIntoPixelsMeasurement(float dps, Context context) {
		final float scale = context.getResources().getDisplayMetrics().density;
		int pixels = (int) (dps * scale + 0.5f);
		return pixels;
	}
}