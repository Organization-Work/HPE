package com.autonomy.vertica.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.autonomy.find.util.FieldUtil;
import com.autonomy.vertica.docview.Autnresponse;
import com.autonomy.vertica.table.mapping.Table;
import com.autonomy.vertica.table.mapping.TableRelationList;
import com.autonomy.vertica.table.mapping.TableRelations;
import com.autonomy.vertica.table.mapping.Tables;

public class VerticaUtil {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(VerticaUtil.class);
	
	 public static List<Table> loadTables(final String filename, final String mappingSchema) throws IOException {
	        final Tables tables;
	        try {
	            final InputStream fieldsInStream = FieldUtil.class.getClassLoader().getResourceAsStream(filename);
	            final URL schemaUrl = FieldUtil.class.getClassLoader().getResource(mappingSchema);


	            final JAXBContext jaxbContext = JAXBContext.newInstance(Tables.class);
	            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

	            final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	            final Schema schema = schemaFactory.newSchema(schemaUrl);
	            unmarshaller.setSchema(schema);

	            tables = (Tables) unmarshaller.unmarshal( fieldsInStream );
	        } catch (final Throwable e) {
	            final String errorMsg = String.format("Error parsing [%1$s] with schema [%2$s]", filename, mappingSchema);
	            LOGGER.error(errorMsg, e);
	            throw new RuntimeException(errorMsg);
	        }

	       

	        return tables.getTable();

	    }
	 
	 public static List<TableRelationList> loadTableRelations(final String filename, final String mappingSchema) throws IOException {
	        final TableRelations tableRels;
	        try {
	            final InputStream fieldsInStream = FieldUtil.class.getClassLoader().getResourceAsStream(filename);
	            final URL schemaUrl = FieldUtil.class.getClassLoader().getResource(mappingSchema);


	            final JAXBContext jaxbContext = JAXBContext.newInstance(TableRelations.class);
	            final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

	            final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	            final Schema schema = schemaFactory.newSchema(schemaUrl);
	            unmarshaller.setSchema(schema);

	            tableRels = (TableRelations) unmarshaller.unmarshal( fieldsInStream );
	        } catch (final Throwable e) {
	            final String errorMsg = String.format("Error parsing [%1$s] with schema [%2$s]", filename, mappingSchema);
	            LOGGER.error(errorMsg, e);
	            throw new RuntimeException(errorMsg);
	        }

	       

	        return tableRels.getTableRelationList();

	    }
	 
	 public static Document generateAdmissionsXML(final String filtersSchema, final Autnresponse jaxbElement) throws IOException {
	        
	        try {
	           // final InputStream fieldsInStream = FieldUtil.class.getClassLoader().getResourceAsStream(filename);
	           // final URL schemaUrl = FieldUtil.class.getClassLoader().getResource(filtersSchema);


	            final JAXBContext jaxbContext = JAXBContext.newInstance(Autnresponse.class);
	            final Marshaller marshaller = jaxbContext.createMarshaller();
	            
	            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	            DocumentBuilder db = dbf.newDocumentBuilder();
	            Document document = db.newDocument();

	            // final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
	            // final Schema schema = schemaFactory.newSchema(schemaUrl);
	            // marshaller.setSchema(schema);

	            marshaller.marshal(jaxbElement, document);
	            
	            TransformerFactory tf = TransformerFactory.newInstance();
	            Transformer t = tf.newTransformer();
	            DOMSource source = new DOMSource(document);
	            StreamResult result = new StreamResult(System.out);
	            t.transform(source, result);
	            
	            return document;
	        } catch (final Throwable e) {
	            final String errorMsg = String.format("Error creating Admissions XML data with schema [%1$s]", filtersSchema);
	            LOGGER.error(errorMsg, e);
	            throw new RuntimeException(errorMsg);
	        }

	    }

}
