/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/internal/InternalUtils.java#2 $
 *
 * Copyright (c) 2008-2010, Autonomy Systems Ltd.
 *
 * Last modified by $Author: darrelln $ on $Date: 2010/06/07 $
 */

package com.autonomy.aci.content.internal;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public final class InternalUtils {

    /**
     * Utilities class, cannot be instantiated.
     */
    private InternalUtils() {
    }

    public static String doubleToString(final double number) {

        final double abs = Math.abs(number);

        final DecimalFormat format;

        if(abs < 100.0) {
            format = new DecimalFormat("#.#####");
        }
        else if(abs < 1000.0) {
            format = new DecimalFormat("#.####");
        }
        else if(abs < 10000.0) {
            format = new DecimalFormat("#.###");
        }
        else if(abs < 30000.0) {
            format = new DecimalFormat("#.##");
        }
        else if(abs < 100000.0) {
            format = new DecimalFormat("#.#");
        }
        else {
            format = new DecimalFormat("#");
        }

        return format.format(number);
    }

    public static Iterable<Double> doublesToIterable(final double... numbers) {
        if (numbers == null) {
            return null;
        }

        return Arrays.asList(wrap(numbers));
    }

    public static Iterable<Integer> intsToIterable(final int... numbers) {
        if (numbers == null) {
            return null;
        }

        return Arrays.asList(wrap(numbers));
    }

    public static Iterable<Long> longsToIterable(final long... numbers) {
        if (numbers == null) {
            return null;
        }

        return Arrays.asList(wrap(numbers));
    }


    public static String[] doublesToStrings(final double... numbers) {
        if(numbers == null) {
            return null;
        }

        final String[] strings = new String[numbers.length];

        int index = 0;

        for(final double number : numbers) {
            strings[index++] = doubleToString(number);
        }

        return strings;
    }

    public static String[] intsToStrings(final int... numbers) {
        if (numbers == null) {
            return null;
        }

        final String[] strings = new String[numbers.length];

        int index = 0;

        for (final int number : numbers) {
            strings[index++] = Integer.toString(number);
        }

        return strings;
    }

    public static String[] longsToStrings(final long... numbers) {
        if (numbers == null) {
            return null;
        }

        final String[] strings = new String[numbers.length];

        int index = 0;

        for (final long number : numbers) {
            strings[index++] = Long.toString(number);
        }

        return strings;
    }

    public static Iterable<String> numbersToStrings(final Iterable<? extends Number> numbers) {
        if(numbers == null) {
            return null;
        }

        final Collection<String> strings = new ArrayList<String>();

        for(final Number number : numbers) {
            strings.add(numberToString(number));
        }
        return strings;
    }

    public static String numberToString(final Number number) {
        if(number == null) {
            return null;
        }
        return doubleToString(number.doubleValue());
    }

    public static Double[] wrap(final double[] doubles) {
        if (doubles == null) {
            return null;
        }

        final Double[] doubs = new Double[doubles.length];

        for (int index = 0 ; index < doubles.length ; ++index) {
            doubs[index] = doubles[index];
        }

        return doubs;
    }

    public static Integer[] wrap(final int[] ints) {
        if (ints == null) {
            return null;
        }

        final Integer[] integers = new Integer[ints.length];

        for (int index = 0 ; index < ints.length ; ++index) {
            integers[index] = ints[index];
        }

        return integers;
    }

    public static Long[] wrap(final long[] longs) {
        if (longs == null) {
            return null;
        }

        final Long[] lngs = new Long[longs.length];

        for (int index = 0 ; index < longs.length ; ++index) {
            lngs[index] = longs[index];
        }

        return lngs;
    }

    public static double[] stringsToDoubles(final Collection<? extends String> strings) {
        final double[] values = new double[strings.size()];

        int index = 0;

        for (final String value : strings) {
            values[index++] = Double.parseDouble(value);
        }

        return values;
    }

    public static <V> List<V> toList(final V value, final V... values) {
        // This is not necessarily fast but it will do for now
        final List<V> list = new ArrayList<V>();
        list.add(value);
        list.addAll(Arrays.asList(values));

        return list;
    }
}
