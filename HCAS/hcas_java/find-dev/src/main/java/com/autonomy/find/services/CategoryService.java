package com.autonomy.find.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.autonomy.aci.actions.idol.clustering.ClusterResults;
import com.autonomy.aci.actions.idol.clustering.ClusterResultsProcessor;
import com.autonomy.aci.client.annotations.IdolAnnotationsProcessorFactory;
import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.find.config.CategoryConfig;

@Service
public class CategoryService {

	@Autowired
	private CategoryConfig config;

	@Autowired
	@Qualifier("categoryAciService")
	private AciService categoryAci;

	@Autowired
	private IdolAnnotationsProcessorFactory processorFactory;

	/**
	 * Retrieves the current breaking news
	 * @return
	 */
	public ClusterResults getBreakingNews() {

		final AciParameters params = new AciParameters("ClusterResults");
		params.add("SourceJobName", config.getBreaking_jobName());
		params.add("NumClusters", config.getBreaking_numClusters());
		params.add("NumResults", config.getBreaking_numResults());
		params.add("DREAnyLanguage", config.getBreaking_anyLanguage());
		params.add("DREOutputEncoding", config.getBreaking_outputEncoding());

		return categoryAci.executeAction(params, new ClusterResultsProcessor());
	}

	/**
	 * Retrieves the current popular news
	 * @return
	 */
	public ClusterResults getPopularNews() {

		final AciParameters params = new AciParameters("ClusterResults");
		params.add("SourceJobName", config.getHot_jobName());
		params.add("NumClusters", config.getHot_numClusters());
		params.add("NumResults", config.getHot_numResults());
		params.add("DREAnyLanguage", config.getHot_anyLanguage());
		params.add("DREOutputEncoding", config.getHot_outputEncoding());

		return categoryAci.executeAction(params, new ClusterResultsProcessor());
	}
	
}
