package com.autonomy.find.util;

import com.autonomy.find.api.exceptions.SecurityTokenExpiredException;

import javax.servlet.http.HttpSession;

public class ExceptionsHelper {
    public static void checkSecurityExpiredError(final RuntimeException e) {
        handleExpiredError(e);
    }

    public static boolean isSecurityExpiredError(final RuntimeException e) {
        final String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return errorMsg.indexOf("securityinfo token has expired") != -1;
    }

    private static void handleExpiredError(final RuntimeException e) {
        if (isSecurityExpiredError(e)) {
            throw new SecurityTokenExpiredException(e.getMessage());
        }

    }

}
