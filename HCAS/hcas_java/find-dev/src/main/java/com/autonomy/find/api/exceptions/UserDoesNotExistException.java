package com.autonomy.find.api.exceptions;

public class UserDoesNotExistException extends Exception {
	public UserDoesNotExistException(final String format, final Object... args) {
		super(String.format(format, args));
	}
}