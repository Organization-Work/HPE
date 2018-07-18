package com.autonomy.idolview;

import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.find.processors.admin.UserExistProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/idolview/UserFilter.java#4 $
 * <p/>
 * Copyright (c) 2012, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: njones $ on $Date: 2014/04/23 $
 */
public class UserFilter implements javax.servlet.Filter {
    private static final Logger LOG = LoggerFactory.getLogger(UserFilter.class);

    @Autowired
    @Qualifier("communityAciService")
    private AciService communityACI;

    private boolean enabled;

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        // N
    }

    @Override
    public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain filterChain) throws IOException, ServletException {
        final String user = ((HttpServletRequest) req).getRemoteUser();

        if (user == null) {
            ((HttpServletResponse) res).sendError(500, "Server authentication not configured");
            return;
        }
        else if (!userExists(user)) {
            LOG.info("Rejecting user {} with 403 response", user);
            ((HttpServletResponse) res).sendError(403);
            return;
        }

        filterChain.doFilter(req, res);
    }

    @Override
    public void destroy() {

    }

    public Boolean userExists(String username) {
        final AciParameters params = new AciParameters("UserExist");
        params.add("username", username);
        return communityACI.executeAction(params, new UserExistProcessor());
    }
}
