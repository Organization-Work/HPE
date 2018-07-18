package com.autonomy.find.api.database;

import javax.persistence.*;

import lombok.Data;

import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import java.util.Date;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.autonomy.aci.client.util.AciParameters;

import java.util.HashSet;
import java.util.Set;


@NamedQueries({
	//saveFilter queries
    @NamedQuery(name = "SavedFiltersFolder.getChildFolders",
            query = "from SavedFiltersFolder folder where folder.parent.id=:parentFolderId"),
    @NamedQuery(name = "SavedFiltersFolder.getRootFolder",
            query = "from SavedFiltersFolder folder where folder.name=:rootname and folder.owner=:owner"),


    //DocumentFolder queries
    @NamedQuery(name = SavedFiltersFolder.GET_FOLDERS,
    	query = "select f from SavedFiltersFolder f where ((f.searchView = :searchView) and (f.folderType is null) and (f.owner = :owner or (cast(:role as string)) in elements(f.roles))) order by f.name"),
    @NamedQuery(name = SavedFiltersFolder.GET_LISTS,
    		//query = "select f from SavedFiltersFolder f where ((f.searchView = :searchView) and (f.folderType= :folderTypeList_new) and (f.owner = :owner or (:role) in elements(f.roles))) order by f.name"),
    		query = "select f from SavedFiltersFolder f where ((f.searchView = :searchView) and (f.folderType in (:folderTypeList_new)) and (f.owner = :owner or (cast(:role as string) in (elements(f.roles))))) order by f.name"),
    @NamedQuery(name = SavedFiltersFolder.GET_DOCUMENTLIST_BY_LABEL,
    query = "select f from SavedFiltersFolder f where f.searchView = :searchView and f.folderType in :folderTypeList and f.parent.id = :parent_id and upper(f.name) = :name and f.owner = :owner"),
    @NamedQuery(name = SavedFiltersFolder.GET_FOLDER_BY_LABEL,
    query = "select f from SavedFiltersFolder f where f.searchView = :searchView and f.folderType is null and f.parent.id = :parent_id and upper(f.name) = :name and f.owner = :owner"),
    @NamedQuery(name = SavedFiltersFolder.GET_BY_REF_AND_VIEW,
    query = "select f from SavedFiltersFolder f where (f.searchView = :searchView or f.folderType in (:folderTypeList)) and (f.owner = :owner or (:role) in elements(f.roles)) and (:ref) in elements(f.refs)")
})


@Data
@Entity
@Table(
		name = "find.savedfolders",
		uniqueConstraints = @UniqueConstraint(columnNames={"name", "view", "owner", "parent_id"})
)
@SequenceGenerator(name = "sequence", sequenceName = "find.savedfolders_id_seq", initialValue = 1)
public class SavedFiltersFolder {
    public static final String PATH_SEPARATOR = "/";
 
