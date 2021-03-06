package com.autonomy.find.dto;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
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
public class TrendingDataLayer {
 
	public class TrendingDataLayerComparator implements Comparator<TrendingDataVal> {

		SimpleDateFormat epochDateFormat = new SimpleDateFormat("MM/dd/yyyy");  //05:00:00 09/20/2003
		@Override
		public int compare(TrendingDataVal o1, TrendingDataVal o2) {
			
			Date o1d=null;
			Date o2d=null;
			try {
				o1d = epochDateFormat.parse(o1.getDate().toString());
				o2d = epochDateFormat.parse(o2.getDate().toString());;
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return o1d.compareTo(o2d);
		}

    }
	
	private String label;
	private String type;
	private String order;
	private String varName;
	private String baselineDate;
	private double baselineValue;
    private TreeSet<TrendingDataVal> vals=new TreeSet<TrendingDataVal>(new TrendingDataLayerComparator());

   
    
    public TrendingDataLayer(final String label, final String type, final String order, final String varName, final String baselineDate, final double baselineValue,  final TreeSet<TrendingDataVal> values) {
         this.label =label;
         this.type=type;
         this.order= order;
         this.varName=varName;
         this.baselineDate=baselineDate;
         this.baselineValue=baselineValue;  
         this.vals = values;         
    }

	public TrendingDataLayer() {}

	public void addVal(TrendingDataVal dv) {
		// TODO Auto-generated method stub
		this.vals.add(dv);
	}
    
}
