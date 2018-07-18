package com.autonomy.vertica.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.autonomy.find.config.SearchConfig;
import com.autonomy.find.fields.FilterField;
import com.autonomy.find.fields.FilterOperator;
import com.autonomy.find.services.ParametricService;
import com.autonomy.find.util.CollUtils;
import com.autonomy.vertica.table.mapping.Table;
import com.autonomy.vertica.templates.FilterTemplate;

public class StructuredDataXMLNew {
	
		private static final Logger LOGGER = LoggerFactory.getLogger(StructuredDataXMLNew.class);
		
		// the main method invoked 
		public Document processDocument(String fileName, FilterTemplate template, 
				String refFieldValue, String fieldName, SearchConfig config, FilterField refField, Map<String, Table> tablesMap, 
				ParametricService parametricService, String searchView) throws Exception {
			
			// get the XML template document
			Document templateDoc = parseMappingDocument(fileName);
			
			//Create the new Document
		    Document newDoc = null;
		    newDoc = getDocBuilder().newDocument();		    
			
			// get the root node from the template
			Node templateRoot = templateDoc.getDocumentElement();
			
			// add the root 
		    Element root = newDoc.createElement(templateRoot.getNodeName());
			newDoc.appendChild(root);
			
			// start processing the child nodes of the  template root
			processTemplateDocument(templateDoc, newDoc, template, 
					refFieldValue, fieldName, config, refField, tablesMap, parametricService, searchView);
		    
		   	return newDoc;
		}
		
