package com.autonomy.vertica.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.find.api.database.SearchSettings;
import com.autonomy.find.config.SearchConfig;
import com.autonomy.find.dto.FieldPair;
import com.autonomy.find.dto.SearchRequestData;
import com.autonomy.find.dto.TrendingDataLayer;
import com.autonomy.find.dto.TrendingDataVal;
import com.autonomy.find.dto.TrendingTotalsData;
import com.autonomy.find.dto.Parametric.BoolOp;
import com.autonomy.find.dto.Parametric.FieldParams;
import com.autonomy.find.dto.Parametric.FieldValueData;
import com.autonomy.find.dto.Parametric.FilterGroup;
import com.autonomy.find.fields.FilterField;
import com.autonomy.find.fields.FilterOperator;
import com.autonomy.find.fields.FilterType;
import com.autonomy.find.fields.OverrideObject;
import com.autonomy.find.services.DocumentFolderService;
import com.autonomy.find.services.DocumentVerticaFolderService;
import com.autonomy.find.services.ParametricService;
import com.autonomy.find.services.admin.AdminService;
import com.autonomy.find.util.RequestProcess;
import com.autonomy.find.util.RequestState;
import com.autonomy.find.util.RequestTracker;
import com.autonomy.vertica.common.Group;
import com.autonomy.vertica.common.GroupingInfo;
import com.autonomy.vertica.common.JoinType;
import com.autonomy.vertica.common.QueryBuilder;
import com.autonomy.vertica.common.QueryBuilderNew;
import com.autonomy.vertica.common.QueryBuilderNew;
import com.autonomy.vertica.common.SelectBuilder;
import com.autonomy.vertica.common.StructuredDataXMLNew;
import com.autonomy.vertica.common.SubExpression;
import com.autonomy.vertica.fields.Field;
import com.autonomy.vertica.fields.FieldValue;
import com.autonomy.vertica.fields.IndexFile;
import com.autonomy.vertica.fields.QueryFields;
import com.autonomy.vertica.query.Document;
import com.autonomy.vertica.query.QueryResponse;
import com.autonomy.vertica.table.mapping.Table;
import com.autonomy.vertica.table.mapping.TableRelationList;
import com.autonomy.vertica.table.mapping.TableRelationObject;
import com.autonomy.vertica.templates.FilterTemplate;
import com.autonomy.vertica.util.VerticaUtil;

@Service
public class SearchVerticaService {
	private static final Logger LOGGER = LoggerFactory.getLogger(SearchVerticaService.class);
	
	@Autowired
    private SearchConfig config;
	
	@Autowired
	private ParametricService parametricService;
	
	@Autowired
    private FilterTemplate filterTemplate;
	
	@Autowired
	private DocumentFolderService documentFolderService;
	
	@Autowired
	private DocumentVerticaFolderService documentVerticaFolderService;
	
	@Autowired
	private ExecutorService searchExecutorService;
	
	@Value("${discoverSearch.mapping.table.schema}")
    private String mappingTableSchema;
	
	@Value("${discoverSearch.mapping.table.file}")
    private String mappingTableFile;
	
	@Value("${discoverSearch.vertica.date.format}")
	private String destDateFormat;
	
	@Value("${discoverSearch.table.relation.schema}")
    private String relationTableSchema;
	
	@Value("${discoverSearch.table.relation.file}")
    private String relationTableFile;
	
	@Autowired
	private RequestTracker requestTracker;
	
	
	
	
	public QueryResponse searchResultsDoc(AciParameters params, String searchView, SearchSettings searchSettings, FilterGroup filterGroup, boolean isRetrieveResultDocs
			, String sortFieldName, String sortOrder, final Set<String> roles) throws Exception {		
		String numHits = params.get("maxresults");
		String totalResults = params.get("totalresults");
		double totalHits = 0;	
		StringBuffer sql = new StringBuffer();
		boolean isRoleAllowedToViewSQL = false;
		
		// display sql to only specific roles
		List<String> allowedRoles = config.getSearchViews().get(searchView).getDisplayCohortUsersList();
		
		if(roles != null && !roles.isEmpty() && allowedRoles != null && !allowedRoles.isEmpty()) {
			for(String role : roles) {
				if(role != null  && allowedRoles.contains(role)) {
					isRoleAllowedToViewSQL = true;
					break;
				}
			}
		}
		
		
		if(totalResults != null && !totalResults.isEmpty() && totalResults.equalsIgnoreCase("true")) {
			totalHits = getTotalHitsVertica(searchView, filterGroup, searchSettings, sql);
		}	
		List<Document> documents = new ArrayList<Document>();
		if(isRetrieveResultDocs) {
			documents = getDocuments(params, searchView, searchSettings, filterGroup, sortFieldName, sortOrder);
		}
		return buildQueryResponse(Double.parseDouble(numHits), totalHits, documents, sql.toString(), isRoleAllowedToViewSQL);
	}

	private boolean isTotalCountDistinct(String searchView, SearchSettings searchSettings) {
		boolean isTotalCountDistinct = config.getSearchViews().get(searchView).isTotalCountDistinct();
		String resultViewName = getMeasurementVariableDescriptionVertica(searchSettings, searchView);
		if(StringUtils.isNotBlank(resultViewName)) {
			Map<String, Boolean> resultViewCountMap = config.getSearchViews().get(searchView).getResultViewCountDistinct();
			if(resultViewCountMap != null && !resultViewCountMap.isEmpty() && resultViewCountMap.get(resultViewName) != null) {
				isTotalCountDistinct =  resultViewCountMap.get(resultViewName).booleanValue();				
			}
		}
		return isTotalCountDistinct;		
	}
	 
	private double getTotalHitsVertica(String searchView, FilterGroup filterGroup, SearchSettings searchSettings, StringBuffer sql) throws Exception {
		SelectBuilder builder = null;
		Map<String, Object> paramMap = new HashMap<String, Object>();
		Set<String> listTables = new HashSet<String>();
		final Map<String, FilterField> filterFieldNames = parametricService.getFilterFieldNames(searchView);
		FilterField field = getMeasurementVariableFieldVertica(searchSettings, searchView);	
		if(field != null) {
			String fromTableName = field.getVertica().getTable().getFromTable();			
			listTables.add(fromTableName);				
			builder = new SelectBuilder(fromTableName, field.getVertica().getSchema());
			String columnName = QueryBuilderNew.getColumnNameForField(field, listTables, getMappingTable(), builder);	
			QueryBuilderNew.joinTables(fromTableName, QueryBuilderNew.getTableNameForMultipleColumn(field), listTables, getMappingTable(),  builder, null, null, false);
			builder.column(columnName, "", field.getVertica().getSelect().getSelectOperator().getAggregateOperator(), 
					isTotalCountDistinct(searchView, searchSettings), "");
			// Dimension tables
			String dimTableName = field.getVertica().getTable().getDimensionTable();
			QueryBuilderNew.joinTables(fromTableName, dimTableName, listTables, getMappingTable(), builder, field.getVertica().getTable().getMappingKey(), null, false);
			
			// default filter
			QueryBuilderNew.handleDefaultFilter(fromTableName, paramMap, listTables, getMappingTable(), builder);
			QueryBuilderNew.handleDefaultFilter( dimTableName, paramMap, listTables, getMappingTable(),  builder);
			
			// mandatory filter - for the anchor table and/or any join relations
			QueryBuilderNew.handleMandatoryFilter(fromTableName, paramMap, listTables, getMappingTable(), builder, filterFieldNames, filterGroup, false);
			
			// mandatory filter - for the inner subexpression
			handleMandatoryFilterForSubExpression(filterGroup, filterFieldNames);
			
			
			// user filter expression
			SubExpression subExp = QueryBuilderNew.buildFilterExpression(documentFolderService, documentVerticaFolderService, filterGroup, filterFieldNames, false, paramMap, 
					listTables, fromTableName, getMappingTable(), builder, config, null, searchView, destDateFormat, field, getTableRelations(),
					field.getVertica().getSchema(), 0, isTotalCountDistinct(searchView, searchSettings), false, null, filterGroup);
			
			// promote the subExp to the view level
			// get the  view join level
			String viewTable = fromTableName;			
			promoteToView(subExp, searchView, viewTable, field, builder, paramMap);
			
			/*String whereClause = QueryBuilderNew.buildFilterExpression(documentFolderService, filterGroup, filterFieldNames, false, paramMap, 
					listTables, fromTableName, getMappingTable(), builder, config, null, searchView, destDateFormat);
			if(whereClause != null) {
				builder.where(whereClause);
			}*/
		}
		if(builder == null || builder.toString().isEmpty()) {
			return 0;
		}
		sql.append(filterTemplate.formatSQL(builder.toString()));
	    return filterTemplate.getTotalHits(builder.toString(), paramMap);
	 }
	
	private QueryResponse buildQueryResponse(double numHits, double totalHits, List<Document> documents, String sql, boolean isRoleAllowedToViewSQL) {
		QueryResponse response = new QueryResponse();
		response.setNumHits(numHits);
		response.setTotalHits(totalHits);		
		response.setDocuments(documents);
		if(isRoleAllowedToViewSQL) {
			response.setSqlQuery(sql);
		}
		return response;
	}
	
