package com.autonomy.find.api.response;

import java.util.List;

import lombok.Data;

@Data
public class ResponseWithItems<A> extends ResponseWithSuccessError {

  private final List<A> items;

    public ResponseWithItems(
            final List<A> items,
            final boolean success,
            final String error) {

        super(success, error);
        this.items = items;
    }

    public ResponseWithItems(
            final List<A> items) {

        super(true, null);
        this.items = items;
    }
}