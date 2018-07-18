/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/identifier/id/Id.java#3 $
 *
 * Copyright (c) 2010, Autonomy Systems Ltd.
 *
 * Last modified by $Author: darrelln $ on $Date: 2010/06/01 $
 */
package com.autonomy.aci.content.identifier.id;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.lang.Validate;

/**
 * A representation of a document id. A document id is a number that uniquely identifies a document within a particular
 * instance of IDOL's Content server, as found in the &lt;autn:id&gt; tag of a query response. Note that the use of
 * document ids is usually discouraged because they are ambiguous in a distributed system - document references are
 * preferred.
 */
public class Id extends AbstractIds implements Comparable<Id> {
    private final int id;

    /**
     * Creates a new {@code Id} instance for the specified numeric document id.
     *
     * @param id The document id
     */
    public Id(final int id) {
        Validate.isTrue(id > 0, "ID must be at least 1");
        this.id = id;
    }

    @Override
    public Iterator<Id> iterator() {
        // Should probably rewrite this to use a custom iterator
        return Arrays.asList(this).iterator();
    }

    /**
     * Accessor for the numeric value of the document id.
     *
     * @return The document id
     */
    public int getId() {
        return id;
    }

    /**
     * For a single id the size is always 1.
     *
     * @return {@code 1}
     */
    @Override
    public int size() {
        return 1;
    }

    /**
     * A single id can never be empty.
     *
     * @return {@code false}
     */
    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }

    /**
     * Provides a natural ordering for the Id class such that Ids can be sorted into increasing numerical order.
     *
     * @param id An id object for comparision
     * @return {@code -1}, {@code 0} or {@code 1}
     */
    @Override
    public int compareTo(final Id id) {
        return Integer.valueOf(this.id).compareTo(id.id);
    }
}
