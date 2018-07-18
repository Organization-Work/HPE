package com.autonomy.find.controllers;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.autonomy.aci.actions.idol.tag.Field;
import com.autonomy.aci.actions.idol.tag.FieldValue;
import com.autonomy.aci.content.fieldtext.FieldText;
import com.autonomy.aci.content.fieldtext.WILD;
import com.autonomy.find.api.database.SearchSettings;
import com.autonomy.find.api.response.ResponseWithResult;
import com.autonomy.find.api.response.ResponseWithSuccessError;
import com.autonomy.find.config.FindConfig;
import com.autonomy.find.config.SearchConfig;
import com.autonomy.find.dto.Links;
import com.autonomy.find.dto.ResultDocument;
import com.autonomy.find.dto.Results;
import com.autonomy.find.dto.SearchRequestData;
import com.autonomy.find.dto.Parametric.TableViewResponse;
import com.autonomy.find.dto.Parametric.TimelineViewResponse;
import com.autonomy.find.dto.Parametric.TrendingTotalsResponse;
import com.autonomy.find.services.CategoryTrainingService;
import com.autonomy.find.services.QuerySummaryClusters;
import com.autonomy.find.services.SearchService;
import com.autonomy.find.services.SearchSettingsService;
import com.autonomy.find.services.admin.AdminService;
import com.autonomy.find.util.ExceptionsHelper;
import com.autonomy.find.util.RequestProcess;
import com.autonomy.find.util.RequestState;
import com.autonomy.find.util.RequestTracker;
import com.autonomy.find.util.SessionHelper;
import com.autonomy.find.util.audit.AuditActions;
import com.autonomy.find.util.audit.AuditLogger;



@Controller
@RequestMapping("/p/ajax/search/")
public class SearchController {
	
	@Autowired
	private SearchService searchService;

    @Autowired
    private SearchSettingsService searchSettingsService;

    @Autowired
    private CategoryTrainingService trainingService;

	@Autowired
	private FindConfig config;

    @Autowired
    private SessionHelper sessionHelper;
    
    @Autowired
 	private SearchConfig searchConfig;
    
    @Autowired
    private RequestTracker requestTracker;
    
    @Autowired
	private AdminService adminService;
    


    private static final Logger LOGGER = LoggerFactory.getLogger(SearchController.class);
    private static final String ERROR_MESSAGE_DETAILS = "Error in retrieving results. Please see server log for details.";
   

    /**
   	 * Performs a search, returns the search results
   	 * @param queryText
   	 * @return
   	 */
   	@RequestMapping({ "/getResults.json" })
	public @ResponseBody
	List<ResultDocument> searchResultsAjax(
			@RequestParam("query") final String queryText) {
		return searchService.searchResultsDoc(queryText);
	}

    /**
     * Performs a search, returns the search results
     * @param requestData
     * @return
     */
    @RequestMapping({ "/getFilteredResults.json" })
    public @ResponseBody
    ResponseWithSuccessError searchFilteredResultsAjax(
            @RequestBody final SearchRequestData requestData,
            @RequestParam(value = "process") final String process,
            @RequestParam(value = "sort") final String sort,
            @RequestParam(value = "sortOrder") final String sortOrder,
            final HttpSession session) {
        try {
            requestData.setSecurtiyInfo(sessionHelper.getUserSecurityInfo(session)); 
            
            // IDOL            
            if(searchIDOL(requestData) || searchConfig.getSearchViews().get(requestData.getSearchView()).getRepository().equalsIgnoreCase("IDOL")) {
            	trainingService.processSearchRequestData(requestData);
            	ResponseWithResult<Results> result = new ResponseWithResult<Results>(searchService.searchFilteredResultsDoc(requestData));
            	return result;
            } else {
            	// Vertica
            	 requestTracker.printAllProcessRequestStates("Starting " + process);
            	 requestTracker.setRequestState(RequestProcess.valueOf(process), RequestState.ACTIVE);
            	 requestTracker.stopOtherActiveProcesses(RequestProcess.valueOf(process));
            	 
            	ResponseWithResult<Results> result =  new ResponseWithResult<Results>(searchService.searchFilteredResultsDocVertica(requestData, sort, sortOrder, adminService.getUsersRoles(sessionHelper.getRemoteUser(session))));
            	return result;
            }

        } catch(RuntimeException e) {
            LOGGER.error("Get filtered results failed.", e);
            ExceptionsHelper.checkSecurityExpiredError(e);

            return new ResponseWithSuccessError("Get filtered results failed", ERROR_MESSAGE_DETAILS);

        } finally {
        	requestTracker.setRequestState(RequestProcess.valueOf(process), RequestState.IDLE);
        	requestTracker.printAllProcessRequestStates("Exiting " + process );
            AuditLogger.log(sessionHelper.getRemoteUser(session), AuditActions.QUERY_RESULTS, requestData.getAuditData());
        }
    }
    
