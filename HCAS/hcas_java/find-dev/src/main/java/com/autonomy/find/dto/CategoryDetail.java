package com.autonomy.find.dto;

import java.util.HashMap;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown=true)
public class CategoryDetail {
	
	private String displayName;
	private HashMap<String, String> types;

	/**
	 * Shortcut method for looking up a type in the types map.
	 * @param type
	 * @return
	 */
	public String getJob(final String job) {
		return getTypes().get(job);
	}
	
	private HashMap<String, String> getTypes() {
		return types;
	}

	public Boolean hasJob(final String job) {
		return getTypes().containsKey(job);
	}

	public String getDisplayName() {
		return displayName;
	}
}
