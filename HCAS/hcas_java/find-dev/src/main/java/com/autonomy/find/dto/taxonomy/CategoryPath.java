package com.autonomy.find.dto.taxonomy;

import com.autonomy.aci.client.annotations.IdolDocument;

import java.util.LinkedList;
import java.util.List;


@IdolDocument("autn:category")
public class CategoryPath extends CategoryName {

    public List<CategoryName> getActualPath() {
        final List<CategoryName> result = new LinkedList<>();

        CategoryName temp = getChild();
        while (temp != null) {
            result.add(temp);
            temp = temp.getChild();
        }
        result.add(this);
        return result;
    }
}