    private boolean searchIDOL(SearchRequestData requestData) {
    	boolean searchIdol = false;
        if(requestData.getQuery() != null && !requestData.getQuery().isEmpty() && !requestData.getQuery().equals("*")) {
        	searchIdol = true;
        }
        return searchIdol;
    }

    /**
     * Performs a search, returns the fields
     *
	 * @param requestData
	 * @return
     */
    @RequestMapping({ "/getFilteredResultFields.json" })
    public @ResponseBody
    ResponseWithSuccessError searchFilteredResultFields(
			@RequestParam(value = "values", defaultValue = "10") final int maxValues,
			@RequestBody final SearchRequestData requestData,
            @RequestParam(value = "singleQueryField", defaultValue = "false") final boolean singleQueryField,
            @RequestParam(value = "process") final String process,
            final HttpSession session) {
        try {
        	 long startTime = System.currentTimeMillis();
             requestData.setSecurtiyInfo(sessionHelper.getUserSecurityInfo(session));
             
             // IDOL
             if(searchIDOL(requestData) || searchConfig.getSearchViews().get(requestData.getSearchView()).getRepository().equalsIgnoreCase("IDOL")) {
            	trainingService.processSearchRequestData(requestData);
            	List<Field> result = searchService.aggregateParametricValuesForFilteredSearch(requestData, maxValues, requestData.getFieldNames(), null, false, true, "documentcount", singleQueryField);
            	return new ResponseWithResult<List<Field>>(result);
             } else {
            	// VERTICA           	 
            	 requestTracker.printAllProcessRequestStates("Starting " + process);
            	 requestTracker.setRequestState(RequestProcess.valueOf(process), RequestState.ACTIVE);
            	 requestTracker.stopOtherActiveProcesses(RequestProcess.valueOf(process));
            	List<com.autonomy.vertica.fields.Field> result = searchService.aggregateParametricValuesFromVertica(requestData, maxValues, requestData.getFieldNames(), null, false, true, "documentcount", 
            			singleQueryField, RequestProcess.valueOf(process));
            	calculateElapsedTime(startTime, "searchFilteredResultFields");
            	return new ResponseWithResult<List<com.autonomy.vertica.fields.Field>>(result);
             }         

        } catch (RuntimeException e) {
            LOGGER.error("Get filtered result fields failed.", e);

            ExceptionsHelper.checkSecurityExpiredError(e);

            return new ResponseWithSuccessError("Get filtered result fields failed.", ERROR_MESSAGE_DETAILS);

        } finally {
        	requestTracker.setRequestState(RequestProcess.valueOf(process), RequestState.IDLE);
        	requestTracker.printAllProcessRequestStates("Exiting " + process );
            final Map<String, Object> dataMap = AuditLogger.getDataMap();
            dataMap.put("values", maxValues);
            dataMap.put("requestData", requestData.getAuditData());

            AuditLogger.log(sessionHelper.getRemoteUser(session), AuditActions.QUERY_FIELDS, dataMap);
        }
    }
    
    private void calculateElapsedTime(long startTime, String method) {
		long elapsedTime = System.currentTimeMillis() - startTime;
		LOGGER.debug(method + " Method execution time: " + elapsedTime);		
		//return elapsedTime;
	}


