package com.autonomy.find.dto;

import com.autonomy.aci.client.annotations.IdolDocument;
import com.autonomy.aci.client.annotations.IdolField;

/**
 * Models the relevant parts of an Idol
 * response for checking if a user exists.
 * 
 * @author liam.goodacre
 */
@IdolDocument("responsedata")
public class UserExists {
	private boolean status;

	@IdolField("autn:userexists")
	public void setStatus(final boolean value) {
		this.status = value;
	}

	public boolean getStatus() {
		return this.status;
	}
	
	public UserExists() {}
}