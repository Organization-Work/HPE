/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/identifier/stateid/StateId.java#3 $
 *
 * Copyright (c) 2010, Autonomy Systems Ltd.
 *
 * Last modified by $Author: darrelln $ on $Date: 2010/06/01 $
 */
package com.autonomy.aci.content.identifier.stateid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/**
 * A representation of a stored state id. A state id is string token that identifies a collection of documents that
 * matched a query.
 */
public class StateId extends AbstractStateIds {
    private final String stateToken;
    private final StateRange range;

    private static final StateRange NO_RANGE = new AbstractStateRange() {
            @Override
            public String toString() {
                return "";
            }

            @Override
            public int size() {
                return 0;
            }
        };

    /**
     * Creates a new {@code StateId} instance for the specified state token.
     *
     * @param stateToken The state token
     */
    public StateId(final String stateToken) {
        this(stateToken, NO_RANGE);
    }

    /**
     * Creates a new {@code StateId} instance for the specified state token with the zero-based range set to a specific
     * document index within the stored state.
     *
     * @param stateToken The state token
     * @param document The specific document index
     */
    public StateId(final String stateToken, final int document) {
        this(stateToken, new Range(document));
    }

    /**
     * Creates a new {@code StateId} instance for the specified state token with a zero-based range of document indices
     * within the stored state.
     *
     * @param stateToken The state token
     * @param start The start index of the range
     * @param end The end index of the range
     */
    public StateId(final String stateToken, final int start, final int end) {
        this(stateToken, new Range(start, end));
    }

    /**
     * Creates a new {@code StateId} instance for the specified state token and range.
     *
     * @param stateToken The state token
     * @param range The index range
     */
    public StateId(final String stateToken, final StateRange range) {
        Validate.isTrue(StringUtils.isNotBlank(stateToken), "State token must not be blank");
        Validate.notNull(range, "Range must not be null");

        this.stateToken = stateToken;
        this.range = StateRangeWrapper.wrap(range);
    }

    @Override
    public Iterator<StateId> iterator() {
        // Should probably rewrite this to use a custom iterator
        return Arrays.asList(this).iterator();
    }

    /**
     * Accessor for the state token.
     *
     * @return The state token
     */
    public String getStateToken() {
        return stateToken;
    }

    /**
     * Accessor for the index range.
     *
     * @return The index range
     */
    public StateRange getRange() {
        return range;
    }

    /**
     * Convenience method for creating a StateId that has the same token as {@code this} but without a range. If
     * {@code this} doesn't have a range then it will just be returned.
     *
     * @return A rangeless {@code StateId}
     */
    public StateId withoutRange() {
        return forRange(NO_RANGE);
    }

    /**
     * Convenience method for creating a StateId that has the same token as {@code this} but with a single document
     * as its zero-based index range. If {@code this} already has the correct range it will just be returned.
     *
     * @param document The document index
     * @return A {@code StateId} with the specified range
     */
    public StateId forDocument(final int document) {
        return forRange(new Range(document));
    }

    /**
     * Convenience method for creating a StateId that has the same token as {@code this} but with the specified
     * zero-based index range. If {@code this} already has the correct range it will just be returned.
     *
     * @param start The start index of the range
     * @param end The end index of the range
     * @return A {@code StateId} with the specified range
     */
    public StateId forRange(final int start, final int end) {
        return forRange(new Range(start, end));
    }

    /**
     * Convenience method for creating a StateId that has the same token as {@code this} but with the specified range.
     * If {@code this} already has the correct range it will just be returned.
     *
     * @param range The index range
     * @return A {@code StateId} with the specified range
     */
    public StateId forRange(final StateRange range) {
        if (getRange().equals(range)) {
            return this;
        }

        return new StateId(stateToken, range);
    }

    /**
     * Convenience method for creating a list of {@code StateId}s suitable for paging through a stored state. Each
     * {@code StateId} will have the same state token as {@code this} but with different index ranges. For example, with
     * a page size of 200 and a total results of 420, the list will contain 3 entries with ranges of <tt>[0-199]</tt>,
     * <tt>[200-399]</tt> and <tt>[400-419]</tt>.
     *
     * @param pageSize The size of each page
     * @param totalResults The total number of results to generate pages for
     * @return {@code StateId}s that can be used to page through a stored state
     */
    public List<StateId> pages(final int pageSize, final int totalResults) {
        Validate.isTrue(pageSize > 0, "Page size must be positive: was " + pageSize);
        Validate.isTrue(totalResults > 0, "Total results must be positive: was " + totalResults);

        final List<StateId> pages = new ArrayList<StateId>(totalResults / pageSize + 1);

        Range range = Range.first(pageSize);

        for ( ; range.getEnd() < totalResults - 1 ; range = range.next()) {
            pages.add(forRange(range));
        }

        // Though not strictly required, tidy up the last range to stop at the right upper bound
        pages.add(forRange(range.getStart(), totalResults - 1));

        return Collections.unmodifiableList(pages);
    }

    /**
     * For a single state id the size is always 1.
     *
     * @return {@code 1}
     */
    @Override
    public int size() {
        return 1;
    }

    /**
     * A single state id can never be empty.
     *
     * @return {@code false}
     */
    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public String toString() {
        final String range = this.range.toString();

        if (StringUtils.isNotBlank(range)) {
            return stateToken + '[' + range + ']';
        }

        return stateToken;
    }

    private static final class StateRangeWrapper extends AbstractStateRange {
        private final String range;
        private final int size;

        private StateRangeWrapper(final StateRange range) {
            Validate.notNull(range, "Range must not be null");

            this.range = range.toString();
            this.size = range.size();

            Validate.notNull(this.range, "Range string must not be null");

            if (StringUtils.isBlank(this.range)) {
                Validate.isTrue(this.size == 0, "If range is blank, size must be 0: was " + this.size);
            }
            else {
                Validate.isTrue(this.size > 0, "If range is not blank, size must be greater than 0: was " + this.size);
            }
        }

        @Override
        public String toString() {
            return range;
        }

        @Override
        public int size() {
            return size;
        }

        private static StateRange wrap(final StateRange range) {
            if (range == NO_RANGE) {
                return NO_RANGE;
            }

            return new StateRangeWrapper(range);
        }
    }
}
