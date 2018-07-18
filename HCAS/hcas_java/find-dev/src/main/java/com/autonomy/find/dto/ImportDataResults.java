package com.autonomy.find.dto;

public final class ImportDataResults {
	private final int documentCount;
	private final String documentMessage;
	
	public ImportDataResults(int documentCount, String documentMessage){
		this.documentCount = documentCount;
		this.documentMessage = documentMessage;
	}
	public int getDocumentCount(){
		return documentCount;
	}
	public String getDocumentMessage(){
		return documentMessage;
	}
}
