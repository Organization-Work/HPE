package com.autonomy.find.api.exceptions;

public class PropertyNotFoundException extends Exception {
	public PropertyNotFoundException() {
		super();
	}
	public PropertyNotFoundException(final String format, final Object... args) {
		super(String.format(format, args));
	}
}
