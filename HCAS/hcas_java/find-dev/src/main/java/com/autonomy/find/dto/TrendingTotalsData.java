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
public class TrendingTotalsData {
	private String graphType;
	private String title;
	private String yaxisLabel;
	private String xaxisLabel;
	private String startDate;
	private String endDate;
	private String minY;
	private String maxY;
	private String validStartDate;
	private String validEndDate;
	private String validMinY;
	private String validMaxY;
	private String format;
	private String field;
	private String date1;
	private String date2;
	private String totalVals;
	private String sortOrder;
	private List<TrendingDataLayer> dataLayers= new ArrayList<TrendingDataLayer>();
	
    public TrendingTotalsData() {
    }
}

