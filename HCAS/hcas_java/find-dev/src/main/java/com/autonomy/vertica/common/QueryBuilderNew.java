package com.autonomy.vertica.common;

// import java.sql.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.autonomy.find.config.SearchConfig;
import com.autonomy.find.dto.Parametric.BoolOp;
import com.autonomy.find.dto.Parametric.DatePeriod;
import com.autonomy.find.dto.Parametric.FieldParams;
import com.autonomy.find.dto.Parametric.FilterGroup;
import com.autonomy.find.fields.CustomFilterObject;
import com.autonomy.find.fields.FilterField;
import com.autonomy.find.fields.FilterOperator;
import com.autonomy.find.fields.FilterType;
import com.autonomy.find.services.DocumentFolderService;
import com.autonomy.find.services.DocumentVerticaFolderService;
import com.autonomy.find.util.CollUtils;
import com.autonomy.find.util.FieldTextUtil.FieldTypeNamePair;
import com.autonomy.vertica.table.mapping.Relation;
import com.autonomy.vertica.table.mapping.Table;
import com.autonomy.vertica.table.mapping.TableRelationObject;

public class QueryBuilderNew {
	
	private static String tableRelationExpression = "To";
	
	private static final Logger LOGGER = LoggerFactory.getLogger(QueryBuilderNew.class);
	
	public static SubExpression buildFilterExpression(
	            final DocumentFolderService documentFolderService,
	            final DocumentVerticaFolderService documentVerticaFolderService,
	            final FilterGroup filterGroup,
	            final Map<String, FilterField> filterFieldNames,
	            final boolean filterCohort,
	            Map<String, Object> paramMap,
	            Set<String> listTables,
	            String refTable,
	            Map<String, Table> tablesMap,
	            SelectBuilder query,
	            SearchConfig config,
	            String fromTable,
	            String searchView,
	            String destDateFormat,
	            FilterField measurementField,
	            Map<String, List<TableRelationObject>> relationMap,
	            String schema,
	            int parentPromoteLevel,
	            final boolean isDistinct,
	            boolean isParentGrpWhen,
	            String ignoreField,
	            final FilterGroup rootParentGroup
	            ) throws Exception {
		
		 if (filterGroup == null || filterGroup.getBoolOperator() == null || filterGroup.isDeactivated()) {
	           return null;
	     }	 
		 
		 final boolean isAND = BoolOp.AND == filterGroup.getBoolOperator() | BoolOp.NOT_AND == filterGroup.getBoolOperator();
	    // final boolean isNOT = BoolOp.NOT_OR == filterGroup.getBoolOperator() | BoolOp.NOT_AND == filterGroup.getBoolOperator();
	    // final boolean isWHEN = BoolOp.WHEN == filterGroup.getBoolOperator();
	     
	     final boolean grpOpAll = BoolOp.AND == filterGroup.getBoolOperator();
	     final boolean grpOpNotAll = BoolOp.NOT_AND == filterGroup.getBoolOperator();
	     
	     final boolean grpOpNotAny = BoolOp.NOT_OR == filterGroup.getBoolOperator();
	     final boolean grpOpWhen = BoolOp.WHEN == filterGroup.getBoolOperator();
	     final boolean grpOpAny = !grpOpAll && !grpOpNotAll && !grpOpWhen;
	     
	     int index = 0;
	     int viewPromoteLevel = 0;
	     int promoteLevel = 0;
	          
	     List<SubExpression> listOfSubexpressions = new ArrayList<SubExpression>();	     
	     SubExpression groupSubExpression = null;
	     
	     viewPromoteLevel=getMaxJoinLevel(relationMap, searchView);

	     // Calculate Max Promotion Level for Children in this Group
	     if(parentPromoteLevel == 0) { // first pass
	    	 promoteLevel=viewPromoteLevel;
	    	 parentPromoteLevel=viewPromoteLevel;
	    	 if (grpOpWhen) {
	    		 //isParentGrpWhen = grpOpWhen;
	    		 promoteLevel=calculateChildMaxJoinLevel(filterGroup, filterFieldNames, filterCohort,searchView, relationMap, viewPromoteLevel);
	    	 }
	     } else { // re-calculate promote level for When 
	    	 promoteLevel=parentPromoteLevel;
	    	 if (grpOpWhen) {	    		
	    		 //isParentGrpWhen = grpOpWhen;
	    		 promoteLevel=calculateChildMaxJoinLevel(filterGroup, filterFieldNames, filterCohort,searchView, relationMap, viewPromoteLevel);
	    	 } 
	     }     
	     
	     TableRelationObject promoteRelation = getTableRelationByJoinLevel(promoteLevel, relationMap, searchView);
	     String promoteCol = promoteRelation.getRefCol();
	     
	     // Evaluate the filters in this group
	     Map<String, List<FieldParams>> fieldValuesList = filterGroup.getFilterFields();
	     
	     if(fieldValuesList != null && ignoreField != null && !ignoreField.isEmpty()) {
	    	fieldValuesList.remove(ignoreField);
	     }
	     Set<String> listOfFilterTables = new HashSet<String>();
	    // boolean defaultCustomFilterApplied = false;
	        if (fieldValuesList != null && !fieldValuesList.isEmpty()) {
	        	
	        	for(final Map.Entry<String, List<FieldParams>> fieldValuesEntry : fieldValuesList.entrySet()) {
	        		
	                final FilterField details = filterFieldNames.get(fieldValuesEntry.getKey());
	                String filterTable = details.getVertica().getTable().getDimensionTable() != null 
    					 	&& !details.getVertica().getTable().getDimensionTable().isEmpty() ? 
        					details.getVertica().getTable().getDimensionTable() : details.getVertica().getTable().getFromTable();
        			listOfFilterTables.add(filterTable);	                
	        	}
	        	
	        	for(final Map.Entry<String, List<FieldParams>> fieldValuesEntry : fieldValuesList.entrySet()) {
	        		index++;
	        		final Map<FieldTypeNamePair, List<FieldParams>> typeFieldsMap = new LinkedHashMap<FieldTypeNamePair, List<FieldParams>>();
	                final FilterField details = filterFieldNames.get(fieldValuesEntry.getKey());
	                
	                for (final FieldParams fieldParam : fieldValuesEntry.getValue()) {
	                    final FieldTypeNamePair type = getFieldTypeName(fieldParam.getType(), details);
	                    List<FieldParams> fieldList = typeFieldsMap.get(type);
	                    if (fieldList == null) {
	                        fieldList = new ArrayList<FieldParams>();
	                        typeFieldsMap.put(type, fieldList);
	                    }
	                    fieldList.add(fieldParam);
	                }
	                // Evaluate all the filters of this type
	                for (final Map.Entry<FieldTypeNamePair, List<FieldParams>> typeFieldEntry : typeFieldsMap.entrySet()) {	                	
	                	index++;
	                	
	                	// Iterate through each filter field value to form the child select queries
	                	Set<String> listTablesNew = new HashSet<String>();
	                	List<FieldParams> fieldParams = typeFieldEntry.getValue();
	                	boolean isNotBoolExistInParam = false;
	                	for(FieldParams params : fieldParams) {
	                		isNotBoolExistInParam = (params.getOp() == FilterOperator.IS_NOT || params.getOp() == FilterOperator.NOT_CONTAINS);
	                		if(isNotBoolExistInParam) {
	                			break;
	                		}
	                	}
	                	//int numberOfRepeatingFields = fieldParams.size();
	                	if(fieldParams != null && fieldParams.size() > 0) {
	                		ListIterator<FieldParams> iterator = fieldParams.listIterator();                		
	                		 while ( iterator.hasNext() ) {	                			 
	                			 index++;
	                			 String filterTable = details.getVertica().getTable().getDimensionTable() != null 
	                					 	&& !details.getVertica().getTable().getDimensionTable().isEmpty() ? 
		                					details.getVertica().getTable().getDimensionTable() : details.getVertica().getTable().getFromTable();
	                			 
	                			 // Evaluate the filter field
	                			 // Find the filter field table relation (either by the dimension table or the from table)
	                			TableRelationObject relation = getTableRelationByTableName(filterTable,
	                					relationMap, searchView);
	                			if(relation == null) {
	                				throw new Exception(" Relation not found for the table " + filterTable);
	                			}		
	                			
	                			
	                			// create the subexpression for this relation
	                			if(relation != null) {
            						int joinLevel = relation.getJoinNumber();
            						int tabNumber = joinLevel;
            						String table = relation.getTable();	                						
            						String refCol = relation.getRefCol();
            						String joinCol = relation.getJoinCol();
            						String extraCols = relation.getExtraColumns();
            						String whereExp = relation.getWhereExp();
            						
            						List<FieldParams> childFieldParams = new ArrayList<FieldParams>();
            						FieldParams childParams = iterator.next();
    	                			childFieldParams.add(childParams);
            						SelectBuilder filterSQL = new SelectBuilder(table, details.getVertica().getSchema(), tabNumber++);            						
            						listTablesNew.add(table);
            						// this method call is just to ensure the joins are proper when we have a multiple columns defined coming from different tables
            						getColumnNameForField(details, listTablesNew, tablesMap, filterSQL); // -- DO NOT REMOVE
            						
            						boolean isNotBool = (childParams.getOp() == FilterOperator.IS_NOT || childParams.getOp() == FilterOperator.NOT_CONTAINS);
                					boolean isNotBoolApplied = false;	
                					
            						if(typeFieldEntry.getKey().getType() == FilterType.HASFILTER) {
            							 for (final FieldParams entry : childFieldParams ) {
            								 if (StringUtils.isNotEmpty(entry.getVal()) && entry.getVal().equalsIgnoreCase("No")) { // treat as a 
            				                    isNotBool = true;
            				                 }
            							 }            							
            						} else {
            							if(grpOpAny && !isNotBoolExistInParam) {
            								childFieldParams.clear();
            								childFieldParams.addAll(fieldParams);
            							}
            							final StringBuffer filterFieldText = filterFieldTextFromValue(documentFolderService, documentVerticaFolderService, typeFieldEntry.getKey(), details, childFieldParams, 
	      									isAND, paramMap, listTables, refTable, tablesMap, query, config, fromTable, searchView, filterFieldNames, destDateFormat, filterSQL);
            							
            							if(filterFieldText != null && StringUtils.isNotEmpty(filterFieldText.toString()) && !isMandatoryFilterField(details)) {
            								
            								// Apply the mandatory filter on the filter fields of mandatory filter table

            								for(FilterField mandatoryField : getMandatoryFilterField(filterFieldNames)) {
            									
            									// if a filter has been added for a table which is part of the custom filter relations 
            									// then add that custom expression too to the filter where clause
            									boolean applyCustomFilter = false;            									
            									if(mandatoryField != null 
            											&& (mandatoryField.getVertica().getTable().getFromTable().equalsIgnoreCase(filterTable) 
            													||  listOfFilterTables.contains(filterTable))
            											&& (mandatoryField.getVertica().getFilterSection() != null
            												&& mandatoryField.getVertica().getFilterSection().getFilters() != null
            												&& !mandatoryField.getVertica().getFilterSection().getFilters().getCustomFilter().isEmpty())) {
            										 for(CustomFilterObject custom : mandatoryField.getVertica().getFilterSection().getFilters().getCustomFilter()) {
            											 if(custom != null && custom.getRelationName() != null) {
            												 if(StringUtils.endsWith(custom.getRelationName(), filterTable)) {            													 
            													 filterSQL.addRelationMapping(custom.getRelationName());
            													 applyCustomFilter = true;
            												 }            												        												  
            											 }            											
            										 }
            									}
            									if(mandatoryField != null 
            											&& (mandatoryField.getVertica().getTable().getFromTable().equalsIgnoreCase(filterTable)
            											|| applyCustomFilter)) {	
            										Map<String, FilterField> curFilterField = new HashMap<String, FilterField>();
            										curFilterField.put(mandatoryField.getName(), mandatoryField);
            										Map<String, List<String>> mandatoryExps = QueryBuilderNew.handleMandatoryFilter(filterTable, paramMap, listTablesNew, 
            												tablesMap, filterSQL, curFilterField, rootParentGroup, false);           										
            										             									
            									}
            								}            								
            								String filterFieldTextStr = "(" + filterFieldText.toString() + ")";
            								filterSQL.where(filterFieldTextStr);
            							} else {
            								// Add the mandatory filter
            				// SK - commented out 2/26
            			/*					if(isMandatoryFilterField(details)) {
            									Map<String, List<String>> mandatoryExps = QueryBuilderNew.handleMandatoryFilter(filterTable, paramMap, listTables, 
            											tablesMap, query, filterFieldNames, filterGroup);
            									List<String> mandatoryExpList= mandatoryExps.get(details.getName());
            									if(mandatoryExpList != null) {
	            									for(String mandatoryExp : mandatoryExpList) {
	            										if(StringUtils.isNotEmpty(mandatoryExp)) {
	            											filterSQL.where(mandatoryExp);
	            										}
	            									}  
            									}
            								}
            				*/
            							}
            						}
            						if(StringUtils.isNotEmpty(details.getVertica().getSelect().getWhereExpression())) {   // default filter where Exp..
            							 filterSQL.where(details.getVertica().getSelect().getWhereExpression());
            						}
	                				
            						if(whereExp != null && !whereExp.isEmpty()) {
	                					 filterSQL.where(whereExp);
	                				} 
            						
	             					// Set the select fields and promote to promoteLevel
	                				List<String> extraColsList=Arrays.asList(extraCols.trim().split("\\s*,\\s*"));
	                					
	                				//LOGGER.debug("grpOpWhen = " + grpOpWhen + " and isParentGrpWhen = " + isParentGrpWhen);
	                				if (!grpOpWhen && !isParentGrpWhen && (refCol.equals(promoteCol) || joinCol.equals(promoteCol) || extraColsList.contains(promoteCol))) {
	                     				filterSQL.column(promoteCol, table, isDistinct || details.getVertica().getSelect().isDistinct());	
	                 				} 
	                				else {	                					
		                 					filterSQL.column(refCol, table, isDistinct || details.getVertica().getSelect().isDistinct());	
		                 					if(	refCol!= null && joinCol != null && !refCol.equalsIgnoreCase(joinCol)) {
		                 						filterSQL.column(joinCol, table, false);
		                 					}
		                 					filterSQL.column(extraCols, table, false);		                 				
		                 					filterSQL=promoteSQLExpression(filterSQL, joinLevel, promoteLevel, relationMap,  searchView,  index,  schema, 
		                 							isDistinct || details.getVertica().getSelect().isDistinct(), grpOpWhen, isParentGrpWhen, listTablesNew
		                 							,  tablesMap, paramMap);		                 					
	                				}
	                				
	                				SelectBuilder notSQL = null;
	                				if (isNotBool) {
                						String xAlias="NOT_" +joinLevel+index;	                						
                						notSQL=new SelectBuilder(promoteRelation.getTable(), details.getVertica().getSchema(), tabNumber--);	                						
                						notSQL.column(promoteRelation.getRefCol(), promoteRelation.getTable(), isDistinct || details.getVertica().getSelect().isDistinct());
                						 if(promoteRelation.getRefCol() != null && promoteRelation.getJoinCol()!= null 
                 								&& !promoteRelation.getRefCol().equalsIgnoreCase(promoteRelation.getJoinCol())) {
                							notSQL.column(promoteRelation.getJoinCol(), promoteRelation.getTable(), false);
                    					}
                						// notSQL.column(promoteRelation.getExtraParentColumns(), parentRelation.getTable(), false);
                						String join = " left join (" + filterSQL + ") " + xAlias + " ON " + xAlias + "." + promoteRelation.getRefCol() 
                								+ " = " + promoteRelation.getTable() + "." + promoteRelation.getRefCol();
                						notSQL.join(join);	  
                						notSQL.where(xAlias + "." + promoteRelation.getRefCol() + " is null");
                						isNotBool = true;
                						isNotBoolApplied = true;
                						tabNumber++;	  				
	                				}
	                				
	                				//LOGGER.debug(" Final sub exp evaluated for the filter is <" + (notSQL != null ? notSQL : filterSQL) + ">");
		            				SubExpression exp = new SubExpression(notSQL != null ? notSQL : filterSQL, promoteLevel);	 
                					exp.setFilterNotExpression(isNotBool);
                					exp.setNotExpressionApplied(isNotBoolApplied);
                					listOfSubexpressions.add(exp);	
                					// OR/Any group operation with no "NOT IN/ NOT CONTAINS" operator - so done
                					if(grpOpAny && !isNotBoolExistInParam) {
        								break; 
        							}
	                			} // end of creating the subexpression   			 
	                		 }	
	                	}     
	                }  // end of evaluating all the filters
	        	}
	        } // end evaluating all fields in a Filter Group
	        
	        // Evaluate any child groups
	        if (filterGroup.getChildGroups() != null) {
	            for (final FilterGroup childGroup : filterGroup.getChildGroups()) {
	                if (!filterCohort || childGroup.getCohortOp() == null)  {
	                    // Only support cohort at the first child level
	                   final  SubExpression subGroupExp = buildFilterExpression(documentFolderService, documentVerticaFolderService, childGroup, filterFieldNames, false, paramMap, listTables,refTable, 
	                    		tablesMap, query, config, fromTable, searchView, destDateFormat, measurementField, relationMap, schema, promoteLevel, isDistinct,
	                    		grpOpWhen, ignoreField, rootParentGroup);
	                   /*if(grpOpWhen) {
	                	   isParentGrpWhen = grpOpWhen;
	                   } else {
	                	   isParentGrpWhen = false;
	                   }*/
	                   if (subGroupExp != null) {
	                	   subGroupExp.setGroupExpression(true);
	                	   listOfSubexpressions.add(subGroupExp);
	                   }
	                }
	            } // end of all child groups           
	        }        
	        
	        // Promote everything to a common join level
        	// Evaluate the Group  here - all subexpressions need to be promoted to the most common join level in the group
        	boolean donePromoting = true;
        	//if(grpOpWhen) {
        		/*listOfSubexpressions = promoteToHighestPromoteLevel(listOfSubexpressions, donePromoting,  promoteLevel, 
        			relationMap,  searchView,  index,  schema, grpOpAll, grpOpNotAll, isDistinct); */
        	//}	
        	
        	
        	// Apply Group Operator
        	if(listOfSubexpressions.size() > 0) {
        		groupSubExpression = applyGroupOperators(listOfSubexpressions, donePromoting, promoteLevel, 
					relationMap, searchView, index, schema,  grpOpNotAny,  grpOpAny,
					 grpOpNotAll,  grpOpAll, grpOpWhen, isDistinct);
        	}
			
        	if(groupSubExpression != null) {        		
        		//LOGGER.debug(" Promoting from current level " + promoteLevel + " to the parent level " + parentPromoteLevel);
        		//LOGGER.debug(" grpOpWhen " + grpOpWhen + " isParentGrpWhen " + isParentGrpWhen);
        		//LOGGER.debug(" Before promotion groupSubExpression  " + groupSubExpression.getSubExpression().toString());
        		groupSubExpression = promoteToParentPromoteLevel(groupSubExpression, donePromoting,  parentPromoteLevel, 
        			relationMap,  searchView,  index,  schema, grpOpAll, grpOpNotAll, isDistinct, false, false, null,  tablesMap, paramMap);
        		//LOGGER.debug(" After promotion groupSubExpression  " + groupSubExpression.getSubExpression().toString());
        	}
			
	      return groupSubExpression;
	 }
	
