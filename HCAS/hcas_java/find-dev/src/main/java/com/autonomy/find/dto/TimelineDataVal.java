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
public class TimelineDataVal {
    private String date;
    private int count;

    public TimelineDataVal(final int count, final String date) {
        this.count = count;
        this.date = date;
    }

	public TimelineDataVal() {}
    
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

}
