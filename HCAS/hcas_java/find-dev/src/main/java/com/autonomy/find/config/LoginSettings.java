package com.autonomy.find.config;

import lombok.Data;

@Data
public class LoginSettings {
    public boolean isSso() {
		return sso;
	}
	public void setSso(boolean sso) {
		this.sso = sso;
	}
	public boolean isUserpass() {
		return userpass;
	}
	public void setUserpass(boolean userpass) {
		this.userpass = userpass;
	}
	public boolean isCanRegister() {
		return canRegister;
	}
	public void setCanRegister(boolean canRegister) {
		this.canRegister = canRegister;
	}
	public boolean isCookieMessageDisplayed() {
		return cookieMessageDisplayed;
	}
	public void setCookieMessageDisplayed(boolean cookieMessageDisplayed) {
		this.cookieMessageDisplayed = cookieMessageDisplayed;
	}
	public String getCookieMessage() {
		return cookieMessage;
	}
	public void setCookieMessage(String cookieMessage) {
		this.cookieMessage = cookieMessage;
	}
	public void setCommunityRepositoryType(String communityRepositoryType) {
		this.communityRepositoryType = communityRepositoryType;
	}

	private boolean sso;
    private boolean userpass;
    private boolean canRegister;
    private boolean cookieMessageDisplayed;
    private String cookieMessage;
    private String communityRepositoryType = "autonomy";

    public boolean ssoLoginAllowed() { return this.sso; }
    public boolean userPassLoginAllowed() { return this.userpass; }
    public boolean registrationAllowed() { return this.canRegister; }
    public String  getCommunityRepositoryType() {return this.communityRepositoryType; }
    
    public boolean isAutonomy() {
		String repository = this.communityRepositoryType;
		if ("autonomy".equals(repository)) {
			return true;
		}
		else {
			return false;
		}
	}
}