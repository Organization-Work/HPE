package com.autonomy.idolview;

import java.util.List;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/idolview/FilterResponse.java#2 $
 * <p/>
 * Copyright (c) 2012, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/11/08 $
 */
public class FilterResponse extends FieldNode {
    public DateCounts dates;

    public FilterResponse(final String name, final int size, final FieldNode... children) {
        super(name, size, children);
    }

    public FilterResponse(final DateCounts dates, final String name, final int size, final FieldNode... children) {
        super(name, size, children);
        this.dates = dates;
    }
}
