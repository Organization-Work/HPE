package com.autonomy.find.api.controllers.admin;

import com.autonomy.find.api.response.ResponseWithResult;
import com.autonomy.find.dto.admin.UpdateRequest;
import com.autonomy.find.services.admin.AdminService;
import com.autonomy.find.util.SessionHelper;
import com.autonomy.find.util.audit.AuditActions;
import com.autonomy.find.util.audit.AuditLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.HashSet;
import java.util.Set;


@Controller
@RequestMapping("/api/admin/privilege.json")
public class AdminPrivilegeController {

//    private static final Logger LOGGER = LoggerFactory.

    private static final String
            ROLE_NOT_FOUND  = "No role found";

    @Autowired
    AdminService adminService;

    @Autowired
    SessionHelper sessionHelper;

    @RequestMapping(method = RequestMethod.GET)
    public @ResponseBody
    ResponseWithResult getPrivilege() {
        HashSet<String> allRoles = adminService.getAllPrivileges();
        return new ResponseWithResult<Set<String>>(allRoles, true, null);
    }

    @RequestMapping(method = RequestMethod.POST, headers = "content-type=application/json; charset=utf-8")
    public @ResponseBody
    ResponseWithResult updatePrivilege(@RequestBody UpdateRequest priviData,
                                       final HttpSession session) {

        Boolean success = adminService.updatePrivilege(priviData.getToAdd(), priviData.getToRemove(), priviData.getItemName());
        String resultString = "Updated privilege. ";
        if(!success) {
            resultString = "Failed to update privilege. ";
        }

        AuditLogger.log(sessionHelper.getRemoteUser(session), AuditActions.ADDED_PRIVILEGE_TO_ROLE, resultString + "Privilege Name " + priviData.getItemName() +
                ": Adding roles " + priviData.getToAdd().toString() +
                ": Removing roles " + priviData.getToRemove().toString());

        return new ResponseWithResult<String>(null, success, null);
    }


}


