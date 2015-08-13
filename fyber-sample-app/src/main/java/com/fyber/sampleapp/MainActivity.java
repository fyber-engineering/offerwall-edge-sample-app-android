package com.fyber.sampleapp;

import android.graphics.ColorFilter;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.RotateAnimation;

import com.fyber.Fyber;
import com.fyber.sampleapp.fragments.FyberFragment;
import com.fyber.sampleapp.fragments.InterstitialFragment;
import com.fyber.sampleapp.fragments.OfferwallFragment;
import com.fyber.sampleapp.fragments.RewardedVideoFragment;
import com.fyber.utils.FyberLogger;

import butterknife.Bind;
import butterknife.ButterKnife;


// Fyber SDK takes advantage of the power of annotations to make mediation simpler to integrate.
// To enable mediation in this app simply uncomment @FyberSDK annotation line below.
// Also, make sure you have the right dependencies in your gradle file.
//@FyberSDK
public class MainActivity extends FragmentActivity {

	private static final String APP_ID = "22915";
	private static final String SECURITY_TOKEN = "token";

	private static final String USER_ID = "userId";

	private static final int DURATION_MILLIS = 300;
	private static final int DEGREES_360 = 360;
	private static final int DEGREES_0 = 0;
	private static final float PIVOT_X_VALUE = 0.5f;
	private static final float PIVOT_Y_VALUE = 0.5f;
	private static final int INTERSTITIAL_FRAGMENT = 0;
	private static final int REWARDED_VIDEO_FRAGMENT = 1;
	private static final int OFFER_WALL_FRAGMENT = 2;
	private static final String TAG = "FyberMainActivity";

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link FragmentPagerAdapter} derivative, which will keep every
	 * loaded fragment in memory. If this becomes too memory intensive, it
	 * may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	@Bind(R.id.tool_bar)
	Toolbar toolbar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_fyber_main);
		ButterKnife.bind(this);

//		enabling Fyber logs so that we can see what is going on the SDK level
		FyberLogger.enableLogging(BuildConfig.DEBUG);

		setupViewPager();
		setupToolbar();
		setupTabStrip();

	}

	@Override
	protected void onResume() {
		super.onResume();
		try {

			// ** SDK INITIALIZATION **

			//when you start Fyber SDK you get a Settings object that you can use to customise the SDK behaviour.
			//Have a look at the method 'customiseFyberSettings' to learn more about possible customisation.
			Fyber.Settings fyberSettings = Fyber
					.with(APP_ID, this)
					.withSecurityToken(SECURITY_TOKEN)
// by default Fyber SDK will start precaching. If you wish to only start precaching at a later time you can uncomment this line and use 'CacheManager' to start, pause or resume on demand.
//					.withManualPrecaching()
// if you do not provide an user id Fyber SDK will generate one for you
//					.withUserId(USER_ID)
					.start();
// uncomment to customise Fyber SDK
//			customiseFyberSettings(fyberSettings);

		} catch (IllegalArgumentException e) {
			Log.d(TAG, e.getLocalizedMessage());
		}
	}

	//User Settings to customise Fyber SDK behaviour
	private void customiseFyberSettings(Fyber.Settings fyberSettings) {
		fyberSettings.notifyUserOnReward(false)
				.closeOfferWallOnRedirect(true)
				.notifyUserOnCompletion(true)
				.addParameter("myCustomParamKey", "myCustomParamValue")
				.setCustomUIString(Fyber.Settings.UIStringIdentifier.GENERIC_ERROR, "my custom generic error msg");
	}

	/*
	* ** Fyber SDK: other features **
	*
	* > this method shows you a couple of features from Fyber SDK that we left out of the sample app:
	* > report installs and rewarded actions (mainly for advertisers)
	* > control over which thread should the requester callback run on
	* > creating a new Requester from an existing Requester
	*/

