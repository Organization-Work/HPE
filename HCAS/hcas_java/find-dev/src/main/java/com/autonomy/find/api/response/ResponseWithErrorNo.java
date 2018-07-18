package com.autonomy.find.api.response;

import lombok.Data;

@Data
public class ResponseWithErrorNo extends ResponseWithSuccessError {

    private final Integer error_no;
    private String error_code;

    public ResponseWithErrorNo(
            final int error_no,
            final boolean success,
            final String error) {

        super(success, error);
        this.error_no = error_no;
        this.error_code = error_code;
    }

    public ResponseWithErrorNo(
            final int error_no,
            final String error) {

        super(false, error);
        this.error_no = error_no;
        this.error_code = error;
    }

    public ResponseWithErrorNo(
    ) {
        super(true, null);
        this.error_no = null;
        this.error_code = null;
    }
}