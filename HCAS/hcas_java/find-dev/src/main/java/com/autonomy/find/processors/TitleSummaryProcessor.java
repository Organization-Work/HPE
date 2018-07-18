package com.autonomy.find.processors;

import com.autonomy.aci.client.services.ProcessorException;
import com.autonomy.aci.client.services.impl.AbstractStAXProcessor;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TitleSummaryProcessor extends AbstractStAXProcessor<List<Map<String, String>>> {

    @Override
    public List<Map<String, String>> process(final XMLStreamReader xmlStreamReader) {
        try {
            return searchForHit(xmlStreamReader);
        } catch (XMLStreamException e) {
            throw new ProcessorException("Error reading XML", e);
        }
    }

    private List<Map<String, String>> searchForHit(final XMLStreamReader xmlStreamReader) throws XMLStreamException {
        final List<Map<String, String>> result = new ArrayList<>();
        while (xmlStreamReader.hasNext()) {
            if (xmlStreamReader.next() == XMLEvent.START_ELEMENT &&
                    "autn:hit".equals(xmlStreamReader.getLocalName())) {
                result.add(searchForMap(xmlStreamReader));
            }
        }
        return result;
    }

    private Map<String, String> searchForMap(final XMLStreamReader xmlStreamReader) throws XMLStreamException {
        final Map<String, String> result = new HashMap<>();
        while (xmlStreamReader.hasNext()) {
            if (xmlStreamReader.next() == XMLEvent.START_ELEMENT) {
                searchForKey("title", "autn:title", result, xmlStreamReader);
                searchForKey("summary", "autn:summary", result, xmlStreamReader);
                if (result.containsKey("title") && result.containsKey("summary")) {
                    return result;
                }
            }
        }
        throw new ProcessorException("");
    }

    private Map<String, String> searchForKey(final String key, final String name, final Map<String, String> result, final XMLStreamReader xmlStreamReader) throws XMLStreamException {
        if (name.equals(xmlStreamReader.getLocalName())) {
            result.put(key, xmlStreamReader.getElementText());
        }
        return result;
    }

}
