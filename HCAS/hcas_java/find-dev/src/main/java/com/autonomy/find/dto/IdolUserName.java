package com.autonomy.find.dto;

import com.autonomy.aci.client.annotations.IdolField;

public class IdolUserName {
	private String value = "";

	@IdolField("autn:name")
	public IdolUserName setValue(final String value) {
		this.value = value;
		return this;
	}

	public String getValue() {
		return this.value;
	}
}
