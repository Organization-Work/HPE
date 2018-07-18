package com.autonomy.find.dto.Parametric;

import com.autonomy.aci.client.annotations.IdolField;
import lombok.Data;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/dto/Parametric/SearchCategory.java#1 $
 * <p/>
 * Copyright (c) 2013, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/05/31 $
 */
@Data
public class SearchCategory {
	private String title, reference, highlight;

	@IdolField("autn:reference")
	public void setReference(final String reference) {
		this.reference = reference;
	}

	@IdolField("autn:title")
	public void setTitle(final String title) {
		this.title = title;
	}

	@IdolField("HIGHLIGHTTEXT")
	public void setHighlight(final String highlight) {
		this.highlight = highlight;
	}
}
