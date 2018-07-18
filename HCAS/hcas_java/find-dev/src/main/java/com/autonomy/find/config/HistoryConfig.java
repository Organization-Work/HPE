package com.autonomy.find.config;

import lombok.Data;

import org.springframework.stereotype.Service;

@Service
@Data
public class HistoryConfig {

	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public int getAciPort() {
		return aciPort;
	}
	public void setAciPort(int aciPort) {
		this.aciPort = aciPort;
	}
	public int getIndexPort() {
		return indexPort;
	}
	public void setIndexPort(int indexPort) {
		this.indexPort = indexPort;
	}
	public void setDatabase(String database) {
		this.database = database;
	}
	private String	host;
	private int		aciPort;
	private int		indexPort;
	private String	database;
	public String getDatabase() {
		return database;
	}

}
