/**
 * 
 */
package com.autonomy.vertica.templates;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.hibernate.engine.jdbc.internal.BasicFormatterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.w3c.dom.Element;

import com.autonomy.find.api.database.DocumentFolder;
import com.autonomy.find.api.database.SavedFiltersFolder;
import com.autonomy.find.dto.FieldPair;
import com.autonomy.find.dto.Parametric.DocExportData;
import com.autonomy.vertica.dao.FilterDAO;
import com.autonomy.vertica.fields.Field;
import com.autonomy.vertica.fields.FieldValue;
import com.autonomy.vertica.fields.IndexFile;
import com.autonomy.vertica.mappers.DocumentFolderMapper;
import com.autonomy.vertica.mappers.DocumentFolderReferenceMapper;
import com.autonomy.vertica.mappers.FieldPairMapper;
import com.autonomy.vertica.mappers.FieldValueDateMapper;
import com.autonomy.vertica.mappers.FieldValueMapper;
import com.autonomy.vertica.query.Document;
import com.autonomy.vertica.query.DocumentField;

/**
 * @author $fara002
 *
 */
public class FilterTemplate implements FilterDAO {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FilterTemplate.class);

	public DataSource vdataSource;
	//private JdbcTemplate jdbcTemplateObject;
	private NamedParameterJdbcTemplate namedJdbcTemplateObject;
	
	public void setDataSource(DataSource vdataSource) {
		//LOGGER.info("------Setting HCAS data source--------" );
	   this.vdataSource = vdataSource;
	  // this.jdbcTemplateObject = new JdbcTemplate(vdataSource);
	   this.namedJdbcTemplateObject = new NamedParameterJdbcTemplate(vdataSource);	   
	   //LOGGER.info("Data Source " + this.vdataSource.getConnection().);
	}

	public String formatSQL(String sql) {
		String tmp=sql.replaceAll("\n","");
		tmp=tmp.replaceAll("\t","");	
		String formattedSQL = new BasicFormatterImpl().format(tmp);
		return formattedSQL; 
	}
	
	@Override
	public Field getParametricField(String sql, Map<String, Object> filterMap, String fieldName, String limit) {
		long methstartTime = System.currentTimeMillis();
	    StringBuffer countSQL = new StringBuffer("Select count(*) as totcount from (");
	    countSQL.append(sql);
	    countSQL.append(") x");
	    Field field = new Field();	
	    try {
	    	
	    	String countSQLStr=formatSQL(countSQL.toString());
	    	long queryStartTime = System.currentTimeMillis();
		    double totalValues = namedJdbcTemplateObject.queryForObject(countSQLStr, filterMap, Double.class);
		    LOGGER.debug(" getParametricField - Executing Query #1: " + countSQLStr);
		    calculateElapsedTime(queryStartTime);
		    
		    field.setTotalValues(totalValues);
		    double numValues = 0;
		    if(limit != null && !limit.isEmpty()) {
		    	StringBuffer newSql = new StringBuffer();
		    	newSql.append(sql);
		    	newSql.append(" ");
		    	newSql.append("limit ");
		    	newSql.append(limit);	
		    	
		    	sql = newSql.toString();	    	
		    	
		    	/*StringBuffer limitSQL = new StringBuffer("Select count(*) as totcount from (");
		    	limitSQL.append(newSql);	    	    	  	
		    	limitSQL.append(") x");	
		    	String limitSQLStr=formatSQL(limitSQL.toString());
		    	queryStartTime = System.currentTimeMillis();
			    numValues = namedJdbcTemplateObject.queryForObject(limitSQLStr.toString(), filterMap, Double.class);
			    LOGGER.debug(" getParametricField - Executing Query #2: " + limitSQLStr);
			    calculateElapsedTime(queryStartTime);*/
		    	numValues = Double.parseDouble(limit);
			    
		    } else {
		    	numValues = totalValues;
		    }
		    
		    field.setNumValues(numValues);
		    
			sql=formatSQL(sql);
			queryStartTime = System.currentTimeMillis();
		    LOGGER.debug(" getParametricField - Executing Query #3: " + sql);	
		    List <FieldValue> fieldValues = namedJdbcTemplateObject.query(sql, filterMap, new FieldValueMapper());			
		    calculateElapsedTime(queryStartTime);
		    for(FieldValue fieldvalue : fieldValues) {
		       	if(fieldvalue != null) {
		    		if(fieldvalue.getValue() == null || fieldvalue.getValue().isEmpty()) {
		    			fieldvalue.setValue("[No Value]");
		    		} 
		    	}
		    }	
		    field.setFieldValues(fieldValues);
		    field.setName(fieldName);
		    
		    
		   
	    } catch(Exception e) {
	    	LOGGER.error(e.getMessage());
			throw e;
	    }
	    calculateElapsedTime(methstartTime, "getParametricField");
	    return field;
	}
	
	@Override
	public Field getTrendingDateField(String sql, Map<String, Object> filterMap, String fieldName, String limit,boolean addEndDate) {
	    
	    Field field = new Field();	    
	    field.setTotalValues(0);
	    int numValues = 0;
	    field.setNumValues(numValues);
	    
		sql=formatSQL(sql);
	    long startTime = System.currentTimeMillis();
	    LOGGER.debug(" getParametricField - Executing Query: " + sql);	
	    //System.out.println(" getParametricField - Executing Query: " + sql);	
	    List <FieldValue> fieldValues = namedJdbcTemplateObject.query(sql, filterMap, new FieldValueDateMapper());
		calculateElapsedTime(startTime);
		
	    for(FieldValue fieldvalue : fieldValues) {
	       	if(fieldvalue != null) {
	    		if(fieldvalue.getValue() == null || fieldvalue.getValue().isEmpty()) {
	    			fieldvalue.setValue("[No Value]");
	    		} else if (fieldvalue.getDate() != null) {
	    			if (addEndDate) {
		    			Calendar c = Calendar.getInstance();
		    			c.setTime(fieldvalue.getDate());
		    			c.add(Calendar.DATE, 1);
		    			fieldvalue.setDate(c.getTime());
	    			}
	    		} else {		
	    			Date nDate=new Date(1893455999);    			
	    			fieldvalue.setDate(nDate);
	    		}
	    	}
	    }
	    
	    field.setFieldValues(fieldValues);
	    field.setName(fieldName);  
	  
	    return field;
	}
	
	
	/**************   Methods to return the results for the "Results Range" section in the Bar charts Tab in UI ***********************/
	
	@Override
	public double getTotalHits(String sql, Map<String, Object> filterMap) {
		long queryStartTime = System.currentTimeMillis();
		sql=formatSQL(sql);
		LOGGER.debug(" getTotalHits - Executing Query: " + sql);
		double totalHits = 0;
		try {
			totalHits = namedJdbcTemplateObject.queryForObject(sql, filterMap, Double.class);	
		} catch(Exception e) {
			LOGGER.error(e.getMessage());
			throw e;
			//totalHits = 0;
		}
		calculateElapsedTime(queryStartTime);
		return totalHits;
	}

	@Override
	public List<Document> getResultDocuments(String sql, Map<String, Object> filterMap, Map<String, String> fieldMap) {
		long queryStartTime = System.currentTimeMillis();
		sql=formatSQL(sql);
		LOGGER.debug(" getResultDocuments - Executing Query: " + sql);
		List<Document> docs = new ArrayList<Document>();				
		List<Map<String, Object>> listRows = namedJdbcTemplateObject.queryForList(sql, filterMap);
		for(Map<String, Object> row : listRows) {
			Document doc = new Document();
			doc.setDate(new Date());
			doc.setDatabase("vertica");
			StringBuffer reference = new StringBuffer();
			StringBuffer title = new StringBuffer();
			for( Map.Entry<String, Object> entry : row.entrySet()) {
				DocumentField docField = new DocumentField();
				if(entry.getKey().contains("reference")) {
					if(entry.getValue() != null) {
						if(!reference.toString().trim().isEmpty()) {
							reference.append(",");
							title.append("_");
						}
						//reference.append(fieldMap.get(entry.getKey())).append(":").append(entry.getValue().toString());
						reference.append(entry.getValue().toString());
						title.append(entry.getValue().toString());
					} else {
						reference.append(" ");
					}
				}
				docField.setName(fieldMap.get(entry.getKey()));
				if(entry.getValue() != null) {
					String value = entry.getValue().toString().replaceAll("\"", "");
					docField.setValue(value);
				}				
				doc.addDocumentField(docField);
			}
			doc.setReference(reference.toString());
			doc.setTitle(title.toString());
			docs.add(doc);			
		}	
		calculateElapsedTime(queryStartTime);
		return docs;
	}
	
	@Override
	public List<String> listParametricFieldValues(String sql, Map<String, Object> filterMap) {	
		
		sql=formatSQL(sql);
		List<String> returnVal = new ArrayList<String>();	
		long startTime = System.currentTimeMillis();
		LOGGER.debug(" listParametricFieldValues - Executing Query: " + sql);		
		List<String> values = namedJdbcTemplateObject.queryForList(sql, filterMap, String.class);
		calculateElapsedTime(startTime);
		for(String value : values) {
			if(value != null && !value.isEmpty()) {
				returnVal.add(value.replaceAll("\"", ""));
			}
		}
		return returnVal;
	
	}
	
	public List<String> getDocumentReferences(String sql, Map<String, Object> filterMap) {
		sql=formatSQL(sql);
		long startTime = System.currentTimeMillis();
		LOGGER.debug(" getDocumentReferences - Executing Query: " + sql);
		
		List<String> values = namedJdbcTemplateObject.queryForList(sql, filterMap, String.class);
		calculateElapsedTime(startTime);		
		return values;
	}
	
	public byte[] getDocument(String sql, Map<String, Object> filterMap) {
		sql=formatSQL(sql);
		long startTime = System.currentTimeMillis();
		LOGGER.debug(" getDocument - Executing Query: " + sql);
		byte[] result = null;
		try {
			result = (byte[]) namedJdbcTemplateObject.query(sql, filterMap, new ResultSetExtractor<byte[]>() {
				@Override
				public byte[] extractData(ResultSet rs) throws SQLException {
					if(rs.next()) {
						return rs.getBytes(1);
					}
					return null;
				}
			});	
		} catch(Exception ae) {
			LOGGER.error(ae.getMessage());
		}
		calculateElapsedTime(startTime);		
		return result;
	}
	
	public Element getDocumentRecord(String sql, Map<String, Object> filterMap, final org.w3c.dom.Document dataDoc) {
		sql=formatSQL(sql);
		long startTime = System.currentTimeMillis();
		if(LOGGER.isDebugEnabled()) {					
			LOGGER.debug(" getDocumentRecord - Executing Query: " + sql);
		}
		
		try {
			if(sql != null && !sql.isEmpty()) {
				//@SuppressWarnings({ "rawtypes", "unchecked" })
				Element element = namedJdbcTemplateObject.query(sql, filterMap, new ResultSetExtractor<Element>() {
					@Override
					public Element extractData(ResultSet resultset) throws SQLException {
						Element dataRoot = dataDoc.createElement("data");
						ResultSetMetaData resultMetaData = resultset.getMetaData();
						int numCols = resultMetaData.getColumnCount();
						
						 while (resultset.next()) {
					            //For each row of data
					            //Create a new element called "row"
					            Element rowEl = dataDoc.createElement("row"); 
	
					            for (int i=1; i <= numCols; i++) {
					               //For each column, retrieve the name and data
					               String colName = resultMetaData.getColumnName(i);
					               String colVal = resultset.getString(i);
					               //If there was no data, add "and up"
					               if (resultset.wasNull()) {
					                  colVal = "[no value]";
					               }
					               //Create a new element with the same name as the column
					               Element dataEl = dataDoc.createElement(colName);
					               //Add the data to the new element
					               dataEl.appendChild(dataDoc.createTextNode(colVal));
					               //Add the new element to the row
					               rowEl.appendChild(dataEl);
					            }
					            //Add the row to the root element			
					            dataRoot.appendChild(rowEl);
						 }
						return dataRoot;
					}
				});	
				return element;
			}
		} catch(Exception ae) {
			LOGGER.error(ae.getMessage());
			throw ae;
		}
		calculateElapsedTime(startTime);		
		return null;
	}
	
	private void calculateElapsedTime(long startTime) {
		long elapsedTime = System.currentTimeMillis() - startTime;
		LOGGER.debug(" Query execution time: " + elapsedTime);		
		//return elapsedTime;
	}
	
	private void calculateElapsedTime(long startTime, String method) {
		long elapsedTime = System.currentTimeMillis() - startTime;
		LOGGER.debug(method + " Method execution time: " + elapsedTime);		
		//return elapsedTime;
	}
	
	@Override
	public List<FieldPair> getPairFieldsForTable(String sql, Map<String, Object> filterMap, String limit) {
	   
		sql=formatSQL(sql);
	    long startTime = System.currentTimeMillis();
	    LOGGER.debug(" getPairFieldsForTable - Executing Query: " + sql);	    	
	    List <FieldPair> fieldValues = namedJdbcTemplateObject.query(sql, filterMap, new FieldPairMapper());
		calculateElapsedTime(startTime);
	    return fieldValues;
	}
	
	
	@Override
	public List<FieldValue> getTrendingTotalsBaseline(String sql, Map<String, Object> filterMap, Map<String, String> fieldMap) {
	
		sql=formatSQL(sql);
		LOGGER.debug(" getTrendingTotalsBaseline - Executing Query: " + sql);
		
				
	    List<FieldValue> fieldValues = namedJdbcTemplateObject.query(sql, filterMap, new FieldValueMapper());
	    for(FieldValue fieldvalue : fieldValues) {
	       	if(fieldvalue != null) {
	    		if(fieldvalue.getValue() == null || fieldvalue.getValue().isEmpty()) {
	    			fieldvalue.setValue("undefined");
	    		} 
	    	}
	    }
	    return null;
	}

	@Override
	public List<FieldValue> getTrendingTotalsAdd(String sql,
			Map<String, Object> filterMap, Map<String, String> fieldMap) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<FieldValue> getTrendingTotalsRemove(String sql,
			Map<String, Object> filterMap, Map<String, String> fieldMap) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public IndexFile getDocumentIndexAndFileName(String sql, Map<String, Object> filterMap) {
		sql=formatSQL(sql);
		long startTime = System.currentTimeMillis();
		LOGGER.debug(" getDocument - Executing Query: " + sql);
		IndexFile result = null;
		try {
			result = (IndexFile) namedJdbcTemplateObject.query(sql, filterMap, new ResultSetExtractor<IndexFile>() {
				@Override
				public IndexFile extractData(ResultSet rs) throws SQLException {
					if(rs.next()) {
						IndexFile fileMetaData = new IndexFile();
						fileMetaData.setBytes(rs.getInt("bytes"));
						fileMetaData.setFilename(rs.getString("filename"));
						fileMetaData.setId(rs.getString("id"));
						fileMetaData.setStart(rs.getInt("start"));
						fileMetaData.setType(rs.getString("type"));						
						return fileMetaData;
					}
					return null;
				}
			});	
		} catch(Exception ae) {
			LOGGER.error(ae.getMessage());
			throw ae;
		}
		calculateElapsedTime(startTime);		
		return result;
	}
	
	/************************  DOC FOLDERS *****************************/
	public List<SavedFiltersFolder> getFolders(String sql, Map<String, Object> filterMap) {
		  return (List<SavedFiltersFolder>) namedJdbcTemplateObject.query(sql, filterMap, new DocumentFolderMapper());
	}
	
	public void add(String sql, Map<String, Object> namedParameters) {		
	      namedJdbcTemplateObject.update(sql, namedParameters);	     
	}
	
	public List<SavedFiltersFolder> getFolderByLabel(String sql, Map<String, Object> filterMap) {
			return (List<SavedFiltersFolder>) namedJdbcTemplateObject.query(sql, filterMap, new DocumentFolderMapper());
	}
	
	public void delete(String sql, Map<String, Object> namedParameters) {
		  sql=formatSQL(sql);
		  LOGGER.debug(" delete - Executing Query: " + sql);
		  namedJdbcTemplateObject.update(sql, namedParameters);		
	}
	
	public void select(String sql, Map<String, Object> namedParameters){
		  sql= formatSQL(sql);
		  LOGGER.debug("Select - Executing Query");
		  namedJdbcTemplateObject.update(sql, namedParameters);	
	}
	
	public void update(String sql, Map<String, Object> namedParameters) {		
		  namedJdbcTemplateObject.update(sql, namedParameters);
	}
	
	public SavedFiltersFolder getFolder(String sql, Map<String, Object> filterMap) {
		return (SavedFiltersFolder) namedJdbcTemplateObject.queryForObject(sql, filterMap, new DocumentFolderMapper());
    }
	
	public void addRefDocs(final int folderId, final Set<String> docs, final String docFolderSchema)
	{
		Iterator<String>  docIterator = docs.iterator();
		String SQL = "INSERT INTO " + docFolderSchema + ".documentfolder_references (documentfolder_id, refs) VALUES (:documentfolder_id, :refs)";
		List<Map<String, Object>> listMap = new ArrayList<Map<String, Object>>();
        while(docIterator.hasNext()) {        	
    		Map<String, Object> namedParameters = new HashMap<String, Object>();   
    	      namedParameters.put("documentfolder_id", folderId);
    	      namedParameters.put("refs", docIterator.next());
    	      listMap.add(namedParameters);
    	      // namedJdbcTemplateObject.update(SQL, namedParameters);
    	      //LOGGER.debug("Created folder reference record = " + folderId + " refs = " + docs);
        }
        namedJdbcTemplateObject.batchUpdate(SQL, getArrayData(listMap));
        
        // update the doc count in doc folder table
        String SQL1 = "UPDATE " + docFolderSchema + ".documentfolder SET doccount = :refs WHERE id = :documentfolder_id";
		  Map<String, Object> namedParameters = new HashMap<String, Object>();
		  namedParameters.put("documentfolder_id", folderId);
	      namedParameters.put("refs", docs.size());
		  namedJdbcTemplateObject.update(SQL1, namedParameters);
		  LOGGER.debug("Updated Record of folder reference = " + folderId );
	}
	
	public void addTagToFolder(SavedFiltersFolder folder, final String docFolderSchema, final List<String> iterator)
	{
		Iterator<String>  docIterator = iterator.iterator();
		String SQL = "INSERT INTO " + docFolderSchema + ".documentfolder_references (documentfolder_id, refs) VALUES (:documentfolder_id, :refs)";
		List<Map<String, Object>> listMap = new ArrayList<Map<String, Object>>();
		
        while(docIterator.hasNext()) {
        	Map<String, Object> namedParameters = new HashMap<String, Object>();
        	namedParameters.put("documentfolder_id", folder.getId());
    	    namedParameters.put("refs", docIterator.next());
    	    listMap.add(namedParameters);
    	     //namedJdbcTemplateObject.update(SQL, namedParameters);
    	     // LOGGER.debug("Created folder reference record = " + folder.getId() + " refs = " + docIterator);
        }
        namedJdbcTemplateObject.batchUpdate(SQL, getArrayData(listMap));
        
        // get the current count to update in the doc folder
        String SQL_COUNT = "SELECT COUNT(*) FROM " + docFolderSchema + ".documentfolder_references WHERE documentfolder_id = (:documentfolder_id)";
        Map<String, Object> namedParameters = new HashMap<String, Object>();
		namedParameters.put("documentfolder_id", folder.getId());
		int current_count = namedJdbcTemplateObject.queryForObject(SQL_COUNT, namedParameters, Integer.class);
		
        
        // now update the count in doc folder table
        String SQL1 = "UPDATE " + docFolderSchema + ".documentfolder SET doccount = :refs WHERE id = :documentfolder_id";
		
	    namedParameters.put("refs", current_count);
		namedJdbcTemplateObject.update(SQL1, namedParameters);
		LOGGER.debug("Updated Record of folder reference = " + folder.getId() );
	}
	
	public static Map<String, Object>[] getArrayData(List<Map<String, Object>> list){
        @SuppressWarnings("unchecked")
        Map<String, Object>[] maps = new HashMap[list.size()];

        Iterator<Map<String, Object>> iterator = list.iterator();
        int i = 0;
        while (iterator.hasNext()) {
            Map<java.lang.String, java.lang.Object> map = (Map<java.lang.String, java.lang.Object>) iterator
                    .next();
            maps[i++] = map;
        }

        return maps;
    }
	
	public SavedFiltersFolder getExportedFolder(String sql, Map<String, Object> filterMap)
	{
		return (SavedFiltersFolder) namedJdbcTemplateObject.query(sql, filterMap, new DocumentFolderReferenceMapper());
	}

}
