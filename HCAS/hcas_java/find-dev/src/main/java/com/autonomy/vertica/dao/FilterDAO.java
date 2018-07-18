/**
 * 
 */
package com.autonomy.vertica.dao;

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.autonomy.vertica.fields.FieldValue;
import com.autonomy.find.dto.FieldPair;
import com.autonomy.vertica.fields.Field;
import com.autonomy.vertica.query.Document;

/**
 * @author $fara002
 *
 */
public interface FilterDAO {
	
	/**
     * This is the method is used for database connection.
     */

    public void setDataSource(DataSource ds);
    
    /**
     * This method is used to return the total number of encounters/patients
     * @return
     */
    public double getTotalHits(String sql, Map<String, Object> filterMap);
    
    /**
     * 
     * @return
     */
    public Field getParametricField(String sql, Map<String, Object> filterMap, String fieldName, String limit);
    
    /**
     * 
     * @param SQL
     * @return
     */
    public List<Document> getResultDocuments(String sql, Map<String, Object> filterMap, Map<String, String> fieldMap);
    
    public List<String> listParametricFieldValues(String sql, Map<String, Object> filterMap);
    
    public List<FieldPair> getPairFieldsForTable(String sql, Map<String, Object> filterMap, String limit);

    public List<FieldValue> getTrendingTotalsBaseline(String sql, Map<String, Object> filterMap, Map<String, String> fieldMap);
    public List<FieldValue> getTrendingTotalsAdd(String sql, Map<String, Object> filterMap, Map<String, String> fieldMap);
    public List<FieldValue> getTrendingTotalsRemove(String sql, Map<String, Object> filterMap, Map<String, String> fieldMap);

	Field getTrendingDateField(String sql, Map<String, Object> filterMap, String fieldName, String limit,boolean addField);

}
