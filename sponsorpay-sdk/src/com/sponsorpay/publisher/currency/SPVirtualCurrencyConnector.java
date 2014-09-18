/**
 * SponsorPay Android SDK
 *
 * Copyright 2011 - 2014 SponsorPay. All rights reserved.
 */
package com.sponsorpay.publisher.currency;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.widget.Toast;

import com.sponsorpay.SponsorPay;
import com.sponsorpay.credentials.SPCredentials;
import com.sponsorpay.publisher.SponsorPayPublisher;
import com.sponsorpay.publisher.SponsorPayPublisher.UIStringIdentifier;
import com.sponsorpay.publisher.currency.SPCurrencyServerRequester.SPCurrencyServerReponse;
import com.sponsorpay.publisher.currency.SPCurrencyServerRequester.SPVCSResultListener;
import com.sponsorpay.utils.HostInfo;
import com.sponsorpay.utils.SponsorPayLogger;
import com.sponsorpay.utils.StringUtils;

/**
 * <p>
 * Provides services to access SponsorPay's Virtual Currency Server.
 * </p>
 */
public class SPVirtualCurrencyConnector implements SPVCSResultListener {

	private static final String TAG = "SPVirtualCurrencyConnector";

	private static final String CURRENT_API_LEVEL_NOT_SUPPORTED_ERROR = "Only devices running Android API level 10 and above are supported";

	private static final int VCS_TIMER = 15;
	
	private static final String URL_PARAM_VALUE_NO_TRANSACTION = "NO_TRANSACTION";

	private static HashMap<String, CacheInfo> cacheInfo = new HashMap<String, SPVirtualCurrencyConnector.CacheInfo>();

		
	/**
	 * Key for the String containing the latest known transaction ID, which is
	 * saved as state in the Publisher SDK preferences file (whose name is
	 * defined in {@link SponsorPayPublisher#PREFERENCES_FILENAME}).
	 */
	private static final String STATE_LATEST_TRANSACTION_ID_KEY_PREFIX = "STATE_LATEST_CURRENCY_TRANSACTION_ID_";
	private static final String STATE_LATEST_TRANSACTION_ID_KEY_SEPARATOR = "_";
	
	private static final String STATE_TRANSACTION_ID_KEY_PREFIX = "STATE_LATEST_CURRENCY_TRANSACTION_ID_";

	private static final String DEFAULT_CURRENCY_ID_KEY_PREFIX = "DEFAULT_CURRENCY_ID_KEY";
	private static boolean showToastNotification = true;
	
	/**
	 * Boolean indicating if the toast notification 
	 * should be shown on a successful query .
	 */
	private boolean mShouldShowNotification;

	/**
	 * Custom currency name
	 */
	private String mCurrency;
	
	/**
	 * Credentials holding AppID, UserId and Security Token 
	 */
	protected SPCredentials mCredentials;
	
	/**
	 * Android application context.
	 */
	protected Context mContext;

	/**
	 * Map of custom key/values to add to the parameters on the requests.
	 */
	protected Map<String, String> mCustomParameters;
	
	protected SPCurrencyServerListener mCurrencyServerListener;

	/**
	 * Custom currency id
	 */
	private static String mCurrencyId;
			
	/**
	 * Initializes a new instance with the provided context and application
	 * data.
	 * 
	 * @param context
	 *            Android application context.
	 * @param credentialsToken
	 *            The token identifying the {@link SPCredentials} to be used.
	 * @param currencyServerListener
	 *            {@link SPCurrencyServerListener} registered by the developer
	 *            code to be notified of the result of requests to the Virtual
	 *            Currency Server.
	 */
	public SPVirtualCurrencyConnector(Context context, String credentialsToken, SPCurrencyServerListener currencyServerListener) {
		mCredentials = SponsorPay.getCredentials(credentialsToken);
		if (StringUtils.nullOrEmpty(mCredentials.getSecurityToken())) {
			throw new IllegalArgumentException("Security token has not been set on the credentials");
		}

		mContext = context;
		mCurrencyServerListener = currencyServerListener;
	}

	/**
	 * Sets a map of custom key/values to add to the parameters on the requests
	 * to the REST API.
	 */
	public SPVirtualCurrencyConnector setCustomParameters(Map<String, String> customParams) {
		mCustomParameters = customParams;
		return this;
	}

