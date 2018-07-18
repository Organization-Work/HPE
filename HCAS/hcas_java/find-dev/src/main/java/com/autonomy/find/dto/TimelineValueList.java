package com.autonomy.find.dto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import lombok.Data;

/*
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/dto/FieldPair.java#2 $
 *
 * Copyright (c) 2013, Autonomy Systems Ltd.
 *
 * Last modified by $Author: tungj $ on $Date: 2013/11/12 $ 
 */
@Data
public class TimelineValueList {
 
	public class TimelineValueListComparator implements Comparator<TimelineFieldValueMeta> {

		@Override
		public int compare(TimelineFieldValueMeta o1, TimelineFieldValueMeta o2) {
			
   			Integer c1 = o1.getCount();
   			Integer c2 = o2.getCount();
			return c1.compareTo(c1);
		}

    }
	private String varname;
	
    private TreeSet<TimelineFieldValueMeta> vals=new TreeSet<TimelineFieldValueMeta>();

   
    
    public TimelineValueList(final String name, final TreeSet<TimelineFieldValueMeta> values) {
         this.varname = name;
         this.vals = values;
    }

	public TimelineValueList() {}
    
	public TreeSet<TimelineFieldValueMeta> getValues() {
		return vals;
	}
	
	public void setValues(TreeSet<TimelineFieldValueMeta> values) {
		this.vals = values;
	}

	public void addValue(TimelineFieldValueMeta value) {
		this.vals.add(value);
	}

	public void setName(String name) {
		this.varname=name;
	}

	public String getName() {
		return varname;
	}

}
