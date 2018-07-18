package com.autonomy.find.processors;

import com.autonomy.aci.actions.idol.query.Document;
import com.autonomy.aci.actions.idol.query.DocumentField;
import com.autonomy.aci.client.services.AciErrorException;
import com.autonomy.aci.client.services.ProcessorException;
import com.autonomy.aci.client.services.impl.AbstractStAXProcessor;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;
import java.util.Date;
import java.util.LinkedList;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/processors/HierachicalDocumentProcessor.java#3 $
 * <p/>
 * Copyright (c) 2013, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/05/28 $
 */
public class HierachicalDocumentProcessor extends AbstractStAXProcessor<Document> {

	private static final Logger LOGGER = LoggerFactory.getLogger(HierachicalDocumentProcessor.class);

	private static final long serialVersionUID = -251771053745506251L;

	/**
	 * Processes the document content fields.
	 *
	 * @param idolResponse The <tt>XMLStreamReader</tt> to process
	 * @param document The <tt>Document</tt> object to store the element values in
	 * @throws javax.xml.stream.XMLStreamException if something went wrong during the parsing
	 */
	private void processContentFields(final XMLStreamReader idolResponse, final Document document) throws XMLStreamException {
		// Skip the <DOCUMENT> (or whatever it's configured as) tag...
		forwardToNextStartElement(idolResponse);

		final LinkedList<String> words = new LinkedList<>();

		int eventType = -1;
		boolean skipFetch = false;

		// Proceed to the first DOCUMENT child element...
		while(idolResponse.hasNext()) {
			if (skipFetch) {
				skipFetch = false;
			}
			else {
				eventType = idolResponse.next();
			}

			if(XMLEvent.START_ELEMENT == eventType) {
				// Create a new DocumentField to contain this element...
				final DocumentField field = new DocumentField();
				final String name = idolResponse.getLocalName();
				words.addLast(name);
				field.setName(StringUtils.join(words, "/"));

				// Get any attributes...
				final int attributeCount = idolResponse.getAttributeCount();
				for(int i = 0; i < attributeCount; i++) {
					// Add an attribute...
					field.addAttribute(idolResponse.getAttributeLocalName(i), idolResponse.getAttributeValue(i));
				}

				// We can't use getElementText() if the node has children in it, so we need to check subsequent events
				skipFetch = true;
				eventType = idolResponse.next();

				final StringBuilder content = new StringBuilder();

				while(eventType == XMLStreamConstants.CHARACTERS
					|| eventType == XMLStreamConstants.CDATA
					|| eventType == XMLStreamConstants.SPACE
					|| eventType == XMLStreamConstants.ENTITY_REFERENCE) {
					content.append(idolResponse.getText());
					eventType = idolResponse.next();
				}

				// Get the character data...
				field.setValue(content.toString());

				// Add the field...
				document.addDocumentField(field);
			}
			// When we get to </autn:content> we have finished with the document content fields...
			else if((XMLEvent.END_ELEMENT == eventType)) {
				if (words.isEmpty()) {
					break;
				}

				words.removeLast();
			}
		}
	}

	// Copypasted from DocumentProcessor. It's be nice if processContentFields was protected so we could override it
	@Override
	public Document process(final XMLStreamReader idolResponse) throws AciErrorException, ProcessorException {
		LOGGER.trace("process() called...");

		// Make sure the XMLStreamReader is on a <autn:hit> element...
		if(!"autn:hit".equals(idolResponse.getLocalName())) {
			LOGGER.error("DocumentProcessor requires the XMLStreamReader to be on a <autn:hit> element, was on {}...", idolResponse.getLocalName());
			throw new IllegalStateException("DocumentProcessor requires the XMLStreamReader to be on a <autn:hit> element, was on a " + idolResponse.getLocalName() + " element.");
		}

		try {
			// This is the document we'll fill out...
			final Document document = new Document();

			// We have to iterate as the fields are not in any guaranteed order...
			while(idolResponse.hasNext()) {
				final int eventType = idolResponse.next();

				if(XMLEvent.START_ELEMENT == eventType) {
					if("autn:reference".equalsIgnoreCase(idolResponse.getLocalName())) {
						document.setReference(idolResponse.getElementText());
					}
					else if("autn:id".equalsIgnoreCase(idolResponse.getLocalName())) {
						document.setId(NumberUtils.toInt(idolResponse.getElementText()));
					}
					else if("autn:section".equalsIgnoreCase(idolResponse.getLocalName())) {
						document.setSection(NumberUtils.toInt(idolResponse.getElementText()));
					}
					else if("autn:weight".equalsIgnoreCase(idolResponse.getLocalName())) {
						document.setWeight(NumberUtils.toFloat(idolResponse.getElementText()));
					}
					else if("autn:links".equalsIgnoreCase(idolResponse.getLocalName())) {
						document.setLinks(idolResponse.getElementText());
					}
					else if("autn:database".equalsIgnoreCase(idolResponse.getLocalName())) {
						document.setDatabase(idolResponse.getElementText());
					}
					else if("autn:title".equalsIgnoreCase(idolResponse.getLocalName())) {
						document.setTitle(idolResponse.getElementText());
					}
					else if("autn:summary".equals(idolResponse.getLocalName())) {
						document.setSummary(idolResponse.getElementText());
					}
					else if("autn:content".equals(idolResponse.getLocalName())) {
						// Process the document content fields...
						processContentFields(idolResponse, document);
					}
					else if("autn:baseid".equalsIgnoreCase(idolResponse.getLocalName())) {
						document.setBaseId(NumberUtils.toInt(idolResponse.getElementText()));
					}
					else if("autn:date".equalsIgnoreCase(idolResponse.getLocalName())) {
						document.setDate(new Date(NumberUtils.toInt(idolResponse.getElementText()) * 1000L));
					}
					else if("autn:datestring".equalsIgnoreCase(idolResponse.getLocalName())) {
						document.setDateString(idolResponse.getElementText());
					}
					else if("autn:expiredate".equalsIgnoreCase(idolResponse.getLocalName())) {
						document.setExpireDate(new Date(NumberUtils.toInt(idolResponse.getElementText()) * 1000L));
					}
					else if("autn:language".equalsIgnoreCase(idolResponse.getLocalName())) {
						document.setLanguage(idolResponse.getElementText());
					}
					else if("autn:languagetype".equalsIgnoreCase(idolResponse.getLocalName())) {
						document.setLanguageType(idolResponse.getElementText());
					}
					else if("autn:languageencoding".equalsIgnoreCase(idolResponse.getLocalName())) {
						document.setLanguageEncoding( idolResponse.getElementText());
					}
				}
				else if((XMLEvent.END_ELEMENT == eventType) && "autn:hit".equals(idolResponse.getLocalName())) {
					// We are done with this <autn:hit>...
					break;
				}
			}

			// Return the filled out document object...
			return document;
		}
		catch(XMLStreamException xmlse) {
			throw new ProcessorException("Unable to create Document object from <autn:hit> child elements.", xmlse);
		}
	}
}
