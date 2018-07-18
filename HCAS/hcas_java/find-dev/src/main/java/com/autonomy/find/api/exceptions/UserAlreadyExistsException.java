package com.autonomy.find.api.exceptions;

public class UserAlreadyExistsException extends Exception {
	public UserAlreadyExistsException(final String format, final Object... args) {
		super(String.format(format, args));
	}
}