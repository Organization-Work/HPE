package com.autonomy.find.dto.taxonomy;

import com.autonomy.find.util.MTree;
import lombok.Data;

import java.util.Map;

@Data
public class CategoryData {
    private final MTree<String> hier;
    private final Map<String, String> names;
    private final Map<String, Integer> docCounts;

    public CategoryData(
            final MTree<String> hier,
            final Map<String, String> names,
            final Map<String, Integer> docCounts
    ) {
        this.hier = hier;
        this.names = names;
        this.docCounts = docCounts;
    }
}