package com.autonomy.vertica.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.autonomy.vertica.fields.Field;

public class FieldMapper implements ResultSetExtractor<Field> {

	/*@Override
	public Field mapRow(ResultSet rs, int rowNum) throws SQLException {
		Field field = new Field();
		field.setNumValues(rs.getInt("totcount"));
		field.setTotalValues(rs.getInt("totcount"));
		return field;
	}*/

	@Override
	public Field extractData(ResultSet rs) throws SQLException, DataAccessException {
		Field field = new Field();
		while(rs.next()) {
			//field.setName("ADMISSION/NOTEEVENT/CAREUNIT");
			field.setNumValues(rs.getInt("totcount"));
			field.setTotalValues(rs.getInt("totcount"));
		}
		return field;
	}

}
