package com.autonomy.find.services.search;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.autonomy.aci.actions.idol.tag.Field;
import com.autonomy.aci.client.services.AciServiceException;
import com.autonomy.aci.content.fieldtext.FieldText;
import com.autonomy.find.dto.SearchRequestData;
import com.autonomy.find.fields.ParaField;
import com.autonomy.find.services.SearchService;



public class ParametricValuesSearchExecutor implements Callable<SearchResult<List<Field>>> {

    @Value("${discoverSearch.parametric.singleQueryField}")
    private int retries;

    private SearchRequestData requestData;
    private int maxValues;
    private List<String> filteredFieldNames;
    private FieldText fieldText;
    private boolean restrictedvalues;
    private boolean rename;
    private String sort;
    private boolean isAggregated;
    private String aggregatedFieldname;
    private SearchService searchService;
    private int requestId;
    private Map<String, ParaField> paraFieldMap;

    private static final Logger LOGGER = LoggerFactory.getLogger(ParametricValuesSearchExecutor.class);

    public ParametricValuesSearchExecutor(  final int requestId,
                            final SearchRequestData requestData,
                            final int maxValues,
                            final List<String> filteredFieldNames,
                            final FieldText fieldText,
                            final boolean restrictedvalues,
                            final boolean rename,
                            final String sort,
                            final boolean isAggregated,
                            final String aggregatedFieldname,
                            final SearchService searchService,
                            final Map<String, ParaField> paraFieldMap
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

    }


    @Override
    public SearchResult<List<Field>> call()throws Exception {
        int attempts = 0;
        while (attempts <= retries) {
            attempts++;
            try {
                final List<Field> fields = searchService.parametricValuesForFilteredSearch(requestData, maxValues, filteredFieldNames, fieldText, restrictedvalues, rename, sort, isAggregated, aggregatedFieldname, paraFieldMap);
                return new SearchResult<List<Field>>(this.requestId, fields);

            } catch (final AciServiceException e) {
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

