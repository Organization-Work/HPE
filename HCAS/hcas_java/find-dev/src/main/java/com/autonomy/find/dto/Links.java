package com.autonomy.find.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/dto/Links.java#1 $
 * <p/>
 * Copyright (c) 2013, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/05/21 $
 */
@Data
public class Links {
	private  List<Link> from=null;
	private  List<Link> to=null;

	public Links(List<Link> wrapLinks, List<Link> toLinks) {
		this.from=wrapLinks;
		this.to=toLinks;
	}
}
