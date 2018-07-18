package com.autonomy.find.config;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.autonomy.find.dto.Parametric.TopicBranch;
import com.autonomy.vertica.common.Group;

import lombok.Data;

@Data
public final class SearchConfig {

    public boolean isShowLinks() {
		return showLinks;
	}

	public void setShowLinks(boolean showLinks) {
		this.showLinks = showLinks;
	}

	public void setLanguageType(String languageType) {
		this.languageType = languageType;
	}

	public void setLinkToField(String linkToField) {
		this.linkToField = linkToField;
	}

	public void setLinkIdField(String linkIdField) {
		this.linkIdField = linkIdField;
	}

	public void setShowScore(boolean showScore) {
		this.showScore = showScore;
	}

	public void setTotalResults(boolean totalResults) {
		this.totalResults = totalResults;
	}

	public void setTotalResultsPredict(boolean totalResultsPredict) {
		this.totalResultsPredict = totalResultsPredict;
	}

	public void setOutputEncoding(String outputEncoding) {
		this.outputEncoding = outputEncoding;
	}

	public void setMaxSearchResults(String maxSearchResults) {
		this.maxSearchResults = maxSearchResults;
	}

	public void setMaxPreviewResults(String maxPreviewResults) {
		this.maxPreviewResults = maxPreviewResults;
	}

	public void setMaxSuggestionResults(String maxSuggestionResults) {
		this.maxSuggestionResults = maxSuggestionResults;
	}

	public void setSuggestionDatabases(String suggestionDatabases) {
		this.suggestionDatabases = suggestionDatabases;
	}

	public void setMaxSuggestionChildren(int maxSuggestionChildren) {
		this.maxSuggestionChildren = maxSuggestionChildren;
	}

	public void setDisplayCharacters(int displayCharacters) {
		this.displayCharacters = displayCharacters;
	}

	public void setMinScore(double minScore) {
		this.minScore = minScore;
	}

	public void setMeta(Map<String, String> meta) {
		this.meta = meta;
	}

	/*public void setDisplayFields(Map<String, DisplayField> displayFields) {
		this.displayFields = displayFields;
	}*/

	public void setCulledSuperCategories(Set<String> culledSuperCategories) {
		this.culledSuperCategories = culledSuperCategories;
	}

	public void setShowNetworkMap(boolean showNetworkMap) {
		this.showNetworkMap = showNetworkMap;
	}

	public void setDefaultTopicBranch(TopicBranch defaultTopicBranch) {
		this.defaultTopicBranch = defaultTopicBranch;
	}

	public void setDocfolderMaxResults(int docfolderMaxResults) {
		this.docfolderMaxResults = docfolderMaxResults;
	}

