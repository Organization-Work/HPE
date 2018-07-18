package com.autonomy.find.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autonomy.find.api.database.DocumentFolder;
import com.autonomy.find.api.database.SavedFilter;
import com.autonomy.find.api.database.SavedFiltersFolder;
import com.autonomy.find.config.SearchConfig;
import com.autonomy.find.config.SearchView;
import com.autonomy.find.dto.DocumentFolderData;
import com.autonomy.find.dto.Parametric.DocExportData;
import com.autonomy.find.dto.Parametric.DocImportType;
import com.autonomy.find.services.admin.AdminService;
import com.autonomy.find.util.CollUtils;
import com.autonomy.find.util.FieldUtil;
import com.autonomy.find.util.audit.AuditActions;
import com.autonomy.find.util.audit.AuditLogger;
import com.autonomy.vertica.common.JoinType;
import com.autonomy.vertica.common.SelectBuilder;
import com.autonomy.vertica.templates.FilterTemplate;
import com.google.gson.Gson;

@Service
public class DocumentVerticaFolderService {
	
	@Autowired
    private SearchConfig searchConfig;

    @Autowired
    private ParametricService parametricService;

    @Autowired
    private SearchService searchService;
    
    @Autowired
    private AdminService adminService;
    
    @Autowired
    private FilterTemplate filterTemplate;
    
    @Autowired
    private DocumentFolderService documentFolderService;

    @Value("${discoverSearch.docfolder.maxResults}")
    private int docfolderMaxResults;

    @Value("${discoverSearch.import.delimiter}")
    private String importDelimiter;

    @Value("${discoverSearch.export.batchsize}")
    private Integer exportBatchSize;

    @Value("${discoverSearch.import.batchsize}")
    private Integer importBatchSize;   
    
