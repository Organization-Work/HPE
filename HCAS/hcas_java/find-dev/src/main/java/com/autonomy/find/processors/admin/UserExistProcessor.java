package com.autonomy.find.processors.admin;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class UserExistProcessor extends CheckSuccessProcessor<Boolean> {

    final static String EXISTS_ELEMENT =  "autn:userexists";
    @Override
    public Boolean process(XMLStreamReader xmlStreamReader) {

        try {
            while (xmlStreamReader.hasNext()) {
                int next = xmlStreamReader.next();

                if(next == XMLStreamReader.START_ELEMENT) {
                    if(isResponseElement(xmlStreamReader.getLocalName())) {
                        if(!isSuccess(xmlStreamReader.getElementText())) {
                            return false;
                        }
                    }
                    if(isElement(EXISTS_ELEMENT, xmlStreamReader.getLocalName())) {
                        return Boolean.parseBoolean(xmlStreamReader.getElementText());
                    }
                }
            }
        } catch (XMLStreamException e) {
            return false;
        }
        return false;
    }
}
