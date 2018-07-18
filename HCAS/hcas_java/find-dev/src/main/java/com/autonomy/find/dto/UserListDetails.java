package com.autonomy.find.dto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


public class UserListDetails {
    private Map<String, UserDetail> users = new HashMap<>();

    public void addUser(UserDetail user) {
        users.put(user.getUsername(), user);
    }

    public void setUsers(Map<String, UserDetail> users) {
        this.users = users;
    }

    public Map<String, UserDetail> getUsers() {
        return users;
    }

    public UserDetail getUserDetailsByUsername(String username) {
        if(users.containsKey(username)) {
            return users.get(username);
        }
        return null;
    }
    public HashSet<String> getUsersUsernames() {
        HashSet<String> usernames = new HashSet<>();
        for (String key: users.keySet()) {

            usernames.add(key);
        }

        return usernames;
    }

    public String toJson() {
        String jsonString = "[";
        for (UserDetail details : users.values()) {
            String json = details.toJson();
            if(json == null) {
                json = "{}";
            }
            jsonString += (json + ",");
        }

        if (jsonString.length() > 0 && jsonString.charAt(jsonString.length()-1)==',') {
            jsonString = jsonString.substring(0, jsonString.length()-1);
        }

        jsonString += "]";

        return jsonString;
    }
}
