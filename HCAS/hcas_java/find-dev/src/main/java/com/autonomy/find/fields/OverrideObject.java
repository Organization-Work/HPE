package com.autonomy.find.fields;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for overrideObject complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="overrideObject">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="resultViewName" type="{http://www.w3.org/2001/XMLSchema}String" minOccurs="0"/>    
 *          &lt;element name="columnName" type="{http://www.w3.org/2001/XMLSchema}String" minOccurs="0"/>      
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "overrideObject", propOrder = {
    "resultViewName",
    "columnName"
})
public class OverrideObject {
	protected String resultViewName;
	protected String columnName;
	
	public String getResultViewName() {
		return resultViewName;
	}
	public void setResultViewName(String resultViewName) {
		this.resultViewName = resultViewName;
	}
	public String getColumnName() {
		return columnName;
	}
	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

}