	public void setCombine(String combine) {
		this.combine = combine;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public void setSuperUserDefaultPassword(String superUserDefaultPassword) {
		this.superUserDefaultPassword = superUserDefaultPassword;
	}

	public void setDefaultLinkContentTemplate(String defaultLinkContentTemplate) {
		this.defaultLinkContentTemplate = defaultLinkContentTemplate;
	}

	public void setTopicmapMaxValues(int topicmapMaxValues) {
		this.topicmapMaxValues = topicmapMaxValues;
	}

	public void setSunburstMaxValues(int sunburstMaxValues) {
		this.sunburstMaxValues = sunburstMaxValues;
	}

	public void setParametricSingleQueryFieldname(
			boolean parametricSingleQueryFieldname) {
		this.parametricSingleQueryFieldname = parametricSingleQueryFieldname;
	}

	public void setSunburstSingleQueryFieldname(boolean sunburstSingleQueryFieldname) {
		this.sunburstSingleQueryFieldname = sunburstSingleQueryFieldname;
	}

	public void setPreloadParaValues(boolean preloadParaValues) {
		this.preloadParaValues = preloadParaValues;
	}
	
	public void setGroupFields(Map<String, Group> groupFields) {
		this.groupFields = groupFields;
	}
	
	public void setTextSearchEnabled(boolean textSearchEnabled) {
		this.textSearchEnabled = textSearchEnabled;
	}

	private String languageType;
	private String linkToField;
	private String linkIdField;
	private boolean showLinks;
	private boolean showScore;
	private boolean totalResults;
	private boolean totalResultsPredict;
    private String outputEncoding;
    private String maxSearchResults;
    private String maxPreviewResults;
    private String maxSuggestionResults;
    private Map<String, SearchView> searchViews;
    private String suggestionDatabases;
    private int maxSuggestionChildren;
    private int displayCharacters;
	private double minScore;
    private Map<String, String> meta;
    //private Map<String, DisplayField> displayFields;
	private Set<String> culledSuperCategories;
    private boolean showNetworkMap;
    private TopicBranch defaultTopicBranch;
    private int docfolderMaxResults;
    private String combine;
    private String summary;
    private String superUserDefaultPassword;
    private String defaultLinkContentTemplate;
    private int topicmapMaxValues;
    private int sunburstMaxValues;
    private boolean parametricSingleQueryFieldname;
    private boolean sunburstSingleQueryFieldname;
    private boolean preloadParaValues;
    private Map<String, Group> groupFields;
    private boolean textSearchEnabled;
    private String folderDBURL;
    private boolean isFolderStoredInVertica;
    

    public SearchConfig(
			final String languageType,
			final String linkToField,
			final String linkIdField,
			final String outputEncoding,
			final String maxSearchResults,
			final String maxPreviewResults,
			final String maxSuggestionResults,
			final Map<String, SearchView> searchViews,
			final String suggestionDatabases,
			final int maxSuggestionChildren,
			final int displayCharacters,
			final Map<String, String> meta,
			//final Map<String, DisplayField> displayFields,
			final double minScore,
			final boolean showScore,
			final boolean totalResults,
			final boolean totalResultsPredict,
			final Set<String> culledSuperCategories,
            final boolean showNetworkMap,
            final TopicBranch defaultTopicBranch,
            final int docfolderMaxResults,
            final String combine,
            final String summary,
            final String superUserDefaultPassword,
            final String defaultLinkContentTemplate,
            final int topicmapMaxValues,
            final int sunburstMaxValues,
            final boolean parametricSingleQueryFieldname,
            final boolean sunburstSingleQueryFieldname,
            final boolean preloadParaValues,
            final Map<String, Group> groupFields,
            final boolean textSearchEnabled,
            final String folderDBURL
            ) {
        this.languageType = languageType;
		this.linkToField = linkToField;
		this.linkIdField = linkIdField;
		this.showLinks = StringUtils.isNotBlank(this.linkToField) && StringUtils.isNotBlank(this.linkIdField);
		this.outputEncoding = outputEncoding;
        this.maxSearchResults = maxSearchResults;
        this.maxPreviewResults = maxPreviewResults;
        this.maxSuggestionResults = maxSuggestionResults;
        this.searchViews = searchViews;
        this.suggestionDatabases = suggestionDatabases;
        this.maxSuggestionChildren = maxSuggestionChildren;
        this.displayCharacters = displayCharacters;
        this.meta = meta;
		//this.displayFields = displayFields;
		this.minScore = minScore;
		this.showScore = showScore;
		this.totalResults = totalResults;
		this.totalResultsPredict = totalResultsPredict;
		this.culledSuperCategories = culledSuperCategories;
        this.showNetworkMap = showNetworkMap;
        this.defaultTopicBranch = defaultTopicBranch;
        this.docfolderMaxResults = docfolderMaxResults;
        this.combine = combine;
        this.summary = summary;
        this.superUserDefaultPassword = superUserDefaultPassword;
        this.defaultLinkContentTemplate = defaultLinkContentTemplate;
        this.topicmapMaxValues = topicmapMaxValues;
        this.sunburstMaxValues = sunburstMaxValues;
        this.parametricSingleQueryFieldname = parametricSingleQueryFieldname;
        this.sunburstSingleQueryFieldname = sunburstSingleQueryFieldname;
        this.preloadParaValues = preloadParaValues;
        this.groupFields = groupFields;
        this.textSearchEnabled = textSearchEnabled;
        this.folderDBURL = folderDBURL;
        this.isFolderStoredInVertica = StringUtils.contains(this.folderDBURL, "vertica") ? true : false;
    }

    public SearchConfig(
            final String languageType,
			final String linkToField,
			final String linkIdField,
            final String outputEncoding,
            final String maxSearchResults,
            final String maxPreviewResults,
            final String maxSuggestionResults,
            final String searchViewsJSON,
            final String suggestionDatabases,
            final int maxSuggestionChildren,
            final int displayCharacters,
            final String metaJSON,
			//final String displayFieldsJSON,
			final double minScore,
			final boolean showScore,
			final boolean totalResults,
			final boolean totalResultsPredict,
			final String culledSuperCategoriesJSON,
            final boolean showNetworkMap,
            final String defaultTopicBranch,
            final int docfolderMaxResults,
            final String combine,
            final String summary,
            final String superUserDefaultPassword,
            final String defaultLinkContentTemplate,
            final int topicmapMaxValues,
            final int sunburstMaxValues,
            final boolean parametricSingleQueryFieldname,
            final boolean sunburstSingleQueryFieldname,
            final boolean preloadParaValues,
            final String groupFieldsJSON,
            final boolean textSearchEnabled,
            final String folderDBURL
    ) throws IOException {
		this(languageType,
				linkToField,
				linkIdField,
				outputEncoding,
				maxSearchResults,
				maxPreviewResults,
				maxSuggestionResults,
                new ObjectMapper().<Map<String, SearchView>>readValue(searchViewsJSON, new TypeReference<Map<String, SearchView>>() {}),
				suggestionDatabases,
				maxSuggestionChildren,
				displayCharacters,
				new ObjectMapper().<Map<String, String>>readValue(metaJSON, new TypeReference<Map<String, String>>() {}),
				//new ObjectMapper().<Map<String, DisplayField>>readValue(displayFieldsJSON, new TypeReference<Map<String, DisplayField>>() {}),
				minScore,
				showScore,
				totalResults,
				totalResultsPredict,
				new HashSet<>(new ObjectMapper().<List<String>>readValue(culledSuperCategoriesJSON, new TypeReference<List<String>>() {})),
                showNetworkMap,
                TopicBranch.valueOf(defaultTopicBranch.toUpperCase()),
                docfolderMaxResults,
                combine,
                summary,
                superUserDefaultPassword,
                defaultLinkContentTemplate,
                topicmapMaxValues,
                sunburstMaxValues,
                parametricSingleQueryFieldname,
                sunburstSingleQueryFieldname,
                preloadParaValues,
                new ObjectMapper().<Map<String, Group>>readValue(groupFieldsJSON, new TypeReference<Map<String, Group>>(){}),
                textSearchEnabled,
                folderDBURL);
    }


    public String getDatabases() {
        // For legacy usage which is not using in healthcare currently, so return null.
        return null;
    }

    public String getDatabase(final String viewName) {
        return searchViews.get(viewName).getDatabase();
    }

    public String getSuperUserDefaultPassword() {
        return superUserDefaultPassword;
    }

    public Map<String, SearchView> getSearchViews() {
        return searchViews;
    }

    public Set<String> getSearchViewsNames() {
        Set<String> names = new HashSet<>();
        for(SearchView view : searchViews.values()) {
            names.add(view.getName());
                //return names.add(view.getName());
        }
        return names;
    }

    public String getDefaultSearchView() {
        for(SearchView view : searchViews.values()) {
            if (view.isDefaultView()) {
                return view.getName();
            }
        }

        for(SearchView view : searchViews.values()) {
            // No default view defined, return the first view as default.
            return view.getName();
        }

        return null;
    }


    public static SearchConfig cloneConfig(final SearchConfig srcConfig) {
        final SearchConfig config = new SearchConfig(
                srcConfig.getLanguageType(),
                srcConfig.getLinkToField(),
                srcConfig.getLinkIdField(),
                srcConfig.getOutputEncoding(),
                srcConfig.getMaxSearchResults(),
                srcConfig.getMaxPreviewResults(),
                srcConfig.getMaxSuggestionResults(),
                srcConfig.getSearchViews(),
                srcConfig.getSuggestionDatabases(),
                srcConfig.getMaxSuggestionChildren(),
                srcConfig.getDisplayCharacters(),
                srcConfig.getMeta(),
               // srcConfig.getDisplayFields(),
                srcConfig.getMinScore(),
                srcConfig.isShowScore(),
                srcConfig.isTotalResults(),
                srcConfig.isTotalResultsPredict(),
                srcConfig.getCulledSuperCategories(),
                srcConfig.isShowNetworkMap(),
                srcConfig.getDefaultTopicBranch(),
                srcConfig.getDocfolderMaxResults(),
                srcConfig.getCombine(),
                srcConfig.getSummary(),
                srcConfig.getSuperUserDefaultPassword(),
                srcConfig.getDefaultLinkContentTemplate(),
                srcConfig.getTopicmapMaxValues(),
                srcConfig.getSunburstMaxValues(),
                srcConfig.isParametricSingleQueryFieldname(),
                srcConfig.isSunburstSingleQueryFieldname(),
                srcConfig.isPreloadParaValues(),
                srcConfig.getGroupFields(),
                srcConfig.isTextSearchEnabled(),
                srcConfig.getFolderDBURL()
        );

        return config;
    }

	public boolean isPreloadParaValues() {

		return preloadParaValues;
	}

	public boolean isSunburstSingleQueryFieldname() {
		
		return sunburstSingleQueryFieldname;
	}

	public boolean isParametricSingleQueryFieldname() {
		
		return parametricSingleQueryFieldname;
	}

	public int getSunburstMaxValues() {
		
		return sunburstMaxValues;
	}

	public int getTopicmapMaxValues() {
		
		return topicmapMaxValues;
	}

	public String getDefaultLinkContentTemplate() {
		return defaultLinkContentTemplate;
	}

	public String getSummary() {
		return summary;
	}

	public String getCombine() {
		return combine;
	}

	public int getDocfolderMaxResults() {
		return docfolderMaxResults;
	}
 
	public TopicBranch getDefaultTopicBranch() {
		return defaultTopicBranch;
	}

	public boolean isShowNetworkMap() {
		return showNetworkMap;
	}

	public Set<String> getCulledSuperCategories() {
		return culledSuperCategories;
	}

	public boolean isTotalResultsPredict() {
		return totalResultsPredict;
	}

	public boolean isTotalResults() {
		// TODO Auto-generated method stub
		return totalResults;
	}

	public boolean isShowScore() {
		return showScore;
	}

	public double getMinScore() {
		return minScore;
	}

	/*public Map<String, DisplayField> getDisplayFields() {
		return displayFields;
	}*/

	public Map<String, String> getMeta() {
		return meta;
	}

	public int getDisplayCharacters() {
		return displayCharacters;
	}

	public int getMaxSuggestionChildren() {
		return maxSuggestionChildren;
	}

	public String getSuggestionDatabases() {
		return suggestionDatabases;
	}

	public String getMaxSuggestionResults() {
		return maxSuggestionResults;
	}

	public String getMaxPreviewResults() {
		return maxPreviewResults;
	}

	public String getMaxSearchResults() {
		return maxSearchResults;
	}

	public String getOutputEncoding() {
		return outputEncoding;
	}

	public String getLinkIdField() {
		return linkIdField;
	}

	public String getLinkToField() {
		return linkToField;
	}

	public String getLanguageType() {
		return languageType;
	}
	
	public Map<String, Group> getGroupFields() {
		return groupFields;
	}
	
	public boolean isTextSearchEnabled() {
		return textSearchEnabled;
	}

	public void setSearchViews(Map<String, SearchView> allowedSearchViews) {
		this.searchViews=allowedSearchViews;
	}


}