	private List<Document> getDocuments(AciParameters params, String searchView, SearchSettings searchSettings, FilterGroup filterGroup, String sortFieldName
			, String sortOrder) throws Exception {
		final Map<String, FilterField> filterFieldNames = parametricService.getFilterFieldNames(searchView);
		
		String fieldNameStr = params.get("printfields");
		String[] fieldNames = StringUtils.split(fieldNameStr, ',');
		SelectBuilder query = null;		
		String fromTable = "";
		Set<String> tableNames = new HashSet<String>();
		Map<String, String> fieldColumnMap = new HashMap<String, String>();
		Map<String, Object> paramMap = new HashMap<String, Object>();
		String fromTableName = "";
		boolean useDistinct = false;
		boolean distinct = false;
		FilterField refField = null;
		String schema = null;
		
		if(StringUtils.isBlank(sortOrder)) {
			sortOrder = "asc";
		}
		
		// handle reference field first to get the from table for the select
		for(String fieldName : fieldNames) {
			// get the reference field			
			FilterField field = filterFieldNames.get(fieldName);
			if(this.isReferenceTypeField(field, searchView, searchSettings)) {
				refField = field;
				fromTable = field.getVertica().getTable().getFromTable();		
				if(query == null)  {
					schema = field.getVertica().getSchema();
					query = new SelectBuilder(fromTable, schema);
					fromTableName = fromTable;
					useDistinct = true; // only the first field one should have distinct if at all in a SELECT
				} else {
					if(!fromTable.equalsIgnoreCase(fromTableName)) { // different table so join
						QueryBuilderNew.joinTables(fromTableName, fromTable, tableNames, getMappingTable(), query, null, JoinType.LeftOuterJoin, true);
					}
				}	
				if(useDistinct) { // use distinct and order by for the first reference field only
					//distinct = field.getVertica().getSelect().isDistinct();
					distinct = config.getSearchViews().get(searchView).isResultsListDistinct();
					if(sortFieldName == null || (sortFieldName != null && sortFieldName.equalsIgnoreCase(field.getName()))) {						
						query.orderBy("reference_" + QueryBuilderNew.cleanParameterName(field.getName()) + " " + sortOrder);
					}					
				}				
				String columnName = QueryBuilderNew.getColumnNameForField(field, tableNames, getMappingTable(), query);
				QueryBuilderNew.joinTables(fromTableName, QueryBuilderNew.getTableNameForMultipleColumn(field), tableNames, getMappingTable(),  query, null, JoinType.LeftOuterJoin, true);
				query.column(columnName, "", distinct, "reference_" + QueryBuilderNew.cleanParameterName(field.getName()));
				if(config.getSearchViews().get(searchView).isResultsListGroupBy()) {
					query.groupBy("reference_" + QueryBuilderNew.cleanParameterName(field.getName()));
				}
				tableNames.add(fromTable);
				// dimension table
				String dimTableName = field.getVertica().getTable().getDimensionTable();
				QueryBuilderNew.joinTables(fromTable, dimTableName, tableNames, getMappingTable(), query, field.getVertica().getTable().getMappingKey(), JoinType.LeftOuterJoin, true);
				fieldColumnMap.put("reference_" + QueryBuilderNew.cleanParameterName(field.getName()), field.getName());
				useDistinct = false; // only one column in select query can be distinct
				distinct = false; // make sure only on distinct in the select, otherwise throws sql errors
				//break;
			} 
		}
		
		// all non reference fields
		for(String fieldName : fieldNames) {
			// get all the non reference fields
			String tableName = "";
			FilterField field = filterFieldNames.get(fieldName);
			if(!this.isReferenceTypeField(field, searchView, searchSettings)) {
				tableName = field.getVertica().getTable().getFromTable();
				if(query == null) { // reference field not found or set
					fromTable = tableName;
					schema = field.getVertica().getSchema();
					query = new SelectBuilder(tableName, schema);
					tableNames.add(tableName);
				}				
				String columnName = QueryBuilderNew.getColumnNameForField(field, tableNames, getMappingTable(), query);
				QueryBuilderNew.joinTables(fromTableName, QueryBuilderNew.getTableNameForMultipleColumn(field), tableNames, getMappingTable(),  query, null, JoinType.LeftOuterJoin, true);
				query.column(columnName, "", false, QueryBuilderNew.cleanParameterName(field.getName()));
				if(config.getSearchViews().get(searchView).isResultsListGroupBy()) {
					query.groupBy(QueryBuilderNew.cleanParameterName(field.getName()));
				}
				if(sortFieldName != null && sortFieldName.equalsIgnoreCase(field.getName())) {
					query.column(QueryBuilderNew.getActualColumnNameForField(field, tableNames,  getMappingTable(), query), "", false, QueryBuilderNew.cleanParameterName(field.getName() + "sort"));
					query.orderBy(QueryBuilderNew.cleanParameterName(field.getName()) + "sort" + " " + sortOrder);
					if(config.getSearchViews().get(searchView).isResultsListGroupBy()) {
						query.groupBy(QueryBuilderNew.cleanParameterName(field.getName() + "sort"));
					}
					fieldColumnMap.put(QueryBuilderNew.cleanParameterName(field.getName() + "sort"), field.getName() + "sort");
				}					
				fieldColumnMap.put(QueryBuilderNew.cleanParameterName(field.getName()), field.getName());				
				QueryBuilderNew.joinTables(fromTable, tableName, tableNames, getMappingTable(), query, null, null, false);
				// dimension table
				String dimTableName = field.getVertica().getTable().getDimensionTable();
				QueryBuilderNew.joinTables(tableName, dimTableName, tableNames, getMappingTable(), query, field.getVertica().getTable().getMappingKey(), JoinType.LeftOuterJoin, true);
			} 
		}		
		
		if(query != null ) {
			// default filter
			QueryBuilderNew.handleDefaultFilter(fromTableName, paramMap, tableNames, getMappingTable(), query);
			
			// mandatory filter - for the anchor table and/or any join relations
			QueryBuilderNew.handleMandatoryFilter(fromTableName, paramMap, tableNames, getMappingTable(), query, filterFieldNames, filterGroup, true);
			
			// mandatory filter - for the inner subexpression
			handleMandatoryFilterForSubExpression(filterGroup, filterFieldNames);		
			
			// user filter expression
			SubExpression subExp = QueryBuilderNew.buildFilterExpression(documentFolderService, documentVerticaFolderService, filterGroup, filterFieldNames, false, paramMap, 
					tableNames, fromTable, getMappingTable(), query, config, null, searchView, destDateFormat, refField, getTableRelations(), schema, 0
					, isTotalCountDistinct(searchView, searchSettings), false, null, filterGroup);

		
			// promote the subExp to the view level
			// get the  view join level
			String viewTable = fromTable;			
			promoteToView(subExp, searchView, viewTable, refField, query, paramMap);
			query.limit(params.get("maxresults"));	
			query.offset(params.get("start"));
		} else {
			return new ArrayList<Document>();
		}
		
		return filterTemplate.getResultDocuments(query.toString(), paramMap, fieldColumnMap);
	}
	

	 
	 /**
	  * This method returns the reference field (Encounter ID of the Patient) based on the result view
	 * @param requestData
	 * @return
	 */
	private FilterField getReferenceFieldVertica(SearchRequestData requestData) {
		final SearchSettings searchSettings = requestData.getUserSearchSettings();
        final String combine = searchSettings == null ? config.getCombine() : searchSettings.getCombine();	        
        FilterField field = null;
		final Map<String, FilterField> filterFieldNames = parametricService.getFilterFieldNames(requestData.getSearchView());
		for(Map.Entry<String, FilterField> entry : filterFieldNames.entrySet()) {			
			
			if(entry.getValue().isReferenceType() ) {
				field = entry.getValue();
				break;
			}
			/*if(combine.equalsIgnoreCase("simple") && entry.getValue().isReferenceType() 
					&& entry.getValue().getVertica().getResultView().equalsIgnoreCase(config.getSearchViews().get(requestData.getSearchView()).getResultView().getSimple())) {
				field = entry.getValue();
				break;
			} else if(combine.equalsIgnoreCase("fieldCheck") && (entry.getValue().isReferenceType() 
					&& entry.getValue().getVertica().getResultView().equalsIgnoreCase(config.getSearchViews().get(requestData.getSearchView()).getResultView().getFieldCheck()))) {
				field = entry.getValue();
				break;
			} else if(combine.equalsIgnoreCase("measurement3") && (entry.getValue().isReferenceType() 
					&& entry.getValue().getVertica().getResultView().equalsIgnoreCase(config.getSearchViews().get(requestData.getSearchView()).getResultView().getMeasurement3()))) {
				field = entry.getValue();
				break;
			} else if(combine.equalsIgnoreCase("measurement4") && (entry.getValue().isReferenceType() 
					&& entry.getValue().getVertica().getResultView().equalsIgnoreCase(config.getSearchViews().get(requestData.getSearchView()).getResultView().getMeasurement4()))) {
				field = entry.getValue();
				break;
			}	*/		
		}		
		return field; 
	 }
	
	 /**
	  * This method returns the measurement variable(Encounter ID of the Patient) based on the result view
	 * @param requestData
	 * @return
	 */
	private FilterField getMeasurementVariableFieldVertica(SearchSettings searchSettings, String searchView) {	
       final String combine = searchSettings == null ? config.getCombine() : searchSettings.getCombine();	        
       FilterField field = null;
		final Map<String, FilterField> filterFieldNames = parametricService.getFilterFieldNames(searchView);
		for(Map.Entry<String, FilterField> entry : filterFieldNames.entrySet()) {
			if(entry != null && entry.getValue() != null && entry.getValue().getVertica() != null && entry.getValue().getVertica().getResultView() != null) {			
				if(combine.equalsIgnoreCase("simple")  
						&& entry.getValue().getVertica().getResultView().equalsIgnoreCase(config.getSearchViews().get(searchView).getResultView().getSimple())) {
					field = entry.getValue();
					break;
				} else if(combine.equalsIgnoreCase("fieldCheck") && (
						entry.getValue().getVertica().getResultView().equalsIgnoreCase(config.getSearchViews().get(searchView).getResultView().getFieldCheck()))) {
					field = entry.getValue();
					break;
				} else if(combine.equalsIgnoreCase("measurement3") && (
						entry.getValue().getVertica().getResultView().equalsIgnoreCase(config.getSearchViews().get(searchView).getResultView().getMeasurement3()))) {
					field = entry.getValue();
					break;
				} else if(combine.equalsIgnoreCase("measurement4") && (
						entry.getValue().getVertica().getResultView().equalsIgnoreCase(config.getSearchViews().get(searchView).getResultView().getMeasurement4()))) {
					field = entry.getValue();
					break;
				}
			}					
		}
		return field; 
	 }
	 /**
	  * This method returns the measurement variable description (Encounter ID of the Patient) based on the result view
	 * @param requestData
	 * @return
	 */
	private String getMeasurementVariableDescriptionVertica(SearchSettings searchSettings, String searchView) {	
      final String combine = searchSettings == null ? config.getCombine() : searchSettings.getCombine();	        
      String desc="count";
      
      if(combine.equalsIgnoreCase("simple")) { 
			desc=config.getSearchViews().get(searchView).getResultView().getSimple();
		 } else if(combine.equalsIgnoreCase("fieldCheck")) {
			desc=config.getSearchViews().get(searchView).getResultView().getFieldCheck();
		} else if(combine.equalsIgnoreCase("measurement3")) {
			desc=config.getSearchViews().get(searchView).getResultView().getMeasurement3();			
		} else if(combine.equalsIgnoreCase("measurement4")) {
			desc=config.getSearchViews().get(searchView).getResultView().getMeasurement4();			
		}
		return desc; 
	}
	
	private FilterField getReferenceFieldForDocReference(SearchRequestData requestData) {		
        FilterField field = null;
		final Map<String, FilterField> filterFieldNames = parametricService.getFilterFieldNames(requestData.getSearchView());
		for(Map.Entry<String, FilterField> entry : filterFieldNames.entrySet()) {
			if(entry.getValue().isReferenceType()) {// reference field
				field = entry.getValue();
				break;
			}
		}
		return field; 
	 }
	
	private boolean isReferenceTypeField(FilterField filterField, String searchView, SearchSettings searchSettings) {		      
        boolean result = false; 	
		if(filterField.isReferenceType() ) {
			result = true;
			
		}
		/*final String combine = searchSettings == null ? config.getCombine() : searchSettings.getCombine();
	    	final Map<String, FilterField> filterFieldNames = parametricService.getFilterFieldNames(searchView);*/	
				
		/*if(combine.equalsIgnoreCase("simple") && filterField.isReferenceType() 
				&& filterField.getVertica()!= null && filterField.getVertica().getResultView() != null
				&& filterField.getVertica().getResultView().equalsIgnoreCase(config.getSearchViews().get(searchView).getResultView().getSimple())) {// reference field
			result = true;
		} else if (combine.equalsIgnoreCase("fieldCheck") && filterField.isReferenceType()
				&& filterField.getVertica()!= null && filterField.getVertica().getResultView() != null
				&& filterField.getVertica().getResultView().equalsIgnoreCase(config.getSearchViews().get(searchView).getResultView().getFieldCheck())) {
			result = true;				
		} else if (combine.equalsIgnoreCase("measurement3") && filterField.isReferenceType()
				&& filterField.getVertica()!= null && filterField.getVertica().getResultView() != null
				&& filterField.getVertica().getResultView().equalsIgnoreCase(config.getSearchViews().get(searchView).getResultView().getMeasurement3())) {
			result = true;				
		} else if (combine.equalsIgnoreCase("measurement4") && filterField.isReferenceType()
				&& filterField.getVertica()!= null && filterField.getVertica().getResultView() != null
				&& filterField.getVertica().getResultView().equalsIgnoreCase(config.getSearchViews().get(searchView).getResultView().getMeasurement4())) {
			result = true;				
		}*/
		return result; 
	 }	 
	
	 private Map<String, Table> getMappingTable() {      

        try {
            final List<Table> tables = VerticaUtil.loadTables(mappingTableFile, mappingTableSchema);
            if (tables == null) {
                throw new Error("No table mapping defiend in [" + mappingTableFile + "].");
            }

            final Map<String, Table> tablesMap = new HashMap<String, Table>();
            for(final Table table : tables) {
                String tableName = table.getTableName();
                tablesMap.put(tableName, table);

            }

            return tablesMap;

        } catch (final Exception e) {
            throw new Error(String.format("Error parsing filter fields configuration: %s", e.getMessage()), e);
        }
	 }
	 
	 private Map<String, List<TableRelationObject>> getTableRelations() {      

	        try {
	            final List<TableRelationList> relations = VerticaUtil.loadTableRelations(relationTableFile, relationTableSchema);
	            if (relations == null || relations.size() <= 0) {
	                throw new Error("No table mapping defiend in [" + relationTableFile + "].");
	            }

	            final Map<String, List<TableRelationObject>> tablesMap = new HashMap<String, List<TableRelationObject>>();
	            for(final TableRelationList tableRelation : relations) {
	                String view = tableRelation.getView();
	                tablesMap.put(view, tableRelation.getRelation());

	            }

	            return tablesMap;

	        } catch (final Exception e) {
	            throw new Error(String.format("Error parsing filter fields configuration: %s", e.getMessage()), e);
	        }
		 }
	 
	 public List<TableRelationObject> getTableRelationsByView(String view) {
		 try {
			 return this.getTableRelations().get(view);
	     } catch (final Exception e) {
	    	 throw new Error(String.format("Error parsing filter fields configuration: %s", e.getMessage()), e);
	     }
	 }
	 
