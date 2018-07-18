package com.autonomy.vertica.common;

import lombok.Data;

@Data
public class GroupingInfo {
	
	String groupLabel;		
	String minVal;
	String maxVal;
	boolean hasNull;
	
}
