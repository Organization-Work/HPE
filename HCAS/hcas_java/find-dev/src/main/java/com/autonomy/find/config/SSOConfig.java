package com.autonomy.find.config;

import lombok.Data;

import org.springframework.stereotype.Service;

@Service
@Data
public final class SSOConfig {
    public String getCasServerLoginUrl() {
		return casServerLoginUrl;
	}
	public void setCasServerLoginUrl(String casServerLoginUrl) {
		this.casServerLoginUrl = casServerLoginUrl;
	}
	public void setCasClientService(String casClientService) {
		this.casClientService = casClientService;
	}
	public void setCasServerLogoutUrl(String casServerLogoutUrl) {
		this.casServerLogoutUrl = casServerLogoutUrl;
	}
	private String casClientService;
    private String casServerLoginUrl;
    private String casServerLogoutUrl;
	public String getCasServerLogoutUrl() {
		return casServerLogoutUrl;
	}
	public String getCasClientService() {
		return casClientService;
	}
}
