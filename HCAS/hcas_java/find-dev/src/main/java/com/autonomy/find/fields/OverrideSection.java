package com.autonomy.find.fields;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>Java class for overrideSection complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="overrideSection">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="override" type="{http://www.w3.org/2001/XMLSchema}overrideObject" minOccurs="0" maxOccurs="unbounded"/> *        
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "overrideSection", propOrder = {
    "override"   
})
public class OverrideSection {
	protected List<OverrideObject> override;

	public List<OverrideObject> getOverride() {
		 if (override == null) {
			 override = new ArrayList<OverrideObject>();
	     }
		 return this.override;		
	}

}
