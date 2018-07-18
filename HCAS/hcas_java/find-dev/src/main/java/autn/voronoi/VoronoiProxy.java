package autn.voronoi;

import com.autonomy.aci.actions.idol.query.QuerySummaryElement;
import com.autonomy.aci.client.services.AciServiceException;

import javax.xml.xpath.XPathExpressionException;
import java.util.List;
import java.util.Map;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/autn/voronoi/VoronoiProxy.java#2 $
 * <p/>
 * Copyright (c) 2012, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/11/08 $
 */
public interface VoronoiProxy {
    Voronoi.Graph nodegraph(
            String query,
            int childLimit,
            boolean clusterSentiment,
            int engine,
            boolean clusterDedupe,
            int aqgMaxResults,
            int termMaxResults,
            Voronoi.LinkMode linkMode,
            Long minDate,
            Long maxDate,
            Double minScore) throws AciServiceException, XPathExpressionException;

    Voronoi.Graph usergraph(
            String query,
            int maxUsers,
            Double minScore) throws AciServiceException, XPathExpressionException;

    List<Voronoi.Profile> userSuggestions(String user, boolean summary, Double minScore) throws XPathExpressionException;

    List<Voronoi.TermMeta> getTermsForProfiles(List<String> profileDocIds, int maxTerms) throws XPathExpressionException;

    Map<String,Object> getContent(
            String reference,
            int engine
    ) throws AciServiceException, XPathExpressionException;

    Map<String,Object> suggest(
            int pageNum,
            int pageSize,
            String[] reference,
            String[] exclude,
            boolean summary,
            boolean totalResults,
            int engine,
            Long minDate,
            Long maxDate,
            Double minScore
    ) throws AciServiceException, XPathExpressionException;

    Map<String,Object> query(
            int pageNum,
            int pageSize,
            String[] exclude,
            String[] query,
            boolean summary,
            boolean totalResults,
            int engine,
            Long minDate,
            Long maxDate,
            boolean matchallterms,
            Double minScore
    ) throws AciServiceException, XPathExpressionException;

    String findUnstemmedTerm(int engine, String stemmedTerm);

    List<QuerySummaryElement> querysummary(
            String query,
            int maxResults,
            int engine,
            Long minDate,
            Long maxDate,
            Double minScore);

    Voronoi.Cluster clusters(
            String query,
            int childLimit,
            int maxResults,
            boolean clusterSentiment,
            int engine,
            boolean clusterDedupe,
            Long minDate,
            Long maxDate,
            boolean useDocIds,
            Double minScore
    ) throws AciServiceException, XPathExpressionException;
}
