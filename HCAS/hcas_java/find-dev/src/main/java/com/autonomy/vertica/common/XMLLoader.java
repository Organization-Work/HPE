package com.autonomy.vertica.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Properties;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.xml.sax.InputSource;

public class XMLLoader {
	
	public static void main(String[] args) throws IOException, XPathExpressionException {
		
		final File file = new File(args[0]); //("c:\\Program Files (x86)\\Hewlett-Packard\\Healthcare Analytics Web Application\\find-hc\\sample_config\\data\\admissions\\admissions-0.xml");
		 XMLLoader loader = new XMLLoader();
		 loader.listFilesForFolder(file);
		 
	}
	
	public void listFilesForFolder(final File folder) throws IOException, XPathExpressionException {
		if(folder.isDirectory()) {
		    for (final File fileEntry : folder.listFiles()) {
		        if (fileEntry.isDirectory()) {
		            listFilesForFolder(fileEntry);
		        } else {
		           processDocuments(fileEntry);
		        }
		    }
		} else {
			processDocuments(folder);
		}
	}

	
	private void processDocuments(File file) throws IOException, XPathExpressionException {
		final InputStream input = new FileInputStream(file);
		
		final InputSource source = new InputSource(new FileInputStream(file));
		final XPathFactory xPathFactory = XPathFactory.newInstance();
		final XPath xPath = xPathFactory.newXPath();
		XPathExpression xDocHADM = xPath.compile("//ADMISSION/HADM_ID");
		
		String hadm_id = xDocHADM.evaluate(source);
		 
		 try {
	            Class.forName("com.vertica.jdbc.Driver");
	        } catch (ClassNotFoundException e) {
	            System.err.println("Could not find the JDBC driver class.");
	            e.printStackTrace();
	            return;
	        }
	        Properties myProp = new Properties();
	        myProp.put("user", "dbadmin");
	        myProp.put("password", "password");
	        Connection conn;
	        
	        try {
	            conn = DriverManager.getConnection(
	                            "jdbc:vertica://172.16.117.51:5433/mimic2",
	                            myProp);
	            // establish connection and make a table for the data.
	            Statement stmt = conn.createStatement();         
	          
	         // Create the prepared statement
	            PreparedStatement pstmt = conn.prepareStatement(
	                            "INSERT INTO mimic2v26.tagged_documents" +
	                            " VALUES(?,?,?)");
	            
	            try {
	                // Insert LONG VARCHAR value 
	                System.out.println("Inserting LONG VARCHAR value");
	                pstmt.setInt(1, Integer.parseInt(hadm_id));
	                pstmt.setBytes(2, IOUtils.toByteArray(input));
	                pstmt.setTimestamp(3, new Timestamp(new Date().getTime()));
	                pstmt.addBatch();
	                pstmt.executeBatch();
	            } catch (SQLException e) {
	                System.out.println("Error message: " + e.getMessage());
	                return; // Exit if there was an error
	            }
	            // Cleanup
	            conn.close();
	        } catch (SQLException e) {
	            e.printStackTrace();
	        }
		 
	}
	
	

}
