package autn.voronoi.controller;

import net.sf.ehcache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/autn/voronoi/controller/CacheController.java#2 $
 * <p/>
 * Copyright (c) 2012, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/11/08 $
 */
@RequestMapping("/cache")
@Controller
public class CacheController {
    @Autowired
    private CacheManager cacheManager;

    @ResponseBody
    @RequestMapping("/clear.json")
    public Map<String, Boolean> clearCache(
            @RequestParam("cache") final String cache,
            @RequestParam(value = "key", required = false) final Long key) {
        if (!cacheManager.cacheExists(cache)) {
            throw new IllegalArgumentException("No such cache " + cache);
        }

        final boolean removed;

        if (key == null) {
            cacheManager.getCache(cache).removeAll();
            removed = true;
        } else {
            removed = cacheManager.getCache(cache).remove(key);
        }

        return Collections.singletonMap("found", removed);
    }

    @ResponseBody
    @RequestMapping("/keys.json")
    public Map<String, List<String>> cacheKeys(@RequestParam("cache") final String cache) {
        if (!cacheManager.cacheExists(cache)) {
            throw new IllegalArgumentException("No such cache " + cache);
        }

        // Cast the keys to strings, otherwise they get returned as Longs, converted to JS,
        // and are rounded to incorrect values due to insufficient JS numeric precision.
        final List keys = cacheManager.getCache(cache).getKeys();
        final List<String> list = new ArrayList<String>(keys.size());
        for (final Object key : keys) {
            list.add(key.toString());
        }

        return Collections.singletonMap("keys", list);
    }

    @ResponseBody
    @RequestMapping("/list.json")
    public Map<String, String[]> listCache() {
        return Collections.singletonMap("caches", cacheManager.getCacheNames());
    }
}