//	public void runExtraFeatures() {
//		FyberSdkExtraFeatures.reportInstall(this);
//		FyberSdkExtraFeatures.reportRewardedAction(this);
//		FyberSdkExtraFeatures.requestAdWithSpecificHandler(this);
//		FyberSdkExtraFeatures.createRequesterFromAnotherRequester(this);
//	}

	// ** Init helper functions **

	private void setupViewPager() {
		// Create the adapter that will return a fragment for each of the three
		// primary sections of the Activity.
		mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.setCurrentItem(1);
	}

	private void setupToolbar() {
		toolbar.setTitle(getString(R.string.fyber_header));
		toolbar.setLogo(R.drawable.ic_launcher);
	}

	private void setupTabStrip() {
		// Get PagerTabStrip
		PagerTabStrip strip = PagerTabStrip.class.cast(findViewById(R.id.pager_header));
		strip.setDrawFullUnderline(true);
		strip.setBackgroundColor(getResources().getColor(R.color.textColorPrimary));
		strip.setTabIndicatorColor(getResources().getColor(R.color.colorAdAvailable));
	}

	// ** Fragment navigation with page adapter helper methods**

	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			switch (position) {

				case INTERSTITIAL_FRAGMENT:
					return new InterstitialFragment();

				case REWARDED_VIDEO_FRAGMENT:
					return new RewardedVideoFragment();

				case OFFER_WALL_FRAGMENT:
					return new OfferwallFragment();

				default:
					return new RewardedVideoFragment();
			}
			// getItem is called to instantiate the fragment for the given page.
		}

		@Override
		public int getCount() {
			// Show 3 total pages ( one for each ad format).
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Drawable image;
			image = getDrawableForPosition(position);
			// changing the ad format icon's color according to ad availability
			setDrawableColor(position, image);
			//merging image with a SpannableString to allow custom title on a PageAdapter
			image.setBounds(0, 0, image.getIntrinsicWidth(), image.getIntrinsicHeight());
			ImageSpan imageSpan = new ImageSpan(image, ImageSpan.ALIGN_BOTTOM);
			SpannableString spannableString = new SpannableString(" ");
			spannableString.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			return spannableString;

		}

		private void setDrawableColor(int position, Drawable image) {
			if (getCurrentFragment(position) != null || position == OFFER_WALL_FRAGMENT) {
				//Offer Wall is always ready to show ads
				if (position == OFFER_WALL_FRAGMENT || getCurrentFragment(position).isReadyToShowAd()) {
					image.setColorFilter(getOffersAvailableColorFilter());
				} else {
					//reset ad format icon's color when there are no offers available
					image.setColorFilter(null);
				}
			}
		}

		private Drawable getDrawableForPosition(int position) {
			Drawable image;
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				image = getDrawable(imageResId[position]);
			} else {
				image = getResources().getDrawable(imageResId[position]);
			}
			return image;
		}

		private int[] imageResId = {
				R.drawable.ic_action_icon_interstitial,
				R.drawable.ic_action_icon_rewarded_video,
				R.drawable.ic_action_icon_offerwall
		};

		// Helper methods to obtain current fragment being displayed on the viewPager

		public FyberFragment getCurrentFragment(int position) {
			String tag = getFragmentTag(position);
			return (FyberFragment) getSupportFragmentManager().findFragmentByTag(tag);
		}

		private String getFragmentTag(int fragmentPosition) {
			return "android:switcher:" + mViewPager.getId() + ":" + fragmentPosition;
		}

		// creating a ColorFilter based on the success color. This is needed due to the limitations of using a custom icon as a title in a PageAdapter
		public ColorFilter getOffersAvailableColorFilter() {
			int iColor = getResources().getColor(R.color.colorAdAvailable);//Color.parseColor("#FFB549");

			int red = (iColor & 0xFF0000) / 0xFFFF;
			int green = (iColor & 0xFF00) / 0xFF;
			int blue = iColor & 0xFF;

			float[] matrix = {0, 0, 0, 0, red
					, 0, 0, 0, 0, green
					, 0, 0, 0, 0, blue
					, 0, 0, 0, 1, 0};

			ColorFilter colorFilter = new ColorMatrixColorFilter(matrix);

			return colorFilter;
		}
	}


	// ** Animations **

	public static Animation getClockwiseAnimation() {
		AnimationSet animationSet = new AnimationSet(true);
		RotateAnimation rotateAnimation = new RotateAnimation(DEGREES_0, DEGREES_360, Animation.RELATIVE_TO_SELF, PIVOT_X_VALUE, Animation.RELATIVE_TO_SELF, PIVOT_Y_VALUE);
		rotateAnimation.setDuration(DURATION_MILLIS);
		animationSet.addAnimation(rotateAnimation);

		return animationSet;
	}

	public static Animation getCounterclockwiseAnimation() {
		AnimationSet animationSet = new AnimationSet(true);
		RotateAnimation rotateAnimation = new RotateAnimation(DEGREES_360, DEGREES_0, Animation.RELATIVE_TO_SELF, PIVOT_X_VALUE, Animation.RELATIVE_TO_SELF, PIVOT_Y_VALUE);
		rotateAnimation.setDuration(DURATION_MILLIS);
		animationSet.addAnimation(rotateAnimation);

		return animationSet;
	}

	// ** exposing PageAdapter so that each Fragment can update the tile according to offer availability **

	public FragmentPagerAdapter getViewPagerAdapter() {
		return mSectionsPagerAdapter;
	}

}
