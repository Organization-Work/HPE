package com.autonomy.find.dto.Parametric;

import lombok.Data;

@Data
public class DocImportType {
    public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public void setImportField(String importField) {
		this.importField = importField;
	}
	private String displayName;
    private String importField;
	public String getImportField() {
		return importField;
	}
	
	@Override
	public boolean equals(Object importType) {
		if (!(importType instanceof DocImportType)) {
	        return false;
	    }
		DocImportType docImportType = (DocImportType)importType;
		return this.displayName.equalsIgnoreCase(docImportType.getDisplayName())
				&& this.importField.equalsIgnoreCase(docImportType.getImportField());
	}
	
	@Override
	public int hashCode() {
	    int hashCode = 1;

	    hashCode = hashCode * 37 + this.displayName.hashCode();
	    hashCode = hashCode * 37 + this.importField.hashCode();

	    return hashCode;
	}
}
