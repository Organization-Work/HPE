package com.autonomy.find.controllers;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.autonomy.find.config.SearchConfig;
import com.autonomy.find.dto.UserSettings;
import com.autonomy.find.services.WebAppUserService;
import com.autonomy.find.util.SessionHelper;

@Controller
@RequestMapping("/p/ajax/settings")
public class SettingsController {

	@Autowired
	private SessionHelper sessionHelper;

	@Autowired
	private WebAppUserService userService;

	@Autowired
	private SearchConfig config;

	/**
	 * Performs a search, returns the search results
	 * 
	 * @param queryText
	 * @return
	 */
	@RequestMapping({ "/updateSettings.json" })
	public @ResponseBody
	boolean updateSettings(final HttpSession session,
			@RequestParam("displayCharacters") final Integer displayCharacters,
			@RequestParam("databases") final String databases,
			@RequestParam("suggestionDatabases") final String suggestionDatabases,
			@RequestParam(value = "minScore", defaultValue = "0") final double minScore) {

		final UserSettings settings = new UserSettings(
				displayCharacters.toString(), databases, suggestionDatabases, null, null, minScore);

		return userService.updateSettings(sessionHelper.getRemoteUser(session),
				settings);
	}

	@RequestMapping({ "/retrieveSettings.json" })
	public @ResponseBody
	UserSettings retrieveSettings(final HttpSession session) {
		final UserSettings settings = userService.retrieveSettings(sessionHelper.getRemoteUser(session));
		settings.setAllDatabase(config.getDatabases());
		settings.setAllSuggestionDatabases(config.getSuggestionDatabases());
		return settings;
	}
}