	public static String handleFilterJoinExpression(String subExpression, String curTable, Set<String> listTablesNew, SelectBuilder filterSQL,
			Map<String, Table> tablesMap,  Map<String, Object> paramMap) {
		String filterParam = null;
		for(String key : paramMap.keySet()) {
			if(key != null && StringUtils.containsIgnoreCase(key, "custom")) {
				filterParam =  key;
			}
		}			
		String parsedExpression  =  parseCustomFilter(subExpression, curTable, listTablesNew, tablesMap, filterSQL, false);
		if(filterParam != null) {
			String namedParameterDefaultFilterStr = "(:" + filterParam  + ")";
			parsedExpression = StringUtils.replace(parsedExpression, "?", namedParameterDefaultFilterStr);
		}
		return parsedExpression;
	}
	
	public static SelectBuilder promoteSQLExpression(SelectBuilder filterSQL, int curLevel, int promoteLevel, Map<String, List<TableRelationObject>> relationMap, 
			String searchView, int index, String schema, boolean isDistinct, boolean grpOpWhen, boolean isParentGrpWhen,
			Set<String> listTablesNew,  Map<String, Table> tablesMap,  Map<String, Object> paramMap) throws Exception {
		//LOGGER.debug(" Inside method promoteSQLExpression with current Level " + curLevel + " and promote Level " + promoteLevel);
		if (curLevel>=promoteLevel) {
			return filterSQL;
		}
		TableRelationObject promoteRel = getTableRelationByJoinLevel(promoteLevel, relationMap, searchView); 
		if(promoteRel == null) {
			throw new Exception(" Promote Relation not found for join level " + promoteLevel);
		}	        				
		TableRelationObject relation = getTableRelationByJoinLevel(curLevel, relationMap, searchView); 
		if(relation == null) {
			throw new Exception(" Relation not found for join level " + curLevel);
		}	        				
		TableRelationObject parentRelation = getTableRelationByTableName(relation.getParentTable(), relationMap, searchView);
		if(parentRelation == null) {
			throw new Exception(" Parent Relation not found for relation / join level < " + relation.getTable() + "> - " +  curLevel);
		}
		
		int pJoinNum= parentRelation.getJoinNumber();
		String xAlias = "PCFT_" + relation.getJoinNumber() + index++;
		SelectBuilder wrapper = new SelectBuilder(parentRelation.getTable(), schema, relation.getJoinNumber());	                							
		
		String subExpression = null;
		if(relation.getJoinExp() != null && !relation.getJoinExp().isEmpty()) {
			subExpression = handleFilterJoinExpression(relation.getJoinExp(), relation.getTable(), listTablesNew, filterSQL, tablesMap, paramMap);			
		}	
		String join = " inner join ( " + filterSQL + " ) " 
				+ xAlias + " ON " + relation.getParentTable() + "." + relation.getParentCol() + " = " 
				+ xAlias + "." + relation.getJoinCol();
		if(StringUtils.isNotBlank(subExpression)) {
			join = join + " AND " + subExpression;
		}
		wrapper.join(join);
		String promoteCol=promoteRel.getRefCol();
		String refCol=parentRelation.getRefCol();
		String joinCol=parentRelation.getJoinCol();
		String extraCols=parentRelation.getExtraColumns();
		String table=parentRelation.getTable();
		
		// Set the select fields and promote to promoteLevel
		List<String> extraColsList=Arrays.asList(extraCols.trim().split("\\s*,\\s*"));
		
		if (!grpOpWhen && !isParentGrpWhen && (refCol.equals(promoteCol) || joinCol.equals(promoteCol) || extraColsList.contains(promoteCol))) {
			wrapper.column(promoteCol, table, isDistinct, promoteCol);
			//wrapper.column(extraCols, table, false);
			return wrapper;
		} 
		else {
		
			wrapper.column(parentRelation.getRefCol(), parentRelation.getTable(), isDistinct);
			if(	parentRelation.getRefCol()!= null && parentRelation.getJoinCol() != null && !parentRelation.getRefCol().equalsIgnoreCase(parentRelation.getJoinCol())) {
				wrapper.column(parentRelation.getJoinCol(), parentRelation.getTable(), false);
			}	        				
			wrapper.column(parentRelation.getExtraColumns(),parentRelation.getTable(), false);
			wrapper.column(parentRelation.getExtraParentColumns(), parentRelation.getTable(), false);
			
			wrapper=promoteSQLExpression(wrapper, pJoinNum, promoteLevel, relationMap,  searchView,  index,  schema, isDistinct, grpOpWhen, isParentGrpWhen, 
					listTablesNew, tablesMap, paramMap);
		}	
		return wrapper;
	}
	
	public static int calculateChildMaxJoinLevel(            
            final FilterGroup filterGroup,
            final Map<String, FilterField> filterFieldNames,
            final boolean filterCohort,
            String searchView,            
            Map<String, List<TableRelationObject>> relationMap,
            int viewPromoteLevel
       ) throws Exception {
		
		// Calculate the max join level in this group of filters
		int commonMaxJoinLevel = 0;
		int lowestJoinLevel = 0;
		final List<SubExpression> joinLevelList = getAllChildJoinLevels(filterGroup, filterFieldNames, filterCohort, searchView, relationMap);
	    	
    	if(joinLevelList.size() > 0 ) {
    		// Sort list of subexpressions by join level lowest-->highest
    		Collections.sort(joinLevelList);
    		// Calculate the common join level - is everything at the same level?	        		
    		boolean donePromoting = isEqualJoinLevel(joinLevelList);
    		if(donePromoting) {
				// common join level
    			commonMaxJoinLevel = joinLevelList.get(0).getJoinLevel();    					
			}
    		
    		// Promote until everything is in the same level
    		while(!donePromoting) {
    			for(SubExpression subExpression : joinLevelList) {
    				lowestJoinLevel = subExpression.getJoinLevel();	
    				
    				if(viewPromoteLevel == lowestJoinLevel) { // reached the highest level
    					commonMaxJoinLevel =  lowestJoinLevel;
    					donePromoting = true;
    					break;
    				}

    				TableRelationObject relation = getTableRelationByJoinLevel(lowestJoinLevel, relationMap, searchView); 
    				if(relation == null) {
    					throw new Exception(" Relation not found for join level " + lowestJoinLevel);
    				}	        				
    				TableRelationObject parentRelation = getTableRelationByTableName(relation.getParentTable(), relationMap, searchView);
    				if(parentRelation == null) {
    					throw new Exception(" Parent Relation not found for relation / join level < " + relation.getTable() + "> - " +  lowestJoinLevel);
    				}    				
    				subExpression.setJoinLevel(parentRelation.getJoinNumber());
    				
    				// Re-calculate done = if all join levels are equal
    				donePromoting = isEqualJoinLevel(joinLevelList);
    				
    				if(donePromoting) {
    					// common join
    					commonMaxJoinLevel = subExpression.getJoinLevel();
    					break;
    				}
    			}
    		}
    	}
    	return commonMaxJoinLevel;
	}
	
	public static List<SubExpression> getAllChildJoinLevels(final FilterGroup filterGroup,
            final Map<String, FilterField> filterFieldNames,
            final boolean filterCohort,
            String searchView,            
            Map<String, List<TableRelationObject>> relationMap) throws Exception {
		final List<SubExpression> joinLevelList = new ArrayList<SubExpression>();
    	final Map<String, List<FieldParams>> fieldValuesList = filterGroup.getFilterFields();
    	if (fieldValuesList != null) {
    		for(final Map.Entry<String, List<FieldParams>> fieldValuesEntry : fieldValuesList.entrySet()) {        		
    			final FilterField details = filterFieldNames.get(fieldValuesEntry.getKey());
    			String filterTable = details.getVertica().getTable().getDimensionTable() != null 
				 	&& !details.getVertica().getTable().getDimensionTable().isEmpty() ? 
					details.getVertica().getTable().getDimensionTable() : details.getVertica().getTable().getFromTable();
            	
    			TableRelationObject relation = getTableRelationByTableName(filterTable,
    					relationMap, searchView);
    			if(relation == null) {
    				throw new Exception(" Relation not found for the table " + filterTable);
    			}		
    			
    			
    			// create the subexpression for this relation
    			if(relation != null) {
    				SubExpression exp = new SubExpression(details.getName(), relation.getJoinNumber());
					joinLevelList.add(exp);
    			}
            }
        }
    	if (filterGroup.getChildGroups() != null) {
            for (final FilterGroup childGroup : filterGroup.getChildGroups()) {
            	final boolean grpOpWhen = BoolOp.WHEN == childGroup.getBoolOperator();
                if (!filterCohort || childGroup.getCohortOp() == null)  {
                	List<SubExpression> childJoinList = getAllChildJoinLevels( childGroup, filterFieldNames, filterCohort, searchView, relationMap);
                	if(childJoinList.size() > 0 ) {
                		joinLevelList.addAll(childJoinList);
                	}
                }
            }
    	}
    	return joinLevelList;
	}
	
	public static SubExpression promoteToParentPromoteLevel(SubExpression exp, boolean donePromoting, int promoteLevel, 
			Map<String, List<TableRelationObject>> relationMap, String searchView, int index, String schema, boolean grpOpAll,
			boolean grpOpNotAll, boolean isDistinct, boolean grpOpWhen, boolean isParentGrpWhen, Set<String> listTablesNew
			,  Map<String, Table> tablesMap,  Map<String, Object> paramMap) throws Exception {		
		 	
    	//List<SubExpression> listOfSubexpressionsReturned = new ArrayList<SubExpression>();
    	// Found subexpressions to evaluate
    	//if(listOfSubexpressions.size() > 0 ) {    		   		
    		// Promote until everything is in the same level    		
    		//for(SubExpression subExpression : listOfSubexpressions) {				
    			SubExpression newSubExp = new SubExpression(promoteSQLExpression(exp.getSubExpression(), 
    					exp.getJoinLevel(), promoteLevel, relationMap,  searchView,  index,  schema, isDistinct, grpOpWhen, isParentGrpWhen, listTablesNew
    					, tablesMap,  paramMap), promoteLevel);
    			newSubExp.setListTables(listTablesNew);
    			//LOGGER.debug(" Promote to parent level returned <" + newSubExp.getSubExpression().toString() + ">");
    			//listOfSubexpressionsReturned.add(newSubExp);
    		//}
    	//}    	
    	return newSubExp;
	}
	 

	private static SubExpression applyGroupOperators(List<SubExpression> listOfSubexpressions, boolean donePromoting, int promoteLevel, 
			Map<String, List<TableRelationObject>> relationMap, String searchView, int index, String schema, boolean grpOpNotAny, boolean grpOpAny,
			boolean grpOpNotAll, boolean grpOpAll, boolean grpOpWhen, boolean isDistinct) throws Exception {
		//LOGGER.debug(listOfSubexpressions.size() + " Subexp with operators grpOpNotAny, grpOpNotAny, grpOpNotAll, grpOpAll, grpOpWhen " + grpOpNotAny + grpOpAny 
			//	+ grpOpNotAll + grpOpAll + grpOpWhen);
		SelectBuilder groupexp = null;
		TableRelationObject commonRelation = getTableRelationByJoinLevel(promoteLevel, relationMap, searchView);
		if(commonRelation == null) {
			throw new Exception(" Promote Relation not found for join level " + promoteLevel);
		}
		// ANY or NOT_ANY
		if (grpOpNotAny || grpOpAny) {			
			boolean firstExp = true;
			for (SubExpression subExp :  listOfSubexpressions) {
				//String xAlias="GRP_" + subExp.getJoinLevel() + index++;
				if (firstExp) {
					groupexp = subExp.getSubExpression();
					firstExp=false;
				} else {
					groupexp = groupexp.union(subExp.getSubExpression().toString());				
				}
			}
		}		
		// ALL or NOT ALL
		if (grpOpNotAll || grpOpAll || grpOpWhen) {
			String lastAlias = commonRelation.getTable();			
			boolean firstExp = true;
			String xAlias = "";
			for (SubExpression subExp :  listOfSubexpressions) {				
				if (firstExp)  {
					xAlias = "GRP_" + subExp.getJoinLevel() + index++;
					
					SelectBuilder wrapper = new SelectBuilder();
					wrapper.column(commonRelation.getRefCol(), xAlias, isDistinct);
					if(commonRelation.getRefCol() != null && commonRelation.getJoinCol()!= null 
								&& !commonRelation.getRefCol().equalsIgnoreCase(commonRelation.getJoinCol())) {
							wrapper.column(commonRelation.getJoinCol(), xAlias, false);
	 				}
					/*if(grpOpWhen) {
						wrapper.column(commonRelation.getExtraColumns(), xAlias, false);
					}*/
					// wrapper.column(commonRelation.getExtraParentColumns(), xAlias, false);
					wrapper.from(" ( " + subExp.getSubExpression() + " ) " + xAlias);					
					groupexp = wrapper;
					
					firstExp = false;
				} else {					
						String xAlias_s = "GRP_" + subExp.getJoinLevel() + index++;
						String join = "\n inner join ( " + subExp.getSubExpression() + " ) as " + xAlias_s + " ON " + xAlias + "." + commonRelation.getRefCol()
							+ " = " + xAlias_s + "." + commonRelation.getRefCol();
						groupexp = groupexp.join(join);												
				}
			}
		}
		// If NOT ALL or NOT ANY, wrap the group expression with left join 
		SelectBuilder retexp = null;
		if (grpOpNotAll || grpOpNotAny) {
			String xAlias="GRP_" + commonRelation.getJoinNumber() + index++;
			SelectBuilder wrapper = new SelectBuilder(commonRelation.getTable(), schema, commonRelation.getJoinNumber());
			wrapper.column(commonRelation.getRefCol(), commonRelation.getTable(), isDistinct);
			if(commonRelation.getRefCol() != null && commonRelation.getJoinCol()!= null 
					&& !commonRelation.getRefCol().equalsIgnoreCase(commonRelation.getJoinCol())) {
				wrapper.column(commonRelation.getJoinCol(), commonRelation.getTable(), false);
			}
			/*wrapper.column(commonRelation.getRefCol(), commonRelation.getTable(), true);
			if(	commonRelation.getRefCol()!= null && commonRelation.getJoinCol() != null && !commonRelation.getRefCol().equalsIgnoreCase(commonRelation.getJoinCol())) {
				wrapper.column(commonRelation.getJoinCol(), commonRelation.getTable(), false);
			}*/
			//wrapper.column(commonRelation.getExtraColumns(),commonRelation.getTable(), false);			
			String join = "left join (" + groupexp + ") " + xAlias + " ON " + commonRelation.getTable() + "." + commonRelation.getRefCol() + " = " 
					+ xAlias + "." + commonRelation.getRefCol();
			wrapper.join(join);
			wrapper.where(xAlias + "." + commonRelation.getRefCol() + " is null");
			retexp = wrapper;
		} 
		groupexp = retexp != null ? retexp : groupexp;
		SubExpression groupSubExpression = new SubExpression(groupexp, promoteLevel);
		return groupSubExpression;
	}

	
	
	
	
