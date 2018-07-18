package com.autonomy.find.processors;

import com.autonomy.aci.client.services.ProcessorException;
import com.autonomy.aci.client.services.impl.AbstractStAXProcessor;
import com.autonomy.aci.client.services.impl.ErrorProcessor;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

public class XmlContentProcessor  extends AbstractStAXProcessor<String> {
    private final boolean excludeContentRoot;

    public XmlContentProcessor() {
        this(false);
    }

    public XmlContentProcessor(final boolean excludeContentRoot) {
        setErrorProcessor(new ErrorProcessor());
        this.excludeContentRoot = excludeContentRoot;
    }

    @Override
    public String process(final XMLStreamReader xmlStreamReader) {
        try {
            if(isErrorResponse(xmlStreamReader)) {
                // Process the error response and throw an exception...
                processErrorResponse(xmlStreamReader);
            }
            return searchForContent(xmlStreamReader);
        } catch (XMLStreamException e) {
            throw new ProcessorException("Error reading XML", e);
        }
    }


    private String searchForContent(final XMLStreamReader xmlStreamReader) throws XMLStreamException {
        final StringBuilder xmlBuilder = new StringBuilder();

        while (xmlStreamReader.hasNext()) {
            if (xmlStreamReader.next() == XMLEvent.START_ELEMENT &&
                    "autn:content".equals(xmlStreamReader.getLocalName())) {
                if (!excludeContentRoot) {
                    xmlBuilder.append("<").append(xmlStreamReader.getLocalName()).append(">");
                }
                xmlBuilder.append(getContentXml(xmlStreamReader)).append("\n");
                if (!excludeContentRoot) {
                    xmlBuilder.append("</").append(xmlStreamReader.getLocalName()).append(">");
                }
                break;
            }
        }
        return xmlBuilder.toString();
    }

    private String getContentXml(final XMLStreamReader xmlStreamReader) throws XMLStreamException {
        final StringBuilder contentBuilder = new StringBuilder();
        while (xmlStreamReader.hasNext()) {
            final int event = xmlStreamReader.next();
            if (event == XMLEvent.START_ELEMENT) {
                contentBuilder.append("<").append(xmlStreamReader.getLocalName()).append(">");
            } else if (event == XMLEvent.END_ELEMENT) {
                if ("autn:content".equals(xmlStreamReader.getLocalName())) {
                    return contentBuilder.toString();
                } else {
                    contentBuilder.append("</").append(xmlStreamReader.getLocalName()).append(">");
                }

            } else if (event == XMLEvent.CHARACTERS) {
                if (xmlStreamReader.hasText()) {
                    contentBuilder.append(xmlStreamReader.getText());
                }
            }
        }
        throw new ProcessorException("autn:reference not found");
    }

}