    /**
     * Performs a search, returns the fields
     *
     * @param requestData
     * @return
     */
    @RequestMapping({ "/getAutocompletValues.json" })
    public @ResponseBody
    ResponseWithSuccessError getAutocompletValues (
            @RequestParam(value = "field") final String fieldname,
            @RequestParam(value = "query") final String queryChars,
            @RequestParam(value = "values", defaultValue = "-1") final int maxValues,
            @RequestParam(value = "sort", required = false) final String sorttype,
            @RequestBody final SearchRequestData requestData,
            final HttpSession session) {
        try {

            final FieldText queryFieldText =  new WILD(fieldname, queryChars );
            requestData.setSecurtiyInfo(sessionHelper.getUserSecurityInfo(session));
            
            if(searchConfig.getSearchViews().get(requestData.getSearchView()).getRepository().equalsIgnoreCase("IDOL")) {            	
            	List<Field> result = searchService.aggregateParametricValuesForFilteredSearch(requestData, maxValues, Collections.<String>singletonList(fieldname), queryFieldText, true, false, sorttype, true);
            	return new ResponseWithResult<List<Field>>(result);
             } else {
            	 Map<String, String> autoCompleteMap = new HashMap<String, String>();
            	 autoCompleteMap.put("AutoComplete_FieldName", fieldname);
            	 autoCompleteMap.put("AutoComplete_FieldValue", queryChars.trim());
            	// VERTICA
            	List<com.autonomy.vertica.fields.Field> result = searchService.aggregateParametricValuesFromVertica(requestData, maxValues, Collections.<String>singletonList(fieldname), autoCompleteMap, true, false, sorttype, true, null);
            	return new ResponseWithResult<List<com.autonomy.vertica.fields.Field>>(result);
             }   

            

        } catch (RuntimeException e) {
            LOGGER.error("Get autocomplete suggestions failed.", e);

            ExceptionsHelper.checkSecurityExpiredError(e);

            return new ResponseWithResult<List<Field>>(Collections.<Field>emptyList());

        }
    }

	/**
	 * Performs a search, looks up the network map
	 *
	 * @param maxValues
	 * @param fieldname
	 * @param requestData
	 * @return
	 */
    @RequestMapping({ "/getFilteredNetworkMap.json" })
    public @ResponseBody
	Map<String, List<FieldValue>> searchFilteredNetworkMap(
			@RequestParam(value = "values", defaultValue = "10") final int maxValues,
			@RequestParam(value = "skip", defaultValue = "0") final int skip,
			@RequestParam(value = "fieldname", required = false) final String fieldname,
			@RequestBody final SearchRequestData requestData,
            final HttpSession session) {
        trainingService.processSearchRequestData(requestData);
        return searchService.networkMap(requestData, maxValues, fieldname, skip);
    }

    @RequestMapping({ "/getTableViewer.json" })
    public @ResponseBody
    TableViewResponse getTableViewer(
            @RequestParam(value = "xField") final String xField,
            @RequestParam(value = "yField") final String yField,
            @RequestParam(value = "xFilterValue", required = false) final String xFilterValue,
            @RequestParam(value = "yFilterValue", required = false) final String yFilterValue,
            @RequestParam(value = "includeParent", defaultValue = "false") final boolean includeParent,
            @RequestParam(value = "sort") final String sort,
            @RequestParam(value = "nX", defaultValue = "20") final int nX,
            @RequestParam(value = "nY", defaultValue = "10") final int nY,
            @RequestBody final SearchRequestData requestData,
            final HttpSession session
    ) throws Exception {
    	try {
	        final String loginUser = sessionHelper.getSessionUser(session);
	
	        requestData.setSecurtiyInfo(sessionHelper.getUserSecurityInfo(session));
	        final Map<String, Object> auditData = AuditLogger.getDataMap();
	        auditData.put("xField", xField);
	        auditData.put("yField", yField);
	        auditData.put("nX", nX);
	        auditData.put("nY", nY);
	
	        AuditLogger.log(loginUser, AuditActions.TABLEVIEWER, auditData);
	
	        trainingService.processSearchRequestData(requestData);
	        if(searchIDOL(requestData) || searchConfig.getSearchViews().get(requestData.getSearchView()).getRepository().equalsIgnoreCase("IDOL")) {
	        	return searchService.tableView(requestData, xField, yField, nX, nY, sort, xFilterValue, yFilterValue, includeParent);
	        } else {
	        	return searchService.tableViewVertica(requestData, xField, yField, nX, nY, sort, xFilterValue, yFilterValue, includeParent);				
	        }
    	} catch (Exception e) {
    		// log and re throw
	        LOGGER.error(e.getMessage());	
	        //ExceptionsHelper.checkSecurityExpiredError(e);	
	        throw new Exception(e);
    	}
        
    }
    
  
	/**
	 * Gets links from/to a particular document
	 * @param reference the reference to search
	 * @return
	 */
	@RequestMapping({ "/getLinks.json" })
	public @ResponseBody
	Links getLinks(
			@RequestParam("reference") final String reference,
			@RequestParam(value="maxResults", defaultValue = "50") final int maxResults) {
		return searchService.getLinks(reference, maxResults);
	}

