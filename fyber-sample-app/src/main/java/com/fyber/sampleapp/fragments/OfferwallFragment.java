package com.fyber.sampleapp.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.fyber.requesters.OfferWallRequester;
import com.fyber.sampleapp.R;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class OfferwallFragment extends FyberFragment {
	private static final String TAG = OfferwallFragment.class.getSimpleName();

	@Bind(R.id.offer_wall_button) Button offerwallButton;

	public OfferwallFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_offerwall,
				container, false);
		ButterKnife.bind(this, view);

		setButtonToSuccessState();

		return view;
	}

	// using butter knife to link Button click
	@OnClick(R.id.offer_wall_button)
	public void onOfferWallButtonCLicked(View view) {

		requestOrShowAd();
	}

	/*
	* ** Code to perform a an Offer Wall request **
	*/

	@Override
	protected void performRequest() {
		//Unless the device is not supported, OfferWallRequester will always return an Intent.
		//However, for consistency reasons Offer Wall has the same callback as other ad formats

		//Requesting the offer wall
		OfferWallRequester
				.create(this)
				.request(getActivity());
	}

	/*
	* ** FyberFragment methods **
	*/

	@Override
	public String getLogTag() {
		return TAG;
	}

	@Override
	public String getRequestText() {
		return getString(R.string.show_offer_wall);
	}

	@Override
	public String getShowText() {
		return getString(R.string.show_offer_wall);
	}

	@Override
	public Button getButton() {
		return offerwallButton;
	}

	@Override
	protected int getRequestCode() {
		return OFFERWALL_REQUEST_CODE;
	}

	/*
	* ** Offer wall specific state methods **
	 */

	@Override
	protected boolean isRequestingState() {
		//for our sample app, Offer Wall is never in the requesting sate. It is always ready to show.
		return false;
	}

	@Override
	protected void setButtonToRequestingMode() {
		//do nothing: there is only one state in Offer Wall
	}

	@Override
	protected void setButtonToOriginalState() {
		//do nothing: there is only one state in Offer Wall
	}

	@Override
	public boolean isReadyToShowAd() {
		return true;
	}
}
