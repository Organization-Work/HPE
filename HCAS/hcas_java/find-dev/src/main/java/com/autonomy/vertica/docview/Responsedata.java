//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.05.04 at 07:04:07 AM EDT 
//


package com.autonomy.vertica.docview;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for responsedata complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="responsedata">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="numhits" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="hit" type="{http://schemas.autonomy.com/aci/}hit"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "responsedata", propOrder = {
    "numhits",
    "hit"
})
public class Responsedata {

    protected int numhits;
    @XmlElement(required = true)
    protected Hit hit;

    /**
     * Gets the value of the numhits property.
     * 
     */
    public int getNumhits() {
        return numhits;
    }

    /**
     * Sets the value of the numhits property.
     * 
     */
    public void setNumhits(int value) {
        this.numhits = value;
    }

    /**
     * Gets the value of the hit property.
     * 
     * @return
     *     possible object is
     *     {@link Hit }
     *     
     */
    public Hit getHit() {
        return hit;
    }

    /**
     * Sets the value of the hit property.
     * 
     * @param value
     *     allowed object is
     *     {@link Hit }
     *     
     */
    public void setHit(Hit value) {
        this.hit = value;
    }

}
