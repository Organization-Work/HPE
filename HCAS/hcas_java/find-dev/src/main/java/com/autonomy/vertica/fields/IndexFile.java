package com.autonomy.vertica.fields;


import lombok.Data;

@Data
public class IndexFile {
	
	String view;
	String type;
	String id;
	String filename;
	int start;
	int bytes;
	
}