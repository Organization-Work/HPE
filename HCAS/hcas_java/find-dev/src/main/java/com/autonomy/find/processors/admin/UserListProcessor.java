package com.autonomy.find.processors.admin;

import com.autonomy.find.dto.UserDetail;
import com.autonomy.find.dto.UserListDetails;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

@SuppressWarnings("serial")
public class UserListProcessor extends UserBaseProcessor {


    public final static String USER_DETAILS_ACTION = "UserReadUserList";

    public final static String USER_ELEMENT = "autn:user";

    @Override
    public UserListDetails process(XMLStreamReader xmlStreamReader) {
        String currentElement = "";
        UserListDetails userList = new UserListDetails();
        try {
            while (xmlStreamReader.hasNext()) {
                int next = xmlStreamReader.next();
                if(next == XMLStreamReader.START_ELEMENT) {
                    currentElement = xmlStreamReader.getLocalName();
                    if(currentElement.equals(USER_ELEMENT)) {
                        UserDetail userDetails = new UserDetail();
                        String username = xmlStreamReader.getElementText();
                        userDetails.setUsername(username);
                        userList.getUsers().put(userDetails.getUsername(), userDetails);
                    }
                }
            }
        } catch (XMLStreamException e) {
           return null;
        }
        return userList;
    }

}