package com.autonomy.find.processors;

import com.autonomy.aci.actions.idol.query.QueryResponseProcessor;

/**
 * Convenience class to handle hierarchical XML documents
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/processors/HierarchicalQueryResponseProcessor.java#2 $
 * <p/>
 * Copyright (c) 2013, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/11/08 $
 */
public class HierarchicalQueryResponseProcessor extends QueryResponseProcessor {
	private static final long serialVersionUID = 3745164391058679326L;

	public HierarchicalQueryResponseProcessor() {
		setDocumentProcessor(new HierachicalDocumentProcessor());
	}
}
