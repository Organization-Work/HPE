//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2016.05.17 at 06:33:42 AM EDT 
//


package com.autonomy.find.fields;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for selectSection complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="selectSection">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="selectOperator" type="{http://www.autonomy.com/find/fields}selectOperator" minOccurs="0"/>
 *         &lt;element name="distinct" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/>
 *          &lt;element name="whereExpression" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *          &lt;element name="outerWhereExpression" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "selectSection", propOrder = {
    "selectOperator",
    "distinct",
    "whereExpression",
    "outerWhereExpression"
})
public class SelectSection {

    protected SelectOperator selectOperator;
    @XmlElement(defaultValue = "false")
    protected Boolean distinct = Boolean.FALSE;
    protected String whereExpression;
    protected String outerWhereExpression;

    /**
     * Gets the value of the selectOperator property.
     * 
     * @return
     *     possible object is
     *     {@link SelectOperator }
     *     
     */
    public SelectOperator getSelectOperator() {
        return selectOperator;
    }

    /**
     * Sets the value of the selectOperator property.
     * 
     * @param value
     *     allowed object is
     *     {@link SelectOperator }
     *     
     */
    public void setSelectOperator(SelectOperator value) {
        this.selectOperator = value;
    }

    /**
     * Gets the value of the distinct property.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isDistinct() {
        return distinct;
    }

    /**
     * Sets the value of the distinct property.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setDistinct(Boolean value) {
        this.distinct = value;
    }

	public String getWhereExpression() {
		return whereExpression;
	}

	public void setWhereExpression(String whereExpression) {
		this.whereExpression = whereExpression;
	}

	public String getOuterWhereExpression() {
		return outerWhereExpression;
	}

	public void setOuterWhereExpression(String outerWhereExpression) {
		this.outerWhereExpression = outerWhereExpression;
	}


}