	 public TableRelationObject getTableRelationByViewAndTable(String view, String tableName) {
		 try {
			 TableRelationObject found = null;
			 if(this.getTableRelations().get(view) == null) {
				 throw new Exception(String.format("No table relation for view %s", view));
			 }
			 for( TableRelationObject relation : this.getTableRelations().get(view)) {
				 if(relation != null && relation.getTable().equalsIgnoreCase(tableName)) {
					 found = relation;
				 }				 
			 }
			 if(found == null) {
				 LOGGER.debug("Relation not found for view and table <" + view + " - " + tableName + ">");
			 }
			 return found;
	     } catch (final Exception e) {
	    	 throw new Error(String.format("Error parsing filter fields configuration: %s", e.getMessage()), e);
	     }
	 }
	 public TableRelationObject getTableRelationByJoinLevel(String view , int joinLevel) throws Exception {
		 if(this.getTableRelations().get(view) == null) {
			 throw new Exception(String.format("No table relation for view %s", view));
		 }
    	for(  TableRelationObject relation : this.getTableRelations().get(view)) {
			 if(relation != null && relation.getJoinNumber() == joinLevel) {
				 return relation;
			 }				 
		}		    
		return null;	        
	 }
	 
	 
	/**
	 * Bar Charts - aggregates (counts) and returns all field values grouping by the filter fields to be displayed as horizontal bars in the bar chart tab in HCA UI.
	 * 
	 * <p> An example is to get all care units notes, the query that is executed in Vertica is,
	 *  <br><br><b>select count(d_careunits.label), d_careunits.label from mimic2v26.noteevents_new
	 *  <br>inner join mimic2v26.d_careunits  ON  noteevents_new.cuid  =  d_careunits.cuid 
	 *  <br>group by d_careunits.label limit 20</b>
	 *  
	 *  <br><br> the above query results in 
	 *  <br><br>
	 *  <table>
	 *  	<tr>
	 *  		<th>Value</th>
	 *  		<th>Count</th>
	 *  	</tr>
	 *  	<tr>
	 *  		<td>T-SICU</td>
	 *  		<td>71</td>
	 *  	</tr>
	 *  	<tr>
	 *  		<td>CSRU</td>
	 *  		<td>54</td>
	 *  	</tr>
	 *  	<tr>
	 *  		<td>MICU</td>
	 *  		<td>15</td>
	 *  	</tr>
	 *  	<tr>
	 *  		<td>FICU</td>
	 *  		<td>10</td>
	 *  	</tr>
	 *  	<tr>
	 *  		<td>NICU</td>
	 *  		<td>8</td>
	 *  	</tr>
	 *  	<tr>
	 *  		<td>CCU</td>
	 *  		<td>8</td>
	 *  	</tr>  
	 *  </table>
	 *  
	 *  
	 * @param params
	 * @param requestData
	 * @return List of field labels with counts
	 * @throws Exception 
	 */
	
	public List<Field> searchParametricFieldValues(AciParameters params, SearchRequestData requestData, RequestProcess process) throws Exception {
			
			long startTime = System.currentTimeMillis();
			final Map<String, FilterField> filterFieldNames = parametricService.getFilterFieldNames(requestData.getSearchView());
			String fieldNameStr = params.get("FieldName");
			String fromTable = "";
			
			
			FilterField refField = getReferenceFieldVertica(requestData);
			
			FilterField measurementVariableField = getMeasurementVariableFieldVertica(requestData.getUserSearchSettings(), requestData.getSearchView());
			
			List<Field> fields = new ArrayList<Field>();
			List<QueryFields> queries = new ArrayList<QueryFields>();
			
			String[] fieldNames = StringUtils.split(fieldNameStr, '+');
			if(fieldNames != null)  {
				// get the values for each field
				for(String fieldName : fieldNames) {
					Map<String, Object> paramMap = new HashMap<String, Object>();
					// new tab request has come in, so abort this one...
					if(requestTracker.getRequestState(process) == RequestState.STOP) {
						break;
					}
					
					SelectBuilder query = null;
					Set<String> tableNames = new HashSet<String>();
					FilterField field = filterFieldNames.get(fieldName);	
					boolean isFieldGrouped = false;
					String refTableName = "";
					boolean overrideResultView = false;
					
					if(field != null && field.getVertica() != null) {
						fromTable = field.getVertica().getTable().getFromTable();
						query = new SelectBuilder(fromTable, field.getVertica().getSchema());
						tableNames.add(fromTable);
						
						// Measuring field like Gender, Ethnicity etc.
						QueryBuilderNew.joinTables(fromTable, QueryBuilderNew.getTableNameForMultipleColumn(field), tableNames, getMappingTable(), query, null, null, false);
						String columnName = QueryBuilderNew.getColumnNameForField(field, tableNames, getMappingTable(), query);
						
						
						// Added this logic on 27-Nov-2017 for Ohio - start
						// if override result view option is set in the config then don't use result view for measurements rather use the actual field in question
						String selectedResultViewName = getMeasurementVariableDescriptionVertica(requestData.getUserSearchSettings(), requestData.getSearchView());
						String overrideColumnName = "";
						if(field.getVertica().getOverrideSection() != null 
								&& field.getVertica().getOverrideSection().getOverride() != null 
								&& !field.getVertica().getOverrideSection().getOverride().isEmpty()
								&& StringUtils.isNotBlank(selectedResultViewName)) {							
							for(OverrideObject override : field.getVertica().getOverrideSection().getOverride()) {
								if(override != null 
										&& StringUtils.isNotBlank(override.getResultViewName()) 
										&& override.getResultViewName().equalsIgnoreCase(selectedResultViewName)) {
									overrideResultView = true;
									overrideColumnName = override.getColumnName();
									break;
								}
							}
						}
						// 						
						// Added this logic on 27-Nov-2017 for Ohio - end
						
						// Added override condition on 27-Nov-2017 for Ohio
						// Measurement variable like Encounter, Patient, Avg. Cost or Length Of stay etc.
						String measurementColumnName = null;						
						if(!overrideResultView && measurementVariableField != null && measurementVariableField.getVertica() != null) {
							measurementColumnName = QueryBuilderNew.getColumnNameForField(measurementVariableField, tableNames, getMappingTable(), query);
							String measrementTableName = measurementVariableField.getVertica().getTable().getFromTable();							
							QueryBuilderNew.joinTables(fromTable, measrementTableName, tableNames, getMappingTable(), query, null, null, false);
							// Dimension tables
							String dimTableName = measurementVariableField.getVertica().getTable().getDimensionTable();
							QueryBuilderNew.joinTables(measrementTableName, dimTableName, tableNames, getMappingTable(), query, 
									measurementVariableField.getVertica().getTable().getMappingKey(), null, false);
						}
						if(measurementColumnName != null && !measurementColumnName.isEmpty()) {
							query.column(measurementColumnName, "", measurementVariableField.getVertica().getSelect().getSelectOperator().getAggregateOperator(),
									isTotalCountDistinct(requestData.getSearchView(), requestData.getUserSearchSettings()), false, "documentcount");
							query.groupBy(columnName);							
						}  else {
							if(overrideResultView) {
								if(StringUtils.isNotBlank(overrideColumnName)) {
									overrideColumnName = QueryBuilderNew.getColumnNameForField(field.getVertica().getTable().getDimensionTable(), fromTable, overrideColumnName, 
										field.getVertica().getSelect().getSelectOperator().getMiscOperator(), field.getVertica().getSelect().getSelectOperator().getSelectExpression());
									query.column(overrideColumnName, "", field.getVertica().getSelect().getSelectOperator().getAggregateOperator(), 
											false, false, "documentcount");
								} else {
									query.column(columnName, "", field.getVertica().getSelect().getSelectOperator().getAggregateOperator(), 
											false, false, "documentcount");
								}
								query.groupBy(columnName);
							} else {
								query.column(columnName, "", field.getVertica().getSelect().getSelectOperator().getAggregateOperator(), 
										false, true, "documentcount");
							}
														
						}
						
						
						// Range field needs case when statements to aggregate the results in groups
						if(QueryBuilderNew.isGroupField(field, config)) { 
							String caseWhenExpression = QueryBuilderNew.buildCaseWhenClauseForGroupField(field, config, tableNames, getMappingTable(), query);
							if(caseWhenExpression != null && !caseWhenExpression.isEmpty()) {
								String columnNameWithCaseWhen = columnName + ", " +  caseWhenExpression;
								query.column(columnNameWithCaseWhen, "", false, "fieldValue");	
								query.groupBy(caseWhenExpression);
								isFieldGrouped = true;
							}							
						} else {
							query.column(columnName, "", false, "fieldValue");	
						}
						
						// reference field / table join
						if(refField != null && refField.getVertica() != null) {
							refTableName = refField.getVertica().getTable().getFromTable();							
							//QueryBuilderNew.joinTables(fromTable, refTableName, tableNames, getMappingTable(), query);
						}
						
						// Dimension tables
						String dimTableName = field.getVertica().getTable().getDimensionTable();
						QueryBuilderNew.joinTables(fromTable, dimTableName, tableNames, getMappingTable(), query, field.getVertica().getTable().getMappingKey(), null, false);
						
						
						// default filter
						QueryBuilderNew.handleDefaultFilter(fromTable, paramMap, tableNames, getMappingTable(), query);
						
						FilterGroup filterGroup = requestData.getFilterGroup();
						// mandatory filter - for the anchor table and/or any join relations
						QueryBuilderNew.handleMandatoryFilter(fromTable, paramMap, tableNames, getMappingTable(), query, filterFieldNames, filterGroup, false);				
						
						// mandatory filter - for the inner subexpression
						handleMandatoryFilterForSubExpression(filterGroup, filterFieldNames);
						
						// Added on 19-Dec-17 as the mandatory filter can not handle this and default filter is no more used
						// default where clause in the outer query for fine-tuning the display in bar charts etc.
						if(StringUtils.isNotBlank(field.getVertica().getSelect().getOuterWhereExpression())) {
							query.where(field.getVertica().getSelect().getOuterWhereExpression());
						}
						
						// user filter expression
						SubExpression subExp = QueryBuilderNew.buildFilterExpression(documentFolderService, documentVerticaFolderService, filterGroup, filterFieldNames, false, 
								paramMap, tableNames, fromTable, getMappingTable(), query, config, null, requestData.getSearchView(), destDateFormat, field, 
								getTableRelations(), field.getVertica().getSchema(), 0, isTotalCountDistinct(requestData.getSearchView(), requestData.getUserSearchSettings()), false,
        				params.get("AutoComplete_FieldName"), filterGroup);
						
						// promote the subExp to the view level
						// get the  view join level
						String viewTable = fromTable;						
						promoteToView(subExp, requestData.getSearchView(), viewTable, field, query, paramMap);
						String whereClauseForAutocomplete = "";
						LOGGER.debug(params.get("AutoComplete_FieldName") + ":" + params.get("AutoComplete_FieldValue"));
						if(fieldName.equalsIgnoreCase(params.get("AutoComplete_FieldName"))	&& (params.get("AutoComplete_FieldValue") != null)) {
							FilterGroup group = requestData.getFilterGroup();
							if(group != null && group.getFilterFields() != null) {
								List<FieldParams> autoCompParams = group.getFilterFields().get(fieldName);
								if(autoCompParams != null) {
									for(FieldParams autoCompParam : autoCompParams) {
										if(autoCompParam != null) {
											autoCompParam.setOp(FilterOperator.CONTAINS);
											autoCompParam.setVal(params.get("AutoComplete_FieldValue"));
										}
									}
								} else {
									FieldParams newParams = new FieldParams();
									newParams.setType('P');
									newParams.setOp(FilterOperator.CONTAINS);
									newParams.setVal(params.get("AutoComplete_FieldValue"));
									List<FieldParams> newList = new ArrayList<FieldParams>();
									newList.add(newParams);
									group.getFilterFields().put(fieldName, newList);
								}
								whereClauseForAutocomplete = QueryBuilder.buildFilterExpression(documentFolderService, documentVerticaFolderService, requestData.getFilterGroup(), filterFieldNames, false, 
										paramMap, tableNames, fromTable, getMappingTable(), query, config, null, requestData.getSearchView(), destDateFormat);
							} else {
								boolean snomed = false;
								if(field.getName().equalsIgnoreCase(config.getSearchViews().get(requestData.getSearchView()).getSnomedTag())) {
									fromTable = requestData.getSearchView() + "_tags_freq";
									snomed = true;
								} else if(field.getName().equalsIgnoreCase(config.getSearchViews().get(requestData.getSearchView()).getSnomedParentTag())) {
									fromTable = requestData.getSearchView() + "_tags_freqP";
									snomed = true;
								}
								if(snomed) {
									query = new SelectBuilder(fromTable, field.getVertica().getSchema());
									query.column("documentcount", "", false, "documentcount");
									query.column("fieldValue", "", false, "fieldValue");
									String namedParameter = QueryBuilderNew.getNamedParameter("fieldValue", field.getName(), FilterOperator.CONTAINS.toString());
									namedParameter = QueryBuilderNew.cleanParameterName(namedParameter);
									namedParameter = namedParameter + QueryBuilderNew.cleanParameterName(params.get("AutoComplete_FieldValue"));
									whereClauseForAutocomplete = "( fieldValue" + " ilike (:" + namedParameter + "))";									
									paramMap.put(namedParameter, "%" + params.get("AutoComplete_FieldValue") + "%");
									
								} else {
									whereClauseForAutocomplete = QueryBuilderNew.buildTextForAutoComplete(fieldName,FilterOperator.CONTAINS,params.get("AutoComplete_FieldValue"),false, 
				 						paramMap, field,tableNames, fromTable,getMappingTable(), query, null);
								}
							}
															
						}
						/*if(whereClause != null) {
							StringBuffer where = new StringBuffer(whereClause);
							if(whereClauseForAutocomplete != null && !whereClauseForAutocomplete.isEmpty()) {
								where = new StringBuffer(whereClauseForAutocomplete);
								//where.append(whereClauseForAutocomplete);
							}
							query.where(where.toString());					
						} else {*/
							if(whereClauseForAutocomplete != null && !whereClauseForAutocomplete.isEmpty()) {
								query.where(whereClauseForAutocomplete);
							}						
						//}
						// added on 5/26 for document count fix - start
						if(params.get("sort") != null)  {
							if(params.get("sort").equalsIgnoreCase("documentcount")) {
								query.orderBy("documentcount desc"); 
							} else if(params.get("sort").equalsIgnoreCase("Alphabetical")) {
								query.orderBy("fieldValue"); 
							}
						}
						// added on 5/26 for document count fix - end
						
						// fix for range fields 4/25/2017 - start
						// 7-July-2017 : added a config setting to turn outer where clause on/off based on client requirement
						if(isFieldGrouped && config.getSearchViews().get(requestData.getSearchView()).isAddWhereForGroupFieldsToOuterQuery()) {
							String whereClauseForRange = QueryBuilder.buildFilterExpression(documentFolderService, documentVerticaFolderService, requestData.getFilterGroup(), filterFieldNames, false, 
									paramMap, tableNames, fromTable, getMappingTable(), query, config, null, requestData.getSearchView(), destDateFormat);
							if(StringUtils.isNotEmpty(whereClauseForRange)) {
								query.where(whereClauseForRange);
							}
						}
						// fix for range fields 4/25/2017 - end
						
						String queryString = query.toString();
						
						// Grouped (Range and Null check) fields need additional "sum" on the group fields
						if(isFieldGrouped) {							
							if(queryString != null) {
								StringBuffer newQueryBuff = new StringBuffer(" (");
								newQueryBuff.append(queryString);
								newQueryBuff.append(" ) x");								
								SelectBuilder newQuery = new SelectBuilder(newQueryBuff.toString());
								newQuery.column("documentcount", "", "sum", false, "documentcount");
								newQuery.column("fieldValue", "", false, true);
								queryString = newQuery.toString();
							}							
						}						
						//Field fieldPara = filterTemplate.getParametricField(queryString, paramMap, field.getName(), params.get("maxvalues"));
						//fields.add(fieldPara);
						QueryFields queryFields = new QueryFields();
						queryFields.setQueryString(queryString);
						queryFields.setParamMap(paramMap);
						queryFields.setFieldName(field.getName());
						queryFields.setMaxValue(params.get("maxvalues"));
						
						queries.add(queryFields);
					}				
				}// end all fields
				
				if(!queries.isEmpty()) {
					int jobs = 0;
					final ExecutorCompletionService<Field> ecs = new ExecutorCompletionService<Field>(searchExecutorService);
					if(config.isPreloadParaValues()) {
						for(QueryFields queryField : queries) {
							ecs.submit(new ParametricQueryExecutor(queryField.getQueryString(), queryField.getParamMap()
									, queryField.getFieldName(), filterTemplate, queryField.getMaxValue()));
							jobs++;
						}
						try {
				            for (int i = 0; i < jobs; i++) {
				                final Field result = ecs.take().get();
				                if (result != null) {
				                    fields.add(result);
				                }
				            }
					    } catch (Exception e) {
					    	throw new RuntimeException(e);
					    }
					} else {
						for(QueryFields queryField : queries) {
							// new tab request has come in, so abort this one...
							if(requestTracker.getRequestState(process) == RequestState.STOP) {
								break;
							}
							Field fieldPara = filterTemplate.getParametricField(queryField.getQueryString(), queryField.getParamMap()
									, queryField.getFieldName(), queryField.getMaxValue());
							fields.add(fieldPara);
						}
					}
				}
			}
			calculateElapsedTime(startTime, "searchFilteredResultFields");
			return fields;
		}
	