		private String processTemplateDocument(Document templateDoc, Document newDoc, FilterTemplate template, 
				String refFieldValue, String fieldName, SearchConfig config, FilterField refField, Map<String, Table> tablesMap, 
				ParametricService parametricService, String searchView) throws Exception {
			//LOGGER.debug(templateDoc.getNodeName());	
			String rootName = templateDoc.getDocumentElement().getNodeName();			
		    NodeList nodeList = templateDoc.getElementsByTagName("*");
			for(int index = 0; index < nodeList.getLength(); index++) {
				Node currentNode = nodeList.item(index);
				if(currentNode.getNodeName().equalsIgnoreCase(rootName)) {					
					continue;
				}				
				NamedNodeMap attributes = currentNode.getAttributes();						
				if(currentNode.getNodeType() == Node.ELEMENT_NODE) {
					LOGGER.debug(currentNode.getNodeName());
					if(!currentNode.hasAttributes()) {	// this is just a container element with no attributes												
						 Element newChild = newDoc.createElement(currentNode.getNodeName());
						 String parentNodeName = currentNode.getParentNode().getNodeName();
						 Node parent = getNode(parentNodeName, newDoc.getElementsByTagName("*"));
						 if(parent != null)
							 parent.appendChild(newChild);
						 
					} else { //if(
							 //isAttributeValuePresentInNode(attributes, "row", "true")) {
						/********* This section is for row elements with children **************/
						
						// this is a parent element 
						//Element newChild = newDoc.createElement(currentNode.getNodeName());
						String parentNodeName = currentNode.getParentNode().getNodeName();
						Node parent = getNode(parentNodeName, newDoc.getElementsByTagName("*"));
						//parent.appendChild(newChild);
						 
						// this parent element is a row
						String newRowName = currentNode.getNodeName();	
						Map<String, Object> paramMap = new HashMap<String, Object>();
						Element currentElement = (Element) currentNode;
							
						// get the source query
						String query = getSourceQuery(currentNode, refFieldValue,  fieldName,  config,  refField, tablesMap, paramMap, parametricService, searchView, null);
						 
						if(query != null && !query.isEmpty()) {
							// store the query results in a temp document
							Document dataDoc = storeDataInTempDocument(query, template, paramMap, newRowName);
							
							//Retrieve all rows stored in  the old (temporary data) document
							Element dataRoot = dataDoc.getDocumentElement();
							NodeList oldRows = dataRoot.getElementsByTagName("row");							
							
							List<Element> templateElements = getAllChildElements(currentElement.getElementsByTagName("*"));
							if(templateElements.isEmpty()) { // no children so leaf node
								templateElements.add(currentElement);
							}
							
							for (int i=0; i < oldRows.getLength(); i++) {
	
						         //Retrieve each row in turn
						         Element thisRow = (Element)oldRows.item(i);
						         
						        
						         //Create the new row
						         Element newRow = newDoc.createElement(newRowName);
						         // add this section to its parent only if there are multiple columns to process, otherwise it causes duplicate entries
						         // as the column iteration adds the elements into the parent
						         //if(templateElements.size() > 1) {
						        	 parent.appendChild(newRow);
						         //}
						         for (Element thisElement : templateElements) {
						        	 
						        	 String newElementName = thisElement.getNodeName();
							         //Create the new element and add the value as text
							         Element newElement = newDoc.createElement(newElementName);
							             
						        	 // this is an inner table
						        	 if(thisElement.getAttribute("parentID") != null && !thisElement.getAttribute("parentID").isEmpty()) {
						        		 NodeList innerNodeList = thisElement.getElementsByTagName("*");
						        		 
						        		 if(thisRow.getElementsByTagName(thisElement.getAttribute("parentID")) != null) {
						        			 //String refValue = "";
						        			 String oldValue = "";
						        			 String columnAlias = thisElement.getAttribute("parentID") + "_alias";
							        		 Element oldValueElement = 
										                (Element)thisRow.getElementsByTagName(columnAlias).item(0);
							        		 if(oldValueElement != null) {
							        			 oldValue = 
										                oldValueElement.getFirstChild().getNodeValue();
										      oldValue = StringUtils.remove(oldValue, '\"');
							        		 }
							        		 Node refNode = getNode(thisElement.getAttribute("parentID"), templateDoc.getElementsByTagName("*"));
							        		 //Add the new element to the new row
								             newRow.appendChild(newElement);	
								             if(oldValue != null && !oldValue.isEmpty()) {								        		 
								            	 processTemplateDocumentForInnerNode(rootName, thisElement, newDoc, template, 
								        				 oldValue, fieldName, config, null, tablesMap, 
								        					parametricService, searchView, refNode, newRow, templateDoc);
								             }
							        		 // the subsection is done so continue
							        		 continue;
							        	 }
						        	 }
						        	 
						        	
						        	 String oldValue = "";
						        	 String useElementNameForFetch = newElementName;
						        	 //Get the database value for this element (column) based on the template information
						        	 // hack to prevent Vertica error for column alias						        	
						        	 useElementNameForFetch = useElementNameForFetch + "_alias";
						        	 if(thisRow.getElementsByTagName(useElementNameForFetch) != null) {
						        		 Element oldValueElement = 
									                (Element)thisRow.getElementsByTagName(useElementNameForFetch).item(0);
						        		 if(oldValueElement != null) {
									      oldValue = 
									                oldValueElement.getFirstChild().getNodeValue();
									      oldValue = StringUtils.remove(oldValue, '\"');
						        		 }
						        	 }
						        	 if(templateElements.size() > 1) {
						        		 newElement.appendChild(newDoc.createTextNode(oldValue));
						        		//Add the new element to the new row
							             newRow.appendChild(newElement);
							         } else {
							        	 newRow.appendChild(newDoc.createTextNode(oldValue));
							         }
						             
						           			        	 
						         }
						         // add this section to its parent
					             //parent.appendChild(newRow);
							 }
						}
						 
						 //move to the end of this block
						// Element currentElement = (Element)currentNode;
						 index = index + getChildNodeCount(currentElement.getElementsByTagName("*"));
					}
				}
				
			}			
			return null;
		}	
		
		
		