    public static final String GET_FOLDERS = "SavedFiltersFolder.getFolders";
    public static final String GET_LISTS = "SavedFiltersFolder.getLists";
    public static final String GET_DOCUMENTLIST_BY_LABEL = "SavedFiltersFolder.getDocumentListByLabel";
    public static final String GET_FOLDER_BY_LABEL = "SavedFiltersFolder.getFolderByLabel";
    public static final String GET_BY_REF_AND_VIEW = "SavedFiltersFolder.getByRefAndView";
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "sequence")
	@Column(name = "id")
	private Integer id = null;

	@Column(name = "name", nullable = false)
	private String name;

    @Column(name = "owner", nullable = false)
   	private String owner;
    
    @Column(name = "docCount")
    private int docCount = 0;

    @Column(name = "folderType")
    private String folderType;
    
    @Column(name = "is_primary")
    private boolean isPrimaryFolder = Boolean.FALSE;
    
    @Column(name = "restricted", nullable = false)
    private boolean restricted  = Boolean.FALSE;
    
    @Column(name = "view")
    private String searchView;
    
    @Column(name = "tooltip")
    private String tooltip;
    
    @Column(name = "read_only")
    private Boolean readOnly = Boolean.FALSE;

    @Column(name = "full_path")
   	private String fullPath = "";

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_id")
    private SavedFiltersFolder parent;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_created", nullable = false, columnDefinition = "TIMESTAMP default CURRENT_TIMESTAMP")
    private Date createDate = new Date();
    
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "date_modified", nullable = false, columnDefinition = "TIMESTAMP default CURRENT_TIMESTAMP")
    private Date modifiedDate = new Date();
    
    @JsonIgnore
    @ElementCollection
    @CollectionTable(name="find.documentfolder_references", joinColumns = @JoinColumn(name = "documentfolder_id"))
    @Column
    private Set<String> refs = new HashSet<String>();
    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name="find.savedFoldersroles", joinColumns = @JoinColumn(name = "savedfolder_id"))
    @Column
    private Set<String> roles = new HashSet<String>();
    
    // flag used to determine if the checkbox for this document folder is selected
    // transient since it's only stored on the session, we don't want to keep this in the database
    @Transient
    private boolean selected;
    
    @Transient
    private boolean primary;
    
    @Transient
    private Integer parent_id;
    
    public SavedFiltersFolder() {

    }

    public SavedFiltersFolder(final String name, final SavedFiltersFolder parent, final String owner) {
        this.name = name;
        this.parent = parent;
        this.owner = owner;
        final String path = (parent == null) ? name : (parent.getFullPath() + PATH_SEPARATOR +  name);
        setFullPath(path);
    }
    
    public SavedFiltersFolder(final String name, final SavedFiltersFolder parent, final String searchView, final String owner){
    	this(name, parent, owner);
    	this.isPrimaryFolder = true;
    	this.searchView = searchView;
    }
    public SavedFiltersFolder(final String name, final SavedFiltersFolder parent, final String owner, final String tooltip, final String searchView, 
    		final boolean restricted,  final String folderType, final Set<String> roles, final boolean readOnly){
    		this.name = name;
    		this.parent = parent;
    		this.owner = owner;
    		this.tooltip = tooltip;
    		this.searchView = searchView;
    		this.restricted = restricted;
    		this.folderType = folderType;
    		this.roles = roles;
    		this.readOnly = readOnly;
    		final String path = (parent == null) ? name : (parent.getFullPath() + PATH_SEPARATOR +  name);
    		setFullPath(path);
    }

    public void setFullPath(String path) {
    	this.fullPath = path;
	}

	public String getFullPath() {
		return fullPath;
	}

	public void setName(final String name) {
        this.name = name;
        final String path = (this.parent == null) ? name : (this.parent.getFullPath() + PATH_SEPARATOR +  name);
        setFullPath(path);
    }

	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}

	public SavedFiltersFolder getParent() {
		return parent;
	}

	public String getName() {
		return name;
	}

	public Boolean getReadOnly() {
		return readOnly;
	}

	public void setModifiedDate(Date date) {
		this.modifiedDate=date;
	}

	public String getOwner() {
		return owner;
	}
	
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public String getSearchView(){
		return searchView;
	}
	
	public void setSearchView(String searchView) {
		this.searchView = searchView;
	}
	
	public int getDocCount(){
		return docCount;
	}
	
	public void setDocCount(int docCount) {
		this.docCount = docCount;
	}
	
	public boolean isPrimaryFolder() {
		return isPrimaryFolder;
	}
	
	public void setPrimaryFolder(boolean isPrimaryFolder) {
		this.isPrimaryFolder = isPrimaryFolder;
	}
	
	public boolean isRestricted() {
		return restricted;
	}

	public void setRestricted(boolean restricted) {
		this.restricted = restricted;
	}
	
	public String getFolderType() {
		return folderType;
	}

	public void setFolderType(String folderType) {
		this.folderType = folderType;
	}
	
	public String getTooltip() {
		return tooltip;
	}

	public void setTooltip(String tooltip) {
		this.tooltip = tooltip;
	}
	
	public Set<String> getRefs(){
		return refs;
	}
	
	public void setRefs(Set<String> refs){
		this.refs=refs;
	}
	
	public Set<String> getRoles(){
		return roles;
	}
	
	public void setRoles(Set<String> roles){
		this.roles = roles;
	}
}