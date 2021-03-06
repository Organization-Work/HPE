package com.autonomy.find.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.autonomy.aci.client.annotations.IdolAnnotationsProcessorFactory;
import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.find.config.ParametricConfig;
import com.autonomy.find.config.SearchConfig;
import com.autonomy.find.config.SearchView;
import com.autonomy.find.dto.Parametric.FieldValueData;
import com.autonomy.find.fields.FilterField;
import com.autonomy.find.fields.ParaField;
import com.autonomy.find.util.CollUtils;
import com.autonomy.find.util.FieldUtil;
import com.autonomy.vertica.common.Group;
import com.autonomy.vertica.service.SearchVerticaService;
import com.googlecode.ehcache.annotations.Cacheable;
import com.googlecode.ehcache.annotations.DecoratedCacheType;

public class ParametricService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParametricService.class);

    private static final String ACTION_GET_FIELD_VALUES = "GetQueryTagValues";
    private static final String PARAM_FIELD_NAME = "FieldName";
    private static final String PARAM_RANGES_NAME = "Ranges";
    private static final String PARAM_TEXT = "Text";
    private static final String DATABASE_MATCH="DatabaseMatch";
    private static final String VALUE_STAR = "*";


    public static final String AGGREGATED_FIELD_KEY = "aggregated";
    public static final String NON_AGGREGRATED_FIELD_KEY = "nonaggregated";


    @Autowired
    private AciService searchAciService;

    @Autowired
    private IdolAnnotationsProcessorFactory processorFactory;

	@Autowired
 	private ParametricConfig parametricConfig;

    @Autowired
    private SearchConfig searchConfig;

    @Autowired
    private ExecutorService searchExecutorService;

    @Value("${discoverSearch.filters.schema}")
    private String filtersSchema;
    
    @Autowired
    private SearchVerticaService verticaService;


    /**
     * Retrieves the parametric field values given a list of field names.
     *
     * @param fieldNames
     * @return
     */
    @Cacheable(cacheName="Parametric.getParaFieldValues_fieldNames", decoratedCacheType = DecoratedCacheType.SELF_POPULATING_CACHE)
    public Map<String, FieldValueData> getParaFieldValues(
            final Map<String, FilterField> fieldNames,
            final String viewName,
            final boolean isAggregate,
            final String aggregateFieldname,
            final String securityInfo
    ) {
        final String database = searchConfig.getDatabase(viewName);
        
        
       
        
        final AciParameters params = parametersForFieldValues(
                new AciParameters(ACTION_GET_FIELD_VALUES), fieldNames, database, viewName);
        if (isAggregate) {
            params.add("merge", true);
        }

        if (StringUtils.isNotBlank(securityInfo)) {
            params.add("securityinfo", securityInfo);
        }
        
        List<FieldValueData> fieldValues = new ArrayList<FieldValueData>();
        
        if(searchConfig.getSearchViews().get(viewName).getRepository().equalsIgnoreCase("IDOL")) {
        	fieldValues = searchAciService.executeAction(
                    params, processorFactory.listProcessorForClass(FieldValueData.class));
        } else {
        	fieldValues = verticaService.getParametricDataValues(params, fieldNames);
        }

        

        if (isAggregate && fieldValues.size() > 0) {
            // should only be one value for merge=true
            fieldValues.get(0).setName(aggregateFieldname);
        }
        final List<FieldValueData> viewFieldValues = new ArrayList<FieldValueData>();
        final String idolRootElement = searchConfig.getSearchViews().get(viewName).getIdolRootElement();
        final String viewFieldNamePrefix = FieldUtil.getSearchViewFieldnamePrefix(idolRootElement);

        for (final FieldValueData field : fieldValues) {
            final String testFieldName = field.getName().replaceFirst(viewFieldNamePrefix, "");
            final FilterField filterField = fieldNames.get(testFieldName);

            if (filterField != null) {
                //field.setName(testFieldName);
                field.setName(filterField.getName());
                field.setDisplayName(filterField.getDisplayName());
                viewFieldValues.add(field);
            }
        }
        final Map<String, FieldValueData> fieldValuesMap =  FieldValueData.collToMap(viewFieldValues);

        return fieldValuesMap;
    }

    public Map<String, FieldValueData> getSingleParaFieldValues(
            final String searchView,
            final String fieldname,
            final String securityInfo
    ) {
        final Map<String, FilterField> paraFields = getParaFieldNames(searchView);
        final FilterField filterField = paraFields.get(fieldname);

        if (filterField == null) {
            return Collections.<String, FieldValueData>emptyMap();
        }

        final ExecutorCompletionService<Map<String, FieldValueData>> ecs = new ExecutorCompletionService<Map<String, FieldValueData>>(searchExecutorService);
        ecs.submit(new ParametricSearchExecutor(Collections.<String, FilterField>singletonMap(fieldname, filterField), searchView, filterField.isAggregated(), fieldname, this, securityInfo));

        try {
            return ecs.take().get();

        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

    }
        /**
         * Retrieves the parametric field values given the location
         * of a json file containing a list of field names
         *
         * @param searchView
         * @return
         */
    @Cacheable(cacheName="Parametric.getParaFieldValues_fileName", decoratedCacheType = DecoratedCacheType.SELF_POPULATING_CACHE)
    public Map<String, FieldValueData> getParaFieldValues(
            final String searchView,
            final boolean singleQueryField,
            final String securityInfo
    ) {
        final Map<String, Map<String, FilterField>> paraFieldsMap = getCategorizedParaFieldNames(searchView);
        
        Map<String, FieldValueData> paraValues = new HashMap<String, FieldValueData>();       
        
        if(searchConfig.getSearchViews().get(searchView).getRepository().equalsIgnoreCase("IDOL")) {

	        final ExecutorCompletionService<Map<String, FieldValueData>> ecs = new ExecutorCompletionService<Map<String, FieldValueData>>(searchExecutorService);        
	
	        int jobs = 0;
	
	        if (!paraFieldsMap.get(NON_AGGREGRATED_FIELD_KEY).isEmpty()) {
	            if (!singleQueryField) {
	                ecs.submit(new ParametricSearchExecutor(paraFieldsMap.get(NON_AGGREGRATED_FIELD_KEY), searchView, false, null, this, securityInfo));
	                jobs++;
	            } else {
	                for (Map.Entry<String, FilterField> entry : paraFieldsMap.get(NON_AGGREGRATED_FIELD_KEY).entrySet()) {
	                    ecs.submit(new ParametricSearchExecutor(Collections.<String, FilterField>singletonMap(entry.getKey(), entry.getValue()), searchView, false, null, this, securityInfo));
	                    jobs++;
	                }
	
	            }
	        }
	
	        if (!paraFieldsMap.get(AGGREGATED_FIELD_KEY).isEmpty()) {
	            for (Map.Entry<String, FilterField> entry : paraFieldsMap.get(AGGREGATED_FIELD_KEY).entrySet()) {
	                ecs.submit(new ParametricSearchExecutor(Collections.singletonMap(entry.getKey(), entry.getValue()), searchView, true, entry.getKey(), this, securityInfo));
	                jobs++;
	            }
	        }
	
	        try {
	            for (int i = 0; i < jobs; i++) {
	                final Map<String, FieldValueData> result = ecs.take().get();
	                if (result != null) {
	                    paraValues.putAll(result);
	                }
	            }
	        } catch (Exception e) {
	            throw new RuntimeException(e);
	        }
        } else {
        	paraValues = getParaFieldValues(paraFieldsMap.get(NON_AGGREGRATED_FIELD_KEY), searchView, false, null, securityInfo);
        }
        return paraValues;
    }
    


    /**
     * Retrieves the parametric field values given the location
     * of a json file containing a list of field names
     *
     * @param searchView
     * @return
     */
@Cacheable(cacheName="Parametric.getDateParaFieldValues_fileName", decoratedCacheType = DecoratedCacheType.SELF_POPULATING_CACHE)
public Map<String, FieldValueData> getDateParaFieldValues(
        final String searchView,
        final String field,
        final String datePeriod, 
        final String dateOffset,
        final boolean singleQueryField,
        final String securityInfo
) {
    final Map<String, Map<String, FilterField>> paraFieldsMap = getCategorizedParaFieldNames(searchView);

    final ExecutorCompletionService<Map<String, FieldValueData>> ecs = new ExecutorCompletionService<Map<String, FieldValueData>>(searchExecutorService);



    final Map<String, FieldValueData> paraValues = new HashMap<String, FieldValueData>();

    int jobs = 0;

    if (!paraFieldsMap.get(NON_AGGREGRATED_FIELD_KEY).isEmpty()) {
        if (!singleQueryField) {
            ecs.submit(new ParametricSearchExecutor(paraFieldsMap.get(NON_AGGREGRATED_FIELD_KEY), searchView, false, null, this, securityInfo));
            jobs++;
        } else {
            for (Map.Entry<String, FilterField> entry : paraFieldsMap.get(NON_AGGREGRATED_FIELD_KEY).entrySet()) {
                ecs.submit(new ParametricSearchExecutor(Collections.<String, FilterField>singletonMap(entry.getKey(), entry.getValue()), searchView, false, null, this, securityInfo));
                jobs++;
            }

        }
    }

    if (!paraFieldsMap.get(AGGREGATED_FIELD_KEY).isEmpty()) {
        for (Map.Entry<String, FilterField> entry : paraFieldsMap.get(AGGREGATED_FIELD_KEY).entrySet()) {
            ecs.submit(new ParametricSearchExecutor(Collections.singletonMap(entry.getKey(), entry.getValue()), searchView, true, entry.getKey(), this, securityInfo));
            jobs++;
        }
    }

    try {
        for (int i = 0; i < jobs; i++) {
            final Map<String, FieldValueData> result = ecs.take().get();
            if (result != null) {
                paraValues.putAll(result);
            }
        }
    } catch (Exception e) {
        throw new RuntimeException(e);
    }

    return paraValues;
}
    /**
     * Retrieve a list of field names for the search view.
     *
     * @param viewName
     * @return
     */
    @Cacheable(cacheName="Parametric.getParaFieldNames_viewName", decoratedCacheType = DecoratedCacheType.SELF_POPULATING_CACHE)
    public Map<String, FilterField> getParaFieldNames(final String viewName) {
    	boolean parametric = true; 
    	if(searchConfig.getSearchViews().get(viewName).getRepository().equalsIgnoreCase("Vertica")) {
    		parametric = false;
    	}
    	return getFieldNames(viewName, parametric);
    }

    @Cacheable(cacheName="Parametric.getFilterFieldNames_viewName", decoratedCacheType = DecoratedCacheType.SELF_POPULATING_CACHE)
    public Map<String, FilterField> getFilterFieldNames(final String viewName) {
        return getFieldNames(viewName, false);
    }

    @Cacheable(cacheName="Parametric.getCategorizedParaFieldNames_viewName", decoratedCacheType = DecoratedCacheType.SELF_POPULATING_CACHE)
    public Map<String,Map<String, FilterField>> getCategorizedParaFieldNames(final String viewName) {
        final Map<String, FilterField> paraFields = getParaFieldNames(viewName);
        final Map<String, FilterField> aggregatedParaFields = new HashMap<String, FilterField>();

        for(final Map.Entry<String, FilterField> entry : paraFields.entrySet()) {
            if (entry.getValue().isAggregated()) {
                aggregatedParaFields.put(entry.getKey(), entry.getValue());
            }
        }

        for (final String key : aggregatedParaFields.keySet()) {
            paraFields.remove(key);
        }

        final Map<String, Map<String, FilterField>> categorizedFields = new HashMap<String, Map<String, FilterField>>();
        categorizedFields.put(AGGREGATED_FIELD_KEY, aggregatedParaFields);
        categorizedFields.put(NON_AGGREGRATED_FIELD_KEY, paraFields);

        return categorizedFields;
    }


    private Map<String, FilterField> getFieldNames(final String viewName, final boolean isParametric) {
        final SearchView searchView =  searchConfig.getSearchViews().get(viewName);
        if (searchView == null) {
            throw new IllegalArgumentException("Unknown search view [" + searchView + "]");
        }

        final String fieldsFile = searchView.getFilterFieldsFile();
        if (fieldsFile == null) {
            throw new RuntimeException("Scope [" + viewName + "] has no filterFieldsFile defined.");
        }

        try {
            final List<FilterField> filterFields = FieldUtil.loadFilterFields(fieldsFile, filtersSchema);
            if (filterFields == null) {
                throw new Error("No filterField defiend in [" + fieldsFile + "].");
            }

            final Map<String, FilterField> fieldsMap = new HashMap<String, FilterField>();
            for(final FilterField field : filterFields) {
                String fieldName = field.getName();
                if(isParametric) {
                    if (field.getParametric() != null) {
                        fieldsMap.put(StringUtils.defaultString(field.getParametric().getName(), fieldName), field);
                    }
                } else {
                    fieldsMap.put(fieldName, field);
                }

            }

            return fieldsMap;

        } catch (final Exception e) {
            throw new Error(String.format("Error parsing filter fields configuration: %s", e.getMessage()), e);
        }
    }

    /**
     * Builds a set of aci parameters for retrieving field values.
     *
     * @param params
     * @param fieldNames
     * @return
     */
    private AciParameters parametersForFieldValues(
            final AciParameters params,
            final Map<String, FilterField> fieldNames,
            final String database,
            final String viewName
    ) {

		addFieldNames(params, fieldNames, viewName);
        params.add(PARAM_TEXT, VALUE_STAR);
        params.add(DATABASE_MATCH, database);
        return params;
    }

	private void addFieldNames(final AciParameters params, final Map<String, FilterField> fieldNames, final String viewName) {
		final List<String> fieldNameKeys = new LinkedList<>();
        final StringBuilder rangeBuilder = new StringBuilder();

		for (final FilterField field : fieldNames.values()) {
            final ParaField paraField = field.getParametric();
			if (paraField != null) {
                String paraFieldName = StringUtils.defaultString(field.getParametric().getName(), field.getName());
                if(searchConfig.getSearchViews().get(viewName).getRepository().equalsIgnoreCase("Vertica")) {
                	paraFieldName = field.getName();
                }
				fieldNameKeys.add(paraFieldName);

                if (StringUtils.isNotEmpty(paraField.getRanges())) {
                    buildParaRangesParam(rangeBuilder, paraField.getRanges(), paraFieldName);
                }

			}
		}

		final String fieldNameTags = CollUtils.intersperse("+", fieldNameKeys);
		params.add(PARAM_FIELD_NAME, fieldNameTags);

        if (rangeBuilder.length() > 0) {
            params.add(PARAM_RANGES_NAME, rangeBuilder.toString());
        }
	}

    public StringBuilder buildParaRangesParam(final StringBuilder rangesBuilder, final String paraRanges, final String fieldname) {
        if (fieldname == null || paraRanges == null) {
            throw new IllegalArgumentException("Missing fieldname or paraRanges parameter");
        }

        final String param = String.format("FIXED%1$s:%2$s", paraRanges, fieldname);
        if (rangesBuilder.length() > 0) {
            rangesBuilder.append("+");
        }

        rangesBuilder.append(param);

        return rangesBuilder;
    }


    private static class ParametricSearchExecutor implements Callable<Map<String, FieldValueData>>  {
        private String viewName;
        private boolean isAggregate;
        private Map<String, FilterField> fieldNames;
        private String aggregateFieldname;
        private ParametricService paraSearchService;
        private String securityInfo;

        public ParametricSearchExecutor(final Map<String, FilterField> fieldNames,
                                        final String viewName,
                                        final boolean isAggregate,
                                        final String aggregateFieldname,
                                        final ParametricService paraSearchService,
                                        final String securityInfo)
        {
            this.fieldNames = fieldNames;
            this.viewName = viewName;
            this.isAggregate = isAggregate;
            this.aggregateFieldname = aggregateFieldname;
            this.paraSearchService = paraSearchService;
            this.securityInfo = securityInfo;
        }


        @Override
        public Map<String, FieldValueData> call() throws Exception {
            return paraSearchService.getParaFieldValues(fieldNames, viewName, isAggregate, aggregateFieldname, securityInfo);
        }
    }
    
    
    
    

}
