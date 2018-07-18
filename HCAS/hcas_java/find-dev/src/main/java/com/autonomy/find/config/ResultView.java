package com.autonomy.find.config;

import lombok.Data;

@Data
public class ResultView {
    private String simple;
    private String fieldCheck;
    private String measurement3;
    private String measurement4;
    
	public String getSimple() {
		return simple;
	}
	public void setSimple(String simple) {
		this.simple = simple;
	}
	public String getFieldCheck() {
		return fieldCheck;
	}
	public void setFieldCheck(String fieldCheck) {
		this.fieldCheck = fieldCheck;
	}
	public String getMeasurement3() {
		return measurement3;
	}
	public void setMeasurement3(String measurement3) {
		this.measurement3 = measurement3;
	}
	public String getMeasurement4() {
		return measurement4;
	}
	public void setMeasurement4(String measurement4) {
		this.measurement4 = measurement4;
	}
	
}
