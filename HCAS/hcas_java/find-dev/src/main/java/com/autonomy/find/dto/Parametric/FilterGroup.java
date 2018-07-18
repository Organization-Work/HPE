package com.autonomy.find.dto.Parametric;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FilterGroup {
    private BoolOp boolOperator;
    private List<FilterGroup> childGroups;
    private String tag;
    private CohortOp cohortOp;
    private boolean deactivated = false;
    private Map<String, List<FieldParams>> filterFields;
    private String whenOp;
    private String whenGroup;
	public BoolOp getBoolOperator() {
		return boolOperator;
	}
	public boolean isDeactivated() {
		return deactivated;
	}
	public Map<String, List<FieldParams>> getFilterFields() {
		return filterFields;
	}
	public String getWhenOp() {
		return whenOp;
	}
	public List<FilterGroup> getChildGroups() {
		return childGroups;
	}
	public CohortOp getCohortOp() {
		return cohortOp;
	}
	public void setBoolOperator(BoolOp and) {
			this.boolOperator=and;
	}
	public void setFilterFields(Map<String, List<FieldParams>> paraFilters) {
		this.filterFields=paraFilters;
	}
	public void setChildGroups(List<FilterGroup> list) {
		this.childGroups=list;
	}

}
