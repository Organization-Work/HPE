package com.autonomy.vertica.common;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.autonomy.vertica.docview.ADMISSION;
import com.autonomy.vertica.docview.DOCUMENT;
import com.autonomy.vertica.docview.PATIENT;

/**
 * 1. If the data loaded time is not latest then get the XMLs tagged by OT and store them in Vertica for Doc Viewer
 * 
 * @author frahman
 * 
 * 
 *
 */
public class XMLTagger {
	
	
	
	public static void main (String [] args) {
		
		//XMLTagger tagger = new XMLTagger();
		
		try {
			String startDate = args[0];			
			
			List<Integer> listIds = retrieveAdmissionIDsToTag(startDate);
			if(!listIds.isEmpty()) {
				for(int hadm_id : listIds) {
					buildAdmissionsXML();
				}
			}
		
			String taggedXML = tagXMLOT(null);
			
			String parsedXML = parseXML(taggedXML);
			
			//saveParsedXMLInVertica();
				
			
			System.out.println(parsedXML);
		} catch(Exception e) {
			 System.out.println("Error message: " + e.getMessage());
		} 	
		
	}	
	
	private static List<Integer> retrieveAdmissionIDsToTag(String startDate) throws ParseException {
		
		List<Integer> listAdmissionId = new ArrayList<Integer>();
		
		SimpleDateFormat datetimeFormatter = new SimpleDateFormat(
                "yyyy-MM-dd hh:mm:ss");
		Date lFromDate = datetimeFormatter.parse(startDate);
		System.out.println("startDate :" + lFromDate);
		Timestamp fromTS1 = new Timestamp(lFromDate.getTime());
		
		Connection conn = getDBConnection();
		PreparedStatement pstmt = null;
        
		String sql = "select hadm_id from mimic2v26.admissions_new where lastloaded_dt > (?)";
	    try { 
	        // Create the prepared statement
	        pstmt = conn.prepareStatement(sql);
	        pstmt.setTimestamp(1, fromTS1);
	        
	        ResultSet rs = pstmt.executeQuery(sql);
	        while(rs.next()) {
	        	listAdmissionId.add(rs.getInt("hadm_id"));
	        }
	    } catch(SQLException e) {
	    	System.out.println("Error message: " + e.getMessage());
	    } finally {
        	if(pstmt != null) {
    			try {
    				pstmt.close();
    			} catch (SQLException e) {
    				// ignore
    			}
    			pstmt = null;
    		}
        	if (conn != null) {
    			try {
    				conn.close();
    			} catch (SQLException e) {
    				// ignore
    			}
    			conn = null;
    		}	    	
	    }
        return listAdmissionId;		
	}
	
	public static String buildAdmissionsXML() throws IOException {
		DOCUMENT doc = new DOCUMENT();
		
		ADMISSION adm = new ADMISSION();
		
		PATIENT patient = new PATIENT();
		
		patient.setADMISSIONDATE("2004-12-05");
		
		adm.setPATIENT(patient);
		
		doc.setADMISSION(adm);
		
		
		
		return generateAdmissionsXML(null, doc);
		
	}
	
	private static String tagXMLOT(String xml) throws IOException {
		String url = "http://localhost:12345";
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("POST");
		//con.setRequestProperty(key, value);
		
		String urlParameters = "action=tagxml&doc=" + xml;
		
		// Send post request
		con.setDoOutput(true);
		DataOutputStream wr = new DataOutputStream(con.getOutputStream());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();
		
		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'POST' request to URL : " + url);
		System.out.println("Post parameters : " + urlParameters);
		System.out.println("Response Code : " + responseCode);

		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		
		//print result
		System.out.println(response.toString());

		
		return response.toString();

	}
	
	private static String parseXML(String taggedXML) throws XPathExpressionException {
		final InputSource source = new InputSource(new StringReader(taggedXML));
		// parse the XML from OT
		final XPathFactory xPathFactory = XPathFactory.newInstance();
		final XPath xPath = xPathFactory.newXPath();
		XPathExpression xDoc = xPath.compile("//autnresponse/responsedata/document");		
		return xDoc.evaluate(source);
	}	
	
	public static String generateAdmissionsXML(final String filtersSchema, final DOCUMENT jaxbElement) throws IOException {
        
        try {
           // final InputStream fieldsInStream = FieldUtil.class.getClassLoader().getResourceAsStream(filename);
           // final URL schemaUrl = FieldUtil.class.getClassLoader().getResource(filtersSchema);


            final JAXBContext jaxbContext = JAXBContext.newInstance(DOCUMENT.class);
            final Marshaller marshaller = jaxbContext.createMarshaller();
            
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.newDocument();

            // final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            // final Schema schema = schemaFactory.newSchema(schemaUrl);
            // marshaller.setSchema(schema);

            marshaller.marshal(jaxbElement, document);
            
            StringWriter writer = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(writer);
            t.transform(source, result);
            
            System.out.println(writer.toString());
            
            return writer.toString();
        } catch (final Throwable e) {
            final String errorMsg = String.format("Error creating Admissions XML data with schema [%1$s]", filtersSchema);
            System.out.println(errorMsg);
            throw new RuntimeException(errorMsg);
        }

    }
	
	
	
	private void saveParsedXMLInVertica(String hadm_id, InputStream input) throws SQLException, IOException {
		// establish connection if already not there
		Connection conn = getDBConnection();
                
      
        // Create the prepared statement
        PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO mimic2v26.tagged_documents" +
                        " VALUES(?,?,?)");
        
        try {
        	
        	conn.setAutoCommit(false);
            // Insert LONG VARCHAR value 
            System.out.println("Inserting LONG VARCHAR value");
            pstmt.setInt(1, Integer.parseInt(hadm_id));
            pstmt.setBytes(2, IOUtils.toByteArray(input));
            pstmt.setTimestamp(3, new Timestamp(new Date().getTime()));
            pstmt.addBatch();
            pstmt.executeBatch();
            
            conn.commit();
        } catch (SQLException e) {
            System.out.println("Error message: " + e.getMessage());
            conn.rollback();           
        } finally {
        	if(pstmt != null) {
    			try {
    				pstmt.close();
    			} catch (SQLException e) {
    				// ignore
    			}
    			pstmt = null;
    		}
        	if (conn != null) {
    			try {
    				conn.close();
    			} catch (SQLException e) {
    				// ignore
    			}
    			conn = null;
    		}	
        }
	}
	
	
	private static Connection getDBConnection() {
		Connection conn = null;
		try {
	            Class.forName("com.vertica.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Could not find the JDBC driver class.");
            e.printStackTrace();          
        }
        Properties myProp = new Properties();
        myProp.put("user", "dbadmin");
        myProp.put("password", "password");
       
        
        try {
            conn = DriverManager.getConnection(
                            "jdbc:vertica://172.16.117.51:5433/mimic2",
                            myProp);
        } catch(SQLException e) {
        	 final String errorMsg = String.format("Error connecting to Vertica:" + e.getMessage());
             System.out.println(errorMsg);
            // throw e;
        }
        return conn;
	}	        
	
	
	
	
	
	

}
