/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/identifier/stateid/AbstractStateIds.java#2 $
 *
 * Copyright (c) 2010, Autonomy Systems Ltd.
 *
 * Last modified by $Author: darrelln $ on $Date: 2010/05/25 $
 */
package com.autonomy.aci.content.identifier.stateid;

import java.util.UUID;

/**
 * An abstract base-class that partially implements the {@link StateIds} interface using sensible defaults.
 */
public abstract class AbstractStateIds implements StateIds {
    private static final String MATCH_NOTHING = "MATCH_NOTHING_" + UUID.randomUUID().toString();

    /**
     *
     * @return {@code "statematchid"} or equivalent
     */
    @Override
    public String getMatchParameterName() {
        return STATE_MATCH_ID;
    }

    /**
     *
     * @return {@code "DRESTATEID"} or equivalent
     */
    @Override
    public String getIndexingIdentifierName() {
        return DRESTATEID;
    }

    /**
     *
     * @return {@code "statedontmatchid"} or equivalent
     */
    @Override
    public String getDontMatchParameterName() {
        return STATE_DONT_MATCH_ID;
    }

    /**
     *
     * @return {@code "stateid"} or equivalent
     */
    @Override
    public String getGetContentParameterName() {
        return STATE_ID;
    }

    @Override
    public String toIndexingString() {
        return toString();
    }

    @Override
    public StateIds append(final String stateId, final int document) {
        return new StateIdsBuilder(this).append(stateId, document);
    }

    @Override
    public StateIds append(final String stateId, final int start, final int end) {
        return new StateIdsBuilder(this).append(stateId, start, end);
    }

    @Override
    public StateIds append(final String stateId, final StateRange range) {
        return new StateIdsBuilder(this).append(stateId, range);
    }


    @Override
    public StateIds append(final String... stateIds) {
        return new StateIdsBuilder(this).append(stateIds);
    }

    @Override
    public StateIds append(final Iterable<?>... stateIds) {
        return new StateIdsBuilder(this).append(stateIds);
    }

    /**
     * Simple implementation that tests for emptiness using <tt>size() == 0</tt>. In most cases this should be
     * overridden with a better implementation.
     *
     * @return <tt>true</tt> if and only if size is 0.
     */
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Simple implementation that calculates the size by counting the number of elements in the iterator. In most cases
     * this should be overridden with a better implementation.
     *
     * @return {@inheritDoc}
     */
    @Override
    public int size() {
        int size = 0;

        for (final StateId stateId : this) {
            ++size;
        }

        return size;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj == this || obj instanceof StateIds && toString().equals(obj.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return MATCH_NOTHING;
        }

        final StringBuilder builder = new StringBuilder();

        for (final StateId stateId : this) {
            if (stateId == null) {
                throw new IllegalStateException("Unexpected null state id in iterator");
            }

            builder.append(stateId).append(',');
        }

        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.toString();
    }
}
