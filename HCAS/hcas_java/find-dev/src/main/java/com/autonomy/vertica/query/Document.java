package com.autonomy.vertica.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;




public class Document {
	  /** Holds value of property reference. */
    private String reference;

    /** Holds value of property id. */
    private int id;

    /** Holds value of property section. */
    private int section;

    /** Holds value of property weight. */
    private float weight;

    /** Holds value of property links. */
    private List<String> links = new LinkedList<String>();

    /** Holds value of property database. */
    private String database;

    /** Holds value of property title. */
    private String title;
    
    /** Holds value of property summary. */
    private String summary;

    /** Holds value of property baseId. */
    private int baseId;

    /** Holds value of property date. */
    private Date date;

    /** Holds value of property dateString. */
    private String dateString;

    /** Holds value of property expireDate. */
    private Date expireDate;

    /** Holds value of property language. */
    private String language;

    /** Holds value of property languageType. */
    private String languageType;

    /** Holds value of property languageEncoding. */
    private String languageEncoding;

    /** Holds value of property documentFields. */
    private Map<String, List<DocumentField>> documentFields = new LinkedHashMap<String, List<DocumentField>>();

    public Document() {
        super();
    }

    public Document(final String reference, final int id, final int section, final float weight, final List<String> links,
            final String database, final String title, final String summary, final int baseId, final Date date,
            final String dateString, final Date expireDate, final String language, final String languageType,
            final String languageEncoding, final Map<String, List<DocumentField>> documentFields) {
        this.reference = reference;
        this.id = id;
        this.section = section;
        this.weight = weight;
        this.links = (links == null) ? new LinkedList<String>() : new LinkedList<String>(links);
        this.database = database;
        this.title = title;
        this.summary = summary;
        this.baseId = baseId;
        this.date = (date == null) ? null : new Date(date.getTime());
        this.dateString = dateString;
        this.expireDate = (expireDate == null) ? null : new Date(expireDate.getTime());
        this.language = language;
        this.languageType = languageType;
        this.languageEncoding = languageEncoding;
        this.documentFields = (documentFields == null)
                ? new LinkedHashMap<String, List<DocumentField>>()
                : new LinkedHashMap<String, List<DocumentField>>(documentFields);
    }

    public Document(final Document document) {
        this(document.reference, document.id, document.section, document.weight, document.links, document.database,
                document.title, document.summary, document.baseId, document.date, document.dateString,
                document.expireDate, document.language, document.languageType, document.languageEncoding,
                document.documentFields);
    }

    /**
     * Adds a field to this document. If a field already exists with the same name, the information is appended to the 
     * list of fields. If a field doesn't exist a new list is created with this field as it's only element. 
     *
     * @param field The document field to add.
     * @throws IllegalArgumentException if the field <tt>name</tt> property is <tt>null</tt> or empty
     */
    public void addDocumentField(final DocumentField field) {
        // This is the key to use in the map...
        final String key = field.getName();
        
        // Sanity check...
        if(StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("document field name is null or blank.");
        }
                
        // Does the field already exist...?
        if(documentFields.containsKey(key)) {
            // Get the container...
            final List<DocumentField> fields = documentFields.get(key);
            
            // Stick the field into the container...
            fields.add(field);
        }
        else {
            // Create a new container...
            final List<DocumentField> fields = new LinkedList<DocumentField>();
            
            // Stick the field into the container...
            fields.add(field);
            
            // Stick the container into the map...
            documentFields.put(key, fields);
        }
    }
    
    /**
     * Gets the first element in the list of document fields for the specified field name. This method is equivalent to:
     * <pre>
     *     DocumentField field = document.getDocumentFields().get(fieldName).get(0);
     * </pre>
     *
     * @param fieldName The name of the field to return info for.
     * @return The first {@code DocumentField} in the list of <tt>DocumentField</tt>'s matching the <tT>fieldName</tt>,
     *         or <tt>null</tt> if no field exists with that name.
     */
    public DocumentField getDocumentField(final String fieldName) {
        return documentFields.containsKey(fieldName) ? documentFields.get(fieldName).get(0) : null;
    }
    
