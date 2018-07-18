package com.autonomy.find.dto.Parametric;

import lombok.Data;

@Data
public class SearchData {
    private String queryText;
    private FilterGroup filterGroup;

    public SearchData() {

    }

    public SearchData(final String queryText, final FilterGroup filterGroup) {
        this.queryText = queryText;
        this.filterGroup = filterGroup;
    }
}
