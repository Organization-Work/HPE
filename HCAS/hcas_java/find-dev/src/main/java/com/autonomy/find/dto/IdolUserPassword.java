package com.autonomy.find.dto;

import com.autonomy.aci.client.annotations.IdolField;

public class IdolUserPassword {
	private String value = "";

	@IdolField("autn:password")
	public IdolUserPassword setValue(final String value) {
		this.value = value;
		return this;
	}

	public String getValue() {
		return this.value;
	}
}
