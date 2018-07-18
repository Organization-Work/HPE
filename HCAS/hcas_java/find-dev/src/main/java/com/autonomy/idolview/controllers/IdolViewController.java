package com.autonomy.idolview.controllers;

import com.autonomy.aci.actions.idol.query.QuerySummaryElement;
import com.autonomy.idolview.DateCounts;
import com.autonomy.idolview.DocSummary;
import com.autonomy.idolview.Filter;
import com.autonomy.idolview.FilterResponse;
import com.autonomy.idolview.IdolView;
import com.autonomy.idolview.PagedRequestDetails;
import com.autonomy.idolview.RequestDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/idolview/controllers/IdolViewController.java#2 $
 * <p/>
 * Copyright (c) 2012, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/11/08 $
 */
@Controller
public class IdolViewController {
    @Autowired
    IdolView idolView;

    @RequestMapping("/p/204.do")
    void response204 (final HttpServletResponse response) throws IOException {
        response.sendError(204, "204 response");
    }

    @RequestMapping("/p/filter.json")
    @ResponseBody
    FilterResponse filteredCounts (@RequestBody final RequestDetails filterSpec, final Locale locale) {
        return idolView.filteredCounts(filterSpec, locale);
    }

    @RequestMapping("/p/filteredDocs.json")
    @ResponseBody
    DocSummary filteredDocuments(@RequestBody final PagedRequestDetails filterSpec) {
        return idolView.filteredDocuments(filterSpec);
    }

    @RequestMapping("/p/dates.json")
    @ResponseBody
    DateCounts dates (@RequestBody final RequestDetails filterSpec) {
        return idolView.dates(filterSpec);
    }

    @RequestMapping("/p/filters.json")
    @ResponseBody
    Collection<Filter> filters() {
        return idolView.filters();
    }

    @RequestMapping("/p/aqg.json")
    @ResponseBody
    List<QuerySummaryElement> aqg(@RequestBody final RequestDetails filterSpec) {
        return idolView.aqg(filterSpec);
    }

}
