package com.autonomy.find.config;

import org.springframework.stereotype.Service;

import lombok.Data;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Data
@Service
public class AgentConfig {

	public boolean isDefaultUseMinScore() {
		return defaultUseMinScore;
	}

	public void setDefaultUseMinScore(boolean defaultUseMinScore) {
		this.defaultUseMinScore = defaultUseMinScore;
	}

	public boolean isDefaultUnreadOnly() {
		return defaultUnreadOnly;
	}

	public void setDefaultUnreadOnly(boolean defaultUnreadOnly) {
		this.defaultUnreadOnly = defaultUnreadOnly;
	}

	public String getDatabasesString() {
		return databasesString;
	}

	public void setDatabasesString(String databasesString) {
		this.databasesString = databasesString;
	}

	public void setDefaultMaxResults(int defaultMaxResults) {
		this.defaultMaxResults = defaultMaxResults;
	}

	public void setDefaultMinScore(int defaultMinScore) {
		this.defaultMinScore = defaultMinScore;
	}

	private int defaultMaxResults;
	private int defaultMinScore;
	private boolean defaultUseMinScore;
	private boolean defaultUnreadOnly;
    private String databasesString;

    public List<String> getDatabaseOptions() {
        final List<String> result = new LinkedList<String>();
        Collections.addAll(result, databasesString.split("\\s*\\+\\s*"));
        return result;
    }

	public int getDefaultMinScore() {
		return defaultMinScore;
	}

	public int getDefaultMaxResults() {
		return defaultMaxResults;
	}
}
