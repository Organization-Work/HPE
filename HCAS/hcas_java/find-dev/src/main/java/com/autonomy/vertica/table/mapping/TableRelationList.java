package com.autonomy.vertica.table.mapping;

import java.util.List;

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
 * &lt;complexType name="tableRelationList">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *       &lt;element name="View" type="{http://www.w3.org/2001/XMLSchema}String"/>
 *         &lt;element name="relation" type="{http://www.autonomy.com/vertica/fields}relation" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tableRelation", propOrder = {
	"view",
    "relation"   
})
public class TableRelationList {
	@XmlElement(required = true)
	protected String view;
	@XmlElement(required = false)	
	protected List<TableRelationObject> relation;
	
	public String getView() {
		return view;
	}
	public void setView(String view) {
		this.view = view;
	}
	public List<TableRelationObject> getRelation() {
		return relation;
	}	
	
}
