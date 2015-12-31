package com.appspot.yourlittleone.domain;

/**
 * A simple wrapper for announcement message.
 */
public final class Announcement {

	private final String message;

	public Announcement(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