    /**
     * Gets the value of the first element in the list of document fields for the specified field name. This method is
     * equivalent to:
     * <pre>
     *     String value = document.getDocumentFields().get(fieldName).get(0).getValue();
     * </pre>
     * or
     * <pre>
     *     String value = document.getDocumentField(fieldName).getValue();
     * </pre>
     *
     * @param fieldName The name of the field to return the value of.
     * @return The value of the first <tt>DocumentField</tt> in the list of <tt>DocumentField</tt>'s matching the <tt>
     *         fieldName</tt>, or <tt>null</tt> if no field exists with that name.
     */
    public String getDocumentFieldValue(final String fieldName) {
        // Get the first document field object...
        final DocumentField field = getDocumentField(fieldName);
        
        // return either the value or null...
        return (field == null) ? null : field.getValue();
    }

    /**
     * Gets all the values for a specified field name.
     *
     * @param fieldName The name of the field to get values for.
     * @return A <tt>List<String></tt> of all values for the specified field name. The list will be empty if no matching
     *         fields were found.
     */
    public List<String> getDocumentFieldValues(final String fieldName) {
        // Get the container if there is one...
        final List<DocumentField> fields = documentFields.get(fieldName);
        
        // This will hold the values to be returned..
        final List<String> values = new ArrayList<String>((fields == null) ? 0 : fields.size());
        
        if(fields != null) {
            // For each field get the value...
            for(final DocumentField field : fields) {
                // Add the value...
                values.add(field.getValue());
            }
        }
        
        // Return the values, or an empty list...
        return values;
    }
    
    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return ReflectionToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
    
    /**
     * Checks to see if the passed in <tt>obj</tt> is equal. It does this by checking on the following four fields in
     * this order:
     * <ol>
     *   <li>reference</li>
     *   <li>database</li>
     *   <li>section</li>
     *   <li>id</li>
     * </ol>
     * 
     * @param obj The object to compare against
     * @return <tt>true</tt> if <tt>obj</tt> is equal, <tt>false</tt> otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        boolean returnValue = false;
        
        if(obj instanceof Document) {
            if(this == obj) {
                returnValue = true;
            }
            else {
                // Cast it...
                final Document rhs = (Document) obj;

                // Check the name of this parameter with the passed in parameter...
                returnValue = new EqualsBuilder()
                        .append(reference, rhs.reference)
                        .append(database, rhs.database)
                        .append(section, rhs.section)
                        .append(id, rhs.id)
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
        return new HashCodeBuilder(73, 13)
                .append(reference)
                .append(database)
                .append(section)
                .append(id)
                .hashCode();
    }
    
    /**
     * Getter for property reference.
     * @return Value of property reference.
     */
    public String getReference() {
        return this.reference;
    }

    /**
     * Setter for property reference.
     * @param reference New value of property reference.
     */
    public void setReference(final String reference) {
        this.reference = reference;
    }

    /**
     * Getter for property id.
     * @return Value of property id.
     */
    public int getId() {
        return this.id;
    }

    /**
     * Setter for property id.
     * @param id New value of property id.
     */
    public void setId(final int id) {
        this.id = id;
    }

    /**
     * Getter for property section.
     * @return Value of property section.
     */
    public int getSection() {
        return this.section;
    }

    /**
     * Setter for property section.
     * @param section New value of property section.
     */
    public void setSection(final int section) {
        this.section = section;
    }

    /**
     * Getter for property weight.
     * @return Value of property weight.
     */
    public float getWeight() {
        return this.weight;
    }

    /**
     * Setter for property weight.
     * @param weight New value of property weight.
     */
    public void setWeight(final float weight) {
        this.weight = weight;
    }

    /**
     * Getter for property links.
     * @return Value of property links.
     */
    public List<String> getLinks() {
        return new LinkedList<String>(links);
    }

    /**
     * Setter for property links.
     * @param links New value of property links.
     */
    public void setLinks(final List<String> links) {
        this.links = (links == null) ? new LinkedList<String>() : new LinkedList<String>(links);
    }

