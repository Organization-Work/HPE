package com.autonomy.find.api.response;

import lombok.Data;

@Data
public class ResponseStatus {
	
	final boolean status;
	
	public ResponseStatus(final boolean _status) {
		this.status = _status;
	}
	
}