		private String getSourceQuery(Node node, String refFieldValue, String refFieldName, SearchConfig config, FilterField refField, Map<String, Table> tablesMap, 
				Map<String, Object> paramMap, ParametricService parametricService, String searchView, Node refNode ) throws Exception {
			String query = "";
			SelectBuilder builder = null;
			boolean hasChildElements = false;
			if(node != null) {			
				Element dataElement = (Element)node;
				List<Element> childElements = null;
				if(hasChildElement(dataElement.getElementsByTagName("*"))) {
					// go through each element to form the query
					// get all child element nodes
					childElements = getAllChildElements(dataElement.getElementsByTagName("*"));
					hasChildElements = true;
				} else {
					// this is the current leaf text node element with no children
					childElements = new ArrayList<Element>();
					childElements.add(dataElement);
				}
				
				// if schema, table and column are defined then create the query
				final Map<String, FilterField> filterFieldNames = parametricService.getFilterFieldNames(searchView);
				String parentTable = dataElement.getAttribute("table");
				String schema = dataElement.getAttribute("schema");
				String defaultWhere = dataElement.getAttribute("where");
				String fromTable = "";
				
				Set<String> tableNames = new HashSet<String>();
				for(Element thisElement : childElements) {
					
					String tableName = thisElement.getAttribute("table");
					String dimTableName = thisElement.getAttribute("dimensionTable");
					String tableColumn = thisElement.getAttribute("column");
					String miscOperator = thisElement.getAttribute("miscOperator");
					String selectExpression = thisElement.getAttribute("selectExpression");
					String parentRelationKey = thisElement.getAttribute("parentRelationKey");
					String dimRelationKey = thisElement.getAttribute("dimRelationKey");
					String columnTableRelationKey = thisElement.getAttribute("columnTableRelationKey");
					String viewFieldName = thisElement.getAttribute("field");
					String distinct = thisElement.getAttribute("distinct");
					String childSchema = thisElement.getAttribute("schema");
					//String distinct = thisElement.getAttribute("distinct");
					
					if(StringUtils.isNotEmpty(childSchema)) {
						schema = childSchema;
					}
					
					FilterField field = null;
					if(viewFieldName != null && !viewFieldName.isEmpty() && filterFieldNames != null && !filterFieldNames.isEmpty()) {
						field = filterFieldNames.get(viewFieldName);
					}
					// if field definition is present then use that
					if(field != null) {
						tableName = field.getVertica().getTable().getFromTable();
						dimTableName = field.getVertica().getTable().getDimensionTable();
						tableColumn = field.getVertica().getColumn();
						miscOperator = field.getVertica().getSelect().getSelectOperator().getMiscOperator();	
						selectExpression = field.getVertica().getSelect().getSelectOperator().getSelectExpression();
						schema = field.getVertica().getSchema();
					}
						
						
					if(tableName == null || tableName.isEmpty()) {
						tableName = parentTable;
					}
					
					if(tableName != null && !tableName.trim().isEmpty()) {
						if(builder == null) { // first field
							fromTable = tableName;
							builder = new SelectBuilder(tableName, schema);
							tableNames.add(tableName);
						}		
						
						String columnName = QueryBuilderNew.getColumnNameForField(dimTableName, tableName, tableColumn.trim(), miscOperator, selectExpression);	
						// hack to prevent vertica error for column alias "name"
						StringBuffer alias = new StringBuffer(thisElement.getNodeName());
						alias.append("_alias");
						String aliasUsed = alias.toString();
						
						if(hasChildElements && thisElement.getAttribute("parentID") != null && !thisElement.getAttribute("parentID").isEmpty()) {
							columnName = "'"+ thisElement.getAttribute("parentID") + "'";
							//alias = "innerTable_" + alias;
							StringBuffer innertable = new StringBuffer("innerTable_");
							innertable.append(alias);
							aliasUsed = innertable.toString();
						}
						if(columnName != null && !columnName.isEmpty()) {
							boolean distinctCol = false;
							if(distinct != null && distinct.equalsIgnoreCase("true")) {
								distinctCol = true;
							}
							builder.column(columnName, "", distinctCol, aliasUsed);
							QueryBuilderNew.joinTables(fromTable, QueryBuilder.getTableNameForMultipleColumn(tableColumn.trim()), tableNames, tablesMap,  builder, columnTableRelationKey, null, false);
							QueryBuilderNew.joinTables(fromTable, tableName, tableNames, tablesMap, builder, parentRelationKey, null, false);							
							QueryBuilderNew.joinTables(tableName, dimTableName, tableNames, tablesMap, builder, dimRelationKey, null, false);
						}
					}
				}					
								
				if(builder != null ) {
					//final Double fieldValue = CollUtils.stringsToDoubles(Collections.singletonList(refFieldValue)).get(0);
					String where = "";
					if(refField != null) {
						QueryBuilderNew.doJoinTables(refField, fromTable, tableNames, tablesMap, builder, "", paramMap);
						where = QueryBuilderNew.buildNumeric(refFieldName, FilterOperator.EQ, refFieldValue, config, paramMap, refField, tableNames, 
							fromTable, tablesMap, builder, null);
					} else { 
						if(refNode != null) {
							Element thisElement = (Element) refNode;
							String tableName = thisElement.getAttribute("table");
							String dimTableName = thisElement.getAttribute("dimensionTable");
							String tableColumn = thisElement.getAttribute("column");
							String miscOperator = thisElement.getAttribute("miscOperator");
							String selectExpression = thisElement.getAttribute("selectExpression");
							String parentRelationKey = thisElement.getAttribute("parentRelationKey");
							String columnTableRelationKey = thisElement.getAttribute("columnTableRelationKey");
							String dimRelationKey = thisElement.getAttribute("dimRelationKey");
							
							where = QueryBuilderNew.buildNumeric(dimTableName, tableName, tableColumn, miscOperator, selectExpression,  refFieldName, FilterOperator.EQ, refFieldValue, config, paramMap, tableNames, 
									fromTable, tablesMap, builder, null, parentRelationKey);
						}
						
					}
					if(defaultWhere != null && !defaultWhere.isEmpty()) {
						builder.where(defaultWhere);
					}
					
					if(where == null) {
						throw new Exception("No doc reference field found for where clause so abort!!");
					}
					
					if(where != null && !where.isEmpty()) {
						builder.where(where);
					}
						
					
				}
				
			}
			if(builder != null) {
				query = builder.toString();
			}
			return query;
		}
		
