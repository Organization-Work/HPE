package com.autonomy.vertica.common;

import java.util.List;

import lombok.Data;

@Data
public class Group {
	
	String groupType;	
	List<GroupingInfo> groups;
	
	public String getGroupType() {
		return groupType;
	}
	public void setGroupType(String groupType) {
		this.groupType = groupType;
	}
	public List<GroupingInfo> getGroups() {
		return groups;
	}
	public void setGroups(List<GroupingInfo> groups) {
		this.groups = groups;
	}
}
