/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/fieldtext/STRINGALL.java#3 $
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
public class STRINGALL extends Specifier {

    public STRINGALL(final String field, final String value, final String... values) {
        this(Arrays.asList(field), value, values);
    }

    public STRINGALL(final String field, final String[] values) {
        this(Arrays.asList(field), values);
    }

    public STRINGALL(final String field, final Iterable<String> values) {
        this(Arrays.asList(field), values);
    }

    public STRINGALL(final String[] fields, final String value, final String... values) {
        this(Arrays.asList(fields), value, values);
    }

    public STRINGALL(final String[] fields, final String[] values) {
        this(Arrays.asList(fields), values);
    }

    public STRINGALL(final String[] fields, final Iterable<String> values) {
        this(Arrays.asList(fields), values);
    }

    public STRINGALL(final Iterable<String> fields, final String value, final String... values) {
        this(fields, InternalUtils.toList(value, values));
    }

    public STRINGALL(final Iterable<String> fields, final String[] values) {
        this(fields, Arrays.asList(values));
    }

    public STRINGALL(final Iterable<String> fields, final Iterable<String> values) {
        super("STRINGALL", fields, values);
        Validate.isTrue(!getValues().isEmpty(), "No values specified");
    }

    public static STRINGALL STRINGALL(final String field, final String value, final String... values) {
        return new STRINGALL(field, value, values);
    }

    public static STRINGALL STRINGALL(final String field, final String[] values) {
        return new STRINGALL(field, values);
    }

    public static STRINGALL STRINGALL(final String field, final Iterable<String> values) {
        return new STRINGALL(field, values);
    }

    public static STRINGALL STRINGALL(final String[] fields, final String value, final String... values) {
        return new STRINGALL(fields, value, values);
    }

    public static STRINGALL STRINGALL(final String[] fields, final String[] values) {
        return new STRINGALL(fields, values);
    }

    public static STRINGALL STRINGALL(final String[] fields, final Iterable<String> values) {
        return new STRINGALL(fields, values);
    }

    public static STRINGALL STRINGALL(final Iterable<String> fields, final String value, final String... values) {
        return new STRINGALL(fields, value, values);
    }

    public static STRINGALL STRINGALL(final Iterable<String> fields, final String[] values) {
        return new STRINGALL(fields, values);
    }

    public static STRINGALL STRINGALL(final Iterable<String> fields, final Iterable<String> values) {
        return new STRINGALL(fields, values);
    }
}
