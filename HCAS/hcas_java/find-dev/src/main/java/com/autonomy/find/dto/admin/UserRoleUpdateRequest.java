package com.autonomy.find.dto.admin;

import java.util.List;


public class UserRoleUpdateRequest {

    private List<String> toAdd;
    private List<String> toRemove;
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String privilege) {
        this.username = privilege;
    }

    public List<String> getToRemove() {
        return toRemove;
    }

    public void setToRemove(List<String> toRemove) {
        this.toRemove = toRemove;
    }

    public List<String> getToAdd() {
        return toAdd;
    }

    public void setToAdd(List<String> toAdd) {
        this.toAdd = toAdd;
    }

}
