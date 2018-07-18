package com.autonomy.idolview;

import com.autonomy.aci.content.fieldtext.EQUAL;
import com.autonomy.aci.content.fieldtext.FieldText;
import com.autonomy.aci.content.fieldtext.GREATER;
import com.autonomy.aci.content.fieldtext.LESS;
import com.autonomy.aci.content.fieldtext.MATCH;
import org.apache.commons.lang.Validate;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/idolview/Filter.java#1 $
 * <p/>
 * Copyright (c) 2012, Autonomy Systems Ltd.
 * <p/>
 * Last modified by $Author: tungj $ on $Date: 2013/05/21 $
 */

public abstract class Filter {
    protected final String key, idolField;

    protected Filter(final String key, final String idolField) {
        Validate.notEmpty(key);
        this.key = key;
        this.idolField = idolField;
    }

    public abstract FieldText getFieldText(final String value);

    public String getKey() {
        return key;
    }

    public String getIdolField() {
        return idolField;
    }

    public String processFieldValue(final String value) {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Filter)) return false;
        return key.equals(((Filter) o).key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    public static class StringFilter extends Filter {
        public StringFilter(final String key, final String idolField) {
            super(key, idolField);
        }

        public StringFilter(final String idolField) {
            this(idolField, idolField);
        }

        @Override
        public FieldText getFieldText(final String value) {
            return new MATCH(idolField, value);
        }
    }

    public static class NumericFilter extends Filter {
        public NumericFilter(final String key, final String idolField) {
            super(key, idolField);
        }

        public NumericFilter(final String idolField) {
            this(idolField, idolField);
        }

        // Regex to match a single number, with test patterns:
        //  ['', '0.2.2', '.2', '0.2', '2', '2.', '.24', '2.2', '-4', '-24']
        //   .map(function(a){ return /^-?(?:\.\d+|\d+(?:\.\d*)?)$/.test(a); })
        // This is two numbers with grouping patterns around them, one of ':,>-→' in between (the unicode arrow is \u2192),
        // optionally with whitespace and sandwiched between '^' and '$'
        private final Pattern pairPattern = Pattern.compile("^(-?(?:\\.\\d+|\\d+(?:\\.\\d*)?))\\s*(?:[:,>-]|\u2192)\\s*(-?(?:\\.\\d+|\\d+(?:\\.\\d*)?))$");

        @Override
        public FieldText getFieldText(final String value) {
            // we want to accept plain numbers, like 2.0, or -1.5
            // we also want to accept ranges like "1:2", "-9,-4", "-9>-4",or half-open ranges like "1:" or ":2"
            // and ranges "> 2.0" or "< 2.5"
            final String trimmedValue = value.trim();

            if (trimmedValue.indexOf('<') == 0) {
                return new LESS(idolField, new BigDecimal(trimmedValue.substring(1).trim()));
            }

            if (trimmedValue.indexOf('>') == 0) {
                return new GREATER(idolField, new BigDecimal(trimmedValue.substring(1).trim()));
            }

            final Matcher matcher = pairPattern.matcher(value);
            if (matcher.find()) {
                final String min = matcher.group(1).trim();
                final String max = matcher.group(2).trim();
                return new NRANGE(idolField, new BigDecimal(min), new BigDecimal(max));
            }

            return new EQUAL(idolField, new BigDecimal(trimmedValue));
        }
    }

    public static class ParametricRangeFilter extends NumericFilter {
        public ParametricRangeFilter(final String key, final String idolField) {
            super(key, idolField);
        }

        public ParametricRangeFilter(final String idolField) {
            super(idolField);
        }

        // flag set to be read client-side
        public boolean isParametricRange() {
            return true;
        }

        @Override
        public String processFieldValue(final String value) {
            // the server gives us ranges like "0,10" or half-open ranges like "10,", we pretty-print them
            final String[] split = value.split(",");
            if (split.length == 2) {
                final String maxStr = new BigDecimal(split[1]).toPlainString();
                if ("-Inf".equalsIgnoreCase(split[0])) {
                    return '<' + maxStr;
                }
                return new BigDecimal(split[0]).toPlainString() + "\u2192" + maxStr;
            }

            return '>' + new BigDecimal(split[0]).toPlainString();
        }
    }

    public static class NRANGE extends com.autonomy.aci.content.fieldtext.Specifier {
        public NRANGE(final String field, final Number lowerBound, final Number upperBound) {
            super("NRANGE", field, lowerBound.toString(), upperBound.toString());
        }
    }
}
