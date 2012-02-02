package com.sponsorpay.sdk.android.testapp;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.sponsorpay.sdk.android.SponsorPay;
import com.sponsorpay.sdk.android.advertiser.SponsorPayAdvertiser;
import com.sponsorpay.sdk.android.publisher.OfferBanner;
import com.sponsorpay.sdk.android.publisher.OfferBannerRequest;
import com.sponsorpay.sdk.android.publisher.SPOfferBannerListener;
import com.sponsorpay.sdk.android.publisher.SponsorPayPublisher;
import com.sponsorpay.sdk.android.publisher.InterstitialLoader.InterstitialLoadingStatusListener;
import com.sponsorpay.sdk.android.publisher.SponsorPayPublisher.UIStringIdentifier;
import com.sponsorpay.sdk.android.publisher.currency.CurrencyServerAbstractResponse;
import com.sponsorpay.sdk.android.publisher.currency.CurrencyServerDeltaOfCoinsResponse;
import com.sponsorpay.sdk.android.publisher.currency.SPCurrencyServerListener;
import com.sponsorpay.sdk.android.publisher.currency.VirtualCurrencyConnector;
import com.sponsorpay.sdk.android.testapp.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.method.KeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Example activity in order to show the usage of Sponsorpay Android SDK.
 */
public class SponsorpayAndroidTestAppActivity extends Activity implements SPOfferBannerListener {

	private static final String APP_ID_PREFS_KEY = "APP_ID";
	private static final String USER_ID_PREFS_KEY = "USER_ID";
	private static final String KEEP_OFFERWALL_OPEN_PREFS_KEY = "KEEP_OFFERWALL_OPEN";
	private static final String SKIN_NAME_PREFS_KEY = "SKIN_NAME";
	private static final String BACKGROUND_URL_PREFS_KEY = "BACKGROUND_URL";
	private static final String SECURITY_TOKEN_PREFS_KEY = "SECURITY_TOKEN";
	private static final String DELAY_PREFS_KEY = "DELAY";
	private static final String CURRENCY_NAME_PREFS_KEY = "CURRENCY_NAME";
	private static final String USE_STAGING_URLS_PREFS_KEY = "USE_STAGING_URLS";

	private static final int DEFAULT_DELAY_MIN = 15;
	private static final String DEFAULT_SECURITY_TOKEN_VALUE = "b6acfc0227c82edb41f118d638e3c1d4e9ce0189";

	/**
	 * Shared preferences file name. Stores the values entered into the UI fields.
	 */
	private static final String PREFERENCES_FILE_NAME = "SponsorPayAdvertiserState";

	private String mOverridenAppId;
	private String mUserId;
	private boolean mShouldStayOpen;
	private String mBackgroundUrl;
	private String mSkinName;
	private String mSecurityToken;
	private int mCallDelay;
	private String mCurrencyName;

	private EditText mAppIdField;
	private EditText mUserIdField;
	private CheckBox mKeepOfferwallOpenCheckBox;
	private EditText mSkinNameField;
	private EditText mBackgroundUrlField;
	private EditText mSecurityTokenField;
	private EditText mDelayField;
	private EditText mCurrencyNameField;

	private CheckBox mUseStagingUrlsCheckBox;

	private LinearLayout mBannerContainer;

	private boolean mShouldSendAdvertiserCallbackOnResume;

	private Map<String, String> mCustomKeyValuesForRequest;
	private EditText mCustomKeyField, mCustomValueField;
	private TextView mKeyValuesList;

