package autn.voronoi.controller;

import javax.xml.xpath.XPathExpressionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import autn.voronoi.DocGraph;
import autn.voronoi.Voronoi;

import com.autonomy.aci.client.services.AciServiceException;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/autn/voronoi/controller/DocGraphController.java#1 $
 * <p/>
 * Copyright (c) 2013, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/05/21 $
 */
@RequestMapping("/p")
@Controller
public class DocGraphController {

	@Autowired
	private DocGraph docGraph;

	@Value("${docgraph.fallbackSuggestMinScore}")
	private double fallbackSuggestMinScore;

	@RequestMapping(value="/docgraph.json")
	@ResponseBody
	Voronoi.Graph docgraph(
		@RequestParam(required = false, defaultValue = "*") final String query,
		@RequestParam(required = false, defaultValue = "0") final int engine,
		@RequestParam(required = false) final Double minScore,
		@RequestParam(required = false, defaultValue = "50") final int maxResults,
		@RequestParam(required = false) final Long minDate,
		@RequestParam(required = false) final Long maxDate,
		@RequestParam(defaultValue = "true") boolean sizeByLinks,
		@RequestParam(defaultValue = "-1") double suggestMinScore
	) throws AciServiceException, XPathExpressionException {

		if (!docGraph.isLinkFieldConfigured()) {
			// We can only size or link by linkFields (e.g. citations) if the link fields are configured
			sizeByLinks = false;

			if (suggestMinScore < 0) {
				suggestMinScore = fallbackSuggestMinScore;
			}
		}

		return docGraph.docgraph(query, engine, maxResults, maxDate, minScore, minDate, sizeByLinks, suggestMinScore);
	}

}
