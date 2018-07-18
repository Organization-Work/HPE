package com.autonomy.find.dto;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown=true)
public class ClusterJob {

	private String name;
	private String numClusters;
	
}
