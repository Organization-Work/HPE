package com.autonomy.find.config;

import lombok.Data;

@Data
public class TaxonomyConfig {
    public Integer getRequestDepth() {
		return requestDepth;
	}
	public void setRequestDepth(Integer requestDepth) {
		this.requestDepth = requestDepth;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
	public void setDatabase(String database) {
		this.database = database;
	}
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}
	public void setRootCategory(String rootCategory) {
		this.rootCategory = rootCategory;
	}
	private boolean active;
    private String database;
    private String fieldName;
    private String rootCategory;
    private Integer requestDepth;
	public String getFieldName() {
		return fieldName;
	}
	public String getDatabase() {
		return database;
	}
	public String getRootCategory() {
		return rootCategory;
	}
	public boolean isActive() {
		return active;
	}
}
