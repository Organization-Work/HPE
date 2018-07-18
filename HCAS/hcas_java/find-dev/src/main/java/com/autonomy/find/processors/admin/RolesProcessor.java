package com.autonomy.find.processors.admin;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.HashSet;

public class RolesProcessor extends CheckSuccessProcessor<HashSet<String>> {

    public static final String ROLE_ELEMENT = "autn:role";

    @Override
    public HashSet<String> process(XMLStreamReader xmlStreamReader) {
        HashSet<String> roles = new HashSet<>();

        try {
            while (xmlStreamReader.hasNext()) {
                int next = xmlStreamReader.next();

                if(next == XMLStreamReader.START_ELEMENT) {
                  if(isResponseElement(xmlStreamReader.getLocalName())) {
                      if(!isSuccess(xmlStreamReader.getElementText())) {
                         return null;
                      }
                  }
                     if(isElement(ROLE_ELEMENT, xmlStreamReader.getLocalName())) {
                        roles.add(xmlStreamReader.getElementText());
                     }
                }
            }
        } catch (XMLStreamException e) {
            return null;
        }
        return roles;
    }
}
