package com.autonomy.find.api.exceptions;

public class DocumentNotFoundException extends Exception {
	public DocumentNotFoundException() {super();}
	public DocumentNotFoundException(final String format, final Object... args) {
		super(String.format(format, args));
	}
}
