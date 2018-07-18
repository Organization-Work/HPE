package com.autonomy.vertica.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.springframework.jdbc.core.RowMapper;

import com.autonomy.vertica.fields.FieldValue;



public class FieldValueDateMapper implements RowMapper<FieldValue> {
	
	@Override
	public FieldValue mapRow(ResultSet rs, int rowNum) throws SQLException {
		FieldValue field = new FieldValue();
		//while(rs.next()) {
			field.setCount(rs.getDouble("documentcount"));
			field.setValue(rs.getString("fieldValue"));
			Date dt=rs.getDate("fieldDate");
			if (dt!=null) {
				field.setDate(dt);
			}
		//}
		return field;
	}

}
