package com.autonomy.find.processors.admin;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.HashSet;


public abstract class ListProcessor extends  CheckSuccessProcessor<HashSet<String>> {

    public abstract String getListElement();

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
                    if(isElement(getListElement(), xmlStreamReader.getLocalName())) {
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
