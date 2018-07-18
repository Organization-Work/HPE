/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/fieldtext/NOTWILD.java#2 $
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
public class NOTWILD extends Specifier {

    public NOTWILD(final String field, final String value, final String... values) {
        this(Arrays.asList(field), value, values);
    }

    public NOTWILD(final String field, final String[] values) {
        this(Arrays.asList(field), values);
    }

    public NOTWILD(final String field, final Iterable<String> values) {
        this(Arrays.asList(field), values);
    }

    public NOTWILD(final String[] fields, final String value, final String... values) {
        this(Arrays.asList(fields), value, values);
    }

    public NOTWILD(final String[] fields, final String[] values) {
        this(Arrays.asList(fields), values);
    }

    public NOTWILD(final String[] fields, final Iterable<String> values) {
        this(Arrays.asList(fields), values);
    }

    public NOTWILD(final Iterable<String> fields, final String value, final String... values) {
        this(fields, InternalUtils.toList(value, values));
    }

    public NOTWILD(final Iterable<String> fields, final String[] values) {
        this(fields, Arrays.asList(values));
    }

    public NOTWILD(final Iterable<String> fields, final Iterable<String> values) {
        super("NOTWILD", fields, values);
        Validate.isTrue(!getValues().isEmpty(), "No values specified");
    }

    public static NOTWILD NOTWILD(final String field, final String value, final String... values) {
        return new NOTWILD(field, value, values);
    }

    public static NOTWILD NOTWILD(final String field, final String[] values) {
        return new NOTWILD(field, values);
    }

    public static NOTWILD NOTWILD(final String field, final Iterable<String> values) {
        return new NOTWILD(field, values);
    }

    public static NOTWILD NOTWILD(final String[] fields, final String value, final String... values) {
        return new NOTWILD(fields, value, values);
    }

    public static NOTWILD NOTWILD(final String[] fields, final String[] values) {
        return new NOTWILD(fields, values);
    }

    public static NOTWILD NOTWILD(final String[] fields, final Iterable<String> values) {
        return new NOTWILD(fields, values);
    }

    public static NOTWILD NOTWILD(final Iterable<String> fields, final String value, final String... values) {
        return new NOTWILD(fields, value, values);
    }

    public static NOTWILD NOTWILD(final Iterable<String> fields, final String[] values) {
        return new NOTWILD(fields, values);
    }

    public static NOTWILD NOTWILD(final Iterable<String> fields, final Iterable<String> values) {
        return new NOTWILD(fields, values);
    }
}
