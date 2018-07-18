package com.autonomy.find.api.exceptions;

public class UsernamePasswordIncorrectException extends Exception {
	public UsernamePasswordIncorrectException(final String format, final Object... args) {
		super(String.format(format, args));
	}
}
