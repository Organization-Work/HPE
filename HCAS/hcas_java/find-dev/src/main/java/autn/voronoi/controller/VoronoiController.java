package autn.voronoi.controller;

import autn.voronoi.Voronoi;
import autn.voronoi.VoronoiProxy;
import com.autonomy.aci.actions.idol.query.QuerySummaryElement;
import com.autonomy.aci.client.services.AciServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/autn/voronoi/controller/VoronoiController.java#2 $
 * <p/>
 * Copyright (c) 2012, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/11/08 $
 */
@RequestMapping("/p")
@Controller
public class VoronoiController {
    @Autowired
    private VoronoiProxy voronoi;

    @RequestMapping(value="/nodegraph.json")
    @ResponseBody
    Voronoi.Graph nodegraph(
            @RequestParam(required = false, defaultValue = "*") final String query,
            @RequestParam(required = false, defaultValue = "6") final int childLimit,
            @RequestParam(required = false, defaultValue = "false") final boolean clusterSentiment,
            @RequestParam(required = false, defaultValue = "0") final int engine,
            @RequestParam(required = false, defaultValue = "true") final boolean clusterDedupe,
            @RequestParam(required = false, defaultValue = "10000") final int aqgMaxResults,
            @RequestParam(required = false, defaultValue = "200") final int termMaxResults,
            @RequestParam(required = false, defaultValue = "querysummary") final Voronoi.LinkMode linkMode,
            @RequestParam(required = false) final Double minScore,
            @RequestParam(required = false) final Long minDate,
            @RequestParam(required = false) final Long maxDate
    ) throws AciServiceException, XPathExpressionException {
        return voronoi.nodegraph(query, childLimit, clusterSentiment, engine, clusterDedupe, aqgMaxResults, termMaxResults, linkMode, minDate, maxDate, minScore);
    }

    @RequestMapping(value="/usergraph.json")
    @ResponseBody
    Voronoi.Graph usergraph(
            @RequestParam(required = false, defaultValue = "*") final String query,
            @RequestParam(required = false, defaultValue = "6") final int maxUsers,
            @RequestParam(required = false) final Double minScore
    ) throws AciServiceException, XPathExpressionException {
        return voronoi.usergraph(query, maxUsers, minScore);
    }

    @RequestMapping(value="/userSuggestions.json")
    @ResponseBody
    Map<String, Object> userSuggestions(
            @RequestParam final String user,
            @RequestParam(required = false) final Double minScore,
            @RequestParam(required = false, defaultValue = "true") final boolean summary
    ) throws AciServiceException, XPathExpressionException {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("profiles", voronoi.userSuggestions(user, summary, minScore));
        map.put("user", user);
        return map;
    }

    public static class TermsProfileRequest {
        public TermsProfileRequest() {}

        public ArrayList<String> docIds;
        public int maxTerms = -1;
        public boolean expand;
        public int engine;
    }

    @RequestMapping(value="/termsForProfiles.json")
    @ResponseBody
    Map<String, Object> termsForProfiles(
            @RequestBody final TermsProfileRequest request
    ) throws AciServiceException, XPathExpressionException {
        final Map<String, Object> map = new HashMap<String, Object>();
        final List<Voronoi.TermMeta> terms = voronoi.getTermsForProfiles(request.docIds, request.maxTerms);

        if (request.expand) {
            for (final Voronoi.TermMeta term : terms) {
                term.text = voronoi.findUnstemmedTerm(request.engine, term.term);
            }
        }

        map.put("terms", terms);
        return map;
    }

    @RequestMapping(value="/content.json")
    @ResponseBody
    Map<String,Object> suggest(
            @RequestParam final String reference,
            @RequestParam(required = false, defaultValue = "0") final int engine
    ) throws AciServiceException, XPathExpressionException {
        return voronoi.getContent(reference, engine);
    }

    @RequestMapping(value="/suggest.json")
    @ResponseBody
    Map<String,Object> suggest(
            @RequestParam(required = false, defaultValue = "0") final int pageNum,
            @RequestParam(required = false, defaultValue = "6") final int pageSize,
            @RequestParam final String[] reference,
            @RequestParam(required = false) final String[] exclude,
            @RequestParam(required = false, defaultValue = "true") final boolean summary,
            @RequestParam(required = false, defaultValue = "true") final boolean totalResults,
            @RequestParam(required = false, defaultValue = "0") final int engine,
            @RequestParam(required = false) final Double minScore,
            @RequestParam(required = false) final Long minDate,
            @RequestParam(required = false) final Long maxDate
    ) throws AciServiceException, XPathExpressionException {
        return voronoi.suggest(pageNum, pageSize, reference, exclude, summary, totalResults, engine, minDate, maxDate, minScore);
    }

    @RequestMapping(value="/query.json")
    @ResponseBody
    Map<String,Object> query(
            @RequestParam(required = false, defaultValue = "0") final int pageNum,
            @RequestParam(required = false, defaultValue = "6") final int pageSize,
            @RequestParam(required = false) final String[] exclude,
            @RequestParam final String[] query,
            @RequestParam(required = false, defaultValue = "true") final boolean summary,
            @RequestParam(required = false, defaultValue = "true") final boolean totalResults,
            @RequestParam(required = false, defaultValue = "true") final boolean matchAllTerms,
            @RequestParam(required = false, defaultValue = "0") final int engine,
            @RequestParam(required = false) final Double minScore,
            @RequestParam(required = false) final Long minDate,
            @RequestParam(required = false) final Long maxDate
    ) throws AciServiceException, XPathExpressionException {
        return voronoi.query(pageNum, pageSize, exclude, query, summary, totalResults, engine, minDate, maxDate, matchAllTerms, minScore);
    }

    @RequestMapping(value="/querysummary.json")
    @ResponseBody
    Map<String,List<QuerySummaryElement>> query(
            @RequestParam final String query,
            @RequestParam(required = false, defaultValue = "-1") final int maxResults,
            @RequestParam(required = false, defaultValue = "0") final int engine,
            @RequestParam(required = false) final Double minScore,
            @RequestParam(required = false) final Long minDate,
            @RequestParam(required = false) final Long maxDate
    ) throws AciServiceException, XPathExpressionException {
        return Collections.singletonMap("querysummary", voronoi.querysummary(query, maxResults, engine, minDate, maxDate, minScore));
    }

    @RequestMapping(value="/clusters.json")
    @ResponseBody
    Voronoi.Cluster clusters(
            @RequestParam(required = false, defaultValue = "*") final String query,
            @RequestParam(required = false, defaultValue = "6") final int childLimit,
            @RequestParam(required = false, defaultValue = "-1") final int maxResults,
            @RequestParam(required = false, defaultValue = "false") final boolean clusterSentiment,
            @RequestParam(required = false, defaultValue = "0") final int engine,
            @RequestParam(required = false, defaultValue = "true") final boolean clusterDedupe,
            @RequestParam(required = false) final Double minScore,
            @RequestParam(required = false) final Long minDate,
            @RequestParam(required = false) final Long maxDate
    ) throws AciServiceException, XPathExpressionException {
        return voronoi.clusters(query, childLimit, maxResults, clusterSentiment, engine, clusterDedupe, minDate, maxDate, false, minScore);
    }
}
