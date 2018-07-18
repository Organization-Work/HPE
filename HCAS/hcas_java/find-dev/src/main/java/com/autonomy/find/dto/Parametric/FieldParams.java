package com.autonomy.find.dto.Parametric;

import java.util.List;
import java.util.Set;

import com.autonomy.find.fields.FilterOperator;

import lombok.Data;

@Data
public class FieldParams {
    private char type;
    private FilterOperator op;
    private String val;
    private List<String> listVal;
	public char getType() {
		return type;
	}
	public FilterOperator getOp() {
		return op;
	}
	public String getVal() {
		return val;
	}
	public void setOp(FilterOperator is) {
		this.op=is;
	}
	public void setType(char c) {
		this.type=c;
	}
	public void setVal(String filterValue) {
		this.val=filterValue;
	}

}
