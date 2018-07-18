package com.autonomy.vertica.common;

import java.util.Set;

public class SubExpression implements Comparable<SubExpression>{
	
	private SelectBuilder subExpression;
	private int joinLevel;
	private boolean isGroupExpression = false;
	private boolean isFilterNotExpression = false;
	private boolean isNotExpressionApplied = false;
	private String fieldName;
	private Set<String> listTables;
	
	
	public Set<String> getListTables() {
		return listTables;
	}
	public void setListTables(Set<String> listTables) {
		this.listTables = listTables;
	}
	public String getFieldName() {
		return fieldName;
	}
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}
	public boolean isNotExpressionApplied() {
		return isNotExpressionApplied;
	}
	public void setNotExpressionApplied(boolean isNotExpressionApplied) {
		this.isNotExpressionApplied = isNotExpressionApplied;
	}
	public boolean isFilterNotExpression() {
		return isFilterNotExpression;
	}
	public void setFilterNotExpression(boolean isFilterNotExpression) {
		this.isFilterNotExpression = isFilterNotExpression;
	}
	public boolean isGroupExpression() {
		return isGroupExpression;
	}
	public void setGroupExpression(boolean isGroupExpression) {
		this.isGroupExpression = isGroupExpression;
	}
	public SubExpression(SelectBuilder retexp, int joinLevel2) {
		this.subExpression = retexp;
		this.joinLevel = joinLevel2;
	}
	public SubExpression(String name, int joinNumber) {
		this.fieldName = name;
		this.joinLevel = joinNumber;
	}
	public SelectBuilder getSubExpression() {
		return subExpression;
	}
	public void setSubExpression(SelectBuilder subExpression) {
		this.subExpression = subExpression;
	}
	public int getJoinLevel() {
		return joinLevel;
	}
	public void setJoinLevel(int joinLevel) {
		this.joinLevel = joinLevel;
	}
	@Override
	public int compareTo(SubExpression o) {
		int compareJoinLevel = o.getJoinLevel();
		return this.joinLevel - compareJoinLevel; // ascending
		// descending
		// return compareJoinLevel - this.joinLevel;
	}
	
	

}
