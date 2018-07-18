package com.autonomy.vertica.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

public class SelectBuilder {

	  	private List<String> columns = new ArrayList<String>();

	    private List<String> tables = new ArrayList<String>();

	    private List<String> joins = new ArrayList<String>();
	    
	    private List<String> innerJoins = new ArrayList<String>();
	    
	    private List<String> leftOuterJoins = new ArrayList<String>();	    

	    private List<String> leftJoins = new ArrayList<String>();

	    private List<String> wheres = new ArrayList<String>();

	    private List<String> orderBys = new ArrayList<String>();

	    private List<String> groupBys = new ArrayList<String>();

	    private List<String> havings = new ArrayList<String>();
	    
	    private List<String> offset = new ArrayList<String>();
	    
	    private List<String> limit = new ArrayList<String>();
	    
	    private List<String> union = new ArrayList<String>();
	    
	    private String schema;
	    
	    private boolean useSchemaQualifier = false;
	    
	    private HashMap<String,Boolean> defaultFilterAdded = new HashMap<String, Boolean>();
	    
	    private Set<String> relationKeys = new HashSet<String>();
	    
	    private Map<String, List<String>> relationKeyJoinMap = new HashMap<String, List<String>>();
	    
	    private int numberOfTabspace;

	    public SelectBuilder() {

	    }

	    public SelectBuilder(String table) {
	        tables.add(table);
	    }
	    
	    public SelectBuilder(String table, String schema) {	    	
	    	this.schema = schema;
	    	if(this.schema != null && !this.schema.isEmpty()) {
	    		useSchemaQualifier = true;
	    		table = schema + "." + table;
	    	}	    	
	        tables.add(table);
	    }
	    
	    public SelectBuilder(String table, String schema, int numberOfTabspace) {	    	
	    	this.schema = schema;
	    	if(this.schema != null && !this.schema.isEmpty()) {
	    		useSchemaQualifier = true;
	    		table = schema + "." + table;
	    	}	    	
	        tables.add(table);
	        this.numberOfTabspace = numberOfTabspace;
	    }

	    private void appendList(StringBuilder sql, List<String> list, String init, String sep, boolean newLine, String tabSpace) {
	        boolean first = true;
	        if(list.size() > 0) {
	        	if(newLine) {
	        		sql.append("\n");
	        		if(tabSpace != null && !tabSpace.isEmpty()) {
		        		sql.append(tabSpace);
		        	}
	        	}	
	        	
	        }
	        for (String s : list) {
	        	if (first) {
	                sql.append(init);
	            } else {
	                sql.append(sep);
	            }
	            sql.append(s);
	            first = false;
	            
	        }
	    }
	    
	    public SelectBuilder column(String name, boolean distinct) {
	    	
	    	if(distinct) {
	    		name = " distinct " + name;
	    	}
	    	if(name != null && !name.isEmpty()) {
	    		columns.add(name);
	    	}
	        return this;
	    }
	    
	    
	    public SelectBuilder column(String name, String table,  boolean distinct, String alias) {
	    	if(table != null && !table.isEmpty()) {
	    		name = table + "." + name;
	    	}
	    	if(alias != null){
	    		name = name + " " + alias;
	    	}	    	
	    	if(distinct) {
	    		name = " distinct " + name;
	    	}
	        columns.add(name);
	        return this;
	    }

	    public SelectBuilder column(String name, String table,  boolean distinct) {
	    	if(name == null || name.isEmpty()) {
	    		return this;
	    	}
	    	if(table != null && !table.isEmpty()) {
	    		name = table + "." + name;
	    	}
	    	if(distinct) {
	    		name = " distinct " + name;
	    	}
	        columns.add(name);
	        return this;
	    }
	    
	    /**
	     * 
	     * @param name
	     * @param table
	     * @param aggregate
	     * @param distinct
	     * @return
	     */
	    public SelectBuilder column(String name, String table, String aggregate, boolean distinct, String alias) {
	    	if(table != null && !table.isEmpty()) {
	    		name = table + "." + name;
	    	}
	    	if(distinct) {
	    		name = " distinct " + name;
	    	}
	    	if(aggregate != null && !aggregate.isEmpty()) {
	    		StringBuffer buffer = new StringBuffer(aggregate);	    		
	    		buffer.append("(").append(name).append(")");
	    		name = buffer.toString();	    		
	    	}
	    	name = name + " " + alias;
	        columns.add(name);
	        return this;
	    }   

	    public SelectBuilder column(String name, String table, boolean distinct, boolean groupBy) {
	    	if(table != null && !table.isEmpty()) {
	    		name = table + "." + name;
	    	}
	    	if(distinct) {
	    		name = " distinct " +  name;
	    	}
	        columns.add(name);
	        if (groupBy) {
	            groupBys.add(name);
	        }
	        return this;
	    }
	    
