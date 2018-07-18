package com.autonomy.vertica.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import com.autonomy.find.api.database.DocumentFolder;
import com.autonomy.find.api.database.SavedFiltersFolder;

public class DocumentFolderReferenceMapper implements ResultSetExtractor<SavedFiltersFolder>{

	@Override
	public SavedFiltersFolder extractData(ResultSet rs) throws SQLException, DataAccessException {
		SavedFiltersFolder docFolder = new SavedFiltersFolder();
		Set<String> documentRefs = new HashSet<String>();
		while(rs.next()) {
			docFolder.setId(rs.getInt("documentfolder_id"));
			documentRefs.add(rs.getString("refs"));
		}
		docFolder.setRefs(documentRefs);
		return docFolder;
	}

}
