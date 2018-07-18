package com.autonomy.find.util;

import java.util.Comparator;
import java.util.List;

import com.autonomy.find.services.search.SearchResult;

public class SearchResultComparatorVertica implements Comparator<SearchResult<List<com.autonomy.vertica.fields.Field>>> {
    @Override
    public int compare(final SearchResult<List<com.autonomy.vertica.fields.Field>> o1, final SearchResult<List<com.autonomy.vertica.fields.Field>> o2) {
        final int v1 = (o1 == null ) ? -1 : o1.getRequestId();
        final int v2 = (o2 == null) ? -1 : o2.getRequestId();

        return v1 -  v2;
    }
}
