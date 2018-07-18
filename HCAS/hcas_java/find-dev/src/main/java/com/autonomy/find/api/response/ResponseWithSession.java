package com.autonomy.find.api.response;

import lombok.Data;

@Data
public class ResponseWithSession extends ResponseWithSuccessError {

  private final String session;

    public ResponseWithSession(
            final String session,
            final boolean success,
            final String error) {

        super(success, error);
        this.session = session;
    }

    public ResponseWithSession(
            final String session) {

        super(true, null);
        this.session = session;
    }
}