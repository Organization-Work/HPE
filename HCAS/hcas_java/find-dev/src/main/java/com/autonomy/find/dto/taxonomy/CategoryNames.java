package com.autonomy.find.dto.taxonomy;

/*
 * $Id: //depot/scratch/frontend/find-healthcare/src/main/java/com/autonomy/find/dto/taxonomy/CategoryNames.java#2 $
 *
 * Copyright (c) 2013, Autonomy Systems Ltd.
 *
 * Last modified by $Author: tungj $ on $Date: 2013/11/18 $ 
 */

import com.autonomy.aci.client.annotations.IdolField;

import java.util.LinkedHashSet;
import java.util.Locale;

public class CategoryNames extends LinkedHashSet<String> {

    public CategoryNames() {}

    @IdolField("CHILDNAME")
    public void addCategory(final String categoryName) {
        this.add(categoryName.toUpperCase(Locale.US));
    }
}
