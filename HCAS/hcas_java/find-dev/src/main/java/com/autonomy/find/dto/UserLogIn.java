package com.autonomy.find.dto;

import com.autonomy.aci.client.annotations.IdolDocument;
import com.autonomy.aci.client.annotations.IdolField;


@IdolDocument("responsedata")
public class UserLogIn {
    private boolean status;
    private String securityInfo;

    @IdolField("autn:authenticate")
    public void setAuthenticated(final boolean value) {
        this.status = value;
    }
    
    @IdolField("autn:securityinfo")
    public void setSecurityInfo(final String value) {
        this.securityInfo = value;
    }

    public boolean isAuthenticated() {
        return this.status;
    }
    
    public String getSecurityInfo() {
        return this.securityInfo;
    }

    public UserLogIn() {}
}