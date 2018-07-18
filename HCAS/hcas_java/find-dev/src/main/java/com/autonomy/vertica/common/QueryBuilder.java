package com.autonomy.vertica.common;

// import java.sql.Date;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.autonomy.aci.content.fieldtext.EQUAL;
import com.autonomy.aci.content.fieldtext.FieldText;
import com.autonomy.aci.content.fieldtext.GREATER;
import com.autonomy.aci.content.fieldtext.LESS;
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

public class QueryBuilder {
	
	private static String tableRelationExpression = "To";
	
	public static String buildFilterExpression(
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
	            String destDateFormat
	            ) {
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
	     
	     StringBuffer childFiltersText = null;
	     
	     final Map<String, List<FieldParams>> fieldValuesList = filterGroup.getFilterFields();
	        if (fieldValuesList != null) {
	        	for(final Map.Entry<String, List<FieldParams>> fieldValuesEntry : fieldValuesList.entrySet()) {
	        		
	        		final Map<FieldTypeNamePair, List<FieldParams>> typeFieldsMap = new HashMap<FieldTypeNamePair, List<FieldParams>>();
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
	                
	                for (final Map.Entry<FieldTypeNamePair, List<FieldParams>> typeFieldEntry : typeFieldsMap.entrySet()) {
	                	final StringBuffer filterFieldText = filterFieldTextFromValue(documentFolderService, documentVerticaFolderService, typeFieldEntry.getKey(), details, typeFieldEntry.getValue(), 
	                    									isAND, paramMap, listTables, refTable, tablesMap, query, config, fromTable, searchView, filterFieldNames, destDateFormat);
	                    if (filterFieldText != null) {
	                        if (!isWHEN) {
	                            childFiltersText = (childFiltersText == null) ? new StringBuffer(filterFieldText) : (isAND ? childFiltersText.append(" and ").append(filterFieldText) : childFiltersText.append(" or ").append(filterFieldText));
	                        } else {
	                            if(childFiltersText == null) {
	                                childFiltersText = new StringBuffer(filterFieldText);
	                            } else {
	                               // final int level = getWhenLevel(filterGroup.getWhenOp());
	                              // childFiltersText = level == 0 ? childFiltersText.WHEN(filterFieldText) : childFiltersText.WHEN(level, filterFieldText);
	                            	childFiltersText = childFiltersText.append(" and ").append(filterFieldText);
	                            }
	                        }
	                    }

	                }      
	                
	        	}
	        }
	        StringBuffer childGroupText = null;
	        if (filterGroup.getChildGroups() != null) {
	            for (final FilterGroup childGroup : filterGroup.getChildGroups()) {
	                if (!filterCohort || childGroup.getCohortOp() == null)  {
	                    // Only support cohort at the first child level
	                    final String groupFieldText = buildFilterExpression(documentFolderService, documentVerticaFolderService, childGroup, filterFieldNames, false, paramMap, listTables,refTable, 
	                    		tablesMap, query, config, fromTable, searchView, destDateFormat);
	                    if (groupFieldText != null) {
	                        childGroupText = (childGroupText == null) ? new StringBuffer(groupFieldText) : (isAND ? childGroupText.append(" and ").append(groupFieldText) : childGroupText.append(" or ").append(groupFieldText));
	                    }
	                }
	            }
	        }

	        if (childFiltersText != null) {
	            fieldText = (childGroupText == null) ? new StringBuffer(childFiltersText) : (isAND ? childFiltersText.append(" and ").append(childGroupText) : childFiltersText.append(" or ").append(childGroupText));

	        }  else {
	            fieldText = childGroupText;
	        }

	        /*if (isNOT && fieldText != null) {
	            fieldText = fieldText.NOT();
	        }*/
	        if(fieldText != null) {
	        	if (isNOT) {
	        		result = "(NOT ("+fieldText.toString()+"))";	        		
	        	} else {
	        		result = "("+fieldText.toString()+")";
	        	}
	        }

	       return result;
		 
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
	             String destDateFormat
	    ) throws IllegalArgumentException {

	        StringBuffer fieldText = null;

	        if (valuesList.size() == 0) {
	            return null; //  unit
	        }
	        
	        switch (typeNamePair.getType()) {
	        	case HASFILTER:
	        		for (final FieldParams entry : valuesList ) {
	                    if (StringUtils.isNotEmpty(entry.getVal())) {
	                        final FilterOperator fieldOp = entry.getOp();
	                        final String textFieldText = buildHasFilter(typeNamePair.getName(), fieldOp, entry.getVal(), isAND, paramMap, details, listTables, refTable, tablesMap, query, ownerTable);
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
	            case TEXT:
	            	if (isRangeField(details, config)) {
	            		final FieldTypeNamePair rangePair = new FieldTypeNamePair(FilterType.PARARANGES, typeNamePair.getName());
	            		return  filterFieldTextFromValue(documentFolderService, documentVerticaFolderService, rangePair, details, valuesList, isAND, paramMap, listTables, refTable, tablesMap, 
	            				query, config, ownerTable, searchView, filterFieldNames, destDateFormat);
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
	                    		query, config, ownerTable, searchView, filterFieldNames, destDateFormat);
	                }

	                for (final FieldParams entry : valuesList ) {
	                    if (StringUtils.isNotEmpty(entry.getVal())) {
	                        final FilterOperator fieldOp = entry.getOp();
	                        final String textFieldText = buildText(typeNamePair.getName(), fieldOp, entry.getVal(), isAND, paramMap, details, listTables, refTable, tablesMap, query, ownerTable);
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
	            				query, config, ownerTable, searchView, filterFieldNames, destDateFormat);
                    }
	            	 for (final FieldParams entry : valuesList ) {
		                    if (StringUtils.isNotEmpty(entry.getVal())) {
		                        final FilterOperator fieldOp = entry.getOp();
		                        final String textFieldText = buildMatch(typeNamePair.getName(), fieldOp, entry.getVal(), isAND, paramMap, details, listTables, refTable,
		                        		tablesMap, query, ownerTable);
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
	                            final Double fieldValue = CollUtils.stringsToDoubles(Collections.singletonList(entry.getVal())).get(0);
	                            numericFieldText = buildNumeric(typeNamePair.getName(), fieldOp, fieldValue, config, paramMap, details, listTables, 
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
	                        // TODO: how do you want to handle deleted document folders?
	                        final FilterOperator fieldOp = entry.getOp();
	                        // the filter field needs to be extracted from the folder table
	                        //DocumentVerticaFolderService documentVerticaFolderService = new DocumentVerticaFolderService();						
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
	                       }
	                    }
	                }
	                break;
	            default:
	                throw new IllegalArgumentException(String.format("Type not recognised: `%s`", typeNamePair.getType()));
	        }


	        return fieldText;

	    }
	 public static String buildHasFilter(final String fieldName, final FilterOperator fieldOp, final String fieldValue, final boolean isAnd, 
				Map<String, Object> paramMap, FilterField field, Set<String> tableNames, 
				String refTable, Map<String, Table> tablesMap, SelectBuilder query, String fromTable) {
		 //boolean paramExists = false;
		String fieldText = null;	       
		String columnName = getColumnNameForField(field);			
		String namedParameter = getNamedParameter(field.getVertica().getColumn(), field.getName(), fieldOp.toString());
		namedParameter = cleanParameterName(namedParameter);		
		
		doJoinTables(field,  refTable, tableNames,  tablesMap, query, fromTable, paramMap);			
		handleDefaultFilter( refTable, paramMap, tableNames, tablesMap,  query);
		if (!fieldValue.equals("[No Value]")) {
			switch (fieldOp) {
				case IS:
					if (StringUtils.contains(fieldValue.toLowerCase(), "y")) { // treat as a not null
						fieldText = "(" + columnName + " IS NOT NULL)";
	                 } else if (StringUtils.contains(fieldValue.toLowerCase(), "n")) {
	                	fieldText = "(" + columnName + " IS NULL)";
	                 }					
					break;	
			}
		} else {
			switch (fieldOp) {
				case IS:
					fieldText = "(" + columnName + " IS NULL)";
					break;
		
			}
		}

		return fieldText;
	 }
	 public static String buildText(final String fieldName, final FilterOperator fieldOp, String fieldValue, final boolean isAnd, 
			 						Map<String, Object> paramMap, FilterField field, Set<String> tableNames, 
			 						String refTable, Map<String, Table> tablesMap, SelectBuilder query, String fromTable) {
	       	//boolean paramExists = false;
		 	String fieldText = null;	       
			String columnName = getColumnNameForField(field);			
			String namedParameter = getNamedParameter(field.getVertica().getColumn(), field.getName(), fieldOp.toString());
			namedParameter = cleanParameterName(namedParameter);
			
            doJoinTables(field,  refTable, tableNames,  tablesMap, query, fromTable, paramMap);			
    	   	handleDefaultFilter( refTable, paramMap, tableNames, tablesMap,  query);
    	   	fieldValue = cleanFilterValues(fieldValue);
          	if (!fieldValue.equals("[No Value]")) {
		        switch (fieldOp) {
		            case IS:
		            	/*if(!paramExists) {
		            		fieldText = "(" + columnName + " " + " in (:" + namedParameter + "))";
		            	}
		                fieldList.add(fieldValue);*/
		            	namedParameter = namedParameter + cleanParameterName(fieldValue);
		            	fieldText = "(" + columnName + " " + " in (:" + namedParameter + "))";		            	
		            	paramMap.put(namedParameter, fieldValue);
		                break;
		            case IS_NOT:
		            	/*if(!paramExists) {
		            		fieldText = "(" + columnName + " " + " not in (:" + namedParameter + "))";
		            	}
		                fieldList.add(fieldValue);*/
		                namedParameter = namedParameter + cleanParameterName(fieldValue);
		                fieldText = "(" + columnName + " " + " not in (:" + namedParameter + "))";		            	
		            	paramMap.put(namedParameter, fieldValue);
		                break;
		            case CONTAINS:
		            	namedParameter = namedParameter + cleanParameterName(fieldValue);
		            	fieldText = "(" + columnName + " " + " ilike (:" + namedParameter + "))";
		            	/*fieldList.clear();
		            	fieldList.add("%" + fieldValue + "%");*/
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
	 
	 public static String buildTextForAutoComplete(final String fieldName, final FilterOperator fieldOp, final String fieldValue, final boolean isAnd, 
				Map<String, Object> paramMap, FilterField field, Set<String> tableNames, 
				String refTable, Map<String, Table> tablesMap, SelectBuilder query, String fromTable) {
		//boolean paramExists = false;
		String fieldText = null;	       
		String columnName = getColumnNameForField(field);			
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
					/*if(!paramExists) {
						fieldText = "(" + columnName + " " + " not in (:" + namedParameter + "))";
					}
				 fieldList.add(fieldValue);*/
				 namedParameter = namedParameter + cleanParameterName(fieldValue);
				 fieldText = "(" + columnName + " " + " not in (:" + namedParameter + "))";		            	
					paramMap.put(namedParameter, fieldValue);
				 break;
				case CONTAINS:
					namedParameter = namedParameter + cleanParameterName(fieldValue);
					fieldText = "(" + columnName + " " + " ilike (:" + namedParameter + "))";
					/*fieldList.clear();
					fieldList.add("%" + fieldValue + "%");*/
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
	 
	 public static String buildTextForAutoCompleteNew(final String fieldName, final FilterOperator fieldOp, final String fieldValue, final boolean isAnd, 
				Map<String, Object> paramMap, FilterField field, Set<String> tableNames, 
				String refTable, Map<String, Table> tablesMap, SelectBuilder query, String fromTable, String view) {
		//boolean paramExists = false;
		String fieldText = null;	       
		String columnName = getColumnNameForField(field);			
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
					/*if(!paramExists) {
						fieldText = "(" + columnName + " " + " not in (:" + namedParameter + "))";
					}
				 fieldList.add(fieldValue);*/
				 namedParameter = namedParameter + cleanParameterName(fieldValue);
				 fieldText = "(" + columnName + " " + " not in (:" + namedParameter + "))";		            	
					paramMap.put(namedParameter, fieldValue);
				 break;
				case CONTAINS:
					namedParameter = namedParameter + cleanParameterName(fieldValue);
					fieldText = "(" + columnName + " " + " ilike (:" + namedParameter + "))";
					/*fieldList.clear();
					fieldList.add("%" + fieldValue + "%");*/
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
				String refTable, Map<String, Table> tablesMap, SelectBuilder query, String ownerTable) {
        
			//boolean paramExists = false;
		 	String fieldText = null;	       
			String columnName = getColumnNameForField(field);			
			String namedParameter = getNamedParameter(field.getVertica().getColumn(), field.getName(), fieldOp.toString());
			namedParameter = cleanParameterName(namedParameter);
            doJoinTables(field,  refTable, tableNames,  tablesMap, query, ownerTable, paramMap);			
    	   	handleDefaultFilter( refTable, paramMap, tableNames, tablesMap,  query);
    	   	fieldValue = cleanFilterValues(fieldValue);
         	if (!fieldValue.equals("[No Value]")) {
		        switch (fieldOp) {
		            case IS:
		            /*	if(!paramExists) {
		            		fieldText = "(" + columnName + " " + " in (:" + namedParameter + "))";
		            	}
		                fieldList.add(fieldValue);*/
		                namedParameter = namedParameter + cleanParameterName(fieldValue);
		            	fieldText = "(" + columnName + " " + " in (:" + namedParameter + "))";		            	
		            	paramMap.put(namedParameter, fieldValue);
		                break;
		            case IS_NOT:
		            	/*if(!paramExists) {
		            		fieldText = "(" + columnName + " " + " not in (:" + namedParameter + "))";
		            	}
		                fieldList.add(fieldValue);*/
		                namedParameter = namedParameter + cleanParameterName(fieldValue);
		            	fieldText = "(" + columnName + " " + " not in (:" + namedParameter + "))";		            	
		            	paramMap.put(namedParameter, fieldValue);
		                break;
		            case CONTAINS:
		            	namedParameter = namedParameter + cleanParameterName(fieldValue);
		            	fieldText = "(" + columnName + " " + " ilike (:" + namedParameter + "))";
		            	/*fieldList.clear();
		            	fieldList.add("%" + fieldValue + "%");*/
		            	paramMap.put(namedParameter, "%" + fieldValue + "%");
		                break;
		            case NOT_CONTAINS:
		            	namedParameter = namedParameter + cleanParameterName(fieldValue);
		            	fieldText = "(" + columnName + " " + " not ilike (:" + namedParameter + "))";
		            	/*fieldList.clear();
		            	fieldList.add("%" + fieldValue + "%");*/
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
        String columnName = getColumnNameForField(field);        
        doJoinTables(field,  refTable, tableNames,  tablesMap, query, ownerTable, paramMap);
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
	                	namedParameterUpper = field.getVertica().getColumn() + upper;
	                	namedParameterUpper = cleanParameterName(namedParameterUpper);
	                	// fieldText = fieldText + "(";           	 
	                	// fieldText = fieldText + "(" + columnName + betweenOP + "(:" + namedParameterLower + ") and (:" + namedParameterUpper + ")))";
	                	fieldText = fieldText + "((" + columnName + geOP + "(:" + namedParameterLower + ")) and ("  + columnName + leOp + "(:" + namedParameterUpper + ")))";	                	
	                	paramMap.put(namedParameterLower, lower);                   
	                    paramMap.put(namedParameterUpper, upper);
	                } else { // only lower
	                	fieldText = fieldText + "(";           	 
	                	fieldText = fieldText + "(" + columnName + geOP + "(:" + namedParameterLower + ")))";
	                	paramMap.put(namedParameterLower, lower);
	                }
	
	            } else {
	                if (upper != null) {
	                	namedParameterUpper = field.getVertica().getColumn() + upper;
	                	namedParameterUpper = cleanParameterName(namedParameterUpper);
	                	fieldText = fieldText + "(";   
	                    fieldText =  fieldText + "(" + columnName +  leOp  + "(:" + namedParameterUpper + ")))";
	                    paramMap.put(namedParameterUpper, upper);
	
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
    	String columnName = getColumnNameForField(field);		
        doJoinTables(field,  refTable, tableNames,  tablesMap, query, ownerTable, paramMap);
	   	handleDefaultFilter( refTable, paramMap, tableNames, tablesMap,  query);
        String namedParameterNumber = field.getVertica().getColumn() + "number";
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
 	
    public static FieldText buildGreaterEqual(final String idolField, final Double value) {
        return (new GREATER(idolField, value)).OR(new EQUAL(idolField, value));
    }

    public static FieldText buildLessEqual(final String idolField, final Double value) {
        return (new LESS(idolField, value)).OR(new EQUAL(idolField, value));
    }
    
    public static String buildAsOfDate(final String idolField, final FilterOperator fieldOp, final String submittedValue, final DateFormat dateFormat,
    		Map<String, Object> paramMap, FilterField field, Set<String> tableNames, 
			String refTable, Map<String, Table> tablesMap, SelectBuilder query, String ownerTable) {
        
    	String fieldText = null;
    	if(!query.isDefaultFilterAdded(field.getName())) {  
	        final String[] values = submittedValue.split(",");
	        String subExpression = field.getVertica().getFilterSection().getFilters().getCustomFilter().get(0).getCustomFilterExpression();
	        subExpression = parseCustomFilter(subExpression, refTable, tableNames, tablesMap, query);
	        doJoinTables(field,  refTable, tableNames,  tablesMap, query, ownerTable, paramMap);
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

    
    public static String buildMonthPartExpression(FilterField field, final String submittedValue, final DateFormat dateFormat) {
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
		String columnName = getColumnNameForField(field);
		// String namedParameterNumber1 = field.getVertica().getColumn() + "number1";
        // namedParameterNumber1 = cleanParameterName(namedParameterNumber1);
        // paramMap.put(namedParameterNumber1, endDate);
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
   			// Calculate date 1 year prior 
    		cal.add(Calendar.MONTH, -12);
      		result=cal.getTime();
    		startDate=sdf.format(result);
  		} else if (values[1].equals("TWO_YEARS")) {
   			// Calculate date 2 years prior 
    		cal.add(Calendar.MONTH, -24);
      		result=cal.getTime();
    		startDate=sdf.format(result);
  		} else if (values[1].equals("THREE_YEARS")) {
   			// Calculate date 3 years prior 
    		cal.add(Calendar.MONTH, -36);
      		result=cal.getTime();
    		startDate=sdf.format(result);
  		} // All or others assumes no lower bound
   		
       	if (!startDate.equals("")) {
           fieldText = "(" + fieldText + " and (" + columnName + " >= " +  startDate + "))";
    	}	

       	return fieldText;
    }
   
    
    public static String buildMonthPart(final String idolField, final FilterOperator fieldOp, final String submittedValue, final DateFormat dateFormat,
    		Map<String, Object> paramMap, FilterField field, Set<String> tableNames, 
			String refTable, Map<String, Table> tablesMap, SelectBuilder query, String ownerTable) {
        
    		String fieldText = "";
    		String subExpression = "";
    		final String[] values = submittedValue.split(",");   
    		
	        if(field.getVertica() != null
	        		&& field.getVertica().getFilterSection() != null
	        		&& field.getVertica().getFilterSection().getFilters() != null
	        		&& field.getVertica().getFilterSection().getFilters().getCustomFilter().size() > 0) {
	        	subExpression = field.getVertica().getFilterSection().getFilters().getCustomFilter().get(0).getCustomFilterExpression();
	        }
	        subExpression = parseCustomFilter(subExpression, refTable, tableNames, tablesMap, query);
	        doJoinTables(field,  refTable, tableNames,  tablesMap, query, ownerTable, paramMap);
		   	handleDefaultFilter( refTable, paramMap, tableNames, tablesMap,  query);	   
		   	
		   	fieldText=buildMonthPartExpression(field, submittedValue, dateFormat);
		   	return fieldText;
    }
   
    public static void handleDefaultFilter(String curTable, Map<String, Object> paramMap, Set<String> tableNames, Map<String, Table> tablesMap, SelectBuilder query) {
    	Table table = tablesMap.get(curTable);
    	String subExpression = "";
    	if(!query.isDefaultFilterAdded(curTable)) {
	    	if(table != null) {
	    		subExpression = table.getDefaultFilter();
	    		 if(subExpression != null && !subExpression.isEmpty()) {
	    			 subExpression = parseCustomFilter(subExpression, curTable, tableNames, tablesMap, query);
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
    public static List<String> handleMandatoryFilter(String curTable, Map<String, Object> paramMap, Set<String> tableNames, Map<String, Table> tablesMap, SelectBuilder query
    		, Map<String, FilterField> filterFieldNames, FilterGroup filterGroup) {
    	 List<String> returnExps = new ArrayList<String>();
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
   
    					 if (fieldValuesEntry.getValue().getIndexed() != null && (fieldValuesEntry.getValue().getIndexed().getType() == FilterType.MONTHPART) && !userAddedFilterManually) {
	    					 FilterField field=fieldValuesEntry.getValue();
	    					 String submittedValue=defaultValue;
	    					 DateFormat dateFormat = new SimpleDateFormat(field.getIndexed().getFormat());
	    					 String subExpressionP = buildMonthPartExpression(field, submittedValue, dateFormat);
	    					 if(!query.isDefaultFilterAdded(fieldValuesEntry.getKey())) {
	    						 query.where(subExpressionP);
	    						 query.setDefaultFilterAdded(fieldValuesEntry.getKey(), true);
	    					 }
			            	 returnExps.add(subExpressionP);
    					 }
    				     // process all the custom filters defined with optional date for AS_OF_DATE or 
	    				 if(fieldValuesEntry.getValue().getVertica().getFilterSection().getFilters() != null) {
	    					 for(CustomFilterObject customFilterObject : fieldValuesEntry.getValue().getVertica().getFilterSection().getFilters().getCustomFilter()) {
	    						 if(customFilterObject != null) {
	    							 // The anchor table filter and/or the mapping relation filter
	    							 if(customFilterObject.getRelationName() == null || (customFilterObject.getRelationName() != null && !query.isRelationAdded(customFilterObject.getRelationName()))) { // default one.... this should always be applied??
	    								String subExpression = customFilterObject.getCustomFilterExpression();
	    								if(subExpression != null && !subExpression.isEmpty()) {
	    			    	    			 subExpression = parseCustomFilter(subExpression, curTable, tableNames, tablesMap, query);	    			    	    						
	    			    	    			 String namedParameterDefaultFilter = getNamedParameter("namedParameterCustomFilter", fieldValuesEntry.getValue().getName(), "");	    			    	    			 	    			    		    		 
	    			    		    		 namedParameterDefaultFilter = cleanParameterName(namedParameterDefaultFilter);
	    			    		    		 if(defaultType != null && defaultType.equalsIgnoreCase("date")) { // for dates
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
	    			    		            	 if(!query.isDefaultFilterAdded(fieldValuesEntry.getKey())) {
	    			    		            		 query.where(subExpression);
	    			    		            		 query.setDefaultFilterAdded(fieldValuesEntry.getKey(), true);
	    			    		            	 }
	    			    		            	 returnExps.add(subExpression);
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
    	 return returnExps;
    }
    
    private static String parseCustomFilter(String subExpression, String fromTable, Set<String> tableNames, Map<String, Table> tablesMap, SelectBuilder query) {    	
    	if(subExpression != null && !subExpression.isEmpty()) {
    		subExpression = StringUtils.trim(subExpression);
    		subExpression = StringUtils.strip(subExpression);
    		String[] tables = StringUtils.substringsBetween(subExpression, "[", "]");
    		for(String table : tables) {
    			joinTables(fromTable, table, tableNames, tablesMap, query, null);
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
        
        
        String columnName = getColumnNameForField(field);	
        
        doJoinTables(field,  refTable, tableNames,  tablesMap, query, ownerTable, paramMap);
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
            	 String namedParameterDateAfter = field.getVertica().getColumn() + "after";
            	 namedParameterDateAfter = cleanParameterName(namedParameterDateAfter);
                 paramMap.put(namedParameterDateAfter, values[0]);                 
                 fieldText = fieldText + "(cast(" + columnName + " as date)" +  " >= " + " (:" + namedParameterDateAfter + "))";               
                break;
            case BEFORE:
            	String namedParameterDateBefore = field.getVertica().getColumn() + "before";
            	namedParameterDateBefore = cleanParameterName(namedParameterDateBefore);
                paramMap.put(namedParameterDateBefore, values[0]);                 
                fieldText = fieldText + "(cast(" + columnName + " as date)" + " < " + " (:" + namedParameterDateBefore + "))"; 
                break;
            case BETWEEN:
            case PERIOD:
            	String namedParameterDateRangeAfter = field.getVertica().getColumn() + "after";
            	namedParameterDateRangeAfter = cleanParameterName(namedParameterDateRangeAfter);
                paramMap.put(namedParameterDateRangeAfter, values[0]);
            	String namedParameterDateRangeBefore = field.getVertica().getColumn() + "before";
            	namedParameterDateRangeBefore = cleanParameterName(namedParameterDateRangeBefore);
                paramMap.put(namedParameterDateRangeBefore, values[1]);               
                fieldText = fieldText + "((cast(" + columnName + " as date)" + " >= " + " (:" + namedParameterDateRangeAfter + "))"; 
                fieldText = fieldText + " and (cast(" + columnName + " as date)" + " < " + " (:" + namedParameterDateRangeBefore + ")))"; 
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
		String columnName = getColumnNameForField(field);
		String namedParameter = field.getVertica().getColumn() + "doc" + folderId;
		namedParameter = cleanParameterName(namedParameter);
		if (fieldValue.size()>0) {				
			paramMap.put(namedParameter, fieldValue);    
	  
	        System.out.println(" Parameters size = " + fieldValue.size());
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
    
    public static String getColumnNameForField(FilterField field) {
		
    	if(field.getVertica().getSelect().getSelectOperator().getSelectExpression() != null && 
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
    
    private static void doJoinTables(FilterField field, String refTable, Set<String> tableNames, Map<String, Table> tablesMap, SelectBuilder query, String ownerTable, 
    		 Map<String, Object> paramMap) {
    	String fromTable = field.getVertica().getTable().getFromTable();
    	
    	handleDefaultFilter( fromTable, paramMap, tableNames, tablesMap,  query);
    	
		// join the from table	
    	if(ownerTable != null && !ownerTable.equalsIgnoreCase(fromTable)) {
    		joinTables(ownerTable, refTable, tableNames, tablesMap, query, null);
    	   	handleDefaultFilter( refTable, paramMap, tableNames, tablesMap,  query);
    	}
    	joinTables(refTable, fromTable, tableNames, tablesMap, query, null);
	   	handleDefaultFilter( refTable, paramMap, tableNames, tablesMap,  query);
		String dimTable = field.getVertica().getTable().getDimensionTable();		
		joinTables(fromTable, dimTable, tableNames, tablesMap, query, field.getVertica().getTable().getMappingKey());
	   	handleDefaultFilter( dimTable, paramMap, tableNames, tablesMap,  query);
	   	QueryBuilder.joinTables(fromTable, QueryBuilder.getTableNameForMultipleColumn(field), tableNames, tablesMap, query, null);
    }
    
    private static void doJoinTables(String fromTable, String dimTable, String mappingKey, String refTable, Set<String> tableNames, Map<String, Table> tablesMap, SelectBuilder query, String ownerTable, 
   		 Map<String, Object> paramMap) {
   	//String fromTable = field.getVertica().getTable().getFromTable();
   	
   	handleDefaultFilter( fromTable, paramMap, tableNames, tablesMap,  query);
   	
		// join the from table	
   	if(ownerTable != null && !ownerTable.equalsIgnoreCase(fromTable)) {
   		joinTables(ownerTable, refTable, tableNames, tablesMap, query, null);
   	   	handleDefaultFilter( refTable, paramMap, tableNames, tablesMap,  query);
   	}
   	joinTables(refTable, fromTable, tableNames, tablesMap, query, null);
	   	handleDefaultFilter( refTable, paramMap, tableNames, tablesMap,  query);
		//String dimTable = field.getVertica().getTable().getDimensionTable();		
		joinTables(fromTable, dimTable, tableNames, tablesMap, query, mappingKey);
	   	handleDefaultFilter( dimTable, paramMap, tableNames, tablesMap,  query);
	   //	QueryBuilder.joinTables(fromTable, QueryBuilder.getTableNameForMultipleColumn(field), tableNames, tablesMap, query, null);
   }
    
    public static void joinTables(String fromTable, String toTable, Set<String> tableNames, Map<String, Table> tablesMap, SelectBuilder query, String relationKey) {
    	if(fromTable != null && toTable != null && !fromTable.equalsIgnoreCase(toTable) && !tableNames.contains(toTable)) {
    		Relation relation = getMappingRelation(fromTable, toTable, tablesMap, relationKey);
			if(relation != null) {
				// Add the parent relation first
				if(StringUtils.isNotEmpty(relation.getParentRelationName())) {
					Relation parentRelation = getMappingRelation(fromTable, toTable, tablesMap, relation.getParentRelationName());
					if(parentRelation != null) {						
						joinTables(parentRelation.getFromTableName(), parentRelation.getToTableName(), tableNames, tablesMap, query, parentRelation.getRelationName());
					}
				}
				if(relation.getJoinExpression() != null && !relation.getJoinExpression().isEmpty()) {
					String exp = relation.getJoinExpression();					
					query.join(exp);
					query.addRelationMapping(relation.getRelationName());
				} else {
					query.join(JoinType.getByName(relation.getRelationType().toUpperCase()), relation.getFromTableName(), relation.getToTableName(), 
							relation.getFromColumnName(), relation.getToColumnName(), null, null);
					query.addRelationMapping(relation.getRelationName());
				}
				tableNames.add(toTable);
				if(StringUtils.isNotEmpty(relation.getParentRelationName())) {
					Relation parentRelation = getMappingRelation(null, null, tablesMap, relation.getParentRelationName());
					if(parentRelation != null) {
						joinTables(parentRelation.getFromTableName(), parentRelation.getToTableName(), tableNames, tablesMap, query, parentRelation.getRelationName());
					}
				}
			}
		}
    }
    
    public static String buildCaseWhenClauseForGroupField(FilterField field, SearchConfig config) {
		StringBuffer caseWhen = new StringBuffer();
		boolean firstIter = true;			
		if(field != null) {
			Group group = config.getGroupFields().get(field.getName());
			String columnName = getColumnNameForField(field);
			//configured val
			if(group != null && field != null && group.getGroupType().equalsIgnoreCase("range")) {
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
			} else {
				if(group != null && field != null && group.getGroupType().equalsIgnoreCase("NullCheck")) {
					
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
    
    
}
	