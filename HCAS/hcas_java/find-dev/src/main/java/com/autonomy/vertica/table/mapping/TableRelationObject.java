package com.autonomy.vertica.table.mapping;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for table complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="tableRelationObject">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *        	&lt;element name="joinNumber" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         	&lt;element name="table" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         	&lt;element name="refCol" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         	&lt;element name="JoinCol" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         	&lt;element name="parentTable" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         	&lt;element name="parentCol" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         	&lt;element name="extraColumns" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *          &lt;element name="extraParentColumns" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *          &lt;element name="joinExp" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *          &lt;element name="whereExp" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tableRelationObject", propOrder = {
		"joinNumber",
	    "table",
	    "refCol",
	    "joinCol",
	    "parentTable",
	    "parentCol",
	    "extraColumns",
	    "extraParentColumns",
	    "joinExp",
	    "whereExp"
})
public class TableRelationObject {
	@XmlElement(required = true)
	protected int joinNumber;
	@XmlElement(required = true)
	protected String table;
	@XmlElement(required = true)
	protected String refCol;
	@XmlElement(required = true)
	protected String joinCol;	
	@XmlElement(required = true)
	protected String parentTable;
	@XmlElement(required = false)
	protected String parentCol;
	@XmlElement(required = false)
	protected String extraColumns;
	@XmlElement(required = false)
	protected String extraParentColumns;
	@XmlElement(required = false)
	protected String joinExp;
	@XmlElement(required = false)
	protected String whereExp;
	
	
	public int getJoinNumber() {
		return joinNumber;
	}
	public void setJoinNumber(int joinNumber) {
		this.joinNumber = joinNumber;
	}
	public String getTable() {
		return table;
	}
	public void setTable(String table) {
		this.table = table;
	}
	public String getRefCol() {
		return refCol;
	}
	public void setRefCol(String refCol) {
		this.refCol = refCol;
	}
	public String getJoinCol() {
		return joinCol;
	}
	public void setJoinCol(String joinCol) {
		this.joinCol = joinCol;
	}
	public String getParentTable() {
		return parentTable;
	}
	public void setParentTable(String parentTable) {
		this.parentTable = parentTable;
	}
	public String getParentCol() {
		return parentCol;
	}
	public void setParentCol(String parentCol) {
		this.parentCol = parentCol;
	}
	public String getExtraColumns() {
		return extraColumns;
	}
	public void setExtraColumns(String extraColumns) {
		this.extraColumns = extraColumns;
	}
	public String getExtraParentColumns() {
		return extraParentColumns;
	}
	public void setExtraParentColumns(String extraParentColumns) {
		this.extraParentColumns = extraParentColumns;
	}
	public String getJoinExp() {
		return joinExp;
	}
	public void setJoinExp(String joinExp) {
		this.joinExp = joinExp;
	}
	public String getWhereExp() {
		return whereExp;
	}
	public void setWhereExp(String whereExp) {
		this.whereExp = whereExp;
	}
	
	
}