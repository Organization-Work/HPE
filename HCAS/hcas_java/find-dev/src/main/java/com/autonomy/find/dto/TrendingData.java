package com.autonomy.find.dto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
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
public class TrendingData {
	private String field;
	private String baselineDate;
	private String startDate;
	private String endDate;
	private TrendingDataLayer totalsLayer=new TrendingDataLayer();
	private List <TrendingDataLayer> valuesLayers=new ArrayList <TrendingDataLayer>();
	
    public TrendingData() {
    }
	
	
    public String getField() {
 		return field;
 	}
     
 	public void setField(String field) {
 		this.field=field;
 	}
  
    public String getBaselineDate() {
 		return baselineDate;
 	}
     
 	public void setBaselineDate(String date) {
 		this.baselineDate=date;
 	}
   
    public String getStartDate() {
 		return startDate;
 	}
     
 	public void setStartDate(String date) {
 		this.startDate=date;
 	}
    public String getEndDate() {
 		return endDate;
 	}
 	public void setEndDate(String date) {
 		this.endDate=date;
 	}
	
    public TrendingDataLayer getTotalsLayer() {
		return totalsLayer;
	}
    
	public void setTotalsLayers(TrendingDataLayer totalsLayer) {
		this.totalsLayer=totalsLayer;
	}

    
	public List<TrendingDataLayer> getValuesLayers() {
		return valuesLayers;
	}

	public void seValuesLayers(List<TrendingDataLayer> layers) {
		this.valuesLayers = layers;
	}

	public void addValuesLayer(TrendingDataLayer layer) {
		this.valuesLayers.add(layer);
	}
	
}

