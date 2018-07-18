package com.autonomy.find.dto;

import lombok.Getter;

import com.autonomy.aci.client.annotations.IdolField;
import com.autonomy.find.idx.Idx;

@Getter
public class SearchHistoryDetails {

	public static final String	IDX_SEARCH		= "SEARCHTEXT";
	public static final String	IDX_USERNAME	= "USERNAME";

	private String				value			= "";
	private String				username		= "";

	public SearchHistoryDetails() {
	}

	public SearchHistoryDetails(final String value, final String username) {
		setValue(value);
		setUsername(username);
	}

	@IdolField(IDX_SEARCH)
	public void setValue(final String value) {
		this.value = (value == null ? "" : value);
	}

	@IdolField(IDX_USERNAME)
	public void setUsername(final String username) {
		this.username = (username == null ? "" : username);
	}

	public Idx asIdx() {
		return new Idx(getValue(), Idx.Field(IDX_SEARCH, getValue()), Idx.Field(IDX_USERNAME, getUsername()));
	}

	public String getUsername() {
		return username;
	}

	public String getValue() {
		return value;
	}
}
