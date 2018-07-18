package com.autonomy.find.api.idol;

import com.autonomy.aci.content.fieldtext.Specifier;


public class RANGE extends Specifier {
    public RANGE(final String field, final String startDate, final String endDate) {
        super("RANGE", field, startDate, endDate);
    }
}
