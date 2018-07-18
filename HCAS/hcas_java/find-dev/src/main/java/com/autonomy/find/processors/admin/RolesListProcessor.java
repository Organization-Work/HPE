package com.autonomy.find.processors.admin;

public class RolesListProcessor extends ListProcessor {

    public static final String ROLE_ELEMENT = "autn:role";

    @Override
    public String getListElement() {
        return ROLE_ELEMENT;
    }

}
