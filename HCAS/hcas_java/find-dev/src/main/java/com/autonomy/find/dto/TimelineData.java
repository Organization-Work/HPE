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
public class TimelineData {
	private TimelineDateRange tRange=new TimelineDateRange();
	private TimelineDataLayer totalsLayer=new TimelineDataLayer();
	private List <TimelineDataLayer> dataLayers=new ArrayList <TimelineDataLayer>();
	
    public TimelineData() {
    }
	
	public TimelineDateRange gettRange() {
		return tRange;
	}

	public void settRange(TimelineDateRange tRange) {
		this.tRange = tRange;
	}

	public TimelineDataLayer getTotalsLayer() {
		return totalsLayer;
	}

	public void setDocCountLayer(TimelineDataLayer layer) {
		this.totalsLayer = layer;
	}

	public List<TimelineDataLayer> getDataLayers() {
		return dataLayers;
	}

	public void setDataLayers(List<TimelineDataLayer> layers) {
		this.dataLayers = layers;
	}

	public void add(TimelineDataLayer layer) {
		this.dataLayers.add(layer);
	}
	
	public void normalize() {
	}

	public void setDateRange(TimelineDateRange timelineDateRange) {
		this.tRange=timelineDateRange;
	}

	public void calcZoomMinMax() {
		if (dataLayers != null) {
			// Calculate min and max dates in doclayer
			Long minDate=(long) 0;
			Long maxDate=(long) 0;
			
			Iterator <TimelineDataLayer> Itr=dataLayers.iterator();
			tRange.setZoomMin(0);
			tRange.setZoomMax(0);
			while (Itr.hasNext()) {
				TimelineDataLayer layer=Itr.next();
				
				if (layer==null || layer.getValues()==null) {
					continue;
				}
				TreeSet<TimelineDataVal> vals=layer.getValues();
				if (vals==null || vals.size()==0) {
					continue;
				}
				TimelineDataVal minval=vals.first();
				TimelineDataVal maxval=vals.last();
			
				if (minval!=null && maxval!=null) {
					minDate=Long.parseLong(minval.getDate());
					maxDate=Long.parseLong(maxval.getDate());
					tRange.setZoomMin(minDate);
					tRange.setZoomMax(maxDate);
					return;
				}
			} 
		}
	}
		
	public void calcMinMax() {
		if (totalsLayer != null) {
			// Calculate min and max dates in doclayer
			Long minDate=(long) 0;
			Long maxDate=(long) 0;
			
			TreeSet<TimelineDataVal> vals=totalsLayer.getValues();
			if ((vals==null) || vals.size()==0) {
				tRange.setDateMin(0);
				tRange.setDateMax(0);
				return;
			}
			TimelineDataVal minval=vals.first();
			TimelineDataVal maxval=vals.last();
			
			if (minval!=null && maxval!=null) {
				minDate=Long.parseLong(minval.getDate());
				maxDate=Long.parseLong(maxval.getDate());
				tRange.setDateMin(minDate);
				tRange.setDateMax(maxDate);
			}
		} 
	}
}

