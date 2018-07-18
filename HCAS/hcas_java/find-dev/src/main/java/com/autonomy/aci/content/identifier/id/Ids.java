/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/identifier/id/Ids.java#3 $
 *
 * Copyright (c) 2010, Autonomy Systems Ltd.
 *
 * Last modified by $Author: darrelln $ on $Date: 2010/05/24 $
 */
package com.autonomy.aci.content.identifier.id;

import com.autonomy.aci.content.identifier.DocumentIdentifiers;

/**
 * A representation of a set of document ids.
 */
public interface Ids extends DocumentIdentifiers, Iterable<Id> {

    /**
     * Combines the ids in {@code this} with the specified ids.
     * <p>
     * It is implementation specific whether or not {@code this} is modified or whether a new object is returned
     * instead. If {@code this} is modified it should also be returned.
     *
     * @param ids The ids to append
     * @return The combined ids object
     */
    Ids append(int... ids);

    /**
     * Combines the ids in {@code this} with the specified ids.
     * <p>
     * It is implementation specific whether or not {@code this} is modified or whether a new object is returned
     * instead. If {@code this} is modified it should also be returned.
     *
     * @param ids The ids to append
     * @return The combined ids object
     */
    Ids append(Iterable<?>... ids);

    /**
     * The number of ids.
     *
     * @return The number of ids.
     */
    int size();

    /**
     * {@code Ids} objects are considered equal if their {@code String} representations are equal.
     *
     * @param obj An object to test for equality
     * @return {@code true} if and only if {@code obj} is an {@code Ids} object with the same {@code toString()} value
     *         as this object.
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