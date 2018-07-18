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

public class TimelineLayersProcessor extends AbstractStAXProcessor<List<TimelineDataLayer>> {
    /**
	 * 
	 */
	private static final long serialVersionUID = -5895404576469160611L;
	private TimelineValueList queryValueList=new TimelineValueList();
	private int maxResults=0;
	private long selDateMin=0;
	private long selDateMax=0;
	
	public TimelineLayersProcessor(TimelineValueList queryValueList,int maxResults,long selDateMin,long selDateMax) {
		this.queryValueList=queryValueList;
		this.maxResults=maxResults;
		this.selDateMin=selDateMin;
		this.selDateMax=selDateMax;
		
        setErrorProcessor(new ErrorProcessor());
    }

    @Override
    public List<TimelineDataLayer> process(final XMLStreamReader reader) {
        List<TimelineDataLayer> tlayers= new ArrayList<TimelineDataLayer>();
        ArrayList<String> layerList = new ArrayList<String>();
        TreeSet<Date> datelist=new TreeSet<Date>();
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
            TimelineDataLayer layer=null;
            String name="";
            String dateStr="";
           	Date date2=null;
           	String varval="";
            	
           	// Initialize layers structure based on the valuesList
           	Iterator<TimelineFieldValueMeta> itr = queryValueList.getValues().iterator();
           	int order=0;
           	while (itr.hasNext()) {
           		TimelineFieldValueMeta tfvm=itr.next();
           		layer=new TimelineDataLayer();
           		layer.setVarName(tfvm.getVarName());
           		layer.setOrder(Integer.toString(order));
           		layer.setName(tfvm.getVarVal());
           		layer.setType("value_totals");
           		tlayers.add(layer);
           		layerList.add(tfvm.getVarVal());
           		order++;
           	}
           	int resultcnt=0;
           	
           	boolean bTruncated=true;
           	long date2esec=0;
           	
           	// fill in the data
           	while(reader.hasNext()) {
                final int eventType = reader.next();

                if(XMLEvent.START_ELEMENT == eventType) {
                    switch (reader.getLocalName()) {
                       case "AS_OF_DATE":         	
	                   		date=reader.getElementText();
	                   		if (!date.equals("")) {
	                   			date2 = simpleDateFormatIn.parse(date);
	                   			dateStr=Long.toString(date2.getTime()); // "Epoch String"
	                   			datelist.add(date2);          
	                   		}
	                   		break;
                       case "VARVAL":
                   	   		varval=reader.getElementText();
                   	   		break;
                       case "COUNT":
                    	    resultcnt+=1;
	                   		scount=reader.getElementText();
	                   		scount=scount.replace("\"", "");
	                   		count=Integer.parseInt(scount);
	                   		if (!date.equals("")) {
	                       	    // Add the dataval to the layer
		                       	curDataVal=new TimelineDataVal(count,dateStr);
		                       	int i=layerList.indexOf(varval);
		                       	if (i!=-1)
		                       	{
		                       		// find order from position in 
		                       		layer=tlayers.get(i);
	                    			layer.addValue(curDataVal);
	                    		}
	                   		}
	                   		break;
                    }
                }
           	}
           	
           	
            // Iterate over all layers
    		for (TimelineDataLayer layer2: tlayers) {
    			TreeSet<TimelineDataVal> vals=layer2.getValues();
    			if (!vals.isEmpty()) {
	    			List<TimelineDataVal> newvals=new ArrayList<TimelineDataVal>();
	    			// Get first value in layer and datelist
	    			Iterator<TimelineDataVal> tlDataValIter = vals.iterator();
	    			Iterator<Date> curDateIter=datelist.iterator(); 
	    			
	    			TimelineDataVal tlData=null;
	    			Date layerDate=null;
	    			Date curDate=null;
	    			int layerCount=0;
	    			
	    			if (tlDataValIter!=null) {
	    				tlData=tlDataValIter.next();
	    				String layerDateStr=tlData.getDate();
	        			layerDate = new Date(Long.parseLong(layerDateStr));
	    			}
	    			if (curDateIter.hasNext()) {
	       				curDate=curDateIter.next();
	    			}
	    			
					// while more in dateList
	    			while(curDate!=null){
	    				// If curdate==layerDate   				
	    				if (curDate.compareTo(layerDate)==0) {
							// get next layerDate
	    					if (tlDataValIter.hasNext()) {
	    	    				tlData=tlDataValIter.next();
	    	    				String layerDateStr=tlData.getDate();
	    	        			layerDate = new Date(Long.parseLong(layerDateStr));
	    	        			layerCount = tlData.getCount();
	    					} 
	        				// get next currdate
	        				if (curDateIter.hasNext()) {
	        					curDate=curDateIter.next();
	        				} else {
	        					curDate=null;
	        				}    						
	    				}
	    				else {
	    					// If curDate < layerDate or curDate > layerDate
	    					// insert a zero value to layer on curdate
	    					TimelineDataVal newVal=new TimelineDataVal();
	    					newVal.setCount(layerCount);
	    					newVal.setDate(Long.toString(curDate.getTime()));
	    					newvals.add(newVal);
	
	    					// get next currdate
	    					if (curDateIter.hasNext()) {
	    						curDate=curDateIter.next();
	    					} else {
	    						curDate=null;
	    					}
	    				}
	    			}
	    			vals.addAll(newvals);
	    			
	    			// If system returned maxresults, remove first date
	    			if (!bTruncated && (resultcnt>=this.maxResults)) {
	    				vals.pollFirst();
	    			}
	    			
	    			// walk thru vals to set to zoom window boundary values
	    			tlDataValIter = vals.iterator();	    			
	    			long cur_epochsec=0;
	    			long last_epochsec=0;
	    			TimelineDataVal last_val=null;
	    			newvals=new ArrayList<TimelineDataVal>();
	    			List<TimelineDataVal> delvals=new ArrayList<TimelineDataVal>();
	    			// Find zoomstart boundary and duplicate previous value at boundry
	    			while (tlDataValIter.hasNext()) {
		    			tlData=tlDataValIter.next();
		    			String layerDateStr=tlData.getDate();
		        		layerDate = new Date(Long.parseLong(layerDateStr));
		        		cur_epochsec=layerDate.getTime()/1000;
		        			
		        		// Set value of beginning of Selected zoomRange to previous date before zoomRange
	    				if ((cur_epochsec>this.selDateMin) && (last_epochsec<this.selDateMin) && (last_epochsec!=0)) {
	    					// insert a zero value to layer on curdate
	    					TimelineDataVal newVal=new TimelineDataVal();
	    					newVal.setCount(last_val.getCount());
	    					newVal.setDate(Long.toString(this.selDateMin*1000));
	    					newvals.add(newVal); 
		    			}	
	    				if (cur_epochsec<this.selDateMin) {
	    					delvals.add(tlData); 
	    				}
	    				last_val=tlData;
	    				last_epochsec=cur_epochsec;
	    			}
	    			// Set value of end of zoomRange to last date in range
	    			if (last_epochsec<this.selDateMax) {
    					TimelineDataVal newVal=new TimelineDataVal();
    					newVal.setCount(last_val.getCount());
    					newVal.setDate(Long.toString(this.selDateMax*1000));
    					newvals.add(newVal); 
	    			}
	    			vals.addAll(newvals);
	    			vals.removeAll(delvals);
	    			layer2.setValues(vals);
    			}
    			// System.out.println("Total Values:"+vals.size()+"    DateList Size:"+datelist.size());
     		}             
                                 
    	} catch (Exception xse) {
			System.out.println("layers:"+tlayers);
    		throw new ProcessorException("Error whie parsing field dependent timeline values", xse);
    	}	               
        return tlayers;
    }
}
