package com.autonomy.find.services;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.find.dto.SearchRequestData;
import com.autonomy.find.processors.SingleTagContentProcessor;


public class CategoryTrainingService {

    @Autowired
    @Qualifier("taxonomyAciService")
    private AciService categoryAci;

    public String getCategoryQueryExtension(final String categoryId) {
        final AciParameters params = new AciParameters("CategoryGetTraining");
        params.put("Category", categoryId);
        return categoryAci.executeAction(params, new SingleTagContentProcessor("autn:boolean"));
    }

    public SearchRequestData processSearchRequestData(final SearchRequestData searchRequestData) {
        if (searchRequestData.hasCategory()) {
            searchRequestData.setQueryExtension(getCategoryQueryExtension(searchRequestData.getCategory()));
        }
        return searchRequestData;
    }
}