	/**
	 * Called when the activity is first created. See {@link Activity}.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		((TextView) findViewById(R.id.sdk_version_string)).setText("SponsorPay Android SDK v. "
				+ SponsorPay.RELEASE_VERSION_STRING);

		mAppIdField = (EditText) findViewById(R.id.app_id_field);
		mUserIdField = (EditText) findViewById(R.id.user_id_field);
		mKeepOfferwallOpenCheckBox = (CheckBox) findViewById(R.id.keep_offerwall_open_checkbox);
		mSkinNameField = (EditText) findViewById(R.id.skin_name_field);
		mBackgroundUrlField = (EditText) findViewById(R.id.background_url_field);
		mSecurityTokenField = (EditText) findViewById(R.id.security_token_field);
		mDelayField = (EditText) findViewById(R.id.delay_field);
		mCurrencyNameField = (EditText) findViewById(R.id.currency_name_field);

		mUseStagingUrlsCheckBox = (CheckBox) findViewById(R.id.use_staging_urls_checkbox);

		mBannerContainer = (LinearLayout) findViewById(R.id.banner_container);

		setCustomErrorMessages();

		mCustomKeyValuesForRequest = new HashMap<String, String>();
		mCustomKeyField = (EditText) findViewById(R.id.custom_key_field);
		mCustomValueField = (EditText) findViewById(R.id.custom_value_field);

		mCustomKeyField.setKeyListener(new KeyListener() {
			
			@Override
			public boolean onKeyUp(View view, Editable text, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					mCustomValueField.requestFocus();
					return true;
				}
				return false;
			}
			
			@Override
			public boolean onKeyOther(View view, Editable text, KeyEvent event) {
				return false;
			}
			
			@Override
			public boolean onKeyDown(View view, Editable text, int keyCode, KeyEvent event) {
				return keyCode == KeyEvent.KEYCODE_ENTER;
			}
			
			@Override
			public int getInputType() {
				return InputType.TYPE_CLASS_TEXT;
			}
			
			@Override
			public void clearMetaKeyState(View view, Editable content, int states) {
			}
		});

		mCustomValueField.setKeyListener(new KeyListener() {
			
			@Override
			public boolean onKeyUp(View view, Editable text, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					SponsorpayAndroidTestAppActivity.this.onAddCustomParameterClick(null);
					mCustomKeyField.requestFocus();
					return true;
				}
				return false;
			}
			
			@Override
			public boolean onKeyOther(View view, Editable text, KeyEvent event) {
				return false;
			}
			
			@Override
			public boolean onKeyDown(View view, Editable text, int keyCode, KeyEvent event) {
				return keyCode == KeyEvent.KEYCODE_ENTER;
			}
			
			@Override
			public int getInputType() {
				return InputType.TYPE_CLASS_TEXT;
			}
			
			@Override
			public void clearMetaKeyState(View view, Editable content, int states) {
			}
		});

		
		mKeyValuesList = (TextView) findViewById(R.id.key_values_list);
		mShouldSendAdvertiserCallbackOnResume = true;
	}

	@Override
	protected void onPause() {
		// Save the state of the UI fields into the app preferences.
		fetchValuesFromFields();

		SharedPreferences prefs = getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);
		Editor prefsEditor = prefs.edit();

		prefsEditor.putString(APP_ID_PREFS_KEY, mOverridenAppId);
		prefsEditor.putString(USER_ID_PREFS_KEY, mUserId);
		prefsEditor.putBoolean(KEEP_OFFERWALL_OPEN_PREFS_KEY, mShouldStayOpen);
		prefsEditor.putString(BACKGROUND_URL_PREFS_KEY, mBackgroundUrl);
		prefsEditor.putString(SKIN_NAME_PREFS_KEY, mSkinName);
		prefsEditor.putString(SECURITY_TOKEN_PREFS_KEY, mSecurityToken);
		prefsEditor.putInt(DELAY_PREFS_KEY, mCallDelay);
		prefsEditor.putString(CURRENCY_NAME_PREFS_KEY, mCurrencyName);

		prefsEditor.putBoolean(USE_STAGING_URLS_PREFS_KEY, mUseStagingUrlsCheckBox.isChecked());

		prefsEditor.commit();

		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Recover the state of the UI fields from the app preferences.
		SharedPreferences prefs = getSharedPreferences(PREFERENCES_FILE_NAME, Context.MODE_PRIVATE);

		mOverridenAppId = prefs.getString(APP_ID_PREFS_KEY, "");
		mUserId = prefs.getString(USER_ID_PREFS_KEY, "");
		mShouldStayOpen = prefs.getBoolean(KEEP_OFFERWALL_OPEN_PREFS_KEY, true);
		mBackgroundUrl = prefs.getString(BACKGROUND_URL_PREFS_KEY, "");
		mSkinName = prefs.getString(SKIN_NAME_PREFS_KEY, "");
		mSecurityToken = prefs.getString(SECURITY_TOKEN_PREFS_KEY, DEFAULT_SECURITY_TOKEN_VALUE);
		mCallDelay = prefs.getInt(DELAY_PREFS_KEY, DEFAULT_DELAY_MIN);
		mCurrencyName = prefs.getString(CURRENCY_NAME_PREFS_KEY, "");

		setValuesInFields();

		mUseStagingUrlsCheckBox.setChecked(prefs.getBoolean(USE_STAGING_URLS_PREFS_KEY, false));

		if (mShouldSendAdvertiserCallbackOnResume) {
			if (mOverridenAppId != null && !mOverridenAppId.equals(""))
				sendAdvertiserCallback();
			else
				Log.w(SponsorpayAndroidTestAppActivity.class.getSimpleName(),
						"No advertiser callback is being sent on application launch because App ID is empty.");
			mShouldSendAdvertiserCallbackOnResume = false;
		}
	}

	/**
	 * Sets one custom UI message in the SDK to demonstrate the use of
	 * SponsorPayPublisher.setCustomUIStrings();
	 */
	private void setCustomErrorMessages() {
		EnumMap<UIStringIdentifier, Integer> customUIStrings = new EnumMap<UIStringIdentifier, Integer>(
				UIStringIdentifier.class);
		customUIStrings.put(UIStringIdentifier.ERROR_DIALOG_TITLE, R.string.custom_error_message);
		SponsorPayPublisher.setCustomUIStrings(customUIStrings, getApplicationContext());
	}

