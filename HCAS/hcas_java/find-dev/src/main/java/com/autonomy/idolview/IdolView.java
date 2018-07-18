package com.autonomy.idolview;

import com.autonomy.aci.actions.idol.query.QuerySummaryElement;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/idolview/IdolView.java#1 $
 * <p/>
 * Copyright (c) 2012, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/05/21 $
 */
public interface IdolView {
    FilterResponse filteredCounts(RequestDetails filterSpec, Locale locale);

    DocSummary filteredDocuments(PagedRequestDetails filterSpec);

    DateCounts dates(RequestDetails filterSpec);

    Collection<Filter> filters();

    List<QuerySummaryElement> aqg(RequestDetails filterSpec);
}
