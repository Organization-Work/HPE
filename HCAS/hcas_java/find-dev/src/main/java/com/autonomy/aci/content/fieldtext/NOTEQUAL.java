/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/fieldtext/NOTEQUAL.java#4 $
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
public class NOTEQUAL extends Specifier {
    public <T extends Number> NOTEQUAL(final String field, final T value, final T... values) {
        this(field, InternalUtils.<Number>toList(value, values));
    }

    public <T extends Number> NOTEQUAL(final String field, final T[] values) {
        this(field, Arrays.asList(values));
    }

    public NOTEQUAL(final String field, final double[] values) {
        this(field, InternalUtils.doublesToIterable(values));
    }

    public NOTEQUAL(final String field, final int[] values) {
        this(field, InternalUtils.intsToIterable(values));
    }

    public NOTEQUAL(final String field, final long[] values) {
        this(field, InternalUtils.longsToIterable(values));
    }

    public NOTEQUAL(final String field, final Iterable<? extends Number> values) {
        this(Arrays.asList(field), values);
    }

    public <T extends Number> NOTEQUAL(final String[] fields, final T value, final T... values) {
        this(fields, InternalUtils.<Number>toList(value, values));
    }

    public <T extends Number> NOTEQUAL(final String[] fields, final T[] values) {
        this(fields, Arrays.asList(values));
    }

    public NOTEQUAL(final String[] fields, final double[] values) {
        this(fields, InternalUtils.doublesToIterable(values));
    }

    public NOTEQUAL(final String[] fields, final int[] values) {
        this(fields, InternalUtils.intsToIterable(values));
    }

    public NOTEQUAL(final String[] fields, final long[] values) {
        this(fields, InternalUtils.longsToIterable(values));
    }

    public NOTEQUAL(final String[] fields, final Iterable<? extends Number> values) {
        this(Arrays.asList(fields), values);
    }

    public <T extends Number> NOTEQUAL(final Iterable<? extends String> fields, final T value, final T... values) {
        this(fields, InternalUtils.<Number>toList(value, values));
    }

    public <T extends Number> NOTEQUAL(final Iterable<? extends String> fields, final T[] values) {
        this(fields, Arrays.asList(values));
    }

    public NOTEQUAL(final Iterable<? extends String> fields, final double[] values) {
        this(fields, InternalUtils.doublesToIterable(values));
    }

    public NOTEQUAL(final Iterable<? extends String> fields, final int[] values) {
        this(fields, InternalUtils.intsToIterable(values));
    }

    public NOTEQUAL(final Iterable<? extends String> fields, final long[] values) {
        this(fields, InternalUtils.longsToIterable(values));
    }

    public NOTEQUAL(final Iterable<? extends String> fields, final Iterable<? extends Number> values) {
        super("NOTEQUAL", fields, InternalUtils.numbersToStrings(values));
        Validate.isTrue(!getValues().isEmpty(), "No values specified");
    }

    public final double[] getNumericValues() {
        return InternalUtils.stringsToDoubles(getValues());
    }

    public static <T extends Number> NOTEQUAL NOTEQUAL(final String field, final T value, final T... values) {
        return new NOTEQUAL(field, value, values);
    }

    public static <T extends Number> NOTEQUAL NOTEQUAL(final String field, final T[] values) {
        return new NOTEQUAL(field, values);
    }

    public static NOTEQUAL NOTEQUAL(final String field, final double[] values) {
        return new NOTEQUAL(field, values);
    }

    public static NOTEQUAL NOTEQUAL(final String field, final int[] values) {
        return new NOTEQUAL(field, values);
    }

    public static NOTEQUAL NOTEQUAL(final String field, final long[] values) {
        return new NOTEQUAL(field, values);
    }

    public static NOTEQUAL NOTEQUAL(final String field, final Iterable<? extends Number> values) {
        return new NOTEQUAL(field, values);
    }

    public static <T extends Number> NOTEQUAL NOTEQUAL(final String[] fields, final T value, final T... values) {
        return new NOTEQUAL(fields, value, values);
    }

    public static <T extends Number> NOTEQUAL NOTEQUAL(final String[] fields, final T[] values) {
        return new NOTEQUAL(fields, values);
    }

    public static NOTEQUAL NOTEQUAL(final String[] fields, final double[] values) {
        return new NOTEQUAL(fields, values);
    }

    public static NOTEQUAL NOTEQUAL(final String[] fields, final int[] values) {
        return new NOTEQUAL(fields, values);
    }

    public static NOTEQUAL NOTEQUAL(final String[] fields, final long[] values) {
        return new NOTEQUAL(fields, values);
    }

    public static NOTEQUAL NOTEQUAL(final String[] fields, final Iterable<? extends Number> values) {
        return new NOTEQUAL(fields, values);
    }

    public static <T extends Number> NOTEQUAL NOTEQUAL(final Iterable<? extends String> fields, final T value, final T... values) {
        return new NOTEQUAL(fields, value, values);
    }

    public static <T extends Number> NOTEQUAL NOTEQUAL(final Iterable<? extends String> fields, final T[] values) {
        return new NOTEQUAL(fields, values);
    }

    public static NOTEQUAL NOTEQUAL(final Iterable<? extends String> fields, final double[] values) {
        return new NOTEQUAL(fields, values);
    }

    public static NOTEQUAL NOTEQUAL(final Iterable<? extends String> fields, final int[] values) {
        return new NOTEQUAL(fields, values);
    }

    public static NOTEQUAL NOTEQUAL(final Iterable<? extends String> fields, final long[] values) {
        return new NOTEQUAL(fields, values);
    }

    public static NOTEQUAL NOTEQUAL(final Iterable<? extends String> fields, final Iterable<? extends Number> values) {
        return new NOTEQUAL(fields, values);
    }
}
