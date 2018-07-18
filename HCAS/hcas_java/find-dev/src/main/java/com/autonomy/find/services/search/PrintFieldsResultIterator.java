package com.autonomy.find.services.search;

import com.autonomy.aci.actions.idol.query.QueryResponse;
import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.find.config.SearchConfig;
import com.autonomy.find.processors.HierarchicalQueryResponseProcessor;
import com.autonomy.find.util.StateResult;

public class PrintFieldsResultIterator extends StoreStateResultsIterator<QueryResponse> {
    private final String printFields;

    private final SearchConfig searchConfig;
    private final String searchView;

    public PrintFieldsResultIterator(final StateResult stateResult,
                                     final AciService searchService,
                                     final SearchConfig searchConfig,
                                     final String searchView,
                                     final String printFields,
                                     final String securityInfo) {
        super(stateResult, searchService, securityInfo);

        this.searchConfig = searchConfig;
        this.searchView = searchView;
        this.printFields = printFields;
    }


    @Override
    public QueryResponse next() {
        final AciParameters params = new AciParameters("query");
        params.add("text", "*");
        params.add("languagetype", searchConfig.getLanguageType());
        params.add("outputencoding", searchConfig.getOutputEncoding());
        params.add("databasematch", searchConfig.getDatabase(searchView));

        if (setNextQueryParams(params)) {
            params.add("print", "fields");
            params.add("printfields", printFields);

            getLogger().debug("Printing fields from storedState [" + params.get("statematchid") + "].");

            return executeQuery(params, new HierarchicalQueryResponseProcessor());

        }

        return null;
    }

}
