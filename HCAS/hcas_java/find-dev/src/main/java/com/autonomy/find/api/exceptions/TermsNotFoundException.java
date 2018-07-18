package com.autonomy.find.api.exceptions;

public class TermsNotFoundException extends Exception {
	
	public TermsNotFoundException() {
		super("");
	}
	
	public TermsNotFoundException(final String format, final Object... args) {
		super(String.format(format, args));
	}
	
}
