package com.autonomy.find.dto.taxonomy;

import com.autonomy.aci.client.annotations.IdolDocument;
import com.autonomy.aci.client.annotations.IdolField;
import com.autonomy.aci.client.annotations.IdolProcessorField;


@IdolDocument("autn:node")
public class CategoryName {
    private String id = null;
    private String name = null;
    private CategoryName child;

    public CategoryName() {
    }

    public CategoryName(final String id, final String name) {
        this.id = id;
        this.name = name;
    }

    @IdolField("autn:id")
    public void setId(final String value) {
        if (this.id == null) {
            this.id = value;
        }
    }

    @IdolField("autn:name")
    public void setName(final String value) {
        if (this.name == null) {
            this.name = value;
        }
    }

    @IdolProcessorField("autn:node")
    public void addParent(final CategoryName category) {
        child = category;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public CategoryName getChild() {
        return child;
    }
}