    @Autowired
    public DataSource vdataSource;
    
    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentVerticaFolderService.class);
    private static final String VERTICATABLE = "savedfolders";
    private static final String FOLDERSROLESTABLE = "savedFoldersRoles";
    private static final String VERTICATABLE_REF = "documentfolder_references";
    private static final String CUSTOM_FILTERS_TABLE = "savedfilters";
    private static final String CUSTOM_FILTER_ROLES_TABLE = "savedfiltersroles";
    private static final String ROOT_FOLDER_NAME = "root";
    private static final String ROOT_FOLDER_OWNER = "system";
    
	@Transactional(readOnly = true)
	public List<SavedFiltersFolder> getFolders_old(final String searchView, final String loginUser) {
    	Set<String> listTypes = documentFolderService.getDocImportTypes(searchView).keySet();
    	com.autonomy.vertica.common.SelectBuilder builder = new SelectBuilder(VERTICATABLE, searchConfig.getSearchViews().get(searchView).getDocFolderSourceDBSchema());
		String namedParameter = "searchViewParam";
		String foldertype = "foldertype";
		//String folderLabel = "label";
		String owner = "loginUser";
		String isShared = "isShared";
		String whereClauseForFolder = "(view = (:" + namedParameter +")" +" "+ "or" +" "+"foldertype in (:" + foldertype + "))" +" "+ "and " + "(owner =(:" +owner+")"+" or "+ "is_Shared = (:"+isShared+")) order by label";
		builder.where(whereClauseForFolder);
		LOGGER.debug("Query getFolder" + builder.toString());
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put(namedParameter, searchView);
		paramMap.put(foldertype, listTypes);
		//paramMap.put(folderLabel, label);
		paramMap.put(owner, loginUser);
		paramMap.put(isShared, "true");
		List<SavedFiltersFolder> documentFolders = filterTemplate.getFolders(builder.toString(), paramMap);
		return documentFolders;
	}
	
	@Transactional(readOnly = true)
	public SavedFiltersFolder getRootFolder(final String searchView){
		SelectBuilder selectBuilder = new SelectBuilder(VERTICATABLE, searchConfig.getSearchViews().get(searchView).getDocFolderSourceDBSchema());
		String rootName = "name";
		String rootOwner = "owner";
		String WhereClauseForRoot = "(name = (:" + rootName + ")" + " and " + "owner = (:" + rootOwner + "))";
		selectBuilder.where(WhereClauseForRoot);
		LOGGER.debug("Query getRootFolder" + selectBuilder.toString());
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put(rootName, ROOT_FOLDER_NAME);
		paramMap.put(rootOwner, ROOT_FOLDER_OWNER);
		SavedFiltersFolder rootFolder = filterTemplate.getFolder(selectBuilder.toString(), paramMap);
		return rootFolder;
	}
	
	@Transactional(readOnly = true)
	public Set<SavedFiltersFolder> getFolders(final String searchView, final String loginUser){
		Set<String> loginUserRoles = adminService.getUsersRoles(loginUser);
		SelectBuilder selectBuilder = new SelectBuilder(VERTICATABLE, searchConfig.getSearchViews().get(searchView).getDocFolderSourceDBSchema());
		String view = "searchViewParam";
		String owner = "loginUser";
		String folderRoles = "folderRoles";
		selectBuilder.join(JoinType.LeftJoin, VERTICATABLE, FOLDERSROLESTABLE, "id", "savedfolder_id", null, null);
		String whereClauseForFolder = "(view = (:" + view + ")" + " " + "and" + " " + "foldertype is null" + " " + "and" + " " + "(owner =(:" + owner + ")" + " or " + "roles in (:" +  folderRoles + ")))";
		selectBuilder.where(whereClauseForFolder);
		LOGGER.debug("Query getFolder" + selectBuilder.toString());
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put(view, searchView);
		paramMap.put(owner, loginUser);
		paramMap.put(folderRoles, loginUserRoles);
		
		Set<SavedFiltersFolder> folders = new HashSet<SavedFiltersFolder>(filterTemplate.getFolders(selectBuilder.toString(), paramMap));
		/*----------------------------------------------------------------------*/
		filterTemplate.getFolders(selectBuilder.toString(), paramMap);
		Map<Integer, SavedFiltersFolder> mapOfFolders = new HashMap();
		for(SavedFiltersFolder folder : folders) {
			mapOfFolders.put(folder.getParent_id(), folder);			
		}
		for(SavedFiltersFolder folder : folders) {
			if(folder.getParent_id() != null) {
				folder.setParent(mapOfFolders.get(folder.getParent_id()));
			}
		}
		
		
		/*--------------------------------------------------------------------*/
		
		
		return folders;
	}
	
	@Transactional(readOnly = false)
	public Set<SavedFiltersFolder> getDocumentList(final String searchView, final String loginUser){
		Set<String> loginUserRoles = adminService.getUsersRoles(loginUser);
		Set<String> listTypes = documentFolderService.getDocImportTypes(searchView).keySet();
		SelectBuilder selectBuilder = new SelectBuilder(VERTICATABLE, searchConfig.getSearchViews().get(searchView).getDocFolderSourceDBSchema());
		String view = "searchViewParam";
		String foldertype = "foldertype";
		String owner = "owner";
		String folderRoles = "folderRoles";
		selectBuilder.join(JoinType.LeftJoin, VERTICATABLE, FOLDERSROLESTABLE, "id", "savedfolder_id", null, null);
		String whereClauseForFolder = "(view = (:" + view + ")" + " " + "and" + " " + "foldertype in (:" + foldertype + ")" + " " + "and" + " " + "(owner =(:" + owner + ")" + " or " + "roles in (:" +  folderRoles + ")))";
		selectBuilder.where(whereClauseForFolder);
		LOGGER.debug("Query DocumentList" + selectBuilder.toString());
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put(view, searchView);
		paramMap.put(foldertype, listTypes);
		paramMap.put(owner, loginUser);
		paramMap.put(folderRoles, loginUserRoles);
		Set<SavedFiltersFolder> documentLists = new HashSet<SavedFiltersFolder>(filterTemplate.getFolders(selectBuilder.toString(), paramMap)) ;
		return documentLists;
	}
	
	@Transactional(readOnly = false)
	public Set<SavedFilter> getCustomFilters(final String searchView, final String loginUser){
		Set<String> loginUserRoles = adminService.getUsersRoles(loginUser);
		SelectBuilder selectBuilder = new SelectBuilder(CUSTOM_FILTERS_TABLE, searchConfig.getSearchViews().get(searchView).getDocFolderSourceDBSchema());
		String view = "searchViewParam";
		String owner = "owner";
		String folderRoles = "folderRoles";
		selectBuilder.join(JoinType.LeftJoin, CUSTOM_FILTERS_TABLE, CUSTOM_FILTER_ROLES_TABLE, "id", "savedfilters_id", null, null);
		String whereClauseForFilters = "(search_view = (:" + view + ")" + " " + "and" + " " + "(owner =(:" + owner + ")" + " or " + "roles in (:" +  folderRoles + ")))";
		selectBuilder.where(whereClauseForFilters);
		LOGGER.debug("Query CustomFilter" + selectBuilder.toString());
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put(view, searchView);
		paramMap.put(owner, loginUser);
		paramMap.put(folderRoles, loginUserRoles);
		
		return null;
	}
	@Transactional(readOnly = false)
	public DocumentFolderData getTreeData(final String searchView, final String loginUser){
		Set<SavedFiltersFolder> folders = getFolders(searchView, loginUser);
		Set<SavedFiltersFolder> documentLists = getDocumentList(searchView, loginUser); 
		folders.add(getRootFolder(searchView));
		return (new DocumentFolderData(folders, documentLists, Collections.EMPTY_SET));
	}
	
	@Transactional(readOnly = false)
    public SavedFiltersFolder add(final String name, final Integer parentId, final String loginUser, final String tooltip, 
    		final String searchView, final String folderType,final String roles, final boolean readOnly) { 
		final SavedFiltersFolder parentFolder = getFolder(parentId, loginUser, searchView);
		
		final String folderTypeValue;
        if(folderType.isEmpty())
        	folderTypeValue = null;
        else
        	folderTypeValue = folderType;
        
        Set<String> folderRoles = new Gson().fromJson(roles, Set.class);
		
		SavedFiltersFolder folder=new SavedFiltersFolder(name, parentFolder, loginUser, tooltip, searchView, false, folderTypeValue, folderRoles, readOnly);
        
        for(Map.Entry<String, DocImportType> entry : documentFolderService.getDocImportTypes(folder.getSearchView()).entrySet()) {
        	if(entry.getKey().equalsIgnoreCase(folder.getFolderType())) {        		
        		if(parametricService.getFilterFieldNames(folder.getSearchView()).get(entry.getValue().getImportField().trim()).isReferenceType()) {
            		folder.setPrimaryFolder(true);
            		break;
        		}        		
        	}
        }
        int doccount = folder.getDocCount();
		String foldertype = folder.getFolderType();
		String fullPath = folder.getFullPath();
		boolean is_primary = folder.isPrimaryFolder();
		String folderName = folder.getName();
		String owner = folder.getOwner();
		boolean is_readOnly = folder.getReadOnly();
		boolean restricted = folder.isRestricted();
		String view = folder.getSearchView();
		String descp = folder.getTooltip();
		int parent_id = folder.getParent().getId();
		
		String schema = searchConfig.getSearchViews().get(view).getDocFolderSourceDBSchema();
		String SQL = "INSERT INTO " + schema + ".savedfolders (doccount, foldertype, full_path, is_primary, name, owner, read_only, restricted, view, tooltip, parent_id) VALUES (:doccount, :foldertype, :fullPath, :is_primary, :folderName, :owner, :is_readOnly, :restricted, :view, :descp, :parent_id)";
		Map<String, Object> namedParameters = new HashMap<String, Object>();   
	        
	      namedParameters.put("doccount", doccount);
	      namedParameters.put("foldertype", foldertype);
	      namedParameters.put("fullPath", fullPath);
	      namedParameters.put("is_primary", is_primary);   
	      namedParameters.put("folderName", folderName);
	      namedParameters.put("owner", owner);
	      namedParameters.put("is_readOnly", is_readOnly);
	      namedParameters.put("restricted", restricted);
	      namedParameters.put("view", view);
	      namedParameters.put("descp", descp);
	      namedParameters.put("parent_id", parent_id);
        
        filterTemplate.add(SQL, namedParameters);
        LOGGER.debug("Created folder record = " + folderName + " view = " + view+ " foldertype = " + foldertype);
        return folder;
    }
	
	private SavedFiltersFolder getFolderByLabel(final String searchView, final String label, final String loginUser) {
        Set<String> listTypes = documentFolderService.getDocImportTypes(searchView).keySet();
        
        SelectBuilder builder = new SelectBuilder(VERTICATABLE, searchConfig.getSearchViews().get(searchView).getDocFolderSourceDBSchema());
		String namedParameter = "searchViewParam";
		String foldertype = "foldertype";
		String folderLabel = "label";
		String owner = "loginUser";
		String isShared = "isShared";
		String whereClauseForFolder = " (view = (:" + namedParameter +")" +" "+ "or" +" "+"foldertype in (:" + foldertype + "))" +" "+ "and " + "label = (:" + folderLabel + ") "+ "and" +" (owner =(:" +owner+")"+" or "+ "is_Shared = (:"+isShared+"))";
		builder.where(whereClauseForFolder);
		LOGGER.debug("Query getFolderByLabel" + builder.toString());
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put(namedParameter, searchView);
		paramMap.put(foldertype, listTypes);
		paramMap.put(folderLabel, label);
		paramMap.put(owner, loginUser);
		paramMap.put(isShared, "true");
		List<SavedFiltersFolder> documentFolder = filterTemplate.getFolderByLabel(builder.toString(), paramMap);
		SavedFiltersFolder docFol = null;
		try{
		 documentFolder = filterTemplate.getFolderByLabel(builder.toString(), paramMap);
		 if(documentFolder.size() > 1) {
				throw new IllegalArgumentException("Multipe folders found with same name !!!");
			}
		 if(documentFolder.size()==1)
		 {
			 docFol = documentFolder.get(0);
		 }
		}
		catch(Exception ex)
		{
			 docFol = null;
			//return dc;
			//ex.printStackTrace();
		}
		
		return docFol;
       // return documentFolder.get(0);
    }
	
	@Transactional(readOnly = false)
    public void delete(final List<String> listIds, final String loginUser, final String searchView) { 
		List<Integer> integerListIds=new ArrayList<Integer>();
    	for(String id:listIds)
    		integerListIds.add(Integer.valueOf(id));

		
		for(Integer id:integerListIds){
			String SQL1 = "DELETE FROM " + searchConfig.getSearchViews().get(searchView).getDocFolderSourceDBSchema() + ".documentfolder_references WHERE documentfolder_id = :id";
			Map<String, Object> namedParameters = new HashMap<String, Object>(); 
			namedParameters.put("id", id);
			filterTemplate.delete(SQL1, namedParameters);
			String SQL = "DELETE FROM " + searchConfig.getSearchViews().get(searchView).getDocFolderSourceDBSchema() + ".documentfolder WHERE id = :id";
			//Map<String, Object> namedParameters = new HashMap<String, Object>(); 
			//namedParameters.put("id", id);
			filterTemplate.delete(SQL, namedParameters);
			LOGGER.debug("Deleted folder record  = " + id );
		}
    }
	
	public SavedFiltersFolder getFolder(final int id, final String loginUser, String searchView){
		SelectBuilder builder = new SelectBuilder(VERTICATABLE, searchConfig.getSearchViews().get(searchView).getDocFolderSourceDBSchema());
		String namedParameter = "id";
		String whereClauseFolder = " id = (:" + namedParameter + ")";
		builder.where(whereClauseFolder);
		LOGGER.debug("Query getFolderById"+builder.toString());
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put(namedParameter, id);
		SavedFiltersFolder savedFolder = filterTemplate.getFolder(builder.toString(), paramMap);
		return savedFolder;
	}
	
	@Transactional(readOnly = false)
    public SavedFiltersFolder edit(final int id, final String tooltip, final String newLabel, final String loginUser, final Set<String> roles, final String searchView, final String importedValues) {
        final SavedFiltersFolder folder = getFolder(id, loginUser, searchView);

        final boolean labelChange = StringUtils.isNotEmpty(newLabel) && !folder.getName().equals(newLabel);
        
        if (labelChange && (getFolderByLabel(folder.getSearchView(), newLabel, loginUser) != null)) {
            throw new IllegalArgumentException("Name already in use");
        }

        if (labelChange) {
            folder.setName(newLabel);
        }

        if (tooltip != null) {
            folder.setTooltip(tooltip);
        }

        if (loginUser.equals(folder.getOwner()))  {
        	folder.setRoles(roles);
        }
		int doccount = folder.getDocCount();
		String foldertype = folder.getFolderType();
		boolean is_primary = folder.isPrimaryFolder();
		String label = folder.getName();
		String owner = folder.getOwner();
		boolean restricted = folder.isRestricted();
		String view = folder.getSearchView();
		boolean isselected = true;
		String SQL = "UPDATE " + searchConfig.getSearchViews().get(searchView).getDocFolderSourceDBSchema() + ".documentfolder SET doccount = :doccount,foldertype= :foldertype,is_primary= :is_primary,is_shared= :is_shared, label= :label,owner= :owner,restricted=:restricted, view= :view, tooltip= :tooltip WHERE id = :id";
		Map<String, Object> namedParameters = new HashMap<String, Object>();
		namedParameters.put("id", id);
		namedParameters.put("doccount", doccount);
	    namedParameters.put("foldertype", foldertype);
	    namedParameters.put("is_primary", is_primary);   
	    namedParameters.put("label", label);
	    namedParameters.put("owner", owner);
	    namedParameters.put("restricted", restricted);
	    namedParameters.put("view", view);
	    namedParameters.put("tooltip", tooltip);
	    // namedParameters.put("isselected", isselected);
        filterTemplate.update(SQL, namedParameters);
        
        String SQL_DELETE = "DELETE FROM " + searchConfig.getSearchViews().get(searchView).getDocFolderSourceDBSchema() + ".documentfolder_references WHERE documentfolder_id = :id";
        Map<String, Object> namedParametersDelete = new HashMap<String, Object>();
        namedParametersDelete.put("id", id);
        filterTemplate.delete(SQL_DELETE, namedParametersDelete);
        
        //splits the string on a delimiter defined as: zero or more whitespace, a literal comma, zero or more whitespace
        //code for updating edited values
        /*
        List<String> importedItemsList = Arrays.asList(importedValues.split("\\s*,\\s*"));
        Set<String> importSet = new HashSet<String>();
        importSet.addAll(importedItemsList);
        
        filterTemplate.addRefDocs(id, importSet, searchConfig.getSearchViews().get(searchView).getDocFolderSourceDBSchema());*/
        
        LOGGER.debug("Updated Record of folder = " + folder.getName() );
        return folder;
    }
	
	@Transactional
    public SavedFiltersFolder importDataToFolder(final int folderId, final Set<String> docs, final String loginUser, DocImportType docImportType, String searchView) {
       
        final SavedFiltersFolder folder = getFolder(folderId, loginUser, searchView);

        if (folder == null) {
            throw new IllegalArgumentException("Folder id [" + folderId + "] not found.");
        }
        String importType=null;
        for(final Map.Entry<String, DocImportType> entry : documentFolderService.getDocImportTypes(searchView).entrySet()) {
        	if(entry.getValue().equals(docImportType)) {
        		importType = entry.getKey();
        		break;
        	}
        }
        if(importType == null || !folder.getFolderType().equalsIgnoreCase(importType)) {
        	LOGGER.error("***** IMPORT FAILED *****");
        	LOGGER.error("Import type [" + importType + "] does not match the folder type [" + folder.getFolderType() + "] for Folder id [" + folderId + "]");
        	throw new IllegalArgumentException("Import type [" + importType + "] does not match the folder type [" + folder.getFolderType() + "] for Folder id [" + folderId + "]");
        }
        filterTemplate.addRefDocs(folderId, docs, searchConfig.getSearchViews().get(searchView).getDocFolderSourceDBSchema());
        return folder;
    }
	
	 @Transactional(readOnly = true)
	    public SavedFiltersFolder getTags(int id, final String loginUser, final String searchView) {
		 SelectBuilder builder = new SelectBuilder(VERTICATABLE_REF, searchConfig.getSearchViews().get(searchView).getDocFolderSourceDBSchema());
			String namedParameter = "docFolderId";
			String whereClauseForFolder = " documentfolder_id = (:" + namedParameter + ")";
			builder.where(whereClauseForFolder);
			LOGGER.debug("Query getFolderIdRef" + builder.toString());
			Map<String, Object> paramMap = new HashMap<String, Object>();
			paramMap.put(namedParameter, id);
			//List<DocumentFolder> documentFolders = filterTemplate.getFolders(builder.toString(), paramMap);
			SavedFiltersFolder documentFolderRefer = filterTemplate.getExportedFolder(builder.toString(), paramMap);
			return documentFolderRefer;
	       // return new List<DocumentFolder>(filterTemplate.getExportedFolder(builder.toString(), paramMap));
	    }
	 
	 
	 @Transactional(readOnly = true)
	    public String getFilterFieldByFolderId(final int id, final String searchView) {	              
	        final SavedFiltersFolder folder =  getFolder(id, null, searchView);
	        for(Map.Entry<String, DocImportType> entry : documentFolderService.getDocImportTypes(searchView).entrySet()) {
	        	if(entry.getKey().equalsIgnoreCase(folder.getFolderType())) {
	        		return entry.getValue().getImportField();        		
	        	}
	        }
	        return null;
	   }
	 
	 
	 	@Transactional(readOnly = false)
	    public boolean tagResultsVertica(final DocExportData docExportData, final String loginUser, final String securityInfo) throws Exception {
	        AuditLogger.log(loginUser, AuditActions.RESULTS_TAG, docExportData.getAuditData(AuditLogger.getDataMap()));
	        SavedFiltersFolder folder = getFolder(docExportData.getExportDocFolderId(), loginUser, docExportData.getSearchView());	        

	        if (folder == null) {
	            throw new IllegalArgumentException("Cannot find folder [" + docExportData.getExportDocFolderId() + "] for current user.");
	        }
	        String filterFieldName = getDocImportTypes(docExportData.getSearchView()).get(folder.getFolderType().trim()).getImportField();
	        
	        if(filterFieldName == null) {
	        	LOGGER.error("Folder [" + docExportData.getExportDocFolderId() + "] is missing the primary field for tagging");
	        	throw new IllegalArgumentException("Folder [" + docExportData.getExportDocFolderId() + "] is missing the primary field for tagging");
	        }
	        
	        final List<String> tagResults = searchService.getTagResultsVertica(docExportData, securityInfo, filterFieldName);     	
	       
	        
	        boolean changed = false; 
	        int maxSize = docfolderMaxResults > 0 ? docfolderMaxResults : 10000;
	        if(tagResults.size() < maxSize) {
	        	maxSize = tagResults.size();
	        }
	        
	        final List<String> iterator = new ArrayList<String>(tagResults.subList(0, maxSize));
	        if (iterator !=  null) {
	        	filterTemplate.addTagToFolder(folder, searchConfig.getSearchViews().get(docExportData.getSearchView()).getDocFolderSourceDBSchema(), iterator);	        	       	
	            changed = true;
	        }
	        return changed;
	 }
	 
	 private Map<String, DocImportType> getDocImportTypes(final String searchView) {
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
	 
	 public List<String> formatImportDataVertica(List<String> importedList){
	    	List<Integer> integerList=new ArrayList<Integer>();
	    	List<String> alphaNumeric=new ArrayList<String>();
	    	List<String> returnList=new ArrayList<String>();
	    	
	    	for(String temp : importedList) {
	    		try{
	    			integerList.add(Integer.parseInt(temp));
	    		}
	    		catch(Exception e){
	    			alphaNumeric.add(temp);
	    		}
	    		finally{
	    			//do nothing
	    		}
			 }
	    	Collections.sort(integerList);
	    	
	    	for(Integer temp : integerList){
	    		returnList.add(Integer.toString(temp));
	    	}
	    	Collections.sort(alphaNumeric);
			for(String temp : alphaNumeric){
				returnList.add(temp);
			}
			return returnList;
	    }
}