/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/fieldtext/MATCHRECURSE.java#3 $
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
public class MATCHRECURSE extends Specifier {

    public MATCHRECURSE(final String field, final String value, final Number depth) {
        this(Arrays.asList(field), value, depth);
    }

    public MATCHRECURSE(final String[] fields, final String value, final Number depth) {
        this(Arrays.asList(fields), value, depth);
    }

    public MATCHRECURSE(final Iterable<String> fields, final String value, final Number depth) {
        super("MATCHRECURSE", fields, value, InternalUtils.numberToString(depth.intValue()));

        if(depth.intValue() < 0) {
            throw new IllegalArgumentException("Depth must not be negative");
        }
    }
    
    public final String getMatchValue() {
        return getValues().get(0);
    }
    
    public final int getDepth() {
        return Integer.parseInt(getValues().get(1));
    }

    public static MATCHRECURSE MATCHRECURSE(final String field, final String value, final Number depth) {
        return new MATCHRECURSE(field, value, depth);
    }

    public static MATCHRECURSE MATCHRECURSE(final String[] fields, final String value, final Number depth) {
        return new MATCHRECURSE(fields, value, depth);
    }

    public static MATCHRECURSE MATCHRECURSE(final Iterable<String> fields, final String value, final Number depth) {
        return new MATCHRECURSE(fields, value, depth);
    }
}