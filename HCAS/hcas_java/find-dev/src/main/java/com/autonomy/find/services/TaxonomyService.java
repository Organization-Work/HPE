package com.autonomy.find.services;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.autonomy.aci.client.annotations.IdolAnnotationsProcessorFactory;
import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.find.config.TaxonomyConfig;
import com.autonomy.find.dto.SearchRequestData;
import com.autonomy.find.dto.taxonomy.CategoryData;
import com.autonomy.find.dto.taxonomy.CategoryName;
import com.autonomy.find.dto.taxonomy.CategoryPath;
import com.autonomy.find.util.CollUtils;
import com.autonomy.find.util.MTree;
import com.googlecode.ehcache.annotations.Cacheable;
import com.googlecode.ehcache.annotations.DecoratedCacheType;

public class TaxonomyService {

    public AciService getCategoryAci() {
		return categoryAci;
	}


	public void setCategoryAci(AciService categoryAci) {
		this.categoryAci = categoryAci;
	}


	public TaxonomyConfig getTaxonomyConfig() {
		return taxonomyConfig;
	}


	public void setTaxonomyConfig(TaxonomyConfig taxonomyConfig) {
		this.taxonomyConfig = taxonomyConfig;
	}


	public IdolAnnotationsProcessorFactory getProcessorFactory() {
		return processorFactory;
	}


	public void setProcessorFactory(IdolAnnotationsProcessorFactory processorFactory) {
		this.processorFactory = processorFactory;
	}


	public TaxonomyDocCountService getDocCountService() {
		return docCountService;
	}


	public void setDocCountService(TaxonomyDocCountService docCountService) {
		this.docCountService = docCountService;
	}


	public static Logger getLogger() {
		return LOGGER;
	}


	public static String getActionGetHierDetails() {
		return ACTION_GET_HIER_DETAILS;
	}


	public static String getParamCategory() {
		return PARAM_CATEGORY;
	}


	public void setCategoryHier(MTree<String> categoryHier) {
		this.categoryHier = categoryHier;
	}


	private static final Logger LOGGER = LoggerFactory.getLogger(TaxonomyService.class);


    private static final String
            ACTION_GET_HIER_DETAILS = "CategoryGetHierDetails",
            PARAM_CATEGORY = "Category";


    @Autowired
    @Qualifier("taxonomyAciService")
    private AciService categoryAci;

    @Autowired
    private TaxonomyConfig taxonomyConfig;

    @Autowired
    private IdolAnnotationsProcessorFactory processorFactory;

    @Autowired
    private TaxonomyDocCountService docCountService;

    private final Map<String, String> categoryNames = new HashMap<>();
    private MTree<String> categoryHier;


    /**
     *
     */
    private void loadCategoryHierAndNames() {
        if (categoryHier != null) {
            return;
        }
        final Map<String, Integer> docCounts = docCountService.getCategoryDocCounts(new SearchRequestData());
        final Map<String, Set<String>> links = new HashMap<>();
        for (final String categoryId : docCounts.keySet()) {
            if (!this.categoryNames.containsKey(categoryId)) {
                try {
                    extractNamesAndLinks(getCategoryPath(categoryId).getActualPath(), this.categoryNames, links);
                }
                catch (final RuntimeException e) {
                    LOGGER.error(String.format("Error loading category with id: `%s`", categoryId));
                }
            }
        }
        this.categoryHier = CollUtils.compileLinksToTree(links, taxonomyConfig.getRootCategory());
    }


    /**
     * Extracts category id + name pairs and parent to children links.
     *
     * @param path          - Path to the category
     * @param categoryNames - Category naming map accumulator
     * @param links         - Category linking accumulator
     */
    private static void extractNamesAndLinks(
            final List<CategoryName> path,
            final Map<String, String> categoryNames,
            final Map<String, Set<String>> links
    ) {
        Set<String> children;
        String parent = null;

        for (final CategoryName category : path) {
            if (parent != null) {
                children = links.get(parent);
                if (children == null) {
                    children = new LinkedHashSet<>();
                }
                children.add(category.getId());
                links.put(parent, children);
            }
            parent = category.getId();
            categoryNames.put(category.getId(), category.getName());
        }
    }


    /**
     * @param categoryId
     * @return
     */
    private CategoryPath getCategoryPath(
            final String categoryId
    ) {
        final AciParameters params = paramsForHierAndNames(null, categoryId);
        return categoryAci.executeAction(params, processorFactory.listProcessorForClass(CategoryPath.class)).get(0);
    }


    /**
     * GetCategoryNames
     *
     * @return Map Id Name
     */
    public Map<String, String> getCategoryNames() {
        loadCategoryHierAndNames();
        return this.categoryNames;
    }


    /**
     * GetCategoryHier
     *
     * @return MTree Id
     */
    public MTree<String> getCategoryHier() {
        loadCategoryHierAndNames();
        return this.categoryHier;
    }

    /**
     * GetCategoryData
     *
     * @return CategoryData
     */
    @Cacheable(cacheName = "getCategoryData", decoratedCacheType = DecoratedCacheType.SELF_POPULATING_CACHE)
    public CategoryData getCategoryData() {
        if (!taxonomyConfig.isActive()) {
            return null;
        }
        return new CategoryData(
                getCategoryHier(),
                getCategoryNames(),
                docCountService.getCategoryDocCounts(new SearchRequestData()));
    }


    /**
     * @param params
     * @param categoryId
     * @return
     */
    public AciParameters paramsForHierAndNames(
            AciParameters params,
            final String categoryId
    ) {
        if (params == null) {
            params = new AciParameters(ACTION_GET_HIER_DETAILS);
        }

        params.add(PARAM_CATEGORY, categoryId);

        return params;
    }
}