		private Document storeDataInTempDocument(String query, FilterTemplate template, Map<String, Object> paramMap, String rowName) throws ParserConfigurationException {
			Document dataDoc = null;
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		    factory.setNamespaceAware(false);
			DocumentBuilder docBuilder = factory.newDocumentBuilder();
			dataDoc = docBuilder.newDocument();
			if(LOGGER.isDebugEnabled()) {
				for(Map.Entry<String, Object> entry : paramMap.entrySet()) {
					LOGGER.debug("Fetching  <" + rowName + ">" + " for " + entry.getKey() + " = " + entry.getValue());
				}
			}
			
			Element dataRoot = template.getDocumentRecord(query, paramMap, dataDoc);
			if(dataRoot != null)
				dataDoc.appendChild(dataRoot);
			return dataDoc;
			
		}
		
		protected Node getNode(String tagName, NodeList nodeList) {
			//NodeList nodeList = templateDoc.getElementsByTagName("*");
			for(int index = 0; index < nodeList.getLength(); index++) {
				Node node = nodeList.item(index);
				if(node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equalsIgnoreCase(tagName)) {
					return node;
				}
				if (node.hasChildNodes()) {
					getNode(tagName, node.getChildNodes());
				}
			}
			return null;
		}
		
		protected String getAttributeValueByName(NamedNodeMap nodeList, String attributeName) {
			String name = "";
			if(nodeList != null && nodeList.getNamedItem(attributeName) != null) {				
				name = nodeList.getNamedItem(attributeName).getNodeValue();				
			}
			return name;
		}
		
		protected boolean isAttributeValuePresentInNode(NamedNodeMap nodeList, String attributeName, String attributeValue) {
			boolean found = false;
			String value = getAttributeValueByName(nodeList, attributeName);
			if(value != null && !value.isEmpty() && value.equalsIgnoreCase(attributeValue)) {
				found = true;
			}
			return found;
		}
		
