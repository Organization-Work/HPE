package com.autonomy.find.processors.admin;

public class PrivilegeListProcessor extends ListProcessor {

    public static final String ROLE_ELEMENT = "autn:privilege";

    @Override
    public String getListElement() {
        return ROLE_ELEMENT;
    }

}