	/**
	 * Retrieve search suggestions for a specified term
	 * @param queryText
	 * @return
	 */
	@RequestMapping({ "/getSuggestions.json" })
	public @ResponseBody
	QuerySummaryClusters.QuerySummaryResult searchSuggestionsAjax(
			@RequestParam("query") final String queryText) {
		return searchService.searchSuggestions(queryText);
	}

    @RequestMapping({ "/getTopTitlesSummaries.json" })
    public @ResponseBody
    List<Map<String, String>> searchTopTitlesSummaries(
            @RequestBody final SearchRequestData requestData) {
        return searchService.searchTopTitlesSummaries(requestData);
    }

    @RequestMapping(value = {"/getSearchSettings.json"})
    @ResponseBody
    SearchSettings getSearchSettings(
            final HttpSession session) {

        return searchSettingsService.getSearchSettings(sessionHelper.getSessionUser(session));
    }

    @RequestMapping(value = {"/setSearchSettings.json"}, method = RequestMethod.POST)
    @ResponseBody
    SearchSettings setSearchSettings(
            @RequestBody final SearchSettings settingData,
            final HttpSession session) {

        return searchSettingsService.setSearchSettings(settingData, sessionHelper.getSessionUser(session));
    }

    @RequestMapping({ "/getTimelineTagValueDetails.json" })
    public @ResponseBody
    TimelineViewResponse getTimelineTagValueDetails(
            @RequestParam(value = "field") final String field,
            @RequestParam(value = "maxResults") final String maxResults,
            @RequestParam(value = "maxDisplayVals") final String maxDisplayVals,
            @RequestParam(value = "validStartDate") final String validStartDate,
            @RequestParam(value = "validEndDate") final String validEndDate,          
            @RequestParam(value = "baselineDate") final String baselineDate,         
            @RequestParam(value = "asOfDate") final String asOfDate,         
            @RequestParam(value = "zoomStartDate") final String zoomStartDate,         
            @RequestParam(value = "zoomEndDate") final String zoomEndDate,         
            @RequestParam(value = "valuesToRenderList") final String valuesToRenderList,         
            @RequestBody final SearchRequestData requestData,
            final HttpSession session
    ) {

        final String loginUser = sessionHelper.getSessionUser(session);

        requestData.setSecurtiyInfo(sessionHelper.getUserSecurityInfo(session));
        final Map<String, Object> auditData = AuditLogger.getDataMap();
        auditData.put("field", field);
        auditData.put("maxResults", maxResults);
        auditData.put("maxDisplayVals", maxDisplayVals);
        auditData.put("validStartDate", validStartDate);
        auditData.put("validEndDate", validEndDate);
        auditData.put("baselineDate", baselineDate);
        auditData.put("asOfDate", asOfDate);
        auditData.put("zoomStartDate", zoomStartDate);
        auditData.put("zoomEndDate", zoomEndDate);
        auditData.put("valuesToRenderList", valuesToRenderList);

        AuditLogger.log(loginUser, AuditActions.TIMELINETAGVALUEDETAILS, auditData);

        trainingService.processSearchRequestData(requestData);
        return searchService.timelineTagValueDetails(requestData, field, maxResults, maxDisplayVals,validStartDate,validEndDate,baselineDate,asOfDate,zoomStartDate,zoomEndDate,valuesToRenderList);
    }

