/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/fieldtext/BIAS.java#3 $
 *
 * Copyright (c) 2008-2010, Autonomy Systems Ltd.
 *
 * Last modified by $Author: darrelln $ on $Date: 2010/06/07 $
 */

package com.autonomy.aci.content.fieldtext;

import com.autonomy.aci.content.internal.InternalUtils;

import java.util.Arrays;

/**
 *
 */
public class BIAS extends Specifier {

    public BIAS(final String field, final Number optimum, final Number range, final Number bias) {
        this(Arrays.asList(field), optimum, range, bias);
    }

    public BIAS(final String[] fields, final Number optimum, final Number range, final Number bias) {
        this(Arrays.asList(fields), optimum, range, bias);
    }

    public BIAS(final Iterable<String> fields, final Number optimum, final Number range, final Number bias) {
        super("BIAS", fields, InternalUtils.numberToString(optimum), InternalUtils.numberToString(range), InternalUtils.numberToString(bias));

        if(range.doubleValue() < 0) {
            throw new IllegalArgumentException("Range must not be negative");
        }
    }
    
    public final double getOptimum() {
        return Double.parseDouble(getValues().get(0));
    }
    
    public final double getRange() {
        return Double.parseDouble(getValues().get(1));
    }
    
    public final double getBias() {
        return Double.parseDouble(getValues().get(2));
    }
    
    public static BIAS BIAS(final String field, final Number optimum, final Number range, final Number bias) {
        return new BIAS(field, optimum, range, bias);
    }

    public static BIAS BIAS(final String[] fields, final Number optimum, final Number range, final Number bias) {
        return new BIAS(fields, optimum, range, bias);
    }

    public static BIAS BIAS(final Iterable<String> fields, final Number optimum, final Number range, final Number bias) {
        return new BIAS(fields, optimum, range, bias);
    }
}
