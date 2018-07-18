/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/fieldtext/BIASVAL.java#3 $
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
public class BIASVAL extends Specifier {

    public BIASVAL(final String field, final String value, final Number bias) {
        this(Arrays.asList(field), value, bias);
    }

    public BIASVAL(final String[] fields, final String value, final Number bias) {
        this(Arrays.asList(fields), value, bias);
    }

    public BIASVAL(final Iterable<String> fields, final String value, final Number bias) {
        super("BIASVAL", fields, value, InternalUtils.numberToString(bias));
    }
    
    public final String getMatchValue() {
        return getValues().get(0);
    }
    
    public final double getBias() {
        return Double.parseDouble(getValues().get(1));
    }

    public static BIASVAL BIASVAL(final String field, final String value, final Number bias) {
        return new BIASVAL(field, value, bias);
    }

    public static BIASVAL BIASVAL(final String[] fields, final String value, final Number bias) {
        return new BIASVAL(fields, value, bias);
    }

    public static BIASVAL BIASVAL(final Iterable<String> fields, final String value, final Number bias) {
        return new BIASVAL(fields, value, bias);
    }
}
