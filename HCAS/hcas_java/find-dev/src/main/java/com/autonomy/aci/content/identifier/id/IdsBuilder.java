/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/identifier/id/IdsBuilder.java#3 $
 *
 * Copyright (c) 2010, Autonomy Systems Ltd.
 *
 * Last modified by $Author: darrelln $ on $Date: 2010/05/20 $
 */
package com.autonomy.aci.content.identifier.id;

import com.autonomy.aci.content.internal.InternalUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.Validate;

/**
 * A builder class for aggregating multiple ids into a single object.
 */
public class IdsBuilder extends AbstractIds {
    private final SortedSet<Id> ids = new TreeSet<Id>();

    /**
     * Creates an empty {@code IdsBuilder}.
     */
    // Only required because we have 2 var args constructors, causing ambiguity
    public IdsBuilder() {

    }

    /**
     * Creates an {@code IdsBuilder} with the specified ids.
     *
     * @param ids The initial ids
     */
    public IdsBuilder(final int... ids) {
        doAppend(Arrays.asList(InternalUtils.wrap(ids)));
    }

    /**
     * Creates an {@code IdsBuilder} with the specified ids. The {@link Iterable} can contain instances of
     * {@link Number} or {@link Id}. Note that all instances of {@link Ids} are suitable iterables.
     *
     * @param ids The initial ids
     */
    public IdsBuilder(final Iterable<?>... ids) {
        doAppend(ids);
    }

    /**
     * Appends the specified ids to this instance, returning {@code this}.
     *
     * @param ids The ids to append
     * @return {@code this}
     */
    @Override
    public IdsBuilder append(final int... ids) {
        return doAppend(Arrays.asList(InternalUtils.wrap(ids)));
    }

    /**
     * Appends the specified ids to this instance, returning {@code this}. The {@link Iterable} can contain instances of
     * {@link Number} or {@link Id}. Note that all instances of {@link Ids} are suitable iterables.
     *
     * @param ids The ids to append
     * @return {@code this}
     */
    @Override
    public IdsBuilder append(final Iterable<?>... ids) {
        return doAppend(ids);
    }

    private IdsBuilder doAppend(final Iterable<?>... ids) {
        Validate.notNull(ids, "The iterable provided was null");

        // In case of nulls, build a new Set and then add them all
        final Set<Id> newIds = new TreeSet<Id>();

        for(final Iterable<?> idIterable : ids) {
            Validate.notNull(idIterable, "One of the iterables provided was null");

            for(final Object id : idIterable) {
                Validate.notNull(id, "One of the ids provided was null");

                if(id instanceof Id) {
                    newIds.add((Id)id);
                }
                else if (id instanceof Number) {
                    newIds.add(new Id(((Number)id).intValue()));
                }
                else {
                    throw new IllegalArgumentException("One of the ids was of an unknown class: " + id.getClass().getName());
                }
            }
        }

        this.ids.addAll(newIds);

        return this;
    }

    @Override
    public Iterator<Id> iterator() {
        // We may later decide to make IdsBuilder a full Collection but for now we need to avoid allowing removal
        // via Iterator, so wrap our collection
        return Collections.unmodifiableCollection(ids).iterator();
    }

    /**
     * Whether or not the {@code IdsBuilder} is empty.
     *
     * @return {@code true} if and only if the size is 0.
     */
    @Override
    public boolean isEmpty() {
        return ids.isEmpty();
    }

    /**
     * The number of ids in the builder.
     *
     * @return The size of the builder
     */
    @Override
    public int size() {
        return ids.size();
    }

    /**
     * Converts an iterable of ids into an {@code IdsBuilder}. This method can be more efficient than using the
     * equivalent constructor but the returned object might be backed by the object provided.
     *
     * @param ids The ids to convert
     * @return An equivalent instance of {@code IdsBuilder}
     */
    public static IdsBuilder from(final Iterable<?> ids) {
        return (ids instanceof IdsBuilder) ? (IdsBuilder)ids : new IdsBuilder(ids);
    }
}
