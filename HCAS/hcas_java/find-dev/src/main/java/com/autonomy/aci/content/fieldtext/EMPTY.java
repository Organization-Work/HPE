/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/fieldtext/EMPTY.java#3 $
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
public class EMPTY extends Specifier {

    public EMPTY(final String field, final String... fields) {
        this(InternalUtils.toList(field, fields));
    }

    public EMPTY(final String[] fields) {
        this(Arrays.asList(fields));
    }

    public EMPTY(final Iterable<String> fields) {
        super("EMPTY", fields);
    }

    public static EMPTY EMPTY(final String field, final String... fields) {
        return new EMPTY(field, fields);
    }

    public static EMPTY EMPTY(final String[] fields) {
        return new EMPTY(fields);
    }

    public static EMPTY EMPTY(final Iterable<String> fields) {
        return new EMPTY(fields);
    }
}
