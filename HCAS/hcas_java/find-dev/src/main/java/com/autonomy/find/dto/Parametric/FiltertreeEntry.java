package com.autonomy.find.dto.Parametric;

import java.util.Set;

import com.autonomy.find.api.database.SavedFilter;
import com.autonomy.find.api.database.SavedFiltersFolder;

import lombok.Data;

@Data
public class FiltertreeEntry {
    private String name;
    private Integer id;
    private FiltertreeEntryType type;
    private Integer parentFolderId;
    private String description;
    private String data;
    private String fullPath;
    private String searchView;
    private Set<String> Roles;
    private Boolean readOnly;

    public FiltertreeEntry() {

    }

    public FiltertreeEntry(final Integer id, final String name, final FiltertreeEntryType type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }
    
    public FiltertreeEntry(final Integer id, final String name, final FiltertreeEntryType type, final String searchView){
    	this(id, name, type);
    	this.searchView = searchView;
    }
    public FiltertreeEntry(final Integer id,
                           final String name,
                           final FiltertreeEntryType type,
                           final Integer parentFolderId ) {
        this(id, name, type);
        this.parentFolderId = parentFolderId;
    }

    public FiltertreeEntry(final Integer id,
                           final String name,
                           final FiltertreeEntryType type,
                           final Integer parentFolderId,
                           final String description) {
        this(id, name, type, parentFolderId);
        this.description = description;
    }

    public FiltertreeEntry(final Integer id,
                           final String name,
                           final FiltertreeEntryType type,
                           final Integer parentFolderId,
                           final String description,
                           final String fullPath,
                           final String searchView) {
        this(id, name, type, parentFolderId, description);
        this.fullPath = fullPath;
        this.searchView = searchView;
    }

    static public FiltertreeEntry toFiltertreeEntry(final SavedFiltersFolder folder) {
        final Integer parentId = folder.getParent() != null ?  folder.getParent().getId() : null;
        final FiltertreeEntry entry = new FiltertreeEntry(folder.getId(), folder.getName(), FiltertreeEntryType.FOLDER, parentId);
        entry.setRoles(folder.getRoles());
        entry.setReadOnly(folder.getReadOnly());

        return entry;
    }

    private void setReadOnly(Object readOnly2) {
		// TODO Auto-generated method stub
		
	}
	
	private void setRoles(Set<String> roles){
		// TODO Auto-generated method stub
	}

	static public FiltertreeEntry toFiltertreeEntry(final SavedFilter item) {
        final Integer parentId = item.getParent() != null ?  item.getParent().getId() : null;
        final FiltertreeEntry entry = new FiltertreeEntry(item.getId(), item.getName(), FiltertreeEntryType.ITEM, parentId, item.getDescription(), item.getFullPath(), item.getSearchView());
        entry.setRoles(item.getRoles());

        return entry;
    }

	public String getName() {
		return name;
	}

	public Integer getParentFolderId() {
		return parentFolderId;
	}

	public String getDescription() {
		return description;
	}

	public String getData() {
		return data;
	}

	public String getSearchView() {
		return searchView;
	}

	public Integer getId() {
		return id;
	}
}