	/**
	 * Sets the custom currency name
	 * 
	 * @param currency
	 *            the custom currency name
	 * @return
	 */
	public SPVirtualCurrencyConnector setCurrency(String currency) {
		mCurrency = currency;
		return this;
	}
	
	/**
	 * Sends a request to the SponsorPay currency server to obtain the variation in amount of
	 * virtual currency for a given user since the last time this method was called. The response
	 * will be delivered to one of the registered listener's callback methods.
	 */
	public void fetchDeltaOfCoins() {
		fetchDeltaOfCoinsForCurrentUserSinceTransactionId(null, null);
	}

	/**
	 * Sends a request to the SponsorPay currency server to obtain the variation in amount of
	 * virtual currency for the current user's transactions newer than the one whose ID is passed.
	 * The response will be delivered to one of the registered listener's callback methods.
	 * 
	 * @param transactionId
	 *            Optionally, provide the ID of the latest known transaction. The delta of coins
	 *            will be calculated from this transaction (not included) up to the present. Leave
	 *            it to null to let the SDK use the latest transaction ID it kept track of.
	 * @param currencyId
	 *            Optionally, provide the ID of the currency that you want to retrieve. If the provided
	 *            ID is empty or null, then the default currency will be requested. If an invalid currency ID
	 *            will be provided then you will receive an invalid application error.           
	 */
	public void fetchDeltaOfCoinsForCurrentUserSinceTransactionId(String transactionId, String currencyId) {
		if (!HostInfo.isSupportedDevice()) {
			SPCurrencyServerErrorResponse errorResponse = new SPCurrencyServerErrorResponse(
					SPCurrencyServerRequestErrorType.ERROR_OTHER, "", CURRENT_API_LEVEL_NOT_SUPPORTED_ERROR);
			mCurrencyServerListener.onSPCurrencyServerError(errorResponse);
			return;
		}
		Calendar calendar = Calendar.getInstance();
		if (calendar.before(getCachedCalendar(calendar))) {
			SponsorPayLogger.d(TAG, "The VCS was queried less than " + VCS_TIMER + "s ago.Replying with cached response");
			SPCurrencyServerReponse response = getCachedResponse();
			if (response != null) {
				onSPCurrencyServerResponseReceived(response);
			} else {
				// this shouldn't occur, but still, we'll leave it there
				mCurrencyServerListener.onSPCurrencyServerError(new 
						SPCurrencyServerErrorResponse(SPCurrencyServerRequestErrorType.ERROR_OTHER,
						StringUtils.EMPTY_STRING, "Unknown error"));
			}
			return;
		}

		calendar.add(Calendar.SECOND, VCS_TIMER);
		setTimerCalendar(calendar);
		mShouldShowNotification = showToastNotification;
		
		mCurrencyId = currencyId;

		if (StringUtils.notNullNorEmpty(currencyId)) {
			transactionId = fetchLatestTransactionIdForCurrentAppAndUser();
			transactionId = setDefaultValueWhenTransactionIDisNullorEmpty(transactionId);
			SPCurrencyServerRequester.requestCurrency(this, mCredentials, transactionId, currencyId, mCustomParameters);
		} else {
				
			String defaultCurrencyId = getSavedDefaultCurrency();
			SharedPreferences prefs = mContext.getSharedPreferences(SponsorPayPublisher.PREFERENCES_FILENAME, Context.MODE_PRIVATE);	
			transactionId = prefs.getString(generatePreferencesLatestTransactionIdKey(mCredentials, defaultCurrencyId),
					URL_PARAM_VALUE_NO_TRANSACTION);
			
			transactionId = setDefaultValueWhenTransactionIDisNullorEmpty(transactionId);
			SPCurrencyServerRequester.requestCurrency(this, mCredentials, transactionId, null, mCustomParameters);
		}
	}
	
	private String setDefaultValueWhenTransactionIDisNullorEmpty(String transactionId) {
		if (StringUtils.nullOrEmpty(transactionId)) {
			transactionId = URL_PARAM_VALUE_NO_TRANSACTION;
		}
		return transactionId;
	}
	