	public static SubExpression buildFilterExpressionOrig(
            final DocumentFolderService documentFolderService,
            final DocumentVerticaFolderService documentVerticaFolderService,
            final FilterGroup filterGroup,
            final Map<String, FilterField> filterFieldNames,
            final boolean filterCohort,
            Map<String, Object> paramMap,
            Set<String> listTables,
            String refTable,
            Map<String, Table> tablesMap,
            SelectBuilder query,
            SearchConfig config,
            String fromTable,
            String searchView,
            String destDateFormat,
            FilterField measurementField,
            Map<String, List<TableRelationObject>> relationMap,
            String schema
            ) throws Exception {
	String result = null;
	StringBuffer fieldText = null;
	 if (filterGroup == null || filterGroup.getBoolOperator() == null || filterGroup.isDeactivated()) {
           return null;
     }
	 
	 // first join the Owner(from table) with the reference table (admissions_new)
	// joinTables(fromTable, refTable, listTables, tablesMap, query);
	 
	 
	 final boolean isAND = BoolOp.AND == filterGroup.getBoolOperator() | BoolOp.NOT_AND == filterGroup.getBoolOperator();
     final boolean isNOT = BoolOp.NOT_OR == filterGroup.getBoolOperator() | BoolOp.NOT_AND == filterGroup.getBoolOperator();
     final boolean isWHEN = BoolOp.WHEN == filterGroup.getBoolOperator();
     
     final boolean grpOpAll = BoolOp.AND == filterGroup.getBoolOperator();
     final boolean grpOpNotAll = BoolOp.NOT_AND == filterGroup.getBoolOperator();
     final boolean grpOpAny = !grpOpAll && !grpOpNotAll;
     final boolean grpOpNotAny = BoolOp.NOT_OR == filterGroup.getBoolOperator();
     final boolean grpOpWhen = BoolOp.WHEN == filterGroup.getBoolOperator();
     
     int index = 0;
     int maxJoinLevel = getMaxJoinLevel(relationMap, searchView);	     
     StringBuffer childFiltersText = null;
     
     List<SubExpression> listOfSubexpressions = new ArrayList<SubExpression>();
     List<SubExpression> listOfGroups = new ArrayList<SubExpression>();
     SubExpression groupSubExpression = null;
     
     // Evaluate the filters in this group
     final Map<String, List<FieldParams>> fieldValuesList = filterGroup.getFilterFields();
        if (fieldValuesList != null) {
        	for(final Map.Entry<String, List<FieldParams>> fieldValuesEntry : fieldValuesList.entrySet()) {
        		index++;
        		final Map<FieldTypeNamePair, List<FieldParams>> typeFieldsMap = new LinkedHashMap<FieldTypeNamePair, List<FieldParams>>();
                final FilterField details = filterFieldNames.get(fieldValuesEntry.getKey());
                
                for (final FieldParams fieldParam : fieldValuesEntry.getValue()) {
                    final FieldTypeNamePair type = getFieldTypeName(fieldParam.getType(), details);
                    List<FieldParams> fieldList = typeFieldsMap.get(type);
                    if (fieldList == null) {
                        fieldList = new ArrayList<FieldParams>();
                        typeFieldsMap.put(type, fieldList);
                    }
                    fieldList.add(fieldParam);
                }
                // Evaluate all the filters of this type
                for (final Map.Entry<FieldTypeNamePair, List<FieldParams>> typeFieldEntry : typeFieldsMap.entrySet()) {	                	
                	index++;
                	// Iterate through each filter field value to form the child select queries
                	List<FieldParams> fieldParams = typeFieldEntry.getValue();	 
                	int numberOfRepeatingFields = fieldParams.size();
                	if(fieldParams != null && fieldParams.size() > 0) {
                		ListIterator<FieldParams> iterator = fieldParams.listIterator();                		
                		 while ( iterator.hasNext() ) {	                			 
                			 index++;
                			 String filterTable = details.getVertica().getTable().getDimensionTable() != null 
                					 	&& !details.getVertica().getTable().getDimensionTable().isEmpty() ? 
	                					details.getVertica().getTable().getDimensionTable() : details.getVertica().getTable().getFromTable();
                			 
                			 // Evaluate the filter field
                			 // Find the filter field table relation (either by the dimension table or the from table)
                			TableRelationObject relation = getTableRelationByTableName(filterTable,
                					relationMap, searchView);
                			if(relation == null) {
                				throw new Exception(" Relation not found for the table " + filterTable);
                			}
                			// create the subexpression for this relation
                			if(relation != null) {
        						int joinLevel = relation.getJoinNumber();
        						int tabNumber = joinLevel;
        						String table = relation.getTable();	                						
        						String refCol = relation.getRefCol();
        						String joinCol = relation.getJoinCol();
        						String parentTable = relation.getParentTable();
        						String parentCol = relation.getParentCol();
        						String extraCols = relation.getExtraColumns();
        						String extraParentCols = relation.getExtraParentColumns();
        						String whereExp = relation.getWhereExp();
        						
        						List<FieldParams> childFieldParams = new ArrayList<FieldParams>();
        						FieldParams childParams = iterator.next();
	                			childFieldParams.add(childParams);
        						SelectBuilder filterSQL = new SelectBuilder(table, details.getVertica().getSchema(), tabNumber++);	                						
            					filterSQL.column(refCol, table, true);
            					if(	refCol!= null && joinCol != null && !refCol.equalsIgnoreCase(joinCol)) {
            						filterSQL.column(joinCol, table, false);
            					}
            					filterSQL.column(extraCols, table, false);
            					final StringBuffer filterFieldText = filterFieldTextFromValue(documentFolderService, documentVerticaFolderService, typeFieldEntry.getKey(), details, childFieldParams, 
      									isAND, paramMap, listTables, refTable, tablesMap, query, config, fromTable, searchView, filterFieldNames, destDateFormat, filterSQL);
                				 if(filterFieldText != null) {
                					 filterSQL.where(filterFieldText.toString());
                				 }
                				 if(whereExp != null && !whereExp.isEmpty()) {
                					 filterSQL.where(whereExp);
                				 }
                				 
                				 // if it is a repeating field and a ALL/NOTALL operation then go one level up
                				 if(numberOfRepeatingFields > 1 && (grpOpAll || grpOpNotAll)) {
                					 // Wrap this expression with the parent
                					 TableRelationObject parentRelation = getTableRelationByTableName(relation.getParentTable(), relationMap, searchView);
                					 String xAlias ="PRPT" + relation.getJoinNumber();
                	    			 if(parentRelation == null) {
                	    				 throw new Exception(" Parent Relation not found for relation / join level < " + relation.getTable() + "> - " +  relation.getJoinNumber());
                	    			 }
                					 SelectBuilder wrapper = new SelectBuilder(parentRelation.getTable(), details.getVertica().getSchema(), tabNumber);	                						
                					 wrapper.column(parentRelation.getRefCol(), parentRelation.getTable(), true);
                					 if(parentRelation.getRefCol() != null && parentRelation.getJoinCol()!= null 
            								&& !parentRelation.getRefCol().equalsIgnoreCase(parentRelation.getJoinCol())) {
            							wrapper.column(parentRelation.getJoinCol(), parentRelation.getTable(), false);
                					 }
                					 wrapper.column(parentRelation.getExtraColumns(), parentRelation.getTable(), false);
                					 wrapper.column(parentRelation.getExtraParentColumns(), parentRelation.getTable(), false);
                					 //wrapper.column(parentRelation.getRefCol(), false);
                					 String join = "inner join (" + filterSQL + ") " + xAlias + " ON " + relation.getParentTable() + "." + relation.getParentCol()+ " = " 
            								+ xAlias + "." + relation.getJoinCol();
                					 if(relation.getJoinExp() != null && !relation.getJoinExp().isEmpty()) {
            							join = join + " AND " + relation.getJoinExp();
                					 }
                					 wrapper.join(join);
                					 joinLevel = parentRelation.getJoinNumber();
                					 filterSQL = wrapper;
                				}
                				 
                				// If NOT CONTAINS or NOT IN, wrap with left join
                				String retexp = "";
                				SelectBuilder notSQL = null;
                				boolean isNotBool = false;
                				boolean isNotBoolApplied = false;
            					if ((grpOpAll || grpOpNotAll) && (childParams.getOp() == FilterOperator.IS_NOT || childParams.getOp() == FilterOperator.NOT_CONTAINS)) {
            						if(numberOfRepeatingFields > 1) {
            							TableRelationObject parentRelation = getTableRelationByTableName(relation.getParentTable(), relationMap, searchView);
                						String xAlias="NOT_" +joinLevel+index;	                						
                						notSQL = new SelectBuilder(parentRelation.getTable(), details.getVertica().getSchema(), tabNumber--);	                						
                						notSQL.column(parentRelation.getRefCol(), parentRelation.getTable(), true);
                						 if(parentRelation.getRefCol() != null && parentRelation.getJoinCol()!= null 
                 								&& !parentRelation.getRefCol().equalsIgnoreCase(parentRelation.getJoinCol())) {
                							notSQL.column(parentRelation.getJoinCol(), parentRelation.getTable(), false);
                    					}
                						notSQL.column(parentRelation.getExtraParentColumns(), parentRelation.getTable(), false);
                						String join = " left join (" + filterSQL + ") " + xAlias + " ON " + xAlias + "." + parentRelation.getRefCol() 
                								+ " = " + parentRelation.getTable() + "." + parentRelation.getRefCol();
                						notSQL.join(join);	  
                						notSQL.where(xAlias + "." + parentRelation.getRefCol() + " is null");
                						isNotBool = true;
                						isNotBoolApplied = true;
                						tabNumber++;
            						} else {
            							/*String xAlias="NOT_" +joinLevel+index;	                						
                						notSQL = new SelectBuilder(table, details.getVertica().getSchema(), tabNumber--);	                						
                						notSQL.column(refCol, table, true);
                						if(	refCol!= null && joinCol != null && !refCol.equalsIgnoreCase(joinCol)) {
                							notSQL.column(joinCol, table, false);
                    					}
                						notSQL.column(extraCols, table, false);
                						String join = " left join (" + filterSQL + ") " + xAlias + " ON " + xAlias + "." + refCol + " = " + table + "." + refCol;
                						notSQL.join(join);	  
                						notSQL.where(xAlias + "." + refCol + " is null");
                						tabNumber++;*/
            							isNotBool = true;
            						}
            					} 
                				
            					SubExpression exp = new SubExpression(notSQL != null ? notSQL : filterSQL, joinLevel);	 
            					exp.setFilterNotExpression(isNotBool);
            					exp.setNotExpressionApplied(isNotBoolApplied);
            					listOfSubexpressions.add(exp);
                				
                			} // end of creating the subexpression   			 
                		 }	
                	}     
                }  // end of evaluating all the filters     
                
        	}
        } // end evaluating all fields in a Filter Group
        
        // Evaluate any child groups
       // List<SubExpression> childGroupList = new ArrayList<SubExpression>();
       // SubExpression childGroupExp = null;
        if (filterGroup.getChildGroups() != null) {
            for (final FilterGroup childGroup : filterGroup.getChildGroups()) {
                if (!filterCohort || childGroup.getCohortOp() == null)  {
                    // Only support cohort at the first child level
                   final  SubExpression subGroupExp = buildFilterExpressionOrig(documentFolderService, documentVerticaFolderService, childGroup, filterFieldNames, false, paramMap, listTables,refTable, 
                    		tablesMap, query, config, fromTable, searchView, destDateFormat, measurementField, relationMap, schema);
                    if (subGroupExp != null) {
                    	subGroupExp.setGroupExpression(true);
                    	listOfSubexpressions.add(subGroupExp);
                    }
                }
            } // end of all child groups           
        }
        
        // Promote everything to a common join level
    	// Evaluate the Group  here - all subexpressions need to be promoted to the most common join level in the group
    	int commonJoinLevel = 0;
    	boolean donePromoting = true;
    	commonJoinLevel = promoteToCommonJoinLevelOrig(listOfSubexpressions, donePromoting,  commonJoinLevel, 
    			relationMap,  searchView,  index,  schema, grpOpAll, grpOpNotAll);
    	
    	// At this point we have all sub exp at same level and with the same keys and non-zero common join level
    	
		if(commonJoinLevel == 0) { // Should never happen
			throw new Exception("Common join Level evaluated to zero!! Something wrong with the table relations config!!!");
		}
		
		// Apply Group Operator
		groupSubExpression = applyGroupOperatorsOrig(listOfSubexpressions, donePromoting, commonJoinLevel, 
				relationMap, searchView, index, schema,  grpOpNotAny,  grpOpAny,
				 grpOpNotAll,  grpOpAll);
		
		
      return groupSubExpression;
	 
 }
	
	private static SubExpression applyGroupOperatorsOrig (List<SubExpression> listOfSubexpressions, boolean donePromoting, int commonJoinLevel, 
			Map<String, List<TableRelationObject>> relationMap, String searchView, int index, String schema, boolean grpOpNotAny, boolean grpOpAny,
			boolean grpOpNotAll, boolean grpOpAll) {
		SelectBuilder groupexp = null;
		TableRelationObject commonRelation = getTableRelationByJoinLevel(commonJoinLevel, relationMap, searchView);
		// ANY or NOT_ANY
		if (grpOpNotAny || grpOpAny) {			
			boolean firstExp = true;
			for (SubExpression subExp :  listOfSubexpressions) {
				String xAlias="GRP_" + subExp.getJoinLevel() + index++;
				if (firstExp) {
					groupexp = subExp.getSubExpression();
					firstExp=false;
				} else {
					groupexp = groupexp.union(subExp.getSubExpression().toString());				
				}
			}
		}		
		// ALL or NOT ALL
		if (grpOpNotAll || grpOpAll) {
			String lastAlias = commonRelation.getTable();			
			boolean firstExp = true;
			String xAlias = "";
			for (SubExpression subExp :  listOfSubexpressions) {				
				if (firstExp)  {
					xAlias = "GRP_" + subExp.getJoinLevel() + index++;
					//if(subExp.isGroupExpression()) {
						SelectBuilder wrapper = new SelectBuilder();
						wrapper.column(commonRelation.getRefCol(), xAlias, true);
						if(commonRelation.getRefCol() != null && commonRelation.getJoinCol()!= null 
								&& !commonRelation.getRefCol().equalsIgnoreCase(commonRelation.getJoinCol())) {
							wrapper.column(commonRelation.getJoinCol(), xAlias, false);
	 					}
						wrapper.column(commonRelation.getExtraColumns(), xAlias, false);
						wrapper.column(commonRelation.getExtraParentColumns(), xAlias, false);
						wrapper.from(" ( " + subExp.getSubExpression() + " ) " + xAlias);					
						groupexp = wrapper;
					/*} else {
						groupexp = subExp.getSubExpression();
					}*/
					firstExp = false;
				} else {
					//if(subExp.isGroupExpression()) {
						String xAlias_s = "GRP_" + subExp.getJoinLevel() + index++;
						String join = "\n inner join ( " + subExp.getSubExpression() + " ) as " + xAlias_s + " ON " + xAlias + "." + commonRelation.getRefCol()
							+ " = " + xAlias_s + "." + commonRelation.getRefCol();
						groupexp = groupexp.join(join);	
					/*} else {
						String join = "\n inner join (" + subExp.getSubExpression() + " ) " + xAlias + " ON " + lastAlias + "." + commonRelation.getRefCol()
						+ " = " + xAlias + "." + commonRelation.getRefCol();
						groupexp = groupexp.join(join);	
					}	*/							
				}
			}
		}
		// If NOT ALL or NOT ANY, wrap the group expression with left join 
		SelectBuilder retexp = null;
		if (grpOpNotAll || grpOpNotAny) {
			String xAlias="GRP_" + commonRelation.getJoinNumber() + index++;
			SelectBuilder wrapper = new SelectBuilder(commonRelation.getTable(), schema, commonRelation.getJoinNumber());	                						
			wrapper.column(commonRelation.getRefCol(), commonRelation.getTable(), true);
			if(	commonRelation.getRefCol()!= null && commonRelation.getJoinCol() != null && !commonRelation.getRefCol().equalsIgnoreCase(commonRelation.getJoinCol())) {
				wrapper.column(commonRelation.getJoinCol(), commonRelation.getTable(), false);
			}
			wrapper.column(commonRelation.getExtraColumns(),commonRelation.getTable(), false);			
			String join = "left join (" + groupexp + ") " + xAlias + " ON " + commonRelation.getTable() + "." + commonRelation.getRefCol() + " = " 
					+ xAlias + "." + commonRelation.getRefCol();
			wrapper.join(join);
			wrapper.where(xAlias + "." + commonRelation.getRefCol() + " is null");
			retexp = wrapper;
		} 
		groupexp = retexp != null ? retexp : groupexp;
		SubExpression groupSubExpression = new SubExpression(groupexp, commonJoinLevel);
		return groupSubExpression;
	}
	
	private static boolean isEqualJoinLevel(List<SubExpression> listOfSubexpressions) {		
		int savedJoinLevel = 0;
		if(listOfSubexpressions.size() > 1) {
			for(SubExpression subExpression : listOfSubexpressions) {
				int currentJoinLevel = subExpression.getJoinLevel();
				if(savedJoinLevel > 0 && savedJoinLevel != currentJoinLevel) {
					return false;
				}
				savedJoinLevel = currentJoinLevel;        				
			}
		}
		return true;
	}
	
