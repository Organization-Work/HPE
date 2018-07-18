package com.autonomy.vertica.common;

public enum JoinType {
	
	InnerJoin("INNER JOIN"),
	LeftJoin("LEFT JOIN"),
	LeftOuterJoin("LEFT OUTER JOIN");
	
	private final String joinTypeString;
	JoinType(String joinTypeString) {
		this.joinTypeString = joinTypeString;
	}
	public String getJoinTypeString() {
		return joinTypeString;
	}
	
	public static JoinType getByName(String name){
	    for(JoinType prop : values()){
	      if(prop.getJoinTypeString().equals(name)){
	        return prop;
	      }
	    }

	    throw new IllegalArgumentException(name + " is not a valid PropName");
	  }
}
