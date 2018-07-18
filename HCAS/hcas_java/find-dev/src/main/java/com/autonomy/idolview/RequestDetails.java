package com.autonomy.idolview;

import java.util.List;
import java.util.Map;

/**
* $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/idolview/RequestDetails.java#1 $
* <p/>
* Copyright (c) 2012, Autonomy Systems Ltd.
* <p/>
* Last modified by $Author: tungj $ on $Date: 2013/05/21 $
*/
public class RequestDetails {
    // map of currently applied fieldtext filters
    // may need some kind of abstraction layer to describe the filters? ?
    public Map<String, String[]> filters;
    // list of properties that we want to have a breakdown count of
    public List<String> hierarchyCountKeys;
    // maximum number of properties we'll return, prevents excessive data
    public int maxValues = -1;
    public String text;
    public Long start;
    public Long end;
    public boolean fetchDates;

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final RequestDetails that = (RequestDetails) o;

        return fetchDates == that.fetchDates
            && maxValues == that.maxValues
            && !(end != null ? !end.equals(that.end) : that.end != null)
            && !(filters != null ? !filters.equals(that.filters) : that.filters != null)
            && !(hierarchyCountKeys != null ? !hierarchyCountKeys.equals(that.hierarchyCountKeys) : that.hierarchyCountKeys != null) && !(start != null ? !start.equals(that.start) : that.start != null)
            && !(text != null ? !text.equals(that.text) : that.text != null);
    }

    @Override
    public int hashCode() {
        int result = filters != null ? filters.hashCode() : 0;
        result = 31 * result + (hierarchyCountKeys != null ? hierarchyCountKeys.hashCode() : 0);
        result = 31 * result + maxValues;
        result = 31 * result + (text != null ? text.hashCode() : 0);
        result = 31 * result + (start != null ? start.hashCode() : 0);
        result = 31 * result + (end != null ? end.hashCode() : 0);
        result = 31 * result + (fetchDates ? 1 : 0);
        return result;
    }
}
