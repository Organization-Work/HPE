package com.autonomy.find.fields;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum FilterOperator {
    GT(">", FilterType.NUMERIC),
    GE(">=", FilterType.NUMERIC),
    LT("<", FilterType.NUMERIC),
    LE("<=", FilterType.NUMERIC),
    EQ("=", FilterType.NUMERIC),
    NE("!=", FilterType.NUMERIC),
    RANGE("between", FilterType.NUMERIC),
    BEFORE("before", FilterType.DATE),
    AFTER("after", FilterType.DATE),
    BETWEEN("between", FilterType.DATE),
    PERIOD("in", FilterType.DATE),
    IS("is", FilterType.TEXT, FilterType.MATCH, FilterType.DOCUMENTFOLDER, FilterType.HASFILTER),
    IS_NOT("is not", FilterType.TEXT, FilterType.MATCH, FilterType.DOCUMENTFOLDER),
    IS_RANGE("is", FilterType.PARARANGES),
    IS_NOT_RANGE("is not", FilterType.PARARANGES),
    CONTAINS("contains", FilterType.TEXT),
    NOT_CONTAINS("not contains", FilterType.TEXT),
	AS_OF_DATE("in", FilterType.ASOFDATE),
	MONTH_PART("in", FilterType.MONTHPART);
	
    FilterOperator(final String displayName, final FilterType... filterTypes) {
        this.displayName = displayName;
        this.filterTypes = filterTypes;
    }

    private final String displayName;
    private final FilterType[] filterTypes;

    public String getDisplayName() {
        return this.displayName;
    }

    public FilterType[] getFilterTypes() {
        return this.filterTypes;
    }

    public static Map<FilterType, List<FilterOperator>> getFilterTypeOperatorsMap() {
        final Map<FilterType, List<FilterOperator>> filterTypeOperatorsMap = new HashMap<FilterType, List<FilterOperator>>();
        for(final FilterOperator filterOp : FilterOperator.values()) {
            for(final FilterType filterType : filterOp.getFilterTypes()) {
                List<FilterOperator> typeOperatorList = filterTypeOperatorsMap.get(filterType);
                if (typeOperatorList == null) {
                    typeOperatorList = new ArrayList<FilterOperator>();
                    typeOperatorList.add(filterOp);
                    filterTypeOperatorsMap.put(filterType, typeOperatorList);
                } else if (!typeOperatorList.contains(filterOp)) {
                    typeOperatorList.add(filterOp);
                }
            }
        }



        return filterTypeOperatorsMap;

    }

}
