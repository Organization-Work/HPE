package com.autonomy.find.api.exceptions;

public class UserNotCreatedException extends Exception {
	public UserNotCreatedException(final String format, final Object... args) {
		super(String.format(format, args));
	}
}
