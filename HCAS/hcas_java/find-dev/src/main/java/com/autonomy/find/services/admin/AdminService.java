package com.autonomy.find.services.admin;

import com.autonomy.aci.actions.DontCareAsLongAsItsNotAnErrorProcessor;
import com.autonomy.aci.client.services.AciErrorException;
import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.find.api.database.NamedUser;
import com.autonomy.find.config.SearchConfig;
import com.autonomy.find.config.SearchView;
import com.autonomy.find.dto.UserDetail;
import com.autonomy.find.dto.UserDetailAndRoles;
import com.autonomy.find.dto.UserListDetails;
import com.autonomy.find.dto.admin.UserRequest;
import com.autonomy.find.processors.admin.*;
import com.autonomy.find.services.WebAppUserService;
import com.autonomy.find.util.SessionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;


@Service
public class AdminService {
    public static final String GLOBAL_PREFIX = "hca-";
    public static final String SUPER_USER = "superuser";
    public static final String VIEW_PREFIX = "view:";
    public static final String SAMPLE_VIEW = "sample-data";

    private static final String DATABASES_PRIVILEGE = "databases";


    // #####################################
    // ProtectedRoles
    // #####################################
    public static enum Privileges {
        ADMIN(GLOBAL_PREFIX + "admin");


        private final String privilegeName;

        private Privileges(String value) {
            privilegeName = value;
        }

        public String getPrivilegeName() {
            return privilegeName;
        }
    }
    // A list of default roles
    public static enum ProtectedRoles {
        ADMIN(GLOBAL_PREFIX + "admin"),
        SUPER_USER(GLOBAL_PREFIX + "superuser"),
        // This one is expected when creating a user.
        EVERYONE("everyone");



        private final String rolename;

        private ProtectedRoles(String value) {
            rolename = value;
        }

        public String getRoleName() {
            return rolename;
        }
    }

    public static enum Roles {
        DEFAULT(GLOBAL_PREFIX + "analyst");

        private final String rolename;

        private Roles(String value) {
            rolename = value;
        }

        public String getRoleName() {
            return rolename;
        }
    }

    private HashSet<String> hiddenPrivileges = new HashSet<>();
    private HashSet<String> hiddenRoles = new HashSet<>();

