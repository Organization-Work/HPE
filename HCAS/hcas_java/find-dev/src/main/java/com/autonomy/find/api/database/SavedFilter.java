package com.autonomy.find.api.database;

import java.io.Serializable;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.*;

import lombok.Data;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.hibernate.annotations.Type;
@NamedQueries({
    @NamedQuery(name = "SavedFilter.getViewFolderFilters",
            query = "from SavedFilter filter where filter.searchView=:searchView and filter.parent.id=:parentFolderId and (filter.owner = :owner or (cast(:role as string)) in elements(filter.roles))"),
    @NamedQuery(name = "SavedFilter.getCustomFilters",
    		query = "from SavedFilter filter where filter.searchView=:searchView and (filter.owner = :owner or (cast(:role as string)) in elements(filter.roles))"),
    @NamedQuery(name = "SavedFilter.getFilterByName",
    		query = "from SavedFilter filter where filter.searchView=:searchView and filter.parent.id=:parent_id and filter.name=:name and filter.owner=:owner")
})


@SuppressWarnings("serial")
@Data
@Entity
@Table(name = "find.savedfilters")
@SequenceGenerator(name = "sequence", sequenceName = "find.savedfilter_id_seq", initialValue = 1)
public class SavedFilter implements Serializable {
	
    @Id
    @Column(name = "id")
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "sequence")
    private Integer id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id")
    private SavedFiltersFolder parent = null;
    
    @Column(name = "filters_text", nullable =  false)
    @Type(type="text")
    private String filtersText;
    
    @Column(name = "name", nullable =  false)
    private String name;

    @Column(name = "owner", nullable = false)
   	private String owner;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "read_only")
    private Boolean readOnly = Boolean.FALSE;

    @Column(name = "search_view", nullable = false)
    private String searchView;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_created", nullable = false, columnDefinition = "TIMESTAMP default CURRENT_TIMESTAMP")
    private Date createDate = new Date();
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_modified", nullable = false, columnDefinition = "TIMESTAMP default CURRENT_TIMESTAMP")
    private Date modifiedDate = new Date();

    @Transient
    private String fullPath;
    
    @ElementCollection(fetch=FetchType.EAGER)
    @CollectionTable(name="find.savedfiltersroles", joinColumns= @JoinColumn(name = "savedfilters_id"))
    @Column
    private Set<String> roles = new HashSet<String>();


    public SavedFilter() {
        
    }

    public SavedFilter(final String name,
                       final String description,
                       final String filtersText,
                       final SavedFiltersFolder parent,
                       final String searchView,
                       final String owner) {
        this.name = name;
        this.description = description;
        this.filtersText = filtersText;
        this.parent = parent;
        this.searchView = searchView;
        this.owner = owner;
    }

	public SavedFiltersFolder getParent() {
		return parent;
	}

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getFullPath() {
		return fullPath;
	}

	public String getSearchView() {
		return searchView;
	}

	public void setFullPath(String fullPath2) {
		this.fullPath=fullPath2;
	}

	public void setDescription(String description2) {
		this.description=description2;
	}

	public void setFiltersText(String data) {
		this.filtersText=data;
	}

	public void setModifiedDate(Date date) {
		this.modifiedDate=date;
	}

	public void setName(String newName) {
		this.name=newName;
	}

	public String getFiltersText() {
		return filtersText;
	}

	public String getOwner() {
		return owner;
	}
	
	public Set<String> getRoles(){
		return roles;
	}
	
	public void setRoles(Set<String> roles){
		this.roles = roles;
	}
	
	public Boolean getReadOnly() {
		return readOnly;
	}
}
