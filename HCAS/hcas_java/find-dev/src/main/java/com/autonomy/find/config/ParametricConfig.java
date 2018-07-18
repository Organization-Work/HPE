package com.autonomy.find.config;

import lombok.Data;

import java.util.Map;

@Data
public class ParametricConfig {
    public void setActive(boolean active) {
		this.active = active;
	}

	private boolean active;

	public boolean isActive() {
		return active;
	}
}
