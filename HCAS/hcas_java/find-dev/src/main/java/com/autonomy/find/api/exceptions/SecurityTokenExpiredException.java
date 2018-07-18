package com.autonomy.find.api.exceptions;

import com.autonomy.aci.client.services.AciErrorException;

public class SecurityTokenExpiredException extends AciErrorException {
    public SecurityTokenExpiredException() {

    }

    public SecurityTokenExpiredException(final String message) {
        super(message);
    }

    public SecurityTokenExpiredException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public SecurityTokenExpiredException(final Throwable cause) {
        super(cause);
    }


}
