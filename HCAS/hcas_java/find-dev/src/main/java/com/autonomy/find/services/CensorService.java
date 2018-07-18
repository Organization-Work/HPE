package com.autonomy.find.services;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamSource;
import org.springframework.stereotype.Service;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.autonomy.aci.actions.idol.query.Document;
import com.autonomy.aci.actions.idol.query.DocumentField;
import com.autonomy.find.config.DisplayField;
import com.autonomy.find.config.SearchConfig;
import com.autonomy.find.config.SearchView;
import com.autonomy.find.dto.ResultDocument;

import lombok.Cleanup;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/services/CensorService.java#7 $
 * <p/>
 * Copyright (c) 2013, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/11/04 $
 */
@Service
public class CensorService {

	public static final String REDACTED = "\u27EAredacted\u27EB";

	@Autowired
 	private SearchConfig config;

	@Autowired
	@Resource(name = "allowedUsers")
	private InputStreamSource source;

	@Value("${discoverSearch.censor.redactionField}")
	private String redactionField;

	private Set<String> whitelist;

	private Set<String> permittedFields = new HashSet<>();

	private String[] redactionIDOLFields;

	private Set<String> permittedIDOLFields = new HashSet<>();

	private XPathExpression xRedaction;

	@Value("${discoverSearch.censor.permittedFields}")
	public void setPermittedFields(final String fields){
		permittedFields.addAll(Arrays.asList(fields.split(",")));
	}

	@PostConstruct
	private void postConstruct() throws IOException {
		@Cleanup
		InputStream stream = source.getInputStream();
		whitelist = new HashSet<String>(IOUtils.readLines(stream, "UTF-8"));

		if (StringUtils.isNotBlank(redactionField)) {
			Map<String,DisplayField> fields = new HashMap<String, DisplayField>();
			Map<String, SearchView> searchViews = config.getSearchViews();
			for(Map.Entry<String, SearchView> entry : searchViews.entrySet()) {				
				fields.putAll(entry.getValue().getDisplayFields());
			}
			//final Map<String,DisplayField> fields = config.getDisplayFields();

			final DisplayField redaction = fields.get(redactionField);
			if (redaction != null) {
				redactionIDOLFields = redaction.getFields();
				try {
					xRedaction = XPathFactory.newInstance().newXPath().compile(StringUtils.join(redactionIDOLFields, '|'));
				} catch (final XPathExpressionException e) {
					throw new Error("Cannot parse the redaction IDOL field as an XPath expression", e);
				}
			}

			for (final String field : permittedFields) {
				final DisplayField permittedField = fields.get(field);
				if (permittedField != null) {
					permittedIDOLFields.addAll(Arrays.asList(permittedField.getFields()));
				}
			}
		}
	}

	public boolean censor(final Node node){
		if (xRedaction == null) {
			return false;
		}
		try {
			final NodeList redactCheck = (NodeList) xRedaction.evaluate(node, XPathConstants.NODESET);

			for (int ii = redactCheck.getLength() - 1; ii >= 0; --ii) {
				if (!whitelist.contains(redactCheck.item(ii).getTextContent())) {
					return true;
				}
			}
		} catch (final XPathExpressionException xee) {
			// do nothing
		}

		return false;
	}

	public Document censor(final Document doc) {
		if (redactionIDOLFields != null) {
			final Map<String, List<DocumentField>> fields = doc.getDocumentFields();
			final List<DocumentField> mentionedUsers = new ArrayList<>();

            for (final String field : redactionIDOLFields) {
                final List<DocumentField> values = fields.get(field);
                if (values != null) {
                    mentionedUsers.addAll(values);
                }
            }

			if (!mentionedUsers.isEmpty()) {
				for (final DocumentField user : mentionedUsers) {
					if (!whitelist.contains(user.getValue())) {
						// Wipe summary
						doc.setSummary(REDACTED);

						// Censor out everything except the permitted fields
						for (final Iterator<Map.Entry<String,List<DocumentField>>> iter = fields.entrySet().iterator(); iter.hasNext();) {
							final Map.Entry<String, List<DocumentField>> entry = iter.next();
							if (!permittedIDOLFields.contains(entry.getKey())) {
								iter.remove();
							}
						}

						if (fields.containsKey("DRECONTENT")) {
							fields.put("DRECONTENT", Collections.singletonList(new DocumentField("DRECONTENT", REDACTED)));
						}

						// getDocumentFields returns a new instance, so we have to set it to the modified values
						doc.setDocumentFields(fields);

						return doc;
					}
				}
			}
		}

		return doc;
	}

	public ResultDocument censor(final ResultDocument doc) {
		final Map<String,List<String>> fields = doc.getDisplayFields();
		if (fields != null) {
			final List<String> mentionedUsers = fields.get(redactionField);
			if (mentionedUsers != null && !mentionedUsers.isEmpty()) {
				for (final String user : mentionedUsers) {
					if (!whitelist.contains(user)) {
						// Wipe summary and DRECONTENT
						doc.setDreContent(REDACTED);
						doc.setSummary(REDACTED);

						// Censor out everything except the permitted fields
						for (final Iterator<Map.Entry<String,List<String>>> iter = fields.entrySet().iterator(); iter.hasNext();) {
							final Map.Entry<String, List<String>> entry = iter.next();
							if (!permittedFields.contains(entry.getKey())) {
								iter.remove();
							}
						}

						return doc;
					}
				}
			}
		}

		return doc;
	}

	public <E extends  Iterable<ResultDocument>> E censor (final E documents){
		for (final ResultDocument document : documents) {
			censor(document);
		}

		return documents;
	}
}
