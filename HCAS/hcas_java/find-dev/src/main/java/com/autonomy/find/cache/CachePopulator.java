package com.autonomy.find.cache;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

import com.autonomy.find.config.FindNewsConfig;
import com.autonomy.find.services.NewsService;

@Service
public class CachePopulator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachePopulator.class);

    @Autowired
    private FindNewsConfig config;

    @Autowired
    private NewsService newsService;

    @Autowired
    @Qualifier("myExecutor")
    private TaskExecutor taskExecutor;

    @PostConstruct
    private void populateCaches() {
        taskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                populateNewsServiceCaches();
            }
        });
    }

    private void populateNewsServiceCaches() {
        boolean succeeded = false;

        try {
            newsService.getCategories();

            succeeded = true;
        } catch (final Exception ex) {
            LOGGER.error("Failed to populate categories to cache.", ex);
        }

        if (succeeded) LOGGER.info("Category cache populated");

        final List<String> types = Arrays.asList("popular", "breaking");
        final List<String> categories = new LinkedList<String>();
        final int maxResults = config.getDefaultMaxResults();
        final boolean headlines = config.isDefaultHeadlines();

        succeeded = false;

        try {
            newsService.getNews(
                    types,
                    categories,
                    maxResults,
                    headlines,
                    null,   //start date
                    null,   //end date
                    null,   //interval
                    true);

            succeeded = true;
        } catch (final Exception ex) {
            LOGGER.error("Failed to populate news caches.", ex);
        }

        if (succeeded) LOGGER.info("News caches populated");
    }

}
