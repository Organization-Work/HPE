package com.autonomy.find.dto;

import com.google.gson.Gson;

import java.util.Date;


public class UserDetail {
    private String uid;
    private String username;
    private String firstname;
    private String lastname;
    private String email;
    private Boolean locked;
    private int lockedLastTime;
    private int maxAgents;
    private int numagents;
    private Date lastLoggedIn;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Boolean getLocked() {
        return locked == null ? false : locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public int getLockedLastTime() {
        return lockedLastTime;
    }

    public void setLockedLastTime(int lockedLastTime) {
        this.lockedLastTime = lockedLastTime;
    }

    public int getMaxAgents() {
        return maxAgents;
    }

    public void setMaxAgents(int maxAgents) {
        this.maxAgents = maxAgents;
    }

    public int getNumagents() {
        return numagents;
    }

    public void setNumagents(int numagents) {
        this.numagents = numagents;
    }

    public Date getLastLoggedIn() {
        return lastLoggedIn;
    }

    public void setLastLoggedIn(Date lastLoggedIn) {
        this.lastLoggedIn = lastLoggedIn;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    @Override
    public String toString() {
       return "{ username: " + getUsername() +
               ", " + "uid: " + getUid();

    }
}
