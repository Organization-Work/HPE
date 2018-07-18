package com.autonomy.find.api.controllers.admin;

import com.autonomy.find.api.response.ResponseWithResult;
import com.autonomy.find.dto.admin.UpdateRequest;
import com.autonomy.find.services.admin.AdminService;
import com.autonomy.find.util.SessionHelper;
import com.autonomy.find.util.audit.AuditActions;
import com.autonomy.find.util.audit.AuditLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


@Controller
@RequestMapping("/api/admin/roles.json")
public class AdminRolesController {

    private static final String
            ROLE_NOT_FOUND  = "No role found";

    @Autowired
    AdminService adminService;

    @Autowired
    SessionHelper sessionHelper;


    @RequestMapping(method = RequestMethod.GET)
    @SuppressWarnings("unchecked")
    public @ResponseBody
    ResponseWithResult getRoles() {
        HashSet<String> allRoles = adminService.getAllRoles();
        return new ResponseWithResult(allRoles, true, null);
    }

    @RequestMapping(method = RequestMethod.POST)
    @SuppressWarnings("unchecked")
    public @ResponseBody
    ResponseWithResult createRole(@RequestParam("name") List<String> name,
                                  final HttpSession session) {
        String roleName = name.get(0);
        boolean result = adminService.createNewRole(name.get(0));
        String resultString = "Created role ";
        if(!result) {
            resultString = "Failed to create user ";
        }
        AuditLogger.log(sessionHelper.getRemoteUser(session), AuditActions.CREATED_ROLE, resultString + roleName);

        return new ResponseWithResult(null, result, null);
    }

    @RequestMapping(method = RequestMethod.DELETE, params={"rolename"})
    @SuppressWarnings("unchecked")
    public @ResponseBody
    ResponseWithResult deleteRole(@RequestParam(value="rolename", required=true) final String rolename,
                                  final HttpSession session) {
        boolean result = adminService.deleteRoleByName(rolename);
        String resultString = "Deleted role ";
        if(!result) {
            resultString = "Failed to delete user ";
        }
        AuditLogger.log(sessionHelper.getRemoteUser(session), AuditActions.DELETE_ROLE, resultString + rolename);

        return new ResponseWithResult(null, result, null);
    }

    @RequestMapping(method = RequestMethod.DELETE, params={"username", "rolename"})
    @SuppressWarnings("unchecked")
    public @ResponseBody
    ResponseWithResult removeUserFromRole(@RequestParam(value="rolename", required=true) final String rolename,
                                          @RequestParam(value="username", required=true) final String username,
                                          final HttpSession session) {
        boolean result = adminService.removeUserFromRole(username, rolename);
        String resultString = "Removed role ";
        if(!result) {
            resultString = "Failed to remove role ";
        }
        AuditLogger.log(sessionHelper.getRemoteUser(session), AuditActions.REMOVED_ROLE_FROM_USER, resultString + rolename + " from user " + username);
        return new ResponseWithResult(null, result, null);
    }

    @RequestMapping(method = RequestMethod.PUT, params={"username", "rolename"})
    @SuppressWarnings("unchecked")
    public @ResponseBody
    ResponseWithResult addUserToRole(@RequestParam(value="rolename", required=true) final String rolename,
                                     @RequestParam(value="username", required=true) final String username,
                                     final HttpSession session) {
        boolean result = adminService.addUserToRole(username, rolename);
        String resultString = "Added role ";
        if(!result) {
            resultString = "Failed to add role ";
        }
        AuditLogger.log(sessionHelper.getRemoteUser(session), AuditActions.ADDED_ROLE_TO_USER, resultString + rolename + " from user " + username);
        return new ResponseWithResult(null, result, null);
    }

//    @RequestMapping(method = RequestMethod.GET, params={"rolename"})
//    @SuppressWarnings("unchecked")
//    public @ResponseBody
//    ResponseWithResult getUsersWithRole(@RequestParam(value="rolename", required=true) final String rolename) {
//        return new ResponseWithResult(null, adminService.getUsersWithRole(rolename), null);
//    }

    @RequestMapping(method = RequestMethod.GET, params={"rolename"})
    @SuppressWarnings("unchecked")
    public @ResponseBody
    ResponseWithResult getPrivilegesWithinRole(@RequestParam(value="rolename", required=true) final String rolename) {
        HashMap<String, HashSet<String>> roleDetails = new HashMap<>();
        roleDetails.put("users", adminService.getUsersWithRole(rolename));
        roleDetails.put("privileges", adminService.getPrivilegesForRole(rolename));

        boolean success = false;

        if(roleDetails != null) {
            success = true;
        }

        return new ResponseWithResult(roleDetails, success, null);
    }

    @RequestMapping(method = RequestMethod.POST, headers = "content-type=application/json; charset=utf-8")
    @SuppressWarnings("unchecked")
    public @ResponseBody
    ResponseWithResult updateRole(@RequestParam(value="type", required=true) final String type,
                                  @RequestBody UpdateRequest roleData,
                                  final HttpSession session) {
        String PRIVILEGE = "privilege",
               USER = "user";

        AuditLogger.log(sessionHelper.getRemoteUser(session), AuditActions.ADDED_ROLE_TO_USER,  "Role name " + roleData.getItemName() +
                                                                                                ": Adding " + type + " " +  roleData.getToAdd().toString() +
                                                                                                ": Removing " + type + " " + roleData.getToRemove().toString());

        if(type.toLowerCase().equals(USER)) {
            for(String userToAdd : roleData.getToAdd()) {
                adminService.addUserToRole(userToAdd, roleData.getItemName());
            }
            for(String userToRemove : roleData.getToRemove()) {
                adminService.addUserToRole(userToRemove, roleData.getItemName());
            }
        }

        if(type.toLowerCase().equals(PRIVILEGE)) {
            String roleName = roleData.getItemName();
            for(String privilegeToAdd : roleData.getToAdd()) {
                adminService.addPrivilegeToRole(roleName, privilegeToAdd);
            }
            for(String privilegeToRemove : roleData.getToRemove()) {
                adminService.removePrivilegeFromRole(roleName, privilegeToRemove);
            }
        }

        return new ResponseWithResult(null, true, null);
    }

}