	    public SelectBuilder column(String name, String table, String aggregate, boolean distinct, boolean groupBy, String alias) {
	    	String columnName = name;
	    	if(table != null && !table.isEmpty()) {
	    		columnName = table + "." + name;
	    	}
	    	if (groupBy) {
		       groupBys.add(columnName);
		    }
	    	if(distinct) {
	    		columnName = " distinct " + columnName;
	    	}	    	
	    	if(aggregate != null && !aggregate.isEmpty()) {
	    		StringBuffer buffer = new StringBuffer(aggregate);
	    		buffer.append("(").append(columnName).append(")");
	    		columnName = buffer.toString();	    		
	    	}
	    	String columnNameWithAlias = columnName + " " + alias;
	        columns.add(columnNameWithAlias);
	       
	        return this;
	    }

	    public SelectBuilder from(String table) {
	        tables.add(table);
	        return this;
	    }

	    public SelectBuilder groupBy(String expr) {
	        groupBys.add(expr);
	        return this;
	    }

	    public SelectBuilder having(String expr) {
	        havings.add(expr);
	        return this;
	    }
	    
	    public SelectBuilder join(JoinType joinType, String fromTableName, String toTableName, String fromColumnName, String ToColumnName, String relationKey, String toTableSchema) {
	    	
	    	
	    	StringBuffer joinString = new StringBuffer();
	    	joinString.append(joinType.getJoinTypeString().toLowerCase());
	    	joinString.append(" ");
	    	
	    	joinString.append(StringUtils.isNotBlank(toTableSchema) ? toTableSchema : schema).append(".").append(toTableName);
	    	joinString.append(" ").append(" ON ").append(" ");
	    	joinString.append(fromTableName).append(".").append(fromColumnName); // left column
	    	joinString.append(" ").append(" = ").append(" ");
	    	joinString.append(toTableName).append(".").append(ToColumnName); // right column
	    	
	    	String join = joinString.toString();
	    	
	    	
	    	if(relationKey != null) {
	    		List<String> joinList = relationKeyJoinMap.get(relationKey);
	    		if((StringUtils.containsIgnoreCase(join, "join")) && joinList == null) {
	    			joinList = new ArrayList<String>();   			
	    		}
	    		boolean addJoinToQuery = true;
	    		if(relationKeyJoinMap.containsKey(relationKey)) { // remove join clauses for same keys, they just need to be added as joins	    			
	    			for(String joinExp : relationKeyJoinMap.get(relationKey)) {
	    				if(joinExp.contains(join)) {
	    					addJoinToQuery = false;
	    				}
	    			}
	    			if(addJoinToQuery) {
		    			join = join.replaceAll("(?i)inner join", " ");
		    			join = join.replaceAll("(?i)left join", " ");
		    			join = join.replaceAll("(?i)left outer join", " ");
	    			}
	    		}
	    		if(joinList != null) {
	    			if(addJoinToQuery) {
	    				joinList.add(join);
	    			}
		    		relationKeyJoinMap.put(relationKey, joinList);
		    	}
	    	} else {
	    		joins.add(join);
	    	}     
	        return this;
	    }   
	    
	    public SelectBuilder join(JoinType joinType, String query, String fromTableName, String toTableName, String fromColumnName, String ToColumnName, boolean edit) {
	    	StringBuffer joinString = new StringBuffer();
	    	joinString.append("(").append(query).append(")").append(" ").append(toTableName);
	    	joinString.append(" ").append(" ON ").append(" ");
	    	joinString.append(fromTableName).append(".").append(fromColumnName); // left column
	    	joinString.append(" ").append(" = ").append(" ");
	    	joinString.append(toTableName).append(".").append(ToColumnName); // right column
	    	
	    	if (joinType == JoinType.InnerJoin) {
	    		innerJoins.add(joinString.toString());
	    	} else if(joinType == JoinType.LeftOuterJoin) {
	    		leftOuterJoins.add(joinString.toString());
	    	} else if(joinType == JoinType.LeftJoin) {
	    		leftJoins.add(joinString.toString());
	    	}	      
	        return this;
	    }   
	   

	    public SelectBuilder join(String join) {
	        joins.add(join);
	        return this;
	    }
	    
