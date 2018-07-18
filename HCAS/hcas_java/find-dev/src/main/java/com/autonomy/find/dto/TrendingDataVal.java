package com.autonomy.find.dto;

import java.util.Date;

import lombok.Data;

/*
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/dto/FieldPair.java#2 $
 *
 * Copyright (c) 2013, Autonomy Systems Ltd.
 *
 * Last modified by $Author: tungj $ on $Date: 2013/11/12 $ 
 */
@Data
public class TrendingDataVal {
    private String date;
    private double add_count=0;
    private double remove_count=0;
    private double total_count=0;

    public TrendingDataVal(final double total_count, final double add_count, double remove_count, final String date) {
        this.total_count = total_count;
        this.add_count = add_count;
        this.remove_count = remove_count;
        this.date = date;
    }

	public TrendingDataVal() {}
}
