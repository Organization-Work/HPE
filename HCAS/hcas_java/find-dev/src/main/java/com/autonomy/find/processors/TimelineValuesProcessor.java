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
import com.autonomy.find.dto.TimelineFieldValueMeta;
import com.autonomy.find.dto.TimelineValueList;

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

public class TimelineValuesProcessor extends AbstractStAXProcessor<TimelineValueList> {
    /**
	 * 
	 */
	private static final long serialVersionUID = -5895404576469160611L;
	private String varname;

	public TimelineValuesProcessor(String varname) {
        this.varname=varname;
		setErrorProcessor(new ErrorProcessor());
    }

	
    @Override
    public TimelineValueList process(final XMLStreamReader reader) {
    	TimelineValueList tvals= new TimelineValueList();

    	SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss MM/dd/yyyy");  //05:00:00 09/20/2003
		SimpleDateFormat simpleDateFormatIn = new SimpleDateFormat("yyyyMMdd");  

		try {
            if(isErrorResponse(reader)) {
                // Process the error response and throw an exception...
                processErrorResponse(reader);
            }
            String date="";
            String dateStr="";
            String scount="";
            int count=0;
            TimelineFieldValueMeta curVal;
            TimelineDataLayer layer=null;
           	Date date2=null;
           	String varval="";
           	
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
                       case "VARVAL":
                   	   		varval=reader.getElementText();
                   	   		break;
                       case "COUNT":
	                   		scount=reader.getElementText();
	                   		scount=scount.replace("\"", "");
	                   		count=Integer.parseInt(scount);
	                   		curVal=new TimelineFieldValueMeta(this.varname,varval,count,dateStr);
	                   		tvals.addValue(curVal);
	                   		break;
                    }
                }
           	}

           	
    		tvals.setName(varname);		
           	// System.out.println("Total Values:"+vals.size()+"    DateList Size:"+datelist.size());
                                 
    	} catch (Exception xse) {
			System.out.println("valuelist:"+tvals);
    		throw new ProcessorException("Error whie parsing field dependent timeline value metadata", xse);
    	}	               
        return tvals;
    }
}