	public static int promoteToCommonJoinLevelOrig(List<SubExpression> listOfSubexpressions, boolean donePromoting, int commonJoinLevel, 
			Map<String, List<TableRelationObject>> relationMap, String searchView, int index, String schema, boolean grpOpAll, boolean grpOpNotAll) throws Exception {		        		        	
    	
    	int lowestJoinLevel = 0;
    	// Found subexpressions to evaluate
    	if(listOfSubexpressions.size() > 0 ) {
    		// Sort list of subexpressions by join level lowest-->highest
    		Collections.sort(listOfSubexpressions);
    		// Calculate the common join level - is everything at the same level?	        		
    		donePromoting = isEqualJoinLevel(listOfSubexpressions);
    		if(donePromoting) {
				// common join level
				commonJoinLevel = listOfSubexpressions.get(0).getJoinLevel();    					
			}
    		
    		// Promote until everything is in the same level
    		while(!donePromoting) {
    			for(SubExpression subExpression : listOfSubexpressions) {
    				lowestJoinLevel = subExpression.getJoinLevel();	

    				TableRelationObject relation = getTableRelationByJoinLevel(lowestJoinLevel, relationMap, searchView); 
    				if(relation == null) {
    					throw new Exception(" Relation not found for join level " + lowestJoinLevel);
    				}	        				
    				TableRelationObject parentRelation = getTableRelationByTableName(relation.getParentTable(), relationMap, searchView);
    				if(parentRelation == null) {
    					throw new Exception(" Parent Relation not found for relation / join level < " + relation.getTable() + "> - " +  lowestJoinLevel);
    				}
    				
    				String xAlias = "PCJL_" + relation.getJoinNumber() + index++;
    				
    				SelectBuilder wrapper = new SelectBuilder(parentRelation.getTable(), schema, relation.getJoinNumber());	                						
    				wrapper.column(parentRelation.getRefCol(), parentRelation.getTable(), true);
    				if(	parentRelation.getRefCol()!= null && parentRelation.getJoinCol() != null && !parentRelation.getRefCol().equalsIgnoreCase(parentRelation.getJoinCol())) {
    					wrapper.column(parentRelation.getJoinCol(), parentRelation.getTable(), false);
					}	        				
    				wrapper.column(parentRelation.getExtraColumns(),parentRelation.getTable(), false);
    				wrapper.column(parentRelation.getExtraParentColumns(), parentRelation.getTable(), false);
    				
    				// apply the filter not in / not contains (if already not applied)
    				String join = "";
    				if((grpOpAll || grpOpNotAll) && subExpression.isFilterNotExpression() && !subExpression.isNotExpressionApplied()) {
    					join = " left join (" + subExpression.getSubExpression() + ") " 
        						+ xAlias + " ON " + relation.getParentTable() + "." + relation.getParentCol() + " = " 
        						+ xAlias + "." + relation.getJoinCol();
        				if(relation.getJoinExp() != null && !relation.getJoinExp().isEmpty()) {
        					join = join + " AND " + relation.getJoinExp();
        				}
        				wrapper.where(xAlias + "." +  relation.getJoinCol() + " is null");
    				} else {    					
        				join = " inner join (" + subExpression.getSubExpression() + ") " 
        						+ xAlias + " ON " + relation.getParentTable() + "." + relation.getParentCol() + " = " 
        						+ xAlias + "." + relation.getJoinCol();
        				if(relation.getJoinExp() != null && !relation.getJoinExp().isEmpty()) {
        					join = join + " AND " + relation.getJoinExp();
        				}
    				}
    				
    				wrapper.join(join);	
    				
    				subExpression.setSubExpression(wrapper);
    				subExpression.setJoinLevel(parentRelation.getJoinNumber());
    				
    				// Re-calculate done = if all join levels are equal
    				donePromoting = isEqualJoinLevel(listOfSubexpressions);
    				
    				if(donePromoting) {
    					// common join
        				commonJoinLevel = subExpression.getJoinLevel();
    					break;
    				}
    			}
    		}
    	} // end promoting all to common join level
    	return commonJoinLevel;
	}
	 
	 public static FieldTypeNamePair getFieldTypeName(final char fieldType, final FilterField details) {
	        FieldTypeNamePair pair = null;
	        String fieldName;

	        switch (fieldType) {
	            case 'P':
	                fieldName = details.getParametric().getName() == null ? details.getName() : details.getParametric().getName();
	                pair = new FieldTypeNamePair(details.getParametric().getType(), fieldName);
	                break;
	            case 'I':
	                fieldName = details.getIndexed().getName() == null ? details.getName() : details.getIndexed().getName();
	                pair = new FieldTypeNamePair(details.getIndexed().getType(), fieldName);
	                break;
	            case 'C':
	                fieldName = details.getName();
	                pair = new FieldTypeNamePair(details.getCustom().getType(), fieldName);
	                break;
	        }

	        return pair;
	   }
	 
	 public static StringBuffer  filterFieldTextFromValue(
	            final DocumentFolderService documentFolderService,
	            final DocumentVerticaFolderService documentVerticaFolderService,
	            final FieldTypeNamePair typeNamePair,
	            final FilterField details,
	            final List<FieldParams> valuesList,
	            final boolean isAND,
	            Map<String, Object> paramMap,
	            Set<String> listTables,
	            String refTable,
	            Map<String, Table> tablesMap,
	            SelectBuilder query,
	            SearchConfig config, 
	             String ownerTable,
	             String searchView,
	             final Map<String, FilterField> filterFieldNames,
	             String destDateFormat,
	             SelectBuilder filterSQL
	    ) throws IllegalArgumentException {

	        StringBuffer fieldText = null;

	        if (valuesList.size() == 0) {
	            return null; //  unit
	        }
	        
	        // escape SQL sensitive characters like single quote ', percentage % and underscore _	        
	        //List<FieldParams> valuesList = cleanFilterValues(oldValuesList);

	        switch (typeNamePair.getType()) {
	            case TEXT:
	            	if (isRangeField(details, config)) {
	            		final FieldTypeNamePair rangePair = new FieldTypeNamePair(FilterType.PARARANGES, typeNamePair.getName());
	            		return  filterFieldTextFromValue(documentFolderService, documentVerticaFolderService, rangePair, details, valuesList, isAND, paramMap, listTables, refTable, tablesMap, 
	            				query, config, ownerTable, searchView, filterFieldNames, destDateFormat, filterSQL);
                    }
	            	
	                boolean isMatchType = true;
	                for (final FieldParams entry : valuesList ) {	                	
	                	// TO DO check if the condition is && or ||	                	
	                    if (FilterOperator.IS != entry.getOp() && FilterOperator.IS_NOT != entry.getOp()) {
	                        isMatchType = false;
	                        break;
	                    }	                    
	                }

	                if (isMatchType) {
	                    final FieldTypeNamePair matchPair = new FieldTypeNamePair(FilterType.MATCH, typeNamePair.getName());
	                    return  filterFieldTextFromValue(documentFolderService, documentVerticaFolderService, matchPair, details, valuesList, isAND, paramMap, listTables, refTable, tablesMap, 
	                    		query, config, ownerTable, searchView, filterFieldNames, destDateFormat, filterSQL);
	                }

	                for (final FieldParams entry : valuesList ) {
	                    if (StringUtils.isNotEmpty(entry.getVal()) || (entry.getListVal() != null && !entry.getListVal().isEmpty())) {
	                        final FilterOperator fieldOp = entry.getOp();
	                        final String textFieldText = buildText(typeNamePair.getName(), fieldOp, entry.getVal(), isAND, paramMap, details, listTables, refTable, tablesMap, query, ownerTable, entry.getListVal());
	                        if (textFieldText != null) {
	                        	if(fieldText == null) {
	                        		fieldText = new StringBuffer(textFieldText);
	                        	} else {
	                        		if(isAND) {
	                        			if(FilterOperator.CONTAINS == fieldOp) {
	                        				fieldText.append(" or ");
	                        			} else {
	                        				fieldText.append(" and ");
	                        			}
	                        			
	                        			fieldText.append(textFieldText);
	                        		} else {
	                        			fieldText.append(" or ");
	                        			fieldText.append(textFieldText);
	                        		}
	                        	}	                        
	                        }
	                    }
	                }
	                break;
	            case MATCH:
	            	if (isRangeField(details, config)) {
	            		final FieldTypeNamePair rangePair = new FieldTypeNamePair(FilterType.PARARANGES, typeNamePair.getName());
	            		return  filterFieldTextFromValue(documentFolderService, documentVerticaFolderService, rangePair, details, valuesList, isAND, paramMap, listTables, refTable, tablesMap, 
	            				query, config, ownerTable, searchView, filterFieldNames, destDateFormat, filterSQL);
                    }
	            	 for (final FieldParams entry : valuesList ) {
		                    if (StringUtils.isNotEmpty(entry.getVal()) || (entry.getListVal() != null && !entry.getListVal().isEmpty())) {
		                        final FilterOperator fieldOp = entry.getOp();
		                        final String textFieldText = buildMatch(typeNamePair.getName(), fieldOp, entry.getVal(), isAND, paramMap, details, listTables, refTable,
		                        		tablesMap, query, ownerTable, entry.getListVal());
		                        if (textFieldText != null) {
		                        	if(fieldText == null) {
		                        		fieldText = new StringBuffer(textFieldText);
		                        	} else {
		                        		if(isAND) {
		                        			fieldText.append(" and ");
		                        			fieldText.append(textFieldText);
		                        		} else {
		                        			fieldText.append(" or ");
		                        			fieldText.append(textFieldText);
		                        		}
		                        	}	                        
		                        }
		                    }
		                }

	                break;

	            case PARARANGES:
	                final List<String> isRangeValues = new ArrayList<String>();
	                final List<String> isNotRangeValues = new ArrayList<String>();
	                String isFieldText = null;

	                for (final FieldParams entry : valuesList ) {
	                    if (StringUtils.isNotEmpty(entry.getVal())) {
	                        if (FilterOperator.IS_RANGE == entry.getOp() || FilterOperator.IS == entry.getOp() || FilterOperator.CONTAINS == entry.getOp()) {
	                            isRangeValues.add(entry.getVal());
	                        } else {
	                            isNotRangeValues.add(entry.getVal());
	                        }
	                    } else if (!entry.getListVal().isEmpty()){
	                    	 if (FilterOperator.IS_RANGE == entry.getOp() || FilterOperator.IS == entry.getOp() || FilterOperator.CONTAINS == entry.getOp()) {
		                            isRangeValues.addAll(entry.getListVal());
		                        } else {
		                            isNotRangeValues.addAll(entry.getListVal());
		                        }
	                    }
	                }
	                if (!isRangeValues.isEmpty())  {
	                	isFieldText =  buildRange(typeNamePair.getName(), isRangeValues, isAND, false, config, paramMap, details, listTables, refTable, tablesMap, query, ownerTable);
	                }
	                
	                if (isFieldText != null) {
                    	fieldText = new StringBuffer(isFieldText);	                        
                    }

	                if (!isNotRangeValues.isEmpty()) {
	                    String isNotFieldText = buildRange(typeNamePair.getName(), isNotRangeValues, isAND, true, config, paramMap, details, listTables, refTable, tablesMap, query, ownerTable);
	                    if (isNotFieldText != null) {
	                        fieldText = (fieldText == null) ? new StringBuffer(isNotFieldText) : (isAND ? fieldText.append(" and ").append(isNotFieldText) : fieldText.append(" and ").append(isNotFieldText));
	                    }
	                }

	                break;

	            case NUMERIC:
	                for (final FieldParams entry : valuesList) {
	                    if (StringUtils.isNotEmpty(entry.getVal())) {
	                        final FilterOperator fieldOp = entry.getOp();
	                        String numericFieldText = null;

	                        if (FilterOperator.RANGE != fieldOp) {
	                            //final Double fieldValue = CollUtils.stringsToDoubles(Collections.singletonList(entry.getVal())).get(0);
	                            numericFieldText = buildNumeric(typeNamePair.getName(), fieldOp, entry.getVal(), config, paramMap, details, listTables, 
	                        			refTable, tablesMap, query, ownerTable);
	                        } else {
	                            numericFieldText = buildRange(typeNamePair.getName(), Collections.singletonList(entry.getVal()), config, paramMap, details, listTables, 
	                        			refTable, tablesMap, query, ownerTable);
	                        }
	                        if (numericFieldText != null) {
	                           fieldText =  (fieldText == null) ? new StringBuffer(numericFieldText) : (isAND ? fieldText.append(" and ").append(numericFieldText) : fieldText.append(" or ").append(numericFieldText));
	                        }
	                    }

	                }
	                break;

	            case DATE:
	                final DateFormat dateFormat = new SimpleDateFormat(details.getIndexed().getFormat());
	                for (final FieldParams entry : valuesList) {
	                    if (StringUtils.isNotEmpty(entry.getVal())) {
	                        final FilterOperator fieldOp = entry.getOp();
	                        final String dateFieldText = buildDate(typeNamePair.getName(), fieldOp, entry.getVal(), dateFormat, paramMap, details, listTables, 
                        			refTable, tablesMap, query, ownerTable, destDateFormat);
	                        if (dateFieldText != null) {
	                            fieldText =  (fieldText == null) ? new StringBuffer(dateFieldText) : (isAND ? fieldText.append(" and ").append(dateFieldText) : fieldText.append(" or ").append(dateFieldText));
	                        }
	                    }
	                }
	                break;
	             
	            case MONTHPART:
	                final DateFormat dateFormat1 = new SimpleDateFormat(details.getIndexed().getFormat());
	                for (final FieldParams entry : valuesList) {
	                    if (StringUtils.isNotEmpty(entry.getVal())) {
	                        final FilterOperator fieldOp = entry.getOp();
	                        final String dateFieldText = buildMonthPart(typeNamePair.getName(), fieldOp, entry.getVal(), dateFormat1, paramMap, details, listTables, 
                        			refTable, tablesMap, query, ownerTable);
	                        if (dateFieldText != null) {
	                            fieldText =  (fieldText == null) ? new StringBuffer(dateFieldText) : (isAND ? fieldText.append(" and ").append(dateFieldText) : fieldText.append(" or ").append(dateFieldText));
	                        }
	                    }
	                }
	                break;
	            case ASOFDATE:
	                final DateFormat dateFormat2 = new SimpleDateFormat(details.getIndexed().getFormat());
	                for (final FieldParams entry : valuesList) {
	                    if (StringUtils.isNotEmpty(entry.getVal())) {
	                        final FilterOperator fieldOp = entry.getOp();
	                        final String dateFieldText = buildAsOfDate(typeNamePair.getName(), fieldOp, entry.getVal(), dateFormat2, paramMap, details, listTables, 
                        			refTable, tablesMap, query, ownerTable);
	                        if (dateFieldText != null) {
	                            fieldText =  (fieldText == null) ? new StringBuffer(dateFieldText) : (isAND ? fieldText.append(" and ").append(dateFieldText) : fieldText.append(" or ").append(dateFieldText));
	                        }
	                    }
	                }
	                break;

	            case DOCUMENTFOLDER:
	               for (final FieldParams entry : valuesList) {
	                    final String folderId = entry.getVal();
	                    if (StringUtils.isNotEmpty(folderId)) {
	                        // TODO: how do we want to handle deleted document folders?
	                    	if(!config.isFolderStoredInVertica()) { // postgres
		                        final FilterOperator fieldOp = entry.getOp();		                        						
		                        String textFieldText = null;
		                        FilterField filterField = null;
		                        if(StringUtils.isNotEmpty(config.getSearchViews().get(searchView).getDocFolderSourceDB()) 
		            	        		&& config.getSearchViews().get(searchView).getDocFolderSourceDB().equalsIgnoreCase("vertica")) {
		                    	   // the filter field needs to be extracted from the folder table
			                       String filterFieldName = documentVerticaFolderService.getFilterFieldByFolderId(Integer.parseInt(folderId), searchView);
			                       filterField = filterFieldNames.get(filterFieldName);		                       
			                       textFieldText = buildDocumentFolder(fieldOp,	                    		   
			                    		   documentVerticaFolderService.getTags(Integer.parseInt(folderId), null, searchView).getRefs(), paramMap, filterField, listTables, 
			                    		   refTable, tablesMap, query, ownerTable, isAND, folderId);
		                       } else {
		                    	   // postgres
		                    	   // the filter field needs to be extracted from the folder table
			                       String filterFieldName = documentFolderService.getFilterFieldByFolderId(Integer.parseInt(folderId), searchView);
			                       filterField = filterFieldNames.get(filterFieldName);
			                       textFieldText = buildDocumentFolder(fieldOp, 
			                        	documentFolderService.getFilterFolderTags(Integer.parseInt(folderId)), paramMap, filterField, listTables, 
		                       			refTable, tablesMap, query, ownerTable, isAND, folderId);
		                       } 
		                       if (textFieldText != null) {
		                            fieldText =  (fieldText == null) ? new StringBuffer(textFieldText) : (isAND ? fieldText.append(" and ").append(textFieldText) 
		                            		: fieldText.append(" or ").append(textFieldText));
		                            
		                            doJoinTables(filterField, refTable, listTables, tablesMap, filterSQL,  ownerTable, paramMap);
		                       }
	                    	} else {
	                    		// VERTICA
	                    		final String documentFolderTable = "documentfolder_references";
		                        final String documentFolderSchema = "find";
		                        final String documentFolderIDColumn = "documentfolder_id";
		                        final String documentFolderRefsColumn = "refs";
	                    		
	                    		final FilterOperator fieldOp = entry.getOp();		                        						
		                        String textFieldText = null;
		                        FilterField filterField = null;       
		                        
		                        if(StringUtils.isNotEmpty(config.getSearchViews().get(searchView).getDocFolderSourceDB()) 
		            	        		&& config.getSearchViews().get(searchView).getDocFolderSourceDB().equalsIgnoreCase("vertica")) {
		                    	   // the filter field needs to be extracted from the folder table
			                       String filterFieldName = documentVerticaFolderService.getFilterFieldByFolderId(Integer.parseInt(folderId), searchView);
			                       filterField = filterFieldNames.get(filterFieldName);		                       
			                       textFieldText = buildDocumentFolder(fieldOp,	                    		   
			                    		   documentVerticaFolderService.getTags(Integer.parseInt(folderId), null, searchView).getRefs(), paramMap, filterField, listTables, 
			                    		   refTable, tablesMap, query, ownerTable, isAND, folderId);
		                       } else {
		                    	   // postgres
		                    	   // the filter field needs to be extracted from the folder table
			                       String filterFieldName = documentFolderService.getFilterFieldByFolderId(Integer.parseInt(folderId), searchView);
			                       filterField = filterFieldNames.get(filterFieldName);
			                       textFieldText = buildDocumentFolderForFolderStoredInVertica(fieldOp, 
			                        	Integer.parseInt(folderId), paramMap, isAND, folderId,
		                       			documentFolderTable, documentFolderIDColumn);
		                       } 
		                       if (textFieldText != null) {
		                            fieldText =  (fieldText == null) ? new StringBuffer(textFieldText) : (isAND ? fieldText.append(" and ").append(textFieldText) 
		                            		: fieldText.append(" or ").append(textFieldText));
		                            
		                            // join between anchor and filter
		                            doJoinTables(filterField, refTable, listTables, tablesMap, filterSQL,  ownerTable, paramMap);
		                            
		                            // join between the filter table and folder table
		                            String fromTable = StringUtils.isNotBlank(filterField.getVertica().getTable().getDimensionTable()) ? filterField.getVertica().getTable().getDimensionTable() : filterField.getVertica().getTable().getFromTable();
		                            if(fromTable != null 
		                            	&& !fromTable.equalsIgnoreCase(documentFolderTable) 
		                            		&& !listTables.contains(documentFolderTable)) {
		                            	
		                            	// the Documentfolder table column refs is hardcoded here
		                            	filterSQL.join(JoinType.InnerJoin, fromTable, documentFolderTable,
		                            			filterField.getVertica().getColumn(), documentFolderRefsColumn, null, documentFolderSchema);
		                            	listTables.add(documentFolderTable);
		                            }                            
		                            
		                       }
	                    		
	                    	}
	                    }
	                }
	                break;	                	            	
	            default:
	                throw new IllegalArgumentException(String.format("Type not recognised: `%s`", typeNamePair.getType()));
	        }


	        return fieldText;

	    }
	 
