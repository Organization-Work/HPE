/**
 * 
 */
package com.autonomy.vertica.fields;

import java.util.Map;

/**
 * @author $fara002
 *
 */
public class QueryFields {
	private String queryString;
	private Map<String, Object> paramMap;
    private String fieldName;     
    private String maxValue;
     
    public String getQueryString() {
		return queryString;
	}
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}
	public Map<String, Object> getParamMap() {
		return paramMap;
	}
	public void setParamMap(Map<String, Object> paramMap) {
		this.paramMap = paramMap;
	}
	public String getFieldName() {
		return fieldName;
	}
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}
	public String getMaxValue() {
		return maxValue;
	}
	public void setMaxValue(String maxValue) {
		this.maxValue = maxValue;
	}
	
}
