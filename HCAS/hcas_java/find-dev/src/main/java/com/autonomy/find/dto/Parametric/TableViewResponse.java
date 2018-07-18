package com.autonomy.find.dto.Parametric;


import lombok.Data;

import java.util.List;

import com.autonomy.vertica.fields.FieldValue;

/*
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/dto/Parametric/TableViewResponse.java#2 $
 *
 * Copyright (c) 2013, Autonomy Systems Ltd.
 *
 * Last modified by $Author: tungj $ on $Date: 2013/11/13 $ 
 */
@Data
public class TableViewResponse {
    private List<FieldValue> xValues;
    private List<FieldValue> yValues;
    
    private double[][] table;

	public void setXValues(List<FieldValue> xValues2) {
		this.xValues=xValues2;
	}

	public void setYValues(List<FieldValue> yValues2) {
		this.yValues=yValues2;
	}

	public void setTable(double[][] data) {
		this.table=data;
	}
}
