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
public class FieldPair {
    private String primaryField;
    private String secondaryField;
    private String primaryValue;
    private String secondaryValue;

    private double count;

//    private float valueAverage;
//    private float valueMin;
//    private float valueMax;

    public FieldPair(final String primaryField, final String secondaryField) {
        this.primaryField = primaryField.trim();
        this.secondaryField = secondaryField.trim();
    }

    public FieldPair() {}

	public void setCount(double parseInt) {
		this.count=parseInt;
	}

	public void setPrimaryValue(String elementText) {
		this.primaryValue=elementText.trim();
	}

	public void setSecondaryValue(String elementText) {
		this.secondaryValue=elementText.trim();
	}

	public Object getPrimaryValue() {
		return primaryValue;
	}

	public double getCount() {
		return count;
	}

	public Object getSecondaryValue() {
		return secondaryValue;
	}

	public String getPrimaryField() {
		return primaryField;
	}
	public String getSecondaryField() {
		return secondaryField;
	}
}
