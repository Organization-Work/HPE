package com.autonomy.find.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestTracker {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RequestTracker.class);	
	
	private Map<RequestProcess, RequestState> trackerMap;
	
	public void setRequestState(RequestProcess process, RequestState s) {
		if(trackerMap == null) {
			trackerMap = new HashMap<RequestProcess, RequestState>();
		}
		trackerMap.put(process, s);
	}
	
	public RequestState getRequestState(RequestProcess process) {
		return trackerMap.get(process);
	}
	
	public boolean isActive() {
		if(trackerMap != null) {
			for(Map.Entry<RequestProcess, RequestState> entry : trackerMap.entrySet()) {
				if(entry.getValue() == RequestState.ACTIVE) {
					return true;
				}
			}
		}
		return false;
	}
	
	public void stopOtherActiveProcesses(RequestProcess process) {
		List<RequestProcess> stopProcessList = new ArrayList<RequestProcess>();
		if(trackerMap != null) {
			for(Map.Entry<RequestProcess, RequestState> entry : trackerMap.entrySet()) {
				if(entry.getValue() == RequestState.ACTIVE && process != entry.getKey()) {
					stopProcessList.add(entry.getKey());
				}
			}
		}
		for(RequestProcess stopProcess : stopProcessList) {
			trackerMap.put(stopProcess, RequestState.STOP);			
		}		
	}
	
	public void printAllProcessRequestStates(String msg) {
		LOGGER.debug(msg);
		if(trackerMap != null) { 
			for(Map.Entry<RequestProcess, RequestState> entry : trackerMap.entrySet()) {
				LOGGER.debug(entry.getKey().toString() + " : " + entry.getValue().toString());
			}
		}
	}

}
