package com.autonomy.find.dto.Parametric;

import com.autonomy.aci.client.annotations.IdolDocument;
import com.autonomy.aci.client.annotations.IdolField;

import lombok.Data;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
@IdolDocument("autn:field")
public class FieldValueData {

    private String name;
    private String displayName;
    private List<String> values = new LinkedList<String>();

    @IdolField("autn:name")
    public void setName(final String name) {
        this.name = name;
    }

    @IdolField("autn:value")
    public void addValue(final String value) {
        this.values.add(value);
    }

    public static Map<String, FieldValueData> collToMap(final List<FieldValueData> values) {
        final Map<String, FieldValueData> result = new HashMap<>();
        for (final FieldValueData value : values) {
            result.put(value.getName(), value);
        }
        return result;
    }

	public String getName() {
		return name;
	}

	public void setDisplayName(String displayName2) {
		this.displayName=displayName2;
	}
	
}
