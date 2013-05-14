/**
 * SponsorPay Android SDK
 *
 * Copyright 2011 - 2013 SponsorPay. All rights reserved.
 */

package com.sponsorpay.sdk.android.utils;

import java.util.HashSet;
import java.util.Set;

import android.util.Log;

public class SponsorPayLogger {

	public enum Level {
		VERBOSE,
		DEBUG,
		INFO,
		WARNING,
		ERROR,
	}
	
	private static boolean logging = false;

	public static boolean toggleLogging() {
		logging = !logging;
		return logging;
	}
	
	public static boolean isLogging() {
		return logging;
	}

	public static boolean enableLogging(boolean shouldLog) {
		logging = shouldLog;
		return logging;
	}

	public static void e(String tag, String message) {
		if (logging) {
			Log.e(tag, message);
			INSTANCE.log(Level.ERROR, tag, message, null);
		}
	}

	public static void e(String tag, String message, Exception exception) {
		if (logging) {
			Log.w(tag, message, exception);
			INSTANCE.log(Level.ERROR, tag, message, exception);
		}
	}

	public static void d(String tag, String message) {
		if (logging) {
			Log.d(tag, message);
			INSTANCE.log(Level.DEBUG, tag, message, null);
		}
	}

	public static void i(String tag, String message) {
		if (logging) {
			Log.i(tag, message);
			INSTANCE.log(Level.INFO, tag, message, null);
		}
	}

	public static void v(String tag, String message) {
		if (logging) {
			Log.v(tag, message);
			INSTANCE.log(Level.VERBOSE, tag, message, null);
		}
	}

	public static void w(String tag, String message) {
		if (logging) {
			Log.w(tag, message);
			INSTANCE.log(Level.WARNING, tag, message, null);
		}
	}

	public static void w(String tag, String message, Exception exception) {
		if (logging) {
			Log.w(tag, message, exception);
			INSTANCE.log(Level.WARNING, tag, message, exception);
		}
	}
	
	// Log to text view methods
	private static SponsorPayLogger INSTANCE = new SponsorPayLogger();
	
	private Set<SPLoggerListener> listeners = new HashSet<SPLoggerListener>();
	
	private SponsorPayLogger(){
	}
	
	public void log(final Level level, final String tag, final String message,
			final Exception exception) {
		if (!listeners.isEmpty()) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					for (SPLoggerListener listener : listeners) {
						listener.log(level, tag, message, exception);
					}
				}
			}).start();
		}
	}
	
	public static boolean addLoggerListener(SPLoggerListener newListener) {
		return INSTANCE.listeners.add(newListener);
	}
	
	public static boolean removeLoggerListener(SPLoggerListener listener) {
		return INSTANCE.listeners.remove(listener);
	}

}
