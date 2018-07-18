/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/identifier/stateid/Range.java#2 $
 *
 * Copyright (c) 2010, Autonomy Systems Ltd.
 *
 * Last modified by $Author: darrelln $ on $Date: 2010/06/04 $
 */
package com.autonomy.aci.content.identifier.stateid;

import org.apache.commons.lang.Validate;

/**
 * Range is a simple implementation of {@link StateRange} that only allows for a single continuous range. It also
 * provides methods for doing paging.
 */
public class Range extends AbstractStateRange {
    private final int start;
    private final int end;

    /**
     * Creates a new {@code Range} instance with a single zero-based document index.
     *
     * @param document The document index
     */
    public Range(final int document) {
        this(document, document);
    }

    /**
     * Creates a new {@code Range} instance with the specified zero-based index range.
     *
     * @param start The start index of the range
     * @param end The end index of the range
     */
    public Range(final int start, final int end) {
        Validate.isTrue(start > -1, "Start must be non-negative");
        Validate.isTrue(start <= end, "Start must be less than end");

        this.start = start;
        this.end = end;
    }

    /**
     * Creates a new range instance for the next page. The size of the new range will be the same as the current range
     * but it will start with the document that immediately follows the end of this range.
     *
     * @return The range of the next page
     */
    public Range next() {
        return next(size());
    }

    /**
     * Creates a new range instance for the next page. The new range will start with the document that immediately
     * follows the end of this range.
     *
     * @param size The size of the new range
     * @return The range of the next page
     */
    public Range next(final int size) {
        return new Range(end + 1, end + size);
    }

    /**
     * Creates a new range instance for specified page. The new range will have the same size as the current range.
     *
     * @param number One-based page number
     * @return The range for the page
     */
    public Range page(final int number) {
        return page(number, size());
    }

    @Override
    public int size() {
        return end - start + 1;
    }

    /**
     * Accessor for the start index of the range.
     *
     * @return The start index
     */
    public int getStart() {
        return start;
    }

    /**
     * Accessor for the end index of the range.
     *
     * @return The end index
     */
    public int getEnd() {
        return end;
    }

    @Override
    public String toString() {
        if (start == end) {
            return String.valueOf(start);
        }

        return start + "-" + end;
    }

    /**
     * Factory method for creating a {@code Range} to represent the first page.
     *
     * @param size The page size
     * @return The first page range
     */
    public static Range first(final int size) {
        return page(1, size);
    }

    /**
     * Factory method for creating a {@code Range} to represent a particular page.
     *
     * @param number The one-based page number
     * @param size The size of each page
     * @return The range for the page
     */
    public static Range page(final int number, final int size) {
        return new Range(size * (number - 1), size * number - 1);
    }
}
