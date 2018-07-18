package com.autonomy.find.processors;

import com.autonomy.aci.client.services.ProcessorException;
import com.autonomy.aci.client.services.impl.AbstractStAXProcessor;
import com.autonomy.aci.client.services.impl.ErrorProcessor;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.util.HashSet;
import java.util.Set;

public class ReferenceProcessor extends AbstractStAXProcessor<Set<String>> {
    public ReferenceProcessor() {
        setErrorProcessor(new ErrorProcessor());
    }

    @Override
    public Set<String> process(final XMLStreamReader xmlStreamReader) {
        try {
            if(isErrorResponse(xmlStreamReader)) {
                // Process the error response and throw an exception...
                processErrorResponse(xmlStreamReader);
            }

            return searchForHit(xmlStreamReader);
        } catch (XMLStreamException e) {
            throw new ProcessorException("Error reading XML", e);
        }
    }

    private Set<String> searchForHit(final XMLStreamReader xmlStreamReader) throws XMLStreamException {
        final Set<String> result = new HashSet<String>();
        while (xmlStreamReader.hasNext()) {
            if (xmlStreamReader.next() == XMLEvent.START_ELEMENT &&
                    "autn:hit".equals(xmlStreamReader.getLocalName())) {
                result.add(searchForReference(xmlStreamReader));
            }
        }
        return result;
    }

    private String searchForReference(final XMLStreamReader xmlStreamReader) throws XMLStreamException {
        while (xmlStreamReader.hasNext()) {
            if (xmlStreamReader.next() == XMLEvent.START_ELEMENT) {
                if ("autn:reference".equals(xmlStreamReader.getLocalName())) {
                    return xmlStreamReader.getElementText();
                }
            }
        }
        throw new ProcessorException("autn:reference not found");
    }
}
