package com.autonomy.idolview;

import java.util.LinkedHashMap;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/idolview/DateCounts.java#2 $
 * <p/>
 * Copyright (c) 2012, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/11/08 $
 */
public class DateCounts {
    private final DatePeriod period;
    private final LinkedHashMap<Long, Integer> dates = new LinkedHashMap<Long, Integer>();

    public DateCounts(final DatePeriod period) {
        this.period = period;
    }

    public DatePeriod getPeriod() {
        return period;
    }

    public LinkedHashMap<Long, Integer> getDates() {
        return dates;
    }
}
