package com.autonomy.find.util.audit;

public enum AuditActions {
    QUERY_RESULTS,
    QUERY_FIELDS,
    DOCFOLDER_EXPORT,
    DOCFOLDER_IMPORT,
    DOCFOLDER_TAG,
    DOCFOLDER_UNTAG,
    DOCFOLDER_CREATE,
    DOCFOLDER_EDIT,
    CUSTOMFILTER_EDIT,
    DOCFOLDER_DELETE,
    RESULTS_EXPORT,
    RESULTS_TAG,
    TABLEVIEWER,
    DOCVIEW,
    FILTERS_SAVE,
    FILTERS_LOAD,
    FILTERS_UPDATE,
    FILTERS_DELETE,
    FILTERS_FOLDER_CREATE,
    FILTERS_FOLDER_DELETE,
    FILTERS_FOLDER_UPDATE,
    USER_LOGIN,
    USER_LOGIN_SSO,
    USER_LOGOUT,
    USER_LOGOUT_SSO,
    USER_REGISTER,

    CREATED_USER,
    UPDATE_USERS_DETAILS,
    CHANGE_USERS_PASSWORD,
    ADDED_ROLE_TO_USER,
    REMOVED_ROLE_FROM_USER,
    DELETED_USER,
    LOCK_USER,

    CREATED_ROLE,
    UPDATED_ROLE,
    ADDED_PRIVILEGE_TO_ROLE,
    DELETE_ROLE,

    CREATED_PRIVILEGE,
    UPDATE_PRIVILEGE,
    DELETED_PRIVILEGE,
    
    TIMELINEDBSTATS,    
    TIMELINEQUERYSTATS,
    TIMELINEPARAMFIELDSTATS,
    TIMELINETAGVALUEDETAILS,
    TIMELINETAGVALUEDELTADETAILS,
    TIMELINETAGVALUENETCHANGEDETAILS,
    
    TRENDINGTOTALS,
    TRENDINGDETAILS
}