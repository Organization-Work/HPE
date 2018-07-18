package com.autonomy.find.util;

public class LoginResult {
	
	private LoginStatus loginStatus;
	private String securityInfo;
	private String message;

	public LoginResult(LoginStatus s, String securityInfo) {
		this.loginStatus = s;
		this.securityInfo = securityInfo;
	}
	
	public LoginStatus getLoginStatus() {
		return this.loginStatus;
	}
	
	public String getSecurityInfo() {
		return this.securityInfo;
	}
	
	public void setMessage(String m) {
		this.message = m;
	}
	
	public String getMessage() {
		return this.message;
	}

}
