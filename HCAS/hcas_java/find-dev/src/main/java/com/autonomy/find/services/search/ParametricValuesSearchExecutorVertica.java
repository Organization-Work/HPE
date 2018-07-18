package com.autonomy.find.services.search;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.autonomy.find.dto.SearchRequestData;
import com.autonomy.find.fields.ParaField;
import com.autonomy.find.services.SearchService;
import com.autonomy.find.util.RequestProcess;



public class ParametricValuesSearchExecutorVertica { 

    @Value("${discoverSearch.parametric.singleQueryField}")
    private int retries;

    private SearchRequestData requestData;
    private int maxValues;
    private List<String> filteredFieldNames;
    private Map fieldText;
    private boolean restrictedvalues;
    private boolean rename;
    private String sort;
    private boolean isAggregated;
    private String aggregatedFieldname;
    private SearchService searchService;
    private int requestId;
    private Map<String, ParaField> paraFieldMap;
    private RequestProcess process;

    private static final Logger LOGGER = LoggerFactory.getLogger(ParametricValuesSearchExecutor.class);

    public ParametricValuesSearchExecutorVertica(  final int requestId,
                            final SearchRequestData requestData,
                            final int maxValues,
                            final List<String> filteredFieldNames,
                            final Map fieldText,
                            final boolean restrictedvalues,
                            final boolean rename,
                            final String sort,
                            final boolean isAggregated,
                            final String aggregatedFieldname,
                            final SearchService searchService,
                            final Map<String, ParaField> paraFieldMap,
                            final RequestProcess process
    ) {
        this.requestId = requestId;
        this.requestData = requestData;
        this.maxValues = maxValues;
        this.filteredFieldNames = filteredFieldNames;
        this.fieldText = fieldText;
        this.restrictedvalues = restrictedvalues;
        this.rename = rename;
        this.sort = sort;
        this.isAggregated = isAggregated;
        this.aggregatedFieldname = aggregatedFieldname;
        this.searchService = searchService;
        this.paraFieldMap = paraFieldMap;
        this.process = process;

    }


    public SearchResult<List<com.autonomy.vertica.fields.Field>> call()throws Exception {
        int attempts = 0;
        while (attempts <= retries) {
            attempts++;
            try {
                final List<com.autonomy.vertica.fields.Field> fields = searchService.parametricValuesForFilteredSearchVertica(requestData, maxValues, filteredFieldNames, fieldText, restrictedvalues, rename, sort, isAggregated, aggregatedFieldname, paraFieldMap, process);
                return new SearchResult<List<com.autonomy.vertica.fields.Field>>(this.requestId, fields);

            } catch (final Exception e) {
                if (attempts > retries) {
                    throw e;
                } else {
                    LOGGER.error("ParametricValuesForFilteredSearch error, will retry [" + attempts + "]", e);
                }
            }
        }

        return null;
    }
}