    /**
     * Setter for property links. Splits the input string on <tt>,</tt> and creates a list from the resulting tokens.
     * @param links New value of property links.
     * @throws java.lang.NullPointerException if <tt>links</tt> is <tt>null</tt>
     */
    public void setLinks(final String links) {
        // Split the links string on comma and create a list to hold them...
        this.links = Arrays.asList(StringUtils.split(links, ','));
    }
    
    /**
     * Getter for property database.
     * @return Value of property database.
     */
    public String getDatabase() {
        return this.database;
    }

    /**
     * Setter for property database.
     * @param database New value of property database.
     */
    public void setDatabase(final String database) {
        this.database = database;
    }

    /**
     * Getter for property title.
     * @return Value of property title.
     */
    public String getTitle() {
        return this.title;
    }

    /**
     * Setter for property title.
     * @param title New value of property title.
     */
    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     * Getter for property summary.
     * @return Value of property summary.
     */
    public String getSummary() {
        return this.summary;
    }

    /**
     * Setter for property summary.
     * @param summary New value of property summary.
     */
    public void setSummary(final String summary) {
        this.summary = summary;
    }

    /**
     * Getter for property baseId.
     * @return Value of property baseId.
     */
    public int getBaseId() {
        return this.baseId;
    }

    /**
     * Setter for property baseId.
     * @param baseId New value of property baseId.
     */
    public void setBaseId(final int baseId) {
        this.baseId = baseId;
    }

    /**
     * Getter for property date.
     * @return Value of property date.
     */
    public Date getDate() {
        return (this.date == null) ? null : new Date(this.date.getTime());
    }

    /**
     * Setter for property date.
     * @param date New value of property date.
     */
    public void setDate(final Date date) {
        this.date = (date == null) ? null : new Date(date.getTime());
    }

    /**
     * Getter for property dateString.
     * @return Value of property dateString.
     */
    public String getDateString() {
        return this.dateString;
    }

    /**
     * Setter for property dateString.
     * @param dateString New value of property dateString.
     */
    public void setDateString(final String dateString) {
        this.dateString = dateString;
    }

    /**
     * Getter for property expireDate.
     * @return Value of property expireDate.
     */
    public Date getExpireDate() {
        return (this.expireDate == null) ? null : new Date(this.expireDate.getTime());
    }

    /**
     * Setter for property expireDate.
     * @param expireDate New value of property expireDate.
     */
    public void setExpireDate(final Date expireDate) {
        this.expireDate = (this.expireDate == null) ? null : new Date(this.expireDate.getTime());
    }

    /**
     * Getter for property language.
     * @return Value of property language.
     */
    public String getLanguage() {
        return this.language;
    }

    /**
     * Setter for property language.
     * @param language New value of property language.
     */
    public void setLanguage(final String language) {
        this.language = language;
    }

    /**
     * Getter for property languageType.
     * @return Value of property languageType.
     */
    public String getLanguageType() {
        return this.languageType;
    }

    /**
     * Setter for property languageType.
     * @param languageType New value of property languageType.
     */
    public void setLanguageType(final String languageType) {
        this.languageType = languageType;
    }

    /**
     * Getter for property languageEncoding.
     * @return Value of property languageEncoding.
     */
    public String getLanguageEncoding() {
        return this.languageEncoding;
    }

    /**
     * Setter for property languageEncoding.
     * @param languageEncoding New value of property languageEncoding.
     */
    public void setLanguageEncoding(final String languageEncoding) {
        this.languageEncoding = languageEncoding;
    }

    /**
     * Getter for property documentFields.
     * @return Value of property documentFields.
     */
    public Map<String, List<DocumentField>> getDocumentFields() {
        return new LinkedHashMap<String, List<DocumentField>>(documentFields);
    }

    /**
     * Setter for property documentFields.
     * @param documentFields New value of property documentFields.
     */
    public void setDocumentFields(final Map<String, List<DocumentField>> documentFields) {
        this.documentFields = (documentFields == null)
                ? new LinkedHashMap<String, List<DocumentField>>()
                : new LinkedHashMap<String, List<DocumentField>>(documentFields);
    }
    
} // End of class Document...