	    public SelectBuilder join(String join, String relationKey) {
	    	if(relationKey != null) {
	    		List<String> joinList = relationKeyJoinMap.get(relationKey);
	    		if((StringUtils.containsIgnoreCase(join, "join")) && joinList == null) {
	    			joinList = new ArrayList<String>();   			
	    		}
	    		boolean addJoinToQuery = true;
	    		if(relationKeyJoinMap.containsKey(relationKey)) { // remove join clauses for same keys, they just need to be added as joins
	    			
	    			for(String joinExp : relationKeyJoinMap.get(relationKey)) {
	    				if(joinExp.contains(join)) {
	    					addJoinToQuery = false;
	    				}
	    			}
	    			if(addJoinToQuery) {
		    			join = join.replaceAll("(?i)inner join", " ");
		    			join = join.replaceAll("(?i)left join", " ");
		    			join = join.replaceAll("(?i)left outer join", " ");
	    			}
	    		}
	    		if(joinList != null) {
	    			if(addJoinToQuery) {
	    				joinList.add(join);
	    			}
		    		relationKeyJoinMap.put(relationKey, joinList);
		    	}
	    	} else {
	    		joins.add(join);
	    	}
	        //joins.add(join);
	        return this;
	    }
	    
	    public SelectBuilder innerJoin(String join) {
	        innerJoins.add(join);
	        return this;
	    }	    

	    public SelectBuilder leftJoin(String join) {
	        leftJoins.add(join);
	        return this;
	    }
	    
	    public SelectBuilder leftOuterJoin(String join) {
	    	leftOuterJoins.add(join);
	    	return this;
	    }

	    public SelectBuilder orderBy(String name) {
	        orderBys.add(name);
	        return this;
	    }
	    
	    public SelectBuilder offset(String value) {
	    	if(value != null && !value.isEmpty()) {
	    		offset.add(value);
	    	}
	        return this;
	    }
	    
	    public SelectBuilder limit(String value) {
	    	if(value != null && !value.isEmpty()) {
	    		limit.add(value);
	    	}
	        return this;
	    }
	    
	    private String getTabspaces(int numberOfTabspaces) {
	    	StringBuffer buff = new StringBuffer();
	    	for (int i = 0; i < numberOfTabspaces; i++) {
	    		buff.append("\t");
	    	}
	    	return buff.toString();
	    }

	    @Override
	    public String toString() {
	    	
	    	String tabSpace = getTabspaces(this.numberOfTabspace);

	        StringBuilder sql = new StringBuilder("\n" + tabSpace + "select ");

	        if (columns.size() == 0) {
	            sql.append("*");
	        } else {
	            appendList(sql, columns, "", ", ", false, tabSpace);
	        }

	        appendList(sql, tables, " from ", ", ", true, tabSpace);
	        appendList(sql, joins, "  ", "  ", true, tabSpace);
	        if(!relationKeyJoinMap.isEmpty()) {
	        	for(Map.Entry<String, List<String>> entry : relationKeyJoinMap.entrySet()) {
	        		 appendList(sql, entry.getValue(), "  ", " and ", true, tabSpace);
	        	}
	        }
	        appendList(sql, leftJoins, " left join ", " left join ", true, tabSpace);
	        appendList(sql, leftOuterJoins, " left outer join ", " left outer join ", true, tabSpace);
	        appendList(sql, innerJoins, " inner join ", " inner join ", true, tabSpace);
	        appendList(sql, wheres, " where ", " and ", true, tabSpace);
	        appendList(sql, groupBys, " group by ", ", ", true, tabSpace);
	        appendList(sql, havings, " having ", " and ", true, tabSpace);
	        appendList(sql, orderBys, " order by ", ", ", true, tabSpace);
	        appendList(sql, offset, " offset ", ", ", true, tabSpace);
	        appendList(sql, limit, " limit ", ", ", true, tabSpace);
	        appendList(sql, union, " union ", " union ", true, tabSpace);	        
	        
	        return sql.toString();
	    }
	    
	    public SelectBuilder union(String expr) {
	    	union.add(expr);
	        return this;
	    }

	    public SelectBuilder where(String expr) {
	        wheres.add(expr);
	        return this;
	    }
	    
	    public SelectBuilder where(String columnName, String tableName, String opType, String value) {
	    	StringBuffer expr = new StringBuffer(tableName);
	    	expr.append(".").append(columnName);
	    	expr.append(" ").append(opType).append(" ").append(value);
	    	
	    	//if(tables.contains(o))
	    	
	        wheres.add(expr.toString());
	        return this;
	    }

		public String getSchema() {
			return schema;
		}

		public void setSchema(String schema) {
			this.schema = schema;
		}

		public boolean isUseSchemaQualifier() {
			return useSchemaQualifier;
		}

		public boolean isDefaultFilterAdded(String table) {
			return defaultFilterAdded.containsKey(table);
		}

		public void setDefaultFilterAdded(String table, boolean defaultFilterAdded) {
			this.defaultFilterAdded.put(table, defaultFilterAdded);
		}

		public boolean isRelationAdded(String relationKey) {
			return relationKeys.contains(relationKey);
		}

		public void addRelationMapping(String relationKey) {
			if(relationKey != null) {
				this.relationKeys.add(relationKey);
			}
		}

		
	
}
