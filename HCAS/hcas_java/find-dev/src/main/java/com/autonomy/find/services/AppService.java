package com.autonomy.find.services;

import com.autonomy.common.io.IOUtils;
import com.autonomy.find.config.FindNewsConfig;
import com.googlecode.ehcache.annotations.Cacheable;
import com.googlecode.ehcache.annotations.DecoratedCacheType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;


@Service
public class AppService {

    private static final long AUTO_REFRESH_INTERVAL = 600000L; // 10 minutes

    @Autowired
    private FindNewsConfig config;


    @Cacheable(cacheName = "AppService.version_info",
            decoratedCacheType = DecoratedCacheType.REFRESHING_SELF_POPULATING_CACHE,
            refreshInterval = AUTO_REFRESH_INTERVAL
    )
    public String loadAppVersionData() throws IOException {
        return IOUtils.toString(getClass().getClassLoader().getResourceAsStream(config.getAppVersionFile()));
    }
}
