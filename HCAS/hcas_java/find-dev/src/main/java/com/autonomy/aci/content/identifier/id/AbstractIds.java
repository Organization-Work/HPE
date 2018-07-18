/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/identifier/id/AbstractIds.java#2 $
 *
 * Copyright (c) 2010, Autonomy Systems Ltd.
 *
 * Last modified by $Author: darrelln $ on $Date: 2010/05/20 $
 */
package com.autonomy.aci.content.identifier.id;

/**
 * An abstract base-class that partially implements the {@link Ids} interface using sensible defaults.
 */
public abstract class AbstractIds implements Ids {

    private static final String MATCH_NOTHING = "0";

    /**
     *
     * @return {@code "matchid"} or equivalent
     */
    @Override
    public String getMatchParameterName() {
        return MATCH_ID;
    }

    /**
     *
     * @return {@code "DREDOCID"} or equivalent
     */
    @Override
    public String getIndexingIdentifierName() {
        return DREDOCID;
    }

    /**
     *
     * @return {@code "dontmatchid"} or equivalent
     */
    @Override
    public String getDontMatchParameterName() {
        return DONT_MATCH_ID;
    }

    /**
     *
     * @return {@code "id"} or equivalent
     */
    @Override
    public String getGetContentParameterName() {
        return ID;
    }

    @Override
    public String toIndexingString() {
        return toString();
    }

    @Override
    public Ids append(final int... ids) {
        return new IdsBuilder(this).append(ids);
    }

    @Override
    public Ids append(final Iterable<?>... ids) {
        return new IdsBuilder(this).append(ids);
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

        for (final Id id : this) {
            ++size;
        }

        return size;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj == this || obj instanceof Ids && toString().equals(obj.toString());
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

        for (final Id id : this) {
            if (id == null) {
                throw new IllegalStateException("Unexpected null id in iterator");
            }

            builder.append(id).append('+');
        }

        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.toString();
    }
}
