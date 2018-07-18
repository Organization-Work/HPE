/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/identifier/stateid/StateIdsBuilder.java#3 $
 *
 * Copyright (c) 2010, Autonomy Systems Ltd.
 *
 * Last modified by $Author: darrelln $ on $Date: 2010/06/07 $
 */
package com.autonomy.aci.content.identifier.stateid;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang.Validate;

/**
 * A builder class for aggregating multiple state ids into a single object.
 * <p>
 * This class makes no attempt at internal synchronization and is not safe to be used by multiple threads without
 * external synchronization.
 */
public class StateIdsBuilder extends AbstractStateIds {
    private final Set<StateId> stateIds = new LinkedHashSet<StateId>();

    /**
     * Creates an empty {@code StateIdsBuilder}.
     */
    // Only required because we have 2 var args constructors, causing ambiguity
    public StateIdsBuilder() {

    }

    /**
     * Creates a {@code StateIdsBuilder} containing the specified state token and a single zero-based document index.
     *
     * @param stateToken The initial state token
     * @param document The specific document index
     */
    public StateIdsBuilder(final String stateToken, final int document) {
        doAppend(Arrays.asList(new StateId(stateToken, document)));
    }

    /**
     * Creates a {@code StateIdsBuilder} containing the specified state token and zero-based document index range.
     *
     * @param stateToken The state token
     * @param start The start index of the range
     * @param end The end index of the range
     */
    public StateIdsBuilder(final String stateToken, final int start, final int end) {
        doAppend(Arrays.asList(new StateId(stateToken, start, end)));
    }

    /**
     * Creates a {@code StateIdsBuilder} containing the specified state token and range.
     *
     * @param stateToken The state token
     * @param range The index range
     */
    public StateIdsBuilder(final String stateToken, final StateRange range) {
        doAppend(Arrays.asList(new StateId(stateToken, range)));
    }

    /**
     * Creates a {@code StateIdsBuilder} containing the specified state tokens.
     *
     * @param stateTokens The state tokens
     */
    public StateIdsBuilder(final String... stateTokens) {
        doAppend(Arrays.asList(stateTokens));
    }

    /**
     * Creates a {@code StateIdsBuilder} with the specified state ids. The {@link Iterable} can contain instances of
     * {@link String} or {@link StateId}. Note that all instances of {@link StateIds} are suitable iterables.
     *
     * @param stateIds The state ids
     */
    public StateIdsBuilder(final Iterable<?>... stateIds) {
        doAppend(stateIds);
    }

    /**
     * Appends the specified state token and a single zero-based document index to this instance, returning
     * {@code this}.
     *
     * @param stateToken The state token to append
     * @param document The document index for the state token
     * @return {@code this}
     */
    @Override
    public StateIdsBuilder append(final String stateToken, final int document) {
        return doAppend(Arrays.asList(new StateId(stateToken, document)));
    }

    /**
     * Appends the specified state token and a zero-based document index range to this instance, returning {@code this}.
     *
     * @param stateToken The state token to append
     * @param start The start index of the range
     * @param end The end index of the range
     * @return {@code this}
     */
    @Override
    public StateIdsBuilder append(final String stateToken, final int start, final int end) {
        return doAppend(Arrays.asList(new StateId(stateToken, start, end)));
    }

    /**
     * Appends the specified state token and range to this instance, returning {@code this}.
     *
     * @param stateToken The state token to append
     * @param range The index range
     * @return {@code this}
     */
    @Override
    public StateIdsBuilder append(final String stateToken, final StateRange range) {
        return doAppend(Arrays.asList(new StateId(stateToken, range)));
    }

    /**
     * Appends the specified state tokens to this instance, returning {@code this}.
     *
     * @param stateTokens The state tokens to append
     * @return {@code this}
     */
    @Override
    public StateIdsBuilder append(final String... stateTokens) {
        return doAppend(Arrays.asList(stateTokens));
    }

    /**
     * Appends the specified state ids to this instance, returning {@code this}. The {@link Iterable} can contain
     * instances of {@link String} or {@link StateId}. Note that all instances of {@link StateIds} are suitable
     * iterables.
     *
     * @param stateIds The state ids to append
     * @return {@code this}
     */
    @Override
    public StateIdsBuilder append(final Iterable<?>... stateIds) {
        return doAppend(stateIds);
    }

    private StateIdsBuilder doAppend(final Iterable<?>... stateIds) {
        Validate.notNull(stateIds, "The iterable provided was null");

        // In case of nulls, build a new Set and then add them all
        final Set<StateId> newStateIds = new LinkedHashSet<StateId>();

        for (final Iterable<?> stateIdIterable : stateIds) {
            Validate.notNull(stateIdIterable, "One of the iterables provided was null");

            for(final Object stateId : stateIdIterable) {
                Validate.notNull(stateId, "One of the state ids provided was null");

                if (stateId instanceof StateId) {
                    newStateIds.add((StateId)stateId);
                }
                else if (stateId instanceof String) {
                    newStateIds.add(new StateId((String)stateId));
                }
                else {
                    throw new IllegalArgumentException("One of the state ids was of an unknown class: " + stateId.getClass().getName());
                }
            }
        }

        this.stateIds.addAll(newStateIds);

        return this;
    }

    @Override
    public Iterator<StateId> iterator() {
        // We may later decide to make StateIdsBuilder a full Collection but for now we need to avoid allowing removal
        // via Iterator, so wrap our collection
        return Collections.unmodifiableCollection(stateIds).iterator();
    }

    /**
     * Whether or not the {@code StateIdsBuilder} is empty.
     *
     * @return {@code true} if and only if the size is 0.
     */
    @Override
    public boolean isEmpty() {
        return stateIds.isEmpty();
    }

    /**
     * The number of state ids in the builder.
     *
     * @return The size of the builder
     */
    @Override
    public int size() {
        return stateIds.size();
    }

    /**
     * Converts an iterable of state ids into a {@code StateIdsBuilder}. This method can be more efficient than using
     * the equivalent constructor but the returned object might be backed by the object provided.
     *
     * @param stateIds The state ids to convert
     * @return An equivalent instance of {@code StateIdsBuilder}
     */
    public static StateIdsBuilder from(final Iterable<?> stateIds) {
        return (stateIds instanceof StateIdsBuilder) ? (StateIdsBuilder)stateIds : new StateIdsBuilder(stateIds);
    }

}
