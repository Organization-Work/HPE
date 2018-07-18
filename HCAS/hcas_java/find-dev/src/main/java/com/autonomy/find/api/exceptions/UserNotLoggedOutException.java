package com.autonomy.find.api.exceptions;

public class UserNotLoggedOutException extends Exception {
	public UserNotLoggedOutException(final String format, final Object... args) {
		super(String.format(format, args));
	}
}