	/**
	 * Fetches user provided values from the state of the UI text fields and text boxes.
	 */
	private void fetchValuesFromFields() {
		mOverridenAppId = mAppIdField.getText().toString();
		mUserId = mUserIdField.getText().toString();
		mShouldStayOpen = mKeepOfferwallOpenCheckBox.isChecked();

		String skinNameValue = mSkinNameField.getText().toString();
		if (!skinNameValue.equals("")) {
			mSkinName = skinNameValue;
		}

		mBackgroundUrl = mBackgroundUrlField.getText().toString();
		mSecurityToken = mSecurityTokenField.getText().toString();

		String delay = mDelayField.getText().toString();
		Integer parsedInt;

		try {
			parsedInt = Integer.parseInt(delay);
			if (parsedInt <= 0) {
				parsedInt = DEFAULT_DELAY_MIN;
			}
		} catch (NumberFormatException e) {
			parsedInt = DEFAULT_DELAY_MIN;
		}

		mDelayField.setText(String.format("%d", parsedInt));

		mCallDelay = parsedInt;

		mCurrencyName = mCurrencyNameField.getText().toString();

		SponsorPayAdvertiser.setShouldUseStagingUrls(mUseStagingUrlsCheckBox.isChecked());
		SponsorPayPublisher.setShouldUseStagingUrls(mUseStagingUrlsCheckBox.isChecked());
	}

	/**
	 * Sets values in the state of the UI text fields and text boxes.
	 */
	private void setValuesInFields() {
		mAppIdField.setText(mOverridenAppId);
		mUserIdField.setText(mUserId);
		mKeepOfferwallOpenCheckBox.setChecked(mShouldStayOpen);
		mSkinNameField.setText(mSkinName);
		mBackgroundUrlField.setText(mBackgroundUrl);
		mSecurityTokenField.setText(mSecurityToken);
		mDelayField.setText(String.format("%d", mCallDelay));
		mCurrencyNameField.setText(mCurrencyName);
	}

	/**
	 * Triggered when the user clicks on the launch offer wall button.
	 * 
	 * @param v
	 */
	public void onLaunchOfferwallClick(View v) {
		fetchValuesFromFields();

		startActivityForResult(
		/* Pass in a User ID */
		SponsorPayPublisher.getIntentForOfferWallActivity(getApplicationContext(), mUserId,
				mShouldStayOpen, mOverridenAppId),
				SponsorPayPublisher.DEFAULT_OFFERWALL_REQUEST_CODE);
	}

	/**
	 * Sends the advertiser callback after fetching the values entered in the UI fields.
	 */
	private void sendAdvertiserCallback() {
		fetchValuesFromFields();

		try {
			SponsorPayAdvertiser.register(getApplicationContext(), mOverridenAppId);
		} catch (RuntimeException ex) {
			showCancellableAlertBox("Exception from SDK", ex.getMessage());
			Log.e(SponsorpayAndroidTestAppActivity.class.toString(), "SponsorPay SDK Exception: ",
					ex);
		}
	}

