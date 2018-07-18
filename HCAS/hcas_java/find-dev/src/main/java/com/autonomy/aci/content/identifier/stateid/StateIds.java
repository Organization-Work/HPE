/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/identifier/stateid/StateIds.java#3 $
 *
 * Copyright (c) 2010, Autonomy Systems Ltd.
 *
 * Last modified by $Author: darrelln $ on $Date: 2010/06/07 $
 */
package com.autonomy.aci.content.identifier.stateid;

import com.autonomy.aci.content.identifier.DocumentIdentifiers;

/**
 * A representation of a set of state ids. A state id identifies a set of documents within a stored state in IDOL. A
 * stored state is primarily identified by an opaque string known as a state token but a state id can also include a
 * zero-based range of indices that restrict the state id to a subset of the documents in the stored state. e.g.
 * <tt>TOKEN[3-6]</tt>. Such ranges are represented by the {@link StateRange} class.
 */
public interface StateIds extends DocumentIdentifiers, Iterable<StateId> {

    /**
     * Combines the state ids in {@code this} with the specified state id and zero-based document index.
     * <p>
     * It is implementation specific whether or not {@code this} is modified or whether a new object is returned
     * instead. If {@code this} is modified it should also be returned.
     *
     * @param stateToken The state token to append
     * @param document The document index for the state token
     * @return The combined state ids object
     */
    StateIds append(String stateToken, int document);

    /**
     * Combines the state ids in {@code this} with the specified state id and zero-based document index range.
     * <p>
     * It is implementation specific whether or not {@code this} is modified or whether a new object is returned
     * instead. If {@code this} is modified it should also be returned.
     *
     * @param stateToken The state token to append
     * @param start The start index of the range
     * @param end The end index of the range
     * @return The combined state ids object
     */
    StateIds append(String stateToken, int start, int end);

    /**
     * Combines the state ids in {@code this} with the specified state id and range.
     * <p>
     * It is implementation specific whether or not {@code this} is modified or whether a new object is returned
     * instead. If {@code this} is modified it should also be returned.
     *
     * @param stateToken The state token to append
     * @param range The index range
     * @return The combined state ids object
     */
    StateIds append(String stateToken, StateRange range);

    /**
     * Combines the state ids in {@code this} with the specified state tokens.
     * <p>
     * It is implementation specific whether or not {@code this} is modified or whether a new object is returned
     * instead. If {@code this} is modified it should also be returned.
     *
     * @param stateTokens The state tokens to append
     * @return The combined state ids object
     */
    StateIds append(String... stateTokens);

    /**
     * Combines the state ids in {@code this} with the specified state ids. The {@link Iterable} can contain instances
     * of {@link String} or {@link StateId}. Note that all instances of {@link StateIds} are suitable iterables.
     * <p>
     * It is implementation specific whether or not {@code this} is modified or whether a new object is returned
     * instead. If {@code this} is modified it should also be returned.
     *
     * @param stateIds The state ids to append
     * @return The combined state ids object
     */
    StateIds append(Iterable<?>... stateIds);

    /**
     * The number of state ids.
     *
     * @return The size
     */
    int size();

    /**
     * {@code StateIds} objects are considered equal if their {@code String} representations are equal.
     *
     * @param obj An object to test for equality
     * @return {@code true} if and only if {@code obj} is a {@code StateIds} object with the same {@code toString()}
     *         value as this object.
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
}