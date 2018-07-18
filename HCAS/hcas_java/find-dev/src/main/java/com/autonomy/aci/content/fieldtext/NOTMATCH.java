/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/fieldtext/NOTMATCH.java#2 $
 *
 * Copyright (c) 2010, Autonomy Systems Ltd.
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
public class NOTMATCH extends Specifier {

    public NOTMATCH(final String field, final String value, final String... values) {
        this(Arrays.asList(field), value, values);
    }

    public NOTMATCH(final String field, final String[] values) {
        this(Arrays.asList(field), values);
    }

    public NOTMATCH(final String field, final Iterable<String> values) {
        this(Arrays.asList(field), values);
    }

    public NOTMATCH(final String[] fields, final String value, final String... values) {
        this(Arrays.asList(fields), value, values);
    }

    public NOTMATCH(final String[] fields, final String[] values) {
        this(Arrays.asList(fields), values);
    }

    public NOTMATCH(final String[] fields, final Iterable<String> values) {
        this(Arrays.asList(fields), values);
    }

    public NOTMATCH(final Iterable<String> fields, final String value, final String... values) {
        this(fields, InternalUtils.toList(value, values));
    }

    public NOTMATCH(final Iterable<String> fields, final String[] values) {
        this(fields, Arrays.asList(values));
    }

    public NOTMATCH(final Iterable<String> fields, final Iterable<String> values) {
        super("NOTMATCH", fields, values);
        Validate.isTrue(!getValues().isEmpty(), "No values specified");
    }

    public static NOTMATCH NOTMATCH(final String field, final String value, final String... values) {
        return new NOTMATCH(field, value, values);
    }

    public static NOTMATCH NOTMATCH(final String field, final String[] values) {
        return new NOTMATCH(field, values);
    }

    public static NOTMATCH NOTMATCH(final String field, final Iterable<String> values) {
        return new NOTMATCH(field, values);
    }

    public static NOTMATCH NOTMATCH(final String[] fields, final String value, final String... values) {
        return new NOTMATCH(fields, value, values);
    }

    public static NOTMATCH NOTMATCH(final String[] fields, final String[] values) {
        return new NOTMATCH(fields, values);
    }

    public static NOTMATCH NOTMATCH(final String[] fields, final Iterable<String> values) {
        return new NOTMATCH(fields, values);
    }

    public static NOTMATCH NOTMATCH(final Iterable<String> fields, final String value, final String... values) {
        return new NOTMATCH(fields, value, values);
    }

    public static NOTMATCH NOTMATCH(final Iterable<String> fields, final String[] values) {
        return new NOTMATCH(fields, values);
    }

    public static NOTMATCH NOTMATCH(final Iterable<String> fields, final Iterable<String> values) {
        return new NOTMATCH(fields, values);
    }
}