	/**
	 * Invoked when the user clicks on the "Send advertiser callback now" button.
	 * 
	 * @param v
	 */
	public void onSendCallbackNowButtonClick(View v) {
		sendAdvertiserCallback();
	}

	/**
	 * Invoked when the user clicks on the "Send advertiser callback with delay" button.
	 * 
	 * @param v
	 */
	public void onSendCallbackWithDelayButtonClick(View v) {
		fetchValuesFromFields();
		try {
			SponsorPayAdvertiser.registerWithDelay(getApplicationContext(), mCallDelay,
					mOverridenAppId);
		} catch (RuntimeException ex) {
			showCancellableAlertBox("Exception from SDK", ex.getMessage());
			Log.e(SponsorpayAndroidTestAppActivity.class.toString(), "SponsorPay SDK Exception: ",
					ex);
		}
	}

	/**
	 * Triggered when the user clicks on the launch interstitial button.
	 * 
	 * @param v
	 */
	public void onLaunchInsterstitialClick(View v) {
		fetchValuesFromFields();

		try {
			SponsorPayPublisher.loadShowInterstitial(this, mUserId,
					new InterstitialLoadingStatusListener() {

						@Override
						public void onInterstitialLoadingTimeOut() {
							Log.d(SponsorpayAndroidTestAppActivity.class.toString(),
									"onInterstitialLoadingTimeOut");
						}

						@Override
						public void onInterstitialRequestError() {
							Log.d(SponsorpayAndroidTestAppActivity.class.toString(),
									"onInterstitialRequestError");
						}

						@Override
						public void onNoInterstitialAvailable() {
							Log.d(SponsorpayAndroidTestAppActivity.class.toString(),
									"onNoInterstitialAvailable");
						}

						@Override
						public void onWillShowInterstitial() {
							Log.d(SponsorpayAndroidTestAppActivity.class.toString(),
									"onWillShowInterstitial");
						}

					}, mShouldStayOpen, mBackgroundUrl, mSkinName, 0, mOverridenAppId);
		} catch (RuntimeException ex) {
			showCancellableAlertBox("Exception from SDK", ex.getMessage());
			Log.e(SponsorpayAndroidTestAppActivity.class.toString(), "SponsorPay SDK Exception: ",
					ex);
		}
	}

	/**
	 * Triggered when the user clicks on the Request New Coins button. Will send a request for delta
	 * of coins to the currency server and register a callback object to show the result in a dialog
	 * box. Uses the values entered for User ID, App ID and Security Token.
	 * 
	 * @param v
	 */
	public void onRequestNewCoinsClick(View v) {
		fetchValuesFromFields();

		final String usedTransactionId = VirtualCurrencyConnector.fetchLatestTransactionId(
				getApplicationContext(), mOverridenAppId, mUserId);

		SPCurrencyServerListener requestListener = new SPCurrencyServerListener() {

			@Override
			public void onSPCurrencyServerError(CurrencyServerAbstractResponse response) {
				showCancellableAlertBox("Response or Request Error", String.format("%s\n%s\n%s\n",
						response.getErrorType(), response.getErrorCode(), response
								.getErrorMessage()));
			}

			@Override
			public void onSPCurrencyDeltaReceived(CurrencyServerDeltaOfCoinsResponse response) {
				showCancellableAlertBox("Response From Currency Server", String.format(
						"Delta of Coins: %s\n\n" + "Used Latest Transaction ID: %s\n\n"
								+ "Returned Latest Transaction ID: %s\n\n", response
								.getDeltaOfCoins(), usedTransactionId, response
								.getLatestTransactionId()));

			}
		};

		try {
			SponsorPayPublisher.requestNewCoins(getApplicationContext(), mUserId, requestListener,
					null, mSecurityToken, mOverridenAppId);
		} catch (RuntimeException ex) {
			showCancellableAlertBox("Exception from SDK", ex.getMessage());
			Log.e(SponsorpayAndroidTestAppActivity.class.toString(), "SponsorPay SDK Exception: ",
					ex);
		}
	}

