package com.autonomy.find.util;

public enum LoginStatus {
	SUCCESS,FAIL,ERROR;

	private int value;
	private String message;
	public String getErrorMessage() {
		return message;
	}
	public void setErrorMessage(String message) {
		this.message = message;
	}
	
}
