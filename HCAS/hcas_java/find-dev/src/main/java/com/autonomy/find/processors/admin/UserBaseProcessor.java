package com.autonomy.find.processors.admin;

import com.autonomy.aci.client.services.impl.AbstractStAXProcessor;
import com.autonomy.find.dto.UserDetail;
import com.autonomy.find.dto.UserListDetails;
import com.autonomy.find.services.WebAppUserService;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.Date;

/**
 * Created with IntelliJ IDEA.
 * User: nathan
 * Date: 26/03/14
 * Time: 14:40
 * To change this template use File | Settings | File Templates.
 */
public class UserBaseProcessor extends AbstractStAXProcessor<UserListDetails> {

    public final static String UID = "autn:uid";
    public final static String USERNAME = "autn:username";
    public final static String LOCKED =  "autn:locked";
    public final static String LOCKED_LAST_TIME =  "autn:lockedlasttime";
    public final static String MAX_AGENTS =  "autn:maxagents";
    public final static String NUM_AGENTS =  "autn:numagents";
    public final static String LAST_LOGGED_IN =   "autn:lastloggedin";

    protected UserDetail getUserDetails(XMLStreamReader xmlStreamReader, String endElementName) throws XMLStreamException {
        UserDetail user = new UserDetail();
        String userName = "";
        while (xmlStreamReader.hasNext()) {
            int next = xmlStreamReader.next();
            if(next == XMLStreamReader.END_ELEMENT) {
                if(endElementName.equals(xmlStreamReader.getLocalName())) {
                    // Found the end of the user, return.
                    // A new user will not have a locked data this will default it to false.
                    if(user.getEmail() == null) user.setEmail("Unknown");
                    if(user.getFirstname() == null) user.setFirstname("Unknown");
                    if(user.getLastname() == null) user.setLastname("Unknown");
                    user.setLocked(user.getLocked());
                    return user;
                }
            }
            if(next == XMLStreamReader.START_ELEMENT) {
                if(WebAppUserService.EMAIL.toLowerCase().equals(xmlStreamReader.getLocalName().toLowerCase())) {
                   user.setEmail(xmlStreamReader.getElementText());
                }
                if( WebAppUserService.FIRST_NAME.toLowerCase().equals(xmlStreamReader.getLocalName().toLowerCase())) {
                   user.setFirstname(xmlStreamReader.getElementText());
                }
                if(WebAppUserService.SECOND_NAME.toLowerCase().equals(xmlStreamReader.getLocalName().toLowerCase())) {
                   user.setLastname(xmlStreamReader.getElementText());
                }
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
                    Date loggedIn = new Date(Long.parseLong(xmlStreamReader.getElementText()) * 1000);
                    user.setLastLoggedIn(loggedIn);
                }
            }
        }

        return user;
    }

    @Override
    public UserListDetails process(XMLStreamReader xmlStreamReader) {
        return null;
    }
}
