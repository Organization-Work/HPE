package com.autonomy.find.api.controllers.admin;

import com.autonomy.find.api.response.ResponseWithResult;
import com.autonomy.find.dto.UserListDetails;
import com.autonomy.find.dto.admin.UserRequest;
import com.autonomy.find.dto.admin.UserRoleUpdateRequest;
import com.autonomy.find.services.admin.AdminService;
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
@RequestMapping("/api/admin/users.json")
public class AdminUserController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminUserController.class);

    private static final String
            UNAUTHORIZED_ERROR = "Account not authorized to access this data",
            USER_NOT_FOUND = "No user found",
            ROLE_NOT_FOUND  = "No role found",
            USER_PROFILE_ERROR = "User profile does not exist.",
            PROFILE_CLUSTER_ERROR = "Profile cluster error.",
            USER_NOT_CREATED_ERROR = "User not created.",
            USER_NOT_NAMED_ERROR = "Anonymous user not named.",
            USERNAME_ALREADY_TAKEN_ERROR = "Username already taken.",
            PASSWORD_CHARACTERS_ERROR = "Password contains illegal characters.",
            USER_NOT_FOUND_ERROR = "User not found.",
            USER_DOES_NOT_EXIST_ERROR = "User does not exist.",
            USERNAME_PASSWORD_ERROR = "Username or password is incorrect.",
            USER_NOT_LOGGED_IN_ERROR = "User not logged in.",
            USER_NOT_LOGGED_OUT_ERROR = "User not logged out.",
            UNABLE_TO_GET_RECENT_SEARCH_HISTORY_ERROR = "Unable to get recent search history.",
            USER_SESSION_NOT_FOUND_ERROR = "User session not found.",
            USER_NOT_DELETED_ERROR = "User was not deleted.",
            PROPERTY_PATH_LOOKUP_ERROR = "Property path lookup error.";

    @Autowired
    AdminService adminService;

    @Autowired
    SessionHelper sessionHelper;

    @RequestMapping(method= RequestMethod.POST, params={"username", "password", "firstname", "lastname", "email"})
    public @ResponseBody
    ResponseWithResult<String> create(@RequestParam(value="username", required=true) final String username,
                                      @RequestParam(value="password", required=true) final String password,
                                      @RequestParam(value="firstname", required=true) final String firstname,
                                      @RequestParam(value="lastname", required=true) final String lastname,
                                      @RequestParam(value="email", required=true) final String email,
                                      final HttpSession session) {
        String response;
        boolean success = adminService.createNewUser(username, password, firstname, lastname, email);
        if(success) {
            // Give newly created user the default role.
            adminService.addUserToRole(username, AdminService.Roles.DEFAULT.getRoleName());
            response = "Created user";
            AuditLogger.log(sessionHelper.getRemoteUser(session), AuditActions.CREATED_USER, "Created user " + username);
        } else {
            response = "Failed to create user";
        }

        return new ResponseWithResult<String>(response, success, null);
    }

    @RequestMapping(method= RequestMethod.DELETE, params={"username"})
    public @ResponseBody
    ResponseWithResult<String> deleteUser(@RequestParam(value="username", required=true) final String username,
                                          final HttpSession session) {
        String response;
        boolean success = adminService.deleteUserByUserName(username);
        if(success) {
            response = "Deleted user";
            AuditLogger.log(sessionHelper.getRemoteUser(session), AuditActions.DELETED_USER, "Deleted user " + username);
        } else {
            response = "Failed to delete user. Please check that this user does not have admin privileges.";
        }

        return new ResponseWithResult<String>(response, success, null);
    }

    @RequestMapping()
    public @ResponseBody
    ResponseWithResult<String> getAllUsers() {
        UserListDetails allUsers = adminService.getAllUsers();
        return new ResponseWithResult<String>(allUsers.toJson(), true, null);
    }

    @RequestMapping(params={"username"})
    public @ResponseBody
    ResponseWithResult<String> getUser(@RequestParam(value="username", required=true) final String username) {
        UserListDetails allUsers = adminService.getUser(username);
        return new ResponseWithResult<String>(allUsers.toJson(), true, null);
    }

    @RequestMapping(method = RequestMethod.POST, headers = "content-type=application/json; charset=utf-8")
    public @ResponseBody
    ResponseWithResult updateUserRoles(@RequestBody UserRoleUpdateRequest userRole,
                                       final HttpSession session) {



        String resultString = "Successfully updated user's roles. ";
        Boolean success = adminService.updateUsersRole(userRole.getToAdd(), userRole.getToRemove(), userRole.getUsername());
        if(!success) {
            resultString = "Failed to update user's roles. ";
        }

        AuditLogger.log(sessionHelper.getRemoteUser(session), AuditActions.ADDED_ROLE_TO_USER, resultString +  "Username " + userRole.getUsername() +
                ": Adding roles " +  userRole.getToAdd().toString() +
                ": Removing roles " + userRole.getToRemove().toString());


        return new ResponseWithResult<String>(null, success, null);
    }

    @RequestMapping(method = RequestMethod.PUT, params={"username", "newPassword"})
    public @ResponseBody
    ResponseWithResult updateUserPassword(@RequestParam(value="username", required=true) final String username,
                                          @RequestParam(value="newPassword", required=true) final String newPassword,
                                          final HttpSession session) {

        AuditLogger.log(sessionHelper.getRemoteUser(session), AuditActions.CHANGE_USERS_PASSWORD, "Changed user  " + username + "'s password.");

        Boolean success = adminService.resetUserPasswordSafe(sessionHelper.getSessionUser(session) ,username, newPassword);
        return new ResponseWithResult<String>(null, success, null);
    }

    @RequestMapping(method = RequestMethod.PUT)
    public @ResponseBody
    ResponseWithResult updateUser(@RequestBody UserRequest user,
                                  final HttpSession session) {

        Boolean success = adminService.updateUserSafe(user);
        AuditLogger.log(sessionHelper.getRemoteUser(session), AuditActions.UPDATE_USERS_DETAILS, "Updating user " + user.getUsername() + "'s details");
        return new ResponseWithResult<String>(null, success, null);
    }
    
    @RequestMapping(method = RequestMethod.PUT,params={"username", "unlock"})
    public @ResponseBody
    ResponseWithResult unlockUser(@RequestParam(value="username", required=true) final String username,
            @RequestParam(value="unlock", required=true) final boolean unlock,
                                  final HttpSession session) {

        Boolean success = adminService.unlockUser(username,unlock);
        AuditLogger.log(sessionHelper.getRemoteUser(session), AuditActions.LOCK_USER, "Lock/Unlock user " + username + "'s details");
        return new ResponseWithResult<String>(null, success, null);
    }



}
