package com.autonomy.find.processors.admin;

import com.autonomy.aci.client.services.impl.AbstractStAXProcessor;
import com.autonomy.find.dto.UserDetailAndRoles;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.Date;


public class UserAndRolesProcessor extends AbstractStAXProcessor<UserDetailAndRoles> {

    public final static String UID = "autn:uid";
    public final static String USERNAME = "autn:username";
    public final static String LOCKED =  "autn:locked";
    public final static String LOCKED_LAST_TIME =  "autn:lockedlasttime";
    public final static String MAX_AGENTS =  "autn:maxagents";
    public final static String NUM_AGENTS =  "autn:numagents";
    public final static String LAST_LOGGED_IN =   "autn:lastloggedin";

    public final static String PRIV_NAME = "autn:name";
    public final static String PRIV_VALUE = "autn:value";

    public final static String USER_DETAILS_ACTION = "UserReadUserListDetails";

    public final static String USER_ELEMENT = "responsedata";
    public final static String USER_PRIV = "autn:privilege";

    public final static String DATABASES = "databases";

    public final static String RESPONSE = "response";
    public final static String RESPONSE_SUCCESS = "SUCCESS";

    @Override
    public UserDetailAndRoles process(XMLStreamReader xmlStreamReader) {
        String currentElement;
        try {
            while (xmlStreamReader.hasNext()) {
                int next = xmlStreamReader.next();

                if(next == XMLStreamReader.START_ELEMENT) {
                    currentElement = xmlStreamReader.getLocalName();

                    if(RESPONSE.equals(currentElement)) {
                        if(!RESPONSE_SUCCESS.toLowerCase().equals(xmlStreamReader.getElementText().toLowerCase())) {
                            // Error, no user found.
                            return null;
                        };
                    }

                    if(currentElement.equals(USER_ELEMENT)) {
                        return getUserDetails(xmlStreamReader, USER_ELEMENT);
                    }
                }
            }
        } catch (XMLStreamException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    protected UserDetailAndRoles getUserDetails(XMLStreamReader xmlStreamReader, String endElementName) throws XMLStreamException {
        UserDetailAndRoles user = new UserDetailAndRoles();
        boolean inUserPriv = false;
        boolean inUserDatabases = false;
        String priviName = "N/A";
        String userName;
        while (xmlStreamReader.hasNext()) {
            int next = xmlStreamReader.next();
            if(next == XMLStreamReader.END_ELEMENT) {

                if(USER_PRIV.equals(xmlStreamReader.getLocalName())) {
                    inUserPriv = false;
                    inUserDatabases = false;
                }

                if(endElementName.equals(xmlStreamReader.getLocalName())) {
                    // Found the end of the user, return.
                    return user;
                }
            }
            if(next == XMLStreamReader.START_ELEMENT) {

                if(USER_PRIV.equals(xmlStreamReader.getLocalName())) {
                    inUserPriv = true;
                }

                if(inUserDatabases && PRIV_VALUE.equals(xmlStreamReader.getLocalName())) {
                     user.addDatabases(xmlStreamReader.getElementText());
                }

                if(!inUserDatabases && inUserPriv && PRIV_NAME.equals(xmlStreamReader.getLocalName())) {
                    priviName = xmlStreamReader.getElementText();
                   if(DATABASES.equals(priviName)) {
                        inUserDatabases = true;
                   } else {
                       xmlStreamReader.next();
                       if("true".equals(xmlStreamReader.getElementText().toLowerCase())) {
                            user.addPrivilege(priviName);
                       }
                   }
                }
//
//              if(inUserDatabases  && PRIV_VALUE.equals(xmlStreamReader.getLocalName())) {
//                  Privs.add(xmlStreamReader.getElementText());
//              }

                if (UID.equals(xmlStreamReader.getLocalName())) {
                    user.setUid(xmlStreamReader.getElementText());
                }
                if (USERNAME.equals(xmlStreamReader.getLocalName())) {
                    userName = xmlStreamReader.getElementText();
                    user.setUsername(userName);
                }
                if (LOCKED.equals(xmlStreamReader.getLocalName())) {
                    user.setLocked(Boolean.parseBoolean(xmlStreamReader.getElementText()));
                }
                if (LOCKED_LAST_TIME.equals(xmlStreamReader.getLocalName())) {
                    user.setLockedLastTime(Integer.parseInt(xmlStreamReader.getElementText()));
                }
                if (MAX_AGENTS.equals(xmlStreamReader.getLocalName())) {
                    user.setMaxAgents(Integer.parseInt(xmlStreamReader.getElementText()));
                }
                if (NUM_AGENTS.equals(xmlStreamReader.getLocalName())) {
                    user.setNumagents(Integer.parseInt(xmlStreamReader.getElementText()));
                }
                if (LAST_LOGGED_IN.equals(xmlStreamReader.getLocalName())) {
                    Date loggedIn = new Date(Integer.parseInt(xmlStreamReader.getElementText()));
                    user.setLastLoggedIn(loggedIn);
                }
            }
        }
        return user;
    }



}
