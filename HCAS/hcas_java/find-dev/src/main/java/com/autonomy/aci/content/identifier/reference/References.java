/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/identifier/reference/References.java#3 $
 *
 * Copyright (c) 2009-2010, Autonomy Systems Ltd.
 *
 * Last modified by $Author: darrelln $ on $Date: 2010/06/01 $
 */
package com.autonomy.aci.content.identifier.reference;

import com.autonomy.aci.content.identifier.DocumentIdentifiers;

/**
 * A representation of a set of document references.
 */
public interface References extends DocumentIdentifiers, Iterable<Reference> {

    /**
     * Combines the references in {@code this} with the specified reference and section number.
     * <p>
     * It is implementation specific whether or not {@code this} is modified or whether a new object is returned
     * instead. If {@code this} is modified it should also be returned.
     *
     * @param reference The reference to append
     * @param section The section number of the reference to append
     * @return The combined references object
     */
    References append(String reference, int section);

    /**
     * Combines the references in {@code this} with the specified references.
     * <p>
     * It is implementation specific whether or not {@code this} is modified or whether a new object is returned
     * instead. If {@code this} is modified it should also be returned.
     *
     * @param references The references to append
     * @return The combined references object
     */
    References append(String... references);

    /**
     * Combines the references in {@code this} with the specified references. The {@link Iterable} can contain instances
     * of {@link String} or {@link Reference}. Note that all instances of {@link References} are suitable iterables.
     * <p>
     * It is implementation specific whether or not {@code this} is modified or whether a new object is returned
     * instead. If {@code this} is modified it should also be returned.
     *
     * @param references The reference to append
     * @return The combined references object
     */
    References append(Iterable<?>... references);

    /**
     * The number of references.
     *
     * @return The size
     */
    int size();

    /**
     * {@code References} objects are considered equal if their {@code String} representations are equal.
     *
     * @param obj An object to test for equality
     * @return {@code true} if and only if {@code obj} is a {@code References} object with the same {@code toString()}
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