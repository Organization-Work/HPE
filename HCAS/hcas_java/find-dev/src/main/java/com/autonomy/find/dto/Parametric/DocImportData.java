package com.autonomy.find.dto.Parametric;

import lombok.Data;

@Data
public class DocImportData {
    private String searchView;
    private String importData;
    private DocImportType importType;
    private int folderId;
    private boolean ignoreFirstRow;
	public int getFolderId() {
		return folderId;
	}
	public String getImportData() {
		return importData;
	}
	public boolean isIgnoreFirstRow() {
		return ignoreFirstRow;
	}
	public DocImportType getImportType() {
		return importType;
	}
	public String getSearchView() {
		return searchView;
	}
	public void setSearchView(String searchView) {
		this.searchView = searchView;
	}
	public void setImportData(String importData) {
		this.importData = importData;
	}
	public void setImportType(DocImportType importType) {
		this.importType = importType;
	}
	public void setFolderId(int folderId) {
		this.folderId = folderId;
	}
	public void setIgnoreFirstRow(boolean ignoreFirstRow) {
		this.ignoreFirstRow = ignoreFirstRow;
	}
}