	 private void calculateElapsedTime(long startTime, String method) {
			long elapsedTime = System.currentTimeMillis() - startTime;
			LOGGER.debug(method + " Method execution time: " + elapsedTime);		
			//return elapsedTime;
	}

		
	/**
	 * AutoCompletes - Get all the unique values for the filter fields from Vertica for the autocomplete feature in the filters
	 * 
	 * <p>An example is to get all the unique care units, the query is 
	 * "select  distinct (d_careunits.label) from mimic2v26.noteevents_new inner join mimic2v26.d_careunits  ON  noteevents_new.cuid  =  d_careunits.cuid",
	 * resulting in the values CCU, CSRU, MICU etc....
	 * 
	 * @param params
	 * @param fieldNames
	 * @return
	 */
		public List<FieldValueData> getParametricDataValues(AciParameters params, Map<String, FilterField> fieldNames) {
			
			
			
			
			List<FieldValueData> listData = new ArrayList<FieldValueData>();
			String fieldNameStr = params.get("FieldName");
			String[] fieldNamesList = StringUtils.split(fieldNameStr, '+');
			if(fieldNamesList != null) {
				for(String fieldName : fieldNamesList) {
					
					FieldValueData data = new FieldValueData();					
					String fromTable = "";
					SelectBuilder query = null;
					Set<String> tableNames = new HashSet<String>();
					FilterField field = fieldNames.get(fieldName);
					
					data.setName(fieldName);
					data.setDisplayName(field.getDisplayName());

					Group group = config.getGroupFields().get(fieldName);
					//configured val
					if(group != null && field != null && group.getGroupType().equalsIgnoreCase("range")) {
						if(!group.getGroups().isEmpty()) {							
							for( GroupingInfo info : group.getGroups()) {
								if(info != null) {
									data.addValue(info.getGroupLabel());									
								}
							}							
						}						
					} else {
						if(field != null && field.getVertica() != null) {
							fromTable = field.getVertica().getTable().getFromTable();
							query = new SelectBuilder(fromTable, field.getVertica().getSchema());
							tableNames.add(fromTable);
							
							// columns						
							query.column(QueryBuilderNew.getColumnNameForField(field, tableNames, getMappingTable(), query), true);
							
							// Dimension tables
							String dimTableName = field.getVertica().getTable().getDimensionTable();							
							QueryBuilderNew.joinTables(fromTable, dimTableName, tableNames, getMappingTable(), query, field.getVertica().getTable().getMappingKey(), null, false);
							List<String> values = filterTemplate.listParametricFieldValues(query.toString(), new HashMap<String, Object>());
							data.setValues(values);
							data.setName(field.getName());						
						}
					}
					listData.add(data);
				}				
			}				
			return listData;			
		}
		
		/**
		 * Document Folder - return all document references
		 * 
		 * @param params
		 * @param requestData
		 * @return
		 * @throws Exception 
		 */
		public List<String> getDocumentFolderRefs(AciParameters params, SearchRequestData requestData, String filterFieldName) throws Exception {
			final Map<String, FilterField> filterFieldNames = parametricService.getFilterFieldNames(requestData.getSearchView());
			SelectBuilder builder = null;
			Map<String, Object> paramMap = new HashMap<String, Object>();
			Set<String> listTables = new HashSet<String>();
			
			//FilterField field = getReferenceFieldForDocReference(requestData);
			FilterField field = filterFieldNames.get(filterFieldName);
			if(field != null) {
				String fromTableName = field.getVertica().getTable().getFromTable();			
				listTables.add(fromTableName);				
				builder = new SelectBuilder(fromTableName, field.getVertica().getSchema());
				//builder.column(field.getVertica().getColumn(), fromTableName, "", true, "");
				// columns						
				builder.column(QueryBuilderNew.getColumnNameForField(field, listTables, getMappingTable(), builder), true);
				
				// Dimension tables
				String dimTableName = field.getVertica().getTable().getDimensionTable();							
				QueryBuilderNew.joinTables(fromTableName, dimTableName, listTables, getMappingTable(), builder, field.getVertica().getTable().getMappingKey(), null, false);
				
				// default filter
				QueryBuilderNew.handleDefaultFilter(fromTableName, paramMap, listTables, getMappingTable(), builder);
				
				FilterGroup filterGroup = requestData.getFilterGroup();
				// mandatory filter - for the anchor table and/or any join relations
				QueryBuilderNew.handleMandatoryFilter(fromTableName, paramMap, listTables, getMappingTable(), builder, filterFieldNames, filterGroup, false);
				
				// mandatory filter - for the inner subexpression
				handleMandatoryFilterForSubExpression(filterGroup, filterFieldNames);
				
				/*String whereClause = QueryBuilder.buildFilterExpression(documentFolderService, filterGroup, filterFieldNames, false, paramMap, 
						listTables, fromTableName, getMappingTable(), builder, config, null, requestData.getSearchView(), destDateFormat);
				if(whereClause != null) {
					builder.where(whereClause);
				}*/
				// user filter expression
				SubExpression subExp = QueryBuilderNew.buildFilterExpression(documentFolderService, documentVerticaFolderService, filterGroup, filterFieldNames, false, 
						paramMap, listTables, fromTableName, getMappingTable(), builder, config, null, requestData.getSearchView(), destDateFormat, field,
						getTableRelations(), field.getVertica().getSchema(), 0, isTotalCountDistinct(requestData.getSearchView(), requestData.getUserSearchSettings()), false,
						params.get("AutoComplete_FieldName"), filterGroup);
				
				// promote the subExp to the view level
				// get the  view join level
				String viewTable = fromTableName;						
				promoteToView(subExp, requestData.getSearchView(), viewTable, field, builder, paramMap);
				builder.limit(params.get("maxresults"));
			}
			if(builder == null || builder.toString().isEmpty()) {
				return null;
			}
		    return filterTemplate.getDocumentReferences(builder.toString(), paramMap);			
		}
		
		/**
		 * Return the document for a given reference id/hadm_id
		 * 
		 * @param reference
		 * @param docRefField
		 * @return
		 * @throws IOException
		 * @throws ParserConfigurationException 
		 * @throws SAXException 
		 */
		public org.w3c.dom.Document getDocumentRecord_old(String reference, String docRefField, String searchView) throws IOException, ParserConfigurationException, SAXException {
			final Map<String, FilterField> filterFieldNames = parametricService.getFilterFieldNames(searchView);
			SelectBuilder builder = null;
			Map<String, Object> paramMap = new HashMap<String, Object>();
			Set<String> listTables = new HashSet<String>();
			
			/*String[] refFields = docRefField.split(",");
			for(String refField : refFields) {
				if(refField != null && !refField.isEmpty()) {
					FilterField field = filterFieldNames.get(refField.trim());
					if(field != null) {
						String fromTableName = "tagged_documents";			
						listTables.add(fromTableName);				
						builder = new SelectBuilder(fromTableName, field.getVertica().getSchema());
						builder.column("rawdata", fromTableName, "",  false, "");
						builder.where("hadm_id", fromTableName, " = ", "0");
						break;
					}
				}
			}*/
			
			FilterField field = filterFieldNames.get(docRefField);
			
			if(field != null) {
				String fromTableName = "tagged_documents";			
				listTables.add(fromTableName);				
				builder = new SelectBuilder(fromTableName, field.getVertica().getSchema());
				builder.column("rawdata", fromTableName, "",  false, "");
				builder.where("hadm_id", fromTableName, " = ", "0");				
			}
			
			
			
			if(builder == null || builder.toString().isEmpty()) {
				return null;
			}
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		    factory.setNamespaceAware(false);
		    DocumentBuilder docBuilder = factory.newDocumentBuilder();
		    
		    byte[] documentoXml = filterTemplate.getDocument(builder.toString(), paramMap);
		    return docBuilder.parse(new ByteArrayInputStream(documentoXml));
		}
		
