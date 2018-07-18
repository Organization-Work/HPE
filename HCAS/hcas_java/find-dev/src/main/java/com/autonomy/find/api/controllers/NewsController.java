package com.autonomy.find.api.controllers;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.autonomy.aci.actions.idol.query.QuerySummaryElement;
import com.autonomy.aci.client.services.ProcessorException;
import com.autonomy.find.api.database.UserData;
import com.autonomy.find.api.database.UserSession;
import com.autonomy.find.api.exceptions.TermsNotFoundException;
import com.autonomy.find.api.exceptions.UserSessionDoesNotExistException;
import com.autonomy.find.api.response.ResponseWithItems;
import com.autonomy.find.api.response.ResponseWithResult;
import com.autonomy.find.api.response.ResponseWithSuccessError;
import com.autonomy.find.config.FindNewsConfig;
import com.autonomy.find.dto.ClusterName;
import com.autonomy.find.dto.NewsResult;
import com.autonomy.find.dto.ResultDocument;
import com.autonomy.find.dto.SearchRequestData;
import com.autonomy.find.services.CategoryService;
import com.autonomy.find.services.HistoryService;
import com.autonomy.find.services.NewsService;
import com.autonomy.find.services.QuerySummaryClusters;
import com.autonomy.find.services.SearchService;
import com.autonomy.find.services.UserService;

