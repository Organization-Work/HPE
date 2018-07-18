package com.autonomy.find.services.search;

import com.autonomy.aci.client.services.AciConstants;
import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.find.config.SearchConfig;
import com.autonomy.find.processors.ReferenceProcessor;
import com.autonomy.find.util.StateResult;

import java.util.Set;

public class ReferenceResultsIterator extends StoreStateResultsIterator<Set<String>>{
    private final SearchConfig searchConfig;
    private final String searchView;

    private final String docRefField;

    public ReferenceResultsIterator(final StateResult stateResult,
                                    final AciService searchService,
                                    final SearchConfig searchConfig,
                                    final String searchView,
                                    final String securityInfo) {
        super(stateResult, searchService, securityInfo);
        this.searchConfig = searchConfig;
        this.searchView = searchView;

        this.docRefField = searchConfig.getSearchViews().get(searchView).getDocRefField();
    }

    @Override
    public Set<String> next() {


        final AciParameters params = new AciParameters("query");
        params.add("text", "*");
        params.add("databasematch", searchConfig.getDatabase(searchView));
        params.add("fieldname", docRefField);
        params.add("print", "none");

        if (setNextQueryParams(params)) {
            getLogger().debug("Retrieving references from storedState [" + params.get("statematchid") + "].");

            return executeQuery(params, new ReferenceProcessor());

        }

        return null;
    }

}
