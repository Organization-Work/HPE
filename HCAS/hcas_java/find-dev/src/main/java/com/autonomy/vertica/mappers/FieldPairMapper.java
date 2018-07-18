package com.autonomy.vertica.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.autonomy.find.dto.FieldPair;

public class FieldPairMapper implements ResultSetExtractor<List<FieldPair>> {

	@Override
	public List<FieldPair> extractData(ResultSet rs) throws SQLException, DataAccessException {
		List<FieldPair> listPair = new ArrayList<FieldPair>();
		while(rs.next()) {
			FieldPair pair = new FieldPair();
			try {
				pair.setPrimaryField(rs.getString("primaryFieldName"));
				pair.setSecondaryField(rs.getString("secondaryFieldName"));
				
				String pv=rs.getString("primaryFieldValue");
				if (pv==null) {
					pair.setPrimaryValue("[No Value]");				
				} else {
					pair.setPrimaryValue(pv);									
				}
				
				String sv=rs.getString("secondaryFieldValue");
				if (sv==null) {
					pair.setSecondaryValue("[No Value]");				
				} else {
					pair.setSecondaryValue(sv);									
				}
				double val=rs.getDouble("documentcount");
				pair.setCount(val);
				listPair.add(pair);
			} catch (Exception e) {
				
			}
		}
		return listPair;
	}
}
