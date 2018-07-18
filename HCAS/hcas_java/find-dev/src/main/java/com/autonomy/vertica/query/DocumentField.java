package com.autonomy.vertica.query;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class DocumentField {
	 /** Holds value of property name. */
    private String name;

    /** Holds value of property attributes. */
    private Map<String, String> attributes = new LinkedHashMap<String, String>();

    /** Holds value of property value. */
    private String value;

    /**
     * Default constructor.
     */
    public DocumentField() {
        super();
    }
    
    /**
     * Value constructor.
     */
    public DocumentField(final String name, final String value) {
        this(name, new LinkedHashMap<String, String>(), value);
    }
    
    /**
     * Value constructor.
     */
    public DocumentField(final String name, final Map<String, String> attributes, final String value) {
        super();
        this.name       = name;
        this.attributes = attributes;
        this.value      = value;
    }
    
    /**
     * Adds an attribute name and value to this document field.
     *
     * @param name The name of the attribute to add.
     * @param value the value of the attribute to add.
     */
    public void addAttribute(final String name, final String value) {
        attributes.put(name, value);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
    
    /**
     * Checks to see if the passed in <tt>obj</tt> is equal. 
     * 
     * @param obj The object to compare against
     * @return <tt>true</tt> if <tt>obj</tt> is equal, <tt>false</tt> otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        boolean returnValue = false;
        
        if(obj instanceof DocumentField) {
            if(this == obj) {
                returnValue = true;
            }
            else {
                // Cast it...
                final DocumentField rhs = (DocumentField) obj;

                // Check the name of this parameter with the passed in parameter...
                returnValue = new EqualsBuilder()
                        .append(name, rhs.name)
                        .append(attributes, rhs.attributes)
                        .append(value, rhs.value)
                        .isEquals();
            }
        }
        
        return returnValue;
    }
    
    /**
     * Returns a hash code value for the object.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder(23, 53)
                .append(name)
                .append(attributes)
                .append(value)
                .hashCode();
    }
    
    /**
     * Getter for property name.
     * @return Value of property name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Setter for property name.
     * @param name New value of property name.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Getter for property attributes.
     * @return Value of property attributes.
     */
    public Map<String, String> getAttributes() {
        return this.attributes;
    }

    /**
     * Setter for property attributes.
     * @param attributes New value of property attributes.
     */
    public void setAttributes(final Map<String, String> attributes) {
        this.attributes = attributes;
    }

    /**
     * Getter for property value.
     * @return Value of property value.
     */
    public String getValue() {
        return this.value;
    }

    /**
     * Setter for property value.
     * @param value New value of property value.
     */
    public void setValue(final String value) {
        this.value = value;
    }
    
} // End of class DocumentField...

