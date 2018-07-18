/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/fieldtext/MATCHALL.java#3 $
 *
 * Copyright (c) 2009-2010, Autonomy Systems Ltd.
 *
 * Last modified by $Author: darrelln $ on $Date: 2010/06/07 $
 */
package com.autonomy.aci.content.fieldtext;

import com.autonomy.aci.content.internal.InternalUtils;

import java.util.Arrays;

import org.apache.commons.lang.Validate;

/**
 *
 */
public class MATCHALL extends Specifier {

    public MATCHALL(final String field, final String value, final String... values) {
        this(Arrays.asList(field), value, values);
    }

    public MATCHALL(final String field, final String[] values) {
        this(Arrays.asList(field), values);
    }

    public MATCHALL(final String field, final Iterable<String> values) {
        this(Arrays.asList(field), values);
    }

    public MATCHALL(final String[] fields, final String value, final String... values) {
        this(Arrays.asList(fields), value, values);
    }

    public MATCHALL(final String[] fields, final String[] values) {
        this(Arrays.asList(fields), values);
    }

    public MATCHALL(final String[] fields, final Iterable<String> values) {
        this(Arrays.asList(fields), values);
    }

    public MATCHALL(final Iterable<String> fields, final String value, final String... values) {
        this(fields, InternalUtils.toList(value, values));
    }

    public MATCHALL(final Iterable<String> fields, final String[] values) {
        this(fields, Arrays.asList(values));
    }

    public MATCHALL(final Iterable<String> fields, final Iterable<String> values) {
        super("MATCHALL", fields, values);
        Validate.isTrue(!getValues().isEmpty(), "No values specified");
    }

    public static MATCHALL MATCHALL(final String field, final String value, final String... values) {
        return new MATCHALL(field, value, values);
    }

    public static MATCHALL MATCHALL(final String field, final String[] values) {
        return new MATCHALL(field, values);
    }

    public static MATCHALL MATCHALL(final String field, final Iterable<String> values) {
        return new MATCHALL(field, values);
    }

    public static MATCHALL MATCHALL(final String[] fields, final String value, final String... values) {
        return new MATCHALL(fields, value, values);
    }

    public static MATCHALL MATCHALL(final String[] fields, final String[] values) {
        return new MATCHALL(fields, values);
    }

    public static MATCHALL MATCHALL(final String[] fields, final Iterable<String> values) {
        return new MATCHALL(fields, values);
    }

    public static MATCHALL MATCHALL(final Iterable<String> fields, final String value, final String... values) {
        return new MATCHALL(fields, value, values);
    }

    public static MATCHALL MATCHALL(final Iterable<String> fields, final String[] values) {
        return new MATCHALL(fields, values);
    }

    public static MATCHALL MATCHALL(final Iterable<String> fields, final Iterable<String> values) {
        return new MATCHALL(fields, values);
    }
}
