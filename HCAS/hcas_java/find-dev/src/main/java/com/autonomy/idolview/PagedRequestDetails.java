package com.autonomy.idolview;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/idolview/PagedRequestDetails.java#2 $
 * <p/>
 * Copyright (c) 2012, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/11/08 $
 */
public class PagedRequestDetails extends RequestDetails {
    public int pageSize = 20;
    public int page = 0;

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass() || !super.equals(o)) return false;

        final PagedRequestDetails that = (PagedRequestDetails) o;

        return page == that.page && pageSize == that.pageSize;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + pageSize;
        result = 31 * result + page;
        return result;
    }
}