	public void onRequestBannerClick(View v) {
		fetchValuesFromFields();

		Log.i(getClass().getSimpleName(), "Requesting banner");
		try {
			SponsorPayPublisher.requestOfferBanner(getApplicationContext(), mUserId, this, null,
					mCurrencyName, mOverridenAppId);
			scrollToBottom();
		} catch (RuntimeException ex) {
			showCancellableAlertBox("Exception from SDK", ex.getMessage());
			Log.e(SponsorpayAndroidTestAppActivity.class.toString(), "SponsorPay SDK Exception: ",
					ex);
		}
	}

	/**
	 * Invoked when the user clicks on the "Add" button on the custom key/values area.
	 * 
	 * @param v
	 */
	public void onAddCustomParameterClick(View v) {
		mCustomKeyValuesForRequest.put(mCustomKeyField.getText().toString(), mCustomValueField
				.getText().toString());

		SponsorPayAdvertiser.setCustomParameters(mCustomKeyValuesForRequest);
		SponsorPayPublisher.setCustomParameters(mCustomKeyValuesForRequest);

		mCustomKeyField.setText("");
		mCustomValueField.setText("");

		updateCustomParametersList();
	}

	/**
	 * Invoked when the user clicks on the "Clear" button on the custom key/values area.
	 * 
	 * @param v
	 */
	public void onClearCustomParametersClick(View v) {
		mCustomKeyValuesForRequest.clear();

		SponsorPayAdvertiser.setCustomParameters(mCustomKeyValuesForRequest);
		SponsorPayPublisher.setCustomParameters(mCustomKeyValuesForRequest);

		updateCustomParametersList();
	}

	private void updateCustomParametersList() {
		String text = "";

		Iterator<String> customKvIterator = mCustomKeyValuesForRequest.keySet().iterator();

		while (customKvIterator.hasNext()) {
			String key = customKvIterator.next();
			String value = mCustomKeyValuesForRequest.get(key);

			text += String.format("%s = %s\n", key, value);
		}

		mKeyValuesList.setText(text);
	}

	private void scrollToBottom() {
		ScrollView rootScrollView = (ScrollView) findViewById(R.id.root_scroll_view);
		rootScrollView.fullScroll(View.FOCUS_DOWN);
	}

	/**
	 * Shows an alert box with the provided title and message and a unique button to cancel it.
	 * 
	 * @param title
	 *            The title for the alert box.
	 * @param text
	 *            The text message to show inside the alert box.
	 */
	public void showCancellableAlertBox(String title, String text) {
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
		dialogBuilder.setTitle(title).setMessage(text).setCancelable(true);
		dialogBuilder.show();
	}

	@Override
	public void onSPOfferBannerAvailable(OfferBanner banner) {
		Log.i(OfferBanner.LOG_TAG, "onOfferBannerAvailable called");
		mBannerContainer.removeAllViews();
		mBannerContainer.addView(banner.getBannerView(this));
	}

	@Override
	public void onSPOfferBannerNotAvailable(OfferBannerRequest bannerRequest) {
		Log.i(OfferBanner.LOG_TAG, "onOfferBannerNotAvailable called");
		mBannerContainer.removeAllViews();
		TextView mMessageView = new TextView(getApplicationContext());
		mMessageView.setText(String.format(getString(R.string.banner_not_available), bannerRequest
				.getHttpStatusCode()));
		mBannerContainer.addView(mMessageView);
	}

	@Override
	public void onSPOfferBannerRequestError(OfferBannerRequest bannerRequest) {
		Log.i(OfferBanner.LOG_TAG, "onOfferBannerRequestError called. HTTP status code="
				+ bannerRequest.getHttpStatusCode());
		mBannerContainer.removeAllViews();
		TextView mMessageView = new TextView(getApplicationContext());

		Exception requestException = bannerRequest.getRequestException();
		String errorDescription = "";
		if (requestException != null) {
			if (requestException.getClass().isInstance(new java.net.UnknownHostException()))
				errorDescription = getString(R.string.banner_request_error_unknown_host);
			else
				errorDescription = String.format("%s", requestException.toString());
		}

		mMessageView.setText(String.format(getString(R.string.banner_request_error),
				errorDescription));
		mBannerContainer.addView(mMessageView);
	}
}