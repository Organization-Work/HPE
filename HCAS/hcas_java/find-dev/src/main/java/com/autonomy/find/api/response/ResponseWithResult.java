package com.autonomy.find.api.response;

import lombok.Data;

@Data
public class ResponseWithResult<A> extends ResponseWithSuccessError {

  private final A result;

    public ResponseWithResult(
            final A result,
            final boolean success,
            final String error) {

        super(success, error);
        this.result = result;
    }

    public ResponseWithResult(
            final A result) {

        super(true, null);
        this.result = result;
    }
}