		public org.w3c.dom.Document getDocumentRecord(String reference, String docRefField, String searchView) throws Exception {
			//TempXML doc = new TempXML();
			StructuredDataXMLNew doc = new StructuredDataXMLNew();
			String mappingFileName = config.getSearchViews().get(searchView).getDocviewMappingFile(); 
			final Map<String, FilterField> filterFieldNames = parametricService.getFilterFieldNames(searchView);
			FilterField field = filterFieldNames.get(docRefField);
			
			return doc.processDocument(mappingFileName, filterTemplate, reference, docRefField, config, field, getMappingTable(), parametricService, searchView);
		}
		
		public List<FieldPair> getTableViewerData(AciParameters params, SearchRequestData requestData) throws Exception {
			try {
				final Map<String, FilterField> filterFieldNames = parametricService.getFilterFieldNames(requestData.getSearchView());
				String fieldNameStr = params.get("FieldName");
				String fromTable = "";
				String tableName = "";
				SelectBuilder query = null;
				Map<String, Object> paramMap = new HashMap<String, Object>();
				FilterField refField = getReferenceFieldVertica(requestData);
				String queryString = "";			
				Set<String> tableNames = new HashSet<String>();
				SelectBuilder newQuery = null;
				String[] fieldNames = StringUtils.split(fieldNameStr, ',');			
				
				// Not possible to plot more than 2 fields in a table
				if(fieldNames.length > 2) {
					return null;
				}
				FilterField measurementVariableField = getMeasurementVariableFieldVertica(requestData.getUserSearchSettings(), requestData.getSearchView());
				boolean isFieldGrouped = false;
				boolean primaryField = true;
				if(fieldNames != null)  {
					// get the values for each field
					for(String fieldName : fieldNames) {			
						
						
						FilterField field = filterFieldNames.get(fieldName);	
						
						String refTableName = "";
						String measurementColumnName = null;
						if(field != null && field.getVertica() != null) {
							String fieldNameAlias = "secondaryFieldName";
							String fieldValueAlias = "secondaryFieldValue";
							
							tableName = field.getVertica().getTable().getFromTable();
							if(primaryField) { // primary field	
								fromTable = tableName;
								query = new SelectBuilder(fromTable, field.getVertica().getSchema());
								tableNames.add(fromTable);
								fieldNameAlias = "primaryFieldName";
								fieldValueAlias = "primaryFieldValue";
								// Measurement variable like Encounter, Patient, Avg. Cost or Length Of stay etc.							
								if(measurementVariableField != null && measurementVariableField.getVertica() != null) {
									measurementColumnName = QueryBuilderNew.getColumnNameForField(measurementVariableField, tableNames, getMappingTable(), query);
									String measrementTableName = measurementVariableField.getVertica().getTable().getFromTable();							
									QueryBuilderNew.joinTables(fromTable, measrementTableName, tableNames, getMappingTable(), query, null, null, false);
									// Dimension tables
									String dimTableName = measurementVariableField.getVertica().getTable().getDimensionTable();
									QueryBuilderNew.joinTables(measrementTableName, dimTableName, tableNames, getMappingTable(), query, 
											measurementVariableField.getVertica().getTable().getMappingKey(), null, false);
								}							
							}
							// this can happen only in second pass
							if(!tableNames.contains(tableName)) {
								QueryBuilderNew.joinTables(fromTable, tableName, tableNames, getMappingTable(), query, null, null, false);
							}					
							
							String columnName = QueryBuilderNew.getColumnNameForField(field, tableNames, getMappingTable(), query);
							
							QueryBuilderNew.joinTables(fromTable, QueryBuilderNew.getTableNameForMultipleColumn(field), tableNames, getMappingTable(), query, null, null, false);
							
							if(primaryField) {
								if(measurementColumnName != null && !measurementColumnName.isEmpty()) {
									query.column(measurementColumnName, "", measurementVariableField.getVertica().getSelect().getSelectOperator().getAggregateOperator(), false, false, "documentcount");
								}
								//query.column(columnName, "", field.getVertica().getSelect().getSelectOperator().getAggregateOperator(), false, true, "documentcount");
								query.groupBy(columnName);
								query.orderBy("documentcount desc");
							}
							query.column("'" + field.getName() + "'", "", false, fieldNameAlias);
							
							
							// Range field needs case when statements to aggregate the results in groups
							if(QueryBuilderNew.isRangeField(field, config)) { 
								String caseWhenExpression = QueryBuilderNew.buildCaseWhenClauseForGroupField(field, config, tableNames, getMappingTable(), query);
								String columnNameWithCaseWhen = columnName + ", " +  caseWhenExpression;
								if(!primaryField) { // secondary
									query.groupBy(columnName);
								}
								query.column(columnNameWithCaseWhen, "", false, fieldValueAlias);	
								query.groupBy(caseWhenExpression);
								isFieldGrouped = true;
								
							} else {
								boolean useGroupBy = true;
								if(primaryField) {
									useGroupBy = false;
								}
								query.column(columnName, "", "", false, useGroupBy, fieldValueAlias);	
							}
							
							// reference field / table join
							if(refField != null && refField.getVertica() != null) {
								refTableName = refField.getVertica().getTable().getFromTable();							
								//QueryBuilderNew.joinTables(fromTable, refTableName, tableNames, getMappingTable(), query);
							}
							
							// Dimension tables
							String dimTableName = field.getVertica().getTable().getDimensionTable();
							QueryBuilderNew.joinTables(fromTable, dimTableName, tableNames, getMappingTable(), query, field.getVertica().getTable().getMappingKey(), null, false);
							
							// default filter
							QueryBuilderNew.handleDefaultFilter(fromTable, paramMap, tableNames, getMappingTable(), query);
							
							FilterGroup filterGroup = requestData.getFilterGroup();
							// mandatory filter - for the anchor table and/or any join relations
							QueryBuilderNew.handleMandatoryFilter(fromTable, paramMap, tableNames, getMappingTable(), query, filterFieldNames, filterGroup, false);
							
							// mandatory filter - for the inner subexpression
							handleMandatoryFilterForSubExpression(filterGroup, filterFieldNames);
							
							if(!primaryField) { // second pass							
								// user filter expression
								SubExpression subExp = QueryBuilderNew.buildFilterExpression(documentFolderService, documentVerticaFolderService, filterGroup, filterFieldNames, false, 
										paramMap, tableNames, fromTable, getMappingTable(), query, config, null, requestData.getSearchView(), destDateFormat, field, 
										getTableRelations(), field.getVertica().getSchema(), 0, isTotalCountDistinct(requestData.getSearchView(), requestData.getUserSearchSettings()), false,
									  params.get("AutoComplete_FieldName"), filterGroup);
								
								// promote the subExp to the view level
								// get the  view join level
								String viewTable = fromTable;						
								promoteToView(subExp, requestData.getSearchView(), viewTable, field, query, paramMap);
								
							}
							queryString = query.toString();
							
							// Range fields need additional "sum" on the group fields
							if(isFieldGrouped ) {							
								if(primaryField) { // first pass for primary field
									newQuery = new SelectBuilder();
									newQuery.column("'" + field.getName() + "'", "", false, fieldNameAlias);
									newQuery.column(fieldValueAlias, "", false, "");
									newQuery.groupBy(fieldValueAlias);
								} else { // secondary field
									if(queryString != null) {
										StringBuffer newQueryBuff = new StringBuffer(" (");
										newQueryBuff.append(queryString);
										newQueryBuff.append(" ) x");
										newQuery.from(newQueryBuff.toString());
										newQuery.column("'" + field.getName() + "'", "", false, fieldNameAlias);
										newQuery.column(fieldValueAlias, "", false, "");
										newQuery.column("documentcount", "", "sum", false, "documentcount");
										newQuery.groupBy(fieldValueAlias);
										queryString = newQuery.toString();
									}
								}
							} else {
								// primary not a group field but secondary still might be
								if(primaryField) {
									newQuery = new SelectBuilder();
									newQuery.column("'" + field.getName() + "'", "", false, fieldNameAlias);
									newQuery.column(fieldValueAlias, "", false, "");
									newQuery.groupBy(fieldValueAlias);
								}
							}
							primaryField = false;						
						}				
					}
				}	
				List<FieldPair> fieldPair = filterTemplate.getPairFieldsForTable(queryString, paramMap, params.get("maxvalues"));			
				return fieldPair;
			} catch(Exception e) {
				LOGGER.error(e.getMessage());
				throw new Exception(e);
			}
		}
		
		private static class ParametricQueryExecutor implements Callable<Field>  {
	        private String queryString;
	        private Map<String, Object> paramMap;
	        private String fieldName;
	        private FilterTemplate filterTemplate;
	        private String maxValue;

	        public ParametricQueryExecutor(
	        								final String queryString,
	        								final Map<String, Object> paramMap,
	        								final String fieldName,
	                                        final  FilterTemplate filterTemplate,
	                                        final String maxValue)
	        {
	        	this.paramMap = paramMap;
	            this.queryString = queryString;	            
	            this.fieldName = fieldName;
	            this.filterTemplate = filterTemplate;
	            this.maxValue = maxValue;
	        }


	        @Override
	        public Field call() throws Exception {
	            return filterTemplate.getParametricField(queryString, paramMap, fieldName, maxValue);
	        }
	    }
		
		
/* getTrendingTotalsData - Generate Totals dataset to render
		
		-- Get Total Count on baseline date
select count(hipaa_834L_analytics.L_RECIP_ID) documentcount, (hipaa_834L_analytics.L_RES_CITY)fieldValue
from managed_care.hipaa_834L_analytics 
where (hipaa_834L_analytics.AS_OF_DATE <= (:baselineDateStr)) 
	and ((hipaa_834L_analytics.fv > (:baselineDateStr)) or hipaa_834L_analytics.fv IS NULL) 
	and ((hipaa_834L_analytics.HMO_START <= (:baselineDateStr)) 
	and ((hipaa_834L_analytics.HMO_END > (:baselineDateStr)) or (hipaa_834L_analytics.HMO_END IS NULL))) 
group by Rollup(fieldValue)
order by fieldValue


select count(hipaa_834L_analytics.L_RECIP_ID) documentcount, hmo_start, (hipaa_834L_analytics.L_RES_CITY)fieldValue
from managed_care.hipaa_834L_analytics 
where (hipaa_834L_analytics.AS_OF_DATE <= (:namedParameterAsOfDate)) 
	and ((hipaa_834L_analytics.fv > (:namedParameterAsOfDate)) or hipaa_834L_analytics.fv IS NULL)  
	and ((hipaa_834L_analytics.hmo_start >= (:baselineDateStr))  
	and (hipaa_834L_analytics.hmo_start <= (:endDateStr)))
group by fieldValue,hmo_start
order by fieldValue,hmo_start

select count(hipaa_834L_analytics.L_RECIP_ID) documentcount, hmo_end, (hipaa_834L_analytics.L_RES_CITY)fieldValue
from managed_care.hipaa_834L_analytics 
where (hipaa_834L_analytics.AS_OF_DATE <= (:namedParameterAsOfDate)) 
	and ((hipaa_834L_analytics.fv > (:namedParameterAsOfDate)) or hipaa_834L_analytics.fv IS NULL)  
	and ((hipaa_834L_analytics.hmo_end >= (:baselineDateStr))  
	and (hipaa_834L_analytics.hmo_end <= (:endDateStr)))
group by fieldValue,hmo_end
order by fieldValue,hmo_end
*/		
		private long getDateDiffinDays(Date d1,Date d2) {
			// Create Calendar instance
			Calendar calendar1 = Calendar.getInstance();
			Calendar calendar2 = Calendar.getInstance();
	 
			// Set the values for the calendar fields YEAR, MONTH, and DAY_OF_MONTH.
			calendar1.setTime(d1);
			calendar1.setTime(d2);
	 
			/*
			 * Use getTimeInMillis() method to get the Calendar's time value in
			 * milliseconds. This method returns the current time as UTC
			 * milliseconds from the epoch
			 */
			long miliSecondForDate1 = calendar1.getTimeInMillis();
			long miliSecondForDate2 = calendar2.getTimeInMillis();
	 
			// Calculate the difference in millisecond between two dates
			long diffInMilis = miliSecondForDate2 - miliSecondForDate1;
	 
			/*
			 * Now we have difference between two date in form of millsecond we can
			 * easily convert it Minute / Hour / Days by dividing the difference
			 * with appropriate value. 1 Second : 1000 milisecond 1 Hour : 60 * 1000
			 * millisecond 1 Day : 24 * 60 * 1000 milisecond
			 */
	 
			// long diffInSecond = diffInMilis / 1000;
			// long diffInMinute = diffInMilis / (60 * 1000);
			// long diffInHour = diffInMilis / (60 * 60 * 1000);
			long diffInDays = diffInMilis / (24 * 60 * 60 * 1000);
			return diffInDays;
		}
		
