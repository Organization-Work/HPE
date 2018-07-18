package com.autonomy.find.api.response;

import lombok.Data;

@Data
public class ResponseWithSuccessError {

  private final boolean success;
  private final String error;
  private String errorDetail = null;

    public ResponseWithSuccessError() {

        this.success = true;
        this.error = null;
    }

    public ResponseWithSuccessError(
            final boolean success,
            final String error) {

        this.success = success;
        this.error = error;
    }

    public ResponseWithSuccessError(
            final String error) {

        this.success = false;
        this.error = error;
    }

    public ResponseWithSuccessError(
            final String error, final String errorDetail) {

        this.success = false;
        this.error = error;
        this.errorDetail = errorDetail;
    }

}