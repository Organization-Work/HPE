package com.autonomy.find.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.autonomy.aci.actions.idol.query.QuerySummaryElement;
import com.autonomy.aci.actions.idol.tag.FieldValue;
import com.autonomy.find.api.exceptions.UserSessionDoesNotExistException;
import com.autonomy.find.config.HistoryConfig;
import com.autonomy.find.services.HistoryService;
import com.autonomy.find.util.SessionHelper;

@Controller
@RequestMapping("/p/ajax/searchhistory")
public class HistoryController {

	private static final String	DEFAULT_NUM_RESULTS	= "3";	// TODO this should be in a config file

	@Autowired
	private SessionHelper		sessionHelper;

	@Autowired
	private HistoryService		searchHistory;

	@Autowired
	private HistoryConfig		historyConfig;

	@RequestMapping("/getRecentSearchesForCurrentUser.json")
	public @ResponseBody
	List<String> getRecentSearchesForUser(final HttpSession session, @RequestParam(defaultValue = DEFAULT_NUM_RESULTS) final int numSearches) throws UserSessionDoesNotExistException {

		final String user = sessionHelper.getRemoteUser(session);
		return searchHistory.getSearchTerms(searchHistory.getRecentSearchesForUser(user, numSearches));
	}

	/**
	 * Relies on storing the SearchText as a Parametric field in Content.  Deprecated in favour of getTopConcepts(), which produces more meaningful results.
	 * @param numSearches
	 * @return
	 */
	@Deprecated
	@RequestMapping("/getTopSearches.json")
	public @ResponseBody
	List<FieldValue> getTopSearches(@RequestParam(defaultValue = DEFAULT_NUM_RESULTS) final int numSearches) {

		return searchHistory.getTopSearches(numSearches);
	}

	@RequestMapping("/getTopConcepts.json")
	public @ResponseBody
	List<QuerySummaryElement> getTopSearchConcepts(@RequestParam(defaultValue = DEFAULT_NUM_RESULTS) final int numSearches) {

		return searchHistory.getTopSearchConcepts(numSearches);
	}

	@RequestMapping("/getAllSearchHistory.json")
	public @ResponseBody
	Map<String, Object> getRecentFrequent(final HttpSession session, @RequestParam(defaultValue = DEFAULT_NUM_RESULTS) final int numSearches) throws UserSessionDoesNotExistException {

		final String user = sessionHelper.getRemoteUser(session);

		final Map<String, Object> output = new HashMap<String, Object>();

		output.put("recent", searchHistory.getSearchTerms(searchHistory.getRecentSearchesForUser(user, numSearches)));
		//output.put("frequent", searchHistory.getTopSearches(numSearches));
		output.put("topConcepts", searchHistory.getTopSearchConcepts(numSearches));

		return output;
	}

	@RequestMapping("/saveToHistory.json")
	public @ResponseBody
	boolean saveToHistory(final HttpSession session, final String search) {
		final String user = sessionHelper.getRemoteUser(session);
        return searchHistory.saveSearch(user, search);
	}

}
