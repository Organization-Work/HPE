package com.autonomy.find.dto;

import com.autonomy.aci.client.annotations.IdolDocument;
import com.autonomy.aci.client.annotations.IdolField;
import lombok.Data;

@Data
@IdolDocument("autn:term")
public class ProfileTerm {
	public String name;
	public long weight;

    public ProfileTerm() {}

	public ProfileTerm(final String name, final long weight) {
		this.name = name;
		this.weight = weight;
	}

    @IdolField("autn:term")
    public void setName(final String value) {
        this.name = value;
    }

    @IdolField(value = "autn:term", attributeName = "weight")
    public void setWeight(final long value) {
        this.weight = value;
    }

    public String toString() {
        return String.format("%s~[%d]", this.name, this.weight);
    }
}