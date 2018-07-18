/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/sort/SortBuilder.java#3 $
 *
 * Copyright (c) 2008-2010, Autonomy Systems Ltd.
 *
 * Last modified by $Author: darrelln $ on $Date: 2010/06/07 $
 */

package com.autonomy.aci.content.sort;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A builder for sort expressions. The order of sort specifiers within the builder is significant but the specifiers are
 * deduplicated.
 */
public class SortBuilder extends AbstractSort {
    private final Set<SortBy> sorts = new LinkedHashSet<SortBy>();

    /**
     * Creates a new {@code SortBuilder} with the sort specifiers provided. Specifiers are added in the order they are
     * encountered.
     *
     * @param sorts Initial sort specifiers
     */
    public SortBuilder(final Iterable<? extends SortBy>... sorts) {
        doThen(sorts);
    }

    /**
     * Appends the sort specifiers provided to the current {@code SortBuilder} in the order they are encountered.
     *
     * @param sorts Sort specifiers
     * @return {@code this}
     */
    @Override
    public SortBuilder then(final Iterable<? extends SortBy>... sorts) {
        return doThen(sorts);
    }
    
    private SortBuilder doThen(final Iterable<? extends SortBy>... sorts) {
        if(sorts == null) {
            throw new IllegalArgumentException("The sort iterable provided was null");
        }

        // In case of nulls, build a new Set and then add them all
        final Collection<SortBy> newSorts = new LinkedHashSet<SortBy>();
        
        for(final Iterable<? extends SortBy> sortIterable : sorts) {
            if(sortIterable == null) {
                throw new IllegalArgumentException("One of the sort iterables provided was null");
            }

            for(final SortBy sort : sortIterable) {
                if(sort == null) {
                    throw new IllegalArgumentException("One of the sort types provided was null");
                }

                newSorts.add(sort);
            }
        }

        this.sorts.addAll(newSorts);
        
        return this;      
    }

    /**
     * Creates an iterator for the current state of the {@code SortBuilder}. Removing elements is not permitted and any
     * changes to the builder while iterating could cause a
     * {@link java.util.ConcurrentModificationException ConcurrentModificationException} to be
     * thrown.
     *
     * @return An iterator for the sort specifiers
     */
    @Override
    public Iterator<SortBy> iterator() {
        return Collections.unmodifiableCollection(sorts).iterator();
    }

    /**
     * The number of specifiers currently in the builder.
     *
     * @return The size of the builder
     */
    @Override
    public int size() {
        return sorts.size();
    }

    /**
     * Whether or not {@code size() == 0}.
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isEmpty() {
        return sorts.isEmpty();
    }

    /**
     * Converts an iterable of sort values into a {@code SortBuilder}. This method can be more efficient than using
     * the equivalent constructor but the returned object might be backed by the object provided.
     *
     * @param sorts The print values to convert
     * @return An equivalent instance of {@code PrintBuilder}
     */
    public static SortBuilder from(final Iterable<? extends SortBy> sorts) {
        return (sorts instanceof SortBuilder) ? (SortBuilder)sorts : new SortBuilder(sorts);
    }
}
