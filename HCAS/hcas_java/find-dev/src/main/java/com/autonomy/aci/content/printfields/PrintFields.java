/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/printfields/PrintFields.java#3 $
 *
 * Copyright (c) 2008-2010, Autonomy Systems Ltd.
 *
 * Last modified by $Author: darrelln $ on $Date: 2010/06/07 $
 */

package com.autonomy.aci.content.printfields;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

/**
 * Helper class for building printfields, such as those used in the <tt>printfields</tt> parameter of a query.
 * Individual field names are represented as Strings and deduplicated where possible in a case-sensitive manner. No
 * attempt is currently made to interpret wildcards intelligently.
 *
 * <p>This class makes no attempt at internal synchronization and is not safe to be used by multiple threads without
 * external synchronization.
 */
public class PrintFields implements Iterable<String> {

    private static final Pattern INVALID = Pattern.compile(".*[,\\+\\s].*");
    private static final Pattern SEPARATORS = Pattern.compile("[,\\+\\s]");

    private final Set<String> values = new LinkedHashSet<String>();

    /**
     * Creates a new instance using the field names provided.
     *
     * @param fields The field names
     */
    public PrintFields(final Iterable<String> fields) {
        doAppend(fields);
    }

    /**
     * Creates a new instance using the field names provided.
     *
     * @param fields The field names
     */
    public PrintFields(final String... fields) {
        doAppend(Arrays.asList(fields));
    }

    /**
     * Appends the given field names to this instance.
     *
     * @param fields The names of the fields
     * @return {@code this}
     */
    public PrintFields append(final Iterable<String> fields) {
        return doAppend(fields);
    }

    /**
     * Appends the given field names to this instance.
     *
     * @param fields The names of the fields
     * @return {@code this}
     */
    public PrintFields append(final String... fields) {
        return doAppend(Arrays.asList(fields));
    }

    private PrintFields doAppend(final Iterable<String> fields) {
        Validate.notNull(fields, "Values must not be null");

        // In case of nulls, build a new Set and then add them all
        final Set<String> newFields = new LinkedHashSet<String>();

        for(final String field : fields) {
            Validate.isTrue(StringUtils.isNotBlank(field), "One of the fields provided was blank");
            Validate.isTrue(!INVALID.matcher(field).matches(), "A field name cannot contain a plus, comma or whitespace: [" + field + "]");

            newFields.add(field);
        }

        this.values.addAll(newFields);

        return this;
    }

    /**
     * {@link Iterator} for the field names. Removing elements is not currently supported.
     *
     * @return A suitable iterator
     */
    @Override
    public Iterator<String> iterator() {
        return values.iterator();
    }

    /**
     * The number of field names currently in this instance. Note that the number of fields actually returned from a
     * query could be considerably different.
     *
     * @return Number of fields
     */
    public int size() {
        return values.size();
    }

    /**
     * Whether or not size is 0. An empty {@code PrintFields} will have an empty string representation, which IDOL will
     * interpret as matching no content fields.
     *
     * @return {@code true} if and only if {@code size() == 0}
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * Two {@code PrintFields} objects are considered equal if they contain the same set of field names. Equality is
     * order invariant but case-sensitive. Note that IDOL can be configured to be either case-sensitive or
     * case-insensitive so this class errs on the side of caution.
     *
     * @param obj A object to test for equality
     * @return {@code true} if the {@code obj} is equal to {@code this}
     */
    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj instanceof PrintFields && this.values.equals(((PrintFields) obj).values);
    }

    /**
     * A suitable hashcode implementation.
     *
     * @return The hashcode
     */
    @Override
    public int hashCode() {
        return values.hashCode();
    }

    /**
     * A {@code String} representation of the databases, suitable for use in a <tt>query</tt> or <tt>getcontent</tt>
     * action.
     *
     * @return The string representation
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();

        for(final String value : this) {
            // Oddly, this doesn't need URL encoding
            builder.append(value).append('+');
        }

        if(builder.length() != 0) {
            builder.deleteCharAt(builder.length() - 1);
        }

        return builder.toString();
    }

    /**
     * Converts an iterable of strings into a {@code PrintFields}. This method can be more efficient than using the
     * equivalent constructor but the returned object might be backed by the object provided.
     *
     * @param fields The references to convert
     * @return An equivalent instance of {@code PrintFields}
     */
    public static PrintFields from(final Iterable<String> fields) {
        return (fields instanceof PrintFields) ? (PrintFields)fields : new PrintFields(fields);
    }

    /**
     * Parser for a printfields string.
     *
     * @param printFields A valid IDOL printfields
     * @return A parsed {@code PrintFields}
     */
    public static PrintFields parse(final String printFields) {
        Validate.notNull(printFields, "PrintFields must not be null");

        final Set<String> printFieldsSet = new LinkedHashSet<String>(Arrays.asList(SEPARATORS.split(printFields)));
        printFieldsSet.remove("");

        return new PrintFields(printFieldsSet);
    }
}