	 public static String buildText(final String fieldName, final FilterOperator fieldOp, String fieldValue, final boolean isAnd, 
			 						Map<String, Object> paramMap, FilterField field, Set<String> tableNames, 
			 						String refTable, Map<String, Table> tablesMap, SelectBuilder query, String fromTable, List<String> fieldList) {
	       
		 	String fieldText = null;	       
			String columnName = getColumnNameForField(field,  tableNames,  tablesMap, query);
    	   	handleDefaultFilter( refTable, paramMap, tableNames, tablesMap,  query);
    	   	fieldValue = cleanFilterValues(fieldValue);
    	   	if(fieldValue != null) {
	          	if (!fieldValue.equals("[No Value]")) {
			        switch (fieldOp) {
			            case IS:		            	
			            	fieldText = "(" + columnName + " " + " in ('" + fieldValue+ "'))";		            	
			                break;
			            case IS_NOT:
			            	if(isAnd) {
			            		fieldText = "(" + columnName + " " + " in ('" + fieldValue + "'))";
			            	} else {
			            		fieldText = "(" + columnName + " " + " not in ('" + fieldValue + "'))";
			            	}
			                break;
			            case CONTAINS:
			            	fieldText = "(" + columnName + " " + " ilike ('%" + fieldValue + "%'))";	
			                break;
			            case NOT_CONTAINS:
			            	if(isAnd) {
			            		fieldText = "(" + columnName + " " + " ilike ('%" + fieldValue + "%'))";
			            	} else {
			            		fieldText = "(" + columnName + " " + " not ilike ('%" + fieldValue + "%'))";
			            	}
			                break;
			        }
	           	} else {
	    	        switch (fieldOp) {
	    	            case IS:
	    	                fieldText = "(" + columnName + " IS NULL)";
	    	                break;
	    	            case IS_NOT:
	    	                fieldText = "(" + columnName + " IS NOT NULL)";
	    	                break;
	    	            case CONTAINS:
	    	                fieldText = "(" + columnName + " IS NULL)";
	    	                break;
	    	            case NOT_CONTAINS:
	    	                fieldText = "(" + columnName + " IS NOT NULL)";
	    	                break;	
	    	        }
	           	}
    	   	} // end of single Vale
    	   	else if(!fieldList.isEmpty()) {
    	   		StringBuffer listValue = createExpressionForListInOperator(fieldList);
    	   	   	//
    	   	 	if (listValue.length() > 0) {
    	   	        switch (fieldOp) {
    	   	            case IS:		            
    	   	            	fieldText = "(" + columnName + " " + " in (" + listValue.toString() + "))";		            	
    	   	                break;
    	   	            case IS_NOT:
    	   	            	if(isAnd) {
    	   	            		fieldText = "(" + columnName + " " + " in (" + listValue.toString() + "))";	
    	   	            	} else {
    	   	            		fieldText = "(" + columnName + " " + " in (" + listValue.toString() + "))";
    	   	            	}
    	   	                break;
    	   	            case CONTAINS:    	   	            	
			            	fieldText = createExpressionForListContainsOperator(fieldList,  columnName);
			                break;
			            case NOT_CONTAINS:
			            	fieldText = createExpressionForListContainsOperator(fieldList,  columnName);
			                break;    	   	            
    	   	        }
    	   	   	} else {
    	   	        switch (fieldOp) {
    	   	            case IS:
    	   	                fieldText = "(" + columnName + " IS NULL)";
    	   	                break;
    	   	            case IS_NOT:
    	   	                fieldText = "(" + columnName + " IS NOT NULL)";
    	   	                break;  
    	   	            case CONTAINS:
	    	                fieldText = "(" + columnName + " IS NULL)";
	    	                break;
	    	            case NOT_CONTAINS:
	    	                fieldText = "(" + columnName + " IS NOT NULL)";
	    	                break;
    	   	        }
    	   	   	}
    	   	}//End of multivalue

	        return fieldText;
	    }
	 
	 public static String buildTextForAutoComplete(final String fieldName, final FilterOperator fieldOp, final String fieldValue, final boolean isAnd, 
				Map<String, Object> paramMap, FilterField field, Set<String> tableNames, 
				String refTable, Map<String, Table> tablesMap, SelectBuilder query, String fromTable) {
		//boolean paramExists = false;
		String fieldText = null;	       
		String columnName = getColumnNameForField(field,  tableNames,  tablesMap,  query);			
		String namedParameter = getNamedParameter(field.getVertica().getColumn(), field.getName(), fieldOp.toString());
		namedParameter = cleanParameterName(namedParameter);
		boolean isHasFilter = false;
		if(field.getParametric().getType() == FilterType.HASFILTER) {
			isHasFilter = true;
		}
		
		doJoinTables(field,  refTable, tableNames,  tablesMap, query, fromTable, paramMap);			
		handleDefaultFilter( refTable, paramMap, tableNames, tablesMap,  query);
		
		if (!fieldValue.equals("[No Value]")) {
			switch (fieldOp) {
				case IS:
					
					namedParameter = namedParameter + cleanParameterName(fieldValue);
					fieldText = "(" + columnName + " " + " in (:" + namedParameter + "))";		            	
					paramMap.put(namedParameter, fieldValue);
				 break;
				case IS_NOT:
					
				 namedParameter = namedParameter + cleanParameterName(fieldValue);
				 fieldText = "(" + columnName + " " + " not in (:" + namedParameter + "))";		            	
					paramMap.put(namedParameter, fieldValue);
				 break;
				case CONTAINS:
					if(isHasFilter) {
						if (StringUtils.contains(fieldValue.toLowerCase(), "y")) { // treat as a not null
							fieldText = "(" + columnName + " IS NOT NULL)";
		                 } else if (StringUtils.contains(fieldValue.toLowerCase(), "n")) {
		                	fieldText = "(" + columnName + " IS NULL)";
		                 }	
					} else {
						namedParameter = namedParameter + cleanParameterName(fieldValue);
						fieldText = "(" + columnName + " " + " ilike (:" + namedParameter + "))";					
						paramMap.put(namedParameter, "%" + fieldValue + "%");
					}
					
				 break;
				case NOT_CONTAINS:
					namedParameter = namedParameter + cleanParameterName(fieldValue);
					fieldText = "(" + columnName + " " + " not ilike (:" + namedParameter + "))";
					paramMap.put(namedParameter, "%" + fieldValue + "%");
				 break;
			
			}
		} else {
			switch (fieldOp) {
			case IS:
			 fieldText = "(" + columnName + " IS NULL)";
			 break;
			case IS_NOT:
			 fieldText = "(" + columnName + " IS NOT NULL)";
			 break;
			case CONTAINS:
			 fieldText = "(" + columnName + " IS NULL)";
			 break;
			case NOT_CONTAINS:
			 fieldText = "(" + columnName + " IS NOT NULL)";
			 break;	
			}
		}		
		return fieldText;
	 }
	 
	 public static String buildTextForAutoCompleteNew(final String fieldName, final FilterOperator fieldOp, final String fieldValue, final boolean isAnd, 
				Map<String, Object> paramMap, FilterField field, Set<String> tableNames, 
				String refTable, Map<String, Table> tablesMap, SelectBuilder query, String fromTable, String view) {
		//boolean paramExists = false;
		String fieldText = null;	       
		String columnName = getColumnNameForField(field, tableNames, tablesMap, query);			
		String namedParameter = getNamedParameter(field.getVertica().getColumn(), field.getName(), fieldOp.toString());
		namedParameter = cleanParameterName(namedParameter);
		
		doJoinTables(field,  refTable, tableNames,  tablesMap, query, fromTable, paramMap);			
		handleDefaultFilter( refTable, paramMap, tableNames, tablesMap,  query);
		if (!fieldValue.equals("[No Value]")) {
			switch (fieldOp) {
				case IS:
					
					namedParameter = namedParameter + cleanParameterName(fieldValue);
					fieldText = "(" + columnName + " " + " in (:" + namedParameter + "))";		            	
					paramMap.put(namedParameter, fieldValue);
				 break;
				case IS_NOT:
					
				 namedParameter = namedParameter + cleanParameterName(fieldValue);
				 fieldText = "(" + columnName + " " + " not in (:" + namedParameter + "))";		            	
					paramMap.put(namedParameter, fieldValue);
				 break;
				case CONTAINS:
					namedParameter = namedParameter + cleanParameterName(fieldValue);
					fieldText = "(" + columnName + " " + " ilike (:" + namedParameter + "))";
					
					paramMap.put(namedParameter, "%" + fieldValue + "%");
				 break;
				case NOT_CONTAINS:
					namedParameter = namedParameter + cleanParameterName(fieldValue);
					fieldText = "(" + columnName + " " + " not ilike (:" + namedParameter + "))";
					paramMap.put(namedParameter, "%" + fieldValue + "%");
				 break;
			
			}
		} else {
			switch (fieldOp) {
			case IS:
			 fieldText = "(" + columnName + " IS NULL)";
			 break;
			case IS_NOT:
			 fieldText = "(" + columnName + " IS NOT NULL)";
			 break;
			case CONTAINS:
			 fieldText = "(" + columnName + " IS NULL)";
			 break;
			case NOT_CONTAINS:
			 fieldText = "(" + columnName + " IS NOT NULL)";
			 break;	
			}
		}		
		return fieldText;
	 }
	 
	 public static String buildMatch(final String fieldName, final FilterOperator fieldOp, String fieldValue, final boolean isAnd, 
				Map<String, Object> paramMap, FilterField field, Set<String> tableNames, 
				String refTable, Map<String, Table> tablesMap, SelectBuilder query, String ownerTable, List<String> fieldList) {
        
		 	String fieldText = null;	       
			String columnName = getColumnNameForField(field,  tableNames,  tablesMap, query);
    	   	handleDefaultFilter( refTable, paramMap, tableNames, tablesMap,  query);
    	   	fieldValue = cleanFilterValues(fieldValue);
    	   	if(fieldValue != null){
	         	if (!fieldValue.equals("[No Value]")) {
			        switch (fieldOp) {
			            case IS:		            
			            	fieldText = "(" + columnName + " " + " in ('" + fieldValue+ "'))";		            	
			                break;
			            case IS_NOT:
			            	if(isAnd) {
			            		fieldText = "(" + columnName + " " + " in ('" + fieldValue + "'))";	
			            	} else {
			            		fieldText = "(" + columnName + " " + " not in ('" + fieldValue + "'))";
			            	}
			                break;
			            case CONTAINS:
			            	fieldText = "(" + columnName + " " + " ilike ('%" + fieldValue + "%'))";		            	
			                break;
			            case NOT_CONTAINS:
			            	if(isAnd) {
			            		fieldText = "(" + columnName + " " + " ilike ('%" + fieldValue + "%'))";
			            	} else {
			            		fieldText = "(" + columnName + " " + " not ilike ('%" + fieldValue + "%'))";
			            	}
			                break;
		
			        }
	           	} else {
	    	        switch (fieldOp) {
	    	            case IS:
	    	                fieldText = "(" + columnName + " IS NULL)";
	    	                break;
	    	            case IS_NOT:
	    	                fieldText = "(" + columnName + " IS NOT NULL)";
	    	                break;
	    	            case CONTAINS:
	    	                fieldText = "(" + columnName + " IS NULL)";
	    	                break;
	    	            case NOT_CONTAINS:
	    	                fieldText = "(" + columnName + " IS NOT NULL)";
	    	                break;	
	    	        }
	           	}
		 	} else if(!fieldList.isEmpty()) {
		 		StringBuffer listValue = createExpressionForListInOperator(fieldList);
    	   	   	
    	   	 	if (listValue.length() > 0) {
    	   	        switch (fieldOp) {
    	   	            case IS:		            
    	   	            	fieldText = "(" + columnName + " " + " in (" + listValue.toString() + "))";		            	
    	   	                break;
    	   	            case IS_NOT:
    	   	            	if(isAnd) {
    	   	            		fieldText = "(" + columnName + " " + " in (" + listValue.toString() + "))";	
    	   	            	} else {
    	   	            		fieldText = "(" + columnName + " " + " in (" + listValue.toString() + "))";
    	   	            	}
    	   	                break;
    	   	            case CONTAINS:    	   	            	
			            	fieldText = createExpressionForListContainsOperator(fieldList,  columnName);
			                break;
			            case NOT_CONTAINS:
			            	fieldText = createExpressionForListContainsOperator(fieldList,  columnName);
			                break;  
    	   	            
    	   	        }
    	   	   	} else {
    	   	        switch (fieldOp) {
    	   	            case IS:
    	   	                fieldText = "(" + columnName + " IS NULL)";
    	   	                break;
    	   	            case IS_NOT:
    	   	                fieldText = "(" + columnName + " IS NOT NULL)";
    	   	                break; 
    	   	            case CONTAINS:
	    	                fieldText = "(" + columnName + " IS NULL)";
	    	                break;
	    	            case NOT_CONTAINS:
	    	                fieldText = "(" + columnName + " IS NOT NULL)";
	    	                break;	
    	   	        }
    	   	   	}
    	   	}//End of multivalue
	        return fieldText;
	}
    
    public static String buildRange(
            final String idolField,
            final List<String> values,
            SearchConfig config, Map<String, Object> paramMap, FilterField field, Set<String> tableNames, 
			String refTable, Map<String, Table> tablesMap, SelectBuilder query, String ownerTable
    ) {
        return buildRange(idolField, values, true, false, config, paramMap, field, tableNames, 
    			refTable, tablesMap, query, ownerTable);
    }


    /**
     * Makes a range field text value (Filter.NRANGE).
     *
     * Null indicates FieldText Unit
     *
     * @param idolField
     * @param values
     * @return
     */
    public static String buildRange(
            final String fieldName,
            final List<String> values,
            final boolean isAND,
            final boolean isNot,
            final SearchConfig config,
            Map<String, Object> paramMap, FilterField field, Set<String> tableNames, 
			String refTable, Map<String, Table> tablesMap, SelectBuilder query, String ownerTable
    ) {
        String result = null; 
        StringBuffer resultText = null;
        String columnName = getColumnNameForField(field,  tableNames, tablesMap,  query); 
	   	handleDefaultFilter( refTable, paramMap, tableNames, tablesMap,  query);
        for (final String value : values) {
        	String fieldText = "";
        	if (value != null && !value.equalsIgnoreCase("[No Value]")) {
        	
	        	Number lower = null;
	            Number upper = null;	        	
	        	// get the min and max from the config and if not found then try to split by commas or arrows
	        	GroupingInfo info = getRangeInfo(field.getName(), config, value);
	        	if(info != null) {
	        		lower = (info.getMinVal() != null && !info.getMinVal().isEmpty()) ? Double.parseDouble(info.getMinVal()) : null;
	        		upper = (info.getMaxVal() != null && !info.getMaxVal().isEmpty()) ? Double.parseDouble(info.getMaxVal()) : null;
	        
	        	} else {
	        		// split on arrows and commas
		            final String[] rangeStrs = value.split("[,\\u2192]");	
		            lower = getRangeNumber(rangeStrs[0]);
		            upper = rangeStrs.length > 1 ? getRangeNumber(rangeStrs[1]) : null;
	        	
	        	}
	        	String betweenOP = " between ";
	        	String geOP = " >= ";
	        	String leOp = " < ";
	        	if(isNot) {
	        		betweenOP = " not between ";
	        		geOP = " < ";
	        		leOp = " >= ";
	        	}
	        	
	            
	            String namedParameterLower = null;
	            String namedParameterUpper = null;
	            if (lower != null) {
	            	namedParameterLower = field.getVertica().getColumn() + lower;
	            	namedParameterLower = cleanParameterName(namedParameterLower);
	            
	            		 
	                if (upper != null && !upper.toString().isEmpty()) {	                	
	                	fieldText = fieldText + "((" + columnName + geOP + "(" + lower + ")) and ("  + columnName + leOp + "(" + upper + ")))";             	
	                	
	                } else { // only lower
	                	fieldText = fieldText + "(";           	 
	                	fieldText = fieldText + "(" + columnName + geOP + "(" + lower + ")))";	                	
	                }
	
	            } else {
	                if (upper != null) {	                	
	                	fieldText = fieldText + "(";   
	                    fieldText =  fieldText + "(" + columnName +  leOp  + "(" + upper + ")))";	                   
	                }
	            }
        	} else {
        		if(value.equalsIgnoreCase("[No Value]")) {
        			fieldText = fieldText + "(" + columnName + " IS NULL)";
        		}
        	}          


            if (fieldText != null && !fieldText.isEmpty()) {
            	resultText = (resultText == null) ? new StringBuffer(fieldText) : isAND ? resultText.append(" and ").append(fieldText) : resultText.append(" or ").append(fieldText);
            }
        }
        if(resultText != null) {
        	result = resultText.toString();
        }

        return result;
    }
    
