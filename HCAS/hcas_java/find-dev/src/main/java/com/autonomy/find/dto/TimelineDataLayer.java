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
public class TimelineDataLayer {
 
	public class TimelineDataLayerComparator implements Comparator<TimelineDataVal> {

		SimpleDateFormat epochDateFormat = new SimpleDateFormat("HH:mm:ss MM/dd/yyyy");  //05:00:00 09/20/2003
		@Override
		public int compare(TimelineDataVal o1, TimelineDataVal o2) {
			
   			Date o1d = new Date(Long.parseLong(o1.getDate()));
			Date o2d = new Date(Long.parseLong(o2.getDate()));
			return o1d.compareTo(o2d);
		}

    }
	
	private String name;
	private String type;
	private String order;
	private String varName;
    private TreeSet<TimelineDataVal> vals=new TreeSet<TimelineDataVal>(new TimelineDataLayerComparator());

   
    
    public TimelineDataLayer(final String name, String order, String varName, final TreeSet<TimelineDataVal> values) {
         this.name = name;
         this.vals = values;
         this.order= order;
         this.setVarName(varName);
    }

	public TimelineDataLayer() {}
    
	public TreeSet<TimelineDataVal> getValues() {
		return vals;
	}
	
	public void setValues(TreeSet<TimelineDataVal> values) {
		this.vals = values;
	}
	public void addValue(TimelineDataVal value) {
		this.vals.add(value);
	}

	public void setName(String name) {
		this.name=name;
	}

	public String getName() {
		return name;
	}
	public void setType(String type) {
		this.type=type;
	}

	public String getType() {
		return type;
	}
	
	public void setOrder(String order) {
		this.order=order;
	}

	public String getOrder() {
		return order;
	}

	public String getVarName() {
		return varName;
	}

	public void setVarName(String varName) {
		this.varName = varName;
	}

}
