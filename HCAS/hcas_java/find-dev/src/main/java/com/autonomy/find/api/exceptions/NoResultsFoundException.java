package com.autonomy.find.api.exceptions;

public class NoResultsFoundException extends Exception {
	public NoResultsFoundException(final String format, final Object... args) {
		super(String.format(format, args));
	}
}