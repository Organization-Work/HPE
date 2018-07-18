package com.autonomy.find.dto;

import lombok.Data;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/dto/Link.java#1 $
 * <p/>
 * Copyright (c) 2013, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/05/21 $
 */
@Data
public class Link {
	public Link(final String title, final String reference) {
		this.title = title;
		this.reference = reference;
	}

	private String title;
	private String reference;
	public void setTitle(String title2) {
		this.title=title2;
	}
	public void setReference(String reference2) {
		this.reference=reference2;
	}
}
