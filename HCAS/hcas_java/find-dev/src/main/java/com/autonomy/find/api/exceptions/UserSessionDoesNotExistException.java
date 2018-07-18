package com.autonomy.find.api.exceptions;

public class UserSessionDoesNotExistException extends Exception {
	public UserSessionDoesNotExistException(final String format, final Object... args) {
		super(String.format(format, args));
	}
}
