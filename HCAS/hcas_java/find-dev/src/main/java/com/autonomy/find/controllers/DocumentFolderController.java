package com.autonomy.find.controllers;

import java.util.ArrayList;
import java.util.Collections;

/*
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/controllers/DocumentFolderController.java#18 $
 *
 * Copyright (c) 2013, Autonomy Systems Ltd.
 *
 * Last modified by $Author: wo $ on $Date: 2014/05/13 $ 
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.autonomy.find.api.database.DocumentFolder;
import com.autonomy.find.api.database.SavedFilter;
import com.autonomy.find.api.database.SavedFiltersFolder;
import com.autonomy.find.config.SearchConfig;
import com.autonomy.find.dto.DocumentFolderData;
import com.autonomy.find.dto.ImportDataResults;
import com.autonomy.find.dto.Parametric.DocExportData;
import com.autonomy.find.dto.Parametric.DocImportData;
import com.autonomy.find.dto.Parametric.DocImportType;
import com.autonomy.find.services.DocumentFolderService;
import com.autonomy.find.services.DocumentVerticaFolderService;
import com.autonomy.find.services.admin.AdminService;
import com.autonomy.find.util.SessionHelper;
import com.autonomy.find.util.audit.AuditActions;
import com.autonomy.find.util.audit.AuditLogger;

@Controller
@RequestMapping("/p/ajax/documentfolder")
public class DocumentFolderController {

    private static final Logger LOGGER = LoggerFactory.getLogger(DocumentFolderController.class);

    private static final String SELECTED_SESSION_KEY = DocumentFolder.class.getCanonicalName() + "-selected";

    @Autowired
    private DocumentFolderService documentFolderService;
    
    @Autowired
    private DocumentVerticaFolderService documentVerticaFolderService;

    @Autowired
    private SessionHelper sessionHelper;
    
    @Autowired
 	private SearchConfig searchConfig;
    
    @Autowired
    private AdminService adminService;
        
    private static final String SOURCEDB = "vertica";


    @RequestMapping(value = {"/addFolder.json"}, method = RequestMethod.POST)
    @ResponseBody
    public SavedFiltersFolder add(
            @RequestParam(value = "tooltip", required = false) final String tooltip,
            @RequestParam("searchView") final String searchView,
            @RequestParam("label") final String name,
            @RequestParam("isReadOnly") final boolean readOnly,
            @RequestParam("folderType") final String folderType,
            @RequestParam("parentId") final Integer parentId,
            @RequestParam("folderRoles") final String roles,
            final HttpSession session
    ) {
        final String loginUser = sessionHelper.getSessionUser(session);
        SavedFiltersFolder folder = null;
        
       /* if(StringUtils.isNotEmpty(searchConfig.getSearchViews().get(searchView).getDocFolderSourceDB()) 
        		&& searchConfig.getSearchViews().get(searchView).getDocFolderSourceDB().equalsIgnoreCase(SOURCEDB))
        {
        	 folder = documentVerticaFolderService.add(name, parentId, loginUser, tooltip, searchView, folderType, roles, readOnly);
        }
        else // Postgres db document fetch for vertica and Idol search view
        {*/
        	folder = documentFolderService.add(name, parentId, loginUser, tooltip, searchView, folderType, roles, readOnly);
        	 
        //}
        AuditLogger.log(loginUser, AuditActions.DOCFOLDER_CREATE, folder);

        return folder;
    }

    @RequestMapping(value = {"/tag.json"}, method = RequestMethod.POST)
    @ResponseBody
    public Boolean tag(
            @RequestParam("id") final int id,
            @RequestParam("ref") final String[] ref,
            final HttpSession session
    ) {
        final String loginUser = sessionHelper.getSessionUser(session);

        final Map<String, Object> auditData = AuditLogger.getDataMap();
        auditData.put("folderId", id);
        auditData.put("ref", ref);

        AuditLogger.log(loginUser, AuditActions.DOCFOLDER_TAG, auditData);

        return documentFolderService.tag(loginUser, id, false, ref);
    }
    
    @RequestMapping(value = {"/istag.json"}, method = RequestMethod.POST)
    @ResponseBody
    public Boolean isTagged(
            @RequestParam("id") final int id,
            @RequestParam("ref") final String ref,
            final HttpSession session
    ) {
       return documentFolderService.isRefInFolder(id, ref);
    }

    @RequestMapping(value = {"/untag.json"}, method = RequestMethod.POST)
    @ResponseBody
    public Boolean untag(
            @RequestParam("id") final int id,
            @RequestParam("ref") final String[] ref,
            final HttpSession session
    ) {
        final String loginUser = sessionHelper.getSessionUser(session);

        final Map<String, Object> auditData = AuditLogger.getDataMap();
        auditData.put("folderId", id);
        auditData.put("ref", ref);

        AuditLogger.log(loginUser, AuditActions.DOCFOLDER_UNTAG, auditData);

        return documentFolderService.untag(loginUser, id, false, ref);
    }

    @RequestMapping(value = {"/getTags.json"}, method = RequestMethod.POST)
    @ResponseBody
    public Set<String> getTags(
            @RequestParam("id") final int id,
            final HttpSession session
    ) {
        return documentFolderService.getTags(id, sessionHelper.getSessionUser(session));
    }

    @RequestMapping(value = {"/editFolder.json"}, method = RequestMethod.POST)
    @ResponseBody
    public SavedFiltersFolder edit(
            @RequestParam("id") final int id,            
            @RequestParam(value = "label", required = false) final String label,
            @RequestParam(value = "searchView") final String searchView,
            @RequestParam(value = "tooltip", required = false) final String tooltip,
            @RequestParam(value = "readOnly", required = false) final Boolean readOnly,
            @RequestParam(value = "roles") final String roles,
            @RequestParam(value = "importedValues") final String importedValues,
            @RequestParam(value = "childNodeIds") final String ChildIds,
            @RequestParam(value = "dataDifference") final boolean dataDifference,
            final HttpSession session
    ) {

        final String loginUser = sessionHelper.getSessionUser(session);

        final Map<String, Object> auditData = AuditLogger.getDataMap();
        auditData.put("folderId", id);
        auditData.put("tooltip", tooltip);
        auditData.put("label", label);
        auditData.put("Share with", roles);
        auditData.put("importedValuesList", importedValues);
        
        AuditLogger.log(loginUser, AuditActions.DOCFOLDER_EDIT, auditData);
       /* if(StringUtils.isNotEmpty(searchConfig.getSearchViews().get(searchView).getDocFolderSourceDB()) 
        		&& searchConfig.getSearchViews().get(searchView).getDocFolderSourceDB().equalsIgnoreCase(SOURCEDB))
        {
        	return documentVerticaFolderService.edit(id, tooltip, label, loginUser, isShared, searchView, importedValues);
        }//vertica
        else // Postgres db document fetch for vertica and Idol search view
        {*/
        	return documentFolderService.edit(id, tooltip, label, loginUser, readOnly, roles, importedValues, ChildIds, dataDifference);
        //}
    }
    
    @RequestMapping(value = {"/getImportedValues.json"}, method = RequestMethod.POST)
    @ResponseBody
    public List<String> editImportedValues(@RequestParam("id") final int id,
    		@RequestParam(value = "searchView") final String searchView,
    		@RequestParam(value = "owner") final String owner,
    		final HttpSession session){
    	
    	 final String loginUser = sessionHelper.getSessionUser(session);
    	 //StringBuffer returnString = new StringBuffer();
    	 List<String> importedList = new ArrayList<String>();
    	 
    	 if(StringUtils.isNotEmpty(searchConfig.getSearchViews().get(searchView).getDocFolderSourceDB()) 
         		&& searchConfig.getSearchViews().get(searchView).getDocFolderSourceDB().equalsIgnoreCase(SOURCEDB)){
    		 SavedFiltersFolder importedData = documentVerticaFolderService.getTags(id, loginUser, searchView);
    		 
    		 for(String value:importedData.getRefs())
    			 importedList.add(value);
    		 
    		 return (documentVerticaFolderService.formatImportDataVertica(importedList));
    	 }//fetch values from vertica
    	 else{
    		 for(String value: documentFolderService.getTags(id, owner))
    			 importedList.add(value);
    		 
    		 return (documentFolderService.formatImportData(importedList));
    	 }//fetch values postgres
    }
    
    @RequestMapping(value = {"/deleteFolder.json"}, method = RequestMethod.POST)
    @ResponseBody
    public void delete(
        @RequestParam(value="ids[]") final List<String> listIds,
        @RequestParam(value="searchView") final String searchView,
        final HttpSession session
    ) {
        final String loginUser = sessionHelper.getSessionUser(session);

        final Map<String, Object> auditData = AuditLogger.getDataMap();
        /*
        List<Integer> integerListIds=new ArrayList<Integer>();
    	for(String id:listIds)
    		integerListIds.add(Integer.valueOf(id));*/

        AuditLogger.log(loginUser, AuditActions.DOCFOLDER_DELETE, auditData);
        if(StringUtils.isNotEmpty(searchConfig.getSearchViews().get(searchView).getDocFolderSourceDB()) 
        		&& searchConfig.getSearchViews().get(searchView).getDocFolderSourceDB().equalsIgnoreCase(SOURCEDB))
        {
        	documentVerticaFolderService.delete(listIds, loginUser, searchView);
        }
        else // Postgres db document fetch for vertica and Idol search view
        {
        	documentFolderService.delete(listIds, loginUser);
        }
    }

    @RequestMapping(value = "/setSelected", method = RequestMethod.POST)
    @ResponseBody
    public void setSelectedFolders(
        @RequestParam("searchView") final String searchView,
        @RequestBody final Set<Integer> ids,
        final HttpSession session
    ) {
        getSelectionMap(session).put(searchView, ids);
    }

    public Map<String, Set<Integer>> getSelectionMap(final HttpSession session) {
        //noinspection unchecked
        Map<String, Set<Integer>> map = (Map<String, Set<Integer>>) session.getAttribute(SELECTED_SESSION_KEY);

        if (map != null) {
            return map;
        }

        map = new HashMap<>();
        session.setAttribute(SELECTED_SESSION_KEY, map);

        return map;
    }

    @RequestMapping(value = {"/getImportTypes.json"}, method = RequestMethod.POST)
    @ResponseBody
    public Map<String, DocImportType> getImportTypes(
            @RequestParam("searchView") final String searchView
    ) {
        return documentFolderService.getDocImportTypes(searchView);
    }

    @RequestMapping(value = {"/docImportData.json"}, method = RequestMethod.POST)
    @ResponseBody
    public ImportDataResults docImportData(
            @RequestBody final DocImportData docImportData,
            final HttpSession session
    ) {
    	try {
    		ImportDataResults data;
	    	if(searchConfig.getSearchViews().get(docImportData.getSearchView()).getRepository().equalsIgnoreCase("IDOL")) {
	    		data= documentFolderService.importData(docImportData, sessionHelper.getSessionUser(session), sessionHelper.getUserSecurityInfo(session));
	    		return data;
	    	} else {
	    		boolean isDocFolderInVertica = false;
	    		if(StringUtils.isNotEmpty(searchConfig.getSearchViews().get(docImportData.getSearchView()).getDocFolderSourceDB()) 
		        		&& searchConfig.getSearchViews().get(docImportData.getSearchView()).getDocFolderSourceDB().equalsIgnoreCase(SOURCEDB)) {
	    			isDocFolderInVertica = true;
	    		}
	    		data= documentFolderService.importDataVertica(docImportData, sessionHelper.getSessionUser(session), sessionHelper.getUserSecurityInfo(session), isDocFolderInVertica);
	    		return data;
	    	}
    	} catch(Exception e) {
    		LOGGER.error("Doc Import Error: " + e.getMessage());
    		return new ImportDataResults(-1, " Error in processing this request. Please contact the administrator or helpdesk!! ");
    	}
        
    }
    
    @RequestMapping(value = {"/exportDocFolder.do"}, method = RequestMethod.POST, consumes = "application/x-www-form-urlencoded")
    public ModelAndView exportDocFolder(
            @ModelAttribute final DocExportData docExportData,
            final HttpServletResponse response,
            final HttpSession session,
            final RedirectAttributes attrs
    ) {
        final String filename = docExportData.getExportFilename();
        response.setHeader("Content-Disposition", "attachment;filename=\"" + filename + "\"" );
        response.setContentType("text/*");
        try {
        	if(searchConfig.getSearchViews().get(docExportData.getSearchView()).getRepository().equalsIgnoreCase("IDOL")) {
        		documentFolderService.exportDocFolder(docExportData, sessionHelper.getSessionUser(session), response.getWriter(), sessionHelper.getUserSecurityInfo(session));
        	} else {
        		if(StringUtils.isNotEmpty(searchConfig.getSearchViews().get(docExportData.getSearchView()).getDocFolderSourceDB()) 
    	        		&& searchConfig.getSearchViews().get(docExportData.getSearchView()).getDocFolderSourceDB().equalsIgnoreCase(SOURCEDB)) {
        			documentFolderService.exportDocFolderVerticaNew(docExportData, sessionHelper.getSessionUser(session), response.getWriter(), sessionHelper.getUserSecurityInfo(session));
        		} else {
        			documentFolderService.exportDocFolderVertica(docExportData, sessionHelper.getSessionUser(session), response.getWriter(), sessionHelper.getUserSecurityInfo(session));
        		}
        		//documentFolderService.exportDocFolderVertica(docExportData, sessionHelper.getSessionUser(session), response.getWriter(), sessionHelper.getUserSecurityInfo(session));
        		
        	}

            return null;
        } catch (Exception ex) {
            LOGGER.error("Failed export doc folder.", ex);

            attrs.addFlashAttribute("exception", ex);
            return new ModelAndView("redirect:/p/error.do");
        }

    }

    @RequestMapping(value = {"/exportResults.do"}, method = RequestMethod.POST, consumes = "application/x-www-form-urlencoded")
    public ModelAndView exportResults(
            @ModelAttribute final DocExportData docExportData,
            final HttpServletResponse response,
            final HttpSession session,
            final RedirectAttributes attrs
    ) {
        final String filename = docExportData.getExportFilename();
        response.setContentType("text/*");
        response.setHeader("Content-Disposition", "attachment;filename=\"" + filename + "\"" );

        try {
        	 if(searchConfig.getSearchViews().get(docExportData.getSearchView()).getRepository().equalsIgnoreCase("IDOL")) {
        		 documentFolderService.exportResults(docExportData, sessionHelper.getSessionUser(session), response.getWriter(), sessionHelper.getUserSecurityInfo(session));
        	 } else {
        		 documentFolderService.exportResultsVertica(docExportData, sessionHelper.getSessionUser(session), response.getWriter(), sessionHelper.getUserSecurityInfo(session));
        	 }

            return null;

        } catch (Exception ex) {
            LOGGER.error("Failed export results.", ex);

            attrs.addFlashAttribute("exception", ex);
            return new ModelAndView("redirect:/p/error.do");
        }

    }

    @RequestMapping(value = {"/tagResults.json"}, method = RequestMethod.POST)
    @ResponseBody
    public Boolean tagResults(
            @RequestBody final DocExportData docExportData,
            final HttpSession session
    ) throws Exception {
    	if(searchConfig.getSearchViews().get(docExportData.getSearchView()).getRepository().equalsIgnoreCase("IDOL")) {
    		return documentFolderService.tagResults(docExportData, sessionHelper.getSessionUser(session), sessionHelper.getUserSecurityInfo(session));
    	} else {
    		if(StringUtils.isNotEmpty(searchConfig.getSearchViews().get(docExportData.getSearchView()).getDocFolderSourceDB()) 
	        		&& searchConfig.getSearchViews().get(docExportData.getSearchView()).getDocFolderSourceDB().equalsIgnoreCase(SOURCEDB)) {
    			return documentVerticaFolderService.tagResultsVertica(docExportData, sessionHelper.getSessionUser(session), sessionHelper.getUserSecurityInfo(session));
    		} else {
    			return documentFolderService.tagResultsVertica(docExportData, sessionHelper.getSessionUser(session), sessionHelper.getUserSecurityInfo(session));
    		}
    		//return documentFolderService.tagResultsVertica(docExportData, sessionHelper.getSessionUser(session), sessionHelper.getUserSecurityInfo(session));
    		
    	}
    }
    
    @RequestMapping(value = {"/getFoldersdata.json"}, method = RequestMethod.POST)
    @ResponseBody
    public DocumentFolderData getFolderData(@RequestParam("searchView") final String searchView, final HttpSession session){  
    	DocumentFolderData folderData = null;
    	try{
    		/*if(StringUtils.isNotEmpty(searchConfig.getSearchViews().get(searchView).getDocFolderSourceDB()) 
	        		&& searchConfig.getSearchViews().get(searchView).getDocFolderSourceDB().equalsIgnoreCase(SOURCEDB))
			{
    			folderData = documentVerticaFolderService.getTreeData(searchView, sessionHelper.getSessionUser(session));
			} 
			else // Postgres db document fetch for vertica and Idol search view
			{*/
				folderData = documentFolderService.getTreeData(searchView, sessionHelper.getSessionUser(session));
			//}
    	}
    	catch(Exception e){
    		LOGGER.error(e.getMessage());
    	}
    	return folderData;
    }
    
    
    @RequestMapping(value = {"/ccpDocFolder.json"}, method = RequestMethod.POST)
    @ResponseBody
    public SavedFiltersFolder pasteDocFolder(
    		@RequestParam(value="sourceId") final String sourceNodeId,
    		@RequestParam(value="destId") final String destNodeId, 
    		@RequestParam(value="mode") final String ccp_mode,
    		@RequestParam(value="childNodeIds") final String childrenIds,
    		final HttpSession session){ 
    	if(ccp_mode.equals("cut_node"))
    		return documentFolderService.cutPaste(sourceNodeId, destNodeId, childrenIds , sessionHelper.getSessionUser(session));
    	else
    		return documentFolderService.copyPaste(sourceNodeId, destNodeId, childrenIds, sessionHelper.getSessionUser(session));
    }
    
    @RequestMapping(value = {"/getUserRoles.json"}, method = RequestMethod.POST)
    @ResponseBody
    public HashSet<String> getUserRoles(final HttpSession session){
    	final String loginUser = sessionHelper.getSessionUser(session);
    	return adminService.getUsersRoles(loginUser);
    }
    
    @RequestMapping(value = {"/getFolderRoles.json"}, method = RequestMethod.POST)
    @ResponseBody
    public Set<String> gertFolderRoles(@RequestParam("folderId") final Integer folderId, final HttpSession session){
    	final String loginUser = sessionHelper.getSessionUser(session);
    	return documentFolderService.getFolderRoles(folderId, loginUser);
    }
    
    @RequestMapping(value = {"/editCustomFilter.json"}, method = RequestMethod.POST)
    @ResponseBody
    public SavedFilter edit(
    		@RequestParam("id") final int id,
    		@RequestParam(value = "name", required=false) final String name,
    		@RequestParam(value = "description", required=false) final String description,
    		@RequestParam(value = "readOnly", required=false) final Boolean readOnly,
    		@RequestParam(value = "roles") final String roles,
    		final HttpSession session) {
    	final String loginUser= sessionHelper.getSessionUser(session);
    	
    	final Map<String, Object> auditData = AuditLogger.getDataMap();
        auditData.put("folderId", id);
        auditData.put("description", description);
        auditData.put("name", name);
        auditData.put("Share with", roles);
        AuditLogger.log(loginUser, AuditActions.CUSTOMFILTER_EDIT, auditData);
        
    	return documentFolderService.editFilter(id, name, description, loginUser, readOnly, roles);
    }
}