    @Autowired
    SearchConfig searchConfig;

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminService.class);
    private static final String USERLOCK = "USERLOCK";
    private static final String USERNAME = "username";

    @Autowired
    @Qualifier("communityAciService")
    private AciService communityAci;

    @Autowired
    private WebAppUserService webAppUserService;

    @Autowired
    private SessionHelper sessionHelper;

    // ########################################
    // Users Helper Methods
    // ########################################

    public AdminService() {
        hiddenPrivileges.add(DATABASES_PRIVILEGE);
        hiddenRoles.add("everyone");
    }


    public NamedUser getUserByID(String ID) {
        return null;
    }

    public UserListDetails getAllUsers() {
        return webAppUserService.listUsers();
    }

    public UserListDetails getUser(String username) {
        return webAppUserService.listUser(username);
    }
    public UserDetailAndRoles getUserWithPrivileges (String username) {
        final AciParameters params = new AciParameters("userread");
        params.put("username", username);
        params.put("privilege", "*");
        return communityAci.executeAction(params, new UserAndRolesProcessor());
    }

    public boolean createNewUser(String username, String password, String firstName, String lastName, String email) {
        return webAppUserService.registerUser(username, password, firstName, lastName, email);
    }

    public boolean deleteUserByUserName(String username) {
        if(isProtectedUser(username)) {
           return false;
        }
        UserListDetails user = getUser(username);
        Map<String, UserDetail> users = user.getUsers();
        //UserDetailAndRoles userWithPrivileges = getUserWithPrivileges(username);
        // Cannot delete admins
        if(user == null || users.isEmpty()) {
            return false;
        }

        return webAppUserService.deleteUser(username);
    }

    // ########################################
    // ProtectedRoles Helper Methods
    // ########################################


    public HashSet<String> getAllRoles() {
        final AciParameters params = new AciParameters("RoleGetRoleList");
        HashSet<String> roles = communityAci.executeAction(params, new RolesListProcessor());
        roles.removeAll(hiddenRoles);
        return roles;
    }

    public HashSet<String> getAllPrivileges() {
        final AciParameters params = new AciParameters("RoleGetPrivilegeList");

        HashSet<String> privileges = communityAci.executeAction(params, new PrivilegeListProcessor());

        // Remove hidden privileges
        privileges.removeAll(hiddenPrivileges);

        return privileges;
    }

    public HashSet<String> getPrivilegesForRole(String roleName) {
       // http://localhost:9030/a=RoleGetRolePrivilegeValueList&rolename=test123&privilege=*
        final AciParameters params = new AciParameters("RoleGetRolePrivilegeValueList");
        params.add("rolename", roleName);
        params.add("privilege", "*");
        HashSet<String> privileges = communityAci.executeAction(params, new RolesGetPrivileges());

        // Remove hidden privileges
        privileges.removeAll(hiddenPrivileges);

        return privileges;
    }

    public boolean addPrivilegeToRole(String roleToAdd, String privilegeName) {
        boolean success = true;
        try {
            doPrivilegeAction("RoleSetPrivilegeForRole", roleToAdd, privilegeName, "true");

            final String viewDb = getViewDatabaseFromPrivilege(privilegeName);
            if (viewDb != null) {
                doPrivilegeAction("RoleSetPrivilegeForRole", roleToAdd, DATABASES_PRIVILEGE, viewDb);
            }
        } catch (AciErrorException e) {
            success = false;
        }
        return success;
    }

    public boolean addPrivilegeToRoleUnsafe(String roleToAdd, String privilegeName) {
        boolean success = true;
        try {
            doPrivilegeActionUnsafe("RoleSetPrivilegeForRole", roleToAdd, privilegeName, "true");

            final String viewDb = getViewDatabaseFromPrivilege(privilegeName);
            if (viewDb != null) {
                doPrivilegeActionUnsafe("RoleSetPrivilegeForRole", roleToAdd, DATABASES_PRIVILEGE, viewDb);
            }
        } catch (AciErrorException e) {
            success = false;
        }
        return success;
    }

    public boolean removePrivilegeFromRole(String roleToRemove, String privilegeName) {

        boolean success = false;

        try {
            doPrivilegeAction("RoleRemovePrivilegeFromRole", roleToRemove, privilegeName, null);

            final String viewDb = getViewDatabaseFromPrivilege(privilegeName);
            if (viewDb != null) {
                doPrivilegeAction("RoleRemovePrivilegeFromRole", roleToRemove, DATABASES_PRIVILEGE, viewDb);
            }

        } catch (AciErrorException e) {
            success = false;
        }
        return success;
    }

    public boolean updatePrivilege(List<String> rolesToAdd, List<String> rolesToRemove, String privilegeName) {
        boolean success = true;
        // Adding the privileges to the roles
        if(rolesToAdd != null) {
            for(String role : rolesToAdd) {
                try {
                    addPrivilegeToRole(role, privilegeName);
                } catch (AciErrorException e) {
                    success = false;
                }
            }
        }

        // Removing the privileges from the roles
        if(success == true && rolesToRemove != null) {
            for(String role : rolesToRemove) {
                try {
                    removePrivilegeFromRole(role, privilegeName);
                } catch (AciErrorException e) {
                    success = false;
                }
            }
        }

        return success;
    }

    private boolean doPrivilegeAction(String actionName, String roleName, String privilege, String value) {

        if(isProtectedRole(roleName)) {
            return false;
        }
        return doPrivilegeActionUnsafe(actionName, roleName, privilege, value);
    }
    /*
        Will remove protected users and roles
     */
    private boolean doPrivilegeActionUnsafe(String actionName, String roleName, String privilege, String value) {
        final AciParameters params = new AciParameters(actionName);
        params.add("Privilege", privilege);
        params.add("RoleName", roleName);
        if(value != null) {
            params.add("Value", value);
        }
        return communityAci.executeAction(params, new DontCareAsLongAsItsNotAnErrorProcessor());
    }

    public boolean updateRole(String roleToAdd, String roleToRemove, String username) {
        boolean success = true;
        try {
            doRoleAction("RoleAddUserToRole", roleToAdd, username, true);
            doRoleAction("RoleRemoveUserFromRole", roleToRemove, username, null);
        } catch (AciErrorException e) {
            success = false;
        }
        return success;
    }

    public boolean updateUsersRole(List<String> rolesToAdd, List<String> rolesToRemove, String username) {
        boolean success = true;
        if(username.equals(SUPER_USER)) {
            return false;
        }
        // Adding the privileges to the roles
        if(rolesToAdd != null) {
            for(String role : rolesToAdd) {
                try {
                    success = doRoleAction("RoleAddUserToRole", role, username, true);
                    if (!success) {
                        break;
                    }
                } catch (AciErrorException e) {
                    success = false;
                }
            }
        }

        // Removing the privileges from the roles
        if(success == true && rolesToRemove != null) {
            for(String role : rolesToRemove) {
                try {
                    doRoleAction("RoleRemoveUserFromRole", role, username, null);
                } catch (AciErrorException e) {
                    success = false;
                }
            }
        }

        return success;
    }

    private boolean doRoleAction(String actionName, String roleName, String username, Boolean value) {

        if(isProtectedUser(username) || isProtectedRole(roleName)) {
            return false;
        }

        final AciParameters params = new AciParameters(actionName);
        params.add("RoleName", roleName);
        params.add("username", username);
        if(value != null) {
            params.add("Value", value);
        }
        return communityAci.executeAction(params, new DontCareAsLongAsItsNotAnErrorProcessor());
    }

    public HashSet<String> getRolePrivilegeBelongsTo(String privilege) {
        final AciParameters params = new AciParameters("RolePrivilegeGetRoleList");
        params.add("Privilege", privilege);
        try {
            return communityAci.executeAction(params, new RolesListProcessor());
        } catch(AciErrorException e) {
            return null;
        }
    }

    /**
     * Creates a community role
     * @param roleName name of the role to create
     * @return
     */
    public boolean createNewRole(String roleName) {
        if(roleName == null) {
            return false;
        }
        final AciParameters params = new AciParameters("roleadd");
        params.put("rolename", roleName);
        try {
            return communityAci.executeAction(params, new DontCareAsLongAsItsNotAnErrorProcessor());
        } catch (AciErrorException e) {
            return false;
        }

    }

    /**
     * Deletes a community role
     * @param roleName name of the role to delete
     * @return
     */
    public boolean deleteRoleByName(String roleName) {
        if(isProtectedRole(roleName)) {
            return false;
        }
        for(ProtectedRoles role : ProtectedRoles.values()) {
            if(role.getRoleName().toLowerCase().equals(roleName.toLowerCase())) {
                return false;
            }
        }

        final AciParameters params = new AciParameters("RoleDelete");
        params.put("rolename", roleName);
        try {
            return communityAci.executeAction(params, new DontCareAsLongAsItsNotAnErrorProcessor());
        } catch (AciErrorException e) {
            return false;
        }
    }

    /**
     *
     * @param privilegeName
     * @return
     */
    public boolean createNewPrivilege(String privilegeName) {
        final AciParameters params = new AciParameters("RoleAddPrivilege");
        params.put("Privilege", privilegeName);
        params.put("SingleValue", true);
        try {
            return communityAci.executeAction(params, new DontCareAsLongAsItsNotAnErrorProcessor());
        } catch (AciErrorException e) {
            return false;
        }

    }

    public boolean deletePrivilege(String privilegeName) {
        final AciParameters params = new AciParameters("RoleDeletePrivilege");
        params.put("Privilege", privilegeName);
        try {
            return communityAci.executeAction(params, new DontCareAsLongAsItsNotAnErrorProcessor());
        } catch (AciErrorException e) {
            return false;
        }

    }

    public boolean addUserToRole(String username, String roleName) {
        if(username.equals(SUPER_USER)) {
            return false;
        }
        return addUserToRoleUnsafe(username, roleName);
    }

    private boolean addUserToRoleUnsafe(String username, String roleName) {

        final AciParameters params = new AciParameters("RoleAddUserToRole");
        params.put("RoleName", roleName);
        params.put("Username", username);
        try {
            return communityAci.executeAction(params, new DontCareAsLongAsItsNotAnErrorProcessor());
        } catch(AciErrorException e) {
            return false;
        }
    }

    public boolean removeUserFromRole(String username, String roleName) {
        if(isProtectedUser(username)) {
            return false;
        }
        final AciParameters params = new AciParameters("RoleRemoveUserFromRole");
        params.put("RoleName", roleName);
        params.put("Username", username);
        try{
            boolean success =  communityAci.executeAction(params, new DontCareAsLongAsItsNotAnErrorProcessor());

            return success;
        } catch(AciErrorException e) {
            return false;
        }
    }


    public HashSet getUsersWithRole(String rolename) {
        final AciParameters params = new AciParameters("RoleGetUserList");
        params.put("RoleName", rolename);
        try {
            return communityAci.executeAction(params, new RoleUsersProcessor());
        } catch(AciErrorException e) {
            return null;
        }
    }

    public HashSet<String> getUsersRoles(String username) {
        final AciParameters params = new AciParameters("RoleUserGetRoleList");
        params.put("username", username);
        try {
            return communityAci.executeAction(params, new RolesListProcessor());
        } catch(AciErrorException e) {
            return null;
        }
    }


    public boolean updateUserSafe(UserRequest user) {
        boolean success = false;
        if(user.getUsername() == null && user.getUsername().equals(SUPER_USER)) {
            return success;
        }
        success = webAppUserService.updateUser(user);
        return success;
    }
    /**
        Will not change superusers password
     */
    public boolean resetUserPasswordSafe(String currentUser, String username, String newpassword) {

        if(username.equals(SUPER_USER) && !currentUser.equals(SUPER_USER)) {
            return false;
        }

        return resetUserPasswordUnsafe(username, newpassword);
    }
    /**
        Will change superusers password
     */
    public boolean resetUserPasswordUnsafe(String username, String newpassword) {
        final AciParameters params = new AciParameters("useredit");
        params.put("username", username);
        params.put("resetpassword", true);
        params.put("newpassword", newpassword);

        try {
            return communityAci.executeAction(params, new DontCareAsLongAsItsNotAnErrorProcessor());
        } catch(AciErrorException e) {
            return false;
        }
    }
    // #############################################
    // Protected roles, privileges and users - things that users should not be able to change.
    // #############################################
    private boolean isProtectedRole(String roleName) {
        if(roleName != null) {
            for(ProtectedRoles role : ProtectedRoles.values()) {
                // Get all current roles
                if(roleName.trim().equalsIgnoreCase(role.getRoleName().toLowerCase())) {
                  return true;
                }
            }
        }
        return false;
    }

    private boolean isProtectedPrivilege(String privName) {
        if(privName != null) {
            for(Privileges priv : Privileges.values()) {
                // Get all current roles
                if(privName.trim().replaceAll("\\s+","").toLowerCase().equals(priv.getPrivilegeName().toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isProtectedUser(String userName) {
        if(userName != null) {
            if(userName.trim().toLowerCase().equals(SUPER_USER.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    // #############################################
    // #############################################


    /**
     *  Ensure we have all of the default privileges/roles in community and the superuser is setup correctly.
     */
    @PostConstruct
    public void setupAdmin() {
        HashSet<String> roles = getAllRoles();
        HashSet<String> privileges = getAllPrivileges();

        privileges = setupPrivileges(privileges);
        setupRoles(roles, privileges);
        setupTestDBRole(roles, privileges);

        // Ensure the super user is setup correctly
        checkOrCreateSuperUser();
    }

    public HashSet<String> setupPrivileges(HashSet<String> privileges) {
        // Be careful removing privileges because ALL roles will loose that privilege (even if you recreate the same privilege).
        // Setup default privileges
        // Get all privileges
        for(Privileges privilege : Privileges.values()) {
            String privilegeName = privilege.getPrivilegeName();
            // Only create privileges if they exists
            if(!privileges.contains(privilegeName)) {
                createNewPrivilege(privilegeName);
            }
        }

        Set<String> searchViewsNames = searchConfig.getSearchViewsNames();

        // Ensure the correct db privileges are available  removing orphaned view privileges.
        for(String privilege :  privileges) {
            // Only delete privileges if they do not exists in the config file
            if(privilege.startsWith(VIEW_PREFIX)) {
                String viewName = privilege.substring(VIEW_PREFIX.length());
                if(!searchViewsNames.contains(viewName)) {
                    deletePrivilege(privilege);
                }
            }
        }

        // Set all db views as privileges
        // Generate roles dynamically from search config
        for(String viewName : searchViewsNames) {
            String viewPrivilege = getViewPrivilegeName(viewName);
            // Only create privileges if they exists
            if(viewPrivilege != null || !privileges.contains(viewPrivilege)) {
                createNewPrivilege(viewPrivilege);
            }
        }

        return getAllPrivileges();
    }

   private void setupRoles(HashSet<String> roles, HashSet<String> privileges) {
       // Setup default roles

       for(ProtectedRoles role : ProtectedRoles.values()) {
           // Get all current roles
           String roleName = role.getRoleName();
           // Only create roles if they don't exists
           if(!roles.contains(roleName)) {
               createNewRole(roleName);
           }
       }

       // Adding admin privilege to the default admin role.
       List<String> rolesToAdd = new ArrayList<>();
       rolesToAdd.add(ProtectedRoles.ADMIN.getRoleName());
       addPrivilegeToRoleUnsafe(ProtectedRoles.ADMIN.getRoleName(), Privileges.ADMIN.getPrivilegeName());

       // Adding admin role and all db privileges to the superuser privilege
       addPrivilegeToRoleUnsafe(ProtectedRoles.SUPER_USER.getRoleName(), Privileges.ADMIN.getPrivilegeName());
       for (String dbName : searchConfig.getSearchViewsNames()) {
           addPrivilegeToRoleUnsafe(ProtectedRoles.SUPER_USER.getRoleName(), getViewPrivilegeName(dbName));
       }
   }

    /*
    Create analyst role and, if it exists, grant it the db:sample privilege.
    This is designed to be a role for users to test the app with
     */

    private void setupTestDBRole(HashSet<String> roles, HashSet<String> privileges) {
        String analystRole = Roles.DEFAULT.getRoleName(),
               // Get sample view privilege full name
               sampleDBPrivilege = getViewPrivilegeName(SAMPLE_VIEW);
        // Only setup the analysis role if it does not exists
        if(!roles.contains(analystRole)) {
            createNewRole(analystRole);
            if(privileges.contains(sampleDBPrivilege)) {
                // If the role has been created and sample view exists, attach the sample db view to the default role.
                addPrivilegeToRoleUnsafe(analystRole, sampleDBPrivilege);
            }
        }
    }

    /*
    Takes a view name and prepends the db view prefix
    This should always be used when creating and retrieving  a db privilege
     */
    public String getViewPrivilegeName(final String viewName) {
        return viewName != null ? VIEW_PREFIX + viewName : null;
    }

    private String getViewDatabaseFromPrivilege(final String privilege) {
        if (privilege != null && privilege.startsWith(VIEW_PREFIX)) {
            final String viewName = privilege.substring(VIEW_PREFIX.length());
            final SearchView searchView = searchConfig.getSearchViews().get(viewName);
            if (searchView != null) {
                return searchView.getDatabase();
            }
        }

        return null;
    }

    /*
    Ensure there is a super user, if there isn't, create it and give it the configured default password.
     */
    private void checkOrCreateSuperUser() {

        String superUserPassword = searchConfig.getSuperUserDefaultPassword();
        final AciParameters params = new AciParameters("userread");
        params.put("username", SUPER_USER);
        params.put("privilege", "*");
        UserDetailAndRoles userList = communityAci.executeAction(params, new UserAndRolesProcessor());

        if(userList == null) {
            createNewUser(SUPER_USER, superUserPassword, null, null, null);
        }
        // Uncomment this to debug admin default login
        // resetUserPasswordUnsafe(SUPER_USER, superUserPassword);
        addUserToRoleUnsafe(SUPER_USER, ProtectedRoles.SUPER_USER.getRoleName());
    }
    /*
     * To unlock/lock user from admin interface
     */
    public boolean unlockUser(String username,boolean unlock) {
        if(username == null) {
            return false;
        }
        final AciParameters params = new AciParameters(USERLOCK);
        params.put(USERNAME, username);
        params.put("unlock",unlock);
        try {
            return communityAci.executeAction(params, new DontCareAsLongAsItsNotAnErrorProcessor());
        } catch (final Exception e) {
            return false;
        }
    }

}