/**
 * Fyber Android SDK
 * <p/>
 * Copyright (c) 2015 Fyber. All rights reserved.
 */
package com.fyber.sampleapp.fragments;

import android.content.Intent;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.animation.Animation;
import android.widget.Button;

import com.fyber.ads.AdFormat;
import com.fyber.requesters.RequestCallback;
import com.fyber.requesters.RequestError;
import com.fyber.sampleapp.MainActivity;
import com.fyber.sampleapp.R;
import com.fyber.utils.FyberLogger;

public abstract class FyberFragment extends Fragment implements RequestCallback {

	protected static final int INTERSTITIAL_REQUEST_CODE = 8792;
	protected static final int OFFERWALL_REQUEST_CODE = 8795;
	protected static final int REWARDED_VIDEO_REQUEST_CODE = 8796;

	private boolean isRequestingState;
	private boolean isReadyToShowAd;
	protected Intent intent;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Do not create a new Fragment when the Activity is re-created such as orientation changes.
		setRetainInstance(true);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		//resetting button and Intent
		setButtonToOriginalState();
		resetIntent();
		isReadyToShowAd = false;
		((MainActivity) getActivity()).getViewPagerAdapter().notifyDataSetChanged();
	}

	/*
	* ** Fragment specific methods **
	*/

	protected abstract String getLogTag();

	protected abstract String getRequestText();

	protected abstract String getShowText();

	protected abstract Button getButton();

	protected abstract int getRequestCode();

	protected abstract void performRequest();


	// when a button is clicked, request or show the ad according to Intent availability
	protected void requestOrShowAd() {
		//avoid requesting an ad when already requesting
		if (!isRequestingState()) {
			//if we already have an Intent, we start the ad Activity
			if (isIntentAvailable()) {
				//start the ad format specific Activity
				startActivityForResult(intent, getRequestCode());

			} else {
				setButtonToRequestingMode();
				//perform the ad request. Each Fragment has its own implementation.
				performRequest();
			}
		}
	}

	/*
	* ** RequestCallback methods **
	*/

	@Override
	public void onAdAvailable(Intent intent) {
		isReadyToShowAd = true;
		((MainActivity) getActivity()).getViewPagerAdapter().notifyDataSetChanged();
		this.intent = intent;

		//if you are using a general purpose requestCallback like this you might want to verify which adFormat will this Intent show.
		//You can use the AdFormat class to obtain an AdFormat from a given Intent. Then you can perform ad format specific actions e.g.:
		AdFormat adFormat = AdFormat.fromIntent(intent);
		switch (adFormat) {
			case OFFER_WALL:
				//in our sample app, we want to show the offer wall in a single step.
				resetRequestingState();
				startActivityForResult(intent, OFFERWALL_REQUEST_CODE);
				break;
			default:
				//we only animate the button if it is not an Offer Wall Intent.
				Animation counterclockwiseAnimation = MainActivity.getCounterclockwiseAnimation();
				counterclockwiseAnimation.setAnimationListener(new CounterclockwiseButtonAnimationListener(true));
				getButton().startAnimation(counterclockwiseAnimation);
				setButtonToSuccessState();
				break;
		}
	}

	@Override
	public void onAdNotAvailable(AdFormat adFormat) {
		FyberLogger.d(getLogTag(), "No ad available");
		resetButtonStateWithAnimation();
	}

	@Override
	public void onRequestError(RequestError requestError) {
		FyberLogger.d(getLogTag(), "Something went wrong with the request: " + requestError.getDescription());
		resetButtonStateWithAnimation();
	}

	/*
	* ** State helper methods **
	*/

	private void resetRequestingState() {
		isRequestingState = false;
	}

	private void resetIntent() {
		intent = null;
	}

	protected boolean isIntentAvailable() {
		return intent != null;
	}

	protected boolean isRequestingState() {
		return isRequestingState;
	}

	public boolean isReadyToShowAd() {
		return isReadyToShowAd;
	}

	/*
	* ** UI state helper methods **
	*/

	private void resetButtonStateWithAnimation() {

		final Animation counterclockwiseAnimation = MainActivity.getCounterclockwiseAnimation();
		counterclockwiseAnimation.setAnimationListener(new CounterclockwiseButtonAnimationListener(false));
		Animation buttonAnimation = getButton().getAnimation();

		// to avoid overlapping animations we make sure that no animation is running before animating the button.
		if (buttonAnimation != null && !buttonAnimation.hasEnded()) {
			buttonAnimation.setAnimationListener(new Animation.AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					counterclockwiseAnimation.setStartOffset(200);
					animateButtonCounterclockwise(counterclockwiseAnimation);
				}

				@Override
				public void onAnimationRepeat(Animation animation) {

				}
			});
		} else {
			animateButtonCounterclockwise(counterclockwiseAnimation);
		}
	}

	private void animateButtonCounterclockwise(Animation counterclockwiseAnimation) {
		getButton().startAnimation(counterclockwiseAnimation);
	}

	// Custom animation listener that ensures button actions are only allowed after animations have finished
	private class CounterclockwiseButtonAnimationListener implements Animation.AnimationListener {

		private boolean isAdAvailable;

		public CounterclockwiseButtonAnimationListener(boolean isAnimationToSuccess) {
			this.isAdAvailable = isAnimationToSuccess;
		}

		@Override
		public void onAnimationStart(Animation animation) {

		}

		@Override
		public void onAnimationEnd(Animation animation) {
			resetRequestingState();
			if (!isAdAvailable) {
				setButtonToOriginalState();
				resetIntent();
			}
		}

		@Override
		public void onAnimationRepeat(Animation animation) {

		}
	}

	protected void setButtonToRequestingMode() {
		getButton().startAnimation(MainActivity.getClockwiseAnimation());
		getButton().setText(getString(R.string.getting_offers));
		isRequestingState = true;
	}

	protected void setButtonToSuccessState() {
		setButtonColorAndText(getButton(), getShowText(), getResources().getColor(R.color.colorAdAvailable));
	}

	protected void setButtonToOriginalState() {
		setButtonColorAndText(getButton(), getRequestText(), getResources().getColor(R.color.colorPrimary));
	}

	private void setButtonColorAndText(Button button, String text, int color) {
		Drawable drawable = button.getBackground();
		ColorFilter filter = new LightingColorFilter(color, color);
		drawable.setColorFilter(filter);
		button.setText(text);
	}

}
