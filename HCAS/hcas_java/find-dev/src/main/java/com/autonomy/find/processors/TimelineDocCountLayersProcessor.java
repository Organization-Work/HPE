package com.autonomy.find.processors;

/*
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/processors/FieldDependentTagValuesProcessor.java#5 $
 *
 * Copyright (c) 2013, Autonomy Systems Ltd.
 *
 * Last modified by $Author: tungj $ on $Date: 2013/11/13 $ 
 */

import com.autonomy.aci.client.services.ProcessorException;
import com.autonomy.aci.client.services.impl.AbstractStAXProcessor;
import com.autonomy.aci.client.services.impl.ErrorProcessor;
import com.autonomy.find.dto.FieldPair;
import com.autonomy.find.dto.TimelineData;
import com.autonomy.find.dto.TimelineDataLayer;
import com.autonomy.find.dto.TimelineDataVal;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class TimelineDocCountLayersProcessor extends AbstractStAXProcessor<TimelineDataLayer> {
    /**
	 * 
	 */
	private int maxValues;
	private static final long serialVersionUID = -5895404576469160611L;
	
	public TimelineDocCountLayersProcessor(int maxValues) {
        setErrorProcessor(new ErrorProcessor());
        this.maxValues=maxValues;
     }

    @Override
    public TimelineDataLayer process(final XMLStreamReader reader) {
        TimelineDataLayer layer= new TimelineDataLayer();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss MM/dd/yyyy");  //05:00:00 09/20/2003
		SimpleDateFormat simpleDateFormatIn = new SimpleDateFormat("yyyyMMdd");  
		try {
            if(isErrorResponse(reader)) {
                // Process the error response and throw an exception...
                processErrorResponse(reader);
            }
            String date="";
            String lastdateStr="0";
            String scount="";
            int count=0;
            TimelineDataVal curDataVal;
            String name="";
            String dateStr="";
           	Date date2=null;
           	String rtype="";
           	int month_count=0;
           	int add_count=0;
           	int remove_count=0;
           	
           	while(reader.hasNext()) {
                final int eventType = reader.next();

                if(XMLEvent.START_ELEMENT == eventType) {
                    switch (reader.getLocalName()) {
                       case "AS_OF_DATE":         	
	                   		date=reader.getElementText();
	                       	if (!date.equals("")) {
	                       		date2 = simpleDateFormatIn.parse(date);
	                       		dateStr=Long.toString(date2.getTime()); // "Epoch String"	                       	
	                       	}
	                       	break;                     
                       case "COUNT":
	                   		scount=reader.getElementText();
	                   		scount=scount.replace("\"", "");
	                   		count=Integer.parseInt(scount);	                   		
	                       	if (!date.equals("")) {
	                       		curDataVal=new TimelineDataVal(count,dateStr);
	                       		layer.addValue(curDataVal);       
		                    }
	                   		break;
                    }
                }
           	}                     
    	} catch (Exception xse) {
			System.out.println("layers:"+layer);
    		throw new ProcessorException("Error whie parsing docCount timeline values", xse);
    	}
       		               
        return layer;
    }
}
