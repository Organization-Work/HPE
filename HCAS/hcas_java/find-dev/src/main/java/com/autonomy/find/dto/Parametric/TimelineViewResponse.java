package com.autonomy.find.dto.Parametric;

import com.autonomy.aci.actions.idol.tag.FieldValue;
import com.autonomy.find.dto.FieldPair;
import com.autonomy.find.dto.TimelineData;

import lombok.Data;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import java.util.List;

/*
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/dto/Parametric/TableViewResponse.java#2 $
 *
 * Copyright (c) 2013, Autonomy Systems Ltd.
 *
 * Last modified by $Author: tungj $ on $Date: 2013/11/13 $ 
 */
@Data
public class TimelineViewResponse {
    private TimelineData tdata;

	public void setTimeline(TimelineData tdata) {
		this.tdata=tdata;
	}
}


