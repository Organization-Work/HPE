package autn.voronoi;

import com.autonomy.aci.actions.idol.tag.Field;
import com.autonomy.aci.actions.idol.tag.FieldListProcessor;
import com.autonomy.aci.client.util.AciParameters;
import com.googlecode.ehcache.annotations.Cacheable;
import com.googlecode.ehcache.annotations.DecoratedCacheType;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/autn/voronoi/QueryTagValuesCache.java#1 $
 * <p/>
 * Copyright (c) 2013, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/05/21 $
 */
@Service
public class QueryTagValuesCache {

	@Cacheable(cacheName = "querytagvaluescache.getquerytagvalues", decoratedCacheType = DecoratedCacheType.SELF_POPULATING_CACHE)
	public List<Field> getQueryTagValues(final String linkToField, final Voronoi.Engine config) {
		// deliberately leaving out min/max dates here, we want how many times a document has been cited in general, not in this specific time period
		final AciParameters params = new AciParameters("getquerytagvalues");
		params.add("text", "*");
		params.add("fieldname", linkToField);
		params.add("documentcount", true);
		params.add("outputencoding", "UTF8");
		params.add("combine", config.combine);
		params.add("databasematch", config.databases);
		params.add("TimeoutMS", 300e3);
		return config.aciService.executeAction(params, new FieldListProcessor());
	}
}
