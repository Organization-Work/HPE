/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/identifier/reference/ReferencesBuilder.java#3 $
 *
 * Copyright (c) 2008-2010, Autonomy Systems Ltd.
 *
 * Last modified by $Author: darrelln $ on $Date: 2010/06/01 $
 */

package com.autonomy.aci.content.identifier.reference;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang.Validate;

/**
 * A builder class for aggregating multiple references into a single object.
 * <p>
 * This class makes no attempt at internal synchronization and is not safe to be used by multiple threads without
 * external synchronization.
 */
public class ReferencesBuilder extends AbstractReferences {
    private final Set<Reference> references = new LinkedHashSet<Reference>();

    /**
     * Creates an empty {@code ReferencesBuilder}.
     */
    // Only required because we have 2 var args constructors, causing ambiguity
    public ReferencesBuilder() {

    }

    /**
     * Creates a {@code ReferencesBuilder} with the specified reference and section number as the only entry.
     *
     * @param reference The initial ids
     * @param section The section number
     */
    public ReferencesBuilder(final String reference, final int section) {
        doAppend(Arrays.asList(new Reference(reference, section)));
    }

    /**
     * Creates a {@code ReferencesBuilder} with the specified references.
     *
     * @param references The initial references
     */
    public ReferencesBuilder(final String... references) {
        doAppend(Arrays.asList(references));
    }

    /**
     * Creates a {@code ReferencesBuilder} with the specified references. The {@link Iterable} can contain instances of
     * {@link String} or {@link Reference}. Note that all instances of {@link References} are suitable iterables.
     *
     * @param references The initial references
     */
    public ReferencesBuilder(final Iterable<?>... references) {
        doAppend(references);
    }  

    /**
     * Appends the specified reference and section number to this instance, returning {@code this}.
     *
     * @param reference The reference to append
     * @param section The section number of the reference to append
     * @return {@code this}
     */
    @Override
    public ReferencesBuilder append(final String reference, final int section) {
        return doAppend(Arrays.asList(new Reference(reference, section)));
    }

    /**
     * Appends the specified references to this instance, returning {@code this}.
     *
     * @param references The references to append
     * @return {@code this}
     */
    @Override
    public ReferencesBuilder append(final String... references) {
        return doAppend(Arrays.asList(references));
    }

    /**
     * Appends the specified references to this instance, returning {@code this}.
     *
     * @param references The references to append
     * @return {@code this}
     */
    @Override
    public ReferencesBuilder append(final Iterable<?>... references) {
        return doAppend(references);
    }

    private ReferencesBuilder doAppend(final Iterable<?>... references) {
        Validate.notNull(references, "The iterable provided was null");

        // In case of nulls, build a new Set and then add them all
        final Set<Reference> newReferences = new LinkedHashSet<Reference>();

        for(final Iterable<?> referenceIterable : references) {
            Validate.notNull(referenceIterable, "One of the iterables provided was null");

            for(final Object reference : referenceIterable) {
                Validate.notNull(reference, "One of the references provided was null");

                if(reference instanceof Reference) {
                    newReferences.add((Reference)reference);
                }
                else if (reference instanceof String) {
                    newReferences.add(new Reference((String)reference));
                }
                else {
                    throw new IllegalArgumentException("One of the references was of an unknown class: " + reference.getClass().getName());
                }
            }
        }

        this.references.addAll(newReferences);

        return this;
    }

    @Override
    public Iterator<Reference> iterator() {
        // We may later decide to make ReferencesBuilder a full Collection but for now we need to avoid allowing removal
        // via Iterator, so wrap our collection
        return Collections.unmodifiableCollection(references).iterator();
    }

    /**
     * Whether or not the {@code ReferencesBuilder} is empty.
     *
     * @return {@code true} if and only if the size is 0.
     */
    @Override
    public boolean isEmpty() {
        return references.isEmpty();
    }

    /**
     * The number of references in the builder.
     *
     * @return The size of the builder
     */
    @Override
    public int size() {
        return references.size();
    }

    /**
     * Converts an iterable of references into a {@code ReferencesBuilder}. This method can be more efficient than using
     * the equivalent constructor but the returned object might be backed by the object provided.
     *
     * @param references The references to convert
     * @return An equivalent instance of {@code ReferencesBuilder}
     */
    public static ReferencesBuilder from(final Iterable<?> references) {
        return (references instanceof ReferencesBuilder) ? (ReferencesBuilder)references : new ReferencesBuilder(references);
    }
}
