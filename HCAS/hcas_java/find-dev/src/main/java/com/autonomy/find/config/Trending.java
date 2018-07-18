package com.autonomy.find.config;

import lombok.Data;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/config/Trending.java#2 $
 * <p/>
 * Copyright (c) 2013, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/11/04 $
 */
@Data
public class Trending {
	private boolean enabled;
	private String title;
	private String minDate;
	private String maxDate;
	private String minPeriod;
	private String maxValues;
	private String maxDataPerVal;
	private String sortType;
	private String sortOrder;
	private String[] dateFields;
	private TrendingSpan[] spanFields;
}
