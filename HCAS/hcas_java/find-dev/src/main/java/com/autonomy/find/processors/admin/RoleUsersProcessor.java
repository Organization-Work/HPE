package com.autonomy.find.processors.admin;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.HashSet;

public class RoleUsersProcessor extends CheckSuccessProcessor<HashSet<String>> {

    public final static String USER_ELEMENT = "autn:user";

    @Override
    public HashSet<String> process(XMLStreamReader xmlStreamReader) {
        String currentElement;
        HashSet<String> userList = new HashSet<>();
        try {
            while (xmlStreamReader.hasNext()) {
                int next = xmlStreamReader.next();
                if(next == XMLStreamReader.START_ELEMENT) {
                    currentElement = xmlStreamReader.getLocalName();
                    if(currentElement.equals(USER_ELEMENT)) {
                       userList.add( xmlStreamReader.getElementText());
                    }
                }
            }
        } catch (XMLStreamException e) {
            return null;
        }
        return userList;
    }

}