		protected int getChildNodeCount(NodeList nodeList) {
			//NodeList nodeList = templateDoc.getElementsByTagName("*");
			int count = 0;
			for(int index = 0; index < nodeList.getLength(); index++) {
				Node node = nodeList.item(index);
				if(node.getNodeType() == Node.ELEMENT_NODE) {
					count++;
				}
			}
			return count;
		}
		
		protected boolean hasChildElement(NodeList nodeList) {			
			for(int index = 0; index < nodeList.getLength(); index++) {
				Node node = nodeList.item(index);
				if(node.getNodeType() == Node.ELEMENT_NODE) {
					return true;
				}
			}
			return false;
		}
		
		protected Node getFirstChildElement(NodeList nodeList) {			
			for(int index = 0; index < nodeList.getLength(); index++) {
				Node node = nodeList.item(index);
				if(node.getNodeType() == Node.ELEMENT_NODE) {
					return node;
				}
			}
			return null;
		}
		
		protected List<Element> getAllChildElements(NodeList nodeList) {
			List<Element> childElements = new ArrayList<Element>();
			for(int index = 0; index < nodeList.getLength(); index++) {
				Node node = nodeList.item(index);
				if(node.getNodeType() == Node.ELEMENT_NODE) {
					Element e = (Element) node;
					if(e.getAttribute("parentID") != null && !e.getAttribute("parentID").isEmpty()) {
						index = index + getChildNodeCount(e.getElementsByTagName("*")); // move to the end of this section
					}
					childElements.add(e);
				}
			}
			return childElements;
		}
		
		
		private Document parseMappingDocument(String fileName) throws ParserConfigurationException, SAXException, IOException {			
		    final InputStream fieldsInStream = StructuredDataXMLNew.class.getClassLoader().getResourceAsStream(fileName);	    
		    return getDocBuilder().parse(fieldsInStream);		
		}
		
		private DocumentBuilder getDocBuilder() throws ParserConfigurationException {
			 DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			    factory.setNamespaceAware(false);
			    return factory.newDocumentBuilder();
		}
		
		
		