		private static Date addDays(Date d, int days)
		{
		    Calendar c = Calendar.getInstance();
		    c.setTime(d);
		    c.add(Calendar.DATE, days);
		    
		    Date nD=new Date();
		    nD.setTime( c.getTime().getTime() );
		    return nD;
		    
		}	
		
		public TrendingTotalsData getTrendingTotalsData(SearchRequestData requestData,String fieldstr, String graphType, String dateField1, String dateField2, String dateStart, String dateEnd, String sortType, String sortOrder) {
			
			final Map<String, FilterField> filterFieldNames = parametricService.getFilterFieldNames(requestData.getSearchView());
			
			// Get Values from Config
			Boolean enabled=config.getSearchViews().get(requestData.getSearchView()).getTrending().isEnabled();
			if (!enabled) {
				TrendingTotalsData trData=new TrendingTotalsData();
				trData.setGraphType("disabled");
				return trData;
			}

			String title=config.getSearchViews().get(requestData.getSearchView()).getTrending().getTitle();

			FilterField measurementVariableField = getMeasurementVariableFieldVertica(requestData.getUserSearchSettings(), requestData.getSearchView());
			
			String MeasurementDesc= getMeasurementVariableDescriptionVertica(requestData.getUserSearchSettings(), requestData.getSearchView());			
			String yaxisLabel=MeasurementDesc;
			String xaxisLabel="Date";
			String format="number";
				
			
			String fieldNameStr = fieldstr;

			String fromTable = "";
			// String spanStartDateFieldName="MC/MC834/HMO_START";
			// String spanendDateStrFieldName="MC/MC834/HMO_END";
			String spanStartDateFieldName=dateField1;
			String spanendDateStrFieldName=dateField2;
		
			
			
			String minDateStr="04/01/2016";
			String maxDateStr="10/01/2016";
			
			String startDateStr=minDateStr;
			String endDateStr=maxDateStr;
			
			String limitMinDateStr=minDateStr;
			String limitMaxDateStr=maxDateStr;
			
			String maxValuesStr="10000";
			int maxValues=Integer.parseInt(maxValuesStr);
			int minZoomRegion=5; // Days
			int maxZoomRegion=120;
			 
			Date limitMinDate=null;
			Date limitMaxDate=null;
			Date minDate=null;
			Date maxDate=null;
			DateFormat df = new SimpleDateFormat("MM/dd/yyyy");

			
			Map<String, Object> paramMap0 = new HashMap<String, Object>();
			Map<String, Object> paramMap1 = new HashMap<String, Object>();
			Map<String, Object> paramMap2 = new HashMap<String, Object>();
			Map<String, Object> paramMap3 = new HashMap<String, Object>();
			Map<String, Object> paramMapI = new HashMap<String, Object>();
			
			FilterField refField = getReferenceFieldVertica(requestData);
			
			SelectBuilder query0 = null;
			SelectBuilder query1 = null;
			SelectBuilder query2 = null;
			SelectBuilder query3 = null;
			SelectBuilder queryI = null;
			
			Set<String> tableNames0 = new HashSet<String>();
			Set<String> tableNames1 = new HashSet<String>();
			Set<String> tableNames2 = new HashSet<String>();
			Set<String> tableNames3 = new HashSet<String>();
			Set<String> tableNamesI = new HashSet<String>();
			
			FilterField field = filterFieldNames.get(fieldNameStr);	
			boolean isFieldGrouped = false;
			String refTableName = "";
	
			
			// Get the FieldValues
			
			
			Field fieldValues=null;
			
		
			if(field != null && field.getVertica() != null) {
				fromTable = field.getVertica().getTable().getFromTable();
				query0= new SelectBuilder(fromTable, field.getVertica().getSchema());
				tableNames0.add(fromTable);
				String columnName = QueryBuilderNew.getColumnNameForField(field, tableNames0, getMappingTable(), query0);	
				String measurementColumnName = null;
				if(measurementVariableField != null && measurementVariableField.getVertica() != null) {
					measurementColumnName = QueryBuilderNew.getColumnNameForField(measurementVariableField,tableNames0, getMappingTable(), query0);
					String measrementTableName = measurementVariableField.getVertica().getTable().getFromTable();							
					QueryBuilderNew.joinTables(fromTable, measrementTableName, tableNames0, getMappingTable(), query0, null, null, false);			
					// Dimension tables
					String dimTableName0 = measurementVariableField.getVertica().getTable().getDimensionTable();
					QueryBuilderNew.joinTables(measrementTableName, dimTableName0, tableNames0, getMappingTable(), query0, 
							measurementVariableField.getVertica().getTable().getMappingKey(), null, false);					
				}
				if(measurementColumnName != null && !measurementColumnName.isEmpty()) {
					query0.column(measurementColumnName, "", measurementVariableField.getVertica().getSelect().getSelectOperator().getAggregateOperator(), false, false, "documentcount");
					query0.groupBy(columnName);
				}  else {
					query0.column(columnName, "", field.getVertica().getSelect().getSelectOperator().getAggregateOperator(), false, true, "documentcount");
				}
				if(QueryBuilderNew.isRangeField(field, config)) { 
					String caseWhenExpression = QueryBuilderNew.buildCaseWhenClauseForGroupField(field, config, tableNames0, getMappingTable(), query0);
					String columnNameWithCaseWhen = columnName + ", " +  caseWhenExpression;
					query0.column(columnNameWithCaseWhen, "", false, "fieldValue");	
					query0.groupBy(caseWhenExpression);
				} else {
					query0.column(columnName, "", false, "fieldValue");	
				}

				String dimTableName = field.getVertica().getTable().getDimensionTable();
				QueryBuilderNew.joinTables(fromTable, dimTableName, tableNames0, getMappingTable(), query0, field.getVertica().getTable().getMappingKey(), null, false);
			
				// default filter
				QueryBuilderNew.handleDefaultFilter(fromTable, paramMap0, tableNames0, getMappingTable(), query0);			
				
				FilterGroup filterGroup = requestData.getFilterGroup();
				// mandatory filter - for the anchor table and/or any join relations
				QueryBuilderNew.handleMandatoryFilter(fromTable, paramMap0, tableNames0, getMappingTable(), query0, filterFieldNames, filterGroup, false);
				
				// mandatory filter - for the inner subexpression
				handleMandatoryFilterForSubExpression(filterGroup, filterFieldNames);
				
				// Common Where Clause
				String whereClause = QueryBuilder.buildFilterExpression(documentFolderService, documentVerticaFolderService, filterGroup, filterFieldNames, false, 
						paramMap0, tableNames0, fromTable, getMappingTable(), query0, config, null, requestData.getSearchView(), destDateFormat);
				
				// Add specific where clauses for each query				
				FilterField startfield = filterFieldNames.get(spanStartDateFieldName);	
				FilterField endfield = filterFieldNames.get(spanendDateStrFieldName);	
				String startcol = QueryBuilderNew.getColumnNameForField(startfield, tableNames0, getMappingTable(), query0);
				String endcol= QueryBuilderNew.getColumnNameForField(endfield, tableNames0, getMappingTable(), query0);

				if (whereClause!=null) {
					query0.where(whereClause.toString());		
				}
				query0.orderBy(columnName);				
											
				// Range fields need additional "sum" on the group fields
/*				if(isFieldGrouped) {							
					if(queryString != null) {
						StringBuffer newQueryBuff = new StringBuffer(" (");
						newQueryBuff.append(queryString);
						newQueryBuff.append(" ) x");								
						SelectBuilder newQuery = new SelectBuilder(newQueryBuff.toString());
						newQuery.column("documentcount", "", "sum", false, "documentcount");
						newQuery.column("fieldValue", "", false, true);
						queryString = newQuery.toString();
					}							
				}
*/				
				String queryFieldValues=query0.toString();
				
				fieldValues = filterTemplate.getParametricField(queryFieldValues, paramMap0,fieldNameStr,maxValuesStr);
				
			
			}
			
			
			// Calculate limit Date Range
			try {
				minDate=df.parse(minDateStr);
				maxDate=df.parse(maxDateStr);
				limitMinDate = df.parse(startDateStr);
				limitMaxDate = df.parse(endDateStr);
				
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// limit viewing to min and max dates
			if (limitMinDate.before(minDate)) {
				limitMinDate=minDate;
				limitMinDateStr=minDateStr;
			}
			if (limitMaxDate.after(maxDate)) {
				limitMaxDate=maxDate;
				limitMaxDateStr=minDateStr;
			}
			
			int totValues=0;
			if (fieldValues!=null) {
				totValues=(int) fieldValues.getTotalValues();
			} 
			
			if (totValues==0) {
				return null;
			}
			
			// limit min if too many values need to be retrieved
			int maxDays=((int) (maxValues)/(totValues) );
			if (maxDays>maxZoomRegion) {
				maxDays=maxZoomRegion;			
				maxValues=(int) (maxDays*fieldValues.getTotalValues());		
				maxValuesStr=Long.toString(maxValues);
			}
			if (maxDays<minZoomRegion) {
				maxDays=(int) minZoomRegion;
				maxValues=(int) (maxDays*fieldValues.getTotalValues());		
				maxValuesStr=Long.toString(maxValues);
			}
			long reqDays=getDateDiffinDays(limitMinDate,limitMaxDate);
			
			limitMinDate=addDays(limitMaxDate,-maxDays);
			limitMinDateStr=df.format(limitMinDate);
			
			// Generate the Inner Join Clause
			// ------------------------------
			if(refField != null && refField.getVertica() != null) {
				fromTable = refField.getVertica().getTable().getFromTable();
				queryI= new SelectBuilder(fromTable, refField.getVertica().getSchema());
				queryI.column(refField.getVertica().getColumn(), "", "", false, false, "");

				tableNamesI.add(fromTable);
				// Measuring field like Gender, Ethnicity etc.
				String columnName = QueryBuilderNew.getColumnNameForField(field, tableNamesI, getMappingTable(), queryI);	
				// Measurement variable like Encounter, Patient, Avg. Cost or Length Of stay etc.
				String measurementColumnName = null;
				if(measurementVariableField != null && measurementVariableField.getVertica() != null) {
					measurementColumnName = QueryBuilderNew.getColumnNameForField(measurementVariableField, tableNamesI, getMappingTable(), queryI);
					String measrementTableName = measurementVariableField.getVertica().getTable().getFromTable();							
					QueryBuilderNew.joinTables(fromTable, measrementTableName, tableNamesI, getMappingTable(), queryI, null, null, false);
					// Dimension tables
					String dimTableName = measurementVariableField.getVertica().getTable().getDimensionTable();
					QueryBuilderNew.joinTables(measrementTableName, dimTableName, tableNamesI, getMappingTable(), queryI, 
							measurementVariableField.getVertica().getTable().getMappingKey(), null, false);
				}
								
				// Dimension tables
				String dimTableName = field.getVertica().getTable().getDimensionTable();
				QueryBuilderNew.joinTables(fromTable, dimTableName, tableNamesI, getMappingTable(), queryI, field.getVertica().getTable().getMappingKey(), null, false);

				// default filter
				QueryBuilderNew.handleDefaultFilter(fromTable, paramMapI, tableNamesI, getMappingTable(), queryI);
				
				FilterGroup filterGroup = requestData.getFilterGroup();
				// mandatory filter - for the anchor table and/or any join relations
				QueryBuilderNew.handleMandatoryFilter(fromTable, paramMapI, tableNamesI, getMappingTable(), queryI, filterFieldNames, filterGroup, false);

				// mandatory filter - for the inner subexpression
				handleMandatoryFilterForSubExpression(filterGroup, filterFieldNames);

				// Common Where Clause				
				StringBuffer whereI = new StringBuffer("");
				String whereClauseI = QueryBuilder.buildFilterExpression(documentFolderService, documentVerticaFolderService, filterGroup, filterFieldNames, false, 
						paramMapI, tableNamesI, fromTable, getMappingTable(), queryI, config, null, requestData.getSearchView(), destDateFormat);
				if (whereClauseI!=null) {
					queryI.where(whereClauseI.toString());		
				}
			}
	
			// Generate the Outer Trending Join Clause
			// ---------------------------------------
			
			if(field != null && field.getVertica() != null) {
				fromTable = field.getVertica().getTable().getFromTable();
				query1= new SelectBuilder(fromTable, field.getVertica().getSchema());
				query2= new SelectBuilder(fromTable, field.getVertica().getSchema());
				query3= new SelectBuilder(fromTable, field.getVertica().getSchema());

				
				tableNames1.add(fromTable);
				tableNames2.add(fromTable);
				tableNames3.add(fromTable);
				
				// Measuring field like Gender, Ethnicity etc.
				String columnName = QueryBuilderNew.getColumnNameForField(field,tableNames1, getMappingTable(), query1);	
				// Measurement variable like Encounter, Patient, Avg. Cost or Length Of stay etc.
				String measurementColumnName = null;
				if(measurementVariableField != null && measurementVariableField.getVertica() != null) {
					measurementColumnName = QueryBuilderNew.getColumnNameForField(measurementVariableField, tableNames1, getMappingTable(), query1);
					String measrementTableName = measurementVariableField.getVertica().getTable().getFromTable();							
					QueryBuilderNew.joinTables(fromTable, measrementTableName, tableNames1, getMappingTable(), query1, null, null, false);
					QueryBuilderNew.joinTables(fromTable, measrementTableName, tableNames2, getMappingTable(), query2, null, null, false);
					QueryBuilderNew.joinTables(fromTable, measrementTableName, tableNames3, getMappingTable(), query3, null, null, false);
					// Dimension tables
					String dimTableName = measurementVariableField.getVertica().getTable().getDimensionTable();
					QueryBuilderNew.joinTables(measrementTableName, dimTableName, tableNames1, getMappingTable(), query1, measurementVariableField.getVertica().getTable().getMappingKey(), null, false);
					QueryBuilderNew.joinTables(measrementTableName, dimTableName, tableNames2, getMappingTable(), query2, measurementVariableField.getVertica().getTable().getMappingKey(), null, false);
					QueryBuilderNew.joinTables(measrementTableName, dimTableName, tableNames3, getMappingTable(), query3, measurementVariableField.getVertica().getTable().getMappingKey(), null, false);
				}
				// Add joinSubClause
				if (queryI!=null) {
					String jONExp="("+queryI.toString()+") sub ON "+fromTable+"."+refField.getVertica().getColumn()+" = sub."+ refField.getVertica().getColumn();
					query1.innerJoin(jONExp);
					query2.innerJoin(jONExp);
					query3.innerJoin(jONExp);
				}
				
				
				
				
				if(measurementColumnName != null && !measurementColumnName.isEmpty()) {
					query1.column(measurementColumnName, "", measurementVariableField.getVertica().getSelect().getSelectOperator().getAggregateOperator(), true, false, "documentcount");
					query1.groupBy(columnName);
					query2.column(measurementColumnName, "", measurementVariableField.getVertica().getSelect().getSelectOperator().getAggregateOperator(), true, false, "documentcount");
					query2.groupBy(columnName);
					query3.column(measurementColumnName, "", measurementVariableField.getVertica().getSelect().getSelectOperator().getAggregateOperator(), true, false, "documentcount");
					query3.groupBy(columnName);
			
				}  else {
					query1.column(columnName, "", field.getVertica().getSelect().getSelectOperator().getAggregateOperator(), false, true, "documentcount");
					query2.column(columnName, "", field.getVertica().getSelect().getSelectOperator().getAggregateOperator(), false, true, "documentcount");
					query3.column(columnName, "", field.getVertica().getSelect().getSelectOperator().getAggregateOperator(), false, true, "documentcount");
				}
				
				
				// Range field needs case when statements to aggregate the results in groups
				if(QueryBuilderNew.isRangeField(field, config)) { 
					String caseWhenExpression = QueryBuilderNew.buildCaseWhenClauseForGroupField(field, config, tableNames1, getMappingTable(), query1);
					String columnNameWithCaseWhen = columnName + ", " +  caseWhenExpression;
					query1.column(columnNameWithCaseWhen, "", false, "fieldValue");	
					query1.groupBy(caseWhenExpression);
					query2.column(columnNameWithCaseWhen, "", false, "fieldValue");	
					query2.groupBy(caseWhenExpression);
					query3.column(columnNameWithCaseWhen, "", false, "fieldValue");	
					query3.groupBy(caseWhenExpression);
					isFieldGrouped = true;
				} else {
					query1.column(columnName, "", false, "fieldValue");	
					query2.column(columnName, "", false, "fieldValue");	
					query3.column(columnName, "", false, "fieldValue");	
				}
				
				
				// reference field / table join
				if(refField != null && refField.getVertica() != null) {
					refTableName = refField.getVertica().getTable().getFromTable();							
					//QueryBuilderNew.joinTables(fromTable, refTableName, tableNames, getMappingTable(), query);
				}
				
				// Dimension tables
				String dimTableName = field.getVertica().getTable().getDimensionTable();
				QueryBuilderNew.joinTables(fromTable, dimTableName, tableNames1, getMappingTable(), query1, field.getVertica().getTable().getMappingKey(), null, false);
				QueryBuilderNew.joinTables(fromTable, dimTableName, tableNames2, getMappingTable(), query2, field.getVertica().getTable().getMappingKey(), null, false);
				QueryBuilderNew.joinTables(fromTable, dimTableName, tableNames3, getMappingTable(), query3, field.getVertica().getTable().getMappingKey(), null, false);

				// default filter
/*				QueryBuilderNew.handleDefaultFilter(fromTable, paramMap1, tableNames1, getMappingTable(), query1);
				QueryBuilderNew.handleDefaultFilter(fromTable, paramMap2, tableNames2, getMappingTable(), query2);
				QueryBuilderNew.handleDefaultFilter(fromTable, paramMap3, tableNames3, getMappingTable(), query3);
*/
				// mandatory filter - for the anchor table and/or any join relations
/*				QueryBuilderNew.handleMandatoryFilter(fromTable, paramMap1, tableNames1, getMappingTable(), query1, filterFieldNames, requestData.getFilterGroup());
				QueryBuilderNew.handleMandatoryFilter(fromTable, paramMap2, tableNames2, getMappingTable(), query2, filterFieldNames, requestData.getFilterGroup());
				QueryBuilderNew.handleMandatoryFilter(fromTable, paramMap3, tableNames3, getMappingTable(), query3, filterFieldNames, requestData.getFilterGroup());
*/				
				// Common Where Clause
				/*
				String whereClause1 = QueryBuilderNew.buildFilterExpression(documentFolderService, requestData.getFilterGroup(), filterFieldNames, false, 
						paramMap1, tableNames1, fromTable, getMappingTable(), query1, config, null);

				String whereClause2 = QueryBuilderNew.buildFilterExpression(documentFolderService, requestData.getFilterGroup(), filterFieldNames, false, 
						paramMap2, tableNames2, fromTable, getMappingTable(), query2, config, null);

				String whereClause3 = QueryBuilderNew.buildFilterExpression(documentFolderService, requestData.getFilterGroup(), filterFieldNames, false, 
						paramMap3, tableNames3, fromTable, getMappingTable(), query3, config, null);
				*/
				
				// Add specific where clauses for each query				
				FilterField startfield = filterFieldNames.get(spanStartDateFieldName);	
				FilterField endfield = filterFieldNames.get(spanendDateStrFieldName);	
				String startcol = QueryBuilderNew.getColumnNameForField(startfield, tableNames1, getMappingTable(), query1);
				String endcol= QueryBuilderNew.getColumnNameForField(endfield, tableNames1, getMappingTable(), query1);

				StringBuffer where1 = new StringBuffer("");
				StringBuffer where2 = new StringBuffer("");
				StringBuffer where3 = new StringBuffer("");

				
				where1.append(" (("+startcol+" <='" + limitMinDateStr +"' ) AND (("+endcol+" >'"+ limitMinDateStr +"') OR ("+endcol+" IS NULL)))");				
				query1.where(where1.toString());					
				query1.orderBy(columnName);
				
				startcol = QueryBuilderNew.getColumnNameForField(startfield, tableNames2, getMappingTable(), query2);
				endcol= QueryBuilderNew.getColumnNameForField(endfield, tableNames2, getMappingTable(), query2);
				
				query2.column(startcol, "", false, "fieldDate");	
				where2.append(" (("+startcol+" >='" + limitMinDateStr +"' ) AND ("+startcol+" <='"+ limitMaxDateStr +"'))");				
				query2.where(where2.toString());					
				query2.orderBy(columnName);
				query2.orderBy(startcol);
				query2.groupBy(startcol);
				
				startcol = QueryBuilderNew.getColumnNameForField(startfield, tableNames3, getMappingTable(), query3);
				endcol= QueryBuilderNew.getColumnNameForField(endfield, tableNames3, getMappingTable(), query3);
				
				query3.column(endcol, "", false, "fieldDate");	
				where3.append(" (("+endcol+" >='" + limitMinDateStr +"' ) AND ("+endcol+" <='"+ limitMaxDateStr +"'))");				
				query3.where(where3.toString());					
				query3.orderBy(columnName);
				query3.orderBy(endcol);
				query3.groupBy(endcol);
				
											
				// Range fields need additional "sum" on the group fields
/*				if(isFieldGrouped) {							
					if(queryString != null) {
						StringBuffer newQueryBuff = new StringBuffer(" (");
						newQueryBuff.append(queryString);
						newQueryBuff.append(" ) x");								
						SelectBuilder newQuery = new SelectBuilder(newQueryBuff.toString());
						newQuery.column("documentcount", "", "sum", false, "documentcount");
						newQuery.column("fieldValue", "", false, true);
						queryString = newQuery.toString();
					}							
				}
*/				
				String queryStringBaseline=query1.toString();
				String queryStringAdds=query2.toString();;
				String queryStringRemoves=query3.toString();;
				
				
				// Run all three queries
	
				Field fieldTotals = filterTemplate.getParametricField(queryStringBaseline, paramMapI,fieldNameStr,maxValuesStr);
				Field fieldAdds = filterTemplate.getTrendingDateField(queryStringAdds, paramMapI,fieldNameStr,maxValuesStr,false);
				Field fieldRemoves = filterTemplate.getTrendingDateField(queryStringRemoves, paramMapI,fieldNameStr,maxValuesStr,true);
										
				// generate TrendingTotalsData Structure, sparse for now
				// -----------------------------------------------------
				TrendingTotalsData trData=new TrendingTotalsData();
			
				// set trending metadata
				// ---------------------
				// List<TrendingDataLayer> dataLayers= new ArrayList<TrendingDataLayer>();
				
				trData.setTitle(title);
				trData.setYaxisLabel(yaxisLabel);
				trData.setXaxisLabel(xaxisLabel);
				trData.setStartDate(limitMinDateStr);
				trData.setEndDate(endDateStr);
				trData.setValidStartDate(limitMinDateStr);
				trData.setValidEndDate(limitMinDateStr);
				trData.setFormat(format);
				trData.setField(fieldNameStr);
				trData.setGraphType(graphType);
				
				
				// get a list of all unique dates from all results for totals
				Set <Date> dateSet=new HashSet<Date>();
				Date bd=new Date();
				try {
					bd = df.parse(limitMinDateStr);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				dateSet.add(bd);

				// Create fv maps
				HashMap<String,HashMap<String,FieldValue>> fdmap=new HashMap<String, HashMap<String, FieldValue>>();
				HashMap<String,HashMap<String,FieldValue>> fdmap2=new HashMap<String, HashMap<String, FieldValue>>();
				
				List<FieldValue> fdlist=fieldAdds.getFieldValues();
				for (int i = 0; i < fdlist.size(); i++) {
					FieldValue fv=fdlist.get(i);
					Date dt=fv.getDate();
					if (dt!=null && dt.after(limitMinDate) && dt.before(limitMaxDate)) {
						dateSet.add(dt);
						String val=fv.getValue();
						HashMap<String,FieldValue> dtMap=fdmap.get(val);
						if (dtMap==null) {
							dtMap=new HashMap<String,FieldValue>();
							fdmap.put(val, dtMap);
						}
						String dtStr=df.format(dt);
						dtMap.put(dtStr, fv);
					}
				}
				List<FieldValue> fdlist2=fieldRemoves.getFieldValues();
				for (int i = 0; i < fdlist2.size(); i++) {
					FieldValue fv2=fdlist2.get(i);
					Date dt=fv2.getDate();
					if (dt!=null && dt.after(limitMinDate) && dt.before(limitMaxDate)) {
						dateSet.add(dt);
						String val=fv2.getValue();
						HashMap<String,FieldValue> dtMap2=fdmap2.get(val);
						if (dtMap2==null) {
							dtMap2=new HashMap<String,FieldValue>();
							fdmap2.put(val, dtMap2);
						}
						String dtStr=df.format(dt);
						dtMap2.put(dtStr, fv2);
					}
				}
				
				Object[] dateArr =  dateSet.toArray();
				Arrays.sort(dateArr);
				
				
/*				TrendingDataLayer newTotLayer=new TrendingDataLayer();
				newTotLayer.setLabel("TOTALS");
				newTotLayer.setType("totals");
				newTotLayer.setOrder("0");
				newTotLayer.setVarName("Totals");
				newTotLayer.setbaselineDateStr(baselineDateStr);
*/				
				// walk the results by field then by date cursor, creating totals array
				List<FieldValue> totlist=fieldTotals.getFieldValues();
				HashMap<String,FieldValue> totMap=new HashMap<String,FieldValue>();
				for (int i=0;i<totlist.size();i++) {
					 FieldValue fv=totlist.get(i);
					 String desc=fv.getValue();
					 totMap.put(desc,fv);
				}
				
				
				

				
				int ai=0;
				int ri=0;
				int ti=0;
				int order=1;
				int vi=0;
				
				List<FieldValue> fvlist=fieldValues.getFieldValues();
				
				while (vi<fvlist.size()) {
					
					FieldValue lref=fvlist.get(vi);
					double curTotalCount=0;
					String curVal=lref.getValue();
				
					
					TrendingDataLayer curValLayer=new TrendingDataLayer();
					curValLayer.setBaselineDate(limitMinDateStr);
					curValLayer.setBaselineValue(0);
					curValLayer.setLabel(curVal);
					curValLayer.setOrder(Integer.toString(order));
					curValLayer.setType("value");
					curValLayer.setVarName(fieldNameStr);
					order++;

					if (vi<100000) {
						// Set the total if we have a baseline record for this value
						FieldValue tot=totMap.get(curVal);
						if (tot!=null) {
							String curtotVal=tot.getValue();
							double totCount=tot.getCount();
							curTotalCount=totCount;
						}
	
						curValLayer.setBaselineDate(limitMinDateStr);
						curValLayer.setBaselineValue(curTotalCount);
												
						int di=0;
	
					
						FieldValue fa=null;
						Date fad=null;
						String fav=null;
						double fac=0;
						
						FieldValue fr=null;
						Date frd=null;
						String frv=null;
						double frc=0;
						Date nd=null;
						String fadStr=null;
						String frdStr=null;
						
						HashMap<String,FieldValue> dtMap=fdmap.get(curVal);					
						HashMap<String,FieldValue> dtMap2=fdmap2.get(curVal);					

						while (di<dateArr.length) {
								
							nd=(Date) dateArr[di];
	
							String ndStr=df.format(nd);
							TrendingDataVal dv=new TrendingDataVal();
							dv.setDate(ndStr);
							dv.setTotal_count(curTotalCount);
							dv.setAdd_count(0);
							dv.setRemove_count(0);					

							if (dtMap!=null) {
								fa=dtMap.get(ndStr);
								if (fa!=null) {
									fav=fa.getValue();
									fad=fa.getDate();
									fadStr=df.format(fad);
									fac=fa.getCount();						
									dv.setAdd_count(fac);				
									if (!ndStr.equals(limitMinDateStr)) {
										curTotalCount+=fac;
										dv.setTotal_count(curTotalCount);																
									}
								}
							}

							if (dtMap2!=null) {
								fr=dtMap2.get(ndStr);
								if (fr!=null) {
									frd=fr.getDate();
									frdStr=df.format(frd);
									frv=fr.getValue();
									frc=fr.getCount();						
									dv.setRemove_count(frc);				
									if (!ndStr.equals(limitMinDateStr)) {
										curTotalCount-=frc;
										dv.setTotal_count(curTotalCount);																
									}
								}
							}

							
						
							curValLayer.addVal(dv);					
							di++;								
						}
					}
					List<TrendingDataLayer> tl = trData.getDataLayers();
					tl.add(curValLayer);
					vi++;
				}	
				return trData;
						
			}				
			
			return null;
		}
		
		public IndexFile fetchFileNameAndOffsetFromVertica(String searchView, String reference) {
			//final Map<String, FilterField> filterFieldNames = parametricService.getFilterFieldNames(searchView);
			SelectBuilder builder = null;
			Map<String, Object> paramMap = new HashMap<String, Object>();
			Set<String> listTables = new HashSet<String>();	
			
			String fromTableName = searchView + "_otindex";			
			listTables.add(fromTableName);				
			builder = new SelectBuilder(fromTableName, config.getSearchViews().get(searchView).getVerticaSchema());
						
			builder.column("view", false);
			builder.column("type", false);	
			builder.column("id", false);	
			builder.column("filename", false);	
			builder.column("start", false);	
			builder.column("bytes", false);	
			builder.where("view", fromTableName, "=", "(:searchViewName)");
			paramMap.put("searchViewName", searchView);
			builder.where("id", fromTableName, "=", "(:idname)");
			paramMap.put("idname", reference);
			
			if(builder == null || builder.toString().isEmpty()) {
				return null;
			}
			
			return filterTemplate.getDocumentIndexAndFileName(builder.toString(), paramMap);		    
		}
		
		private void promoteToView(SubExpression subExp, String searchView, String viewTable, FilterField field, SelectBuilder builder,
				Map<String, Object> paramMap) throws Exception {
			if(subExp != null) {
				int viewJoinLevel = getTableRelationByViewAndTable(searchView, viewTable).getJoinNumber();
				while (subExp.getJoinLevel() < viewJoinLevel) {
					TableRelationObject relation = getTableRelationByJoinLevel(searchView, subExp.getJoinLevel());
					TableRelationObject parentRelation = getTableRelationByViewAndTable(searchView, relation.getParentTable());
					String xAlias = "PTV" + relation.getJoinNumber();
					SelectBuilder wrapper = new SelectBuilder(parentRelation.getTable(), field.getVertica().getSchema(), subExp.getJoinLevel());	                						
					wrapper.column(parentRelation.getRefCol(), parentRelation.getTable(), true);
					if(parentRelation.getRefCol() != null && parentRelation.getJoinCol()!= null 
							&& !parentRelation.getRefCol().equalsIgnoreCase(parentRelation.getJoinCol())) {
						wrapper.column(parentRelation.getJoinCol(), parentRelation.getTable(), false);
					}
					wrapper.column(parentRelation.getExtraColumns(), parentRelation.getTable(), false);
					wrapper.column(parentRelation.getExtraParentColumns(), parentRelation.getTable(), false);
					
					SelectBuilder filterSQL = subExp.getSubExpression();
					String subExpression = null;
					if(relation.getJoinExp() != null && !relation.getJoinExp().isEmpty()) {
						subExpression = QueryBuilderNew.handleFilterJoinExpression(relation.getJoinExp(), relation.getTable(), subExp.getListTables(), filterSQL, getMappingTable(), paramMap);			
					}	
					String join = " inner join (" + filterSQL + ") " + xAlias + " ON " + relation.getParentTable() + "." + relation.getParentCol()+ " = " 
							+ xAlias + "." + relation.getJoinCol();								
					if(StringUtils.isNotBlank(subExpression)) {
						join = join + " AND " + subExpression;
					}
					
					wrapper.join(join);
					subExp.setSubExpression(wrapper);
					subExp.setJoinLevel(parentRelation.getJoinNumber());
				}
				// final expression
				int filterJoinLevel = subExp.getJoinLevel();
				TableRelationObject relation = getTableRelationByJoinLevel(searchView, filterJoinLevel);
				
				// final wrapper for the inner query distinct
				SelectBuilder wrapper = new SelectBuilder();
				wrapper.column(relation.getRefCol(), "FWRAP", true);
				wrapper.from("(" + subExp.getSubExpression().toString() + "\n) FWRAP");			
				String fref = relation.getRefCol();
				String join = " inner join (" + wrapper.toString() + "\n) FEXP ON " + relation.getTable() + "." + relation.getRefCol() + " = " 
						+ "FEXP." + fref;			
				builder.join(join);	
			}
		}	
		
		private void handleMandatoryFilterForSubExpression(FilterGroup filterGroup, final Map<String, FilterField> filterFieldNames) {
			// also add the mandatory filter to the filter group so that it is also applied to the where clause/inner query subexpression
			List<FilterField> mandatoryFields = QueryBuilderNew.getMandatoryFilterField(filterFieldNames);	
			for(FilterField mandatoryField : mandatoryFields) {
				if(mandatoryField != null) {
					Map<String, Character> filterTypeMap = new HashMap<String, Character>();
					if(mandatoryField.getParametric() != null) {
						filterTypeMap.put(mandatoryField.getParametric().getType().value(), 'P');
					}
					if(mandatoryField.getIndexed() != null) {
						filterTypeMap.put(mandatoryField.getIndexed().getType().value(), 'I');
					}
					if(mandatoryField.getCustom() != null) {
						filterTypeMap.put(mandatoryField.getCustom().getType().value(), 'C');
					}
					// Mandatory filter needs to be added to the group
					if(filterGroup != null) {
						/*Map<String, List<FieldParams>>  filterFields = null; //new HashMap<String, List<FieldParams>>();
						if(filterGroup.getFilterFields() == null) {
							if(filterGroup.getChildGroups() != null && filterGroup.getChildGroups().get(0) != null) {
								filterFields = filterGroup.getChildGroups().get(0).getFilterFields();
							}
						} else {
							filterFields = filterGroup.getFilterFields();
						}
						if(filterFields.get(mandatoryField.getName()) == null) {					
					        List<FieldParams> listParams = new ArrayList<FieldParams>();        
					        FieldParams fieldParam = new FieldParams();
					        fieldParam.setOp(FilterOperator.IS);
					        fieldParam.setType(filterTypeMap.get(mandatoryField.getVertica().getFilterSection().getDefaultType()) == null ? 'I' : filterTypeMap.get(mandatoryField.getVertica().getFilterSection().getDefaultType()));
					        fieldParam.setVal(String.valueOf(mandatoryField.getVertica().getFilterSection().getDefaultValue()));
					        listParams.add(fieldParam);
					        filterFields.put(mandatoryField.getName(), listParams);
						}*/
						
						// SK commented out 2/26
						//addMandatoryFilterInFilterGroup(filterGroup, mandatoryField, filterTypeMap, null);
					}
				}
			}
		}
		
		private void addMandatoryFilterInFilterGroup(FilterGroup filterGroup, FilterField mandatoryField, Map<String, Character> filterTypeMap, String mandatoryFilterVal) {
			//String mandatoryFilter
			if(filterGroup != null) {
				Map<String, List<FieldParams>>  filterFields = filterGroup.getFilterFields();			
				if(filterFields != null) {
					if(filterFields.get(mandatoryField.getName()) != null) {
						mandatoryFilterVal = filterFields.get(mandatoryField.getName()).get(0).getVal();
					}
					if(filterFields.get(mandatoryField.getName()) == null) {				
				        List<FieldParams> listParams = new ArrayList<FieldParams>();        
				        FieldParams fieldParam = new FieldParams();
				        fieldParam.setOp(FilterOperator.IS);
				        fieldParam.setType(filterTypeMap.get(mandatoryField.getVertica().getFilterSection().getDefaultType()) == null ? 'I' : filterTypeMap.get(mandatoryField.getVertica().getFilterSection().getDefaultType()));
				        fieldParam.setVal(StringUtils.isNotEmpty(mandatoryFilterVal) ? mandatoryFilterVal : String.valueOf(mandatoryField.getVertica().getFilterSection().getDefaultValue()));
				        listParams.add(fieldParam);
				        filterFields.put(mandatoryField.getName(), listParams);
					}
				}
				
				if (filterGroup.getChildGroups() != null) {
		            for (final FilterGroup childGroup : filterGroup.getChildGroups()) {
		                if (childGroup != null && childGroup.getCohortOp() == null)  {
		                    // Only support cohort at the first child level
		                	addMandatoryFilterInFilterGroup(childGroup, mandatoryField, filterTypeMap, mandatoryFilterVal);
		                }
		            } // end of all child groups           
			    }  
				
			}
			
		}
		
}
