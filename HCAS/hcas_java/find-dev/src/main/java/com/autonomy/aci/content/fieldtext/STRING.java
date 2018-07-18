/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/fieldtext/STRING.java#3 $
 *
 * Copyright (c) 2008-2010, Autonomy Systems Ltd.
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
public class STRING extends Specifier {

    public STRING(final String field, final String value, final String... values) {
        this(Arrays.asList(field), value, values);
    }

    public STRING(final String field, final String[] values) {
        this(Arrays.asList(field), values);
    }

    public STRING(final String field, final Iterable<String> values) {
        this(Arrays.asList(field), values);
    }

    public STRING(final String[] fields, final String value, final String... values) {
        this(Arrays.asList(fields), value, values);
    }

    public STRING(final String[] fields, final String[] values) {
        this(Arrays.asList(fields), values);
    }

    public STRING(final String[] fields, final Iterable<String> values) {
        this(Arrays.asList(fields), values);
    }

    public STRING(final Iterable<String> fields, final String value, final String... values) {
        this(fields, InternalUtils.toList(value, values));
    }

    public STRING(final Iterable<String> fields, final String[] values) {
        this(fields, Arrays.asList(values));
    }

    public STRING(final Iterable<String> fields, final Iterable<String> values) {
        super("STRING", fields, values);
        Validate.isTrue(!getValues().isEmpty(), "No values specified");
    }

    public static STRING STRING(final String field, final String value, final String... values) {
        return new STRING(field, value, values);
    }

    public static STRING STRING(final String field, final String[] values) {
        return new STRING(field, values);
    }

    public static STRING STRING(final String field, final Iterable<String> values) {
        return new STRING(field, values);
    }

    public static STRING STRING(final String[] fields, final String value, final String... values) {
        return new STRING(fields, value, values);
    }

    public static STRING STRING(final String[] fields, final String[] values) {
        return new STRING(fields, values);
    }

    public static STRING STRING(final String[] fields, final Iterable<String> values) {
        return new STRING(fields, values);
    }

    public static STRING STRING(final Iterable<String> fields, final String value, final String... values) {
        return new STRING(fields, value, values);
    }

    public static STRING STRING(final Iterable<String> fields, final String[] values) {
        return new STRING(fields, values);
    }

    public static STRING STRING(final Iterable<String> fields, final Iterable<String> values) {
        return new STRING(fields, values);
    }
}
