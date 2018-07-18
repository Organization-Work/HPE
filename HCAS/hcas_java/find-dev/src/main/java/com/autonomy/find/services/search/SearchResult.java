package com.autonomy.find.services.search;

import java.util.List;

import com.autonomy.aci.actions.idol.tag.Field;

import lombok.Data;

@Data
public class SearchResult<T> {
    private int requestId;
    private T result;

    public SearchResult(final int requestId, final T result) {
        this.requestId = requestId;
        this.result = result;
    }

	public int getRequestId() {
		return requestId;
	}

	public T getResult() {
		return result;
	}
}
