package com.autonomy.find.processors;

/*
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/processors/FieldDependentTagValuesProcessor.java#5 $
 *
 * Copyright (c) 2013, Autonomy Systems Ltd.
 *
 * Last modified by $Author: tungj $ on $Date: 2013/11/13 $ 
 */

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import com.autonomy.aci.client.services.ProcessorException;
import com.autonomy.aci.client.services.impl.AbstractStAXProcessor;
import com.autonomy.aci.client.services.impl.ErrorProcessor;
import com.autonomy.find.dto.TimelineDateRange;

public class TimelineDateRangeProcessor extends AbstractStAXProcessor<TimelineDateRange> {
    /**
	 * 
	 */
	private static final long serialVersionUID = -5895404576469160611L;
	private int maxValues;
	private String datePeriod;
	
	public TimelineDateRangeProcessor(int maxValues,String datePeriod) {
        this.maxValues=maxValues;
        this.datePeriod=datePeriod;
		setErrorProcessor(new ErrorProcessor());
    }


	@Override
    public TimelineDateRange process(final XMLStreamReader reader) {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss MM/dd/yyyy");  //05:00:00 09/20/2003
		TimelineDateRange tRange=new TimelineDateRange();
		
		try {
            if(isErrorResponse(reader)) {
                // Process the error response and throw an exception...
                processErrorResponse(reader);
            }
            String valTotalStr="";
            String dateMinStr="";
            String dateMaxStr="";
            long valTotal=0;
            long dateMin=0;
            long dateMax=0;
            Date dateMinDate=new Date();
            Date dateMaxDate=new Date();
            String valMinDateStr,valMaxDateStr;
            int fieldnum=0;
            
            while(reader.hasNext()) {
                final int eventType = reader.next();

                if(XMLEvent.START_ELEMENT == eventType) {
                    switch (reader.getLocalName()) {
                    	case "autn:field":
                    		fieldnum++;
                    		break;
                       case "autn:valuemin":         	
	                       	if (fieldnum==1) {
	                       		dateMinStr=reader.getElementText();
	                       		dateMin=(long) Double.parseDouble(dateMinStr);	                       		                	                       	
	                       		dateMinDate = new Date(dateMin*1000);
	                       		valMinDateStr=simpleDateFormat.format(dateMinDate); // "05:00:00 09/20/2003"
	                       	}
	                       	break;
                       case "autn:valuemax":         	
	                       	if (fieldnum==1) {
	                    		dateMaxStr=reader.getElementText();
	                     		dateMax=(long) Double.parseDouble(dateMaxStr);	                       		                	                       	
	                          	dateMaxDate = new Date(dateMax*1000);
	                          	valMaxDateStr=simpleDateFormat.format(dateMaxDate); // "05:00:00 09/20/2003"
	                       	}
	                       	break;
                       case "autn:total_values":         	
	                       	if (fieldnum==2) {
	                      		valTotalStr=reader.getElementText();
	                      		valTotal=(long) Double.parseDouble(valTotalStr);	  
	                       	}
	                       	break;
                   }  
      
                }
            }
            
            // Calculate Zoomed Range
            int units=(int) Math.ceil((long) maxValues/valTotal);
            if (units<5) units=5;

            
            
            Date newDate = new Date(dateMaxDate.getTime());

            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(newDate);         
            
            switch (datePeriod.toLowerCase()) {
	        	case "year"   :  calendar.add(Calendar.YEAR, -units);  break;
	        	case "quarter":  calendar.add(Calendar.MONTH, -3*units);  break;
	        	case "month"  :  calendar.add(Calendar.MONTH, -units);  break;
	        	case "week"   :  calendar.add(Calendar.DATE, -7*units);  break;
	        	case "day"    :  calendar.add(Calendar.DATE, -units);  break;
	        	case "hour"   :  calendar.add(Calendar.HOUR, -units);  break;
	        	case "minute" :  calendar.add(Calendar.MINUTE, -units);  break;
	        	case "second" :  calendar.add(Calendar.SECOND, -units);  break;
            }       
            newDate.setTime(calendar.getTime().getTime());     
            if (newDate.compareTo(dateMinDate)<0) {
            	newDate=dateMinDate;
            }
            
            long zoomMax=dateMax;
            long zoomMin=(long) newDate.getTime()/1000;
       
            	// dateMax-span;
            
            
            tRange.setValTotal(valTotal);
            tRange.setDateMin(dateMin);
            tRange.setDateMax(dateMax);
            tRange.setZoomMin(zoomMin);
            tRange.setZoomMax(zoomMax);
            
    	} catch (Exception xse) {
    		throw new ProcessorException("Error whie parsing timeline range values", xse);
    	}
       		               
        return tRange;
    }

	public int getMaxValues() {
		return maxValues;
	}

	public void setMaxValues(int maxValues) {
		this.maxValues = maxValues;
	}
}
