package com.autonomy.find.api.exceptions;

public class UserNotDeletedException extends Exception {
	public UserNotDeletedException(final String format, final Object... args) {
		super(String.format(format, args));
	}
}