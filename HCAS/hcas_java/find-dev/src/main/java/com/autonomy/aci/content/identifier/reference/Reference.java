/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/identifier/reference/Reference.java#3 $
 *
 * Copyright (c) 2008-2010, Autonomy Systems Ltd.
 *
 * Last modified by $Author: darrelln $ on $Date: 2010/05/24 $
 */

package com.autonomy.aci.content.identifier.reference;

import com.autonomy.aci.client.util.AciURLCodec;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/**
 * A representation of a document reference. A document reference is a string, and optionally a section number, that
 * identifies a document within IDOL's Content server. Though references allow for complex many-to-many relationships
 * with documents, they are usually one-to-one or one-to-many by convention.
 */
public class Reference extends AbstractReferences {
    private final String reference;
    private final int section;

    /**
     * Creates a new {@code Reference} instance for the specified string reference.
     *
     * @param reference The document reference
     */
    public Reference(final String reference) {
        Validate.isTrue(StringUtils.isNotBlank(reference), "Reference must not be blank");

        this.reference = reference;

        section = -1;
    }

    /**
     * Creates a new {@code Reference} instance for the specified string reference and section number.
     *
     * @param reference The document reference
     * @param section The section number
     */
    public Reference(final String reference, final int section) {
        Validate.isTrue(StringUtils.isNotBlank(reference), "Reference must not be blank");
        Validate.isTrue(section >= 0, "Section number must not be less than zero");

        this.reference = reference;
        this.section = section;
    }

    @Override
    public Iterator<Reference> iterator() {
        // Should probably rewrite this to use a custom iterator
        return Arrays.asList(this).iterator();
    }

    /**
     * Accessor for the string reference of the document reference, i.e. the unescaped reference without the section
     * number.
     *
     * @return The document reference
     */
    public String getReference() {
        return reference;
    }

    /**
     * Whether or not this reference has a section number.
     *
     * @return {@code true} if and only if the reference has a section number.
     */
    public boolean hasSection() {
        return section != -1;
    }

    /**
     * Accessor for the section number, if the reference has a section number.
     *
     * @return The section number
     * @throws IllegalStateException if the reference doesn't have a section number
     */
    public int getSection() {
        if (!hasSection()) {
            throw new IllegalStateException("Reference does not have a section number");
        }

        return section;
    }

    /**
     * For a single reference the size is always 1.
     *
     * @return {@code 1}
     */
    @Override
    public int size() {
        return 1;
    }

    /**
     * A single reference can never be empty.
     *
     * @return {@code false}
     */
    @Override
    public boolean isEmpty() {
        return false;
    }

    /**
     * The string representation of this identifier, suitable for use as part of a <tt>DREREPLACE</tt>.
     *
     * <p>Sectioning does not apply for index commands so the section number is discarded.
     *
     * @return A suitably encoded string for use with index commands
     */
    @Override
    public String toIndexingString() {
        return AciURLCodec.getInstance().encode(reference);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(AciURLCodec.getInstance().encode(reference));

        if (hasSection()) {
            builder.append('$').append(section);
        }

        return builder.toString();
    }
}