    public static String buildNumeric(final String fieldName, final FilterOperator fieldOp, final Object value,  
    		SearchConfig config, Map<String, Object> paramMap, FilterField field, Set<String> tableNames, 
			String refTable, Map<String, Table> tablesMap, SelectBuilder query, String ownerTable) {
    	String fieldText = null;
    	String columnName = getColumnNameForField(field, tableNames, tablesMap,  query);
    	//doJoinTables(fromTable, dimensionTable, mappingKey,  refTable, tableNames,  tablesMap, query, ownerTable, paramMap);
        handleDefaultFilter( refTable, paramMap, tableNames, tablesMap,  query);
        
       	if (!value.equals("[No Value]")) {
       		switch (fieldOp) {        
	            case GT:
	            	fieldText = "(" + columnName + " > " + "('" + value + "'))";
	            	break;
	            case GE:
	            	fieldText = "(" + columnName + " >= " + "('" + value + "'))";
	                break;
	            case LT:
	            	fieldText = "(" + columnName + " < " + "('" + value + "'))";	
	            	break;
	            case LE:
	            	fieldText = "(" + columnName + " <= " + "('" + value + "'))";
	                break;
	            case EQ:
	            	fieldText = "(" + columnName + " = " + "('" + value + "'))";
	                break;
	            case NE:
	            	fieldText = "(" + columnName + " <> " + "('" + value + "'))";
	                break;
       		}
       	} else {
	        switch (fieldOp) {
	        	case GT:
	        		break;
	        	case GE:
	        		break;
	        	case LT:
	        		break;
	            case LE:
	        		break;
	            case EQ:
	                fieldText = "(" + columnName + " IS NULL)";
	                break;	            
	            case IS:
	                fieldText = "(" + columnName + " IS NULL)";
	                break;	            
	            case CONTAINS:
	                fieldText = "(" + columnName + " IS NULL)";
	                break;	            
	            case NOT_CONTAINS:
	                fieldText = "(" + columnName + " IS NOT NULL)";
	                break;	
	            case IS_NOT:
	                fieldText = "(" + columnName + " IS NOT NULL)";
	                break;	
	        }
       	}
       	return fieldText;
    }
 	
	public static String buildNumeric(String dimensionTable, String fromTable, String tableColumn, String miscOperator, String selectExpression,
    		final String fieldName, final FilterOperator fieldOp, final Object value,  
    		SearchConfig config, Map<String, Object> paramMap, Set<String> tableNames, 
			String refTable, Map<String, Table> tablesMap, SelectBuilder query, String ownerTable, String mappingKey) {
    	String fieldText = null;
    	String columnName = getColumnNameForField(dimensionTable, fromTable, tableColumn, miscOperator, selectExpression);		
        doJoinTables(fromTable, dimensionTable, mappingKey,  refTable, tableNames,  tablesMap, query, ownerTable, paramMap);
	   	handleDefaultFilter( refTable, paramMap, tableNames, tablesMap,  query);
        String namedParameterNumber = tableColumn + "number";
        namedParameterNumber = cleanParameterName(namedParameterNumber);
        paramMap.put(namedParameterNumber, value);
       	if (!value.equals("[No Value]")) {
       		switch (fieldOp) {        
	            case GT:
	            	fieldText = "(" + columnName + " > " + "(:" + namedParameterNumber + "))";	            	
	                break;
	            case GE:
	            	fieldText = "(" + columnName + " >= " + "(:" + namedParameterNumber + "))";
	                break;
	            case LT:
	            	fieldText = "(" + columnName + " < " + "(:" + namedParameterNumber + "))";	            	
	            	break;
	            case LE:
	            	fieldText = "(" + columnName + " <= " + "(:" + namedParameterNumber + "))";
	                break;
	            case EQ:
	            	fieldText = "(" + columnName + " = " + "(:" + namedParameterNumber + "))";
	                break;
	            case NE:
	            	fieldText = "(" + columnName + " <> " + "(:" + namedParameterNumber + "))";
	                break;
       		}
       	} else {
	        switch (fieldOp) {
	        	case GT:
	        		break;
	        	case GE:
	        		break;
	        	case LT:
	        		break;
	            case LE:
	            	break;
	            case EQ:
	                fieldText = "(" + columnName + " IS NULL)";
	                break;	            
	            case IS:
	                fieldText = "(" + columnName + " IS NULL)";
	                break;	            
	            case CONTAINS:
	                fieldText = "(" + columnName + " IS NULL)";
	                break;	            
	            case NOT_CONTAINS:
	                fieldText = "(" + columnName + " IS NOT NULL)";
	                break;	
	            case IS_NOT:
	                fieldText = "(" + columnName + " IS NOT NULL)";
	                break;	
	        }
       	}
       	return fieldText;
    }   	
    
    
    public static String buildAsOfDate(final String idolField, final FilterOperator fieldOp, final String submittedValue, final DateFormat dateFormat,
    		Map<String, Object> paramMap, FilterField field, Set<String> tableNames, 
			String refTable, Map<String, Table> tablesMap, SelectBuilder query, String ownerTable) {
        
    	String fieldText = null;
    	if(!query.isDefaultFilterAdded(field.getName())) {  
	        final String[] values = submittedValue.split(",");
	        String subExpression = field.getVertica().getFilterSection().getFilters().getCustomFilter().get(0).getCustomFilterExpression();
	        subExpression = parseCustomFilter(subExpression, refTable, tableNames, tablesMap, query, false);
	        //doJoinTables(field,  refTable, tableNames,  tablesMap, query, ownerTable, paramMap);
		   	handleDefaultFilter( refTable, paramMap, tableNames, tablesMap,  query);	       
	
	        switch (fieldOp) {
	            case AS_OF_DATE:		
	     			String namedParameterDateAsOf = getNamedParameter("namedParameterAsOfDate", field.getName(), fieldOp.toString());
	     			namedParameterDateAsOf = cleanParameterName(namedParameterDateAsOf);
	                paramMap.put(namedParameterDateAsOf, values[0]); 
	                String namedParameterAsOf = "(:" + namedParameterDateAsOf + ")";
	                subExpression = StringUtils.replace(subExpression, "?", namedParameterAsOf);
	                if(subExpression != null) {
	                	fieldText = fieldText + subExpression ;
	                }
	                break;            
	        }
    	}

        return fieldText;
    }

    
    public static String buildMonthPartExpression(FilterField field, final String submittedValue, final DateFormat dateFormat, String columnName,
    		Set<String> tableNames, Map<String, Table> tablesMap, SelectBuilder query) {
	   	// Generate a list of partition values
		String fieldText = null;		   
		fieldText="";
        final String[] values = submittedValue.split(",");
 	
	   	// Generate End-date string (either sysdate or value
	   	long time = System.currentTimeMillis();
		java.util.Date qDate=new Date(time);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
		SimpleDateFormat idf = new SimpleDateFormat("MM/yyyy");

    		
		// Check end valid date
		if (!values[0].equals("")) {
			try {
    			qDate=idf.parse(values[0]);
    		} catch (ParseException e) {
    			throw new IllegalArgumentException("Bad date format [" + dateFormat.toString() + "].");
    		}
		};

		// Generate upper partition bound condition
		String endDate=sdf.format(qDate);
		if(StringUtils.isBlank(columnName)) {
			columnName = getColumnNameForField(field, tableNames, tablesMap, query);
		}
    	fieldText = "(" + columnName + " < " +  endDate + ")";
		
   		// Generate lower partition bound condition if one exists    	
		// Generate lower bound string based on including fixed value
		String startDate=""; 		
		Calendar cal = Calendar.getInstance();
		cal.setTime(qDate);
	
		java.util.Date result = null;
		if (values[1].equals("THREE_MONTHS")) {
			// Calculate date 3 months prior 
			cal.add(Calendar.MONTH, -3);    		
    		result=cal.getTime();
    		startDate=sdf.format(result);
    		
		} else if (values[1].equals("SIX_MONTHS")) {
   			// Calculate date 6 months prior 
    		cal.add(Calendar.MONTH, -6);
      		result=cal.getTime();
    		startDate=sdf.format(result);
  		} else if (values[1].equals("ONE_YEAR")) {
   			// Calculate date 1 months prior 
    		cal.add(Calendar.MONTH, -12);
      		result=cal.getTime();
    		startDate=sdf.format(result);
  		} else if (values[1].equals("TWO_YEARS")) {
   			// Calculate date 1 months prior 
    		cal.add(Calendar.MONTH, -24);
      		result=cal.getTime();
    		startDate=sdf.format(result);
  		} else if (values[1].equals("THREE_YEARS")) {
   			// Calculate date 1 months prior 
    		cal.add(Calendar.MONTH, -36);
      		result=cal.getTime();
    		startDate=sdf.format(result);
  		}// All or others assumes no lower bound
   		
       	if (!startDate.equals("")) {
           fieldText = "(" + fieldText + " and (" + columnName + " >= " +  startDate + "))";
    	}	

       	return fieldText;
    }
   
    
    public static String buildMonthPart(final String idolField, final FilterOperator fieldOp, final String submittedValue, final DateFormat dateFormat,
    		Map<String, Object> paramMap, FilterField field, Set<String> tableNames, 
			String refTable, Map<String, Table> tablesMap, SelectBuilder query, String ownerTable) {
        
    		String fieldText = "";
	        final String[] values = submittedValue.split(",");
	        String subExpression = "";
	        if(field.getVertica() != null
	        		&& field.getVertica().getFilterSection() != null
	        		&& field.getVertica().getFilterSection().getFilters() != null
	        		&& field.getVertica().getFilterSection().getFilters().getCustomFilter().size() > 0) {
	        	subExpression = field.getVertica().getFilterSection().getFilters().getCustomFilter().get(0).getCustomFilterExpression();
	        }
	        subExpression = parseCustomFilter(subExpression, refTable, tableNames, tablesMap, query, false);
	        //doJoinTables(field,  refTable, tableNames,  tablesMap, query, ownerTable, paramMap);
		   	handleDefaultFilter(refTable, paramMap, tableNames, tablesMap,  query);	   
		   	
		   	fieldText=buildMonthPartExpression(field, submittedValue, dateFormat, null, tableNames, tablesMap, query);
		   	return fieldText;
    }
 
    
    
       
    
    
    // SK: Commented out 2/26/2017

    public static void handleDefaultFilter(String curTable, Map<String, Object> paramMap, Set<String> tableNames, Map<String, Table> tablesMap, SelectBuilder query) {
    	Table table = tablesMap.get(curTable);
    	String subExpression = "";
    	
    	// SK: 2/26/2017 This is covered by handleMandatoryFilters, so not needed
     	if (true)
     		return;
   	
    	if(!query.isDefaultFilterAdded(curTable)) {
	    	if(table != null) {
	    		subExpression = table.getDefaultFilter();
	    		 if(subExpression != null && !subExpression.isEmpty()) {
	    			 subExpression = parseCustomFilter(subExpression, curTable, tableNames, tablesMap, query, false);
	    			 String namedParameterDefaultFilter = getNamedParameter("namedParameterAsOfDate", curTable, "");	      			 
		    		 namedParameterDefaultFilter = cleanParameterName(namedParameterDefaultFilter);
		    		 long time = System.currentTimeMillis();
		    		 java.sql.Date date = new java.sql.Date(time);
		             paramMap.put(namedParameterDefaultFilter, date); 
		             String namedParameterDefaultFilterStr = "(:" + namedParameterDefaultFilter + ")";
		             subExpression = StringUtils.replace(subExpression, "?", namedParameterDefaultFilterStr);	  
		             if(subExpression != null && !subExpression.isEmpty()) {
		            	 query.where(subExpression);
		            	 query.setDefaultFilterAdded(curTable,true);
		             }
	    		 }
	    	}
    	}	
    }

    
    
    // This method is to handle the mandatory filter
    public static Map<String, List<String>> handleMandatoryFilter(String curTable, Map<String, Object> paramMap, Set<String> tableNames, 
    		Map<String, Table> tablesMap, SelectBuilder query, Map<String, FilterField> filterFieldNames, FilterGroup filterGroup, boolean isResultOrExport) {
    	 Map<String, List<String>> returnSubExps = new HashMap<String, List<String>>();
    	 List<String> subExps = null;
   
    	 
    	 // apply the mandatory filters
    	 for(final Map.Entry<String, FilterField> fieldValuesEntry : filterFieldNames.entrySet()) {
    		 if(fieldValuesEntry != null
    			&& fieldValuesEntry.getValue() != null 
    			&& fieldValuesEntry.getValue().getVertica() != null
    			&& fieldValuesEntry.getValue().getVertica().getFilterSection() != null) {
    				 
			 	 boolean userAddedFilterManually = false;
				 // avoid repetitive filter processing for a query
				// if(!query.isDefaultFilterAdded(fieldValuesEntry.getKey())) {    					 
				 
    				 // found the mandatory filter
    				 String defaultType = fieldValuesEntry.getValue().getVertica().getFilterSection().getDefaultType();
    				 String defaultValue = fieldValuesEntry.getValue().getVertica().getFilterSection().getDefaultValue();
    				 
    				 // Override the default filter value if a user selected this same filter from UI
    				 // also, remove it from the FilterGroup to avoid repeated filtering
    				 if (filterGroup != null && !filterGroup.isDeactivated()) {
    		    		 Map<String, List<FieldParams>> fieldValuesList = filterGroup.getFilterFields();
    		    		 if (fieldValuesList != null) {
    		    			 List<FieldParams>  valuesList = fieldValuesList.get(fieldValuesEntry.getKey());
    		    			 if(valuesList != null && valuesList.size() > 0) {
    		    				 for (final FieldParams entry : valuesList ) {
        		    				 if(entry != null) {
        		    					 userAddedFilterManually = true;
        		    					 defaultValue = entry.getVal(); // should only be one value ideally??
        		    					 // fieldValuesList.remove(fieldValuesEntry.getKey()); // remove the filter from FilterGroup to avoid double filtering
        		    					 break; 
        		    				 }
        		    			 } 
    		    			 }    		    			 
    		    		 }
    			     }    
    				 // process only if filter is mandatory OR if not mandatory it has been added manually by a user
    				 if(fieldValuesEntry.getValue().getVertica().getFilterSection().isMandatory() || userAddedFilterManually) {
	    				 // If MonthPartition, add the filter expression
   
    					 if (fieldValuesEntry.getValue().getIndexed() != null && (fieldValuesEntry.getValue().getIndexed().getType() == FilterType.MONTHPART)) {
    					    	// removd this condition on 16-Nov for a Ohio bug - user added filter was not being applied				 
    							//&& !userAddedFilterManually) {
    					
	    					 FilterField field=fieldValuesEntry.getValue();
	    					 String submittedValue=defaultValue;
	    					 DateFormat dateFormat = new SimpleDateFormat(field.getIndexed().getFormat());
	    					 String subExpressionP = buildMonthPartExpression(field, submittedValue, dateFormat, null, tableNames, tablesMap, query);
	    					
	    					 if(!query.isDefaultFilterAdded(fieldValuesEntry.getKey())) {
	    						 // add default month partition only for the anchor table
	    						 if(fieldValuesEntry.getValue().getVertica().getTable().getFromTable().equalsIgnoreCase(curTable)) {
	    							 query.where(subExpressionP);
	    							 query.setDefaultFilterAdded(fieldValuesEntry.getKey(), true);
	    						 }
	    						 /*if(!field.getVertica().getTable().getFromTable().equalsIgnoreCase(curTable)) {
		    						 joinTables(curTable, field.getVertica().getTable().getFromTable(), tableNames, tablesMap, query, null, null); 
		    					 }*/
	    						 // Handle the month partition custom filters here....
	    						 if(fieldValuesEntry.getValue().getVertica().getFilterSection().getFilters() != null) {
	    	    					 for(CustomFilterObject customFilterObject : fieldValuesEntry.getValue().getVertica().getFilterSection().getFilters().getCustomFilter()) {
	    	    						 if(customFilterObject != null) {
	    	    							 // The anchor table filter and/or the mapping relation filter
	    	    							 if(customFilterObject.getRelationName() != null 
	    	    									 && query.isRelationAdded(customFilterObject.getRelationName())) {
	    	    								 String subExpression = customFilterObject.getCustomFilterExpression();
	    	    								 subExpression = parseCustomFilter(subExpression, curTable, tableNames, tablesMap, query, isResultOrExport); // this is the column name for the table
	    	    								 subExpression = buildMonthPartExpression(field, submittedValue, dateFormat, subExpression, tableNames, tablesMap, query);
	    	    								 if(customFilterObject.getRelationName() != null 
	    	    										 && !query.isDefaultFilterAdded(fieldValuesEntry.getKey().concat(customFilterObject.getRelationName()))) {
	    	    									 query.where(subExpression);
	    	    									 query.setDefaultFilterAdded(fieldValuesEntry.getKey().concat(customFilterObject.getRelationName()), true);
	    	    								 }
	    	    							 }
	    	    						 }
	    	    					 }
	    						 }
	    					 }
	    					 if(returnSubExps.get(fieldValuesEntry.getKey()) == null) {
	    						 subExps = new ArrayList<String>();	    						 
	    					 } else {
	    						 subExps = returnSubExps.get(fieldValuesEntry.getKey());
	    					 }
	    					 subExps.add(subExpressionP);
			            	 returnSubExps.put(fieldValuesEntry.getKey(), subExps);	
    					 }
    					 
    					 
    				     // process all the custom filters defined with optional date for AS_OF_DATE or 
	    				 if(fieldValuesEntry.getValue().getVertica().getFilterSection().getFilters() != null
	    						 && (fieldValuesEntry.getValue().getIndexed() == null 
	    						 || (fieldValuesEntry.getValue().getIndexed() != null 
	    						 	&& fieldValuesEntry.getValue().getIndexed().getType() != FilterType.MONTHPART))) {
	    					 for(CustomFilterObject customFilterObject : fieldValuesEntry.getValue().getVertica().getFilterSection().getFilters().getCustomFilter()) {
	    						 if(customFilterObject != null) {
	    							 // The anchor table filter and/or the mapping relation filter
	    							 if((customFilterObject.getRelationName() == null 
	    									 && fieldValuesEntry.getValue().getVertica().getTable().getFromTable().equalsIgnoreCase(curTable)) 
	    									 || (customFilterObject.getRelationName() != null 
	    									 && query.isRelationAdded(customFilterObject.getRelationName()))) { // default one.... this should always be applied??
	    								String subExpression = customFilterObject.getCustomFilterExpression();
	    								if(subExpression != null && !subExpression.isEmpty()) {
	    			    	    			 subExpression = parseCustomFilter(subExpression, curTable, tableNames, tablesMap, query, isResultOrExport);	    			    	    						
	    			    	    			 String namedParameterDefaultFilter = getNamedParameter("namedParameterCustomFilter", fieldValuesEntry.getValue().getName(), "");	    			    	    			 	    			    		    		 
	    			    		    		 namedParameterDefaultFilter = cleanParameterName(namedParameterDefaultFilter).toLowerCase();
	    			    		    		 if(defaultType != null && (defaultType.equalsIgnoreCase(FilterType.DATE.value()) 
	    			    		    				 || defaultType.equalsIgnoreCase(FilterType.ASOFDATE.value()))) { // for dates
	    			    		    			 if(defaultValue == null || defaultValue.equalsIgnoreCase("sysdate")) {
	    			    		    				 long time = System.currentTimeMillis();
	    	    			    		    		 java.sql.Date date = new java.sql.Date(time);
	    	    			    		             paramMap.put(namedParameterDefaultFilter, date); 
	    			    		    			 } else {
	    			    		    				 if(defaultValue != null) {
	    			    		    					 paramMap.put(namedParameterDefaultFilter, defaultValue); 
	    			    		    				 }
	    			    		    			 }
	    			    		    		 } else {
	    			    		    			 // text
	    			    		    			 paramMap.put(namedParameterDefaultFilter, defaultValue); 
	    			    		    		 }
	    			    		             String namedParameterDefaultFilterStr = "(:" + namedParameterDefaultFilter + ")";
	    			    		             subExpression = StringUtils.replace(subExpression, "?", namedParameterDefaultFilterStr);	  
	    			    		             if(subExpression != null && !subExpression.isEmpty()) {
	    			    		            	 if(customFilterObject.getRelationName() == null && !query.isDefaultFilterAdded(fieldValuesEntry.getKey()))  {
	    			    		            		 query.where(subExpression);
	    			    		            		 query.setDefaultFilterAdded(fieldValuesEntry.getKey(), true);
	    			    		            	 }
	    			    		            	 if(customFilterObject.getRelationName() != null && !query.isDefaultFilterAdded(customFilterObject.getRelationName())) {
	    			    		            		 if(isResultOrExport) {
	    			    		            			 query.join(subExpression, customFilterObject.getRelationName());
	    			    		            		 }
	    			    		            		 query.where(subExpression);
	    			    		            		 query.setDefaultFilterAdded(customFilterObject.getRelationName(), true);
	    			    		            	 }
	    			    		            	 if(returnSubExps.get(fieldValuesEntry.getKey()) == null) {
	    				    						 subExps = new ArrayList<String>();	    						 
	    				    					 } else {
	    				    						 subExps = returnSubExps.get(fieldValuesEntry.getKey());
	    				    					 }
	    				    					 subExps.add(subExpression);
	    						            	 returnSubExps.put(fieldValuesEntry.getKey(), subExps);	    			    		            	 
	    			    		             }
	    			    	    		 }
	    							 }    							
	    						 }
	    					 }
	    				 }  
    				 }
    			// }
    		 }
		 }
    	 return returnSubExps;
    }
    
