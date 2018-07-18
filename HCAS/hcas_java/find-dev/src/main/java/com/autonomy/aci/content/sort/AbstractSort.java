/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/sort/AbstractSort.java#2 $
 *
 * Copyright (c) 2009-2010, Autonomy Systems Ltd.
 *
 * Last modified by $Author: darrelln $ on $Date: 2010/05/06 $
 */
package com.autonomy.aci.content.sort;

/**
 * Abstract base class that implements most of the {@link Sort} interface.
 */
public abstract class AbstractSort implements Sort {
    /**
     * {@inheritDoc}
     */
    @Override
    public Sort then(final Iterable<? extends SortBy>... sorts) {
        return new SortBuilder(this).then(sorts);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof Sort && this.toString().equals(obj.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * A naive implementation that calculates the size by counting the number of elements in the iterator. Subclasses
     * should generally override this method.
     *
     * @return {@inheritDoc}
     */
    @Override
    public int size() {
        int size = 0;

        for (final SortBy sortBy : this) {
            ++size;
        }

        return size;
    }

    /**
     * A naive implementation that just checks whether {@code size() == 0}. Subclasses should consider overriding this
     * method.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        for (final SortBy sort : this) {
            if (sort == null) {
                throw new IllegalStateException("Unexpected null sort in iterator");
            }

            builder.append(sort).append('+');
        }

        if (builder.length() > 0) {
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.toString();
    }
}
