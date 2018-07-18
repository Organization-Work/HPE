package com.autonomy.vertica.fields;

import java.util.LinkedList;
import java.util.List;



public class Field {
	/** The name of the field. */
    private String name;
    
    /** The number of values returned by the action. */
    private double numValues;
    
    /** The total number of values that could be returned. */
    private double totalValues;

    /** The count of the set of numeric values in fields matched by the parametric query.  */
    private double values;

    /** The sum of the set of numeric values in fields matched by the parametric query.  */
    private float valueSum;

    /** The arithmetic average of the set of numeric values in fields matched by the parametric query.  */
    private float valueAverage;

    private float valueMin;

    private float valueMax;

    /** The returned field values. */
    private List<FieldValue> fieldValues = new LinkedList<FieldValue>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getNumValues() {
		return numValues;
	}

	public void setNumValues(double numValues) {
		this.numValues = numValues;
	}

	public double getTotalValues() {
		return totalValues;
	}

	public void setTotalValues(double totalValues) {
		this.totalValues = totalValues;
	}

	public double getValues() {
		return values;
	}

	public void setValues(double values) {
		this.values = values;
	}

	public float getValueSum() {
		return valueSum;
	}

	public void setValueSum(float valueSum) {
		this.valueSum = valueSum;
	}

	public float getValueAverage() {
		return valueAverage;
	}

	public void setValueAverage(float valueAverage) {
		this.valueAverage = valueAverage;
	}

	public float getValueMin() {
		return valueMin;
	}

	public void setValueMin(float valueMin) {
		this.valueMin = valueMin;
	}

	public float getValueMax() {
		return valueMax;
	}

	public void setValueMax(float valueMax) {
		this.valueMax = valueMax;
	}

	public List<FieldValue> getFieldValues() {
		return fieldValues;
	}

	public void setFieldValues(List<FieldValue> fieldValues) {
		this.fieldValues = fieldValues;
	}
}
