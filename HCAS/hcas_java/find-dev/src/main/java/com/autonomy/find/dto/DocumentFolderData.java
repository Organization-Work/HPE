package com.autonomy.find.dto;

import java.util.List;
import java.util.Set;

import com.autonomy.find.api.database.SavedFilter;
import com.autonomy.find.api.database.SavedFiltersFolder;

public class DocumentFolderData {
	public Set<SavedFiltersFolder> folders;
	public Set<SavedFilter> customFilters;
	public Set<SavedFiltersFolder> lists;
	
	public DocumentFolderData(final Set<SavedFiltersFolder> folders, final Set<SavedFiltersFolder> lists, final Set<SavedFilter> customFilters){
		this.folders = folders;
		this.lists = lists;
		this.customFilters = customFilters;
	}
}
