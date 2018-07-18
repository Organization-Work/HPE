package com.autonomy.find.api.exceptions;

public class UserNotNamedException extends Exception {
	public UserNotNamedException(final String format, final Object... args) {
		super(String.format(format, args));
	}
}
