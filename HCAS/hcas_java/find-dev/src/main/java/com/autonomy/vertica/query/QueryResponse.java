package com.autonomy.vertica.query;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class QueryResponse {
	
	 /** The number of hits returned by this response. */
    private double numHits;
    
    /** The total number of hits matched by this response. Might not be 100% accurate if <tt>Predict=true</tt> was set
        and will be <tt>0</tt> if <tt>TotalResults=true</tt> was not specified. */
    private double totalHits;
    
    /** The total number of documents that were touched during the action... */
    private int totalDbDocs;
    
    /** The total number of sections that were touched during the action... */
    private int totalDbSecs;
    
    /** If <tt>Predict=false</tt> is specified, then this list contains objects that represent each element within the
        <tt>&lt;autn:databasehits&gt;</tt> tag. */
   // private List<DatabaseHit> databaseHits = new LinkedList<DatabaseHit>();
    
    /** Holds the basic query summary terms... */
    private List<String> querySummary = new LinkedList<String>();
    
    /** Holds all the advanced query summary elements... */
  //  private List<QuerySummaryElement> advancedQuerySummary = new LinkedList<QuerySummaryElement>();
    
    /** If <tt>SpellCheck=true</tt> is specified and a word is miss-spelt, then this contains that word. */
    private String spelling;
    
    /** If <tt>SpellCheck=true</tt> is specified and a word is miss-spelt, then this contains the query text with the 
        word spelt correctly... */
    private String spellingQuery;
    
    /** The store state token, will only be non-null if <tt>StoreState=true</tt> was specified... */
    private String state;
    
    /** Contains all the documents that were returned in the response... */
    private List<Document> documents = new LinkedList<Document>();

    /** Contains all engine details when a query has been sent via a DAH and <tt>StoredStateDetail=true</tt> has been
        sent. */
   // private List<Engine> engines = new LinkedList<Engine>();

    /** Holds any warnings that came back in the query response. */
    private List<String> warnings = new LinkedList<String>();
    
    /** Holds the sql query executed in Vertica */
    private String sqlQuery;
    
    /**
     * Adds an object representing the number of matching hits in a database from the database hits portion of the ACI 
     * response.
     * 
     * @param idolDatabase The object contain the database info to be added
     *//*
    public void addIdolDatabase(final DatabaseHit idolDatabase) {
        databaseHits.add(idolDatabase);
    }
    
    *//**
     * Adds an object representing a single element in the advanced query summary portion of the ACI response.
     * 
     * @param querySummaryElement The element to be added
     *//*
    public void addQuerySummaryElement(final QuerySummaryElement querySummaryElement) {
        advancedQuerySummary.add(querySummaryElement);
    }*/

    /**
     * Adds an object representing a single document in the ACI response.
     * 
     * @param document The document to be added
     */ 
    public void addDocument(final Document document) {
        documents.add(document);
    }

    /**
     * Adds an object representing a single engine in the ACI response.
     *
     * @param engine The engine to be added
     */
   /* public void addEngine(final Engine engine) {
        engines.add(engine);
    }*/

    /**
     * Adds a warning from the ACI response into this object.
     * 
     * @param warning The warning to be added
     */ 
    public void addWarning(final String warning) {
        warnings.add(warning);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
    
   /* public List<DatabaseHit> getDatabaseHits() {
        return databaseHits;
    }

    public void setDatabaseHits(final List<DatabaseHit> databaseHits) {
        this.databaseHits = databaseHits;
    }

    public List<QuerySummaryElement> getAdvancedQuerySummary() {
        return advancedQuerySummary;
    }

    public void setAdvancedQuerySummary(final List<QuerySummaryElement> advancedQuerySummary) {
        this.advancedQuerySummary = advancedQuerySummary;
    }*/

    public double getNumHits() {
        return numHits;
    }

    public void setNumHits(final double numHits) {
        this.numHits = numHits;
    }

    public double getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(final double totalHits) {
        this.totalHits = totalHits;
    }

    public int getTotalDbDocs() {
        return totalDbDocs;
    }

    public void setTotalDbDocs(final int totalDbDocs) {
        this.totalDbDocs = totalDbDocs;
    }

    public int getTotalDbSecs() {
        return totalDbSecs;
    }

    public void setTotalDbSecs(final int totalDbSecs) {
        this.totalDbSecs = totalDbSecs;
    }

    public List<String> getQuerySummary() {
        return querySummary;
    }

    public void setQuerySummary(final List<String> querySummary) {
        this.querySummary = querySummary;
    }

    public String getSpelling() {
        return spelling;
    }

    public void setSpelling(final String spelling) {
        this.spelling = spelling;
    }

    public String getSpellingQuery() {
        return spellingQuery;
    }

    public void setSpellingQuery(final String spellingQuery) {
        this.spellingQuery = spellingQuery;
    }

    public String getState() {
        return state;
    }

    public void setState(final String state) {
        this.state = state;
    }

    public List<Document> getDocuments() {
        return documents;
    }

    public void setDocuments(final List<Document> documents) {
        this.documents = documents;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(final List<String> warnings) {
        this.warnings = warnings;
    }

	public String getSqlQuery() {
		return sqlQuery;
	}

	public void setSqlQuery(String sqlQuery) {
		this.sqlQuery = sqlQuery;
	}

  /*  public List<Engine> getEngines() {
        return engines;
    }

    public void setEngines(final List<Engine> engines) {
        this.engines = engines;
    }*/

} // End of class QueryResponse...



