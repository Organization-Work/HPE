package com.autonomy.find.config;

import lombok.Data;

import org.springframework.stereotype.Service;

@Data
@Service
public class CategoryConfig {
	
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getAciPort() {
		return aciPort;
	}
	public void setAciPort(String aciPort) {
		this.aciPort = aciPort;
	}
	public void setHot_jobName(String hot_jobName) {
		this.hot_jobName = hot_jobName;
	}
	public void setHot_numClusters(String hot_numClusters) {
		this.hot_numClusters = hot_numClusters;
	}
	public void setHot_numResults(String hot_numResults) {
		this.hot_numResults = hot_numResults;
	}
	public void setHot_anyLanguage(String hot_anyLanguage) {
		this.hot_anyLanguage = hot_anyLanguage;
	}
	public void setHot_outputEncoding(String hot_outputEncoding) {
		this.hot_outputEncoding = hot_outputEncoding;
	}
	public void setBreaking_jobName(String breaking_jobName) {
		this.breaking_jobName = breaking_jobName;
	}
	public void setBreaking_numClusters(String breaking_numClusters) {
		this.breaking_numClusters = breaking_numClusters;
	}
	public void setBreaking_numResults(String breaking_numResults) {
		this.breaking_numResults = breaking_numResults;
	}
	public void setBreaking_anyLanguage(String breaking_anyLanguage) {
		this.breaking_anyLanguage = breaking_anyLanguage;
	}
	public void setBreaking_outputEncoding(String breaking_outputEncoding) {
		this.breaking_outputEncoding = breaking_outputEncoding;
	}
	private String host;
	private String aciPort;
	
	private String hot_jobName;
	private String hot_numClusters;
	private String hot_numResults;
	private String hot_anyLanguage;
	private String hot_outputEncoding;
	
	private String breaking_jobName;
	private String breaking_numClusters;
	private String breaking_numResults;
	private String breaking_anyLanguage;
	private String breaking_outputEncoding;
	public Object getBreaking_jobName() {
		return breaking_jobName;
	}
	public Object getBreaking_numClusters() {
		return breaking_numClusters;
	}
	public Object getBreaking_numResults() {
		return breaking_numResults;
	}
	public Object getBreaking_anyLanguage() {
		return breaking_anyLanguage;
	}
	public Object getBreaking_outputEncoding() {
		return breaking_outputEncoding;
	}
	public Object getHot_jobName() {
		// TODO Auto-generated method stub
		return hot_jobName;
	}
	public Object getHot_numClusters() {
		// TODO Auto-generated method stub
		return hot_numClusters;
	}
	public Object getHot_numResults() {
		// TODO Auto-generated method stub
		return hot_numResults;
	}
	public Object getHot_anyLanguage() {
		// TODO Auto-generated method stub
		return hot_anyLanguage;
	}
	public Object getHot_outputEncoding() {
		// TODO Auto-generated method stub
		return hot_outputEncoding;
	}
}