	private String getSavedDefaultCurrency(){
		SharedPreferences prefs = mContext.getSharedPreferences(SponsorPayPublisher.PREFERENCES_FILENAME, Context.MODE_PRIVATE);
		return prefs.getString(DEFAULT_CURRENCY_ID_KEY_PREFIX, StringUtils.EMPTY_STRING);
	}
	
	/**
	 * Saves the provided transaction ID for the current user into the publisher state preferences
	 * file. Used to save the latest transaction id as returned by the server.
	 * 
	 * @param successfulResponse
	 *            Saves the response that has been received from the server into the shared preferences,
	 *            with key a standard prefix concatenated with the currency's ID and value the received
	 *            latest transaction ID.
	 */
	private void saveLatestTransactionForCurrentUser(SPCurrencyServerSuccessfulResponse successfulResponse) {
		SharedPreferences prefs = mContext.getSharedPreferences(SponsorPayPublisher.PREFERENCES_FILENAME, Context.MODE_PRIVATE);
		Editor editor = prefs.edit();
		editor.putString(generatePreferencesLatestTransactionIdKey(mCredentials),
				successfulResponse.getLatestTransactionId());

		if (successfulResponse.isDefault()) {
			editor.putString(DEFAULT_CURRENCY_ID_KEY_PREFIX, successfulResponse.getCurrencyId());
		}

		editor.commit();
	}

	/**
	 * Retrieves the saved latest known transaction ID for the current user from the publisher state
	 * preferences file.
	 * 
	 * @return The retrieved transaction ID or null.
	 */
	private String fetchLatestTransactionIdForCurrentAppAndUser() {
		return fetchLatestTransactionId(mContext, mCredentials.getCredentialsToken());
	}

	/**
	 * Retrieves the saved latest transaction ID for a given user from the publisher state
	 * preferences file.
	 * 
	 * @param context
	 *          Android application context.
	 * @param credentialsToken
	 * 			The credentials token.
	 * @return The retrieved transaction ID or null.
	 * @deprecated - This method will be removed on the next major release of
	 *             the SDK(v7.0.0)
	 */
	@Deprecated
	public static String fetchLatestTransactionId(Context context, String credentialsToken) {
		SPCredentials credentials = SponsorPay.getCredentials(credentialsToken);
		SharedPreferences prefs = context.getSharedPreferences(SponsorPayPublisher.PREFERENCES_FILENAME, Context.MODE_PRIVATE);
		String retval = null;
		if (StringUtils.nullOrEmpty(mCurrencyId)) {
			mCurrencyId = prefs.getString(DEFAULT_CURRENCY_ID_KEY_PREFIX, StringUtils.EMPTY_STRING);
		} 
		
		retval = prefs.getString(generatePreferencesLatestTransactionIdKey(credentials, mCurrencyId),
				URL_PARAM_VALUE_NO_TRANSACTION);
		
		return retval;
	}

	/**
	 * Saves the returned latest transaction id and shows the notification
	 * if required
	 */
	private void onDeltaOfCoinsResponse(SPCurrencyServerSuccessfulResponse response) {
		saveLatestTransactionForCurrentUser(response);
		// set the currency name. First try to get the provided name. If the
		// developer didn't provide a currency name, then try to get the
		// currency name from the response. Otherwise, show the default VCS.
		String currencyName = null;

		if (StringUtils.notNullNorEmpty(mCurrency)) {
			currencyName = mCurrency;
		} else {
			String serverCurrencyResponseName = response.getCurrencyName();
			currencyName = StringUtils.nullOrEmpty(serverCurrencyResponseName) ? serverCurrencyResponseName
					: SponsorPayPublisher.getUIString(UIStringIdentifier.VCS_DEFAULT_CURRENCY);
		}

		if (response.getDeltaOfCoins() > 0 && mShouldShowNotification) {
			String text = String.format(SponsorPayPublisher.getUIString(UIStringIdentifier.VCS_COINS_NOTIFICATION),
					response.getDeltaOfCoins(), currencyName);
			Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
		}
	}
	
	private static String generatePreferencesLatestTransactionIdKey(SPCredentials credentials) {
		return STATE_LATEST_TRANSACTION_ID_KEY_PREFIX + credentials.getAppId()
				+ STATE_LATEST_TRANSACTION_ID_KEY_SEPARATOR + credentials.getUserId() + STATE_LATEST_TRANSACTION_ID_KEY_SEPARATOR
				+ STATE_TRANSACTION_ID_KEY_PREFIX + mCurrencyId;
	}
	