    @RequestMapping({ "/getTimelineTagValueDeltaDetails.json" })
    public @ResponseBody
    TimelineViewResponse getTimelineTagValueDeltaDetails(
            @RequestParam(value = "field") final String field,
            @RequestParam(value = "maxResults") final String maxResults,
            @RequestParam(value = "maxDisplayVals") final String maxDisplayVals,
            @RequestParam(value = "validStartDate") final String validStartDate,
            @RequestParam(value = "validEndDate") final String validEndDate,          
            @RequestParam(value = "baselineDate") final String baselineDate,         
            @RequestParam(value = "asOfDate") final String asOfDate,         
            @RequestParam(value = "zoomStartDate") final String zoomStartDate,         
            @RequestParam(value = "zoomEndDate") final String zoomEndDate,         
            @RequestParam(value = "valuesToRenderList") final String valuesToRenderList,         
            @RequestBody final SearchRequestData requestData,
            final HttpSession session
    ) {

        final String loginUser = sessionHelper.getSessionUser(session);

        requestData.setSecurtiyInfo(sessionHelper.getUserSecurityInfo(session));
        final Map<String, Object> auditData = AuditLogger.getDataMap();
        auditData.put("field", field);
        auditData.put("maxResults", maxResults);
        auditData.put("maxDisplayVals", maxDisplayVals);
        auditData.put("validStartDate", validStartDate);
        auditData.put("validEndDate", validEndDate);
        auditData.put("baselineDate", baselineDate);
        auditData.put("asOfDate", asOfDate);
        auditData.put("zoomStartDate", zoomStartDate);
        auditData.put("zoomEndDate", zoomEndDate);
        auditData.put("valuesToRenderList", valuesToRenderList);

        AuditLogger.log(loginUser, AuditActions.TIMELINETAGVALUEDELTADETAILS, auditData);

        trainingService.processSearchRequestData(requestData);
        return searchService.timelineTagValueDeltaDetails(requestData, field, maxResults, maxDisplayVals,validStartDate,validEndDate,baselineDate,asOfDate,zoomStartDate,zoomEndDate,valuesToRenderList);
    }
    
    @RequestMapping({ "/getTrendingTotals.json" })
    public @ResponseBody
    TrendingTotalsResponse getTrendingTotals(
            @RequestParam(value = "field") final String field,
            @RequestParam(value = "graphType") final String graphType,
            @RequestParam(value = "dateField1") final String dateField1,
            @RequestParam(value = "dateField2") final String dateField2,
            @RequestParam(value = "dateStart") final String dateStart,
            @RequestParam(value = "dateEnd") final String dateEnd,
            @RequestParam(value = "sortType") final String sortType,           
            @RequestParam(value = "sortOrder") final String sortOrder,
            @RequestBody final SearchRequestData requestData,
            final HttpSession session
    ) {
        final String loginUser = sessionHelper.getSessionUser(session);
 
        requestData.setSecurtiyInfo(sessionHelper.getUserSecurityInfo(session));
        final Map<String, Object> auditData = AuditLogger.getDataMap();
        auditData.put("field", field);

        AuditLogger.log(loginUser, AuditActions.TRENDINGTOTALS, auditData);

        trainingService.processSearchRequestData(requestData);
        if(searchConfig.getSearchViews().get(requestData.getSearchView()).getRepository().equalsIgnoreCase("IDOL")) {
        	// TODO: Log Error 
        	// Not supported for IDOL View
        	return null;
        } else {
        	return searchService.trendingTotalsVertica(requestData, field, graphType, dateField1,dateField2,dateStart, dateEnd, sortType, sortOrder);
        }  
    }

    
}
