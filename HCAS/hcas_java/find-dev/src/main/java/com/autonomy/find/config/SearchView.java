package com.autonomy.find.config;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class SearchView {
	 
    public void setName(String name) {
		this.name = name;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public void setSnomedTag(String snomedTag) {
		this.snomedTag = snomedTag;
	}

	public void setSnomedParentTag(String snomedParentTag) {
		this.snomedParentTag = snomedParentTag;
	}

	public void setDocImportFile(String docImportFile) {
		this.docImportFile = docImportFile;
	}

	public void setDefaultView(boolean defaultView) {
		this.defaultView = defaultView;
	}

	public void setIdolRootElement(String idolRootElement) {
		this.idolRootElement = idolRootElement;
	}

	public void setDocRefField(String docRefField) {
		this.docRefField = docRefField;
	}

	public void setPtCohortField(String ptCohortField) {
		this.ptCohortField = ptCohortField;
	}

	public void setFilterFieldsFile(String filterFieldsFile) {
		this.filterFieldsFile = filterFieldsFile;
	}

	public void setDocviewFile(String docviewFile) {
		this.docviewFile = docviewFile;
	}
	
	public void setDocviewMappingFile(String docviewMappingFile) {
		this.docviewMappingFile = docviewMappingFile;
	}

	public void setResultView(ResultView resultView) {
		this.resultView = resultView;
	}
	public void setPopView(PopView popView) {
		this.popView = popView;
	}
	
	 public void setRepository(String repository) {
	    	this.repository = repository;
	 }
	 
	 public void setDocFolderSourceDB(String docFolderSourceDB) {
	    	this.docFolderSourceDB = docFolderSourceDB;
	 }
	 
	 public void setDocFolderSourceDBSchema(String docFolderSourceDBSchema) {
			this.docFolderSourceDBSchema = docFolderSourceDBSchema;
		}

	private String name;
    private String displayName;
    private String database;
    private String snomedTag;
    private String snomedParentTag;
    private String docImportFile;
    private boolean defaultView = false;
    private String idolRootElement;
    private String docRefField;
    private String ptCohortField;
    private String filterFieldsFile;
    private String docviewFile;
    private String docviewMappingFile;
    private ResultView resultView;
    private PopView popView;
    private String repository;
    private Map<String, DisplayField> displayFields;
    private Trending trending;
    private String documentSource; // fileSystem or IDOL
    private String documentSubDirectoryNameOffset;
    private String verticaSchema;
    private boolean totalCountDistinct = true;
    private int topicMapMaxCount = 5;
    private String docFolderSourceDB;
    private String docFolderSourceDBSchema;
    private boolean filterQueryDistinct = false;
    private boolean docViewEnabled = true;
    private boolean resultsListDistinct = true;
    private boolean resultsListGroupBy = true;
    private Map<String, Boolean> resultViewCountDistinct;
	private boolean addWhereForGroupFieldsToOuterQuery = true;
	//private boolean displayCohort = false;
	private List<String> displayCohortUsersList;
   
    
    public String getRepository() {
    	return repository;
    }

	public String getName() {
		return name;
	}
	
	public String getDisplayName() {
		return displayName;
	}
    public String getDatabase() {
		return database;
	}
    
    public String getSnomedTag() {
		return snomedTag;
	}

    public String getSnomedParentTag() {
		return snomedParentTag;
	}
	public String getDocImportFile() {
		return docImportFile;
	}

    public boolean isDefaultView() {
		return defaultView;
	}
	public String getIdolRootElement() {
		return idolRootElement;
	}
	public String getDocRefField() {
		return docRefField;
	}
	public String getPtCohortField() {
		return ptCohortField;
	}
	public String getFilterFieldsFile() {
		return filterFieldsFile;
	}
	public String getDocviewFile() {
		return docviewFile;
	}
	public String getDocviewMappingFile() {
		return docviewMappingFile;
	}
	public ResultView getResultView() {
		return resultView;
	}

	public PopView getPopView() {
		return popView;
	}	
	
	public String getDocFolderSourceDB() {
    	return docFolderSourceDB;
    }
	
	public String getDocFolderSourceDBSchema() {
		return docFolderSourceDBSchema;
	}
   //TODO: delete these two
    //private String parametricFieldsFile;
	

}
