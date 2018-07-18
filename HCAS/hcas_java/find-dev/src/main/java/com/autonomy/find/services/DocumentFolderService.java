package com.autonomy.find.services;

/*
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/services/DocumentFolderService.java#30 $
 *
 * Copyright (c) 2013, Autonomy Systems Ltd.
 *
 * Last modified by $Author: wo $ on $Date: 2014/05/13 $ 
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autonomy.aci.actions.idol.query.Document;
import com.autonomy.aci.actions.idol.query.QueryResponse;
import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.aci.content.identifier.reference.ReferencesBuilder;
import com.autonomy.find.api.database.DocumentFolder;
import com.autonomy.find.api.database.SavedFilter;
import com.autonomy.find.api.database.SavedFiltersFolder;
import com.autonomy.find.config.FindConfig;
import com.autonomy.find.config.SearchConfig;
import com.autonomy.find.config.SearchView;
import com.autonomy.find.dto.SearchRequestData;
import com.autonomy.find.dto.Parametric.BoolOp;
import com.autonomy.find.dto.Parametric.DocExportData;
import com.autonomy.find.dto.Parametric.DocImportData;
import com.autonomy.find.dto.Parametric.DocImportType;
import com.autonomy.find.dto.Parametric.ExportFormat;
import com.autonomy.find.dto.Parametric.FieldParams;
import com.autonomy.find.dto.Parametric.FilterGroup;
import com.autonomy.find.fields.FilterField;
import com.autonomy.find.fields.FilterOperator;
import com.autonomy.find.processors.StoreStateProcessor;
import com.autonomy.find.services.admin.AdminService;
import com.autonomy.find.services.search.PrintFieldsResultIterator;
import com.autonomy.find.services.search.ReferenceResultsIterator;
import com.autonomy.find.util.CollUtils;
import com.autonomy.find.util.FieldTextUtil;
import com.autonomy.find.util.FieldUtil;
import com.autonomy.find.util.StateResult;
import com.autonomy.find.util.audit.AuditActions;
import com.autonomy.find.util.audit.AuditLogger;
import com.autonomy.vertica.service.SearchVerticaService;
import com.autonomy.vertica.templates.FilterTemplate;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.autonomy.find.dto.DocumentFolderData;
import com.autonomy.find.dto.ImportDataResults;

@Service
public class DocumentFolderService {

    @Autowired
    private SearchConfig searchConfig;

    @Autowired
    private AciService searchAciService;

    @Autowired
    private ParametricService parametricService;

    @Autowired
    private SearchService searchService;
    
    @Autowired 
    private FilterService filterService;
    
    @Autowired
    private AdminService adminService;

    @Autowired
    @Qualifier("findSessionFactory")
    private SessionFactory sessionFactory;

    @Value("${discoverSearch.docfolder.maxResults}")
    private int docfolderMaxResults;

    @Value("${discoverSearch.import.delimiter}")
    private String importDelimiter;

    @Value("${discoverSearch.export.batchsize}")
    private Integer exportBatchSize;

    @Value("${discoverSearch.import.batchsize}")
    private Integer importBatchSize;
    
    @Autowired
    private SearchVerticaService verticaService;
    
    @Autowired
    private DocumentVerticaFolderService documentVerticaFolderService;
    
    @Autowired
    private FilterTemplate filterTemplate;

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentFolderService.class);

    private static final int DEFAULT_EXPORT_BATCH = 200;
    private static final int DEFAULT_IMPORT_BATCH = 1000;
    
    public void checkDuplicate(final String folderType, final String searchView, final Integer parentId, final String name, final String loginUser, final Session session) {
    	if (folderType!=null && getDocumentListByLabel(searchView,  parentId, name.toUpperCase(), loginUser, session) != null) {
            throw new IllegalArgumentException("Document List Name already in use");
        }
        else if(folderType==null && getFolderByLabel(searchView, parentId, name.toUpperCase(), loginUser,session) != null){
        	throw new IllegalArgumentException("Folder Name already in use");
        }
    }
    
    @Transactional(readOnly = false)
    public SavedFiltersFolder add(final String name, final Integer parentId, final String loginUser, final String tooltip, 
    		final String searchView, final String folderType,final String roles, final boolean readOnly) {
        final Session session = sessionFactory.getCurrentSession();
        final Set<String> folderRoles = new Gson().fromJson(roles, Set.class);
        final SavedFiltersFolder parentFolder = (SavedFiltersFolder) session.get(SavedFiltersFolder.class, parentId);
        
        checkPermissionFolder(parentFolder, loginUser, "add");
        
        final String folderTypeValue;
        if(folderType.isEmpty())
        	folderTypeValue = null;
        else
        	folderTypeValue = folderType;
        
        SavedFiltersFolder folder=new SavedFiltersFolder(name, parentFolder, loginUser, tooltip, searchView, false, folderTypeValue, folderRoles, readOnly);
        
        checkDuplicate(folderTypeValue, folder.getSearchView(),  folder.getParent().getId(), folder.getName(), loginUser, session);
        
        for(Map.Entry<String, DocImportType> entry : getDocImportTypes(folder.getSearchView()).entrySet()) {
        	if(entry.getKey().equalsIgnoreCase(folder.getFolderType())) {        		
        		if(parametricService.getFilterFieldNames(folder.getSearchView()).get(entry.getValue().getImportField().trim()).isReferenceType()) {
        			// this is a primary folder
            		folder.setPrimaryFolder(true);
            		break;
        		}        		
        	}
        }
        session.persist(folder);
        return folder;
    }
    
    public boolean isPrimary(SavedFiltersFolder folder, String searchView) {
    	for(Map.Entry<String, DocImportType> entry : getDocImportTypes(searchView).entrySet()) {
        	if(entry.getKey().equalsIgnoreCase(folder.getFolderType())) {        		
        		if(parametricService.getFilterFieldNames(searchView).get(entry.getValue().getImportField().trim()).isReferenceType()) {
        			// this is a primary folder
            		return true;
        		}        		
        	}
        }
    	return false;
    }

    @Transactional(readOnly = false)
    public void delete(final List<String> listIds, final String loginUser) {
        final Session session = sessionFactory.getCurrentSession();
        for(String id : listIds){
        	if(id.contains("filter")){
        		String integerPartOfId = id.replace("filter", "");
        		SavedFilter filter = filterService.getFilter(Integer.valueOf(integerPartOfId), session, loginUser);
        		checkPermissionFilter(filter, loginUser);
        	}
        	else{
        		SavedFiltersFolder folder = getFolder(Integer.valueOf(id), session, loginUser);
        		checkPermissionFolder(folder, loginUser, "delete");
        	}
        }
        
        for(String id : listIds){
        	if(id.contains("filter")){
        		String integerPartOfId = id.replace("filter", "");
        		session.delete(filterService.getFilter(Integer.valueOf(integerPartOfId), session, loginUser));
        	}
        	else{
        		session.delete(getFolder(Integer.valueOf(id), session, loginUser));
        	}
        }
    }

    @Transactional(readOnly = false)
    public SavedFiltersFolder edit(final int id, final String tooltip, final String newLabel, final String loginUser, final Boolean readOnly, final String roles, final String importedValues, final String ChildIds, final boolean dataDifference) {
        final Session session = sessionFactory.getCurrentSession();
        final SavedFiltersFolder folder = getFolder(id, session, loginUser);
        
        checkPermissionFolder(folder, loginUser, "edit");

        final boolean labelChange = StringUtils.isNotEmpty(newLabel) && !folder.getName().equals(newLabel);
        final Set<String> folderRoles = new Gson().fromJson(roles, Set.class );
        final List<String> listChildIds = new Gson().fromJson(ChildIds, List.class);
       
        final Set<String> rolesDifference = Sets.symmetricDifference(folderRoles, folder.getRoles());
        
        //splits the string on a delimiter defined as: zero or more whitespace, a literal comma, zero or more whitespace
        List<String> importedItemsList = Arrays.asList(importedValues.split("\\s*,\\s*"));
        Set<String> importSet = new HashSet<String>();
        importSet.addAll(importedItemsList);

        if (labelChange) {
        	checkDuplicate(folder.getFolderType(), folder.getSearchView(),  folder.getParent().getId(), newLabel, loginUser, session);
            folder.setName(newLabel);
        }

        if (tooltip != null) {
            folder.setTooltip(tooltip);
        }
        
        if(!readOnly.equals(folder.getReadOnly())){
        	folder.setReadOnly(readOnly);
        	for(String nodeId : listChildIds){
        		if(!nodeId.contains("filter")){
        			final SavedFiltersFolder childFolder = getFolder(Integer.valueOf(nodeId), session, loginUser);
            		childFolder.setReadOnly(readOnly);
        		}
        		else{
        			String intPartId = nodeId.replace("filter", "");
        			final SavedFilter filter = filterService.getFilter(Integer.valueOf(intPartId), session, loginUser);
        			filter.setReadOnly(readOnly);
        		}
        	}
        }
      
        if (!rolesDifference.isEmpty()) {
       		folder.getRoles().clear();
           	folder.setRoles(folderRoles);
        	for(String nodeId : listChildIds){
        		if(!nodeId.contains("filter")){
        			final SavedFiltersFolder childFolder = getFolder(Integer.valueOf(nodeId), session, loginUser);
            		childFolder.getRoles().clear();
            		childFolder.setRoles(folderRoles);
        		}
        		else{
        			String intPartId = nodeId.replace("filter", "");
        			final SavedFilter filter = filterService.getFilter(Integer.valueOf(intPartId), session, loginUser);
        			filter.getRoles().clear();
        			filter.setRoles(folderRoles);
        		}
        	}
        }

        session.update(folder);
        return folder;
    }
    @Transactional(readOnly=false)
    public SavedFilter editFilter(final int id, final String newName, final String description, final String loginUser, final Boolean readOnly, final String roles){
    	final Session session = sessionFactory.getCurrentSession();
    	final SavedFilter filter = (SavedFilter) session.get(SavedFilter.class, Integer.valueOf(id));
    	
    	checkPermissionFilter(filter, loginUser);
    	
    	final boolean nameChange = StringUtils.isNotEmpty(newName) && !filter.getName().equals(newName);
    	final Set<String> filterRoles = new Gson().fromJson(roles, Set.class);
    	
    	final Set<String> rolesDifference = Sets.symmetricDifference(filterRoles, filter.getRoles());
    	
    	if(nameChange && filterService.getFilterByLabel(filter.getSearchView(), filter.getParent().getId(), newName, loginUser, session)!=null){
    		throw new IllegalArgumentException("Filter name already in use");
    	}
    	
    	if(nameChange){
    		filter.setName(newName);
    	}
    	
    	if(!readOnly.equals(filter.getReadOnly())){
    		filter.setReadOnly(readOnly);
    	}
    	
    	if(description != null){
    		filter.setDescription(description);
    	}
    	
    	if(loginUser.equals(filter.getOwner()) && !rolesDifference.isEmpty()){
    		filter.getRoles().clear();
    		filter.setRoles(filterRoles);
    	}
    	session.update(filter);    	
    	return filter;
    }
    
    @Transactional(readOnly = false)
    public boolean tag(final String loginUser, final int id, final boolean canChangeRestricted, final String... references) {
        final Session session = sessionFactory.getCurrentSession();
        final SavedFiltersFolder folder = getFolder(id, session, loginUser);

        if (!canChangeRestricted && folder.isRestricted()) {
            throw new IllegalArgumentException("Restricted folders cannot be edited unless canChangeRestricted is set");
        }

        final boolean changed = folder.getRefs().addAll(Arrays.asList(references));
        folder.setDocCount(folder.getRefs().size());
        sessionFactory.getCurrentSession().update(folder);
        return changed;
    }

    public SavedFiltersFolder getFolder(final int id, final Session session, final String loginUser) {
        final SavedFiltersFolder folder =  (SavedFiltersFolder) session.get(SavedFiltersFolder.class, id);

        /*if (!folder.getOwner().equals(loginUser)) {
            throw new IllegalArgumentException("Current user is not the owner of this folder.");
        }*/

        return folder;
    }

    private SavedFiltersFolder getDocumentListByLabel(final String searchView, final Integer parentId, final String name, final String loginUser, final Session session) {
        final Query query = session.getNamedQuery(SavedFiltersFolder.GET_DOCUMENTLIST_BY_LABEL);
        Set<String> listTypes = getDocImportTypes(searchView).keySet();
        
        query.setParameterList("folderTypeList", listTypes);
        query.setParameter("searchView", searchView);
        query.setParameter("name", name);
        query.setParameter("parent_id", parentId);
        query.setParameter("owner", loginUser);

        //noinspection unchecked
        return (SavedFiltersFolder) query.uniqueResult();
    }
    
    private SavedFiltersFolder getFolderByLabel(final String searchView, final Integer parentId, final String name, final String loginUser, final Session session){
    	final Query query = session.getNamedQuery(SavedFiltersFolder.GET_FOLDER_BY_LABEL);
    	query.setParameter("searchView", searchView);
    	query.setParameter("name", name);
    	query.setParameter("parent_id", parentId);
    	query.setParameter("owner", loginUser);
    	
    	return (SavedFiltersFolder) query.uniqueResult();
    }
    @Transactional(readOnly = false)
    public boolean untag(final String loginUser, final int id, final boolean canChangeRestricted, final String... references) {
        final Session session = sessionFactory.getCurrentSession();

        final SavedFiltersFolder folder = getFolder(id, session, loginUser);

        if (!canChangeRestricted && folder.isRestricted()) {
            throw new IllegalArgumentException("Restricted folders cannot be edited unless canChangeRestricted is set");
        }

        final boolean changed = folder.getRefs().removeAll(Arrays.asList(references));
        folder.setDocCount(folder.getRefs().size());
        sessionFactory.getCurrentSession().update(folder);
        return changed;
    }

    @Transactional(readOnly = true)
    public Set<String> getTags(final int id, final String loginUser) {
        // we create a new object to
        //   a) ensure this copy is not modified
        //   b) prefetch the lazy-loaded collection
        return new HashSet<>(getFolder(id, sessionFactory.getCurrentSession(), loginUser).getRefs());
    }

    @Transactional(readOnly = true)
    public Set<String> getFilterFolderTags(final int id) {
        final Session session = sessionFactory.getCurrentSession();
        // we create a new object to
        //   a) ensure this copy is not modified
        //   b) prefetch the lazy-loaded collection
        final SavedFiltersFolder folder =  (SavedFiltersFolder) session.get(SavedFiltersFolder.class, id);
        return new HashSet<>(folder.getRefs());
    }
    
    @Transactional(readOnly = true)
    public String getFilterFieldByFolderId(final int id, final String searchView) {
        final Session session = sessionFactory.getCurrentSession();        
        final SavedFiltersFolder folder =  (SavedFiltersFolder) session.get(SavedFiltersFolder.class, id);
        for(Map.Entry<String, DocImportType> entry : getDocImportTypes(searchView).entrySet()) {
        	if(entry.getKey().equalsIgnoreCase(folder.getFolderType())) {
        		return entry.getValue().getImportField();        		
        	}
        }
        return null;
    }


    @Transactional(readOnly = true)
    public List<SavedFiltersFolder> getFoldersByRef(final String searchView, final String ref, final String loginUser) {
        final Session session = sessionFactory.getCurrentSession();
        Set<String> userRoles = adminService.getUsersRoles(loginUser);
        final Query query = session.getNamedQuery(SavedFiltersFolder.GET_BY_REF_AND_VIEW);
        Set<String> listTypes = new HashSet<String>();
        if(getDocImportTypes(searchView) != null) {
        	listTypes = getDocImportTypes(searchView).keySet();
        }
        
        Set<SavedFiltersFolder> documentLists = new HashSet<SavedFiltersFolder>();
        for(String role : userRoles){
        	query.setParameterList("folderTypeList", listTypes);
        	query.setParameter("searchView", searchView);
        	query.setParameter("ref", ref);
        	query.setParameter("owner", loginUser);
        	query.setParameter("role", role);
        	documentLists.addAll(query.list());
        }

        //no inspection unchecked
        return (List<SavedFiltersFolder>) documentLists;
    }
    
    @Transactional(readOnly = true)
    public boolean isRefInFolder(final int id, final String ref) {
        Set<String> refs = getFilterFolderTags(id);
        if(refs != null) {
        	return refs.contains(ref) ? true : false;
        }
        return false;
    }

    @Transactional(readOnly = false)
    public boolean tagResults(final DocExportData docExportData, final String loginUser, final String securityInfo) {
        AuditLogger.log(loginUser, AuditActions.RESULTS_TAG, docExportData.getAuditData(AuditLogger.getDataMap()));

        final Session session = sessionFactory.getCurrentSession();
        final SavedFiltersFolder folder = getFolder(docExportData.getExportDocFolderId(), session, loginUser);

        if (folder == null) {
            throw new IllegalArgumentException("Cannot find folder [" + docExportData.getExportDocFolderId() + "] for current user.");
        }
       
        
        final ReferenceResultsIterator iterator = searchService.getTagResults(docExportData, securityInfo);     	
       

        boolean changed = false;
        
        final int batchSize = importBatchSize != null ? importBatchSize : DEFAULT_IMPORT_BATCH;
        
        
        if (iterator !=  null) {
            iterator.setMaxBatchDocs(batchSize);
            final Set<String> folderRefs = folder.getRefs();
            while (iterator.hasNext()) {
                folderRefs.addAll(iterator.next());
            }
            folder.setDocCount(folderRefs.size());
            session.update(folder);
            changed = true;

            iterator.destroy();

        }

        return changed;
    }
    
    @Transactional(readOnly = false)
    public boolean tagResultsVertica(final DocExportData docExportData, final String loginUser, final String securityInfo) throws Exception {
        AuditLogger.log(loginUser, AuditActions.RESULTS_TAG, docExportData.getAuditData(AuditLogger.getDataMap()));

        final Session session = sessionFactory.getCurrentSession();
        SavedFiltersFolder folder = getFolder(docExportData.getExportDocFolderId(), session, loginUser);
        
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
    		final Set<String> folderRefs = folder.getRefs();          
            folderRefs.addAll(iterator);           
            folder.setDocCount(folderRefs.size());
            session.update(folder);               	
            changed = true;
        }
        return changed;
    }

    @Transactional(readOnly = true)
    public void exportResults(final DocExportData docExportData, final String loginUser, final PrintWriter writer, final String securityInfo) {
        AuditLogger.log(loginUser, AuditActions.RESULTS_EXPORT, docExportData.getAuditData(AuditLogger.getDataMap()));

        final PrintFieldsResultIterator iterator = searchService.getExportResultsFields(docExportData, securityInfo);

        printResultsFields(iterator, writer, docExportData);

        if (iterator != null) {
            iterator.destroy();
        }

    }
    
    @Transactional(readOnly = true)
    public void exportResultsVertica(final DocExportData docExportData, final String loginUser, final PrintWriter writer, final String securityInfo) throws Exception {
        AuditLogger.log(loginUser, AuditActions.RESULTS_EXPORT, docExportData.getAuditData(AuditLogger.getDataMap()));

        final com.autonomy.vertica.query.QueryResponse iterator = searchService.getExportResultsFieldsVertica(docExportData, securityInfo);

        printResultsFieldsVertica(iterator, writer, docExportData);

       /* if (iterator != null) {
            iterator.destroy();
        }*/

    }

    @Transactional(readOnly = true)
    public void exportDocFolder(final DocExportData docExportData, final String loginUser, final PrintWriter writer, final String securityInfo) {
        AuditLogger.log(loginUser, AuditActions.DOCFOLDER_EXPORT, docExportData.getAuditData(AuditLogger.getDataMap()));

        Set<String> tags = getTags(docExportData.getExportDocFolderId(), loginUser);

        final String docRefFieldname = searchConfig.getSearchViews().get(docExportData.getSearchView()).getDocRefField();
        final String[] printFields = docExportData.getExportSourceFields().split(",");

        PrintFieldsResultIterator iterator = null;
        if (!tags.isEmpty()) {
            iterator = getFolderExportFields(docRefFieldname, tags.size(), docExportData.getSearchView(), tags, Arrays.asList(printFields), securityInfo);
        }

        printResultsFields(iterator, writer, docExportData);

        if (iterator != null) {
            iterator.destroy();
        }

    }
    
    @Transactional(readOnly = true)
    public void exportDocFolderVertica(final DocExportData docExportData, final String loginUser, final PrintWriter writer, final String securityInfo) throws Exception {
        AuditLogger.log(loginUser, AuditActions.DOCFOLDER_EXPORT, docExportData.getAuditData(AuditLogger.getDataMap()));

        Set<String> tags = getTags(docExportData.getExportDocFolderId(), loginUser);

        final String docRefFieldname = searchConfig.getSearchViews().get(docExportData.getSearchView()).getDocRefField();
        final String[] printFields = docExportData.getExportSourceFields().split(",");

        com.autonomy.vertica.query.QueryResponse iterator = null;
        if (!tags.isEmpty()) {
            iterator = getFolderExportFieldsVertica(docExportData, docRefFieldname, tags.size(), docExportData.getSearchView(), tags, Arrays.asList(printFields), securityInfo);
        }

        printResultsFieldsVertica(iterator, writer, docExportData);

       /* if (iterator != null) {
            iterator.destroy();
        }*/

    }
        
    @Transactional(readOnly = true)
    public void exportDocFolderVerticaNew(final DocExportData docExportData, final String loginUser, final PrintWriter writer, final String securityInfo) throws Exception {
        AuditLogger.log(loginUser, AuditActions.DOCFOLDER_EXPORT, docExportData.getAuditData(AuditLogger.getDataMap()));

        Set<String> tags = new HashSet<String>();
        SavedFiltersFolder tagsRef = documentVerticaFolderService.getTags(docExportData.getExportDocFolderId(), loginUser, docExportData.getSearchView());
        tags=tagsRef.getRefs();
        
        final String docRefFieldname = searchConfig.getSearchViews().get(docExportData.getSearchView()).getDocRefField();
        final String[] printFields = docExportData.getExportSourceFields().split(",");

        com.autonomy.vertica.query.QueryResponse iterator = null;
        if (!tags.isEmpty()) {
            iterator = getFolderExportFieldsVertica(docExportData, docRefFieldname, tags.size(), docExportData.getSearchView(), tags, Arrays.asList(printFields), securityInfo);
        }

        printResultsFieldsVertica(iterator, writer, docExportData);
    }


    private void printResultsFields(final PrintFieldsResultIterator iterator, final PrintWriter writer, final DocExportData docExportData) {
        final String[] sourceFields = docExportData.getExportSourceFields().split(",");
        final String[]  targetFields = docExportData.getExportTargetFields().split(",");
        final ExportFormat exportFormat = docExportData.getExportFormat();

        final String exportHeader = setExportOutputHeader(targetFields, exportFormat);
        writer.write(exportHeader);

        if (iterator != null)  {
            final int maxBatchDocs = exportBatchSize != null ? exportBatchSize : DEFAULT_EXPORT_BATCH;
            iterator.setMaxBatchDocs(maxBatchDocs);

            while(iterator.hasNext()) {
                final QueryResponse response = iterator.next();
                if (response != null) {
                    convertOutput(response.getDocuments(), exportFormat, sourceFields, targetFields, writer);
                } else {
                    LOGGER.error("Export results: NULL query response.");
                }
            }
        }
        final String exportFooter = setExportOutputFooter(exportFormat);
        writer.write(exportFooter);
    }
    
    private void printResultsFieldsVertica(final com.autonomy.vertica.query.QueryResponse response, final PrintWriter writer, final DocExportData docExportData) {
        final String[] sourceFields = docExportData.getExportSourceFields().split(",");
        final String[]  targetFields = docExportData.getExportTargetFields().split(",");
        final ExportFormat exportFormat = docExportData.getExportFormat();

        final String exportHeader = setExportOutputHeader(targetFields, exportFormat);
        writer.write(exportHeader);

        final int maxBatchDocs = exportBatchSize != null ? exportBatchSize : DEFAULT_EXPORT_BATCH;;
        if (response != null) {
            convertOutputVertica(response.getDocuments(), exportFormat, sourceFields, targetFields, writer);
        } else {
            LOGGER.error("Export results: NULL query response.");
        }       
        
        final String exportFooter = setExportOutputFooter(exportFormat);
        writer.write(exportFooter);
    }
    
    private void convertOutputVertica(final List<com.autonomy.vertica.query.Document> documents, final ExportFormat exportFormat, final String[] sourceFields, final String[] targetFields, final PrintWriter writer) {
        for( final com.autonomy.vertica.query.Document doc : documents) {
            final StringBuilder contentBuilder = new StringBuilder();
            setDocHeader(exportFormat, contentBuilder);
            //LOGGER.debug("Exporting idol doc field keys {}", doc.getDocumentFields().keySet());
            final StringBuilder docOutput = new StringBuilder();
            for (int j = 0; j < sourceFields.length; j++) {
                final String sourceField = sourceFields[j];
                final String targetField = targetFields[j];

                final String values = StringUtils.join(doc.getDocumentFieldValues(sourceField), ",");
                LOGGER.trace("Exporting field [{}]: values: {}", sourceField, values);
                if (ExportFormat.CSV == exportFormat) {
                    outputCsv(values, docOutput);                    
                } else {
                    outputXml(targetField, values, docOutput);
                }
            }
            setDocFooter(exportFormat, docOutput);
            contentBuilder.append(docOutput.toString());

            writer.write(contentBuilder.toString());
        }


    }

    private void convertOutput(final List<Document> documents, final ExportFormat exportFormat, final String[] sourceFields, final String[] targetFields, final PrintWriter writer) {
        for( final Document doc : documents) {
            final StringBuilder contentBuilder = new StringBuilder();
            setDocHeader(exportFormat, contentBuilder);
            LOGGER.debug("Exporting idol doc field keys {}", doc.getDocumentFields().keySet());
            final StringBuilder docOutput = new StringBuilder();
           /* if (ExportFormat.CSV == exportFormat) {
                docOutput.append("\"");        
            }*/
            for (int j = 0; j < sourceFields.length; j++) {
                final String sourceField = sourceFields[j];
                final String targetField = targetFields[j];

                final String values = StringUtils.join(doc.getDocumentFieldValues(sourceField), ",");
                LOGGER.trace("Exporting field [{}]: values: {}", sourceField, values);
                if (ExportFormat.CSV == exportFormat) {
                    outputCsv(values, docOutput);
                } else {
                    outputXml(targetField, values, docOutput);
                }
            }
            setDocFooter(exportFormat, docOutput);
            contentBuilder.append(docOutput.toString());

            writer.write(contentBuilder.toString());
        }


    }

    private void setDocFooter(final ExportFormat exportFormat, final StringBuilder outputBuilder) {
        if (ExportFormat.XML == exportFormat) {
            outputBuilder.append("\t").append("</doc>");
        }
        outputBuilder.append("\n");
    }

    private void setDocHeader(final ExportFormat exportFormat, final StringBuilder outputBuilder) {
        if (ExportFormat.XML == exportFormat) {
            outputBuilder.append("\t").append("<doc>").append("\n");
        }
    }

    private String setExportOutputHeader(final String[] targetFields, final ExportFormat exportFormat) {
        final StringBuilder outputBuilder = new StringBuilder();
        if (ExportFormat.CSV == exportFormat) {
            for(final String field : targetFields) {
                outputBuilder.append(StringEscapeUtils.escapeCsv(field)).append(",");
            }
            outputBuilder.deleteCharAt(outputBuilder.length() - 1);
            outputBuilder.append("\n");
        } else {
            outputBuilder.append("<export>").append("\n");
        }

        return outputBuilder.toString();
    }

    private String setExportOutputFooter(final ExportFormat exportFormat) {
        final StringBuilder outputBuilder = new StringBuilder();
        if (ExportFormat.XML == exportFormat) {
            outputBuilder.append("</export>");
        }

        return outputBuilder.toString();

    }


    private void outputCsv(final String fieldVal, final StringBuilder outputBuilder) {

        if (outputBuilder.length() > 0 ) {
            outputBuilder.append(",");
        }
        String outputVal=StringEscapeUtils.escapeCsv(fieldVal);
        if (!outputVal.startsWith("\"")) {
        	outputVal="\"\t"+outputVal+"\"";
        }
        outputBuilder.append(outputVal);
    }

    private void outputXml(final String targetField, final String fieldVal, final StringBuilder outputBuilder) {
        String fieldTag = targetField.replaceAll("\\s+", "_");
        outputBuilder.append("\t\t").append("<").append(fieldTag).append(">");
        outputBuilder.append(StringEscapeUtils.escapeXml(fieldVal));
        outputBuilder.append("</").append(fieldTag).append(">").append("\n");
    }

    @Transactional
    public ImportDataResults importData(final DocImportData fieldImport, final String loginUser, final String securityInfo) throws Exception {
        if (fieldImport.getFolderId() == 0) {
            throw new IllegalArgumentException("Missing folder id");
        }

        Set<String> matchedDocs = new HashSet<String>();


        final String data = fieldImport.getImportData();
        final Set<String> importedValues = new HashSet<String>();

        if (data != null) {
            final BufferedReader reader = new BufferedReader( new StringReader(data));
            try {
                String line = reader.readLine();
                if (fieldImport.isIgnoreFirstRow()) {
                    line = reader.readLine();
                }
                while( line != null ) {
                    line = line.trim().replaceAll("[,;]$", "");
                    if (StringUtils.isNotEmpty(line)) {
                        importedValues.add(line);
                    }
                    line = reader.readLine();
                }

            } catch (IOException ex) {
                throw new Error("Parsing import file error: " + ex.getMessage());
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                  // do nothing;
                }
            }

            // do a search to get the doc ids
            final ReferenceResultsIterator iterator = getDocReferences(fieldImport.getImportType().getImportField(), fieldImport.getSearchView(), importedValues, securityInfo);
            final int batchSize = importBatchSize != null ? importBatchSize : DEFAULT_IMPORT_BATCH;
            if (iterator != null) {
                iterator.setMaxBatchDocs(batchSize);
                while(iterator.hasNext()) {
                    matchedDocs.addAll(iterator.next());
                }

                iterator.destroy();
            }
        }

        if (!matchedDocs.isEmpty()) {
            importDataToFolder(fieldImport.getFolderId(), importedValues, loginUser, fieldImport.getImportType(), fieldImport.getSearchView());
        }

        final Map<String, Object> auditData = AuditLogger.getDataMap();
        auditData.put("importType", fieldImport.getImportType());
        auditData.put("matchedDocs", matchedDocs.size());

        AuditLogger.log(loginUser, AuditActions.DOCFOLDER_IMPORT, auditData);

        //return matchedDocs.size();
        return new ImportDataResults(matchedDocs.size(), null);
    }
    
    @Transactional
    public ImportDataResults importDataVertica(final DocImportData fieldImport, final String loginUser, final String securityInfo, final boolean isDocFolderInVertica) throws Exception {
        if (fieldImport.getFolderId() == 0) {
            throw new IllegalArgumentException("Missing folder id");
        }
        final Session session = sessionFactory.getCurrentSession();
        final SavedFiltersFolder folder = getFolder(fieldImport.getFolderId(), session, loginUser);
        checkPermissionFolder(folder, loginUser, "Import");
        
        Set<String> matchedDocs = new HashSet<String>();

        final String data = fieldImport.getImportData();
        final Set<String> importedValues = new HashSet<String>();
        final Map<String, Integer> importedValues_map = new HashMap<String, Integer>(); 
        String duplicateCountMessage="";
        String duplicateValuesMessage="";
        if (data != null) {
            final BufferedReader reader = new BufferedReader( new StringReader(data));
            try {
                String line = reader.readLine();
                if (fieldImport.isIgnoreFirstRow()) {
                    line = reader.readLine();
                }
                int count=0;
                while( line != null ) {
                    line = line.trim().replaceAll("[\"\t;]", "");
                    if (StringUtils.isNotEmpty(line)) {
                        importedValues.add(line);
                        
                        if(importedValues_map.get(line)==null)
                        {
                        	importedValues_map.put(line, 1);
                        }
                        else{
                        	count=importedValues_map.get(line);
                        	importedValues_map.put(line, ++count);
                        }
                    }
                    line = reader.readLine();
                }
                
                final List<String> duplicateValuesList = new ArrayList<String>();
                
                for (Map.Entry<String, Integer> ee : importedValues_map.entrySet()) {
                    if(ee.getValue()>1){
                    	duplicateValuesList.add(ee.getKey());
                    }
                }
                duplicateCountMessage=duplicateValuesList.size() + " entries found to be duplicates.";
                if(duplicateValuesList.size()>0){
                duplicateValuesMessage="Duplicate Values: [ ";
                
                for(int i=0; i<duplicateValuesList.size();i++)
                	duplicateValuesMessage+= duplicateValuesList.get(i)+" ";
                
                duplicateValuesMessage+="] were imported only once." + "\n";  
                }
            } catch (IOException ex) {
                throw new Error("Parsing import file error: " + ex.getMessage());
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {
                  // do nothing;
                }
            }

            // do a search to get the doc ids
            // TODO query Vertica to match the references... 
            List<String> importedList = new ArrayList<String>();
            importedList.addAll(importedValues);
            final com.autonomy.vertica.query.QueryResponse response = getDocReferencesVertica(fieldImport.getImportType().getImportField(), fieldImport.getSearchView(), importedList, securityInfo);
            
            final int batchSize = importBatchSize != null ? importBatchSize : DEFAULT_IMPORT_BATCH;
            if(response != null) {
            	for(com.autonomy.vertica.query.Document doc : response.getDocuments()) {
            		if(doc.getDocumentFields().entrySet() != null) {
            			for(Map.Entry<String, List<com.autonomy.vertica.query.DocumentField>> entry : doc.getDocumentFields().entrySet()) {
            				if(entry != null && entry.getValue() != null) {
            					for(com.autonomy.vertica.query.DocumentField field : entry.getValue()) {
            						if(field != null && field.getValue() != null) {
            							matchedDocs.add(field.getValue());
            						}            					
            					}
            				}
            			}
            		}
            	} 
            }           
        }

        if(!isDocFolderInVertica) { /// postgres
        	importDataToFolder(fieldImport.getFolderId(), importedValues, loginUser, fieldImport.getImportType(), fieldImport.getSearchView());
        } else {
        	documentVerticaFolderService.importDataToFolder(fieldImport.getFolderId(), importedValues, loginUser, fieldImport.getImportType(), fieldImport.getSearchView());
        }

        final Map<String, Object> auditData = AuditLogger.getDataMap();
        auditData.put("importType", fieldImport.getImportType());
        auditData.put("matchedDocs", matchedDocs.size());

        AuditLogger.log(loginUser, AuditActions.DOCFOLDER_IMPORT, auditData);
        
        StringBuffer documentMessage = new StringBuffer();
        
        
        if(importedValues.isEmpty()) {
        	documentMessage.append("<strong>"+"Removed Successfully "+"</strong>"+ "\n" );
        	documentMessage.append(" All entries removed from the list ");
        } else {
        	 documentMessage.append("<strong>"+"Added Successfully "+"</strong>"+ "\n" );
        	 int UnMatchedDocsCount = importedValues.size()-matchedDocs.size();
             int totalImport=importedValues.size();
             documentMessage.append(matchedDocs.size() + " entries exist in data set. " + "\n");
             documentMessage.append(UnMatchedDocsCount + " entries do not exist in data set."+ "\n");
             documentMessage.append(duplicateCountMessage+ "\n");
             documentMessage.append(duplicateValuesMessage);
             documentMessage.append(totalImport + " entries in total imported ");
        }
        return new ImportDataResults(matchedDocs.size(), documentMessage.toString());
    }

    @Transactional
    private SavedFiltersFolder importDataToFolder(final int folderId, final Set<String> docs, final String loginUser, DocImportType docImportType, String searchView) throws Exception {
       
	    	final Session session = sessionFactory.getCurrentSession();
	        final SavedFiltersFolder folder = getFolder(folderId, session, loginUser);
	        try {
		        if (folder == null) {
		            throw new IllegalArgumentException("Folder id [" + folderId + "] not found.");
		        }
		        String importType=null;
		        for(final Map.Entry<String, DocImportType> entry : getDocImportTypes(searchView).entrySet()) {
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
		       // if (!docs.isEmpty())  {
		            // add the tags
		        	folder.getRefs().clear();
		            folder.getRefs().addAll(docs);
		            folder.setDocCount(folder.getRefs().size());
		            session.update(folder);
		       // }
	        } catch(Exception e) {
	        	//LOGGER.error(e.getMessage());
	        	throw new Exception(e);
	        }
        return folder;
    }
    
    public List<String> formatImportData(List<String> importedList){
    	
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

    private ReferenceResultsIterator getDocReferences(final String idolField,
                                                      final String searchView,
                                                      final Set<String> fieldValues,
                                                      final String securityInfo) {

        if (fieldValues == null || fieldValues.isEmpty()) {
            return null;
        }

        final Map<String, FilterField> filterFields = parametricService.getFilterFieldNames(searchView);
        final FilterField importField = filterFields.get(idolField);
        final boolean isReferenceType = importField != null && importField.isReferenceType();

        final AciParameters params = new AciParameters("query");
        params.add("databasematch", searchConfig.getDatabase(searchView));
        params.add("text", "*");
        params.add("combine", "simple");
        params.add("maxresults", fieldValues.size());
        params.add("storestate", true);
        params.add("print", "noresults");

        if(StringUtils.isNotBlank(securityInfo)) {
            params.add("securityinfo", securityInfo);
        }

        if(isReferenceType) {
            params.add("matchreference", ReferencesBuilder.from(fieldValues));
            params.add("referencefield", idolField);
        } else {
            params.add("fieldtext", FieldTextUtil.buildFieldMatch(idolField, fieldValues, importField));
        }

        final StateResult stateResult = searchAciService.executeAction(params, new StoreStateProcessor());

        return new ReferenceResultsIterator(stateResult,
                                            searchAciService,
                                            searchConfig,
                                            searchView,
                                            securityInfo);


    }
    
    private com.autonomy.vertica.query.QueryResponse getDocReferencesVertica(final String idolField,
            final String searchView,
            final List<String> fieldValues,
            final String securityInfo) throws Exception {

		if (fieldValues == null || fieldValues.isEmpty()) {
			return null;
		}
		
		final Map<String, FilterField> filterFields = parametricService.getFilterFieldNames(searchView);
		final FilterField importField = filterFields.get(idolField);
		final boolean isReferenceType = importField != null && importField.isReferenceType();
		
		final AciParameters params = new AciParameters("query");
		params.add("databasematch", searchConfig.getDatabase(searchView));
		params.add("text", "*");
		params.add("combine", "simple");
		params.add("maxresults", fieldValues.size() + 1);
		params.add("storestate", true);
		params.add("print", "noresults");
		
		if(StringUtils.isNotBlank(securityInfo)) {
			params.add("securityinfo", securityInfo);
		}
		
		if(isReferenceType) {
			params.add("matchreference", ReferencesBuilder.from(fieldValues));
			params.add("referencefield", idolField);
		} else {
			Set<String> importedValues = new HashSet<String>(); 
			importedValues.addAll(fieldValues);
			params.add("fieldtext", FieldTextUtil.buildFieldMatch(idolField, importedValues, importField));
		}
		
		params.add("printfields", idolField);
		
		 FilterGroup group = new FilterGroup();
	        Map<String, List<FieldParams>> paraFilters = new HashMap<>();
	        List<FieldParams> listParams = new ArrayList<FieldParams>();        
	        FieldParams fieldParam = new FieldParams();
	        fieldParam.setOp(FilterOperator.IS);
	        char type = importField.getParametric() == null ? 'I' : 'P';
	        fieldParam.setType(type);
	        fieldParam.setListVal(fieldValues);
	        listParams.add(fieldParam);
	        paraFilters.put(idolField, listParams);
	        group.setFilterFields(paraFilters);
	        group.setBoolOperator(BoolOp.AND);
	       
		
		com.autonomy.vertica.query.QueryResponse response = verticaService.searchResultsDoc(params, searchView, null, group, true, null, null, null);
		return response;
	}

    private PrintFieldsResultIterator getFolderExportFields(final String docRefFieldname,
                                         final int maxResults,
                                         final String searchView,
                                         final Set<String> docRefIds,
                                         final List<String> printFields,
                                         final String securityInfo)
    {
        final StringBuilder strBuilder = new StringBuilder();

        for(final String pfield : printFields) {
            strBuilder.append(pfield).append(',');
        }

        final String printFieldsStr = StringUtils.chop(strBuilder.toString());

        final Map<String, FilterField> filterFields = parametricService.getFilterFieldNames(searchView);
        final FilterField docField = filterFields.get(docRefFieldname);
        final boolean isReferenceType = docField != null && docField.isReferenceType();

        final AciParameters params = new AciParameters("query");
      		params.add("databasematch", searchConfig.getDatabase(searchView));
            params.add("text", "*");
            params.add("combine", "simple");
      		params.add("maxresults", maxResults);
            params.add("storestate", true);
            params.add("print", "noresults");

            if(StringUtils.isNotBlank(securityInfo)) {
                params.add("securityinfo", securityInfo);
            }

            if(isReferenceType) {
                params.add("matchreference", ReferencesBuilder.from(docRefIds));
                params.add("referencefield", docRefFieldname);

            } else {
                params.add("fieldtext", FieldTextUtil.buildFieldMatch(docRefFieldname, docRefIds, docField));
            }

            final StateResult stateResult = searchAciService.executeAction(params, new StoreStateProcessor());

            return new PrintFieldsResultIterator(stateResult,
                                                 searchAciService,
                                                 searchConfig,
                                                 searchView,
                                                 printFieldsStr,
                                                 securityInfo);

    }

    private com.autonomy.vertica.query.QueryResponse getFolderExportFieldsVertica(final DocExportData docExportData,
    		final String docRefFieldname,
            final int maxResults,
            final String searchView,
            final Set<String> docRefIds,
            final List<String> printFields,
            final String securityInfo
            ) throws Exception
		{
			final StringBuilder strBuilder = new StringBuilder();
			
			for(final String pfield : printFields) {
				strBuilder.append(pfield).append(',');
			}
			
			final String printFieldsStr = StringUtils.chop(strBuilder.toString());
			        
			
			final Map<String, FilterField> filterFields = parametricService.getFilterFieldNames(searchView);
			final FilterField docField = filterFields.get(docRefFieldname);
			final boolean isReferenceType = docField != null && docField.isReferenceType();
			
			final AciParameters params = new AciParameters("query");
			params.add("databasematch", searchConfig.getDatabase(searchView));
			params.add("text", "*");
			params.add("combine", "simple");
			params.add("maxresults", maxResults);
			params.add("storestate", true);
			params.add("print", "noresults");
			
			if(StringUtils.isNotBlank(securityInfo)) {
				params.add("securityinfo", securityInfo);
			}
			
			if(isReferenceType) {
				params.add("matchreference", ReferencesBuilder.from(docRefIds));
				params.add("referencefield", docRefFieldname);			
			} else {
				params.add("fieldtext", FieldTextUtil.buildFieldMatch(docRefFieldname, docRefIds, docField));
			}
			
			params.add("printfields", printFieldsStr);
	        
	        final SearchRequestData requestData = new SearchRequestData();
	        requestData.setSearchView(searchView);
	        String docFieldName = "";
	        for(final Map.Entry<String, FilterField> filterFieldColl : filterFields.entrySet()) {
	        	if(filterFieldColl.getValue().getCustom() != null 
	        			&& filterFieldColl.getValue().getCustom().getMappedField().equalsIgnoreCase(docRefFieldname)) {
	        		docFieldName = filterFieldColl.getKey();
	        	}
	        }        
	        FilterGroup group = new FilterGroup();
	        Map<String, List<FieldParams>> paraFilters = new HashMap<>();
	        List<FieldParams> listParams = new ArrayList<FieldParams>();        
	        FieldParams fieldParam = new FieldParams();
	        fieldParam.setOp(FilterOperator.IS);
	        fieldParam.setType('C');
	        fieldParam.setVal(String.valueOf(docExportData.getExportDocFolderId()));
	        listParams.add(fieldParam);
	        paraFilters.put(docFieldName, listParams);
	        group.setFilterFields(paraFilters);
	        group.setBoolOperator(BoolOp.AND);
	        requestData.setFilterGroup(group);
	             
	        searchService.addSearchParamsVertica(params, requestData, String.valueOf(maxResults));
	        
	        requestData.setRetrieveResultDocs(true);
	        
	        final com.autonomy.vertica.query.QueryResponse response = verticaService.searchResultsDoc(params, requestData.getSearchView(),requestData.getUserSearchSettings(), requestData.getFilterGroup(), requestData.isRetrieveResultDocs(), null, null, null);
	        return response;		
		}
    
	    @Transactional(readOnly = true)
		public Set<SavedFiltersFolder> getFolders(final String searchView, final String loginUser){
			final Session session = sessionFactory.getCurrentSession();
			Set<String> userRoles = adminService.getUsersRoles(loginUser);
			Set<SavedFiltersFolder> folders = new HashSet<SavedFiltersFolder>();
			for(String role : userRoles){
				final Query query = session.getNamedQuery(SavedFiltersFolder.GET_FOLDERS);
				query.setParameter("searchView", searchView);
				query.setParameter("owner", loginUser);
				query.setParameter("role", role);
				folders.addAll((List<SavedFiltersFolder>) query.list());
			}
			return folders;
		}
    
    	@Transactional(readOnly = true)
    	private Set<SavedFiltersFolder> getDocumentList(final String searchView, final String loginUser) {
    		final Session session = sessionFactory.getCurrentSession();
    		Set<String> userRoles = adminService.getUsersRoles(loginUser);
    		Set<String> listTypes = getDocImportTypes(searchView).keySet();
    		Set<SavedFiltersFolder> documentLists=new HashSet<SavedFiltersFolder>();
    		
    		for(String role : userRoles){
    			final Query query = session.getNamedQuery(SavedFiltersFolder.GET_LISTS);
    			query.setParameterList("folderTypeList_new", listTypes);
        		query.setParameter("searchView", searchView);
                query.setParameter("owner", loginUser);
                query.setParameter("role", role);
                
                documentLists.addAll((List<SavedFiltersFolder>) query.list());
    		}
    		
    		return documentLists;
		}
    	
    	@Transactional(readOnly = true)
    	public Set<SavedFilter> getCustomFilters(final String searchView, final String loginUser){
    		final Session session = sessionFactory.getCurrentSession();
    		Set<String> userRoles = adminService.getUsersRoles(loginUser);
    		Set<SavedFilter> folderFilters = new HashSet<SavedFilter>();
    		
    		for(String role : userRoles){
    			final Query query = session.getNamedQuery("SavedFilter.getCustomFilters");
        		query.setParameter("searchView", searchView);
        		query.setParameter("owner", loginUser);
        		query.setParameter("role", role);
        		
        		folderFilters.addAll((List<SavedFilter>)query.list());
    		}
 
    		return folderFilters;
    	}
    	
    	
    	@Transactional(readOnly = true)
    	public DocumentFolderData getTreeData(final String searchView, final String loginUser){
    		Set<SavedFiltersFolder> folders =new HashSet<SavedFiltersFolder>();
        	Set<SavedFiltersFolder> Lists = getDocumentList(searchView, loginUser);
        	Set<SavedFilter> customFilter = getCustomFilters(searchView, loginUser);
        	
        	Set<SavedFiltersFolder> immediateParents = new HashSet<SavedFiltersFolder>();
        	for(SavedFiltersFolder documentList : Lists){
        		immediateParents.add(documentList.getParent());
        	}
        	for(SavedFilter filter : customFilter){
        		immediateParents.add(filter.getParent());
        	}
        	
        	if(filterService.checkRootFolder()!=null){
        		folders.add(filterService.getRootFolder());
        	}
        	
        	//user folders and folders that has been shared with user roles
        	Set<SavedFiltersFolder> userViewFolders = getFolders(searchView, loginUser);
        	
        	for(SavedFiltersFolder viewfolder : userViewFolders){
        		folders.add(viewfolder);
        		recursiveFunction(folders, viewfolder);
        	}
        	
        	for(SavedFiltersFolder immediateParent : immediateParents){
        		folders.add(immediateParent);
        		recursiveFunction(folders, immediateParent);
        	}
        	
        	return(new DocumentFolderData(folders, Lists, customFilter));
    	}
    	
    	private void recursiveFunction(Set<SavedFiltersFolder> folders,SavedFiltersFolder folder){
    		if(folder.getParent().getName()==filterService.getRootFolder().getName()){
    			return;
    		}
    		folders.add(folder.getParent());
    		recursiveFunction(folders, folder.getParent());
    	}
    	
    	public void checkPermissionFilter(final SavedFilter filter,final String loginUser){
    		if(filter.getReadOnly() && !loginUser.equals(filter.getOwner())){
    			throw new IllegalArgumentException(loginUser +" has read only permission for "+filter.getName());
    		}
    	}
    	
    	public void checkPermissionFolder(final SavedFiltersFolder folder, final String loginUser, final String operation){
    		if(folder.getReadOnly() && !loginUser.equals(folder.getOwner())){
    			throw new IllegalArgumentException(loginUser +" has read only permission for "+folder.getName());
    		}
    		
    		if(!folder.getOwner().equals("system") && folder.getRoles().isEmpty() && !loginUser.equals(folder.getOwner())){
            	throw new IllegalArgumentException(loginUser+" has no "+ operation +" permission for "+folder.getName());
            }
    	}
    	
    	@Transactional(readOnly = false)
    	public SavedFiltersFolder cutPaste(final String sourceId, final String destId, final String childIds, final String loginUser){
    		final Session session = sessionFactory.getCurrentSession();
    		final SavedFiltersFolder  destFolder = getFolder(Integer.valueOf(destId), session, loginUser);
    		final List<String> listChildIds = new Gson().fromJson(childIds, List.class );
    		checkPermissionFolder(destFolder, loginUser, "cut/paste");
    		if(sourceId.contains("filter")){
    			final SavedFilter filter = (SavedFilter) session.get(SavedFilter.class, Integer.valueOf(sourceId.replace("filter", "")));
    			checkPermissionFilter(filter,loginUser);
    			if(filterService.getFilterByLabel(filter.getSearchView(), destFolder.getId(), filter.getName(), loginUser, session)!=null){
    	    		throw new IllegalArgumentException("Filter name already in use");
    	    	}
    			filter.setParent(destFolder);
    			filter.setModifiedDate(new Date());
    			filter.getRoles().clear();
    			filter.setReadOnly(false);
    			filter.setOwner(loginUser);
    			session.update(filter);
    		}
    		else{
    			final SavedFiltersFolder sourceFolder = getFolder(Integer.valueOf(sourceId), session, loginUser);
    			checkPermissionFolder(sourceFolder, loginUser, "cut/paste");
    			checkDuplicate(sourceFolder.getFolderType(), sourceFolder.getSearchView(),  destFolder.getId(), sourceFolder.getName(), loginUser, session);
    			/*for(String childId : listChildIds){
        			if(childId.contains("filter")){
        				final SavedFilter childFilter = (SavedFilter) session.get(SavedFilter.class, Integer.valueOf(childId.replace("filter", "")));
        				checkPermissionFilter(childFilter, loginUser);
        			}
        			else{
        				final SavedFiltersFolder childFolder = getFolder(Integer.valueOf(childId), session, loginUser);
        				checkPermissionFolder(childFolder, loginUser);
        			}
        		}*/
    			sourceFolder.setParent(destFolder);
    			String path = destFolder.getFullPath() +  SavedFiltersFolder.PATH_SEPARATOR + sourceFolder.getName();
    			sourceFolder.setFullPath(path);
    			sourceFolder.setModifiedDate(new Date());
    			sourceFolder.getRoles().clear();
    			sourceFolder.setReadOnly(false);
    			sourceFolder.setOwner(loginUser);
    			for(String nodeId : listChildIds){
    				if(!nodeId.contains("filter")){
    					final SavedFiltersFolder childFolder = getFolder(Integer.valueOf(nodeId), session, loginUser);
    					checkPermissionFolder(childFolder, loginUser,"cut/paste");
    					final String childPath = childFolder.getParent().getFullPath() + SavedFiltersFolder.PATH_SEPARATOR + childFolder.getName();
    					childFolder.setFullPath(childPath);
    					childFolder.setModifiedDate(new Date());
    					childFolder.getRoles().clear();
    					childFolder.setReadOnly(false);
    					childFolder.setOwner(loginUser);
    				}
    				else{
    					final SavedFilter childFilter = (SavedFilter) session.get(SavedFilter.class, Integer.valueOf(nodeId.replace("filter", "")));
    					checkPermissionFilter(childFilter, loginUser);
    					Set<String> difference = Sets.symmetricDifference(destFolder.getRoles(), childFilter.getRoles());
    					childFilter.setModifiedDate(new Date());
    					childFilter.getRoles().clear();
    					childFilter.setReadOnly(false);
    					childFilter.setOwner(loginUser);
    				}
    			}
    			session.update(sourceFolder);
    		}
    		return destFolder;
    	}
    	
    	@Transactional(readOnly = false)
    	private void copyCustomFilter(final String childIds, final Map<Integer, Integer> oldNewIdsMap, final Session session, final String loginUser){
    		final List<String> listChildIds = new Gson().fromJson(childIds, List.class );
    		final List<String> customFilterIds=new ArrayList<String>();
    		for(String id: listChildIds){
    			if(id.contains("filter")){
    				customFilterIds.add(id);
    			}
    		}
    		
    		for(String id: customFilterIds){
    			final SavedFilter copiedFilter = (SavedFilter) session.get(SavedFilter.class, Integer.valueOf(id.replace("filter", "")));
    			final SavedFilter filter = filterService.saveFilters(copiedFilter.getName(), 
    					copiedFilter.getDescription(), 
    					copiedFilter.getFiltersText(), 
    					oldNewIdsMap.get(copiedFilter.getParent().getId()), 
    					copiedFilter.getSearchView(), 
    					loginUser);
    			//filter.getRoles().addAll(copiedFilter.getRoles());
    		}
    	}
    	
    	@Transactional(readOnly = false)
    	public SavedFiltersFolder copyPaste(final String sourceId, final String destId, final String childIds, final String loginUser){
    		final Session session = sessionFactory.getCurrentSession();
    		final SavedFiltersFolder destFolder = getFolder(Integer.valueOf(destId), session, loginUser);
    		checkPermissionFolder(destFolder, loginUser, "copy/paste");
    		if(sourceId.contains("filter")){
    			final SavedFilter copiedFilter = (SavedFilter) session.get(SavedFilter.class, Integer.valueOf(sourceId.replace("filter", "")));
    			final SavedFilter filter = filterService.saveFilters(copiedFilter.getName(), 
    					copiedFilter.getDescription(), 
    					copiedFilter.getFiltersText(), 
    					destFolder.getId(), 
    					copiedFilter.getSearchView(), 
    					loginUser);
    			//filter.getRoles().addAll(copiedFilter.getRoles());
    		}
    		else{
    			final SavedFiltersFolder sourceFolder = getFolder(Integer.valueOf(sourceId), session, loginUser);
    		
    			Map<Integer, Integer> childParentMapTest = new LinkedHashMap<Integer, Integer>();
    			childParentMapTest.put(Integer.valueOf(sourceId), sourceFolder.getParent().getId());
    			//create a child-parent map
    			createChildParentMap(Integer.valueOf(sourceId), childParentMapTest);
    		
    			//map for oldId and newId
    			Map<Integer, Integer> oldNewIdsMap = new HashMap<Integer, Integer>();
    		
    			for(Map.Entry<Integer, Integer> entry : childParentMapTest.entrySet()){
    				Integer childId = entry.getKey();
    				Integer parentId = entry.getValue();
    				final SavedFiltersFolder oldFolder = getFolder(childId, session, loginUser);
    				Integer newParentId = childId.equals(Integer.valueOf(sourceId))?destFolder.getId():oldNewIdsMap.get(parentId);
    				
    				final String copiedFolderType = (oldFolder.getFolderType()==null)? "" : oldFolder.getFolderType();
    				
    				SavedFiltersFolder newChildFolder = add(oldFolder.getName(), newParentId, loginUser, oldFolder.getTooltip(), oldFolder.getSearchView(), copiedFolderType, Collections.emptyList().toString(), false);
			
    				if(newChildFolder.getFolderType()!=null){
    					newChildFolder.getRefs().addAll(oldFolder.getRefs());
    					newChildFolder.setDocCount(oldFolder.getRefs().size());
    				}
    				oldNewIdsMap.put(childId, newChildFolder.getId());	
    			}
    			copyCustomFilter(childIds, oldNewIdsMap, session, loginUser);
    			}
    			session.update(destFolder);
    			return destFolder;
    	}
    	
    	
    	private void createChildParentMap(Integer sourceId, Map<Integer, Integer> childParentMap){
    		final Session session = sessionFactory.getCurrentSession();
    		final Query foldersQuery = session.getNamedQuery("SavedFiltersFolder.getChildFolders");
            foldersQuery.setInteger("parentFolderId", sourceId);
            final List<SavedFiltersFolder> childFolders = foldersQuery.list(); 
            
            if(!childFolders.isEmpty()){
            	for(SavedFiltersFolder child: childFolders){
                	childParentMap.put(child.getId(), sourceId);
                	createChildParentMap(child.getId(), childParentMap);
                }
            }
    	}
    	
    	@Transactional( readOnly = false)
    	public HashSet<String> getFolderRoles(final Integer folderId, final String loginUser){
    		final Session session = sessionFactory.getCurrentSession();
    		final SavedFiltersFolder folder = getFolder(folderId, session, loginUser);
    		return new HashSet<String>(folder.getRoles());
    	}
 }
