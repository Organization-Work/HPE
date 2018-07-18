package com.autonomy.find.dto;

import com.autonomy.aci.client.annotations.IdolDocument;
import com.autonomy.aci.client.annotations.IdolField;

@IdolDocument("autn:snapshot")
public class ClusterName {
	private String name;
	private String displayName;

	@IdolField("autn:name")
	public void setStatus(final String value) {
		this.name = value;
	}

	public String getName() {
		return this.name;
	}
	
	public void setDisplayName(final String value) {
		this.displayName = value;
	}
	
	public String getDisplayName() {
		return this.displayName;
	}

	public ClusterName() {}
}