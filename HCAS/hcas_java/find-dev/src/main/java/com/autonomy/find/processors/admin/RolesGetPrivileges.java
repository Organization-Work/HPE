package com.autonomy.find.processors.admin;

/**
 * Created with IntelliJ IDEA.
 * User: nathan
 * Date: 22/04/14
 * Time: 18:24
 * To change this template use File | Settings | File Templates.
 */

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.util.HashSet;


public class RolesGetPrivileges extends CheckSuccessProcessor<HashSet<String>> {


    public final static String UID = "autn:uid";

    public final static String PRIV_NAME = "autn:name";
    public final static String PRIV_VALUE = "autn:value";

    public final static String DATABASES = "databases";

    @Override
    public HashSet<String> process(XMLStreamReader xmlStreamReader) {
        HashSet<String> privileges = new HashSet<String>();
        String priviName = null;
        try {
            while (xmlStreamReader.hasNext()) {
                int next = xmlStreamReader.next();

                if(next == XMLStreamReader.START_ELEMENT) {
                    if(PRIV_NAME.equals(xmlStreamReader.getLocalName())) {
                        priviName = xmlStreamReader.getElementText();
                    }

                    if(PRIV_VALUE.equals(xmlStreamReader.getLocalName())) {
                        if(priviName != null && !DATABASES.equals(priviName))
                            privileges.add(priviName);
                    }
                }
            }
        }  catch (XMLStreamException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return privileges;
    }
}
