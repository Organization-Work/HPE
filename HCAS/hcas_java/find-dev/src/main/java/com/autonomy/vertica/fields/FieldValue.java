package com.autonomy.vertica.fields;

import java.util.Date;

public class FieldValue {
	
	  private String value;
		private double count;

	    private Date date;

	   public String getValue() {
		return value;
	}
	    
	    public FieldValue(final String value, final double count, final Date date) {
	        this.value = value;
	        this.count = count;
	        this.date = (date == null) ? new Date(1893455999) : new Date(date.getTime());
	    }


	public FieldValue() {
		
	}

	public void setValue(String value) {
		this.value = value;
	}

	public double getCount() {
		return count;
	}

	public void setCount(double count) {
		this.count = count;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		if (date==null) {
			this.date = new Date(1893455999);		
		} else {
			this.date = date;
		}
	}

	

}