    private static String parseCustomFilter(String subExpression, String fromTable, Set<String> tableNames, Map<String, Table> tablesMap, SelectBuilder query, boolean isResultOrExport) {    	
    	if(subExpression != null && !subExpression.isEmpty()) {
    		subExpression = StringUtils.trim(subExpression);
    		subExpression = StringUtils.strip(subExpression);
    		String[] tables = StringUtils.substringsBetween(subExpression, "[", "]");
    		if(tables != null) {
	    		for(String table : tables) {
	    			joinTables(fromTable, table, tableNames, tablesMap, query, null, null, isResultOrExport);
	    		}
    		}
    		subExpression = StringUtils.replace(subExpression, "[", "");
    		subExpression = StringUtils.replace(subExpression, "]", "");
    	} 
    	return subExpression;
    }
    
    public static String buildDate(final String idolField, final FilterOperator fieldOp, String submittedValue, DateFormat dateFormat,
    		Map<String, Object> paramMap, FilterField field, Set<String> tableNames, 
			String refTable, Map<String, Table> tablesMap, SelectBuilder query, String ownerTable, String destFormat) {
        String fieldText = "";
        String[] values;
        String columnName = getColumnNameForField(field, tableNames,  tablesMap, query);	
	   	handleDefaultFilter( refTable, paramMap, tableNames, tablesMap,  query);
	   	
	   	boolean sourceDestinationFormatsDifferent = false;
	   	if(destFormat != null && !destFormat.isEmpty()) {	   		
		   	final DateFormat destDateFormat = new SimpleDateFormat(destFormat); 
		   	if(!destDateFormat.equals(dateFormat)) {
		   		sourceDestinationFormatsDifferent = true;
		   		String[] newValues = submittedValue.split(",");
   				try {
   					if(FilterOperator.PERIOD != fieldOp) {   						
	   					int index = 0;
	   					values = new String[newValues.length];
	   	                for(final String value : newValues) {
	   	                    if (dateFormat.parse(value) == null) {
	   	                        throw new IllegalArgumentException("Bad date format [" + dateFormat.toString() + "].");
	   	                    } else {
	   	                    	values[index] = destDateFormat.format(dateFormat.parse(value));
	   	                    }
	   	                 index++;
	   	                }
	   	                submittedValue = StringUtils.join(values, ",");	   	                
   					}
   					if(columnName.contains("to_char")) { // this is a date being converted to another format
			        	int beginIndex = columnName.indexOf("'");
			        	int endIndex = columnName.lastIndexOf("'");
			        	if(beginIndex > 0 && beginIndex < endIndex) {
				        	String format = columnName.substring(beginIndex + 1, endIndex);        	
				        	columnName = columnName.replace(format, destFormat);
			        	}
			        }
   	            	dateFormat = destDateFormat; // format has been converted so use this   	            	
   	            } catch (ParseException e ) {
   	                throw new IllegalArgumentException("Bad date format [" + dateFormat.toString() + "].");
   	            }
		   	}
	   	}

        if (FilterOperator.PERIOD == fieldOp) {
            values = getPeriodValues(submittedValue, dateFormat);
            

        } else {
        	
        	values = submittedValue.split(",");
        	
            if (values.length == 0) {
                return fieldText;
            }
            if (FilterOperator.BETWEEN == fieldOp && (values.length < 2 || StringUtils.isEmpty(values[0]) || StringUtils.isEmpty(values[1]))) {
                return fieldText;
            }

            try {
                for(final String value : values) {
                    if (dateFormat.parse(value) == null) {
                        throw new IllegalArgumentException("Bad date format [" + dateFormat.toString() + "].");
                    }
                }
            } catch (ParseException e ) {
                throw new IllegalArgumentException("Bad date format [" + dateFormat.toString() + "].");
            }

        }
        

        switch (fieldOp) {
            case AFTER:
            	               
                 fieldText = fieldText + "(cast(" + columnName + " as date)" +  " >= " + " ('" + values[0] + "'))";               
                break;
            case BEFORE:
            	     
                fieldText = fieldText + "(cast(" + columnName + " as date)" + " < " + " ('" + values[0] + "'))"; 
                break;
            case BETWEEN:
            case PERIOD:              
                fieldText = fieldText + "((cast(" + columnName + " as date)" + " >= " + " ('" + values[0] + "'))"; 
                fieldText = fieldText + " and (cast(" + columnName + " as date)" + " < " + " ('" + values[1] + "')))"; 
                break;
        }

        return fieldText;
    }
    
