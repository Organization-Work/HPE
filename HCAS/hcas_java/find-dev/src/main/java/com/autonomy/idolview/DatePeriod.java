package com.autonomy.idolview;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/idolview/DatePeriod.java#2 $
 * <p/>
 * Copyright (c) 2012, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/11/08 $
 */
public enum DatePeriod {
    // in descending order
    year(365 * 24 * 3600), month(28 * 24 * 3600), week(7 * 24 * 3600), day(24 * 3600), hour(3600), minute(60);

    private final int approxSeconds;
    private static final int DESIRED_CATEGORIES = 5;

    DatePeriod(final int approxSeconds) {
        this.approxSeconds = approxSeconds;
    }

    public int getApproxSeconds() {
        return approxSeconds;
    }

    public static DatePeriod chooseAppropriatePeriod(final DatePeriod defaultType, final Long start, final Long end) {
        if (start == null || end == null) {
            // this might not be a sensib
            return defaultType;
        }

        final long time = end - start;

        if (time < 0) {
            throw new IllegalArgumentException("end should be before start");
        }

        for (final DatePeriod period : DatePeriod.values()) {
            if (period.approxSeconds * DESIRED_CATEGORIES < time) {
                return period;
            }
        }

        return defaultType;
    }
}
