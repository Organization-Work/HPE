package com.autonomy.find.config;

import lombok.Data;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Data
@Service
public class FindNewsConfig {


	private String host;
	private String aciPort;

	private String clusterNamesFile;
    private String appVersionFile;
	private int defaultMaxResults;
	private int numResults;
	private boolean defaultHeadlines;
	private String clusterAnyLanguage;
	private String clusterOutputEncoding;
	private String searchDatabase;
	private int searchMaxResults;
    private String agentDatabases;

    private int profileTopicsVoronoiEngine;

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

	public String getAgentDatabases() {
		return agentDatabases;
	}

	public void setAgentDatabases(String agentDatabases) {
		this.agentDatabases = agentDatabases;
	}

	public void setClusterNamesFile(String clusterNamesFile) {
		this.clusterNamesFile = clusterNamesFile;
	}

	public void setAppVersionFile(String appVersionFile) {
		this.appVersionFile = appVersionFile;
	}

	public void setDefaultMaxResults(int defaultMaxResults) {
		this.defaultMaxResults = defaultMaxResults;
	}

	public void setNumResults(int numResults) {
		this.numResults = numResults;
	}

	public void setDefaultHeadlines(boolean defaultHeadlines) {
		this.defaultHeadlines = defaultHeadlines;
	}

	public void setClusterAnyLanguage(String clusterAnyLanguage) {
		this.clusterAnyLanguage = clusterAnyLanguage;
	}

	public void setClusterOutputEncoding(String clusterOutputEncoding) {
		this.clusterOutputEncoding = clusterOutputEncoding;
	}

	public void setSearchDatabase(String searchDatabase) {
		this.searchDatabase = searchDatabase;
	}

	public void setSearchMaxResults(int searchMaxResults) {
		this.searchMaxResults = searchMaxResults;
	}

	public void setProfileTopicsVoronoiEngine(int profileTopicsVoronoiEngine) {
		this.profileTopicsVoronoiEngine = profileTopicsVoronoiEngine;
	}

	public List<String> getAgentDatabasesList() {
        final List<String> result = new LinkedList<String>();
        Collections.addAll(result, this.agentDatabases.split("\\+"));
        return result;
    }

	public int getProfileTopicsVoronoiEngine() {
		return profileTopicsVoronoiEngine;
	}

	public Integer getSearchMaxResults() {
		return searchMaxResults;
	}

	public String getSearchDatabase() {
		return searchDatabase;
	}

	public Integer getDefaultMaxResults() {
		return defaultMaxResults;
	}

	public Boolean isDefaultHeadlines() {
		return defaultHeadlines;
	}

	public String getAppVersionFile() {
		return appVersionFile;
	}

	public String getClusterAnyLanguage() {
		return clusterAnyLanguage;
	}

	public int getNumResults() {
		return numResults;
	}

	public String getClusterOutputEncoding() {
		return clusterOutputEncoding;
	}

	public String getClusterNamesFile() {
		return clusterNamesFile;
	}
}
