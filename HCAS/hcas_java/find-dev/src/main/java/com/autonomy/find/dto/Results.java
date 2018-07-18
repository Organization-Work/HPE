package com.autonomy.find.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/dto/Results.java#2 $
 * <p/>
 * Copyright (c) 2013, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: wo $ on $Date: 2014/02/24 $
 */
@Data
public class Results {
	private final List<ResultDocument> docs;
	// map of database names to totalResults
	private final Map<String, Double> totalResults;
    private String resultStateMatchId;
    private String resultStateDontMatchId;
    private String sqlQuery;

    public Results(final List<ResultDocument> docs, final Map<String, Double> totalResults) {
        this.docs = docs;
        this.totalResults = totalResults;
    }

    public Results(final List<ResultDocument> docs, final Map<String, Double> totalResults, final String resultStateMatchId, final String resultStateDontMatchId, final String sqlQuery ) {
        this(docs, totalResults);
        this.resultStateMatchId = resultStateMatchId;
        this.resultStateDontMatchId = resultStateDontMatchId;
        this.sqlQuery = sqlQuery;
    }

	public List<ResultDocument> getDocs() {
		return docs;
	}


}