    public static String[] getPeriodValues(final String periodValue, final DateFormat dateFormat) {
        final String[] values = new String[2];

        if (StringUtils.isEmpty(periodValue)) {
            throw new IllegalArgumentException("Empty date period value.");
        }

        final DatePeriod datePeriod = DatePeriod.valueOf(periodValue.toUpperCase());

        final Calendar toDate = Calendar.getInstance();
        final Calendar fromDate = Calendar.getInstance();

        switch (datePeriod) {
            case THIS_WEEK:
                final int firstDayOfWeek = toDate.getFirstDayOfWeek();
                final int dayOfWeek = toDate.get(Calendar.DAY_OF_WEEK);
                fromDate.set(fromDate.get(Calendar.YEAR), fromDate.get(Calendar.MONTH), fromDate.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
                fromDate.roll(Calendar.DAY_OF_MONTH, firstDayOfWeek - dayOfWeek);
                break;
            case THIS_MONTH:
                fromDate.set(fromDate.get(Calendar.YEAR), fromDate.get(Calendar.MONTH), 1, 0, 0, 0);
                break;
            case THIS_YEAR:
                fromDate.set(fromDate.get(Calendar.YEAR), Calendar.JANUARY, 1, 0, 0, 0);
                break;
            case LAST_YEAR_TODATE:
                fromDate.set(fromDate.get(Calendar.YEAR) - 1, Calendar.JANUARY, 1, 0, 0, 0);
                break;
            case LAST_2_YEARS_TODATE:
                fromDate.set(fromDate.get(Calendar.YEAR) - 2, Calendar.JANUARY, 1, 0, 0, 0);
                break;
            case LAST_4_YEARS_TODATE:
                fromDate.set(fromDate.get(Calendar.YEAR) - 4, Calendar.JANUARY, 1, 0, 0, 0);
                break;

        }

        values[0] = dateFormat.format(fromDate.getTime());
        values[1] = dateFormat.format(toDate.getTime());

        return values;
    } 
    
    public static String buildDocumentFolder(final FilterOperator fieldOp, final Set<String> fieldValue,
    		Map<String, Object> paramMap, FilterField field, Set<String> tableNames, 
			String refTable, Map<String, Table> tablesMap, SelectBuilder query, String ownerTable, boolean isAND, String folderId) {
        // we only support IS and IS_NOT, but might as well reuse the enum
    	
    	String fieldText = null;	       
		String columnName = getColumnNameForField(field, tableNames, tablesMap, query);
		String namedParameter = field.getVertica().getColumn() + "doc" + folderId;
		namedParameter = cleanParameterName(namedParameter);
		if (fieldValue.size()>0) {				
			paramMap.put(namedParameter, fieldValue);    
	  
	        //System.out.println(" Parameters size = " + fieldValue.size());
	        //doJoinTables(field,  refTable, tableNames,  tablesMap, query, ownerTable, paramMap);
	        switch (fieldOp) {
	            case IS:
	               fieldText = "(" + columnName + " " + " in (:" + namedParameter + "))";
	               break;
	            case IS_NOT:
	            	if(isAND) {
	            		fieldText = "(" + columnName + " " + " in (:" + namedParameter + "))";
	            	} else {
	            		fieldText = "(" + columnName + " " + " not in (:" + namedParameter + "))";
	            	}
	            	break;
	        }
		} else {
			fieldText="(false)";
		}
		return fieldText;
    }
    
    
    public static String buildDocumentFolderForFolderStoredInVertica(final FilterOperator fieldOp, final Integer fieldValue,
    		Map<String, Object> paramMap, boolean isAND, String folderId, String documentFolderTable, String documentFolderIDColumn) {
        // we only support IS and IS_NOT, but might as well reuse the enum
    	
    	String fieldText = null;	       
		String columnName = documentFolderTable + "." + documentFolderIDColumn;
		String namedParameter = documentFolderIDColumn + "doc" + folderId;
		namedParameter = cleanParameterName(namedParameter);
		if (fieldValue > 0) {				
			paramMap.put(namedParameter, fieldValue);    
	  
	        
	        //doJoinTables(field,  refTable, tableNames,  tablesMap, query, ownerTable, paramMap);
	        switch (fieldOp) {
	            case IS:
	               fieldText = "(" + columnName + " " + " = (:" + namedParameter + "))";
	               break;
	            case IS_NOT:
	            	if(isAND) {
	            		fieldText = "(" + columnName + " " + " = (:" + namedParameter + "))";
	            	} else {
	            		fieldText = "(" + columnName + " " + " <> (:" + namedParameter + "))";
	            	}
	            	break;
	        }
		} else {
			fieldText="(false)";
		}
		return fieldText;
    }
    public static Float getRangeNumber(final String value) {
        try {
            return Float.valueOf(value);

        } catch (final Exception e) {
            return null;
        }
    }
    
    public static int getWhenLevel(final String whenop) {
        int level = 0;

        if (whenop != null) {
            final String num = whenop.substring(4);
            if (StringUtils.isNotEmpty(num)) {
                level = Integer.valueOf(num);
            }
        }

        return level;
    }
    
    public static String getColumnNameForField(FilterField field, Set<String> tableNames, Map<String, Table> tablesMap, SelectBuilder query) {
		
    	if(field.getVertica().getSelect() != null && field.getVertica().getSelect().getSelectOperator() != null 
    			&& field.getVertica().getSelect().getSelectOperator().getSelectExpression() != null && 
    			!field.getVertica().getSelect().getSelectOperator().getSelectExpression().isEmpty()) {
    		return field.getVertica().getSelect().getSelectOperator().getSelectExpression();
    	}
    	String columnTableName = "";
		if(field.getVertica().getTable().getDimensionTable() != null && !field.getVertica().getTable().getDimensionTable().isEmpty() ) {
			columnTableName = field.getVertica().getTable().getDimensionTable();
		} else {
			columnTableName = field.getVertica().getTable().getFromTable();
		}
		String columnName = "";		
		String [] columns = field.getVertica().getColumn().split(",");
		StringBuffer buff = new StringBuffer();
		int i = 1;
		boolean func = false;
		for(String column : columns) {
			if(i == 1) {
				if(field.getVertica().getSelect().getSelectOperator().getMiscOperator() != null) {
					if(!field.getVertica().getSelect().getSelectOperator().getMiscOperator().isEmpty()) {
						buff.append(field.getVertica().getSelect().getSelectOperator().getMiscOperator());
						buff.append("(");
						func = true;
					}					
				}				
			}
			if(column.contains(".")) { // already has a table definition tablename.column
				String [] columnDefs = column.split("\\.");
				if(columnDefs.length == 2) {
					if(i==1 && columns.length > 1 && func && field.getVertica().getSelect().getSelectOperator().getMiscOperator().equalsIgnoreCase("age_in_years")) {
						buff.append("coalesce").append("(").append(columnDefs[0]).append(".").append(columnDefs[1].trim()).append(",").append(" sysdate ").append(")");					
					} else {
						buff.append(columnDefs[0]);
						buff.append(".");
						buff.append(columnDefs[1].trim());
					}
					joinTables( columnTableName, columnDefs[0], tableNames, tablesMap, query, null, null, false); 
				}
			} else {
				if(i==1 && columns.length > 1 && func && field.getVertica().getSelect().getSelectOperator().getMiscOperator().equalsIgnoreCase("age_in_years")) {
					buff.append("coalesce").append("(").append(columnTableName).append(".").append(column.trim()).append(",").append(" sysdate").append(")");					
				} else {
					buff.append(columnTableName);
					buff.append(".");
					buff.append(column.trim());
				}				
			}
			if(columns.length > 1 && columns.length != i) {
				buff.append(",");
			}
			if(columns.length == i && func) {
				buff.append(") ");
			}
			//func = false;
			i++;									
		}
		columnName = buff.toString();
		
		
		return columnName;
	}
    
    public static String getColumnNameForField(String dimensionTable, String fromTable, String tableColumn, String miscOperator, String selectExpression) {
    	if(selectExpression != null && 
    			!selectExpression.isEmpty()) {
    		return selectExpression;
    	}		
    	String columnName = "";
    	String columnTableName = "";
    	if(dimensionTable != null && !dimensionTable.isEmpty() ) {
			columnTableName = dimensionTable;
		} else {
			columnTableName = fromTable;
		}    	
		if(tableColumn != null && !tableColumn.isEmpty()) {
			String [] columns = tableColumn.split(",");
			StringBuffer buff = new StringBuffer();
			int i = 1;			
			boolean func = false;
			for(String column : columns) {
				if(i == 1) {
					if(miscOperator != null) {
						if(!miscOperator.isEmpty()) {							
							buff.append(miscOperator);
							buff.append("(");
							func = true;														
						}					
					}				
				}
				if(column.contains(".")) { // already has a table definition tablename.column
					String [] columnDefs = column.split("\\.");
					if(columnDefs.length == 2) {						
						buff.append(columnDefs[0]);
						buff.append(".");
						buff.append(columnDefs[1].trim());					
					}
				} else {
					buff.append(columnTableName);
					buff.append(".");
					buff.append(column.trim());				
				}
				if(columns.length > 1 && columns.length != i) {
					buff.append(",");
				}
				if(columns.length == i && func) {				
					buff.append(") "); 
				}
				i++;									
			}
			columnName = buff.toString();
		}
		return columnName;
	}
    
    public static String getTableNameForMultipleColumn(FilterField field) {
		// TODO remove the hard codings
    	String columnTableName = "";
		String [] columns = field.getVertica().getColumn().split(",");
		StringBuffer buff = new StringBuffer();
		int i = 1;
		boolean func = false;
		for(String column : columns) {
			if(column.contains(".")) { // already has a table definition tablename.column
				String [] columnDefs = column.split("\\.");
				if(columnDefs.length == 2) {
					columnTableName = columnDefs[0];
				}
			}									
		}		
		return columnTableName;
	}
    
    public static String getTableNameForMultipleColumn(String tableColumn) {
		// TODO remove the hard codings
    	String columnTableName = "";
		String [] columns = tableColumn.split(",");
		StringBuffer buff = new StringBuffer();
		int i = 1;
		boolean func = false;
		for(String column : columns) {
			if(column.contains(".")) { // already has a table definition tablename.column
				String [] columnDefs = column.split("\\.");
				if(columnDefs.length == 2) {
					columnTableName = columnDefs[0];
				}
			}									
		}		
		return columnTableName;
	}
    
    public static Relation getMappingRelation(String fromTable, String toTable, Map<String, Table> tablesMap, String relationName) {
	    String relationKey = fromTable + tableRelationExpression  + toTable;
	    if(relationName != null && !relationName.isEmpty()) {
	    	relationKey = relationName;
	    }
	    if(tablesMap.get(fromTable) != null) {
			for(Relation relation : tablesMap.get(fromTable).getJoinRelation()) {
				if(relation != null && relation.getRelationName().equalsIgnoreCase(relationKey)) {
					return relation;
				}
			}
	    }
		return null;	        
	}
    
    public static TableRelationObject getTableRelationByTableName(String fromTable, Map<String, List<TableRelationObject>> relationMap, String searchView) {
	    
	    if(relationMap.get(searchView) != null) {
	    	for( TableRelationObject relation : relationMap.get(searchView)) {
				 if(relation != null && relation.getTable().equalsIgnoreCase(fromTable)) {
					 return relation;
				 }				 
			 }			 
	    }
		return null;	        
	}
    
  public static int getMaxJoinLevel(Map<String, List<TableRelationObject>> relationMap, String searchView) {
	    
	  int maxJoinNum=0; 
	  if(relationMap.get(searchView) != null) {
	    	for( TableRelationObject relation : relationMap.get(searchView)) {
				 if(relation != null && relation.getJoinNumber() > maxJoinNum) {
					 maxJoinNum=relation.getJoinNumber();
				 }				 
			 }			 
	    }
		return maxJoinNum;	        
	}
    
 public static TableRelationObject getTableRelationByJoinLevel(int joinLevel, Map<String, List<TableRelationObject>> relationMap, String searchView) {
	    
	    if(relationMap.get(searchView) != null) {
	    	for( TableRelationObject relation : relationMap.get(searchView)) {
				 if(relation != null && relation.getJoinNumber() == joinLevel) {
					 return relation;
				 }				 
			 }			 
	    }
		return null;	        
	}
    
    public static void doJoinTables(FilterField field, String refTable, Set<String> tableNames, Map<String, Table> tablesMap, SelectBuilder query, String ownerTable, 
    		 Map<String, Object> paramMap) {
    	String fromTable = field.getVertica().getTable().getFromTable();
    	
    	handleDefaultFilter( fromTable, paramMap, tableNames, tablesMap,  query);
    	
		// join the from table	
    	if(ownerTable != null && !ownerTable.equalsIgnoreCase(fromTable)) {
    		joinTables(ownerTable, refTable, tableNames, tablesMap, query, null, null, false);
    	   	handleDefaultFilter( refTable, paramMap, tableNames, tablesMap,  query);
    	}
    	joinTables(refTable, fromTable, tableNames, tablesMap, query, null, null, false);
	   	handleDefaultFilter( refTable, paramMap, tableNames, tablesMap,  query);
		String dimTable = field.getVertica().getTable().getDimensionTable();		
		joinTables(fromTable, dimTable, tableNames, tablesMap, query, field.getVertica().getTable().getMappingKey(), null, false);
	   	handleDefaultFilter( dimTable, paramMap, tableNames, tablesMap,  query);
	   	joinTables(fromTable, QueryBuilderNew.getTableNameForMultipleColumn(field), tableNames, tablesMap, query, null, null, false);
    }
    
    private static void doJoinTables(String fromTable, String dimTable, String mappingKey, String refTable, Set<String> tableNames, Map<String, Table> tablesMap, SelectBuilder query, String ownerTable, 
   		 Map<String, Object> paramMap) {
   	//String fromTable = field.getVertica().getTable().getFromTable();
   	
   	handleDefaultFilter( fromTable, paramMap, tableNames, tablesMap,  query);
   	  
		// join the from table	
   	if(ownerTable != null && !ownerTable.equalsIgnoreCase(fromTable)) {
   		joinTables(ownerTable, refTable, tableNames, tablesMap, query, null, null, false);
   	   	handleDefaultFilter( refTable, paramMap, tableNames, tablesMap,  query);
   	}
   	joinTables(refTable, fromTable, tableNames, tablesMap, query, null, null, false);
	   	handleDefaultFilter( refTable, paramMap, tableNames, tablesMap,  query);
		//String dimTable = field.getVertica().getTable().getDimensionTable();		
		joinTables(fromTable, dimTable, tableNames, tablesMap, query, mappingKey, null, false);
	   	handleDefaultFilter( dimTable, paramMap, tableNames, tablesMap,  query);
	   //	QueryBuilder.joinTables(fromTable, QueryBuilder.getTableNameForMultipleColumn(field), tableNames, tablesMap, query, null);
   }
    
    public static void joinTables(String fromTable, String toTable, Set<String> tableNames, Map<String, Table> tablesMap, SelectBuilder query, 
    		String relationKey, JoinType override, boolean isResultsOrExport) {
    	if(fromTable != null && toTable != null && !fromTable.equalsIgnoreCase(toTable) && !tableNames.contains(toTable)) {
    		Relation relation = getMappingRelation(fromTable, toTable, tablesMap, relationKey);    		
			if(relation != null) {
				LOGGER.debug("Found relation " + relationKey);
				// Add the parent relation first
				if(StringUtils.isNotEmpty(relation.getParentRelationName())) {
					Relation parentRelation = getMappingRelation(fromTable, toTable, tablesMap, relation.getParentRelationName());
					if(parentRelation != null) {
						LOGGER.debug("Found parent relation " + parentRelation.getRelationName());
						joinTables(parentRelation.getFromTableName(), parentRelation.getToTableName(), tableNames, tablesMap, query, parentRelation.getRelationName(), override, isResultsOrExport);
					}
				}
				if(relation.getJoinExpression() != null && !relation.getJoinExpression().isEmpty()) {
					String exp = relation.getJoinExpression();
					if(override != null) {
						exp = exp.replaceAll("(?i)inner join", override.getJoinTypeString().toLowerCase());
					}
					if(isResultsOrExport) {
						query.join(exp, relation.getRelationName());
					} else {
						query.join(exp);
					}
					query.addRelationMapping(relation.getRelationName());
				} else {
					
					query.join(override != null ? override : JoinType.getByName(relation.getRelationType().toUpperCase()), relation.getFromTableName(), relation.getToTableName(), 
							relation.getFromColumnName(), relation.getToColumnName(), isResultsOrExport ? relation.getRelationName() : null, null);
					query.addRelationMapping(relation.getRelationName());
				}
				tableNames.add(toTable);
				
			}
		}
    }
    
    public static String buildCaseWhenClauseForGroupField(FilterField field, SearchConfig config, Set<String> tableNames, Map<String, Table> tablesMap, SelectBuilder query) {
		StringBuffer caseWhen = new StringBuffer();
		boolean firstIter = true;			
		if(field != null) {
			Group group = config.getGroupFields().get(field.getName());
			String columnName = getColumnNameForField(field, tableNames, tablesMap, query);
			//configured val
			if(group != null && group.getGroupType().equalsIgnoreCase("range")) { // RANGE (Groups)
				if(!group.getGroups().isEmpty()) {
					int loop = 0;
					for(GroupingInfo info : group.getGroups()) {
						loop++;							
						boolean foundMaxOrMin = false;
						if(info != null) {
							if(firstIter) {
								
								caseWhen.append(" case ");
								
							}
							
							caseWhen.append(" when ");
							
							if(info.getMaxVal() != null && !info.getMaxVal().isEmpty()) {
								if(foundMaxOrMin) {
									
									caseWhen.append(" and ");
									
								}
								caseWhen.append(" ");
								caseWhen.append(columnName).append(" < ").append(info.getMaxVal());
								caseWhen.append(" ");
								foundMaxOrMin = true;
							}
							if(info.getMinVal() != null && !info.getMinVal().isEmpty()) {
								if(foundMaxOrMin) {									
									caseWhen.append(" and ");									
								}
								caseWhen.append(" ");
								caseWhen.append(columnName).append(" >= ").append(info.getMinVal());									
								caseWhen.append(" ");
								foundMaxOrMin = true;
							}
							if(foundMaxOrMin) {
								caseWhen.append(" then '").append(info.getGroupLabel()).append("' ");
							}								
							firstIter = false;
							if(group.getGroups().size() == loop) {
								// end
								caseWhen.append(" end ");
							}
						}
					}
				}
			} else if(group != null && field != null && group.getGroupType().equalsIgnoreCase("NullCheck")) {  // NULL Check
				if(!group.getGroups().isEmpty()) {
					int loop = 0;
					for(GroupingInfo info : group.getGroups()) {
						loop++;							
						boolean foundNullOrNotNull = false;
						if(info != null) {
							if(firstIter) {
								caseWhen.append(" case ");
							}							
							caseWhen.append(" when ");							
							if(info.isHasNull()) { // Null
								if(foundNullOrNotNull) {									
									caseWhen.append(" and ");									
								}
								caseWhen.append(" ");
								caseWhen.append(columnName).append(" is null ");
								caseWhen.append(" ");
								foundNullOrNotNull = true;
							}
							if(!info.isHasNull()) { // Not null
								if(foundNullOrNotNull) {									
									caseWhen.append(" and ");									
								}
								caseWhen.append(" ");
								caseWhen.append(columnName).append(" is not null ");									
								caseWhen.append(" ");
								foundNullOrNotNull = true;
							}
							if(foundNullOrNotNull) {
								caseWhen.append(" then '").append(info.getGroupLabel()).append("' ");
							}								
							firstIter = false;
							if(group.getGroups().size() == loop) {
								// end
								caseWhen.append(" end ");
							}
						}
					}
				}
			}
			
		}
		return caseWhen.toString();
	}	
    
    public static boolean isRangeField(FilterField field, SearchConfig config) {
		if(field != null) {
			Group group = config.getGroupFields().get(field.getName());
			//configured val
			if(group != null && field != null && group.getGroupType().equalsIgnoreCase("range")) {
				if(!group.getGroups().isEmpty()) {
					
					return true;
					
				}
				
			}
			
		}
		return false;
		
	}
    
    public static boolean isGroupField(FilterField field, SearchConfig config) {
  		if(field != null) {
  			Group group = config.getGroupFields().get(field.getName());
  			//configured val
  			if(group != null && field != null && (group.getGroupType().equalsIgnoreCase("range") || group.getGroupType().equalsIgnoreCase("NullCheck"))) {
  				if(!group.getGroups().isEmpty()) {
  					
  					return true;
  					
  				}
  				
  			}
  			
  		}
  		return false;
  		
  	}
    
    public static GroupingInfo getRangeInfo(String fieldName, SearchConfig config, String groupLabel) {
    	Group group = config.getGroupFields().get(fieldName);
		//configured val
		if(group != null && group.getGroupType().equalsIgnoreCase("range")) {
			if(!group.getGroups().isEmpty()) {
				for(GroupingInfo info : group.getGroups()) {
					if(info != null && info.getGroupLabel() != null && info.getGroupLabel().equalsIgnoreCase(groupLabel)) {
						return info;
					}
				}
			}
		}
		return null;
    }
    
    public static String cleanParameterName(String paramName) {
    	String cleaned = StringUtils.replace(paramName, ",", "").trim();
    	cleaned = StringUtils.replace(cleaned, "/", "").trim();
    	cleaned = cleaned.replaceAll("[^A-Za-z0-9]", "");
    	return cleaned;    	
    }

    public static String getNamedParameter(String columnName, String fieldName, String postFix) {
    	StringBuffer buff = new StringBuffer(columnName);
    	buff.append(fieldName);
    	buff.append(postFix);
    	return buff.toString();
    }
    
    private static String cleanFilterValues(String value) {
    	 return StringUtils.replace(value, "'", "''");    	
    }
    
    public static List<FilterField> getMandatoryFilterField(final Map<String, FilterField> filterFieldNames) {
    	List<FilterField> mandatoryFilterFields = new ArrayList<FilterField>();
    	 for(final Map.Entry<String, FilterField> fieldValuesEntry : filterFieldNames.entrySet()) {    		
    		 if(isMandatoryFilterField(fieldValuesEntry.getValue())) {
    			 mandatoryFilterFields.add(fieldValuesEntry.getValue());
    		 }
    	 }
    	 return mandatoryFilterFields;
    }
    
    public static boolean isMandatoryFilterField(FilterField filterField) {
   	 if(filterField != null   			
   		&& filterField.getVertica() != null
   		&& filterField.getVertica().getFilterSection() != null
   		&& filterField.getVertica().getFilterSection().isMandatory()) {
   		return true;   		 
   	 }
   	 return false;
   }
    
public static String getActualColumnNameForField(FilterField field, Set<String> tableNames, Map<String, Table> tablesMap, SelectBuilder query) {
		
    	/*if(field.getVertica().getSelect() != null && field.getVertica().getSelect().getSelectOperator() != null 
    			&& field.getVertica().getSelect().getSelectOperator().getSelectExpression() != null && 
    			!field.getVertica().getSelect().getSelectOperator().getSelectExpression().isEmpty()) {
    		return field.getVertica().getSelect().getSelectOperator().getSelectExpression();
    	}*/
    	String columnTableName = "";
		if(field.getVertica().getTable().getDimensionTable() != null && !field.getVertica().getTable().getDimensionTable().isEmpty() ) {
			columnTableName = field.getVertica().getTable().getDimensionTable();
		} else {
			columnTableName = field.getVertica().getTable().getFromTable();
		}
		String columnName = "";		
		String [] columns = field.getVertica().getColumn().split(",");
		StringBuffer buff = new StringBuffer();
		int i = 1;
		boolean func = false;
		for(String column : columns) {
			if(i == 1) {
				if(field.getVertica().getSelect().getSelectOperator().getMiscOperator() != null) {
					if(!field.getVertica().getSelect().getSelectOperator().getMiscOperator().isEmpty()) {
						buff.append(field.getVertica().getSelect().getSelectOperator().getMiscOperator());
						buff.append("(");
						func = true;
					}					
				}				
			}
			if(column.contains(".")) { // already has a table definition tablename.column
				String [] columnDefs = column.split("\\.");
				if(columnDefs.length == 2) {
					if(i==1 && columns.length > 1 && func && field.getVertica().getSelect().getSelectOperator().getMiscOperator().equalsIgnoreCase("age_in_years")) {
						buff.append("coalesce").append("(").append(columnDefs[0]).append(".").append(columnDefs[1].trim()).append(",").append(" sysdate ").append(")");					
					} else {
						buff.append(columnDefs[0]);
						buff.append(".");
						buff.append(columnDefs[1].trim());
					}
					joinTables( columnTableName, columnDefs[0], tableNames, tablesMap, query, null, null, false); 
				}
			} else {
				if(i==1 && columns.length > 1 && func && field.getVertica().getSelect().getSelectOperator().getMiscOperator().equalsIgnoreCase("age_in_years")) {
					buff.append("coalesce").append("(").append(columnTableName).append(".").append(column.trim()).append(",").append(" sysdate").append(")");					
				} else {
					buff.append(columnTableName);
					buff.append(".");
					buff.append(column.trim());
				}				
			}
			if(columns.length > 1 && columns.length != i) {
				buff.append(",");
			}
			if(columns.length == i && func) {
				buff.append(") ");
			}
			//func = false;
			i++;									
		}
		columnName = buff.toString();
		
		
		return columnName;
	}

	private static StringBuffer createExpressionForListInOperator(List<String> fieldList) {
		StringBuffer listValue = new StringBuffer();
		Iterator<String> iter = fieldList.iterator(); 
   	   	while(iter.hasNext()) {
   	   		String fieldValueIter = iter.next();
   	   		fieldValueIter = cleanFilterValues(fieldValueIter);
   	   		listValue.append("'").append(fieldValueIter).append("'");
   	   		if(iter.hasNext()) {
   	   			listValue.append(",");
   	   		}   		
   	   	}
   	   	return listValue;
	}
	private static String createExpressionForListContainsOperator(List<String> fieldList, String columnName) {
		StringBuffer fieldTextTemp = new StringBuffer();
		Iterator<String> iter = fieldList.iterator();
		if(iter.hasNext()) {
			fieldTextTemp.append("(");
		}
   	   	while(iter.hasNext()) {
   	   		String value = iter.next();
       		if(StringUtils.isNotBlank(value)) {       			
       			fieldTextTemp.append("(").append(columnName).append(" ");
       			fieldTextTemp.append(" ilike ('%").append(cleanFilterValues(value)).append("%'))");       			   			
       			if(iter.hasNext()) {
       				fieldTextTemp.append(" or ");
       			}  else {
       	   			fieldTextTemp.append(")");
       	   		}
       		}
       	}
   	   	return fieldTextTemp.toString();
	}
    
}
	