	private static String generatePreferencesLatestTransactionIdKey(SPCredentials credentials, String credentialsId) {
		return STATE_LATEST_TRANSACTION_ID_KEY_PREFIX + credentials.getAppId()
				+ STATE_LATEST_TRANSACTION_ID_KEY_SEPARATOR + credentials.getUserId() + STATE_LATEST_TRANSACTION_ID_KEY_SEPARATOR
				+ STATE_TRANSACTION_ID_KEY_PREFIX + credentialsId;
	}

	/**
	 * Indicates whether the toast notification should be shown after a successful query
	 * @param showNotification
	 */
	public static void shouldShowToastNotification(boolean showNotification) {
		showToastNotification = showNotification;
	}

	@Override
	public void onSPCurrencyServerResponseReceived(SPCurrencyServerReponse response) {
		if (response instanceof SPCurrencyServerSuccessfulResponse) {
			
			SPCurrencyServerSuccessfulResponse lastResponse;
			SPCurrencyServerSuccessfulResponse successfulResponse = (SPCurrencyServerSuccessfulResponse) response;
			String defaultCurrency = getSavedDefaultCurrency();
			
			if (StringUtils.nullOrEmpty(mCurrencyId)
					&& (StringUtils.notNullNorEmpty(defaultCurrency) && !defaultCurrency.equalsIgnoreCase(successfulResponse.getCurrencyId()))) {
				
				SharedPreferences prefs = mContext.getSharedPreferences(SponsorPayPublisher.PREFERENCES_FILENAME, Context.MODE_PRIVATE);
				// save new default currency id
				prefs.getString(DEFAULT_CURRENCY_ID_KEY_PREFIX, successfulResponse.getCurrencyId());

				String transactionId = prefs.getString(generatePreferencesLatestTransactionIdKey(mCredentials),
						successfulResponse.getLatestTransactionId());
				// request again
				SPCurrencyServerRequester.requestCurrency(this, mCredentials, transactionId , null, mCustomParameters);
			} else {

				lastResponse = new SPCurrencyServerSuccessfulResponse(0, successfulResponse.getLatestTransactionId(), successfulResponse.getCurrencyId(),
						successfulResponse.getCurrencyName(), successfulResponse.isDefault());
				
				setCachedResponse(lastResponse);
				onDeltaOfCoinsResponse(successfulResponse);
				mCurrencyServerListener.onSPCurrencyDeltaReceived(successfulResponse);
			}
		} else {
			setCachedResponse(response);
			mCurrencyServerListener.onSPCurrencyServerError((SPCurrencyServerErrorResponse) response);
		}
	}
	
	//Helper methods

	private class CacheInfo {
		private Calendar calendar;
		private SPCurrencyServerReponse response;
	}

	private void setTimerCalendar(Calendar calendar) {
		CacheInfo pair = cacheInfo.get(createyKey());
		if (pair == null) {
			pair = new CacheInfo();
			cacheInfo.put(createyKey(), pair);
		}
		pair.calendar = calendar;
	}

	private void setCachedResponse(SPCurrencyServerReponse reponse) {
		CacheInfo pair = cacheInfo.get(createyKey());
		if (pair == null) {
			pair = new CacheInfo();
			cacheInfo.put(createyKey(), pair);
		}
		pair.response = reponse;
	}

	private Calendar getCachedCalendar(Calendar defaultIfNull) {
		CacheInfo pair = cacheInfo.get(createyKey());
		if (pair == null) {
			pair = new CacheInfo();
			pair.calendar = defaultIfNull;
			cacheInfo.put(createyKey(), pair);
		}
		return pair.calendar;
	}

	private SPCurrencyServerReponse getCachedResponse() {
		CacheInfo pair = cacheInfo.get(createyKey());
		if (pair == null) {
			pair = new CacheInfo();
			pair.calendar = Calendar.getInstance();
			cacheInfo.put(createyKey(), pair);
		}
		return pair.response;
	}
	
	private String createyKey(){
		String key = mCredentials.getCredentialsToken();
		if (mCurrencyId != null) {
			return key + mCurrencyId;
		} else {
			String defaultCurrencyId = getSavedDefaultCurrency();
		    return key + defaultCurrencyId;
		}
	}

}
