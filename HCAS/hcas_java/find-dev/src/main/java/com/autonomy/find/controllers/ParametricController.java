package com.autonomy.find.controllers;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import com.autonomy.find.api.database.SavedFilter;
import com.autonomy.find.api.database.SavedFiltersFolder;
import com.autonomy.find.api.response.ResponseWithResult;
import com.autonomy.find.api.response.ResponseWithSuccessError;
import com.autonomy.find.config.ParametricConfig;
import com.autonomy.find.config.SearchConfig;
import com.autonomy.find.dto.Parametric.FieldValueData;
import com.autonomy.find.dto.Parametric.FilterGroup;
import com.autonomy.find.dto.Parametric.FiltertreeEntry;
import com.autonomy.find.fields.FilterField;
import com.autonomy.find.fields.FilterFields;

import com.autonomy.find.services.FilterService;
import com.autonomy.find.services.ParametricService;
import com.autonomy.find.util.JSON;
import com.autonomy.find.util.SessionHelper;
import com.autonomy.find.util.audit.AuditActions;
import com.autonomy.find.util.audit.AuditLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;

@Controller
@RequestMapping("/p/ajax/parametric/")
public class ParametricController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParametricController.class);

    private static final int COULD_NOT_GET_FIELD_VALUES_ERROR_ID = 1;
    private static final String COULD_NOT_GET_FIELD_NAMES_ERROR = "Could not get field names.";
    private static final String COULD_NOT_GET_FIELD_VALUES_ERROR = "Could not get field values.";

    private static final String ERROR_MESSAGE_DETAILS = "Error in retrieving results. Please see server log for details.";

    @Autowired
    private ParametricService parametricService;

    @Autowired
    private ParametricConfig parametricConfig;

    @Autowired
    private FilterService filterService;

    @Autowired
    private SessionHelper sessionHelper;
    
    @Autowired
 	private SearchConfig searchConfig;

    @RequestMapping("getParaFieldValues.json")
    @ResponseBody
    public ResponseWithSuccessError getParaFieldValues(@RequestParam("searchView")  final String searchView,
                                                       @RequestParam(value = "singleQueryField", defaultValue = "false") final boolean singleQueryField,
                                                       final HttpSession session) {

        try {        	
        	// IDOL
        	if(searchConfig.getSearchViews().get(searchView).getRepository().equalsIgnoreCase("IDOL")) {
        		final Map<String, FieldValueData> fieldValues = parametricService.getParaFieldValues(searchView, singleQueryField, sessionHelper.getUserSecurityInfo(session));
        		return new ResponseWithResult<>(fieldValues);
        	} else {
        		// Vertica
        		/*final Map<String, FilterField> filterFields = parametricService.getFilterFieldNames(searchView);
                return new ResponseWithResult<>(filterFields);*/
        		final Map<String, FieldValueData> fieldValues = parametricService.getParaFieldValues(searchView, singleQueryField, sessionHelper.getUserSecurityInfo(session));
        		return new ResponseWithResult<>(fieldValues);
        	}            
        }
        catch (final RuntimeException e) {
            LOGGER.error(COULD_NOT_GET_FIELD_VALUES_ERROR, e);
            return new ResponseWithSuccessError(COULD_NOT_GET_FIELD_VALUES_ERROR, ERROR_MESSAGE_DETAILS);
        }
    }

    @RequestMapping("getDateParaFieldValues.json")
    @ResponseBody
    public ResponseWithSuccessError getDateParaFieldValues(@RequestParam("searchView")  final String searchView,
                                                             @RequestParam("field")  final String field,
                                                             @RequestParam("datePeriod")  final String datePeriod,
                                                             @RequestParam("dateOffset")  final String dateOffset,
                                                             @RequestParam(value = "singleQueryField", defaultValue = "false") final boolean singleQueryField,
                                                             final HttpSession session ) {

        try {
            final Map<String, FieldValueData> fieldValues = parametricService.getDateParaFieldValues(searchView, field, datePeriod, dateOffset, singleQueryField, sessionHelper.getUserSecurityInfo(session));
            return new ResponseWithResult<>(fieldValues);
        }
        catch (final RuntimeException e) {
            LOGGER.error(COULD_NOT_GET_FIELD_VALUES_ERROR, e);
            return new ResponseWithSuccessError(COULD_NOT_GET_FIELD_VALUES_ERROR, ERROR_MESSAGE_DETAILS);
        }
    }

    @RequestMapping("getSingleParaFieldValues.json")
    @ResponseBody
    public ResponseWithSuccessError getSingleParaFieldValues(@RequestParam("searchView")  final String searchView,
                                                             @RequestParam("field")  final String field,
                                                             final HttpSession session ) {

        try {
            final Map<String, FieldValueData> fieldValues = parametricService.getSingleParaFieldValues(searchView, field, sessionHelper.getUserSecurityInfo(session));
            return new ResponseWithResult<>(fieldValues);
        }
        catch (final RuntimeException e) {
            LOGGER.error(COULD_NOT_GET_FIELD_VALUES_ERROR, e);
            return new ResponseWithSuccessError(COULD_NOT_GET_FIELD_VALUES_ERROR, ERROR_MESSAGE_DETAILS);
        }
    }

    @RequestMapping("getParaFieldNames.json")
    @ResponseBody
    public ResponseWithSuccessError getFilterFieldNames(@RequestParam("searchView")  final String searchView) {

        try {
            final Map<String, FilterField> filterFields = parametricService.getFilterFieldNames(searchView);
            return new ResponseWithResult<>(filterFields);
        }
        catch (final RuntimeException e) {
            LOGGER.error(COULD_NOT_GET_FIELD_NAMES_ERROR, e);
            return new ResponseWithSuccessError(COULD_NOT_GET_FIELD_NAMES_ERROR, ERROR_MESSAGE_DETAILS);
        }
    }

    @RequestMapping("getTreeFolderContent.json")
    @ResponseBody
    public List<FiltertreeEntry> getTreeFolderContent(@RequestParam("folderId") final Integer folderId,
                                                      @RequestParam("searchView") final String searchView,
                                                      final HttpSession session) {
        final String user = sessionHelper.getRemoteUser(session);
        return filterService.getFolderContent(folderId, searchView, user);
    }
    

    @RequestMapping("createTreeFolder.json")
    @ResponseBody
    public FiltertreeEntry createTreeFolder(@RequestBody final FiltertreeEntry folderData,
                                            final HttpSession session) {
        final String loginUser = sessionHelper.getSessionUser(session);

        final SavedFiltersFolder folder = filterService.createFolder(folderData.getName(), folderData.getParentFolderId(), folderData.getSearchView(), loginUser);
        final FiltertreeEntry entry =  FiltertreeEntry.toFiltertreeEntry(folder);

        AuditLogger.log(loginUser, AuditActions.FILTERS_FOLDER_CREATE, entry);

        return entry;

    }

    @RequestMapping("saveFilter.json")
    @ResponseBody
    public FiltertreeEntry saveFilter(@RequestBody final FiltertreeEntry filterData,
                                      final HttpSession session) {

        final String loginUser = sessionHelper.getSessionUser(session);

        final SavedFilter filter = filterService.saveFilters(filterData.getName(),
                filterData.getDescription(),
                filterData.getData(),
                filterData.getParentFolderId(),
                filterData.getSearchView(),
                loginUser);

        AuditLogger.log(loginUser, AuditActions.FILTERS_SAVE, filterData);

        return null;


    }

    @RequestMapping("updateFilter.json")
    @ResponseBody
    public FiltertreeEntry updateFilter(@RequestBody final FiltertreeEntry filterData,
                                        final HttpSession session) {

        final String loginUser = sessionHelper.getSessionUser(session);

        final SavedFilter filter = filterService.updateFilter(filterData.getId(),
                filterData.getDescription(),
                filterData.getData(),
                loginUser);

        AuditLogger.log(loginUser, AuditActions.FILTERS_UPDATE, filterData);

        return null;


    }


    @RequestMapping("getFilterData.json")
    @ResponseBody
    public FilterGroup getFilterData(@RequestParam("filterId") final Integer filterId,
                                     final HttpSession session) {
        FilterGroup groupData = null;
        final String filterText =  filterService.getFilterData(filterId, sessionHelper.getSessionUser(session));
        try {
            groupData = JSON.toObject(new StringReader(filterText), FilterGroup.class);

        } catch( Exception e) {
            LOGGER.error("Error getting filter data", e);
        }

        return groupData;
    }

    @RequestMapping(value = {"/deleteFilter.json"}, method = RequestMethod.POST)
    @ResponseBody
    public void deleteFilter(@RequestParam("id") final Integer id,
                                    final HttpSession session) {

        final String loginUser = sessionHelper.getSessionUser(session);
        final Map<String, Object> auditData = AuditLogger.getDataMap();
        auditData.put("filterId", id);

        filterService.deleteFilter(id, loginUser);
        AuditLogger.log(loginUser, AuditActions.FILTERS_DELETE, auditData);

    }

    @RequestMapping(value = {"/deleteFolder.json"}, method = RequestMethod.POST)
    @ResponseBody
    public void deleteFolder(@RequestParam("id") final Integer id,
                                    final HttpSession session) {
        final String loginUser = sessionHelper.getSessionUser(session);
        final Map<String, Object> auditData = AuditLogger.getDataMap();
        auditData.put("folderId", id);

        filterService.deleteFolder(id, loginUser);
        AuditLogger.log(loginUser, AuditActions.FILTERS_FOLDER_DELETE, auditData);
    }

    @RequestMapping("renameFolder.json")
    @ResponseBody
    public FiltertreeEntry renameFolder(@RequestParam("id") final Integer id,
                                    @RequestParam("newName") final String newName,
                                    final HttpSession session) {

        final String loginUser = sessionHelper.getSessionUser(session);
        final Map<String, Object> auditData = AuditLogger.getDataMap();
        auditData.put("folderId", id);
        auditData.put("folderName", newName);

        final SavedFiltersFolder folder =  filterService.renameFolder(id, newName, loginUser);

        AuditLogger.log(loginUser, AuditActions.FILTERS_FOLDER_UPDATE, auditData);

        return  FiltertreeEntry.toFiltertreeEntry(folder);
    }

    @RequestMapping("renameFilter.json")
    @ResponseBody
    public FiltertreeEntry renameFilter(@RequestParam("id") final Integer id,
                                    @RequestParam("newName") final String newName,
                                    final HttpSession session) {
        final String loginUser = sessionHelper.getSessionUser(session);
        final Map<String, Object> auditData = AuditLogger.getDataMap();
        auditData.put("filterId", id);
        auditData.put("filterName", newName);

        final SavedFilter filter =  filterService.renameFilter(id, newName, loginUser);

        AuditLogger.log(loginUser, AuditActions.FILTERS_UPDATE, auditData);

        return  FiltertreeEntry.toFiltertreeEntry(filter);
    }

    @RequestMapping("getRootFolder.json")
    @ResponseBody
    public FiltertreeEntry getRootFolder(final HttpSession session) {
        final SavedFiltersFolder folder =  filterService.getRootFolder();
        return  FiltertreeEntry.toFiltertreeEntry(folder);
    }
    
    @RequestMapping(value = {"/getFilterItem.json"}, method = RequestMethod.POST)
    @ResponseBody
    public FiltertreeEntry getFilterItem(@RequestParam("filterId") final Integer filterId,
    		@RequestParam("searchView") final String searchview,
    		final HttpSession session
    		){
    	
    	final String loginUser = sessionHelper.getSessionUser(session);
    	return filterService.getFiletrItem(filterId, null, loginUser);
    }
}