		private String processTemplateDocumentForInnerNode(String rootName, Node currentNode, Document newDoc, FilterTemplate template, 
				String refFieldValue, String fieldName, SearchConfig config, FilterField refField, Map<String, Table> tablesMap, 
				ParametricService parametricService, String searchView, Node refNode, Node parent, Document templateDoc) throws Exception {
			//LOGGER.debug(templateDoc.getNodeName());	
			
			if(currentNode == null || parent == null || currentNode.getNodeName().equalsIgnoreCase(rootName)) {					
				return "";
			}	
			
			if(currentNode.getNodeType() == Node.ELEMENT_NODE) {
				LOGGER.debug("InnerNode:" + currentNode.getNodeName());
				Element currentElement = (Element) currentNode;				
				if(currentElement.getAttribute("parentID") != null && !currentElement.getAttribute("parentID").isEmpty()
						&& currentElement.getAttribute("row") == null || currentElement.getAttribute("row").isEmpty()) {
					
					processTemplateDocumentForInnerNode(rootName, getFirstChildElement(currentElement.getChildNodes()), newDoc, template, 
							refFieldValue, fieldName, config, null, tablesMap, 
	        					parametricService, searchView, refNode, getNode(currentNode.getNodeName(), parent.getChildNodes()), templateDoc);
					
				} else {
					
					String newRowName = currentNode.getNodeName();	
					Map<String, Object> paramMap = new HashMap<String, Object>();
					
						
					// get the source query
					String query = getSourceQuery(currentNode, refFieldValue,  fieldName,  config,  refField, tablesMap, paramMap, parametricService, searchView, refNode);
				 
					if(query != null && !query.isEmpty()) {
						// store the query results in a temp document
						Document dataDoc = storeDataInTempDocument(query, template, paramMap, newRowName);
						
						
						//Retrieve all rows stored in  the old (temporary data) document
						Element dataRoot = dataDoc.getDocumentElement();
						NodeList oldRows = dataRoot.getElementsByTagName("row");							
						
						List<Element> templateElements = getAllChildElements(currentElement.getElementsByTagName("*"));
						if(templateElements.isEmpty()) { // no children so leaf node
							templateElements.add(currentElement);
						}
						
						Map<String, Map<String, Element>> innerNodes = new HashMap<String, Map<String, Element>>();						
						List<Element> innerNodeList = new ArrayList<Element>();
						for (int i=0; i < oldRows.getLength(); i++) {						
		
					         //Retrieve each row in turn
					         Element thisRow = (Element)oldRows.item(i);					         
					         //Create the new row
					         Element newRow = newDoc.createElement(newRowName);
					         
					         for (Element thisElement : templateElements) {					        	 
					        	 String newElementName = thisElement.getNodeName();
					        	 //Create the new element and add the value as text
					             Element newElement = newDoc.createElement(newElementName);  
					        	 
					        	 // this is an inner table - so recurse
					        	 if(thisElement.getAttribute("parentID") != null && !thisElement.getAttribute("parentID").isEmpty() 
					        			 && hasChildElement(thisElement.getElementsByTagName("*"))) {
						        		 
					        		 	if(thisRow.getElementsByTagName(thisElement.getAttribute("parentID")) != null) {
						        			 
						        			 Map<String, Element> innerNodeMap = innerNodes.get(newElementName);
							            	 if(innerNodeMap == null) {
							            		 innerNodeMap = new HashMap<String, Element>();
							            		 innerNodes.put(newElementName, innerNodeMap);
							            	 }	
						        			 //String refValue = "";
						        			 String oldValue = "";
						        			 String columnAlias = thisElement.getAttribute("parentID") + "_alias";
							        		 Element oldValueElement = 
										                (Element)thisRow.getElementsByTagName(columnAlias).item(0);
							        		 if(oldValueElement != null) {
							        			 oldValue = 
										                oldValueElement.getFirstChild().getNodeValue();
							        			 oldValue = StringUtils.remove(oldValue, '\"');
							        		 }
							        		 
							        		 //Add the new element to this row
								             newRow.appendChild(newElement);
								             if(oldValue != null && !oldValue.isEmpty()) {
								            	 /*List<Element> listElement = innerNodeMap.get(oldValue);
								            	 if(listElement == null) {
								            		 listElement = new ArrayList<Element>();
								            	 }
								            	 listElement.add(newElement);*/
								            	 innerNodeMap.put(oldValue, newElement);								            	 
								            	 
								        		 /*processTemplateDocumentForInnerNode(rootName, thisElement, newDoc, template, 
								        				 oldValue, fieldName, config, null, tablesMap, 
								        					parametricService, searchView, innerRefNode, newRow, templateDoc);*/
								             }
							        		 // the subsection is done so continue
							        		 continue;
							        	 }
						        	 }	        	 
					        	 
					        	 String oldValue = "";
					        	 String useElementNameForFetch = newElementName;
					        	 // Get the database value for this element (column) based on the template information
					        	 // hack to prevent Vertica error for column alias
					        	 useElementNameForFetch = useElementNameForFetch + "_alias";
					        	 if(thisRow.getElementsByTagName(useElementNameForFetch) != null) {
					        		 Element oldValueElement = 
								                (Element)thisRow.getElementsByTagName(useElementNameForFetch).item(0);
					        		 if(oldValueElement != null) {
								      oldValue = 
								                oldValueElement.getFirstChild().getNodeValue();
								      oldValue = StringUtils.remove(oldValue, '\"');
					        		 }
					        	 }
					             
					             if(templateElements.size() > 1) {
					        		 newElement.appendChild(newDoc.createTextNode(oldValue));
					        		//Add the new element to the new row
						             newRow.appendChild(newElement);
						         } else {
						        	 newRow.appendChild(newDoc.createTextNode(oldValue));
						         }				        	 
					         }
					         // add this section to its parent
				             parent.appendChild(newRow);
						 } // end of all rows
						
						// now process all the inner nodes for this element
						for (Element thisElement : templateElements) {
							Map<String, Element> innerNodeMap = innerNodes.get(thisElement.getNodeName());
							if(innerNodeMap != null && !innerNodeMap.isEmpty()) {
								boolean parentElementAlreadyCreated = false;
								Element innerParentElement = null;
								Element innerRowElement = null;
								String keyElementName = thisElement.getAttribute("parentID");
								
								if(thisElement.getAttribute("parentID") != null && !thisElement.getAttribute("parentID").isEmpty()) {
									if(thisElement.getAttribute("row") == null || thisElement.getAttribute("row").isEmpty()) {
										innerRowElement = (Element)this.getFirstChildElement(thisElement.getChildNodes());
									} else {
										innerRowElement = thisElement;
										parentElementAlreadyCreated = true;
									}
								}
								String newInnerRowName = innerRowElement.getNodeName();	
								
								String queryString = getSourceQuery(innerRowElement,  refFieldValue,  fieldName,  config,  refField, tablesMap, paramMap, 
										parametricService, searchView, refNode);
								
								
								if(queryString != null && !queryString.isEmpty()) {
									// store the query results in a temp document
									Document dataDocInner = storeDataInTempDocument(queryString, template, paramMap, newInnerRowName);
									
									//Retrieve all rows stored in  the old (temporary data) document
									Element dataRootInner = dataDocInner.getDocumentElement();
									NodeList oldRowsInner = dataRootInner.getElementsByTagName("row");							
									
									List<Element> templateElementsInner = getAllChildElements(innerRowElement.getElementsByTagName("*"));
									if(templateElementsInner.isEmpty()) { // no children so leaf node
										templateElementsInner.add(innerRowElement);
									}
									for (int i=0; i < oldRowsInner.getLength(); i++) {						
										 boolean skipThisRow = true;
								         //Retrieve each row in turn
								         Element thisRow = (Element)oldRowsInner.item(i);					         
								         //Create the new row
								         Element newRow = newDoc.createElement(newInnerRowName);
								         
								         
								         for (Element thisElementInner : templateElementsInner) {					        	 
								        	 String newElementName = thisElementInner.getNodeName();
								        	 //Create the new element and add the value as text
								             Element newElement = newDoc.createElement(newElementName);
								             
								             String oldValue = "";
								        	 String useElementNameForFetch = newElementName;
								        	 // Get the database value for this element (column) based on the template information
								        	 // hack to prevent Vertica error for column alias
								        	 useElementNameForFetch = useElementNameForFetch + "_alias";
								        	 if(thisRow.getElementsByTagName(useElementNameForFetch) != null) {
								        		 Element oldValueElement = 
											                (Element)thisRow.getElementsByTagName(useElementNameForFetch).item(0);
								        		 if(oldValueElement != null) {
											      oldValue = 
											                oldValueElement.getFirstChild().getNodeValue();
											      oldValue = StringUtils.remove(oldValue, '\"');
											      
											      if(oldValue != null && keyElementName.equalsIgnoreCase(newElementName) && innerNodeMap.get(oldValue) != null) {
											    	  //innerNodeList = innerNodeMap.get(oldValue);
											    	  innerParentElement = innerNodeMap.get(oldValue);
											    	  skipThisRow = false; 
										          }
								        		 }
								        	 }
								             
								             if(templateElements.size() > 1) {
								        		 newElement.appendChild(newDoc.createTextNode(oldValue));
								        		 if(parentElementAlreadyCreated) {
										        	 newRow = innerParentElement;
										         }
								        		//Add the new element to the new row
									             newRow.appendChild(newElement);
									         } else {
									        	 if(parentElementAlreadyCreated) {
										        	 newRow = innerParentElement;
										         }
									        	 newRow.appendChild(newDoc.createTextNode(oldValue));
									         }		
								         }
								        if( !skipThisRow && innerParentElement != null) {
								        	/*for(Element element : innerNodeList) {
								        		element.appendChild(newRow);
								        	}*/
								        	if(!parentElementAlreadyCreated) {
								        		innerParentElement.appendChild(newRow);
								        	}
								        }
									} // end of this inner node rows
								}					
							}							
						}	
					}
				}
				
			}
			
			return null;
		}	

		
		
}
