//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.05.17 at 06:33:42 AM EDT 
//


package com.autonomy.find.fields;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for paraField complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="paraField">
 *   &lt;complexContent>
 *     &lt;extension base="{http://www.autonomy.com/find/fields}idolField">
 *       &lt;sequence>
 *         &lt;element name="ranges" minOccurs="0">
 *           &lt;simpleType>
 *             &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *               &lt;pattern value="\{(\.|\d+),(\d+,)*(\.|\d+)\}"/>
 *             &lt;/restriction>
 *           &lt;/simpleType>
 *         &lt;/element>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "paraField", propOrder = {
    "ranges"
})
public class ParaField
    extends IdolField
{

    protected String ranges;

    /**
     * Gets the value of the ranges property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRanges() {
        return ranges;
    }

    /**
     * Sets the value of the ranges property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRanges(String value) {
        this.ranges = value;
    }

}
