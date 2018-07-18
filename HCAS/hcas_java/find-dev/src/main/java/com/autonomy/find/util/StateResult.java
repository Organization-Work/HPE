package com.autonomy.find.util;

import lombok.Data;

@Data
public class StateResult {
    private String stateId;
    private Integer numhits;

    public StateResult(final String stateId, final int numhits) {
        if (stateId == null || numhits < 0) {
            throw new IllegalArgumentException("Null stateId or numhits less than 0");
        }

        this.stateId = stateId;
        this.numhits = numhits;
    }

	public int getNumhits() {
		return numhits;
	}

	public String getStateId() {
		return stateId;
	}

}
