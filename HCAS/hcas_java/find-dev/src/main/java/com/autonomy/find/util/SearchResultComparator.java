package com.autonomy.find.util;

import com.autonomy.aci.actions.idol.tag.Field;
import com.autonomy.find.services.search.SearchResult;

import java.util.Comparator;
import java.util.List;

public class SearchResultComparator implements Comparator<SearchResult<List<Field>>> {
    @Override
    public int compare(final SearchResult<List<Field>> o1, final SearchResult<List<Field>> o2) {
        final int v1 = (o1 == null ) ? -1 : o1.getRequestId();
        final int v2 = (o2 == null) ? -1 : o2.getRequestId();

        return v1 -  v2;
    }
}
