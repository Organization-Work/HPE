package com.autonomy.find.services;

import com.autonomy.aci.actions.DontCareAsLongAsItsNotAnErrorProcessor;
import com.autonomy.aci.client.annotations.IdolAnnotationsProcessorFactory;
import com.autonomy.aci.client.services.AciConstants;
import com.autonomy.aci.client.services.AciErrorException;
import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.find.config.FindConfig;
import com.autonomy.find.config.LoginSettings;
import com.autonomy.find.config.SearchConfig;
import com.autonomy.find.dto.*;
import com.autonomy.find.dto.admin.UserRequest;
import com.autonomy.find.processors.admin.UserListDetailsProcessor;
import com.autonomy.find.processors.admin.UserProcessor;
import com.autonomy.find.processors.admin.UserListProcessor;
import com.autonomy.find.services.admin.AdminService;
import com.autonomy.find.util.LoginResult;
import com.autonomy.find.util.LoginStatus;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WebAppUserService {

    public static final String
            USERNAME = "UserName",
            PASSWORD = "Password",
            EMAIL = "find_Email",
            FIRST_NAME = "find_FirstName",
            SECOND_NAME = "find_LastName",
            FIELD_TAG = "Field",
            DEVICE = "FieldFindNews_Device",
            REGISTERED = "FieldFindNews_Registered",
            USERADD = "UserAdd",
            USEREXIST = "UserExist",
            USERREAD = "UserRead",
            USEREDIT = "useredit",
            UNKNOWN_VALUE = "Unknown";

    private static final Logger LOGGER = LoggerFactory.getLogger(WebAppUserService.class);

    @Autowired
    private SearchConfig config;

    @Autowired
    @Qualifier("communityAciService")
    private AciService communityACI;

    @Autowired
    @Qualifier("userService")
    private UserService userService;

    @Autowired
    private IdolAnnotationsProcessorFactory processorFactory;

    @Autowired
    @Qualifier("findSessionFactory")
    private SessionFactory findSessionFactory;
    
    @Autowired
    private AdminService adminService;
    
    @Autowired
    private FindConfig findConfig;

    /**
     * Registers a user if they don't already exist
     *
     * @param username
     */
    public void registerIfNeeded(final String username) {
        if (!userExists(username)) {
            registerUser(username);
        }
    }

    /**
     * Checks if a user exists under some username
     *
     * @param username
     * @return
     */
    public boolean userExists(final String username) {
        final AciParameters params = new AciParameters(USEREXIST);

        params.put(USERNAME, username);

        try {
            List<UserExists> userExists = communityACI.executeAction(params, processorFactory.listProcessorForClass(UserExists.class));
            return userExists.get(0).getStatus();
        } catch (final Exception e) {
            return false;
        }
    }
    
    private boolean isAutonomy() {
    	 final LoginSettings settings = this.findConfig.getLoginSettings();
    	 boolean isAutonomy = settings.isAutonomy();
    	 return isAutonomy;
    }
    
    private String getRepositoryType() {
    	final LoginSettings settings = this.findConfig.getLoginSettings();
    	String type = settings.getCommunityRepositoryType();
    	return type;
    }

    /**
     * Checks if a username and password combination authenticates a user.
     *
     * @param username
     * @param password
     * @returns userData
     */
    @SuppressWarnings("rawtypes")
	public LoginResult userLogin(final String username, final String password) {
        final AciParameters params = new AciParameters(USERREAD);

        params.put(USERNAME, username);
        params.put(PASSWORD, password);
        
        if (!isAutonomy()) {
        	params.put("DeferLogin", "true");
        }
        
        params.put("SecurityInfo", "true");
        
        boolean isSuperuser = AdminService.SUPER_USER.equals(username);
        
        if (isSuperuser) {
        	params.put("Repository", "autonomy");
        }
        else {
        	String type = getRepositoryType();
        	params.put("Repository", type);
        }

        try {
            List<UserLogIn> userExists = communityACI.executeAction(params, processorFactory.listProcessorForClass(UserLogIn.class));
            if(userExists.get(0).isAuthenticated()){
            	UserDetailAndRoles details = this.adminService.getUserWithPrivileges(username);
            	
            	List<String> privileges = details.getPrivileges();
            	if (privileges.size() == 0 && !isSuperuser) {
            		LoginResult resultError = new LoginResult(LoginStatus.ERROR, "");
            		resultError.setMessage("Your credentials are valid, but you have no privileges defined for your role. Please contact your administrator.");
            		return resultError;
            	}
            	LoginResult resultSuccess = new LoginResult(LoginStatus.SUCCESS, userExists.get(0).getSecurityInfo());
            	return resultSuccess;
            } else {
            	LoginResult resultFail = new LoginResult(LoginStatus.FAIL, "");
            	return resultFail;
            }
        } catch (AciErrorException e) {
        	LoginResult resultError = new LoginResult(LoginStatus.ERROR, "");
        	resultError.setMessage(e.getMessage());
            return resultError;
        }
    }


    /**
     * Registers a user
     *
     * @param username
     * @return
     */
    public boolean registerUser(final String username) {
        return registerUser(username, "demo");
    }

    /**
     * Registers a user
     *
     * @param username
     * @return
     */
    public boolean registerUser(final String username, final String password) {
        final AciParameters params = new AciParameters(USERADD);
        params.put(USERNAME, username);
        params.put(PASSWORD, password);
        params.addAll(UserSettings.paramsForNewUser(config));
        try {
            return communityACI.executeAction(params, new DontCareAsLongAsItsNotAnErrorProcessor());
        } catch (final Exception e) {
            return false;
        }
    }

    public boolean registerUser(final String username, final String password,  String firstName,  String lastName,  String email) {
        if(firstName == null) firstName = UNKNOWN_VALUE;
        if(lastName == null) lastName = UNKNOWN_VALUE;
        if(email == null) email = UNKNOWN_VALUE;

        final AciParameters params = new AciParameters(USERADD);
        params.put(USERNAME, username);
        params.put(PASSWORD, password);
        params.put(FIELD_TAG + EMAIL, email);
        params.put(FIELD_TAG + FIRST_NAME, firstName);
        params.put(FIELD_TAG + SECOND_NAME, lastName);
        params.addAll(UserSettings.paramsForNewUser(config));
        try {
            return communityACI.executeAction(params, new DontCareAsLongAsItsNotAnErrorProcessor());
        } catch (final Exception e) {
            return false;
        }
    }
    public boolean updateUser(UserRequest user) {
        if(user.getUsername() == null) {
            return false;
        }
        final AciParameters params = new AciParameters(USEREDIT);
        params.put(USERNAME, user.getUsername());
        params.put(FIELD_TAG + EMAIL, user.getEmail());
        params.put(FIELD_TAG + FIRST_NAME, user.getFirstname());
        params.put(FIELD_TAG + SECOND_NAME, user.getLastname());
        try {
            return communityACI.executeAction(params, new DontCareAsLongAsItsNotAnErrorProcessor());
        } catch (final Exception e) {
            return false;
        }
    }

    public boolean deleteUser(final String username) {
        return userService.deleteCommunityProfile(username);
    }

    public UserListDetails listUsers() {
    	   	
    	//
    	// This is a workaround for the issue reported here: 
    	// https://customers.autonomy.com/support/secure/case.jsf?caseNumber=1245337
    	//
    	// Since UserReadUserListDetails can fail with an error, we are calling /a=UserReadUserList instead.
    	// This action returns usernames, but not the details. Details will be retrieved by making a second
    	// call: /a=UserRead&username= and do it repeatedly for each user. This is done in listUser(username) method.
    	//
    	final AciParameters _params = new AciParameters("UserReadUserList");
    	UserListDetails _userList = communityACI.executeAction(_params, new UserListProcessor());
    	UserListDetails resultUserList = new UserListDetails();
    	for(String key : _userList.getUsers().keySet()) {
    		String username = key;
    		UserListDetails userDetails = listUser(username);
    		UserDetail userDetail = userDetails.getUserDetailsByUsername(username);
    		
    		resultUserList.getUsers().put(username, userDetail);
    	}
    	
    	return resultUserList;
    	
    	/***   	
        final AciParameters params = new AciParameters("UserReadUserListDetails");
        UserListDetails userList = communityACI.executeAction(params, new UserListDetailsProcessor());
        return userList;
        ***/
    }

    public UserListDetails listUser(String username) {
        final AciParameters params = new AciParameters("userread");
        params.put("username", username);
        UserListDetails userList = communityACI.executeAction(params, new UserProcessor());
        return userList;
    }

    /**
     * @param username
     * @param password
     * @param device
     * @return
     */
    public boolean registerUserDevice(
            final String username,
            final String password,
            final String device) {

        return false;
    }

    public boolean registerDevice(
            final String device) {
        if (!userExists(device)) {
            final AciParameters params = new AciParameters(USERADD);
            params.put(USERNAME, device);
            params.put(PASSWORD, "");
            params.put(DEVICE, device);
            params.put(REGISTERED, false);
            try {
                return communityACI.executeAction(params, new DontCareAsLongAsItsNotAnErrorProcessor());
            } catch (final Exception e) {
            }
        }
        return false;
    }

    public boolean register(
            final String username,
            final String password,
            final String device) {
        if (username == null && password == null) {
            return registerDevice(device);
        } else {
            return registerUserDevice(username, password, device);
        }
    }

    /**
     * Updates a user's settings
     *
     * @param username
     * @param settings
     * @return
     */
    public boolean updateSettings(final String username,
                                  final UserSettings settings) {
        final AciParameters params = settings.asAciParams(username);
        try {
            return communityACI.executeAction(params, new DontCareAsLongAsItsNotAnErrorProcessor());
        } catch (final Exception e) {
            return false;
        }
    }

    public UserSettings retrieveSettings(final String username) {
        final AciParameters params = new AciParameters();
        params.put(AciConstants.PARAM_ACTION, USERREAD);
        params.put(USERNAME, username);
        try {
            List<UserSettings> userExists = communityACI.executeAction(params, processorFactory.listProcessorForClass(UserSettings.class));
            return userExists.get(0);
        } catch (final Exception e) {
            return new UserSettings("" + config.getDisplayCharacters(), config.getDatabases(), config.getSuggestionDatabases(), config.getDatabases(), config.getSuggestionDatabases(), config.getMinScore());
        }
    }
}
