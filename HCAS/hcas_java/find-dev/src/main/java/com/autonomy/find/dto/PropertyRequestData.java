package com.autonomy.find.dto;

import java.util.HashMap;

import lombok.Data;

@Data
public class PropertyRequestData {
	private String session;
	private HashMap<String, Object> data;
	public String getSession() {
		// TODO Auto-generated method stub
		return session;
	}
	public HashMap<String, Object> getData() {
		return data;
	}
}
