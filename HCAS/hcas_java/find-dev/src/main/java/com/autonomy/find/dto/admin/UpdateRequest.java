package com.autonomy.find.dto.admin;

import java.util.List;

public class UpdateRequest {

    private List<String> toAdd;
    private List<String> toRemove;
    private String itemName;

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
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
