package com.autonomy.find.services;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.autonomy.find.processors.HierarchicalQueryResponseProcessor;
import com.googlecode.ehcache.annotations.Cacheable;
import com.googlecode.ehcache.annotations.DecoratedCacheType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.autonomy.aci.actions.idol.query.QueryResponse;
import com.autonomy.aci.actions.idol.query.QuerySummaryElement;
import com.autonomy.aci.actions.idol.tag.Field;
import com.autonomy.aci.actions.idol.tag.FieldListProcessor;
import com.autonomy.aci.actions.idol.tag.FieldValue;
import com.autonomy.aci.client.annotations.IdolAnnotationsProcessorFactory;
import com.autonomy.aci.client.services.AciConstants;
import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.aci.content.database.Databases;
import com.autonomy.find.config.HistoryConfig;
import com.autonomy.find.dto.SearchHistoryDetails;
import com.autonomy.find.idx.Idx;
import com.autonomy.nonaci.indexing.IndexingService;
import com.autonomy.nonaci.indexing.impl.DreAddDataCommand;

@Service
public class HistoryService {

    private static final int DEFAULT_CONCEPT_SEARCH_DEPTH = 100;

    @Autowired
    @Qualifier("historyAciService")
    private AciService history;

    @Autowired
    @Qualifier("historyIndexingService")
    private IndexingService historyIndexing;

    @Autowired
    private HistoryConfig historyConfig;

    @Autowired
    private IdolAnnotationsProcessorFactory processorFactory;

    final private long AUTO_REFRESH_INTERVAL = 600000L; // 10 minutes

    public List<SearchHistoryDetails> getRecentSearchesForUser(final String userId, final int numSearches) {

        final AciParameters params = new AciParameters();
        params.put(AciConstants.PARAM_ACTION, "Query");
        params.put("DatabaseMatch", new Databases(historyConfig.getDatabase()));
        params.put("FieldText", "MATCH{" + userId + "}:" + SearchHistoryDetails.IDX_USERNAME);
        params.put("Print", "All");
        params.put("Sort", "DocIDDecreasing");
        params.put("MaxResults", numSearches);

        return history.executeAction(params, processorFactory.listProcessorForClass(SearchHistoryDetails.class));
    }

    /**
     * Receives a list of a user's recent search terms
     *
     * @param userId
     * @param numSearches
     * @return
     */
    public List<String> getRecentSearchesForUserAsStrings(final String userId, final int numSearches) {
        final List<SearchHistoryDetails> searches = getRecentSearchesForUser(userId, numSearches);
        final List<String> results = new LinkedList<String>();
        for (final SearchHistoryDetails search : searches) {
            results.add(search.getValue());
        }
        return results;
    }

    /**
     * Relies on SearchText being a parametric field in the Content engine.
     * Produces less useful results than getTopSearchConcepts
     *
     * @param numSearches
     * @return
     */
    @Deprecated
    public List<FieldValue> getTopSearches(final int numSearches) {

        final AciParameters params = new AciParameters();
        params.put(AciConstants.PARAM_ACTION, "GetQueryTagValues");
        params.put("DatabaseMatch", new Databases(historyConfig.getDatabase()));
        params.put("FieldName", SearchHistoryDetails.IDX_SEARCH);
        params.put("Text", "*");
        params.put("Print", "All");
        params.put("DocumentCount", true);
        params.put("Sort", "DocumentCount");
        params.put("MaxValues", numSearches);

        final List<Field> fullResults = history.executeAction(params, new FieldListProcessor());
        if (!fullResults.isEmpty()) {
            return fullResults.get(0).getFieldValues();
        } else {
            return new ArrayList<FieldValue>();
        }
    }

    @SuppressWarnings("unchecked")
    @Cacheable(cacheName = "HistoryService.top_search_concepts",
            decoratedCacheType = DecoratedCacheType.REFRESHING_SELF_POPULATING_CACHE,
            refreshInterval = AUTO_REFRESH_INTERVAL
    )
    public List<QuerySummaryElement> getTopSearchConcepts(final int numSearches) {

        final AciParameters params = new AciParameters();
        params.put(AciConstants.PARAM_ACTION, "Query");
        params.put("DatabaseMatch", new Databases(historyConfig.getDatabase()));
        params.put("Text", "*");
        params.put("Print", "NoResults");
        params.put("MaxResults", DEFAULT_CONCEPT_SEARCH_DEPTH);
        params.put("QuerySummary", true);

        final QueryResponse response = history.executeAction(params, new HierarchicalQueryResponseProcessor());
        final List<QuerySummaryElement> concepts = response.getAdvancedQuerySummary();

        if (concepts.size() > numSearches && numSearches > 0) {
            return concepts.subList(0, numSearches);
        } else {
            return concepts;
        }
    }

    public boolean saveSearch(final String user, final String search) {

        // Prevent the saving of blank strings
        if (search == null || search.trim().isEmpty()) {
            return false;
        }

        for (final String term : search.split("\\bAND\\b")) {
            final DreAddDataCommand command = new DreAddDataCommand();

            command.setDreDbName(historyConfig.getDatabase());
            command.setPostData(Idx.Render((new SearchHistoryDetails(term.trim(), user)).asIdx()));

            historyIndexing.executeCommand(command);
        }
        return true;
    }

    public List<String> getSearchTerms(final List<SearchHistoryDetails> searchDetails) {
        final List<String> searches = new ArrayList<String>();
        for (final SearchHistoryDetails term : searchDetails) {
            searches.add(term.getValue());
        }
        return searches;
    }

}
