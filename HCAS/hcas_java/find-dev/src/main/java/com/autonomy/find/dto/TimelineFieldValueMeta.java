package com.autonomy.find.dto;

import lombok.Data;

/*
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/dto/FieldPair.java#2 $
 *
 * Copyright (c) 2013, Autonomy Systems Ltd.
 *
 * Last modified by $Author: tungj $ on $Date: 2013/11/12 $ 
 */
@Data
public class TimelineFieldValueMeta implements Comparable<TimelineFieldValueMeta> {
    private String varname;
    private String varval;
    private String date;
    private int count;
    
	public TimelineFieldValueMeta(String varname, String varval, int count, String dateStr) {
		this.varname=varname;
		this.varval=varval;
		this.count = count;
        this.date = dateStr;	
	}
    
	@Override
	public int compareTo(TimelineFieldValueMeta tv) {
		// Reverse Sorted (Largest to smallest)
		return tv.count - count;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public void setCount(int parseInt) {
		this.count=parseInt;
	}


	public int getCount() {
		return count;
	}
	
	public String getVarName() {
		return varname;
	}
	
	public String getVarVal() {
		return varval;
	}

	
}
