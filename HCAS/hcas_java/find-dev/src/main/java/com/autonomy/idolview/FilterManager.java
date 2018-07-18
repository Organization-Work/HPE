package com.autonomy.idolview;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/idolview/FilterManager.java#1 $
 * <p/>
 * Copyright (c) 2012, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/05/21 $
 */
@Service
public class FilterManager {

    private final Map<String, Filter> filterMap = new LinkedHashMap<String, Filter>();

    @Autowired
    public void setFilters(final Filter[] filters) {
        for (final Filter filter : filters) {
            filterMap.put(filter.key.toUpperCase(Locale.US), filter);
        }
    }

    public Filter getFilter(final String key) {
        final Filter filter = filterMap.get(key.toUpperCase(Locale.US));

        if (filter == null) {
            throw new IllegalArgumentException("no filter defined for key:" + key);
        }

        return filter;
    }

    public Collection<Filter> getFilters() {
        return Collections.unmodifiableCollection(filterMap.values());
    }
}
