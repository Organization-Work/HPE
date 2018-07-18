package com.autonomy.find.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/dto/AgentOptionsRequest.java#1 $
 * <p/>
 * Copyright (c) 2013, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/05/21 $
 */
@Data
public class AgentOptionsRequest {
	public AgentOptionsRequest() {

	}

	private String aid;
	private String name;
	private String newName;
	private boolean unreadOnly = true;
	private List<String> concepts;
	private List<String> documents;
	private List<String> databases;
	private Long startDate;
	private Boolean removeStartDate;
	private Map<String, List<String>> filters;
	private Double minScore;
	private Integer dispChars;
    private String categoryId;
	public String getName() {
		return name;
	}
	public boolean isUnreadOnly() {
		return unreadOnly;
	}
	public Long getStartDate() {
		return startDate;
	}
	public List<String> getConcepts() {
		return concepts;
	}
	public List<String> getDatabases() {
		return databases;
	}
	public Map<String, List<String>> getFilters() {
		return filters;
	}
	public Double getMinScore() {
		return minScore;
	}
	public Integer getDispChars() {
		return dispChars;
	}
	public String getCategoryId() {
		return categoryId;
	}
	public List<String> getDocuments() {
		return documents;
	}
	public String getAid() {
		return aid;
	}
	public String getNewName() {
		return newName;
	}
	public Boolean getRemoveStartDate() {
		return removeStartDate;
	}
}
