package com.autonomy.find.processors;

import com.autonomy.aci.client.services.ProcessorException;
import com.autonomy.aci.client.services.impl.AbstractStAXProcessor;
import com.autonomy.aci.client.services.impl.ErrorProcessor;
import com.autonomy.find.util.StateResult;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.util.HashSet;
import java.util.Set;

public class StoreStateProcessor extends AbstractStAXProcessor<StateResult> {
    public StoreStateProcessor() {
        setErrorProcessor(new ErrorProcessor());
    }

    @Override
    public StateResult process(final XMLStreamReader xmlStreamReader) {
        try {
            if(isErrorResponse(xmlStreamReader)) {
                // Process the error response and throw an exception...
                processErrorResponse(xmlStreamReader);
            }

            return searchForState(xmlStreamReader);
        } catch (XMLStreamException e) {
            throw new ProcessorException("Error reading XML", e);
        }
    }

    private StateResult searchForState(final XMLStreamReader xmlStreamReader) throws XMLStreamException {
        String stateId = null;
        String numhits = null;

        while (xmlStreamReader.hasNext()) {
            if (xmlStreamReader.next() == XMLEvent.START_ELEMENT) {
                if ("autn:state".equals(xmlStreamReader.getLocalName())) {
                    stateId = xmlStreamReader.getElementText();
                } else if ("autn:numhits".equals(xmlStreamReader.getLocalName())) {
                    numhits = xmlStreamReader.getElementText();
                }

                if (stateId != null && numhits != null) {
                   return new StateResult(stateId, Integer.valueOf(numhits));
                }
            }
        }

        return null;
    }
}
