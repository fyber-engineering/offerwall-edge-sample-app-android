/**
 * SponsorPay Android Advertiser SDK
 *
 * Copyright 2011 SponsorPay. All rights reserved.
 */

package com.sponsorpay.sdk.android.advertiser;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import com.sponsorpay.sdk.android.HostInfo;
import com.sponsorpay.sdk.android.UrlBuilder;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Runs in the background the Advertiser Callback HTTP request.
 */
public class AdvertiserCallbackSender extends AsyncTask<HostInfo, Void, Boolean> {

	/**
	 * HTTP status code that the response should have in order to determine that the API has been
	 * contacted successfully.
	 */
	private static final int SUCCESFUL_HTTP_STATUS_CODE = 200;

	/**
	 * The API resource URL to contact when talking to the Sponsorpay Advertiser API
	 */
	private static final String API_PRODUCTION_RESOURCE_URL = "http://service.sponsorpay.com/installs";
	private static final String API_STAGING_RESOURCE_URL = "http://staging.service.sponsorpay.com/installs";

	/**
	 * The key for encoding the parameter corresponding to whether a previous invocation of the
	 * advertiser callback had received a successful response.
	 */
	private static final String SUCCESSFUL_ANSWER_RECEIVED_KEY = "answer_received";

	/**
	 * The HTTP request that will be executed to contact the API with the callback request
	 */
	private HttpUriRequest mHttpRequest;

	/**
	 * The response returned by the SponsorPay API
	 */
	private HttpResponse mHttpResponse;

	/**
	 * The HTTP client employed to call the Sponsorpay API
	 */
	private HttpClient mHttpClient;

	/**
	 * True if the advertiser callback was sent and received a successful response in a previous
	 * invocation.
	 */
	private boolean mWasAlreadySuccessful = false;

	/**
	 * Interface to be implemented by parties interested in the response from the SponsorPay server
	 * for the advertiser callback.
	 */
	public interface APIResultListener {

		/**
		 * Invoked when we receive a response for the advertiser callback request.
		 * 
		 * @param wasSuccessful
		 *            true if the request was successful, false otherwise.
		 */
		void onAPIResponse(boolean wasSuccessful);
	}

	/**
	 * Registered listener for the result of the advertiser callback request.
	 */
	private APIResultListener mListener;

	/**
	 * Used to extract required information for the host application and device. This data will be
	 * sent on the callback request.
	 */
	private HostInfo mHostInfo;

	/**
	 * <p>
	 * Constructor. Sets the request callback listener and stores the host information.
	 * </p>
	 * See {@link AdvertiserHostInfo} and {@link APIResultListener}.
	 * 
	 * @param hostInfo
	 *            the host information for the given device
	 * @param listener
	 *            the callback listener
	 */
	public AdvertiserCallbackSender(HostInfo hostInfo, APIResultListener listener) {
		mListener = listener;
		mHostInfo = hostInfo;
	}

	/**
	 * Set whether a previous invocation of the advertiser callback had received a successful
	 * response.
	 */
	public void setWasAlreadySuccessful(boolean value) {
		mWasAlreadySuccessful = value;
	}

	/**
	 * Triggers the callback request that contacts the Sponsorpay Advertiser API. If and when a
	 * succesful response is received from the server, the {@link APIResultListener} registered
	 * through the constructor {@link #AsyncAPICaller(AdvertiserHostInfo, APIResultListener)} will
	 * be notified.
	 */
	public void trigger() {
		// if HostInfo must launch a RuntimeException due to an invalid App ID value, let it do that
		// in the main thread:
		mHostInfo.getAppId();

		execute(mHostInfo);
	}

	/**
	 * <p>
	 * Method overridden from {@link AsyncTask}. Executed on a background thread, runs the API
	 * contact request.
	 * </p>
	 * <p>
	 * Encodes the host information in the request URL, runs the request, waits for the response,
	 * parses its status code and lets the UI thread receive the result and notify the registered
	 * {@link APIResultListener}.
	 * <p/>
	 * 
	 * @param params
	 *            Only one parameter of type {@link AdvertiserHostInfo} is expected.
	 * @return True for a succesful request, false otherwise. This value will be communicated to the
	 *         UI thread by the Android {@link AsyncTask} implementation.
	 */
	@Override
	protected Boolean doInBackground(HostInfo... params) {
		Boolean returnValue = null;

		HostInfo hostInfo = params[0];

		// Prepare HTTP request by URL-encoding the device information
		String baseUrl = SponsorPayAdvertiser.shouldUseStagingUrls() ? API_STAGING_RESOURCE_URL
				: API_PRODUCTION_RESOURCE_URL;

		String callbackUrl = UrlBuilder.buildUrl(baseUrl, hostInfo,
				new String[] { SUCCESSFUL_ANSWER_RECEIVED_KEY },
				new String[] { mWasAlreadySuccessful ? "1" : "0" });

		Log.d(AdvertiserCallbackSender.class.getSimpleName(),
				"Advertiser callback will be sent to: " + callbackUrl);

		mHttpRequest = new HttpGet(callbackUrl);
		mHttpClient = new DefaultHttpClient();

		try {
			mHttpResponse = mHttpClient.execute(mHttpRequest);

			// We're not parsing the response, just making sure that a successful status code has
			// been received.
			int responseStatusCode = mHttpResponse.getStatusLine().getStatusCode();

			if (responseStatusCode == SUCCESFUL_HTTP_STATUS_CODE) {
				returnValue = true;
			} else {
				returnValue = false;
			}

			Log.d(AdvertiserCallbackSender.class.getSimpleName(), "Server returned status code: "
					+ responseStatusCode);
		} catch (Exception e) {
			returnValue = false;
			Log.e(AdvertiserCallbackSender.class.getSimpleName(),
					"An exception occurred when trying to send advertiser callback: " + e);
		}
		return returnValue;
	}

	/**
	 * This method is called by the Android {@link AsyncTask} implementation in the UI thread (or
	 * the thread which invoked {@link #trigger()}) when
	 * {@link #doInBackground(AdvertiserHostInfo...)} returns. It will invoke the registered
	 * {@link APIResultListener}
	 * 
	 * @param requestWasSuccessful
	 *            true if the response has a successful status code (equal to
	 *            {@link #SUCCESFUL_HTTP_STATUS_CODE}). false otherwise.
	 */
	@Override
	protected void onPostExecute(Boolean requestWasSuccessful) {
		super.onPostExecute(requestWasSuccessful);

		if (mListener != null) {
			mListener.onAPIResponse(requestWasSuccessful);
		}
	}
}
