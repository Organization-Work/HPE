package com.autonomy.find.dto;

import java.util.Date;

import lombok.Data;

/*
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/dto/FieldPair.java#2 $
 *
 * Copyright (c) 2013, Autonomy Systems Ltd.
 *
 * Last modified by $Author: tungj $ on $Date: 2013/11/12 $ 
 */
@Data
public class TimelineDateRange {
    private long valTotal;
    private long dateMin;
    private long dateMax;
    private long zoomMin;
    private long zoomMax;


	public TimelineDateRange() {}



	public long getValTotal() {
		return valTotal;
	}


	public void setValTotal(long valTotal) {
		this.valTotal = valTotal;
	}


	public long getDateMin() {
		return dateMin;
	}


	public void setDateMin(long dateMin) {
		this.dateMin = dateMin;
	}


	public long getDateMax() {
		return dateMax;
	}


	public void setDateMax(long dateMax) {
		this.dateMax = dateMax;
	}


	public long getZoomMin() {
		return zoomMin;
	}


	public void setZoomMin(long zoomMin) {
		this.zoomMin = zoomMin;
	}


	public long getZoomMax() {
		return zoomMax;
	}


	public void setZoomMax(long zoomMax) {
		this.zoomMax = zoomMax;
	}


    
}
