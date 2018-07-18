package com.autonomy.find.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.autonomy.aci.actions.idol.tag.Field;
import com.autonomy.aci.actions.idol.tag.FieldListProcessor;
import com.autonomy.aci.actions.idol.tag.FieldValue;
import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.find.config.ParametricConfig;
import com.autonomy.find.config.TaxonomyConfig;
import com.autonomy.find.dto.SearchRequestData;
import com.autonomy.find.util.FieldTextDetail;
import com.autonomy.find.util.FieldTextUtil;
import com.autonomy.find.util.Queries;
import com.googlecode.ehcache.annotations.Cacheable;
import com.googlecode.ehcache.annotations.DecoratedCacheType;

public class TaxonomyDocCountService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaxonomyDocCountService.class);

    private static final String
            ACTION_GET_QUERY_TAG_VALUES = "GetQueryTagValues",
            PARAM_FIELD_NAME = "FieldName",
            PARAM_TEXT = "text",
            PARAM_DATABASE_MATCH = "databasematch",
            PARAM_MINSCORE = "minscore",
            PARAM_FIELD_TEXT = "FieldText",
            PARAM_DOCUMENT_COUNT = "DocumentCount",
            PARAM_SORT = "Sort",
            VALUE_DOCUMENT_COUNT = "ReverseDocumentCount";

    @Autowired
    private TaxonomyConfig taxonomyConfig;

    @Autowired
    private ParametricConfig parametricConfig;

    @Autowired
    private ParametricService parametricService;

    @Autowired
    @Qualifier("searchAciService")
    private AciService contentAci;


    /**
     * GetCategoryDocCounts
     *
     * @param requestData - Request Data
     * @return Map Id Count
     */
    @Cacheable(cacheName = "getCategoryDocCounts", decoratedCacheType = DecoratedCacheType.SELF_POPULATING_CACHE)
    public Map<String, Integer> getCategoryDocCounts(
            final SearchRequestData requestData
    ) {
        final AciParameters params = paramsForDocCounts(null, requestData);
        final List<Field> fields = contentAci.executeAction(params, new FieldListProcessor());
        final List<FieldValue> values = fields.get(0).getFieldValues();
        return fields.isEmpty()
                ? new HashMap<String, Integer>()
                : fieldValuesToDocCountMap(values);
    }


    /**
     * Builds a map from a list of field values.
     *
     * @param fields - List of field values to generate a map from
     * @return Map from field name to field count
     */
    public Map<String, Integer> fieldValuesToDocCountMap(final List<FieldValue> fields) {
        final Map<String, Integer> result = new HashMap<>();
        for (final FieldValue field : fields) {
            result.put(field.getValue(), field.getCount());
        }
        return result;
    }


    /**
     * Generates a set of aci parameters for retrieving document count data.
     *
     * @param params
     * @param requestData
     * @return
     */
    public AciParameters paramsForDocCounts(
            AciParameters params,
            final SearchRequestData requestData
    ) {
        if (params == null) {
            params = new AciParameters(ACTION_GET_QUERY_TAG_VALUES);
        }

        params.add(PARAM_FIELD_NAME, taxonomyConfig.getFieldName());

        final Map<String, FieldTextDetail> fieldNames = null; //TODO: FIXMEME: parametricService.getParaFieldNames(requestData.getSearchView());
        params.add(PARAM_FIELD_TEXT, FieldTextUtil.buildFilterExpression(requestData.getFilters(), fieldNames));

        params.add(PARAM_TEXT,  Queries.buildQueryWithExtension(requestData));

        if (taxonomyConfig.getDatabase() != null) {
            params.add(PARAM_DATABASE_MATCH, taxonomyConfig.getDatabase());
        }

        if (requestData.getMinScore() != null) {
            params.add(PARAM_MINSCORE, requestData.getMinScore());
        }

        params.add(PARAM_SORT, VALUE_DOCUMENT_COUNT);
        params.add(PARAM_DOCUMENT_COUNT, true);

        return params;
    }
}
