package com.autonomy.find.dto;

import com.autonomy.find.api.database.SearchSettings;
import com.autonomy.find.dto.Parametric.FilterGroup;

import lombok.Data;

import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Data
public class SearchRequestData {
    private String query = "*";
    private Map<String, List<String>> filters = new HashMap<>();
    private FilterGroup filterGroup;
    private int page = 0;
	private Double minScore;
	private Integer displayChars;
    private String category;
    private String queryExtension;
    private String searchView;
    private List<String> fieldNames;
    private SearchSettings userSearchSettings;
    private String stateMatchId;
    private String stateDontMatchId;
    private String securtiyInfo;
    private boolean retrieveResultDocs = false;
    private boolean gotAll = false;

    public boolean hasCategory() {
        return StringUtils.isNotEmpty(this.category);
    }

    public boolean hasQueryExtension() {
        return StringUtils.isNotEmpty(this.queryExtension);
    }

    public boolean hasValidQuery() {
        return StringUtils.isNotEmpty(this.query);
    }

    public SearchRequestData() {}

    public SearchRequestData(final String query, final Map filters) {
        this.query = query;
        this.filters = filters;
    }

    public SearchRequestData(final String query, final FilterGroup filterGroup) {
        this.query = query;
        this.filterGroup = filterGroup;
    }


    public SearchRequestData(final String query, final Map filters, final String queryExtension) {
        this(query, filters);
        this.queryExtension = queryExtension;
    }

    public SearchRequestData(final String query, final FilterGroup filterGroup, final String queryExtension) {
        this(query, filterGroup);
        this.queryExtension = queryExtension;
    }

    public SearchRequestData(final String query, final FilterGroup filterGroup, final String queryExtension, final String searchView) {
        this(query, filterGroup, queryExtension);
        this.searchView = searchView;
    }

    public Map<String, Object> getAuditData() {
        final Map<String, Object> dataMap = new TreeMap<String, Object>();
        dataMap.put("query", query);
        dataMap.put("searchView", searchView);
        dataMap.put("page", page);
        if (userSearchSettings != null) {
            dataMap.put("userSearchSettings", userSearchSettings);
        }

        if (filterGroup != null) {
            dataMap.put("filterGroup", filterGroup);
        }

        if (stateMatchId != null) {
            dataMap.put("stateMatchId", stateMatchId);
        }

        if (stateDontMatchId != null) {
            dataMap.put("stateDontMatchId", stateDontMatchId);
        }

        if (fieldNames != null) {
            dataMap.put("fieldNames", fieldNames);
        }

        return dataMap;
    }

	public void setSecurtiyInfo(String userSecurityInfo) {
		this.securtiyInfo=userSecurityInfo;
	}

	public List<String> getFieldNames() {
		return fieldNames;
	}

	public String getQuery() {
		return query;
	}

	public String getQueryExtension() {
		return queryExtension;
	}

	public String getCategory() {
		return category;
	}

	public void setQueryExtension(String categoryQueryExtension) {
		this.queryExtension=categoryQueryExtension;
	}

	public Map<String, List<String>> getFilters() {
		return filters;
	}

	public Double getMinScore() {
		return minScore;
	}

	public String getSearchView() {
		return searchView;
	}

	public FilterGroup getFilterGroup() {
		return filterGroup;
	}

	public void setFilterGroup(FilterGroup group) {
		this.filterGroup=group;
	}

	public int getPage() {
			return page;
	}

	public String getStateMatchId() {
		return stateMatchId;
	}

	public String getStateDontMatchId() {
		return stateDontMatchId;
	}

	public SearchSettings getUserSearchSettings() {
		return userSearchSettings;
	}

	public Integer getDisplayChars() {
		return displayChars;
	}

	public String getSecurtiyInfo() {
		return securtiyInfo;
	}

	public void setStateMatchId(String join) {
		this.stateMatchId=join;
	}

	public void setStateDontMatchId(String join) {
		this.stateDontMatchId=join;
	}

}
