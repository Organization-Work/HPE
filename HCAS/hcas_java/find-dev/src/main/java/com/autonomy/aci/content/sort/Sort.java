/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/sort/Sort.java#3 $
 *
 * Copyright (c) 2008-2010, Autonomy Systems Ltd.
 *
 * Last modified by $Author: darrelln $ on $Date: 2010/06/07 $
 */

package com.autonomy.aci.content.sort;

/**
 * Interface for representing IDOL sort expressions. An example expression might be:
 *
 * <pre>
 * relevance+MYFIELD:increasing
 * </pre>
 *
 * The {@link SortBuilder} class can be used to combine sort expressions into more complex expressions. However, it is
 * often unnecessary to explicitly instantiate a {@link SortBuilder} as the {@code Sort} interface forces all specifiers
 * to provide support for combining expressions.
 * <p/>
 * {@link SortBy} provides static fields and methods for working with the most common sort specifiers.
 * </p>
 * Example:
 *
 * <pre>
 * Sort sort = SortBy.RELEVANCE.then(SortBy.increasing("MYFIELD"));</pre>
 *
 * This becomes even simpler using static imports:
 *
 * <pre>
 * Sort sort = RELEVANCE.then(increasing("MYFIELD"));
 * </pre>
 */
public interface Sort extends Iterable<SortBy> {
    /**
     * Builds a complex sort expression where the current sort is used first and the specified sorts are used in the
     * event of a tie. The current object may or may not be modified.
     *
     * @param sorts Sorting specifiers to add to the current sort
     * @return The composite sort expression
     */
    Sort then(Iterable<? extends SortBy>... sorts);

    /**
     * The number of sort specifiers in the sort. This should be equal to the number of items in the iterator.
     *  
     * @return The current number of specifiers in the sort
     */
    int size();

    /**
     * Whether or not the {@code Sort} is empty.
     *
     * @return {@code true} if and only if {@code size() == 0}
     */
    boolean isEmpty();

    /**
     * Two {@code Sort} objects are considered equal if they have the same {@link #toString()} representation.
     *
     * @param obj An object to test for equality
     * @return {@code true} if and only if {@code obj} is a {@code Sort} object with the same {@link #toString()} value
     *         as this object
     */
    @Override
    boolean equals(Object obj);

    /**
     * The hashcode should be that of the {@code String} representation.
     *
     * @return The hashcode of the {@code String} representation.
     */
    @Override
    int hashCode();

    /**
     * The {@code String} representation of the sort expression, as it should be sent to IDOL.
     *
     * @return The {@code String} representation
     */
    @Override
    String toString();
}
