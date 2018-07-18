package com.autonomy.find.api.database;

/*
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/api/database/DocumentFolder.java#11 $
 *
 * Copyright (c) 2013, Autonomy Systems Ltd.
 *
 * Last modified by $Author: wo $ on $Date: 2014/02/03 $ 
 */


import lombok.Data;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.autonomy.aci.client.util.AciParameters;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import java.util.HashSet;
import java.util.Set;

@NamedQueries({
    @NamedQuery(name = DocumentFolder.GET_FOLDERS_Old,
            query = "select f from DocumentFolder f where (f.searchView = :searchView or f.folderType in (:folderTypeList)) and (f.owner = :owner or f.isShared = true) order by f.label"),
    @NamedQuery(name = DocumentFolder.GET_BY_LABEL_Old,
            query = "select f from DocumentFolder f where (f.searchView = :searchView or f.folderType in (:folderTypeList)) and f.label = :label and (f.owner = :owner or f.isShared = true)"),
   /* @NamedQuery(name = DocumentFolder.GET_BY_REF_AND_VIEW_Old,
            query = "select f from DocumentFolder f where (f.searchView = :searchView or f.folderType in (:folderTypeList)) and (f.owner = :owner or f.isShared = true) and (:ref) in elements(f.refs)")*/
})

@Data
@Entity
@Table(
    name = "find.documentfolder",
    uniqueConstraints = @UniqueConstraint(columnNames={"label", "view", "owner"})
)
@SequenceGenerator(name = "sequence", sequenceName = "find.documentfolder_id_seq")
public class DocumentFolder {
    public static final String GET_FOLDERS_Old = "DocumentFolder.getFoldersOld";
    public static final String GET_BY_LABEL_Old = "DocumentFolder.getByLabelOld";
    /*public static final String GET_BY_REF_AND_VIEW_Old = "DocumentFolder.getByRefAndViewOld";*/

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "sequence")
    @Column(name = "id")
    private Integer id = null;

    @Column(name = "view")
    private String searchView;

    @Column(name = "tooltip")
    private String tooltip;

    @Column(name = "label")
    private String label;

    @Column(name = "owner")
    private String owner;

    @Column(name = "docCount")
    private int docCount;

    @Column(name = "is_shared")
    private Boolean isShared = Boolean.FALSE;

   // private Boolean isSelected=Boolean.FALSE;
    
    @Column(name = "folderType")
    private String folderType;
    
    // flag which can be set to prevent the client tagging the document
    @Column(name = "restricted", nullable = false)
    private boolean restricted;

    /*@JsonIgnore
    @ElementCollection
    @CollectionTable(name="find.documentfolder_references", joinColumns = @JoinColumn(name = "documentfolder_id"))
    @Column
    private Set<String> refs = new HashSet<String>();*/

    // flag used to determine if the checkbox for this document folder is selected
    // transient since it's only stored on the session, we don't want to keep this in the database
    @Transient
    private boolean selected;
    
    // flag used to determine if this folder is primary to the view so specifically the checkbox for this document folder needs to be displayed
    @Column(name = "is_primary")
    private boolean isPrimaryFolder;
    
    @Transient
    private boolean primary;

    public DocumentFolder () {}

    public DocumentFolder(final String tooltip, final String searchView, final String label, final boolean restricted, final String owner, final boolean isShared,
    		final String folderType) {
        this.tooltip = tooltip;
        this.searchView = searchView;
        this.label = label;
        this.restricted = restricted;
        this.owner = owner;
        this.isShared = isShared;
        this.folderType = folderType;
    }

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getSearchView() {
		return searchView;
	}

	public void setSearchView(String searchView) {
		this.searchView = searchView;
	}

	public String getTooltip() {
		return tooltip;
	}

	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public int getDocCount() {
		return docCount;
	}

	public void setDocCount(int docCount) {
		this.docCount = docCount;
	}

	public Boolean getIsShared() {
		return isShared;
	}

	public void setIsShared(Boolean isShared) {
		this.isShared = isShared;
	}

	public String getFolderType() {
		return folderType;
	}

	public void setFolderType(String folderType) {
		this.folderType = folderType;
	}

	public boolean isRestricted() {
		return restricted;
	}

	public void setRestricted(boolean restricted) {
		this.restricted = restricted;
	}

	/*public Set<String> getRefs() {
		return refs;
	}*/

	/*public void setRefs(Set<String> refs) {
		this.refs = refs;
	}
	*/
	
	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public boolean isPrimaryFolder() {
		return isPrimaryFolder;
	}

	public void setPrimaryFolder(boolean isPrimaryFolder) {
		this.isPrimaryFolder = isPrimaryFolder;
	}
	
	public boolean isPrimary() {
		return primary;
	}

	public void setPrimary(boolean primary) {
		this.primary = primary;
	}
}
