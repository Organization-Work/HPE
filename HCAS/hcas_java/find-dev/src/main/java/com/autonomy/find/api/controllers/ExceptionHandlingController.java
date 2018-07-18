package com.autonomy.find.api.controllers;

import com.autonomy.find.api.response.ResponseWithSuccessError;
import com.autonomy.find.util.ExceptionsHelper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@ControllerAdvice
public class ExceptionHandlingController {

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(RuntimeException.class)
    @ResponseBody
    public ResponseWithSuccessError handleRuntimeException(final HttpServletRequest request, final RuntimeException e) {
        if (ExceptionsHelper.isSecurityExpiredError(e)) {
            final HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        }

        if("XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))) {
            return new ResponseWithSuccessError("Server error encountered.", e.getMessage());
        } else {
            throw e;
        }
    }
}
