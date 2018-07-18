package com.autonomy.find.controllers;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.autonomy.find.api.response.ResponseWithResult;
import com.autonomy.find.api.response.ResponseWithSuccessError;
import com.autonomy.find.dto.SearchRequestData;
import com.autonomy.find.dto.taxonomy.CategoryData;
import com.autonomy.find.services.CategoryTrainingService;
import com.autonomy.find.services.TaxonomyDocCountService;
import com.autonomy.find.services.TaxonomyService;
import com.autonomy.find.util.MTree;


@Controller
@RequestMapping("/p/ajax/taxonomy/")
public class TaxonomyController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaxonomyController.class);

    private static final String
            ERROR_GETTING_DOC_COUNTS = "Error getting taxonomy document counts.",
            ERROR_GETTING_DATA = "Error getting taxonomy data.";

    @Autowired
    private TaxonomyService taxonomyService;

    @Autowired
    private TaxonomyDocCountService docCountService;

    @Autowired
    private CategoryTrainingService trainingService;


    /**
     * GetCategoryDocCounts
     *
     * @param requestData - Optional query string, default = "*"
     * @return Response (Map Id Count)
     */
    @RequestMapping("getCategoryDocCounts.json")
    @ResponseBody
    public ResponseWithSuccessError getCategoryDocCounts(
            @RequestBody final SearchRequestData requestData
    ) {
        trainingService.processSearchRequestData(requestData);
        try {
            return new ResponseWithResult<>(docCountService.getCategoryDocCounts(requestData));
        } catch (final RuntimeException e) {
            LOGGER.error(ERROR_GETTING_DOC_COUNTS, e);
            return new ResponseWithSuccessError(ERROR_GETTING_DOC_COUNTS);
        }
    }


    /**
     * GetCategoryData
     *
     * @return Response CategoryData
     */
    @RequestMapping("getCategoryData.json")
    @ResponseBody
    public ResponseWithSuccessError getCategoryData() {
        try {
            return new ResponseWithResult<>(taxonomyService.getCategoryData());
        } catch (final RuntimeException e) {
            LOGGER.error(ERROR_GETTING_DATA, e);
            return new ResponseWithSuccessError(ERROR_GETTING_DATA);
        }
    }


    /**
     * GetCategoryDocCounts
     *
     * @param requestData - Optional query string, default = "*"
     * @return Response (Map Id Count)
     */
//    @RequestMapping("getCategoryDocCounts.json")
    @ResponseBody
    public ResponseWithSuccessError getCategoryDocCountsMOCK(
            @RequestBody final SearchRequestData requestData
    ) {
        trainingService.processSearchRequestData(requestData);
        final Map<String, Integer> mock = new HashMap<>();
        mock.put("DEMO_A", 0);
        mock.put("DEMO_B", 2);
        mock.put("DEMO_C", 2);
        mock.put("DEMO_D", 0);
        mock.put("DEMO_E", 4);
        mock.put("DEMO_F", 0);
        mock.put("DEMO_G", 0);
        mock.put("DEMO_H", 3);
        mock.put("DEMO_I", 3);
        mock.put("DEMO_J", 0);
        return new ResponseWithResult<>(mock);
    }


    /**
     * GetCategoryData
     *
     * @return Response CategoryData
     */
//    @RequestMapping("getCategoryData.json")
    @ResponseBody
    public ResponseWithSuccessError getCategoryDataMOCK() {

        final Map<String, Integer> mockCounts = new HashMap<String, Integer>() {{
            put("DEMO_A", 3);
            put("DEMO_B", 3);
            put("DEMO_C", 3);
            put("DEMO_D", 5);
            put("DEMO_E", 5);
            put("DEMO_F", 5);
            put("DEMO_G", 5);
            put("DEMO_H", 4);
            put("DEMO_I", 4);
            put("DEMO_J", 4);
        }};

        final Map<String, String> mockNames = new HashMap<String, String>() {{
            put("DEMO_A", "Demo A");
            put("DEMO_B", "Demo B");
            put("DEMO_C", "Demo C");
            put("DEMO_D", "Demo D");
            put("DEMO_E", "Demo E");
            put("DEMO_F", "Demo F");
            put("DEMO_G", "Demo G");
            put("DEMO_H", "Demo H");
            put("DEMO_I", "Demo I");
            put("DEMO_J", "Demo J");
        }};

        final MTree<String> mockHier =
                MTree.node("DEMO_A",
                        MTree.node("DEMO_B",
                                MTree.node("DEMO_C"),
                                MTree.node("DEMO_D")),
                        MTree.node("DEMO_E",
                                MTree.node("DEMO_F",
                                        MTree.node("DEMO_G")),
                                MTree.node("DEMO_H"),
                                MTree.node("DEMO_I")),
                        MTree.node("DEMO_J"));

        return new ResponseWithResult<>(new CategoryData(mockHier, mockNames, mockCounts));
    }
}
