package com.autonomy.vertica.table.mapping;

import java.util.ArrayList;
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
 * &lt;complexType name="tableRelationGroup">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence> *        
 *          &lt;element name="tableRelations" type="{http://www.autonomy.com/vertica/fields}tableRelationGroup" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tableRelationGroup", propOrder = {   
    "tableRelation"
})
public class TableRelationGroup {
	@XmlElement(required = true)
	protected List<TableRelationList>  tableRelation;

	public List<TableRelationList> getTableRelation() {
		 if (tableRelation == null) {
			 tableRelation = new ArrayList<TableRelationList>();
	        }
		return this.tableRelation;
	}	
}
