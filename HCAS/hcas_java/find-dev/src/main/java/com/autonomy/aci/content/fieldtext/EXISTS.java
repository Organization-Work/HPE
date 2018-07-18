/*
 * $Id: //depot/products/aci/api/public/java/ContentParameter/src/main/java/com/autonomy/aci/content/fieldtext/EXISTS.java#3 $
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
public class EXISTS extends Specifier {

    public EXISTS(final String[] fields) {
        this(Arrays.asList(fields));
    }

    public EXISTS(final String field, final String... fields) {
        this(InternalUtils.toList(field, fields));
    }

    public EXISTS(final Iterable<String> fields) {
        super("EXISTS", fields);
    }

    public static EXISTS EXISTS(final String[] fields) {
        return new EXISTS(fields);
    }

    public static EXISTS EXISTS(final String field, final String... fields) {
        return new EXISTS(field, fields);
    }

    public static EXISTS EXISTS(final Iterable<String> fields) {
        return new EXISTS(fields);
    }
}
