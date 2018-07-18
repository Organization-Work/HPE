package com.autonomy.find.util;

import lombok.Data;

@Data
public class FieldTextDetail {
    private String displayName;
    private String type;
    private String format;
    private String header;
    private boolean parametric = true;
    private String mappedField;
    private boolean restricted = false;
    private boolean printable = true;
    private String specifier;
    private int weight = 0;
    private boolean aggregate = false;
	public String getType() {
		return type;
	}
}
