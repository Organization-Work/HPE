package com.autonomy.find.dto;

import com.autonomy.aci.client.annotations.IdolDocument;
import com.autonomy.aci.client.annotations.IdolField;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.find.config.SearchConfig;
import org.apache.commons.lang.math.NumberUtils;

@IdolDocument("autn:fields")
public class UserSettings {

	private static final String USERNAME = "username";
	
	private static final String FIELD_DISPLAY_CHARS = "Fieldfind_displayCharacters";
	private static final String FIELD_DATABASES = "Fieldfind_databases";
	private static final String FIELD_SUGGESTION_DATABASES = "Fieldfind_suggestionDatabases";
	private static final String FIELD_MINSCORE = "Fieldfind_minscore";

	private static final String DISPLAY_CHARS = "find_displaycharacters";
	private static final String DATABASES = "find_databases";
	private static final String SUGGESTION_DATABASES = "find_suggestiondatabases";
	private static final String MINSCORE = "find_minscore";

	private String displayCharacters;
	private String databases;
	private String suggestionDatabases;
	private String allDatabases;
	private String allSuggestionDatabases;
	private double minScore = 0;

	public UserSettings() {
	}

	public UserSettings(
			final String displayCharacters,
			final String databases,
			final String suggestionDatabases,
			final String allDatabases,
			final String allSuggestionDatabases,
			final double minScore) {

		this.displayCharacters = displayCharacters;
		this.databases = databases;
		this.suggestionDatabases = suggestionDatabases;
		this.allDatabases = allDatabases;
		this.allSuggestionDatabases = allSuggestionDatabases;
		this.minScore = minScore;
	}

	@IdolField(DISPLAY_CHARS)
	public void setDisplayCharacters(final String value) {
		this.displayCharacters = value;
	}

	public String getDisplayCharacters() {
		return this.displayCharacters;
	}

	@IdolField(DATABASES)
	public void setDatabases(final String value) {
		this.databases = value;
	}

	public String getDatabases() {
		return this.databases;
	}

	@IdolField(SUGGESTION_DATABASES)
	public void setSuggestionDatabases(final String value) {
		this.suggestionDatabases = value;
	}

	public String getSuggestionDatabases() {
		return this.suggestionDatabases;
	}
	
	public void setAllDatabase(final String allDatabases) {
		this.allDatabases = allDatabases;
	}
	
	public String getAllDatabases() {
		return this.allDatabases;
	}
	
	public void setAllSuggestionDatabases(final String allSuggestionDatabases) {
		this.allSuggestionDatabases = allSuggestionDatabases;
	}
	
	public String getAllSuggestionDatabases() {
		return this.allSuggestionDatabases;
	}

	public double getMinScore() {
		return minScore;
	}

	@IdolField(MINSCORE)
	public void setMinScore(final String minScore) {
		this.setMinScore(NumberUtils.toDouble(minScore));
	}

	public void setMinScore(final double minScore) {
		this.minScore = minScore;
	}

	public AciParameters asAciParams(final String username) {
		final AciParameters params = new AciParameters("UserEdit");
		params.put(USERNAME, username);
		params.put(FIELD_DISPLAY_CHARS, this.displayCharacters);
		params.put(FIELD_DATABASES, this.databases);
		params.put(FIELD_SUGGESTION_DATABASES, this.suggestionDatabases);
		params.put(FIELD_MINSCORE, this.minScore);
		return params;
	}

	public static AciParameters paramsForNewUser(final SearchConfig config) {
		final AciParameters params = new AciParameters();
		params.put(FIELD_DISPLAY_CHARS, config.getDisplayCharacters());
		params.put(FIELD_DATABASES, config.getDatabases());
		params.put(FIELD_SUGGESTION_DATABASES,
				config.getSuggestionDatabases());
		params.put(FIELD_MINSCORE, config.getMinScore());
		return params;
	}
}
