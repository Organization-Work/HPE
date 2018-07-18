package com.autonomy.find.dto.admin;

import lombok.Data;

@Data
public class LogFile {
    private String filename;
    private String fileUrl;
    private long lastModified;
    private String sizeVal;

    public LogFile(final String filename, final long lastModified, final String sizeVal) {
        this.filename = filename;
        this.lastModified = lastModified;
        this.sizeVal = sizeVal;
    }

	public void setFileUrl(String string) {
		this.fileUrl=string;
	}
}
