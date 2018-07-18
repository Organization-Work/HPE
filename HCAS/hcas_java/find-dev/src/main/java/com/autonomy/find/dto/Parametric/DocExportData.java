package com.autonomy.find.dto.Parametric;

import com.autonomy.find.dto.SearchRequestData;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

@Data
public class DocExportData {
    private String searchView;
    private ExportFormat exportFormat;
    private Integer exportDocFolderId;
    private String exportName;
    private String exportSourceFields;
    private String exportTargetFields;
    private Integer exportMaxDocs;
    private String exportSearchData;

    public String getExportFilename() {
        String filename = (exportName != null) ? exportName : null;

        if (filename != null && exportFormat != null) {
            filename += "." + exportFormat.toString().toLowerCase();
        }

        return filename;
    }

    public Map<String, Object> getAuditData(final Map<String, Object> data) {
        if (exportName != null) {
            data.put("exportName", exportName);
        }

        if (exportFormat != null) {
            data.put("exportFormat", exportFormat);
        }

        if (exportDocFolderId != null) {
            data.put("exportDocFolderId", exportDocFolderId);
        }

        if (exportSourceFields != null) {
            data.put("exportSourceFields", exportSourceFields);
        }

        if (exportMaxDocs != null) {
            data.put("exportMaxDocs", exportMaxDocs);
        }

        if (searchView != null) {
            data.put("searchView", searchView);
        }

        return data;
    }

	public int getExportDocFolderId() {
		return exportDocFolderId;
	}

	public String getSearchView() {
		return searchView;
	}

	public String getExportSourceFields() {
		return exportSourceFields;
	}

	public String getExportTargetFields() {
		return exportTargetFields;
	}

	public ExportFormat getExportFormat() {
		return exportFormat;
	}

	public String getExportSearchData() {
		return exportSearchData;
	}

	public Integer getExportMaxDocs() {
		return exportMaxDocs;
	}
}
