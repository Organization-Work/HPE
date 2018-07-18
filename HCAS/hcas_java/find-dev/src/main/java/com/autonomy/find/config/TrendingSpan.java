package com.autonomy.find.config;

import lombok.Data;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/config/TrendingSpan.java#2 $
 * <p/>
 * Copyright (c) 2013, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/11/04 $
 */
@Data
public class TrendingSpan {
	private String name;
	private String spanStartField;
	private String spanEndField;
	private String baselineDate;
}
