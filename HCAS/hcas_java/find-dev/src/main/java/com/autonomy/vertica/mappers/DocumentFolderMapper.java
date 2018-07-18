package com.autonomy.vertica.mappers;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.autonomy.find.api.database.DocumentFolder;
import com.autonomy.find.api.database.SavedFiltersFolder;

public class DocumentFolderMapper implements RowMapper<SavedFiltersFolder>{

	@Override
	public SavedFiltersFolder mapRow(ResultSet rs, int rowNum) throws SQLException {
		SavedFiltersFolder docFolder = new SavedFiltersFolder();
		docFolder.setId(rs.getInt("id"));
		docFolder.setDocCount(rs.getInt("doccount"));
		docFolder.setFolderType(rs.getString("foldertype"));
		docFolder.setPrimaryFolder(rs.getBoolean("is_primary"));
		docFolder.setName(rs.getString("name"));
		docFolder.setOwner(rs.getString("owner"));
		docFolder.setReadOnly(rs.getBoolean("read_only"));
		docFolder.setRestricted(rs.getBoolean("restricted"));
		docFolder.setSearchView(rs.getString("view"));
		docFolder.setTooltip(rs.getString("tooltip"));
		docFolder.setParent_id(rs.getInt("parent_id"));
		
		return docFolder;
	}

}
