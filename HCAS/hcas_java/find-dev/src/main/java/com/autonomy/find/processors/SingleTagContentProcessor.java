package com.autonomy.find.processors;

import com.autonomy.aci.client.services.ProcessorException;
import com.autonomy.aci.client.services.impl.AbstractStAXProcessor;
import org.apache.commons.lang.Validate;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

public class SingleTagContentProcessor extends AbstractStAXProcessor<String> {

    private final String fieldName;

    public SingleTagContentProcessor(final String fieldName) {
        Validate.notNull(fieldName);

        this.fieldName = fieldName;
    }

    @Override
    public String process(final XMLStreamReader xmlStreamReader) {
        try {
            while (xmlStreamReader.hasNext()) {
                if (xmlStreamReader.next() == XMLEvent.START_ELEMENT &&
                        fieldName.equals(xmlStreamReader.getLocalName())) {
                    return xmlStreamReader.getElementText();
                }
            }
            throw new ProcessorException("No content found.");
        } catch (XMLStreamException e) {
            throw new ProcessorException("Error reading XML", e);
        }
    }
}
