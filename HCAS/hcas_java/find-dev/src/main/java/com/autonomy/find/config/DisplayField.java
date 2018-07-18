package com.autonomy.find.config;

import lombok.Data;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/config/DisplayField.java#2 $
 * <p/>
 * Copyright (c) 2013, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/11/04 $
 */
@Data
public class DisplayField {
	public String getIcon() {
		return icon;
	}
	public void setIcon(String icon) {
		this.icon = icon;
	}
	public void setFields(String[] fields) {
		this.fields = fields;
	}
	private String icon;
	private String[] fields;
	public String[] getFields() {
		return fields;
	}
}
