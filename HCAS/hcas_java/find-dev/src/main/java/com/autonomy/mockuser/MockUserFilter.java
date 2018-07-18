package com.autonomy.mockuser;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/mockuser/MockUserFilter.java#2 $
 * <p/>
 * Copyright (c) 2012, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/11/08 $
 */
public class MockUserFilter implements Filter {
	private String mockUser;


	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {
		mockUser = filterConfig.getInitParameter("mockUser");
	}

	@Override
	public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
		chain.doFilter(new HttpServletRequestWrapper((HttpServletRequest)request){
			@Override
			public String getRemoteUser() {
				return mockUser;
			}
		}, response);
	}

	@Override
	public void destroy() {}
}