@Controller
@RequestMapping("/api/news")
public class NewsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewsController.class);

    private static final String CATEGORY_SYNTAX_ERROR = "Syntax error in category display name settings file.",
            CATEGORY_FORMAT_ERROR = "Incorrect category display name settings file format.",
            CATEGORY_IO_ERROR = "Could not locate the category display name settings file.",
            UNIDENTIFIED_ERROR = "Unidentified error.",
            COULD_NOT_QUERY_ERROR = "Could not perform query.",
            TERM_METADATA_NOT_FOUND_ERROR = "Term meta data not found for category/type/cluster",
            USER_SESSION_NOT_FOUND_ERROR = "User session not found.",
            COULD_NOT_ADD_TO_USER_PROFILE_ERROR = "Could not add to user profile.";

    @Autowired
    private FindNewsConfig config;

    @Autowired
    private NewsService findNewsService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private HistoryService historyService;
    
    @Autowired
    private UserService userService;

    /**
     * getCategories
     * 
     * Loads existing categories from community Loads the cluster name to display
     * name JSON Filters the categories using the JSON map
     * 
     * Possible errors: Syntax - Unable to parse JSON Format - Error converting
     * JSON to Map (Possibly type error) IO - File not found | Cannot open file |
     * ...
     * 
     * @return
     */
    @RequestMapping("getCategories.json")
    public @ResponseBody
    ResponseWithItems<ClusterName> getCategories(
            @RequestParam(value = "session", required = false) final String session) {
        String errorMessage;

        try {
            final UserSession userSession = userService.getUserSession(session);
            final UserData user = userSession.getUserData();
            userService.refreshSession(userSession);

            // Retrieve, package, and return the categories.
            return new ResponseWithItems<ClusterName>(
                    findNewsService.getCategories(), true, null);

        } catch (final UserSessionDoesNotExistException e) {
            // If no session or invalid, return with an unsuccessful response
            LOGGER.error(USER_SESSION_NOT_FOUND_ERROR, e);
            return new ResponseWithItems<ClusterName>(
                    new LinkedList<ClusterName>(), false, USER_SESSION_NOT_FOUND_ERROR);

        } catch (final JsonParseException e) {
            // Syntax error
            errorMessage = CATEGORY_SYNTAX_ERROR;
            LOGGER.error(CATEGORY_SYNTAX_ERROR, e);

        } catch (final JsonMappingException e) {
            // Format error
            errorMessage = CATEGORY_FORMAT_ERROR;
            LOGGER.error(CATEGORY_FORMAT_ERROR, e);

        } catch (final IOException e) {
            // File not found
            errorMessage = CATEGORY_IO_ERROR;
            LOGGER.error(CATEGORY_IO_ERROR, e);
        }

        // The result in the case of an error
        return new ResponseWithItems<ClusterName>(new LinkedList<ClusterName>(),
                false, errorMessage);
    }

    @RequestMapping("getSearchSuggestions.json")
    public @ResponseBody
    ResponseWithResult<QuerySummaryClusters.QuerySummaryResult> getSearchSuggestions(
            @RequestParam(value = "session", required = false) final String session,
            @RequestParam("query") final String queryText) {

        try {
            final UserSession userSession = userService.getUserSession(session);
            final UserData user = userSession.getUserData();
            userService.refreshSession(userSession);

            // Attempt a search
            return new ResponseWithResult<QuerySummaryClusters.QuerySummaryResult>(
                    searchService.searchSuggestions(queryText), true, null);

        } catch (final UserSessionDoesNotExistException e) {
            // If no session or invalid, return with an unsuccessful response
            LOGGER.error(USER_SESSION_NOT_FOUND_ERROR, e);
            return new ResponseWithResult<QuerySummaryClusters.QuerySummaryResult>(
                    QuerySummaryClusters.QuerySummaryResult.create(), false, USER_SESSION_NOT_FOUND_ERROR);

        } catch (final Exception e) {
            // If an error occurs, return with an unsuccessful response
            LOGGER.error(UNIDENTIFIED_ERROR, e);
            return new ResponseWithResult<QuerySummaryClusters.QuerySummaryResult>(
                    QuerySummaryClusters.QuerySummaryResult.create(), false, UNIDENTIFIED_ERROR);
        }
    }

    @RequestMapping("getSearchSuggestionsList.json")
    public @ResponseBody
    ResponseWithItems<String> getSearchSuggestionsList(
            @RequestParam(value = "session", required = false) final String session,
            @RequestParam(value = "query", required = true) final String queryText,
            @RequestParam(value = "maxResults", required = false) final Integer maxResults) {

        try {
            final UserSession userSession = userService.getUserSession(session);
            final UserData user = userSession.getUserData();
            userService.refreshSession(userSession);

            // Attempt a search
            return new ResponseWithItems<String>(
                    maxResults == null
                    ? searchService.getRelatedConcepts(queryText)
                    : searchService.getRelatedConceptsCapped(queryText, maxResults), true, null);

        } catch (final UserSessionDoesNotExistException e) {
            // If no session or invalid, return with an unsuccessful response
            LOGGER.error(USER_SESSION_NOT_FOUND_ERROR, e);
            return new ResponseWithItems<String>(
                    new LinkedList<String>(), false, USER_SESSION_NOT_FOUND_ERROR);

        } catch (final Exception e) {
            // If an error occurs, return with an unsuccessful response
            LOGGER.error(UNIDENTIFIED_ERROR, e);
            return new ResponseWithItems<String>(new LinkedList<String>(), false,
                    UNIDENTIFIED_ERROR);
        }
    }

    @RequestMapping("search.json")
    public @ResponseBody
    ResponseWithItems<ResultDocument> search(
            @RequestParam(value = "session", required = false) final String session,
            @RequestParam(value = "query", required = true) final String queryText,
            @RequestParam(value = "maxResults", required = false) final Integer maxResults,
            @RequestParam(value = "tag", required = false, defaultValue = "false") final boolean tag,
            @RequestParam(value = "addToHistory", required = false, defaultValue = "true") final boolean addToHistory,
            @RequestParam(value = "trailingEnd", required = false, defaultValue = "false") final boolean trailingEnd)  {


        try {
            final UserSession userSession = userService.getUserSession(session);
            final UserData user = userSession.getUserData();
            userService.refreshSession(userSession);

            final int searchMaxResults = ((maxResults != null) ? maxResults : config.getSearchMaxResults());
            
            // Attempt a search
            final List<ResultDocument> searchResult = searchService.searchResultsDB(new SearchRequestData(queryText, new HashMap<>()), config.getSearchDatabase(), "" + searchMaxResults);
            
            // Save search
            if (addToHistory) {
                historyService.saveSearch(user.getId().toString(), queryText);
            }
            
            ResultDocument.process(searchResult, trailingEnd, false, tag);
            
            return new ResponseWithItems<ResultDocument>(searchResult, true, null);
            
        } catch (final UserSessionDoesNotExistException e) {
            // If no session or invalid, return with an unsuccessful response
            LOGGER.error(USER_SESSION_NOT_FOUND_ERROR, e);
            return new ResponseWithItems<ResultDocument>(
                    new LinkedList<ResultDocument>(), false, USER_SESSION_NOT_FOUND_ERROR);
            
        } catch (final Exception e) {
            // If an error occurs, return with an unsuccessful response
            LOGGER.error(UNIDENTIFIED_ERROR, e);
            return new ResponseWithItems<ResultDocument>(
                    new LinkedList<ResultDocument>(), false, UNIDENTIFIED_ERROR);
        }
    }

    @RequestMapping("getTopSearchConcepts.json")
    public @ResponseBody
    ResponseWithItems<QuerySummaryElement> getTopSearchConcepts(
            @RequestParam(value = "session", required = false) final String session,
            @RequestParam(value = "maxResults", required = true) final Integer maxResults) {

        try {
            final UserSession userSession = userService.getUserSession(session);
            final UserData user = userSession.getUserData();
            userService.refreshSession(userSession);

            return new ResponseWithItems<QuerySummaryElement>(
            	historyService.getTopSearchConcepts(maxResults), true, null);

        } catch(final UserSessionDoesNotExistException e) {
            // If no session or invalid, return with an unsuccessful response
            LOGGER.error(USER_SESSION_NOT_FOUND_ERROR, e);
            return new ResponseWithItems<QuerySummaryElement> (
                    new LinkedList <QuerySummaryElement> (), false, USER_SESSION_NOT_FOUND_ERROR);
        }
    }

    @RequestMapping("getTopSearchConceptsList.json")
    public @ResponseBody
    ResponseWithItems<String> getTopSearchConceptsList(
            @RequestParam(value = "session", required = false) final String session,
            @RequestParam(value = "maxResults", required = true) final int maxResults) {

        try {
            final UserSession userSession = userService.getUserSession(session);
            final UserData user = userSession.getUserData();
            userService.refreshSession(userSession);

            final List<QuerySummaryElement> elements = historyService
                    .getTopSearchConcepts(maxResults);
            final List<String> results = new LinkedList<String>();

            for (final QuerySummaryElement element : elements) {
                results.add(element.getValue());
            }

            return new ResponseWithItems<String>(results, true, null);

        } catch (final UserSessionDoesNotExistException e) {
            // If no session or invalid, return with an unsuccessful response
            LOGGER.error(USER_SESSION_NOT_FOUND_ERROR, e);
            return new ResponseWithItems<String>(
                    new LinkedList<String>(), false, USER_SESSION_NOT_FOUND_ERROR);
        }
    }

    /**
     * 
     * @param session
     * @param types
     * @param categories
     * @param maxResults
     * @param headlines
     * @return
     */
    @RequestMapping("getNews.json")
    public @ResponseBody
    ResponseWithItems<NewsResult> getNews(
            @RequestParam(value = "session", required = false) final String session,
            @RequestParam(value = "types[]", required = false) final List<String> types,
            @RequestParam(value = "categories[]", required = false) final List<String> categories,
            @RequestParam(value = "maxResults", required = false) final Integer maxResults,
            @RequestParam(value = "headlines", required = false) final Boolean headlines,
            @RequestParam(value = "startDate", required = false) final Long startDate,
            @RequestParam(value = "endDate", required = false) final Long endDate,
            @RequestParam(value = "interval", required = false) final Long interval,
            @RequestParam(value = "tag", required = false, defaultValue = "false") final boolean tag,
            @RequestParam(value = "trailingEnd", required = false, defaultValue = "false") final boolean trailingEnd) {
        
        String errorMessage;

        try {
            final UserSession userSession = userService.getUserSession(session);
            final UserData user = userSession.getUserData();
            userService.refreshSession(userSession);

            final List<NewsResult> results = findNewsService.getNews(
                    typesOrDefault(types), categoriesOrDefault(categories),
                    maxResultsOrDefault(maxResults),
                    headlinesOrDefault(headlines), startDate, endDate,
                    interval, true);

            final Set<ResultDocument> docs = new HashSet<ResultDocument>();
            
            //for (final NewsResult newsResult : results) {
            	//                for (final NewsResultCluster cluster : newsResult.getClusters()) {
            	//                    docs.addAll(cluster.getDocuments());
                // }
            //}

            ResultDocument.process(docs, trailingEnd, false, tag);

            // Attempt to retrieve the news
            return new ResponseWithItems<NewsResult>(results, true, null);

        } catch (final UserSessionDoesNotExistException e) {
            // If no session or invalid, return with an unsuccessful response
            LOGGER.error(USER_SESSION_NOT_FOUND_ERROR, e);
            return new ResponseWithItems<NewsResult>(
                    new LinkedList<NewsResult>(), false, USER_SESSION_NOT_FOUND_ERROR);

        } catch (JsonParseException e) {
            // Syntax error
            errorMessage = CATEGORY_SYNTAX_ERROR;
            LOGGER.error(CATEGORY_SYNTAX_ERROR, e);

        } catch (final JsonMappingException e) {
            // Format error
            errorMessage = CATEGORY_FORMAT_ERROR;
            LOGGER.error(CATEGORY_FORMAT_ERROR, e);

        } catch (final IOException e) {
            // File not found
            errorMessage = CATEGORY_IO_ERROR;
            LOGGER.error(CATEGORY_IO_ERROR, e);

        } catch (final Exception e) {
            errorMessage = UNIDENTIFIED_ERROR;
            LOGGER.error(UNIDENTIFIED_ERROR, e);
        }

        // The result in the case of an error
        return new ResponseWithItems<NewsResult>(new LinkedList<NewsResult>(),
                false, errorMessage);
    }

    /**
     * Provides default value if types is null
     * 
     * @param types
     * @return
     */
    private List<String> typesOrDefault(final List<String> types) {
        return (types == null || types.isEmpty()) ? Arrays.asList("popular", "breaking") : types;
    }

    /**
     * Provides default value if categories is null
     * 
     * @param categories
     * @return
     */
    private List<String> categoriesOrDefault(final List<String> categories) {
        return (categories == null) ? new LinkedList<String>() : categories;
    }

    /**
     * Provides default value if maxResults is null
     * 
     * @param maxResults
     * @return
     */
    private int maxResultsOrDefault(final Integer maxResults) {
        return (maxResults == null) ? config.getDefaultMaxResults() : maxResults;
    }

    /**
     * Provides default value if headlines is null
     * 
     * @param headlines
     * @return
     */
    private boolean headlinesOrDefault(final Boolean headlines) {
        return (headlines == null) ? config.isDefaultHeadlines() : headlines;
    }

    /**
     * Add document to current user profile
     * 
     * @param documents
     * @return
     */
    @RequestMapping("addProfileDocuments.json")
    public @ResponseBody
    ResponseWithSuccessError addProfileDocuments(
            @RequestParam(value = "session", required = false) final String session,
            @RequestParam(value = "documents[]", required = true) final List<String> documents) {

        try {
            final UserSession userSession = userService.getUserSession(session);
            final UserData user = userSession.getUserData();
            userService.refreshSession(userSession);

            findNewsService.addProfileDocuments(user.getId().toString(), documents);
        }
        catch (final UserSessionDoesNotExistException e) {
            // If no session or invalid, return with an unsuccessful response
            LOGGER.error(USER_SESSION_NOT_FOUND_ERROR, e);
            return new ResponseWithItems<ResultDocument>(
                    new LinkedList<ResultDocument>(), false, USER_SESSION_NOT_FOUND_ERROR);

        }
        catch (final ProcessorException e) {
            LOGGER.error(COULD_NOT_ADD_TO_USER_PROFILE_ERROR, e);
            return new ResponseWithSuccessError(false, COULD_NOT_ADD_TO_USER_PROFILE_ERROR);
        }
        catch (final Exception e) {
            LOGGER.error(USER_SESSION_NOT_FOUND_ERROR, e);
            return new ResponseWithSuccessError(false, USER_SESSION_NOT_FOUND_ERROR);
        }

        return new ResponseWithSuccessError(true, null);
    }


    @RequestMapping("queryTopicDocs.json")
    public @ResponseBody
    ResponseWithSuccessError queryTopicDocs(
            @RequestParam(value = "session", required = false) final String session,
            @RequestParam(value = "query[]", required = false) final List<String> query,
            @RequestParam(value = "pageNum", required = false, defaultValue = "0") final int pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "6") final int pageSize,
            @RequestParam(value = "daysBack", required = false, defaultValue = "7") final int daysBack
    ) {

        try {
            final UserSession userSession = userService.getUserSession(session);
            userService.refreshSession(userSession);
        }
        catch (final Exception e) {
            LOGGER.error(USER_SESSION_NOT_FOUND_ERROR, e);
            return new ResponseWithSuccessError(USER_SESSION_NOT_FOUND_ERROR);
        }

        try { return new ResponseWithResult<>(findNewsService.queryTopicDocs(query, pageSize, pageNum, daysBack)); }
        catch (final Exception e) {
            LOGGER.error(COULD_NOT_QUERY_ERROR, e);
            return new ResponseWithSuccessError(COULD_NOT_QUERY_ERROR);
        }
    }


    /**
     *
     * @param session
     * @param clusterTitle
     * @param endDate
     * @param days
     * @return
     */
    @RequestMapping("getThemeTimeline.json")
    public @ResponseBody
    ResponseWithItems<NewsService.DateTag> getThemeTimeline(
            @RequestParam(value = "session", required = false) final String session,
            @RequestParam(value = "clusterTitle", required = true) String clusterTitle,
            @RequestParam(value = "endDate", required = false) Long endDate,
            @RequestParam(value = "days", required = false, defaultValue = "7") int days) {

        try {
            final UserSession userSession = userService.getUserSession(session);
            final UserData user = userSession.getUserData();
            userService.refreshSession(userSession);

            return new ResponseWithItems<NewsService.DateTag>(
                    findNewsService.getThemeTimeline(clusterTitle, endDate, days), true, null);

        } catch (final UserSessionDoesNotExistException e) {
            // If no session or invalid, return with an unsuccessful response
            LOGGER.error(USER_SESSION_NOT_FOUND_ERROR, e);
            return new ResponseWithItems<NewsService.DateTag>(
                    new LinkedList<NewsService.DateTag>(), false, USER_SESSION_NOT_FOUND_ERROR);

        } catch (final Exception e) {
            LOGGER.error(UNIDENTIFIED_ERROR, e);
            return new ResponseWithItems<NewsService.DateTag>(
                    new LinkedList<NewsService.DateTag>(), false, e.toString());
        }
    }
    
    /**
     *
     * @param session
     * @param queryText
     * @param endDate
     * @param days
     * @return
     */
    @RequestMapping("getSearchTimeline.json")
    public @ResponseBody
    ResponseWithItems<NewsService.DateTag> getSearchTimeline(
            @RequestParam(value = "session", required = false) final String session,
            @RequestParam(value = "query", required = true) String queryText,
            @RequestParam(value = "endDate", required = false) Long endDate,
            @RequestParam(value = "days", required = false, defaultValue = "7") int days) {

        try {
            final UserSession userSession = userService.getUserSession(session);
            final UserData user = userSession.getUserData();
            userService.refreshSession(userSession);

            return new ResponseWithItems<NewsService.DateTag>(findNewsService.getSearchTimeline(queryText, endDate, days), true, null);
        }
        catch (final UserSessionDoesNotExistException e) {
            // If no session or invalid, return with an unsuccessful response
            LOGGER.error(USER_SESSION_NOT_FOUND_ERROR, e);
            return new ResponseWithItems<NewsService.DateTag>(
                    new LinkedList<NewsService.DateTag>(), false, USER_SESSION_NOT_FOUND_ERROR);

        }
    }

    /**
     * 
     * @param clusterTitle
     * @param endDate
     * @param days
     * @return
     */
    @RequestMapping("getThemeTimelineCountList.json")
    public @ResponseBody
    ResponseWithItems<Integer> getThemeTimelineCountList(
            @RequestParam(value = "session", required = false) final String session,
            @RequestParam(value = "clusterTitle", required = true) String clusterTitle,
            @RequestParam(value = "endDate", required = false) Long endDate,
            @RequestParam(value = "days", required = false, defaultValue = "7") int days) {

        try {
            final UserSession userSession = userService.getUserSession(session);
            final UserData user = userSession.getUserData();
            userService.refreshSession(userSession);

            return new ResponseWithItems<Integer>(findNewsService.getThemeTimelineCountList(clusterTitle, endDate, days), true, null);

        } catch (final UserSessionDoesNotExistException e) {
            // If no session or invalid, return with an unsuccessful response
            LOGGER.error(USER_SESSION_NOT_FOUND_ERROR, e);
            return new ResponseWithItems<Integer>(
                    new LinkedList<Integer>(), false, USER_SESSION_NOT_FOUND_ERROR);

        } catch (final Exception e) {
            LOGGER.error(UNIDENTIFIED_ERROR, e);
            return new ResponseWithItems<Integer>(new LinkedList<Integer>(), false, e.toString());
        }
    }
    
    /**
     * 
     * @param queryText
     * @param endDate
     * @param days
     * @return
     */
    @RequestMapping("getSearchTimelineCountList.json")
    public @ResponseBody
    ResponseWithItems<Integer> getSearchTimelineCountList(
            @RequestParam(value = "session", required = false) final String session,
            @RequestParam(value = "query", required = true) String queryText,
            @RequestParam(value = "endDate", required = false) Long endDate,
            @RequestParam(value = "days", required = false, defaultValue = "7") int days) {

        try {
            final UserSession userSession = userService.getUserSession(session);
            final UserData user = userSession.getUserData();
            userService.refreshSession(userSession);

            return new ResponseWithItems<Integer>(findNewsService.getSearchTimelineCountList(queryText, endDate, days), true, null);

        } catch (final UserSessionDoesNotExistException e) {
            // If no session or invalid, return with an unsuccessful response
            LOGGER.error(USER_SESSION_NOT_FOUND_ERROR, e);
            return new ResponseWithItems<Integer>(
                    new LinkedList<Integer>(), false, USER_SESSION_NOT_FOUND_ERROR);
        }
    }
    
    @RequestMapping("getTermCacheTitles.json")
    public @ResponseBody
    ResponseWithItems<String> getTermCacheTitles(
            @RequestParam(value = "session", required = true) final String session) {
        try {
            final UserSession userSession = userService.getUserSession(session);
            final UserData user = userSession.getUserData();
            userService.refreshSession(userSession);

            return new ResponseWithItems<String>(new LinkedList<String>(findNewsService.getTermCacheTitles()), true, null);

        } catch (final UserSessionDoesNotExistException e) {
            // If no session or invalid, return with an unsuccessful response
            LOGGER.error(USER_SESSION_NOT_FOUND_ERROR, e);
            return new ResponseWithItems<String>(
                    new LinkedList<String>(), false, USER_SESSION_NOT_FOUND_ERROR);
        }
    }

    @RequestMapping("getTermsForCluster.json")
    public @ResponseBody
    ResponseWithResult<String> getTermCacheTitles(
            @RequestParam(value = "session", required = false) final String session,
            @RequestParam("clusterTitle") final String clusterTitle) {
        try {
            final UserSession userSession = userService.getUserSession(session);
            final UserData user = userSession.getUserData();
            userService.refreshSession(userSession);

            return new ResponseWithResult<String>(
                    findNewsService.getTermsForClusterTitle(clusterTitle),
                    true, null);

        }
        catch (final UserSessionDoesNotExistException e) {
            // If no session or invalid, return with an unsuccessful response
            LOGGER.error(USER_SESSION_NOT_FOUND_ERROR, e);
            return new ResponseWithResult<String>(
                    "", false, USER_SESSION_NOT_FOUND_ERROR);

        }
        catch (final TermsNotFoundException e) {
            LOGGER.error("Terms not found in database.", e);
            return new ResponseWithResult<String>("", false, "Terms not found in database.");
        }
    }
}
