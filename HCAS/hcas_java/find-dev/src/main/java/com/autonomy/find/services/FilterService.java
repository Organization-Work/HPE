package com.autonomy.find.services;

import com.autonomy.find.api.database.SavedFilter;
import com.autonomy.find.api.database.SavedFiltersFolder;
import com.autonomy.find.config.SearchConfig;
import com.autonomy.find.config.SearchView;
import com.autonomy.find.dto.Parametric.DocImportType;
import com.autonomy.find.dto.Parametric.FiltertreeEntry;
import com.autonomy.find.services.admin.AdminService;
import com.autonomy.find.util.CollUtils;
import com.autonomy.find.util.FieldUtil;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class FilterService {
    @Autowired
    @Qualifier("findSessionFactory")
    private SessionFactory findSessionFactory;
   
    @Autowired
    private SearchConfig searchConfig;
    
    @Autowired
    private ParametricService parametricService;
    
    @Autowired
    private AdminService adminService;
    
    @Autowired
    private DocumentFolderService documentFolderService;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(FilterService.class);
    private static final String ROOT_FOLDER_NAME = "root";
    private static final String SHARED_FOLDER_NAME = "shared";

    @Transactional(readOnly = false)
    public SavedFiltersFolder getRootFolder() {
        final Session session = findSessionFactory.getCurrentSession();
        final Query query = session.getNamedQuery("SavedFiltersFolder.getRootFolder");
        query.setString("owner", "system");
        query.setString("rootname", ROOT_FOLDER_NAME);

        SavedFiltersFolder rootFolder = (SavedFiltersFolder) query.uniqueResult();

        if (rootFolder == null) {
            // create one
            rootFolder = new SavedFiltersFolder(ROOT_FOLDER_NAME, null, "system");
            session.save(rootFolder);
        }

        return rootFolder;

    }
    
    @Transactional(readOnly = true)
    public SavedFiltersFolder checkRootFolder(){
    	final Session session = findSessionFactory.getCurrentSession();
    	final Query query = session.getNamedQuery("SavedFiltersFolder.getRootFolder");
    	query.setString("owner", "system");
    	query.setString("rootname", ROOT_FOLDER_NAME);
    	
    	SavedFiltersFolder rootFolder = (SavedFiltersFolder) query.uniqueResult();
    	
    	return rootFolder;
    }

    @Transactional(readOnly = false)
    public SavedFiltersFolder createFolder(final String folderName, final Integer parentId, final String searchView, final String loginUser)   {
        final Session session = findSessionFactory.getCurrentSession();
        final SavedFiltersFolder parentFolder = (SavedFiltersFolder) session.get(SavedFiltersFolder.class, parentId);
        documentFolderService.checkPermissionFolder(parentFolder, loginUser, "add");
        final SavedFiltersFolder folder = new SavedFiltersFolder(folderName, parentFolder, searchView, loginUser);
        documentFolderService.checkDuplicate(null, folder.getSearchView(), folder.getParent().getId(), folder.getName(), loginUser, session);
        session.save(folder);
        return folder;
    }

    @Transactional(readOnly = false)
    public void deleteFolder(final Integer folderId, final String loginUser)   {
        final Session session = findSessionFactory.getCurrentSession();
        SavedFiltersFolder folder = documentFolderService.getFolder(folderId, session, loginUser);
        documentFolderService.checkPermissionFolder(folder, loginUser, "delete");
        session.delete(folder);
    }
    
    public SavedFilter getFilter(final int id, final Session session, final String loginUser){
    	final SavedFilter filter = (SavedFilter) session.get(SavedFilter.class, id);
    	/*if(!filter.getOwner().equals(loginUser)){
    		throw new IllegalArgumentException("Current user is not the owner of this filter.");
    	}*/
    	return filter;
    }

    /*@Transactional(readOnly = true)
    public List<FiltertreeEntry> getFolderContent(final Integer folderId, final String searchView, final String loginUser)   {
        List<FiltertreeEntry> content = new ArrayList<FiltertreeEntry>();

        final Session session = findSessionFactory.getCurrentSession();
        Set<String> userRoles = adminService.getUsersRoles(loginUser);


        final SavedFiltersFolder parentFolder = (SavedFiltersFolder) session.get(SavedFiltersFolder.class, folderId);

        if (!isValidFolder(parentFolder, loginUser)) {
            throw new IllegalArgumentException("Folder with id [" + folderId + "] not found for current.");
        }
        // get all child folders
        final Query foldersQuery = session.getNamedQuery("SavedFiltersFolder.getChildFolders");
        foldersQuery.setInteger("parentFolderId", folderId);
        final List childFolders = foldersQuery.list();

        for(int i = 0; i < childFolders.size(); i++) {
            final SavedFiltersFolder folder = (SavedFiltersFolder) childFolders.get(i);
            if(folder.getFolderType()==null){
            	content.add(FiltertreeEntry.toFiltertreeEntry(folder));
            }
        }
        
        //get all the filter items
        final Set<SavedFilter> childItems=new HashSet<SavedFilter>();
        for(String role : userRoles){
        	final Query itemsQuery = session.getNamedQuery("SavedFilter.getViewFolderFilters");
        	itemsQuery.setString("searchView", searchView);
        	itemsQuery.setInteger("parentFolderId", folderId);
        	itemsQuery.setParameter("owner", loginUser);
        	itemsQuery.setParameter("role", role);
        	childItems.addAll(itemsQuery.list());
        }
        
        for(SavedFilter item : childItems){
        	final String fullPath = parentFolder.getFullPath() + SavedFiltersFolder.PATH_SEPARATOR +  item.getName();
        	item.setFullPath(fullPath);
        	content.add(FiltertreeEntry.toFiltertreeEntry(item));
        }
        
        return content;
    }*/
    
    @Transactional(readOnly = true)
    private boolean checkSharedFolder(SavedFiltersFolder folder, String loginUser){
    	Set<String> userRoles = adminService.getUsersRoles(loginUser);
    	for(String role : userRoles){
    		if(folder.getRoles().contains(role))
    			return true;
    	}
    	return false;
    }
    
    @Transactional(readOnly = true)
    private boolean checkSharedFilter(SavedFilter filter, String loginUser){
    	Set<String> userRoles = adminService.getUsersRoles(loginUser);
    	for(String role : userRoles){
    		if(filter.getRoles().contains(role))
    			return true;
    	}
    	return false;
    }
    
    @Transactional(readOnly = true)
    public List<FiltertreeEntry> getFolderContent(final Integer folderId, final String searchView, final String loginUser)   {
    	List<FiltertreeEntry> content = new ArrayList<FiltertreeEntry>();
        final Session session = findSessionFactory.getCurrentSession();
        Set<String> userRoles = adminService.getUsersRoles(loginUser);
        
        final SavedFiltersFolder parentFolder = (SavedFiltersFolder) session.get(SavedFiltersFolder.class, folderId);
        // get all child folders either owned by me or shared with me
        final Query foldersQuery = session.getNamedQuery("SavedFiltersFolder.getChildFolders");
        foldersQuery.setInteger("parentFolderId", folderId);
        final List childFolders = foldersQuery.list();
        
        for(int i = 0; i < childFolders.size(); i++) {
            final SavedFiltersFolder folder = (SavedFiltersFolder) childFolders.get(i);
            if(folder.getFolderType()==null && (folder.getOwner().equals(loginUser) || checkSharedFolder(folder, loginUser))){
            	content.add(FiltertreeEntry.toFiltertreeEntry(folder));
            }
        }
        
        // get all restricted folders
        Set<SavedFilter> filters = documentFolderService.getCustomFilters(searchView, loginUser);
        Set<SavedFiltersFolder> filtersParents = new HashSet<SavedFiltersFolder>();
        for(SavedFilter filter : filters)
        	filtersParents.add(filter.getParent());
        
        for(SavedFiltersFolder filtersParentFolder : filtersParents){
        	SavedFiltersFolder restrictedFolder = getRestrictedFolder(filtersParentFolder, parentFolder);
        	FiltertreeEntry restrictedFiltertreeEntry = FiltertreeEntry.toFiltertreeEntry(restrictedFolder);
        	if(!content.contains(restrictedFiltertreeEntry) && !restrictedFiltertreeEntry.getName().equals("root")){
        		content.add(restrictedFiltertreeEntry);
        	}
        }
        
        /*for(SavedFilter filter : documentFolderService.getCustomFilterData(searchView, loginUser)){
        	if(checkSharedFilter(filter, loginUser) && !filter.getParent().getOwner().equals(loginUser) 
        			&& !checkSharedFolder(filter.getParent(), loginUser) && !filter.getParent().equals(parentFolder)) // ignore all owned by me
        	{
        		SavedFiltersFolder restrictedFolder = getRestrictedFolder(filter.getParent(), parentFolder);
        		if(!restrictedFolder.getName().equals("root"))
        			content.add(FiltertreeEntry.toFiltertreeEntry(restrictedFolder));
        	}
        		
        }*/
      
        
        //get all the filter items
        final Set<SavedFilter> childItems=new HashSet<SavedFilter>();
        for(String role : userRoles){
        	final Query itemsQuery = session.getNamedQuery("SavedFilter.getViewFolderFilters");
        	itemsQuery.setString("searchView", searchView);
        	itemsQuery.setInteger("parentFolderId", folderId);
        	itemsQuery.setParameter("owner", loginUser);
        	itemsQuery.setParameter("role", role);
        	childItems.addAll(itemsQuery.list());
        }
        
        for(SavedFilter item : childItems){
        	final String fullPath = parentFolder.getFullPath() + SavedFiltersFolder.PATH_SEPARATOR +  item.getName();
        	item.setFullPath(fullPath);
        	content.add(FiltertreeEntry.toFiltertreeEntry(item));
        }
        
        return content;
    }
    
    private SavedFiltersFolder getRestrictedFolder(final SavedFiltersFolder filterParent, final SavedFiltersFolder parentFolder){
    	if(filterParent.getName().equals("root"))
    		return filterParent;
    	else if(filterParent.getParent().equals(parentFolder))
    		return filterParent;
    	
    	return getRestrictedFolder(filterParent.getParent(), parentFolder);
    }
    

    @Transactional(readOnly = false)
    public SavedFilter updateFilter(final Integer filterId, final String description, final String data, final String loginUser)   {

        final Session session = findSessionFactory.getCurrentSession();

        final SavedFilter existFilter = (SavedFilter) session.get(SavedFilter.class, filterId);

        if(getFilterByLabel(existFilter.getSearchView(), existFilter.getParent().getId(), existFilter.getName(), loginUser, session)!=null){
    		throw new IllegalArgumentException("Filter name already in use");
    	}

        existFilter.setDescription(description);
        existFilter.setFiltersText(data);
        existFilter.setModifiedDate(new Date());

        session.update(existFilter);

        return existFilter;

    }

    @Transactional(readOnly = false)
    public SavedFiltersFolder renameFolder(final Integer folderId, final String newName, final String loginUser) {
        final Session session = findSessionFactory.getCurrentSession();

        final SavedFiltersFolder existFolder = (SavedFiltersFolder) session.get(SavedFiltersFolder.class, folderId);

        documentFolderService.checkPermissionFolder(existFolder, loginUser, "rename");
        documentFolderService.checkDuplicate(existFolder.getFolderType(), existFolder.getSearchView(), existFolder.getParent().getId(), newName, loginUser, session);

        //TODO: double check to make sure name is not duplicated within the same parent;
        existFolder.setName(newName);
        existFolder.setModifiedDate(new Date());

        session.update(existFolder);

        return existFolder;

    }

    @Transactional(readOnly = false)
    public SavedFilter renameFilter(final Integer filterId, final String newName, final String loginUser)   {

        final Session session = findSessionFactory.getCurrentSession();

        final SavedFilter existFilter = (SavedFilter) session.get(SavedFilter.class, filterId);

        documentFolderService.checkPermissionFilter(existFilter, loginUser);
        if(getFilterByLabel(existFilter.getSearchView(), existFilter.getParent().getId(), newName, loginUser, session)!=null){
    		throw new IllegalArgumentException("Filter name already in use");
    	}

        existFilter.setName(newName);

        final String fullPath = existFilter.getParent().getFullPath() + SavedFiltersFolder.PATH_SEPARATOR +  existFilter.getName();
        existFilter.setFullPath(fullPath);
        existFilter.setModifiedDate(new Date());

        session.update(existFilter);

        return existFilter;

    }


    @Transactional(readOnly = false)
    public SavedFilter saveFilters(final String saveName,
                                   final String description,
                                   final String data,
                                   final Integer parentId,
                                   final String searchView,
                                   final String loginUser)   {

        final Session session = findSessionFactory.getCurrentSession();

        final SavedFiltersFolder parentFolder = (SavedFiltersFolder) session.get(SavedFiltersFolder.class, parentId);

       /* if (!isValidFolder(parentFolder, loginUser)) {
            throw new IllegalArgumentException("Parent with id [" + parentId + "] not found for current user.");
        }*/
        documentFolderService.checkPermissionFolder(parentFolder, loginUser, "add");
        final SavedFilter filter = new SavedFilter(saveName, description, data, parentFolder, searchView, loginUser);
        if(getFilterByLabel(filter.getSearchView(), filter.getParent().getId(), filter.getName(), loginUser, session)!=null){
    		throw new IllegalArgumentException("Filter name already in use");
    	}
        session.save(filter);

        return filter;

    }

    @Transactional(readOnly = false)
    public void deleteFilter(final Integer filterId, String loginUser)   {
        final Session session = findSessionFactory.getCurrentSession();
        SavedFilter filter = getFilter(Integer.valueOf(filterId), session, loginUser);
        documentFolderService.checkPermissionFilter(filter, loginUser);
        session.delete(filter);
    }


    @Transactional(readOnly = true)
    public String getFilterData(final Integer filterId, final String loginUser)   {

        final Session session = findSessionFactory.getCurrentSession();

        final SavedFilter existFilter = (SavedFilter) session.get(SavedFilter.class, filterId);

        if (!isValidFilter(existFilter, loginUser)) {
            throw new IllegalArgumentException("Filter with id [" + filterId + "] not found for current user.");
        }

        return existFilter.getFiltersText();

    }
    
    @Transactional(readOnly = true)
    public FiltertreeEntry getFiletrItem(final Integer itemId, final String searchView, final String loginUser){
    	final Session session = findSessionFactory.getCurrentSession();
    	final SavedFilter Item = (SavedFilter) session.get(SavedFilter.class, itemId);
    	/*if (!isValidFilter(Item, loginUser)) {
            throw new IllegalArgumentException("Filter with id [" + itemId + "] not found for current user.");
        }*/
    	
    	final SavedFiltersFolder parentFolder =  (SavedFiltersFolder) session.get(SavedFiltersFolder.class, Item.getParent().getId());
    	/*if (!parentFolder.getOwner().equals(loginUser) && !parentFolder.getRoles().isEmpty()) {
            throw new IllegalArgumentException("Current user is not the owner of this folder.");
        }*/
    	
    	final String fullPath = parentFolder.getFullPath() + SavedFiltersFolder.PATH_SEPARATOR +  Item.getName();
    	Item.setFullPath(fullPath);
 
    	
    	return FiltertreeEntry.toFiltertreeEntry(Item);
    }
    
    public boolean isValidFilter(final SavedFilter filter, final String owner) {
        return filter != null && (filter.getOwner().equals(owner) || !filter.getRoles().isEmpty());
    }

    private boolean isValidFolder(final SavedFiltersFolder folder, final String owner) {
        return folder != null && (folder.getOwner().equals(owner) || !folder.getRoles().isEmpty());
    }
    
    public Map<String, DocImportType> getDocImportTypes(final String searchView) {
        final SearchView view = searchConfig.getSearchViews().get(searchView);
        final String importTypeFile = view.getDocImportFile();

        if (importTypeFile == null) {
            return Collections.<String, DocImportType>emptyMap();
        }

        try {
            return CollUtils.<String, DocImportType>jsonToMap(FieldUtil.loadFieldValuesJSON(importTypeFile), String.class, DocImportType.class);
        } catch (final IOException e) {
            throw new Error(String.format("Error reading file: %s", importTypeFile), e);
        }
    }
    
    public SavedFilter getFilterByLabel(final String searchView, final Integer parentId, final String name, final String loginUser, 
    		final Session session){
    	final Query query = session.getNamedQuery("SavedFilter.getFilterByName");
    	query.setParameter("searchView", searchView);
    	query.setParameter("parent_id", parentId);
    	query.setParameter("name", name);
    	query.setParameter("owner", loginUser);
    	
    	return (SavedFilter) query.uniqueResult(); 
    }
}
