package com.autonomy.find.dto;

import com.google.gson.Gson;

import java.util.*;


public class UserDetailAndRoles {


    private final String DATABASES = "databases";


    private String uid;
    private String username;
    private Boolean locked;
    private int lockedLastTime;
    private int maxAgents;
    private int numagents;
    private Date lastLoggedIn;
    private List<String> privileges = new ArrayList<>();
    private List<String> databases = new ArrayList<>();

    public List<String> getDatabases() {
        return databases;
    }

    public void addDatabases(String databaseName) {
        this.databases.add(databaseName);
    }

    public boolean hasPrivilege(String privilege) {
        return privileges.contains(privilege);
    }

    public List<String> getPrivileges() {
        return privileges;
    }

//    public HashSet<String> getDatabases() {
//        HashSet<String> databases = new HashSet<>();
//
//        for(Map.Entry<String, HashSet<String>> userPrivileges : privileges.entrySet()) {
//            if(userPrivileges.getKey().equals(DATABASES)) {
//                for (String userPrivilege : userPrivileges.getValue()) {
//                    databases.add(userPrivilege);
//                }
//            }
//        }
//        return databases;
//    }

    public void addPrivilege(String privilageName) {
        privileges.add(privilageName);
    }

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
        return locked;
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
