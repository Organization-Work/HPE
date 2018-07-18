package com.autonomy.find.processors;

/*
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/processors/FieldDependentTagValuesProcessor.java#5 $
 *
 * Copyright (c) 2013, Autonomy Systems Ltd.
 *
 * Last modified by $Author: tungj $ on $Date: 2013/11/13 $ 
 */

import com.autonomy.aci.client.services.ProcessorException;
import com.autonomy.aci.client.services.impl.AbstractStAXProcessor;
import com.autonomy.aci.client.services.impl.ErrorProcessor;
import com.autonomy.find.dto.FieldPair;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.List;

public class FieldDependentTagValuesProcessor extends AbstractStAXProcessor<List<FieldPair>> {
    public FieldDependentTagValuesProcessor() {
        setErrorProcessor(new ErrorProcessor());
    }

    @Override
    public List<FieldPair> process(final XMLStreamReader reader) {
        final List<String> fieldNames = new ArrayList<>();
        final List<FieldPair> pairs = new ArrayList<>();

        FieldPair lastFieldPair = null;
        int numFields = 0;

        try {
            if(isErrorResponse(reader)) {
                // Process the error response and throw an exception...
                processErrorResponse(reader);
            }

            while(reader.hasNext()) {
                final int eventType = reader.next();

// e.g. to parse http://tungj-notebook:9100/a=getquerytagvalues&text=*&fieldname=/XML/PATIENT/ETHNICITY,/XML/PATIENT/RELIGION&documentcount=true&databasematch=patients&FieldDependence=true&documentcount=true
                if(XMLEvent.START_ELEMENT == eventType) {
                    switch (reader.getLocalName()) {
                        case "autn:name":
                            fieldNames.add(reader.getElementText());
                            numFields = fieldNames.size();
                            break;
                        case "autn:value":
                            if (numFields > 2) {
                                throw new ProcessorException("Expected at most two fields");
                            }
                            else if (numFields < 2) {
                                // one or less fields has results, no pairs
                                return pairs;
                            }
                            lastFieldPair = new FieldPair(fieldNames.get(0), fieldNames.get(1));
                            pairs.add(lastFieldPair);
                            lastFieldPair.setCount(Integer.parseInt(reader.getAttributeValue(null, "count")));
                            lastFieldPair.setPrimaryValue(reader.getElementText());
                            break;
                        case "autn:subvalue":
                            if (lastFieldPair == null) {
                                throw new ProcessorException("autn:subvalue should only occur after an autn:value tag");
                            }
                            lastFieldPair.setSecondaryValue(reader.getElementText());
                            break;
                    }
                }
            }
        } catch (XMLStreamException xse) {
            throw new ProcessorException("Error while parsing field dependent tag values", xse);
        }

        return pairs;
    }
}
