package com.autonomy.find.util;

import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.find.dto.UserDetailAndRoles;
import com.autonomy.find.processors.admin.UserAndRolesProcessor;
import com.autonomy.find.services.WebAppUserService;
import com.autonomy.find.services.admin.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.List;


@Service
public class SessionHelper {

    @Autowired
    private WebAppUserService users;

    @Autowired
    private AdminService adminService;

    @Autowired
    @Qualifier("communityAciService")
    private AciService communityACI;

	private static final String USERNAME = "username";
	private static final String SECURITYINFO = "securityinfo";

	public String setRemoteUser(final HttpSession session, final String username) {
		session.setAttribute(USERNAME, username);
        UserDetailAndRoles userList = adminService.getUserWithPrivileges(username);

        if(userList != null) {
            // Set user roles on the session.
            List<String> roles = userList.getPrivileges();

            if(roles != null) {
                for(String role: roles) {
                    session.setAttribute(role, true);
                }
            }
        }
        return userList.getPrivileges().contains(AdminService.Privileges.ADMIN.getPrivilegeName()) ? "/admin/adminview.do" : "/p/search.do";
    }

    public void removeRemoteUser(final HttpSession session, String username) {
        session.removeAttribute(USERNAME);

        final AciParameters params = new AciParameters("userread");
        params.put("username", username);
        params.put("privilege", "*");
        UserDetailAndRoles userList = communityACI.executeAction(params, new UserAndRolesProcessor());

        List<String> roles = userList.getPrivileges();

        if(roles != null) {
            for(String role: roles) {
                session.removeAttribute(role);
            }
        }
    }

	public String getRemoteUser(final HttpSession session) {
		return (String) session.getAttribute(USERNAME);
	}

    public String getSessionUser(final HttpSession session) {
        final String user = getRemoteUser(session);
        if (user == null) {
            throw new IllegalArgumentException("No valid user found in this session");
        }

        return user;
    }
    
    public void setUserSecurityInfo(final HttpSession s, String sec) {
    	s.setAttribute(SECURITYINFO, sec);
    }
    
    public String getUserSecurityInfo(final HttpSession s) {
    	return (String) s.getAttribute(SECURITYINFO);
    }
}
