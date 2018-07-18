package com.autonomy.find.api.exceptions;

public class UserNotLoggedInException extends Exception {
	public UserNotLoggedInException(final String format, final Object... args) {
		super(String.format(format, args));
	}
}