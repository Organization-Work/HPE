package com.autonomy.find.services;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.zip.GZIPInputStream;

import javax.annotation.PostConstruct;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.autonomy.aci.actions.idol.query.Document;
import com.autonomy.aci.actions.idol.query.DocumentListProcessor;
import com.autonomy.aci.actions.idol.query.QueryResponse;
import com.autonomy.aci.actions.idol.tag.Field;
import com.autonomy.aci.actions.idol.tag.FieldListProcessor;
import com.autonomy.aci.actions.idol.tag.FieldValue;
import com.autonomy.aci.client.annotations.IdolAnnotationsProcessorFactory;
import com.autonomy.aci.client.services.AciConstants;
import com.autonomy.aci.client.services.AciService;
import com.autonomy.aci.client.services.impl.DocumentProcessor;
import com.autonomy.aci.client.transport.AciParameter;
import com.autonomy.aci.client.util.AciParameters;
import com.autonomy.aci.client.util.AciURLCodec;
import com.autonomy.aci.content.fieldtext.FieldText;
import com.autonomy.aci.content.fieldtext.MATCH;
import com.autonomy.aci.content.printfields.PrintFields;
import com.autonomy.common.io.IOUtils;
import com.autonomy.find.api.database.DocumentFolder;
import com.autonomy.find.api.database.SavedFiltersFolder;
import com.autonomy.find.api.database.SearchSettings;
import com.autonomy.find.config.DisplayField;
import com.autonomy.find.config.SearchConfig;
import com.autonomy.find.dto.FieldPair;
import com.autonomy.find.dto.Link;
import com.autonomy.find.dto.Links;
import com.autonomy.find.dto.ResultDocument;
import com.autonomy.find.dto.Results;
import com.autonomy.find.dto.SearchRequestData;
import com.autonomy.find.dto.TimelineData;
import com.autonomy.find.dto.TimelineDataLayer;
import com.autonomy.find.dto.TimelineDateRange;
import com.autonomy.find.dto.TimelineFieldValueMeta;
import com.autonomy.find.dto.TimelineValueList;
import com.autonomy.find.dto.TrendingTotalsData;
import com.autonomy.find.dto.Parametric.BoolOp;
import com.autonomy.find.dto.Parametric.CohortOp;
import com.autonomy.find.dto.Parametric.DocExportData;
import com.autonomy.find.dto.Parametric.FieldParams;
import com.autonomy.find.dto.Parametric.FilterGroup;
import com.autonomy.find.dto.Parametric.TableViewResponse;
import com.autonomy.find.dto.Parametric.TimelineViewResponse;
import com.autonomy.find.dto.Parametric.TrendingTotalsResponse;
import com.autonomy.find.dto.taxonomy.CategoryNames;
import com.autonomy.find.fields.FilterField;
import com.autonomy.find.fields.FilterOperator;
import com.autonomy.find.fields.FilterType;
import com.autonomy.find.fields.IdolField;
import com.autonomy.find.fields.ParaField;
import com.autonomy.find.processors.FieldDependentTagValuesProcessor;
import com.autonomy.find.processors.HierarchicalQueryResponseProcessor;
import com.autonomy.find.processors.StoreStateProcessor;
import com.autonomy.find.processors.TimelineDocCountLayersProcessor;
import com.autonomy.find.processors.TimelineLayersDeltaProcessor;
import com.autonomy.find.processors.TimelineLayersProcessor;
import com.autonomy.find.processors.TimelineValuesProcessor;
import com.autonomy.find.processors.TitleSummaryProcessor;
import com.autonomy.find.services.QuerySummaryClusters.Cluster;
import com.autonomy.find.services.QuerySummaryClusters.QuerySummaryResult;
import com.autonomy.find.services.search.ParametricValuesSearchExecutor;
import com.autonomy.find.services.search.ParametricValuesSearchExecutorVertica;
import com.autonomy.find.services.search.PrintFieldsResultIterator;
import com.autonomy.find.services.search.ReferenceResultsIterator;
import com.autonomy.find.services.search.SearchResult;
import com.autonomy.find.util.FieldTextDetail;
import com.autonomy.find.util.FieldTextUtil;
import com.autonomy.find.util.FieldUtil;
import com.autonomy.find.util.JSON;
import com.autonomy.find.util.Queries;
import com.autonomy.find.util.RequestProcess;
import com.autonomy.find.util.SearchResultComparator;
import com.autonomy.find.util.SearchResultComparatorVertica;
import com.autonomy.find.util.StateResult;
import com.autonomy.vertica.fields.IndexFile;
import com.autonomy.vertica.service.SearchVerticaService;
import com.autonomy.vertica.templates.FilterTemplate;
import com.googlecode.ehcache.annotations.Cacheable;

@Service
public class SearchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchService.class);

    private static final String LINK_FORMAT = "<a href=\"FindNews://%s\">%s</a>";

    private static final String CONCEPT_ID = "CID";
    private static final String CONCEPT_SID = "SID";

    private static final String PARAM_FIELDTEXT = "FieldText";


	public final DocumentBuilder documentBuilder;

	{
		try {
			DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
			dFactory.setNamespaceAware(true);
			
			documentBuilder = dFactory.newDocumentBuilder();
		} catch (final ParserConfigurationException pce) {
			throw new Error("Unable to create an XML DocumentBuilderFactory", pce);
		}
	}

	@Value("${discoverSearch.networkmap.skipPadMultiplier}")
	private int skipPadMultiplier;

    @Autowired
    private AciService searchAciService;

    @Autowired
    private IdolAnnotationsProcessorFactory processorFactory;

    @Autowired
    private SearchConfig config;

    @Autowired
    private CensorService censor;

    @Autowired
    private ParametricService parametricService;

	@Value("${discoverSearch.viewDoc.xPath.docRoot}")
 	private String xDocRootPath;

	@Value("${discoverSearch.viewDoc.xPath.conceptPlusRef}")
 	private String conceptPlusRefPath;

    @Value("${discoverSearch.viewDoc.xPath.conceptRef}")
    private String conceptRefPath;
    
    @Value("${discoverSearch.viewDoc.documentsRootDirectory}")
    private String documentRootDirectory;
    
    @Value("${discoverSearch.viewDoc.documentFileExtension}")
    private String documentFileExtension;
    
    @Value("${discoverSearch.viewDoc.xPath.templateDocRoot}")
    private String verticaXMLPath;
    private String umiaRefPath="//OT_UIMA";
    private String umiaConceptRefPath="//.";
    private String uimaHighlightRefPath="//OT_UIMA/annotator:HIGHLIGHT";
    	

    @Value("${discoverSearch.tableview.maxFieldPairValues}")
    private int MAX_FIELD_PAIR_VALUES;

    @Value("${discoverSearch.timeline.maxValues}")
    private int MAX_TIMELINE_PAIR_VALUES;

    private XPathExpression xDocRoot, xConceptPlusRef, xConceptRef,xUIMARef,xUimaConceptRef,xUiimaHighlightRef,xVerticaDoc;
 ;

    @Autowired
    private DocumentFolderService documentFolderService;

    @Autowired
    private ExecutorService searchExecutorService;
    
    @Autowired
    private FilterTemplate filterTemplate;
    
    @Autowired
    private SearchVerticaService verticaService;
    

	@PostConstruct
	private void postConstruct() {
		try {
			final XPathFactory xPathFactory = XPathFactory.newInstance();
			final XPath xPath = xPathFactory.newXPath();
			xDocRoot = xPath.compile(xDocRootPath);
            xConceptPlusRef = xPath.compile(conceptPlusRefPath);
            xConceptRef = xPath.compile(conceptRefPath);
            xUIMARef= xPath.compile(umiaRefPath);
            xUimaConceptRef=xPath.compile(umiaConceptRefPath);
            xUiimaHighlightRef=xPath.compile(uimaHighlightRefPath);
            xVerticaDoc=xPath.compile(verticaXMLPath);
            
		} catch (final XPathExpressionException xpe) {
			throw new Error("Invalid Xpath configured", xpe);
		}
	}


	// Copied from Voronoi
    private static boolean isStarOrEmpty(final String query) {
        return StringUtils.isBlank(query) || query.trim().equals("*");
    }

    /**
     * Performs a search
     *
     * Submits a search query to Idol and produces a list of search result items
     * from the response
     *
     * @param queryText
     * @return
     */
    @Cacheable(cacheName="SearchService.search_results")
    public List<ResultDocument> searchResults(final String queryText) {

        return searchFilteredResults(new SearchRequestData(queryText, new HashMap<>()));
    }


    /**
     * Performs a search
     *
     * Submits a search query to Idol and produces a list of search result items
     * from the response
     *
     * @param requestData
     * @return
     */
    @Cacheable(cacheName="SearchService.search_results")
    public List<ResultDocument> searchFilteredResults(final SearchRequestData requestData) {

        final LinkedList<ResultDocument> results = new LinkedList<ResultDocument>();
        for (final String database : config.getDatabases().split(",")) {
            results.addAll(searchResultsDB(requestData, database, config.getMaxSearchResults()));
        }
        Collections.sort(results);
        return results;
    }

    public org.w3c.dom.Document getLinkedContent(final String linkDb,
                                   final String linkId,
                                   final String linkQParams,
                                   final String securityInfo)
    {
        final AciParameters params = new AciParameters("getcontent");
        params.add("databasematch", linkDb);
      	params.add("reference", linkId);

        if(StringUtils.isNotBlank(securityInfo)) {
            params.add("securityinfo", securityInfo);
        }

        final List<AciParameter> extraParams = Queries.queryToAciParams(linkQParams);

        if (!extraParams.isEmpty()) {
            params.addAll(extraParams);
        }

        final org.w3c.dom.Document content =  searchAciService.executeAction(params, new DocumentProcessor());



        return content;

    }


    /**
     * Performs a search
     *
     * Submits a search query to Idol and produces a list of search result items
     * from the response
     *
     * @param queryText
     * @return
     */
    @Cacheable(cacheName="SearchService.search_results")
    public List<ResultDocument> searchResultsDoc(final String queryText) {
		// TODO: confirm that this can be deleted with the phone team, it's not used in the UI anymore
        return searchFilteredResultsDoc(new SearchRequestData(queryText, new HashMap<>())).getDocs();
    }

  
    
    
	private Element populateParents(String conceptID,String categoryName,org.w3c.dom.Document rootDoc, Element relTree,Map<String, Element> relMap,Map<String, List<String>> relParentMap,Map<String, Node> relNodeMap) {
		

		// Create a element for this node
		Element relEl = relMap.get(categoryName);
		if (relEl == null) {
			relEl = rootDoc.createElement("relElement");		
			relEl.setAttribute("title", categoryName.toUpperCase());
			relMap.put(categoryName, relEl);
		}
		
		List<String> pIDs=relParentMap.get(conceptID);
		if (pIDs==null) {
			// Add concept to the root element if not there already
			relTree.appendChild(relEl);
			
		} else {
			// Create all necessary parent relationship element nodes up to the trunk
			for (String p:pIDs) {
				// look up UIMA node
				Node node=relNodeMap.get(p);
				if (p!=null) {
					String pName = (node.hasAttributes() ? node.getAttributes().getNamedItem("destDesc").getNodeValue() : "");
					Element pEl=populateParents(p,pName,rootDoc,relTree,relMap,relParentMap,relNodeMap);
					// Add current node to the parent
					pEl.appendChild(relEl);
				}
			}	
		}
		return relEl;
	}

	public Node viewDoc(final String database,
            final String searchView,
            final String reference,
            final String links,
            final String loginUser,
            final String securityInfo,
            final String[] selectedNodes) throws Exception {

		final String docRefField = config.getSearchViews().get(searchView).getDocRefField();
		final AciParameters params = new AciParameters("getcontent");
		params.add("databasematch", database);
		params.add("reference", reference);
		params.add("referencefield", docRefField);
		if (StringUtils.isNotBlank(links) && !"*".equals(links))  {
		params.add("links", links);
		params.add("boolean", true);
		params.add("starttag", "<span class=\"autn-highlight\">");
		params.add("endtag", "</span>");
		params.add("highlight", "terms");
		}
		
		if (StringUtils.isNotBlank(securityInfo)) {
		params.add("securityinfo", securityInfo);
		}
        
        Element document = null;
        //final Element document = (Element) xDocRoot.evaluate(searchAciService.executeAction(params, new DocumentProcessor()), XPathConstants.NODE);
        if(config.getSearchViews().get(searchView).getRepository().equalsIgnoreCase("IDOL")) {
        	         	
        	document = (Element) xDocRoot.evaluate(searchAciService.executeAction(params, new DocumentProcessor()), XPathConstants.NODE);
        	
        } else {
        	// This is Vertica section
        	org.w3c.dom.Document doc = null;
			try {			
				
							
				try {					
					String source = config.getSearchViews().get(searchView).getDocumentSource();
					if(source != null && !source.isEmpty()) {
						switch(source.toUpperCase()) {
							case "IDOL":
								// the database name has to be overwritten here with the correct DB name			
								params.put("databasematch", config.getSearchViews().get(searchView).getDatabase());
								params.remove("referencefield");
								// get the tagged document from IDOL for the highlighting etc..
								LOGGER.debug("Get document # " + reference + " from IDOL");
								document = (Element) xDocRoot.evaluate(searchAciService.executeAction(params, new DocumentProcessor()), XPathConstants.NODE);
								break;
							case "FILE":
							case "FILESYSTEM":
								// Call Vertica to get the offset/index and file names
								// Table = <view>_otindex
								// select * from view_otindex where view = <viewname>
								IndexFile fileNameAndOffset = verticaService.fetchFileNameAndOffsetFromVertica(searchView, reference);
								if(fileNameAndOffset != null) {
									String absoluteFilePath = getAbsoluteFilePath(documentRootDirectory, fileNameAndOffset.getFilename());
									
									// Read the compressed file
									// Use the offset/index to unpack the doc
									byte[] bytes = readCompressedFile(absoluteFilePath, documentFileExtension, fileNameAndOffset.getStart(), fileNameAndOffset.getBytes());
									if(bytes != null) {									
										LOGGER.debug("Got document # " + reference + " from " + absoluteFilePath);
										DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
										factory.setNamespaceAware(false);									
										doc =  factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
										document = (Element) xVerticaDoc.evaluate(doc, XPathConstants.NODE);
									}	
								}
								break;
							default:
								LOGGER.debug("Unknown document source configured in properties for " + searchView);
								break;
						}								
					}
				} catch(Exception e) {
					LOGGER.error(e.getMessage());					
				}
				if(document == null) {
					LOGGER.debug("Document# " + reference + " not found in IDOL/FileSystem. Trying to find it in Vertica");
					doc = verticaService.getDocumentRecord(reference, docRefField, searchView);
					document = (Element) xVerticaDoc.evaluate(doc, XPathConstants.NODE);
					// document.setAttribute("message", "Tagged document not found!!! Displaying untagged version. ");
					/*if(LOGGER.isDebugEnabled()) {
				   	 	TransformerFactory tf = TransformerFactory.newInstance();
				           Transformer t;
							try {
								t = tf.newTransformer();
								StringWriter buffer = new StringWriter();
								DOMSource source = new DOMSource(document);
					            StreamResult result = new StreamResult(buffer);
					            t.transform(source, result);
					            String xml = buffer.toString();
					            LOGGER.debug(xml);
							} catch (TransformerConfigurationException e) {
								
							} catch (TransformerException e) {					
								
							}
					}*/
				}
			} catch (ParserConfigurationException | SAXException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}    	 	
        }
		
		if (censor.censor(document)) {
			final org.w3c.dom.Document redacted = documentBuilder.newDocument();
			
			Element redatedElement = redacted.createElement("redacted");
			redatedElement.setAttribute("folders", JSON.toJSON(Collections.<String>emptyList()));
			redatedElement.setAttribute("reference", reference);
			redacted.appendChild(redatedElement);
			
			return redacted;
		}
		
		
		// Get the UIMA annotations
		final NodeList uimaNodeList=(NodeList) xUIMARef.evaluate(document,XPathConstants.NODESET);
		if (uimaNodeList!=null && uimaNodeList.getLength()>0) {
			

			final Node uimaNode=uimaNodeList.item(0);
			final String uvalue=uimaNode.getTextContent();
			InputStream ins=new ByteArrayInputStream(uvalue.getBytes("UTF-8"));		
			org.w3c.dom.Document uimadoc=null;
			try {
				uimadoc = documentBuilder.parse(ins);
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			// Find the Highlight annotation and make it the document to send to client
			
			final NodeList uimaConceptsList=(NodeList) xUimaConceptRef.evaluate(uimadoc,XPathConstants.NODESET);
	
			for (int i = 0; i < uimaConceptsList.getLength(); i++) {
				final Node node = uimaConceptsList.item(i);
				if (node.getNodeName().equals("annotator:HIGHLIGHT")) {
					String hdocStr=(node.hasAttributes() ? node.getAttributes().getNamedItem("highlightDoc").getNodeValue() : "");
					InputStream hdocstream=new ByteArrayInputStream(hdocStr.getBytes("UTF-8"));		
					org.w3c.dom.Document hdoc=null;
					try {
						hdoc = documentBuilder.parse(hdocstream);
						document=hdoc.getDocumentElement();	
						break;
					} catch (SAXException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}			
				}
			}
					
			try {
				
				JSONArray conceptList = new JSONArray(); 
				JSONArray negList=new JSONArray();
				JSONArray contextList=new JSONArray();
				JSONArray relList=new JSONArray();
				
				for (int i = 0; i < uimaConceptsList.getLength(); i++) {
					final Node node = uimaConceptsList.item(i);
					if (node.getNodeName().equals("annotator:CONCEPT")) {
						final Node categoryIDNode = node.hasAttributes() ? node.getAttributes().getNamedItem("xmi:id") : null;
						
						if (categoryIDNode == null) {
						    LOGGER.error("Concept node [" +  "] has no '" + "id" + "' attribute.");
						    continue;
						}
						
						final String hid=	(node.hasAttributes() ? node.getAttributes().getNamedItem("xmi:id").getNodeValue() : "0");				
						final String name = (node.hasAttributes() ? node.getAttributes().getNamedItem("desc").getNodeValue() : "");
						final String ctype = (node.hasAttributes() ? node.getAttributes().getNamedItem("ctype").getNodeValue() : "none/none");
						final String source = (node.hasAttributes() ? node.getAttributes().getNamedItem("source").getNodeValue() : "(None)");
						final int lastIdx = name.lastIndexOf(" (");
						final String categoryName = lastIdx>0?name.substring(0, lastIdx):name;
						final String hier = lastIdx>0?name.substring(lastIdx + 2, name.length() - 1):"Other";
		
						final int firsttype = ctype.indexOf("/");
						final String type = firsttype>0?ctype.substring(firsttype+1, ctype.length() - 1):"(None)";
						final String begin=	(node.hasAttributes() ? node.getAttributes().getNamedItem("begin").getNodeValue() : "0");				
						final String end=	(node.hasAttributes() ? node.getAttributes().getNamedItem("end").getNodeValue() : "0");				
						final String categoryID = categoryIDNode.getNodeValue();
						final String text = (node.hasAttributes() ? node.getAttributes().getNamedItem("text").getNodeValue() : "(None)");
						
						
						
						JSONObject json = new JSONObject();
						json.put("hid", hid);
						json.put("id", categoryID);
						json.put("name", name);
						json.put("dname", categoryName);
						json.put("ctype", ctype);
						json.put("hier", hier);
						json.put("source", source);
						json.put("begin", begin);
						json.put("end", end);
						json.put("text", text);												
						conceptList.put(json);	
						
					} else if (node.getNodeName().equals("annotator:NEG")) {
						final String hid=	(node.hasAttributes() ? node.getAttributes().getNamedItem("xmi:id").getNodeValue() : "0");				
						final String ntype = (node.hasAttributes() ? node.getAttributes().getNamedItem("negationType").getNodeValue() : "(None)");
						final String text = (node.hasAttributes() ? node.getAttributes().getNamedItem("text").getNodeValue() : "(None)");
						final String begin=	(node.hasAttributes() ? node.getAttributes().getNamedItem("begin").getNodeValue() : "0");				
						final String end=	(node.hasAttributes() ? node.getAttributes().getNamedItem("end").getNodeValue() : "0");				
						
						JSONObject json = new JSONObject();
						json.put("hid", hid);
						json.put("id", text);
						json.put("name", text);
						json.put("negationType", ntype);
						json.put("begin", begin);
						json.put("end", end);
						json.put("text", text);												
						negList.put(json);	
									
					} else if (node.getNodeName().equals("annotator:CONTEXT")) {
						final String hid=	(node.hasAttributes() ? node.getAttributes().getNamedItem("xmi:id").getNodeValue() : "0");				
						final String value = (node.hasAttributes() ? node.getAttributes().getNamedItem("value").getNodeValue() : "(None)");
						final String cType = (node.hasAttributes() ? node.getAttributes().getNamedItem("ctype").getNodeValue() : "(None)");
						final String hints=	(node.hasAttributes() ? node.getAttributes().getNamedItem("hints").getNodeValue() : "");				
						final String appliesTo=	(node.hasAttributes() ? node.getAttributes().getNamedItem("appliesTo").getNodeValue() : "");							
		
						JSONObject json = new JSONObject();
						json.put("hid", hid);
						json.put("id", cType+"_"+hid);
						json.put("name", cType);
						json.put("value", value);
						json.put("cType", cType);
						json.put("appliesTo", appliesTo);
						json.put("hints", hints);	
						json.put("text", cType);
						
						contextList.put(json);	
					}  else if (node.getNodeName().equals("annotator:RELATION")) {
						final String hid=	(node.hasAttributes() ? node.getAttributes().getNamedItem("xmi:id").getNodeValue() : "0");				
						final String destDesc = (node.hasAttributes() ? node.getAttributes().getNamedItem("destDesc").getNodeValue() : "(None)");
						final String destID = (node.hasAttributes() ? node.getAttributes().getNamedItem("destID").getNodeValue() : "(None)");
						final String relDesc = (node.hasAttributes() ? node.getAttributes().getNamedItem("relDesc").getNodeValue() : "(None)");
						final String relID = (node.hasAttributes() ? node.getAttributes().getNamedItem("relID").getNodeValue() : "(None)");
						final String sources= (node.hasAttributes() ? (node.getAttributes().getNamedItem("sources")!=null ? node.getAttributes().getNamedItem("sources").getNodeValue() : ""):"");				
		
						if (!sources.equals("")) {
							JSONObject json = new JSONObject();
							json.put("hid", hid);
							json.put("id", destID+"_"+hid);
							json.put("name", destDesc);
							json.put("relID", relID);
							json.put("sources", sources);
							json.put("relDesc", relDesc);
							json.put("text", destDesc);
							
							relList.put(json);	
						}
					} 
				}
				
				// Add Concept Elements to page
				document.setAttribute("conceptData", conceptList.toString());
				
				// 		Add Features:Negation
				document.setAttribute("negData", negList.toString());
				
			   	// 		Add Features:Related Concepts 
				document.setAttribute("relData", relList.toString());
				
				
				// 		Add Features:Context
				document.setAttribute("contextData", contextList.toString());
			    System.out.print(conceptList);
			    System.out.flush();
				
		    
		    } catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
	
			LOGGER.debug("Reference ["+ reference + "] concepts count: [" + uimaConceptsList.getLength() +"].");
			
			//final Element highlightDoc = rootDoc.createElement("highlightDoc");
			//document.appendChild(highlightDoc);
			
			// Generate Concepts from UIMA annotations
		}
		List<SavedFiltersFolder> folders = new ArrayList<SavedFiltersFolder>();
		try {	
			folders = documentFolderService.getFoldersByRef(database, reference, loginUser);
		} catch(Exception e) {
			LOGGER.error("ERROR getting folders for [" + reference + "], " + e.getMessage());
			
		}
			if(selectedNodes != null && selectedNodes.length > 0) {				
				document.setAttribute("selectedNodes", StringUtils.join(selectedNodes, ","));
			}
			document.setAttribute("folders", JSON.toJSON(folders));
			document.setAttribute("reference", reference);
		
		
		// TODO remove after testing - just for printing the final XML
		/*if(LOGGER.isDebugEnabled()) {
	   	 	TransformerFactory tf = TransformerFactory.newInstance();
	           Transformer t;
				try {
					t = tf.newTransformer();
					StringWriter buffer = new StringWriter();
					DOMSource source = new DOMSource(document);
		            StreamResult result = new StreamResult(buffer);
		            t.transform(source, result);
		            String xml = buffer.toString();
		            LOGGER.debug(xml);
				} catch (TransformerConfigurationException e) {
					
				} catch (TransformerException e) {					
					
				}
		}*/
		
		return document;
	}
	
	/*protected String generateFilePath(String rootDirectory, String searchView, String reference, int subDirectoryIndex, String fileExtension) {
		Path filePath = Paths.get(rootDirectory, searchView);		
		if(reference != null && !reference.isEmpty()) {			
			// remove all invalid characters from the file name for various OS to maintain consistency
			String referenceReplaced = StringUtils.replaceEach(reference, new String[]{"<", ">", "\"", "?", "*", "/", "|", ":", "\\", "-"},
						new String[] {"_", "_", "_", "_", "_", "_", "_", "_", "_", "_"});
			int index = 0;
			while (index < referenceReplaced.length()) {
				String innerDirectoryName = referenceReplaced.substring(index, Math.min(index + subDirectoryIndex, referenceReplaced.length()));
				if(innerDirectoryName != null) {
					filePath = filePath.resolve(innerDirectoryName);
				}
			    index += subDirectoryIndex;
			}					
			String fileName = referenceReplaced + "." + fileExtension;		
			filePath = filePath.resolve(fileName);
		}		
		filePath = filePath.normalize();	// remove redundancies like /. or /..
		return filePath.toString();
	}*/
	
	protected String getAbsoluteFilePath(String rootDirectory, String fileNamePath) {
		Path filePath = Paths.get(rootDirectory, fileNamePath);		
		filePath = filePath.normalize();	// remove redundancies like /. or /..
		return filePath.toString();
	}
	
	protected byte[] readCompressedFile(String fileName, String fileExtension, int offset, int bytes) {
		GZIPInputStream zStream = null; 
		 //ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
	        if (!fileName.endsWith(fileExtension)) {
	        	fileName = fileName + "." + fileExtension;
	        }
            File file = new File(fileName);
            FileInputStream fis = new FileInputStream(file);            
            // int end = bytes + offset;	        
            int readBufferSize = 64 * 1024; // 64KB
            zStream = new GZIPInputStream(fis, readBufferSize); 
            
            LOGGER.debug(" Reading [" + fileName + "]");
            // LOGGER.debug(" Reading [" + fileName + "] from [" + offset + "] to [" + end + "]" );
            byte[] buffer = org.apache.commons.io.IOUtils.toByteArray(zStream);
           
            
           /* byte[] buff = new byte[readBufferSize];
            int bytesRead;
           
            while ((bytesRead = zStream.read(buff)) > 0)
            {
                output.write(buff, 0, bytesRead);
            }
            LOGGER.debug(" Read [" + output.size() + "] bytes" );         
           // byte[] sliced = Arrays.copyOfRange(output.toByteArray(), offset, end);
           // LOGGER.debug(" Extracted [" + sliced.length + "] bytes from [" + offset + "] to [" + end + "]" );
            if(output != null) {
            	output.close();
            }*/
            if(zStream != null) { 
            	zStream.close();
            }
            
            return buffer;            
        } catch(Exception e) {
        	LOGGER.error("ERROR reading [" + fileName + "]:" + e.getMessage());        	
        } finally {
        	try {
        		/*if(output != null) {
                	output.close();
                }*/
        		if(zStream != null) {
        			zStream.close();
        		}
			} catch (IOException e) {
				// ignore
			}
        }
        return null;
	}
	
	protected Node getNode(String tagName, NodeList nodeList) {		
		for(int index = 0; index < nodeList.getLength(); index++) {
			Node node = nodeList.item(index);
			if(node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equalsIgnoreCase(tagName)) {
				return node;
			}
			if (node.hasChildNodes()) {
				getNode(tagName, node.getChildNodes());
			}
		}
		return null;
	}

	
	
    public Results searchFilteredResultsDoc(
            final SearchRequestData requestData
    ) {
        //final LinkedList<ResultDocument> results = new LinkedList<>();
		//final Map<String, Integer> counts = new HashMap<>();
        final String database = config.getDatabase(requestData.getSearchView());

        return searchResultsDBDoc(requestData, database, config.getMaxSearchResults());
        /*
		for (final String database : databases.split(",")) {
			final Results dbResults = searchResultsDBDoc(requestData, database, config.getMaxSearchResults());
			results.addAll(dbResults.getDocs());
			counts.putAll(dbResults.getTotalResults());
        }
        Collections.sort(results);
        return new Results(results, counts);
        */
    }

    private List<FieldPair> parametricPairsForFilteredSearch(final SearchRequestData requestData,
                                                             final String xField,
                                                             final List<FieldValue> xValues,
                                                             final String yField,
                                                             final List<FieldValue> yValues,
                                                             final Map<String, FilterField> filterFields) {
        final List<String> fieldnames = new ArrayList<>();

        final String database = config.getDatabase(requestData.getSearchView());

        final AciParameters params = addSearchParams(new AciParameters("getquerytagvalues"), requestData, database, null, false);

        if (xField != null) {
            final String xParaFieldname = getParametricNameFromFilter(xField, requestData.getSearchView());
            fieldnames.add(xParaFieldname);

            final FieldText xFieldText = FieldTextUtil.buildMatchValues(xParaFieldname, xValues, false, filterFields.get(xField));
            FieldTextUtil.appendFieldText(params, xFieldText);
        }

        if (yField != null) {
            final String yParaFieldname = getParametricNameFromFilter(yField, requestData.getSearchView());
            fieldnames.add(yParaFieldname);

            final FieldText yFieldText = FieldTextUtil.buildMatchValues(yParaFieldname, yValues, false, filterFields.get(yField));
            FieldTextUtil.appendFieldText(params, yFieldText);
        }


        params.add("fieldname", StringUtils.join(fieldnames, ','));
        params.add("documentcount", true);
        params.add("FieldDependence", true);
        params.add("sort", "documentcount");

        if (MAX_FIELD_PAIR_VALUES >= 0) {
            params.add("maxvalues", MAX_FIELD_PAIR_VALUES);
        }

        return searchAciService.executeAction(params, new FieldDependentTagValuesProcessor());
    }
    
    private List<FieldPair> parametricPairsForFilteredSearchVertica(final SearchRequestData requestData,
            final String xField,
            final List<com.autonomy.vertica.fields.FieldValue> xValues,
            final String yField,
            final List<com.autonomy.vertica.fields.FieldValue> yValues,
            final Map<String, FilterField> filterFields
           ) throws Exception {
    	
			final List<String> fieldnames = new ArrayList<>();
			
			final String database = config.getDatabase(requestData.getSearchView());
			
			final AciParameters params = addSearchParams(new AciParameters("getquerytagvalues"), requestData, database, null, false);
			
			if (xField != null) {
			final String xParaFieldname = getParametricNameFromFilter(xField, requestData.getSearchView());
			fieldnames.add(xParaFieldname);
			
			/*final FieldText xFieldText = FieldTextUtil.buildMatchValues(xParaFieldname, xValues, false, filterFields.get(xField));
			FieldTextUtil.appendFieldText(params, xFieldText);*/
			}
			
			if (yField != null) {
			final String yParaFieldname = getParametricNameFromFilter(yField, requestData.getSearchView());
			fieldnames.add(yParaFieldname);
			
			/*final FieldText yFieldText = FieldTextUtil.buildMatchValues(yParaFieldname, yValues, false, filterFields.get(yField));
			FieldTextUtil.appendFieldText(params, yFieldText);*/
			}
			
			
			params.add("fieldname", StringUtils.join(fieldnames, ','));
			params.add("documentcount", true);
			params.add("FieldDependence", true);
			params.add("sort", "documentcount");
			
			if (MAX_FIELD_PAIR_VALUES >= 0) {
			params.add("maxvalues", MAX_FIELD_PAIR_VALUES);
			}
			
			//return searchAciService.executeAction(params, new FieldDependentTagValuesProcessor());
			return verticaService.getTableViewerData(params, requestData);
		}

      

    @Cacheable(cacheName="SearchService.search_fields")
   	public List<Field> aggregateParametricValuesForFilteredSearch(
               final SearchRequestData requestData,
               final int maxValues,
               final List<String> filteredFieldNames,
               final FieldText fieldText,
               final boolean restrictedvalues,
               final boolean rename,
               final String sort,
               final boolean singleQueryField
    ){
        final Map<String,Map<String, FilterField>> paraFieldNames = parametricService.getCategorizedParaFieldNames(requestData.getSearchView());
        final Map<String, FilterField> aggregatedFieldsMap = paraFieldNames.get(parametricService.AGGREGATED_FIELD_KEY);
        final Map<String, FilterField> nonAggregatedFieldsMap = paraFieldNames.get(parametricService.NON_AGGREGRATED_FIELD_KEY);
        final List<String> aggregatedList = new ArrayList<String>();
        final List<String> nonAggregatedList = new ArrayList<String>();
        final Map<String, ParaField> paraFieldMap = new HashMap<String, ParaField>();

        if (filteredFieldNames != null && !filteredFieldNames.isEmpty())
        {
            for (final String filteredName : filteredFieldNames) {
                final String paraFieldName = getParametricNameFromFilter(filteredName, requestData.getSearchView());

                if (paraFieldName == null) {
                    throw new IllegalArgumentException("Illegal fieldname [" + filteredName + "] specified");
                }

                if (nonAggregatedFieldsMap.containsKey(paraFieldName)) {
                    nonAggregatedList.add(paraFieldName);
                    paraFieldMap.put(paraFieldName, nonAggregatedFieldsMap.get(paraFieldName).getParametric());
                } else if (aggregatedFieldsMap.containsKey(paraFieldName)) {
                    aggregatedList.add(paraFieldName);
                    paraFieldMap.put(paraFieldName, aggregatedFieldsMap.get(paraFieldName).getParametric());
                } else {
                    throw new IllegalArgumentException("Illegal para fieldname [" + paraFieldName + "] specified");
                }
            }

        } else {
            for(final Map.Entry<String, FilterField> entry : nonAggregatedFieldsMap.entrySet()) {
                nonAggregatedList.add(entry.getKey());
                paraFieldMap.put(entry.getKey(), entry.getValue().getParametric());
            }

            for(final Map.Entry<String, FilterField> entry : aggregatedFieldsMap.entrySet()) {
                aggregatedList.add(entry.getKey());
                paraFieldMap.put(entry.getKey(), entry.getValue().getParametric());
            }
        }

        final ExecutorCompletionService<SearchResult<List<Field>>> ecs = new ExecutorCompletionService<SearchResult<List<Field>>>(searchExecutorService);

        int jobs = 0;
        if (!nonAggregatedList.isEmpty()) {
            if (!singleQueryField) {
                ecs.submit(new ParametricValuesSearchExecutor(jobs, requestData, maxValues, nonAggregatedList, fieldText, restrictedvalues, rename, sort, false, null, this, paraFieldMap));
                jobs++;
            } else {
                for(final String fieldname : nonAggregatedList) {
                    ecs.submit(new ParametricValuesSearchExecutor(jobs, requestData, maxValues, Collections.singletonList(fieldname), fieldText, restrictedvalues, rename, sort, false, null, this, paraFieldMap));
                    jobs++;
                }

            }

        }

        if (!aggregatedList.isEmpty()) {
            for(final String fieldname : aggregatedList) {
                ecs.submit(new ParametricValuesSearchExecutor(jobs, requestData, maxValues, Collections.singletonList(fieldname), fieldText, restrictedvalues, rename, sort, true, fieldname, this, paraFieldMap));
                jobs++;
            }
        }

        final Set<SearchResult<List<Field>>> resultSet = new TreeSet<SearchResult<List<Field>>>(new SearchResultComparator());
        try {
            for (int i = 0; i < jobs; i++) {
                final SearchResult<List<Field>> result = ecs.take().get();
                if (result != null) {
                    resultSet.add(result);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        final List<Field> paraFields = new ArrayList<Field>();
        final Iterator<SearchResult<List<Field>>> it = resultSet.iterator();

        final Map<String, FilterField> paraFieldsMap = new HashMap<String, FilterField>();
        paraFieldsMap.putAll(aggregatedFieldsMap);
        paraFieldsMap.putAll(nonAggregatedFieldsMap);


        while (it.hasNext()) {
            final List<Field> fields = it.next().getResult();
            if (fields != null) {
                for(final Field field : fields) {
                    final FilterField paraFilterField = paraFieldsMap.get(field.getName());
                    if (paraFilterField != null) {
                        field.setName(paraFilterField.getName());
                    }
                }

                paraFields.addAll(fields);
            }
        }

        return paraFields;

    }


    private String getParametricNameFromFilter(final String filterName, final String view) {
        String paraFieldname = null;

        final Map<String, FilterField> filterFieldNames = parametricService.getFilterFieldNames(view);
        final FilterField filterField = filterFieldNames.get(filterName);
        if(config.getSearchViews().get(view).getRepository().equalsIgnoreCase("Vertica")) {
        	if(filterField != null)
        		paraFieldname = filterField.getName();
    	} else {
    		if (filterField != null && filterField.getParametric() != null) {
                paraFieldname = StringUtils.defaultString(filterField.getParametric().getName(), filterName);
            }
    	}
        return paraFieldname;
    }

    private String getDateNameFromFilter(final String filterName, final String view) {
        String dateFieldname = null;

        final Map<String, FilterField> filterFieldNames = parametricService.getFilterFieldNames(view);
        final FilterField filterField = filterFieldNames.get(filterName);
        if (filterField != null) {
        	final IdolField idolField=filterField.getIndexed();
        	if (idolField !=null && idolField.getType()==FilterType.DATE) {	
        		dateFieldname = StringUtils.defaultString(idolField.getName(), filterName);
        	}
        }
        return dateFieldname;
    }

	public  List<Field> parametricValuesForFilteredSearch(
            final SearchRequestData requestData,
            final int maxValues,
            final List<String> filteredFieldNames,
            final FieldText fieldText,
            final boolean restrictedvalues,
            final boolean rename,
            final String sort,
            final boolean isAggregated,
            final String aggregatedFieldname,
            final Map<String, ParaField> paraFieldMap
    ){
        if (filteredFieldNames == null || filteredFieldNames.isEmpty()) {
            throw new IllegalArgumentException("Missing field names parameter");
        }

        final String database = config.getDatabase(requestData.getSearchView());

		// this will have lots of spurious unrelated search-parameters, e.g. characters etc., they should be
		// safely ignored though

		final AciParameters params = addSearchParams(new AciParameters("getquerytagvalues"), requestData, database, null, false);
		params.add("documentcount", true);
		params.add("sort", sort);

		if (maxValues > 0) {
			params.add("maxvalues", maxValues);
		}

		if (restrictedvalues) {
			// Quote: 'Restricts tag values for fields in both the FieldText and FieldName parameters to
			// only those values that satisfy the FieldText'.
			// So we can ensure that we don't get any values except those specifically
			// (except any which might be returned as part of the base searchRequest)
			params.add("RestrictedValues", true);
		}

		FieldTextUtil.appendFieldText(params, fieldText);
        params.add("FieldName", StringUtils.join(filteredFieldNames, "+"));

        if (isAggregated) {
            params.add("merge", true);
        }

        addRangesParam(filteredFieldNames, paraFieldMap, params);

		final List<Field> fields = searchAciService.executeAction(params, new FieldListProcessor());
        if (isAggregated) {
            if (fields != null && !fields.isEmpty()) {
                fields.get(0).setName(aggregatedFieldname);
            }
        }
        final List<Field> viewFields = new ArrayList<Field>();
        final String idolRootElement = config.getSearchViews().get(requestData.getSearchView()).getIdolRootElement();
        final String viewFieldNamePrefix = FieldUtil.getSearchViewFieldnamePrefix(idolRootElement);

		for (final Field field : fields) {
            final String testFieldName = field.getName().replaceFirst(viewFieldNamePrefix, "");
            if (filteredFieldNames.contains(testFieldName)) {
                if (rename && !isAggregated) {
                    field.setName(testFieldName);
                }

                viewFields.add(field);
            }
		}

		return viewFields;
	}

    private AciParameters addRangesParam(final List<String> filteredFieldNames, final Map<String, ParaField> paraFieldMap, final AciParameters params) {
        if (filteredFieldNames == null || paraFieldMap ==  null) {
            return params;
        }

        final StringBuilder rangesBuilder = new StringBuilder();

        for(final String fieldname : filteredFieldNames) {
            final ParaField paraField = paraFieldMap.get(fieldname);
            if (paraField != null && paraField.getRanges() != null) {
                parametricService.buildParaRangesParam(rangesBuilder, paraField.getRanges(), fieldname);
            }

        }

        if (rangesBuilder.length() > 0) {
            params.add("Ranges", rangesBuilder.toString());
        }

        return params;
    }

	private List<FieldValue> filterFieldsByCategory(final int maxValues, final List<FieldValue> fields, final int skip){
//		if (skip <= 0) {
//			return fields;
//		}

		final Map<String, Integer> discardCounts = new HashMap<>();

		final List<FieldValue> toReturn = new ArrayList<>(fields.size());

		// implicit assumption that the fields are input in descending order of counts, so we discard the first few
		for (final FieldValue field : fields) {
			final String name = field.getValue();
			final int lastIdx = name.lastIndexOf(" (");
			final String category = name.substring(lastIdx + 2, name.length() - 1);
			Integer discarded = discardCounts.get(category);

			// TODO: remove. this is a hack which they've asked for temporarily until they fix the data
			if (config.getCulledSuperCategories().contains(category)) {
				continue;
			}

			if (discarded == null) {
				discarded = 0;
				discardCounts.put(category, 0);
			}

			if (discarded < skip) {
				discardCounts.put(category, discarded + 1);
			}
			else {
				toReturn.add(field);

				if (toReturn.size() >= maxValues) {
					return toReturn;
				}
			}
		}

		return toReturn;
	}

    @Cacheable(cacheName="SearchService.search_tableview")
    public TableViewResponse tableView(
            final SearchRequestData requestData,
            final String xField,
            final String yField,
            final int nX,
            final int nY,
            final String sort,
            final String xFilterValue,
            final String yFilterValue,
            final boolean includeParent
    ) {
        final Map<String, FilterField> fieldNames = parametricService.getFilterFieldNames(requestData.getSearchView());

        if (!fieldNames.containsKey(xField) || !fieldNames.containsKey(yField)) {
            throw new IllegalArgumentException("Illegal fieldname specified");
        }

        // TODO: if re-support concept+ in table viewer, need to re-implement getSNOMEDChildren() as no longer use agentstore to index snomed docs.
        final CategoryNames xFieldRestrictions = StringUtils.isEmpty(xFilterValue) ? null : getSNOMEDChildren(xFilterValue, includeParent);
        final CategoryNames yFieldRestrictions = StringUtils.isEmpty(yFilterValue) ? null : getSNOMEDChildren(yFilterValue, includeParent);
        final boolean usingFieldRestrictions = xFieldRestrictions != null || yFieldRestrictions != null;

        if (usingFieldRestrictions) {
            // if we have the restrictions (SNOMED only), we need to add a filter to the request to restrict to results within the SNOMEDPARENTTAGS field
            final FilterGroup group = new FilterGroup();
            group.setBoolOperator(BoolOp.AND);

            final Map<String, List<FieldParams>> paraFilters = new HashMap<>();
            addFilters(paraFilters, xField, xFilterValue);
            addFilters(paraFilters, yField, yFilterValue);

            group.setFilterFields(paraFilters);

            group.setChildGroups(Arrays.asList(requestData.getFilterGroup()));

            requestData.setFilterGroup(group);
        }

        final List<Field> fields;

        if ((xFieldRestrictions == null) ^ (yFieldRestrictions == null)) {
            // one field is using restrictions, one is not. have to fetch them separately
            fields = new ArrayList<>(2);
            fields.addAll(aggregateParametricValuesForFilteredSearch(requestData, xFieldRestrictions == null ? nX : -1, Arrays.asList(xField), null, false, false, sort, false));
            fields.addAll(aggregateParametricValuesForFilteredSearch(requestData, yFieldRestrictions == null ? nY : -1, Arrays.asList(yField), null, false, false, sort, false));
        }
        else {
            // if we're using field restrictions for both, we need to fetch all possible values, then filter down to those within the parent category
            // if we're not using filters for either, we can just ask for the top nX or nY values
            final int max = usingFieldRestrictions ? -1 : Math.max(nX, nY);
            fields = aggregateParametricValuesForFilteredSearch(requestData, max, Arrays.asList(xField, yField), null, false, false, sort, false);
        }

        final TableViewResponse response = new TableViewResponse();

        if (fields.size() < 2) {
            // only one field, there's nothing to plot
            return response;
        }

        // order is preserved, so this should give us x and y fields
        final Field xFieldVal = fields.get(0);
        final Field yFieldVal = fields.get(1);
        final List<FieldValue> xValues = filterFieldValues(nX, xFieldVal, xFieldRestrictions);
        final List<FieldValue> yValues = filterFieldValues(nY, yFieldVal, yFieldRestrictions);

        final List<FieldPair> pairs = parametricPairsForFilteredSearch(requestData, xField, xValues, yField, yValues, fieldNames);

        final double[][] data = new double[yValues.size()][xValues.size()];
        final Map<String, Integer> xMap = indexMap(xValues);
        final Map<String, Integer> yMap = indexMap(yValues);

        for (final FieldPair pair : pairs) {
            final Integer xIdx = xMap.get(pair.getPrimaryValue());
            if (xIdx != null) {
                final Integer yIdx = yMap.get(pair.getSecondaryValue());
                if (yIdx != null) {
                    data[yIdx.intValue()][xIdx.intValue()] = pair.getCount();
                }
            }
        }

        response.setTable(data);
       // response.setXValues(xValues);
       // response.setYValues(yValues);
        
        response.setXValues(mapFieldValueVertica(xValues));
        response.setYValues(mapFieldValueVertica(yValues));

        return response;
    }
    
    @Cacheable(cacheName="SearchService.search_tableview")
    public TableViewResponse tableViewVertica(
            final SearchRequestData requestData,
            final String xField,
            final String yField,
            final int nX,
            final int nY,
            final String sort,
            final String xFilterValue,
            final String yFilterValue,
            final boolean includeParent
    ) throws Exception {
        final Map<String, FilterField> fieldNames = parametricService.getFilterFieldNames(requestData.getSearchView());

        if (!fieldNames.containsKey(xField) || !fieldNames.containsKey(yField)) {
        	LOGGER.error("Illegal fieldname found in TableViewer. Field: <"  + xField + "> or <" + yField + "> is not correctly configured!!!");
            throw new IllegalArgumentException("Illegal fieldname specified");
        }

        // TODO: if re-support concept+ in table viewer, need to re-implement getSNOMEDChildren() as no longer use agentstore to index snomed docs.
        final CategoryNames xFieldRestrictions = StringUtils.isEmpty(xFilterValue) ? null : getSNOMEDChildren(xFilterValue, includeParent);
        final CategoryNames yFieldRestrictions = StringUtils.isEmpty(yFilterValue) ? null : getSNOMEDChildren(yFilterValue, includeParent);
        final boolean usingFieldRestrictions = xFieldRestrictions != null || yFieldRestrictions != null;

        if (usingFieldRestrictions) {
            // if we have the restrictions (SNOMED only), we need to add a filter to the request to restrict to results within the SNOMEDPARENTTAGS field
            final FilterGroup group = new FilterGroup();
            group.setBoolOperator(BoolOp.AND);

            final Map<String, List<FieldParams>> paraFilters = new HashMap<>();
            addFilters(paraFilters, xField, xFilterValue);
            addFilters(paraFilters, yField, yFilterValue);

            group.setFilterFields(paraFilters);

            group.setChildGroups(Arrays.asList(requestData.getFilterGroup()));

            requestData.setFilterGroup(group);
        }

        final List<com.autonomy.vertica.fields.Field> fields;

        if ((xFieldRestrictions == null) ^ (yFieldRestrictions == null)) {
            // one field is using restrictions, one is not. have to fetch them separately
            fields = new ArrayList<>(2);
            fields.addAll(aggregateParametricValuesFromVertica(requestData, xFieldRestrictions == null ? nX : -1, Arrays.asList(xField), null, false, false, sort, false, null));
            fields.addAll(aggregateParametricValuesFromVertica(requestData, yFieldRestrictions == null ? nY : -1, Arrays.asList(yField), null, false, false, sort, false, null));
        }
        else {
            // if we're using field restrictions for both, we need to fetch all possible values, then filter down to those within the parent category
            // if we're not using filters for either, we can just ask for the top nX or nY values
            final int max = usingFieldRestrictions ? -1 : Math.max(nX, nY);
            fields = aggregateParametricValuesFromVertica(requestData, max, Arrays.asList(xField, yField), null, false, false, sort, false, null);
        }

        final TableViewResponse response = new TableViewResponse();

        if (fields.size() < 2) {
            // only one field, there's nothing to plot
            return response;
        }

        // order is preserved, so this should give us x and y fields
        final com.autonomy.vertica.fields.Field xFieldVal = fields.get(0);
        final com.autonomy.vertica.fields.Field yFieldVal = fields.get(1);
        final List<com.autonomy.vertica.fields.FieldValue> xValues = filterFieldValuesVertica(nX, xFieldVal, xFieldRestrictions);
        final List<com.autonomy.vertica.fields.FieldValue> yValues = filterFieldValuesVertica(nY, yFieldVal, yFieldRestrictions);

        final List<FieldPair> pairs = parametricPairsForFilteredSearchVertica(requestData, xField, xValues, yField, yValues, fieldNames);

        final double[][] data = new double[yValues.size()][xValues.size()];
        final Map<String, Integer> xMap = indexMapVertica(xValues);
        final Map<String, Integer> yMap = indexMapVertica(yValues);

        for (final FieldPair pair : pairs) {
            final Integer xIdx = xMap.get(pair.getPrimaryValue());
            if (xIdx != null) {
                final Integer yIdx = yMap.get(pair.getSecondaryValue());
                if (yIdx != null) {
                    data[yIdx.intValue()][xIdx.intValue()] = pair.getCount();
                }
            }
        }

        response.setTable(data);
        
       // response.setXValues(mapFieldValueVertica(xValues));
      //  response.setYValues(mapFieldValueVertica(yValues));
        
        response.setXValues(xValues);
        response.setYValues(yValues);

        return response;
    }
    
    private static List<com.autonomy.vertica.fields.FieldValue> mapFieldValueVertica(List<FieldValue> fieldValues) {
    	List<com.autonomy.vertica.fields.FieldValue> returnValue = new ArrayList<com.autonomy.vertica.fields.FieldValue>();
    	for(FieldValue value : fieldValues) {
    		if(value != null) {
    			returnValue.add(new com.autonomy.vertica.fields.FieldValue(value.getValue(), (double)value.getCount(), value.getDate()));
    		}
    	}
    	return returnValue;
    }

        
   private static void addFilters(final Map<String, List<FieldParams>> paraFilters, final String filterField, final String filterValue) {
        if (StringUtils.isEmpty(filterValue)) {
            return;
        }

        List<FieldParams> valuesParam = paraFilters.get(filterField);

        if (valuesParam == null) {
            valuesParam = new ArrayList<FieldParams>();
            paraFilters.put(filterField, valuesParam);
        }
        final FieldParams value = new FieldParams();
        value.setType('P');
        value.setOp(FilterOperator.IS);
        value.setVal(filterValue);

        valuesParam.add(value);
    }

    private CategoryNames getSNOMEDChildren(final String snomedValue, final boolean includeParent) {

        //TODO: need different implemenation as no longer use agentstore to index snomed docs.
        return null;
        /*
        final AciParameters params = new AciParameters("query");
        params.add("fieldtext", new MATCH("DRETITLE", snomedValue));
        params.add("databasematch", "activated");
        params.add("print", "fields");
        params.add("printfields", "childname");
        params.add("maxresults", 1);

        final List<CategoryNames> response = viewDocAgentstoreAciService.executeAction(params, processorFactory.listProcessorForClass(CategoryNames.class));

        final CategoryNames children = response.isEmpty() ? new CategoryNames() : response.get(0);

        if (includeParent) {
            // include the parent category as well as the children for comparison purposes
            children.addCategory(snomedValue);
        }

        return children;
        */
    }

    private List<com.autonomy.vertica.fields.FieldValue> filterFieldValuesVertica(final int max, final com.autonomy.vertica.fields.Field field, final Set<String> fieldRestrictions) {
        final List<com.autonomy.vertica.fields.FieldValue> values = field.getFieldValues();

        if (fieldRestrictions == null) {
            // if we have no filter, we'll just the first ${max} results
            return new ArrayList<>(values.subList(0, Math.min(values.size(), max)));
        }

        final List<com.autonomy.vertica.fields.FieldValue> toReturn = new ArrayList<>(max);

        int count = 0;

        for (final com.autonomy.vertica.fields.FieldValue value : field.getFieldValues()) {
            if (fieldRestrictions.contains(value.getValue())) {
                toReturn.add(value);
                ++count;

                if (count >= max) {
                    break;
                }
            }
        }

        return toReturn;
    }
    
    private ArrayList<FieldValue> filterFieldValues(final int max, final Field field, final Set<String> fieldRestrictions) {
        final List<FieldValue> values = field.getFieldValues();

        if (fieldRestrictions == null) {
            // if we have no filter, we'll just the first ${max} results
            return new ArrayList<>(values.subList(0, Math.min(values.size(), max)));
        }

        final ArrayList<FieldValue> toReturn = new ArrayList<>(max);

        int count = 0;

        for (final FieldValue value : field.getFieldValues()) {
            if (fieldRestrictions.contains(value.getValue())) {
                toReturn.add(value);
                ++count;

                if (count >= max) {
                    break;
                }
            }
        }

        return toReturn;
    }

    private static Map<String, Integer> indexMap(final List<FieldValue> values) {
        final Map<String, Integer> valueToIndexMap = new HashMap<>();
        int idx = 0;

        for (final FieldValue fieldValue : values) {
            valueToIndexMap.put(fieldValue.getValue(), idx++);
        }

        return valueToIndexMap;
    }
    
    private static Map<String, Integer> indexMapVertica(final List<com.autonomy.vertica.fields.FieldValue> values) {
        final Map<String, Integer> valueToIndexMap = new HashMap<>();
        int idx = 0;

        for (final com.autonomy.vertica.fields.FieldValue fieldValue : values) {
            valueToIndexMap.put(fieldValue.getValue().trim(), idx++);
        }

        return valueToIndexMap;
    }

    @Cacheable(cacheName="SearchService.search_networkmap")
	public Map<String, List<FieldValue>> networkMap(
			final SearchRequestData requestData,
			final int maxValues,
			final String fieldname,
			final int skip) {
		final Map<String, FieldTextDetail> fieldNames = null; //TODO: FIXMEME:  parametricService.getParaFieldNames(requestData.getSearchView());

		if (!fieldNames.containsKey(fieldname)) {
			throw new IllegalArgumentException("Illegal fieldname specified");
		}

		final int fillFactor = skip * skipPadMultiplier
				// TODO: remove. this is a hack since they don't like the SUBSTANCE category
				+ maxValues * config.getCulledSuperCategories().size();

		final List<Field> fields = aggregateParametricValuesForFilteredSearch(requestData, maxValues + fillFactor, Collections.singletonList(fieldname), null, false, true, "documentcount", false);

		final Map<String, List<FieldValue>> fieldProbabilities = new HashMap<String, List<FieldValue>>();

		if (fields.isEmpty()) {
			return fieldProbabilities;
		}

		final List<FieldValue> values = filterFieldsByCategory(maxValues, fields.get(0).getFieldValues(), skip);

		if (values.isEmpty()) {
			return fieldProbabilities;
		}

		// for every field, need to find its coincidence with every other field
		fieldProbabilities.put("", values);

		final Set<String> legalValues = new HashSet<String>();

		for (final FieldValue fv : values) {
			legalValues.add(fv.getValue());
		}

		final FieldText cooccurenceCheck = new MATCH(fieldname, legalValues);

		for (final FieldValue fv : values) {
			final String text = fv.getValue();

			final List<Field> subValues = aggregateParametricValuesForFilteredSearch(requestData, -1, Collections.singletonList(fieldname), new MATCH(fieldname, text).AND(cooccurenceCheck), true, true, "documentcount", false);

			if (subValues.isEmpty()) {
				// for some unexpected reason, we got no fields, shouldn't happen
				fieldProbabilities.put(text, Collections.<FieldValue>emptyList());
			}
			else {
				final List<FieldValue> newValues = subValues.get(0).getFieldValues();

				// we might have some values which we don't want despite RestrictedValues=true if the
				// SearchRequestData had some extra fieldtext other fieldstext relating to the field,
				for (final ListIterator<FieldValue> it = newValues.listIterator(); it.hasNext(); ) {
					if (!legalValues.contains(it.next().getValue())) {
						it.remove();
					}
				}

				fieldProbabilities.put(text, newValues);
			}
		}

		return fieldProbabilities;
	}

	@Cacheable(cacheName="SearchService.search_links")
	public Links getLinks(final String reference, final int maxResults) {
		// this is a temporary implementation, once we have proper fields indexed we can do a more efficient query
		final String idField = config.getLinkIdField();
		final String toField = config.getLinkToField();
		final AciParameters params = new AciParameters("getcontent");
		params.add("reference", reference);
		params.add("databasematch", config.getDatabases());
		params.add("print", "fields");
		params.add("printfields", new PrintFields(idField, toField));
		final List<Document> docs = searchAciService.executeAction(params, new DocumentListProcessor());
		if (docs.isEmpty()) {
			throw new IllegalArgumentException("Invalid reference");
		}

		final Document doc = docs.get(0);

		final List<Document> from = getDocuments(new MATCH(toField, doc.getDocumentFieldValue(idField)), maxResults, null);
		final List<String> outLinks = doc.getDocumentFieldValues(toField);

		final int outLinkCount = outLinks.size();
		final Map<String, Link> toLinks = new LinkedHashMap<>(outLinkCount);

		if (outLinkCount > 0) {
			for (final String title : outLinks) {
				toLinks.put(title, new Link(title, null));
			}


			for (final Document idolDoc : getDocuments(new MATCH(idField, outLinks), outLinkCount, new PrintFields(idField))) {
				final Link link = toLinks.get(idolDoc.getDocumentFieldValue(idField));
				if (link != null) {
					link.setTitle(idolDoc.getTitle());
					link.setReference(idolDoc.getReference());
				}
			}
		}

		return new Links(wrapLinks(from), new ArrayList<>(toLinks.values()));
	}

	private static List<Link> wrapLinks(final List<Document> docs) {
		final ArrayList<Link> links = new ArrayList<>(docs.size());
		for (final Document doc : docs) {
			links.add(new Link(doc.getTitle(), doc.getReference()));
		}

		return links;
	}

	private List<Document> getDocuments(final FieldText fieldText, final int maxResults, final PrintFields printFields) {
		final AciParameters params = new AciParameters("query");
		params.add("databasematch", config.getDatabases());
		params.add("fieldtext", fieldText);
		params.add("print", "fields");
		params.add("printfields", printFields);
		params.add("maxresults", maxResults);
		params.add("languagetype", config.getLanguageType());
		params.add("outputencoding", config.getOutputEncoding());
		return searchAciService.executeAction(params, new DocumentListProcessor());
	}

    /**
     * Performs a search given a database
     * 
     * Submits a search query to Idol and produces a list of search result items
     * from the response
     * 
     * @param requestData
     * @return
     */
    @Cacheable(cacheName="SearchService.search_results_db")
    public List<ResultDocument> searchResultsDB(
            final SearchRequestData requestData,
            final String database,
            final String maxResults
    ) {

        final AciParameters params = addSearchParams(new AciParameters("query"), requestData, database, maxResults, false);

        try {
            final List<ResultDocument> items = searchAciService.executeAction(params, processorFactory.listProcessorForClass(ResultDocument.class));
            return items;
        }
        catch (final Exception e) {
            LOGGER.error("Failed to get search results", e);
            return new LinkedList<ResultDocument>();
        }
    }

    /**
     *
     * @param requestData
     * @param database
     * @param maxResults
     * @return
     */
    public Results searchResultsDBDoc(
            final SearchRequestData requestData,
            final String database,
            final String maxResults
    ) {
        final AciParameters params = handlePatientCohortFilters(new AciParameters("query"), requestData, database);

        addSearchParams(params, requestData, database, maxResults, config.isTotalResults() && requestData.getPage() == 0);

        final AciURLCodec encoder = AciURLCodec.getInstance();

        try {
			final QueryResponse response = searchAciService.executeAction(params, new HierarchicalQueryResponseProcessor());
            final String queryText = requestData.getQuery();
            final List<String> links = (queryText != null && !"*".equals(queryText)) ? Collections.<String>singletonList(encoder.encode(queryText)) : null;
            return new Results(
				censor.censor(ResultDocument.fromDocuments(config.getMeta(), config.getSearchViews().get(requestData.getSearchView()).getDisplayFields(), response.getDocuments(), config.getLinkIdField(), config.getLinkToField(), links)),
				Collections.singletonMap(database,  (double)response.getTotalHits()),
                requestData.getStateMatchId(),
                requestData.getStateDontMatchId(), null);
        }
        catch (final RuntimeException re) {
            LOGGER.error("Failed to get search results", re);
            throw new RuntimeException(re.getMessage());
        }
        catch (final Exception e) {
            LOGGER.error("Failed to get search results", e);
            return new Results(new LinkedList<ResultDocument>(), Collections.singletonMap(database, 0d));
        }
    }

    /**
     *
     * @param params
     * @param requestData
     * @param database
     * @param maxResultsStr
     * @return
     */
    public AciParameters addSearchParams(
            final AciParameters params,
            final SearchRequestData requestData,
            final String database,
            final String maxResultsStr,
			final boolean totalResults
    ) {
        final Map<String, FilterField> filterFieldNames = parametricService.getFilterFieldNames(requestData.getSearchView());
        try {
        	addBaseSearchParams(params, requestData, database, true, filterFieldNames);
        } catch(Exception e) {
        	LOGGER.error(" Ignorable error for Vertica source system >>>>>>>> " + e.getMessage());
        }
        


        final String action = params.get(AciConstants.PARAM_ACTION);

        final SearchSettings searchSettings = requestData.getUserSearchSettings();
        final String combine = searchSettings == null ? config.getCombine() : searchSettings.getCombine();
        final String summary = searchSettings == null ? config.getSummary() : searchSettings.getSummary();

        //  Pagination offset
		if (StringUtils.isNotEmpty(maxResultsStr)) {
			final int maxResults = Integer.parseInt(maxResultsStr);
			final int offset = maxResults * requestData.getPage();
			params.add("start", offset+1);
			params.add("maxresults", maxResults + offset);
		}

		params.add("totalresults", totalResults);
		if (totalResults) {
			params.add("predict", config.isTotalResultsPredict());
		}

        params.add("combine", combine);

        if (requestData.getMinScore() != null) {
      			params.add("minscore", requestData.getMinScore());
      	}

        if ("query".equalsIgnoreCase(action)) {
            params.add("summary", summary);

            final Integer reqDisplayChars = requestData.getDisplayChars();
            params.add("characters", reqDisplayChars != null ? reqDisplayChars : config.getDisplayCharacters());

            //  Gives us the date
            params.add("xmlmeta", "true");
            //  Gives us the: Source, People, Places, Companies

            final Map<String, DisplayField> displayFields = config.getSearchViews().get(requestData.getSearchView()).getDisplayFields();
            final Set<String> printFieldnames = new HashSet<String>();

            if (displayFields != null) {
                for(final Map.Entry<String, DisplayField> displayField : displayFields.entrySet()) {
                    final String[] displayFieldnames = displayField.getValue().getFields();
                    if (displayFieldnames != null) {
                        for(final String dfieldname : displayFieldnames) {
                            if (filterFieldNames.containsKey(dfieldname)) {
                                printFieldnames.add(dfieldname);
                            }
                        }
                    }
                }
            }

            if (!printFieldnames.isEmpty()) {
                params.add("print", "Fields");
                params.add("printfields", StringUtils.join(printFieldnames, ","));
            } else {
                params.add("print", "all");

            }

        }

        return params;
    }

    public AciParameters addBaseSearchParams(
            final AciParameters params,
            final SearchRequestData requestData,
            final String database,
            final boolean filterCohort,
            final Map<String, FilterField> filterFieldNames
    ) {
        final Map<String, FieldTextDetail> paraFieldNames = null; //TODO: FIXMEME: parametricService.getParaFieldNames(requestData.getSearchView());

        params.add(PARAM_FIELDTEXT, FieldTextUtil.buildFilterExpression(documentFolderService, requestData.getFilterGroup(), filterFieldNames, filterCohort));

        params.add("text", Queries.buildQueryWithExtension(requestData));

        params.add("languagetype", config.getLanguageType());
        params.add("outputencoding", config.getOutputEncoding());
        params.add("databasematch", database);

        if (StringUtils.isNotBlank(requestData.getSecurtiyInfo())) {
            params.add("securityinfo", requestData.getSecurtiyInfo());
        }

        if (requestData.getStateMatchId() != null || requestData.getStateDontMatchId() != null) {
            final String cohortField = config.getSearchViews().get(requestData.getSearchView()).getPtCohortField();
            params.add("referencefield", cohortField);

            if (requestData.getStateMatchId() != null) {
                params.add("statematchid", requestData.getStateMatchId());
            }

            if (requestData.getStateDontMatchId() != null) {
                params.add("statedontmatchid", requestData.getStateDontMatchId());
            }
        }

        return params;
    }

    private AciParameters handlePatientCohortFilters(
            final AciParameters parentParams,
            final SearchRequestData requestData,
            final String database
    )  {

        if (requestData.getStateMatchId() != null || requestData.getStateDontMatchId() != null) {
            return parentParams;
        }

        final String cohortField = config.getSearchViews().get(requestData.getSearchView()).getPtCohortField();

        final FilterGroup rootGroup = requestData.getFilterGroup();
        // Only support cohort filters in direct child group.
        final List<FilterGroup> childGroups = (rootGroup != null) ? rootGroup.getChildGroups() : null;
        if (childGroups == null) {
            return parentParams;
        }


        final List<String> matchedStateList = new ArrayList<String>();
        final List<String> notMatchedStateList = new ArrayList<String>();

        final Map<String, FilterField> filterFieldNames = parametricService.getFilterFieldNames(requestData.getSearchView());

        for(final FilterGroup childGroup : rootGroup.getChildGroups()) {
            if (childGroup.getCohortOp() != null) {
                final AciParameters params = new AciParameters("query");
                final SearchRequestData cohortGroupRequestData = new SearchRequestData(requestData.getQuery(), childGroup, requestData.getQueryExtension(), requestData.getSearchView());

                addBaseSearchParams(params, cohortGroupRequestData, database, false, filterFieldNames);

                params.add("storestate", Boolean.TRUE);
                params.add("storedstatefield", cohortField);
                params.add("print", "noresults");
                params.add("combine", "fieldcheck");
                params.add("maxresults", 1000000);

                final StateResult stateResult = searchAciService.executeAction(params, new StoreStateProcessor());
                if (stateResult != null) {
                    if (CohortOp.PT_MATCH == childGroup.getCohortOp()) {
                        matchedStateList.add(stateResult.getStateId());
                    } else {
                        notMatchedStateList.add(stateResult.getStateId());
                    }

                } else {
                    //LOGGER.debug(params.toString());
                }
            }

        }

        if (matchedStateList.size() > 0) {
            requestData.setStateMatchId(StringUtils.join(matchedStateList, ","));
        }

        if (notMatchedStateList.size() > 0) {
            requestData.setStateDontMatchId(StringUtils.join(notMatchedStateList, ","));
        }


        return parentParams;

    }



    public PrintFieldsResultIterator getExportResultsFields(final DocExportData docExportData, final String securityInfo) {
        final AciParameters params = getDocExportParams(docExportData, securityInfo);
        params.add("storestate", true);
        params.add("print", "noresults");

        final StringBuilder strBuilder = new StringBuilder();
        final String[] printFields = docExportData.getExportSourceFields().split(",");        
        for(final String pfield : printFields) {
            strBuilder.append(pfield).append(',');
        }
        final String printFieldsStr = StringUtils.chop(strBuilder.toString());

        final StateResult stateResult = searchAciService.executeAction(params, new StoreStateProcessor());

        return new PrintFieldsResultIterator(stateResult,
                                             searchAciService,
                                             config,
                                             docExportData.getSearchView(),
                                             printFieldsStr,
                                             securityInfo);

    }
    
    public com.autonomy.vertica.query.QueryResponse getExportResultsFieldsVertica(final DocExportData docExportData, final String securityInfo) throws Exception {
        final AciParameters params = getDocExportParams(docExportData, securityInfo);
        params.add("storestate", true);
        params.add("print", "noresults");

        final StringBuilder strBuilder = new StringBuilder();
        final String[] printFields = docExportData.getExportSourceFields().split(",");        
        for(final String pfield : printFields) {
            strBuilder.append(pfield).append(',');
        }
        final String printFieldsStr = StringUtils.chop(strBuilder.toString());
        params.add("printfields", printFieldsStr);
        
        final SearchRequestData requestData;
        try {
            requestData = JSON.toObject((new StringReader(docExportData.getExportSearchData())), SearchRequestData.class);
            requestData.setSecurtiyInfo(securityInfo);

        } catch(IOException ex) {
            throw new IllegalArgumentException("Failed to retrieve search data for export");
        }
        
        addSearchParamsVertica(params, requestData, docExportData.getExportMaxDocs().toString());
        
        params.put("start", 0); // for exporting always start at 0 (zero)
        
        requestData.setRetrieveResultDocs(true);
        
        final com.autonomy.vertica.query.QueryResponse response = verticaService.searchResultsDoc(params, requestData.getSearchView(),
        		requestData.getUserSearchSettings(), requestData.getFilterGroup(), requestData.isRetrieveResultDocs(), null, null, null);

        //final StateResult stateResult = searchAciService.executeAction(params, new StoreStateProcessor());
       // verticaService.searchResultsDoc(params, requestData);
        
        

       return response;

    }

    public ReferenceResultsIterator getTagResults(final DocExportData docExportData, final String securityInfo) {
        final AciParameters params = getDocExportParams(docExportData, securityInfo);
        params.add("storestate", true);
        params.add("print", "noresults");
        
             
        final StateResult stateResult = searchAciService.executeAction(params, new StoreStateProcessor());       

        return new ReferenceResultsIterator(stateResult,
                                            searchAciService,
                                            config,
                                            docExportData.getSearchView(),
                                            securityInfo);

     }
    
    public List<String> getTagResultsVertica(final DocExportData docExportData, final String securityInfo, String fieldName) throws Exception {
       // final AciParameters params = getDocExportParams(docExportData, securityInfo);
        final String database = config.getDatabase(docExportData.getSearchView());

        final SearchRequestData requestData;
        try {
            requestData = JSON.toObject((new StringReader(docExportData.getExportSearchData())), SearchRequestData.class);
            requestData.setSecurtiyInfo(securityInfo);

        } catch(IOException ex) {
            throw new IllegalArgumentException("Failed to retrieve search data for export");
        }

        final SearchSettings settings = requestData.getUserSearchSettings();
        final String combine = settings ==  null ? config.getCombine() : settings.getCombine();

        final AciParameters params = handlePatientCohortFilters(new AciParameters("query"), requestData, database);
        final Map<String, FilterField> filterFieldNames = parametricService.getFilterFieldNames(requestData.getSearchView());
        addBaseSearchParams(params, requestData, database, true, filterFieldNames);

        params.add("maxresults", docExportData.getExportMaxDocs());
        params.add("combine", combine);
        params.add("xmlmeta", "true");

       // return params;
        params.add("storestate", true);
        params.add("print", "noresults");
        
        return verticaService.getDocumentFolderRefs(params, requestData, fieldName);
    }


    private AciParameters getDocExportParams(final DocExportData docExportData, final String securityInfo) {
        final String database = config.getDatabase(docExportData.getSearchView());        
        final SearchRequestData requestData;
        if(docExportData.getExportSearchData() == null)  {
        	throw new IllegalArgumentException("Export data is null. Please retry!!!");
        }
        try {
            requestData = JSON.toObject((new StringReader(docExportData.getExportSearchData())), SearchRequestData.class);
            requestData.setSecurtiyInfo(securityInfo);

        } catch(IOException ex) {
            throw new IllegalArgumentException("Failed to retrieve search data for export: " + ex.getMessage());
        }

        final SearchSettings settings = requestData.getUserSearchSettings();
        final String combine = settings ==  null ? config.getCombine() : settings.getCombine();

        final AciParameters params = handlePatientCohortFilters(new AciParameters("query"), requestData, database);
        final Map<String, FilterField> filterFieldNames = parametricService.getFilterFieldNames(requestData.getSearchView());
        addBaseSearchParams(params, requestData, database, true, filterFieldNames);

        params.add("maxresults", docExportData.getExportMaxDocs());
        params.add("combine", combine);
        params.add("xmlmeta", "true");

        return params;

    }

    public void tagDocuments(final List<ResultDocument> items) {
        for (final ResultDocument item : items) {
        	item.tagSummary();
        }
    }

    private void addTrailingEnd(final List<ResultDocument> documents) {
    	for (final ResultDocument document : documents) {
			document.formatSummary();
    	}
    }

    /**
     * Searches for suggestions.
     * 
     * Submits a search to Idol for search suggestions related to a given query
     * text. Computes cluster relationships and returns A list of clusters (each
     * of which may have cluster children).
     * 
     * @param queryText
     * @return
     */
    @Cacheable(cacheName="SearchService.search_suggestions")
    public QuerySummaryResult searchSuggestions(final String queryText) {
        final LinkedList<Cluster> clusters = new LinkedList<Cluster>();
        final LinkedList<Cluster> mainClusters = new LinkedList<Cluster>();
        final HashSet<String> set = new HashSet<String>();
        for (final String database : config.getSuggestionDatabases().split(",")) {
            final QuerySummaryResult oneResult = searchSuggestionsForDB(queryText, database);
            nubInsert(mainClusters, set, oneResult.getMainClusters());
            nubInsert(clusters, set, oneResult.getClusters());
        }
        Collections.sort(mainClusters);
        Collections.sort(clusters);
        return QuerySummaryResult.create(mainClusters, clusters);
    }

    public static void incrementalNubInsert(final LinkedList<Cluster> xs, final HashSet<String> set, final Cluster el) {
        if (!set.contains(el.getName())) {
            xs.add(el);
            set.add(el.getName());
        }
    }

    public static void nubInsert(final LinkedList<Cluster> xs, final HashSet<String> set, final List<Cluster> els) {
        for (final Cluster el : els) {
            incrementalNubInsert(xs, set, el);
        }
    }

    /**
     * Searches for suggestions, given a database and query.
     * 
     * Submits a search to Idol for search suggestions related to a given query
     * text. Computes cluster relationships and returns A list of clusters (each
     * of which may have cluster children).
     * 
     * @param queryText
     * @return
     */
    @Cacheable(cacheName="SearchService.search_suggestions_db")
    public QuerySummaryClusters.QuerySummaryResult searchSuggestionsForDB(final String queryText, final String database) {
        final AciParameters params = new AciParameters("query");

        params.add("text", isStarOrEmpty(queryText) ? "*" : queryText);

        params.add("languagetype", config.getLanguageType());
        params.add("outputencoding", config.getOutputEncoding());
        params.add("maxresults", config.getMaxSuggestionResults());
        params.add("databasematch", database);
        params.add("print", "noresults");
        params.add("querysummary", true);
        params.add("combine", config.getCombine());

        final QueryResponse response;

        try {
            response = searchAciService.executeAction(params, new HierarchicalQueryResponseProcessor());
        }
        catch (final Exception ex) {
            LOGGER.error("Failed to get search suggestions", ex);
            return QuerySummaryResult.create();
        }
        return QuerySummaryClusters.structureSummaryElements(response.getAdvancedQuerySummary(), config.getMaxSuggestionChildren(), database);
    }

    @Cacheable(cacheName="SearchService.search_top_titles_summaries")
    public List<Map<String, String>> searchTopTitlesSummaries(final SearchRequestData requestData) {
        final AciParameters params = new AciParameters("query");
        addSearchParams(params, requestData, config.getDatabases(), config.getMaxPreviewResults(), false);
        return searchAciService.executeAction(params, new TitleSummaryProcessor());
    }

    /**
     * Get related concepts
     * 
     * Potentially queries for concepts related to a given training text.
     * 
     * @param training
     * @return
     */
    @Cacheable(cacheName="SearchService.related_concepts")
    public List<String> getRelatedConcepts(final String training) {
        // Given an empty training text, return the empty list
        if ("".equals(training.trim())) {
            return new LinkedList<String>();
        }
        // Otherwise, query for concepts
        return clusterNames(searchSuggestions(training));
    }

    /**
     * Get related concepts
     * 
     * Potentially queries for concepts related to a given training text.
     * 
     * @param training
     * @return
     */
    @Cacheable(cacheName="SearchService.related_concepts")
    public List<String> getRelatedConceptsCapped(final String training, final Integer maxResults) {
        final List<String> results = getRelatedConcepts(training);
        return results.subList(0, (int)Math.min(maxResults, results.size()));
    }

    /**
     * 
     * @param summaryResult
     * @return
     */
    private List<String> clusterNames(final QuerySummaryResult summaryResult) {
        final List<Cluster> clusters = new LinkedList<Cluster>();
        clusters.addAll(summaryResult.getMainClusters());
        clusters.addAll(summaryResult.getClusters());
        return clusterNames(clusters);
    }

    /**
     *
     * @param clusters
     * @return
     */
    private List<String> clusterNames(final List<Cluster> clusters) {
        final List<String> result = new LinkedList<String>();

        // For each cluster passed in
        for (final Cluster cluster : clusters) {
            // Push its name
            result.add(cluster.getName());
            // And the names of its children
            result.addAll(clusterNames(cluster.getChildren()));
        }

        return result;
    }


	public TimelineViewResponse timelineDBStats(SearchRequestData requestData,
			String dateField, String datePeriod, String dateOffset) {
		// TODO Auto-generated method stub
		return null;
	}

	public TimelineViewResponse timelineQueryStats(
			SearchRequestData requestData, String dateField, String datePeriod,
			String dateOffset) {
		// TODO Auto-generated method stub
		return null;
	}

	public TimelineViewResponse timelineParamFieldStats(
			SearchRequestData requestData, String dateField, String datePeriod,
			String dateOffset, String paramField, String paramMaxValues) {
		// TODO Auto-generated method stub
		return null;
	}


	public TimelineViewResponse timelineTagValueDetails(
			SearchRequestData requestData, 
			String field, 
			String maxResults, 
			String maxDisplayVals, 
			String validStartDate, 
			String validEndDate, 
			String baselineDate, 
			String asOfDate, 
			String zoomStartDate, 
			String zoomEndDate, 
			String valuestoRender)
   {
	        
		final TimelineViewResponse response = new TimelineViewResponse();

	        final TimelineData tdata = calculateTimeLineData(requestData, field, maxResults,maxDisplayVals, validStartDate, validEndDate, baselineDate, asOfDate,zoomStartDate,zoomEndDate,valuestoRender);
	    
	        response.setTimeline(tdata);

	        return response;
	}

	public TimelineViewResponse timelineTagValueDeltaDetails(
			SearchRequestData requestData, 
			String field, 
			String maxResults, 
			String maxDisplayVals, 
			String validStartDate, 
			String validEndDate, 
			String baselineDate, 
			String asOfDate, 
			String zoomStartDate, 
			String zoomEndDate, 
			String valuestoRender)
   {
	        
		final TimelineViewResponse response = new TimelineViewResponse();

        final TimelineData tdata = calculateTimeLineDeltaData(requestData, field, maxResults,maxDisplayVals, validStartDate, validEndDate, baselineDate, asOfDate,zoomStartDate,zoomEndDate,valuestoRender);
	    

	        response.setTimeline(tdata);

	        return response;
	}

	
	private TimelineData calculateTimeLineData(final SearchRequestData requestData,
            final String field,
            final String maxResults, 
            final String maxDisplayVals,
            final String validStartDate,
            final String validEndDate,
            final String baselineDate,
            final String asOfDate,
            final String zoomStartDate,
            final String zoomEndDate,
            final String valuesToRenderList 
			) {

  
    		String database = config.getDatabase(requestData.getSearchView());
    		String fieldName="";
    		
    		database+="_CLASS_TOTAL";
    		
    		final TimelineData timelineData=new TimelineData();
   	   		
    		// Get docCountLayer, the record-count across zoom window
  	   		final AciParameters params1a = addSearchParams(new AciParameters("query"), requestData, database, null, false);
    				
  	   		params1a.remove("print");
  	   		params1a.put("printfields", "AS_OF_DATE,VARNAME,VARVAL,TYPE,COUNT");
    		params1a.put("sort", "Date");
    		params1a.put("text","*");

    		
    		int queryMaxValues=MAX_TIMELINE_PAIR_VALUES;
    		
    		if (maxResults!=null && !maxResults.isEmpty()) {
    			int mResults=Integer.parseInt(maxResults);
    	   		if (mResults<=MAX_TIMELINE_PAIR_VALUES) {
        			queryMaxValues=mResults;
        		}
    		}
  
       		String sDFilterStartTime="1409544000e";
       		String sDFilterEndTime="1420174800e";
    		if ((validStartDate!=null) && (!validStartDate.isEmpty())) {
    			sDFilterStartTime=validStartDate+"e";
       		}
       		if ((validEndDate!=null) && (!validEndDate.isEmpty())) {
    			sDFilterEndTime=validEndDate+"e";
       		}
 
    		
    		String ftext="MATCH{TOTAL}:TYPE AND RANGE{"+sDFilterStartTime+","+sDFilterEndTime+"}:AS_OF_DATE";
    		params1a.put("fieldtext",ftext);
    		params1a.put("maxresults",queryMaxValues);
  		
 			TimelineDataLayer docCountLayer=searchAciService.executeAction(params1a, new TimelineDocCountLayersProcessor(queryMaxValues));
    		timelineData.setDocCountLayer(docCountLayer);
   			
    		timelineData.calcMinMax();
    		   		
      		
       		TimelineDateRange tRange=timelineData.gettRange();  // Returned Range of Dates for doccount query in validrange
       		long countDate=tRange.getDateMax()/1000;
    		
    		
      		if (field.isEmpty()) {
    			// return timelineData;	
    			fieldName="CRISE_CAT";
    		} else {
    			fieldName=field;
    		}
    		String FieldMatchStr="MATCH{"+fieldName+"}:VARNAME";

      		// Get the parametric value layers on last date of doccounts	
     		final AciParameters params2 = addSearchParams(new AciParameters("query"), requestData, database, null, false);
  	   		params2.put("printfields", "AS_OF_DATE,VARNAME,VARVAL,COUNT");
    		params2.put("sort", "Date");
    		params2.put("maxresults",MAX_TIMELINE_PAIR_VALUES);    		
       		params2.put("text","*");
  	
    		
    		String fieldtext=FieldMatchStr;
    		fieldtext+=" AND RANGE{"+countDate+"e,"+countDate+"e}:AS_OF_DATE";
    		params2.put("fieldtext",fieldtext);
    		
   			TimelineValueList queryValuesList;
   			queryValuesList=searchAciService.executeAction(params2, new TimelineValuesProcessor(fieldName));
   		    		
  			// timelineData.setValList(queryValuesList);
	
  			// Generate filters for fieldvalues based on valuesToRenderList, valuesForVarList, & maxDisplayVals
    		int fMaxDisplayVal=10;
  			if ((maxDisplayVals!=null) && (!maxDisplayVals.isEmpty())) {
  				fMaxDisplayVal=Integer.parseInt(maxDisplayVals);
       		}
			
  			String fvalFilter="";
  			List<String> flist;
  			if ((valuesToRenderList!=null) && !valuesToRenderList.isEmpty()) {
				// Get requested list from passed parameters
   				flist=Arrays.asList(valuesToRenderList.split(","));		
   				// Truncate to top 10 
   				flist=new ArrayList<String>(flist.subList(0,flist.size()<fMaxDisplayVal?flist.size(): fMaxDisplayVal));
			} else {
				// Get tentative list from returned var vals
				flist=new ArrayList<String>();
				TreeSet<TimelineFieldValueMeta> tvals = queryValuesList.getValues();
				if (tvals!=null) {
					Iterator<TimelineFieldValueMeta> itr = tvals.iterator();
				    while (itr.hasNext() && flist.size()<fMaxDisplayVal){
				      TimelineFieldValueMeta valmeta=itr.next();
				      String valname=valmeta.getVarVal();
				      flist.add(valname);
				    }
				}
			}
  			
  		
 
       		long zoomMin;
    		long zoomMax;
        	    
    		if ((zoomStartDate!=null) && (zoomEndDate!=null) && (!zoomStartDate.isEmpty()) && (!zoomEndDate.isEmpty())) {
    			// zoomMin=Long.parseLong(zoomStartDate);
    			zoomMin=Long.parseLong(zoomStartDate);;
    			zoomMax=Long.parseLong(zoomEndDate);
    		} else {
    			zoomMin=tRange.getDateMin()/1000;
    			zoomMax=tRange.getDateMax()/1000;    			
    		}
   		
    		
    		
      		// Get the parametric value layers across zoom window	
     		final AciParameters params3 = addSearchParams(new AciParameters("query"), requestData, database, null, false);
  	   		params3.put("printfields", "AS_OF_DATE,VARNAME,VARVAL,COUNT");
    		params3.put("sort", "Date");
    		params3.put("maxresults",MAX_TIMELINE_PAIR_VALUES*2);    		
       		params3.put("text","*");
    	
    		fieldtext=FieldMatchStr;
    		// Get results previous to zoom max, will truncate based on selectedZoom in layers processor
    		fieldtext+=" AND RANGE{.,"+zoomMax+"e}:AS_OF_DATE";
    		
    		String FieldValMatchStr=StringUtils.join(flist, ',');
    		if (!FieldValMatchStr.isEmpty()) {
    			fieldtext+=" AND MATCH{"+FieldValMatchStr+"}:VARVAL";
    		}
    		params3.put("fieldtext",fieldtext);
    		
   			timelineData.setDataLayers(searchAciService.executeAction(params3, new TimelineLayersProcessor(queryValuesList,queryMaxValues,zoomMin,zoomMax)));
   			timelineData.normalize();  		
   			
   			// Set the resulting zoom window
   			timelineData.calcZoomMinMax();

   	   		return timelineData;
}

	
	private TimelineData calculateTimeLineDeltaData(final SearchRequestData requestData,
            final String field,
            final String maxResults, 
            final String maxDisplayVals,
            final String validStartDate,
            final String validEndDate,
            final String baselineDate,
            final String asOfDate,
            final String zoomStartDate,
            final String zoomEndDate,
            final String valuesToRenderList 
			) {

  
    		String database = config.getDatabase(requestData.getSearchView());
    		String fieldName="";
    		
    		database+="_CLASS_TOTAL";
    		
    		final TimelineData timelineData=new TimelineData();
   	   		
    		// Get docCountLayer, the record-count across zoom window
  	   		final AciParameters params1a = addSearchParams(new AciParameters("query"), requestData, database, null, false);
    				
  	   		params1a.remove("print");
  	   		params1a.put("printfields", "AS_OF_DATE,VARNAME,VARVAL,TYPE,COUNT");
    		params1a.put("sort", "Date");
    		params1a.put("text","*");

    		
    		int queryMaxValues=MAX_TIMELINE_PAIR_VALUES;
    		
    		if (maxResults!=null && !maxResults.isEmpty()) {
    			int mResults=Integer.parseInt(maxResults);
    	   		if (mResults<=MAX_TIMELINE_PAIR_VALUES) {
        			queryMaxValues=mResults;
        		}
    		}
  
       		String sDFilterStartTime="1409544000e";
       		String sDFilterEndTime="1420174800e";
    		if ((validStartDate!=null) && (!validStartDate.isEmpty())) {
    			sDFilterStartTime=validStartDate+"e";
       		}
       		if ((validEndDate!=null) && (!validEndDate.isEmpty())) {
    			sDFilterEndTime=validEndDate+"e";
       		}
 
    		
    		String ftext="MATCH{TOTAL}:TYPE AND RANGE{"+sDFilterStartTime+","+sDFilterEndTime+"}:AS_OF_DATE";
    		params1a.put("fieldtext",ftext);
    		params1a.put("maxresults",queryMaxValues);
  		
 			TimelineDataLayer docCountLayer=searchAciService.executeAction(params1a, new TimelineDocCountLayersProcessor(queryMaxValues));
    		timelineData.setDocCountLayer(docCountLayer);
   			
    		timelineData.calcMinMax();
    		   		
      		
       		TimelineDateRange tRange=timelineData.gettRange();  // Returned Range of Dates for doccount query in validrange
       		long countDate=tRange.getDateMax()/1000;
    		
    		
      		if (field.isEmpty()) {
    			// return timelineData;	
    			fieldName="CRISE_CAT";
    		} else {
    			fieldName=field;
    		}
    		String FieldMatchStr="MATCH{"+fieldName+"}:VARNAME";

      		// Get the parametric value layers on last date of doccounts	
     		final AciParameters params2 = addSearchParams(new AciParameters("query"), requestData, database, null, false);
  	   		params2.put("printfields", "AS_OF_DATE,VARNAME,VARVAL,COUNT");
    		params2.put("sort", "Date");
    		params2.put("maxresults",MAX_TIMELINE_PAIR_VALUES);    		
       		params2.put("text","*");
  	
    		
    		String fieldtext=FieldMatchStr;
    		fieldtext+=" AND RANGE{"+countDate+"e,"+countDate+"e}:AS_OF_DATE";
    		params2.put("fieldtext",fieldtext);
    		
   			TimelineValueList queryValuesList;
   			queryValuesList=searchAciService.executeAction(params2, new TimelineValuesProcessor(fieldName));
   		    		
  			// timelineData.setValList(queryValuesList);
	
  			// Generate filters for fieldvalues based on valuesToRenderList, valuesForVarList, & maxDisplayVals
    		int fMaxDisplayVal=10;
  			if ((maxDisplayVals!=null) && (!maxDisplayVals.isEmpty())) {
  				fMaxDisplayVal=Integer.parseInt(maxDisplayVals);
       		}
			
  			String fvalFilter="";
  			List<String> flist;
  			if ((valuesToRenderList!=null) && !valuesToRenderList.isEmpty()) {
				// Get requested list from passed parameters
   				flist=Arrays.asList(valuesToRenderList.split(","));		
   				// Truncate to top 10 
   				flist=new ArrayList<String>(flist.subList(0,flist.size()<fMaxDisplayVal?flist.size(): fMaxDisplayVal));
			} else {
				// Get tentative list from returned var vals
				flist=new ArrayList<String>();
				TreeSet<TimelineFieldValueMeta> tvals = queryValuesList.getValues();
				if (tvals!=null) {
					Iterator<TimelineFieldValueMeta> itr = tvals.iterator();
				    while (itr.hasNext() && flist.size()<fMaxDisplayVal){
				      TimelineFieldValueMeta valmeta=itr.next();
				      String valname=valmeta.getVarVal();
				      flist.add(valname);
				    }
				}
			}
  			
  		
 
       		long zoomMin;
    		long zoomMax;
        	    
    		if ((zoomStartDate!=null) && (zoomEndDate!=null) && (!zoomStartDate.isEmpty()) && (!zoomEndDate.isEmpty())) {
    			// zoomMin=Long.parseLong(zoomStartDate);
    			zoomMin=Long.parseLong(zoomStartDate);;
    			zoomMax=Long.parseLong(zoomEndDate);
    		} else {
    			zoomMin=tRange.getDateMin()/1000;
    			zoomMax=tRange.getDateMax()/1000;    			
    		}
  		
    		database = config.getDatabase(requestData.getSearchView());    		
    		database+="_CLASS";
		
    		
      		// Get the parametric value layers across zoom window	
     		final AciParameters params3 = addSearchParams(new AciParameters("query"), requestData, database, null, false);
 	   		params3.remove("print");
  	   		params3.put("printfields", "AS_OF_DATE,VARNAME,VARVAL,TYPE,COUNT");
    		params3.put("sort", "Date");
    		params3.put("maxresults",MAX_TIMELINE_PAIR_VALUES);    		
       		params3.put("text","*");
    	
    		fieldtext=FieldMatchStr;
    		// Get results previous to zoom max, will truncate based on selectedZoom in layers processor
    		fieldtext+=" AND RANGE{.,"+zoomMax+"e}:AS_OF_DATE";
    		
    		String FieldValMatchStr=StringUtils.join(flist, ',');
    		if (!FieldValMatchStr.isEmpty()) {
    			fieldtext+=" AND MATCH{"+FieldValMatchStr+"}:VARVAL";
    		}
    		params3.put("fieldtext",fieldtext);
    		
   			timelineData.setDataLayers(searchAciService.executeAction(params3, new TimelineLayersDeltaProcessor(queryValuesList,queryMaxValues,zoomMin,zoomMax)));
   			timelineData.normalize();  		
   			
   			// Set the resulting zoom window
   			timelineData.calcZoomMinMax();

   	   		return timelineData;
}
	
	/**
	 * Vertica related method for Search
	 * 
	 * @param requestData
	 * @param maxValues
	 * @param filteredFieldNames
	 * @param fieldText
	 * @param restrictedvalues
	 * @param rename
	 * @param sort
	 * @param singleQueryField
	 * @return
	 */
	 @Cacheable(cacheName="SearchService.search_fields")
	   	public List<com.autonomy.vertica.fields.Field> aggregateParametricValuesFromVertica(
	               final SearchRequestData requestData,
	               final int maxValues,
	               final List<String> filteredFieldNames,
	               final Map fieldText,
	               final boolean restrictedvalues,
	               final boolean rename,
	               final String sort,
	               final boolean singleQueryField,
	               final RequestProcess process
	    ) {
		 
		/* List<com.autonomy.vertica.fields.Field> fieldList = new ArrayList<>();		
		 fieldList = filterTemplate.listParametricFields();		
		 return fieldList;*/
		 
		 final Map<String,Map<String, FilterField>> paraFieldNames = parametricService.getCategorizedParaFieldNames(requestData.getSearchView());
	        final Map<String, FilterField> aggregatedFieldsMap = paraFieldNames.get(parametricService.AGGREGATED_FIELD_KEY);
	        final Map<String, FilterField> nonAggregatedFieldsMap = paraFieldNames.get(parametricService.NON_AGGREGRATED_FIELD_KEY);
	        final List<String> aggregatedList = new ArrayList<String>();
	        final List<String> nonAggregatedList = new ArrayList<String>();
	        final Map<String, ParaField> paraFieldMap = new HashMap<String, ParaField>();

	        if (filteredFieldNames != null && !filteredFieldNames.isEmpty())
	        {
	            for (final String filteredName : filteredFieldNames) {
	                final String paraFieldName = getParametricNameFromFilter(filteredName, requestData.getSearchView());

	                if (paraFieldName == null) {
	                    throw new IllegalArgumentException("Illegal fieldname [" + filteredName + "] specified");
	                }

	                if (nonAggregatedFieldsMap.containsKey(paraFieldName)) {
	                    nonAggregatedList.add(paraFieldName);
	                    paraFieldMap.put(paraFieldName, nonAggregatedFieldsMap.get(paraFieldName).getParametric());
	                } else if (aggregatedFieldsMap.containsKey(paraFieldName)) {
	                    aggregatedList.add(paraFieldName);
	                    paraFieldMap.put(paraFieldName, aggregatedFieldsMap.get(paraFieldName).getParametric());
	                } else {
	                    throw new IllegalArgumentException("Illegal para fieldname [" + paraFieldName + "] specified");
	                }
	            }

	        } else {
	            for(final Map.Entry<String, FilterField> entry : nonAggregatedFieldsMap.entrySet()) {
	                nonAggregatedList.add(entry.getKey());
	                paraFieldMap.put(entry.getKey(), entry.getValue().getParametric());
	            }

	            for(final Map.Entry<String, FilterField> entry : aggregatedFieldsMap.entrySet()) {
	                aggregatedList.add(entry.getKey());
	                paraFieldMap.put(entry.getKey(), entry.getValue().getParametric());
	            }
	        }

	        final Set<SearchResult<List<com.autonomy.vertica.fields.Field>>> resultSet = new TreeSet<SearchResult<List<com.autonomy.vertica.fields.Field>>>(new SearchResultComparatorVertica());
	        try {
	          
	                final SearchResult<List<com.autonomy.vertica.fields.Field>> result = new ParametricValuesSearchExecutorVertica(0, requestData, maxValues, nonAggregatedList, fieldText, restrictedvalues, rename, sort, false, null, this, paraFieldMap, process).call();
	                if (result != null) {
	                    resultSet.add(result);
	                }
	           
	        } catch (Exception e) {
	            throw new RuntimeException(e);
	        }


	        final List<com.autonomy.vertica.fields.Field> paraFields = new ArrayList<com.autonomy.vertica.fields.Field>();
	        final Iterator<SearchResult<List<com.autonomy.vertica.fields.Field>>> it = resultSet.iterator();

	        final Map<String, FilterField> paraFieldsMap = new HashMap<String, FilterField>();
	        paraFieldsMap.putAll(aggregatedFieldsMap);
	        paraFieldsMap.putAll(nonAggregatedFieldsMap);


	        while (it.hasNext()) {
	            final List<com.autonomy.vertica.fields.Field> fields = it.next().getResult();
	            if (fields != null) {
	                for(final com.autonomy.vertica.fields.Field field : fields) {
	                    final FilterField paraFilterField = paraFieldsMap.get(field.getName());
	                    if (paraFilterField != null) {
	                        field.setName(paraFilterField.getName());
	                    }
	                }

	                paraFields.addAll(fields);
	            }
	        }

	        return paraFields;
	 
	 }
	 
	 /**
	  * Vertica - search documents
	  * 
	  * @param requestData
	  * @return
	  */
	 public Results searchFilteredResultsDocVertica(
	            final SearchRequestData requestData,
	            final String sort,
	            final String sortOrder,
	            final Set<String> roles
	    ) {
	        
	        final String database = config.getDatabase(requestData.getSearchView());
	        String sortFieldName = null;
	        if(sort != null) {
	        	DisplayField displayField = config.getSearchViews().get(requestData.getSearchView()).getDisplayFields().get(sort);
	        	if(displayField != null) {
	        		for(String fieldName : displayField.getFields()) {
	        			if(fieldName != null) {
	        				sortFieldName = fieldName;
	        				break;
	        			}
	        		}
	        	}
	        }
	        return searchResultsDBDocVertica(requestData, database, config.getMaxSearchResults(), sortFieldName, sortOrder, roles);
	       
	 }
	 
	 /**
	  * Vertica - search docs
	  * 
	  * @param requestData
	  * @param database
	  * @param maxResults
	  * @return
	  */
	 public Results searchResultsDBDocVertica(
	            final SearchRequestData requestData,
	            final String database,
	            final String maxResults,
	            final String sortFieldName,
	            final String sortOrder,
	            final Set<String> roles
	    ) {
	        final AciParameters params = handlePatientCohortFilters(new AciParameters("query"), requestData, database);

	        addSearchParams(params, requestData, database, maxResults, config.isTotalResults() && requestData.getPage() == 0);
	        
	        addSearchParamsVertica(params, requestData, maxResults);
	        

	        final AciURLCodec encoder = AciURLCodec.getInstance();

	        try {
	        	//final QueryResponse response = searchAciService.executeAction(params, new HierarchicalQueryResponseProcessor());
	        	
	        	final com.autonomy.vertica.query.QueryResponse response = verticaService.searchResultsDoc(params, requestData.getSearchView(),
	            		requestData.getUserSearchSettings(), requestData.getFilterGroup(), requestData.isRetrieveResultDocs(), sortFieldName, sortOrder, roles);
	        	
	        	final String queryText = requestData.getQuery();
	            final List<String> links = (queryText != null && !"*".equals(queryText)) ? Collections.<String>singletonList(encoder.encode(queryText)) : null;
	            return new Results(
					censor.censor(ResultDocument.fromDocumentsVertica(config.getMeta(), config.getSearchViews().get(requestData.getSearchView()).getDisplayFields(), response.getDocuments(), 
							config.getLinkIdField(), config.getLinkToField(), links)),
					Collections.singletonMap(database,  response.getTotalHits()),
	                requestData.getStateMatchId(),
	                requestData.getStateDontMatchId(), response.getSqlQuery());
	        }
	        catch (final RuntimeException re) {
	            LOGGER.error("Failed to get search results", re);
	            throw new RuntimeException(re.getMessage());
	        }
	        catch (final Exception e) {
	            LOGGER.error("Failed to get search results", e);
	            return new Results(new LinkedList<ResultDocument>(), Collections.singletonMap(database, 0d));
	        }
	    }
	 
	 public  List<com.autonomy.vertica.fields.Field> parametricValuesForFilteredSearchVertica(
	            final SearchRequestData requestData,
	            final int maxValues,
	            final List<String> filteredFieldNames,
	            final Map<String, String> fieldText,
	            final boolean restrictedvalues,
	            final boolean rename,
	            final String sort,
	            final boolean isAggregated,
	            final String aggregatedFieldname,
	            final Map<String, ParaField> paraFieldMap,
	            final RequestProcess process
	    ) throws Exception{
	        if (filteredFieldNames == null || filteredFieldNames.isEmpty()) {
	            throw new IllegalArgumentException("Missing field names parameter");
	        }

	        final String database = config.getDatabase(requestData.getSearchView());

			// this will have lots of spurious unrelated search-parameters, e.g. characters etc., they should be
			// safely ignored though

			final AciParameters params = addSearchParams(new AciParameters("getquerytagvalues"), requestData, database, null, false);
			params.add("documentcount", true);
			params.add("sort", sort);

			if (maxValues > 0) {
				params.add("maxvalues", maxValues);
			}

			if (restrictedvalues) {
				// Quote: 'Restricts tag values for fields in both the FieldText and FieldName parameters to
				// only those values that satisfy the FieldText'.
				// So we can ensure that we don't get any values except those specifically
				// (except any which might be returned as part of the base searchRequest)
				params.add("RestrictedValues", true);
			}

			//FieldTextUtil.appendFieldText(params, fieldText);
			if(fieldText != null && !fieldText.isEmpty()) {			
				for(Map.Entry<String, String> entry : fieldText.entrySet()) {
					params.put(entry.getKey(), entry.getValue());				
				}	
			}
	        params.add("FieldName", StringUtils.join(filteredFieldNames, "+"));

	        if (isAggregated) {
	            params.add("merge", true);
	        }

	        addRangesParam(filteredFieldNames, paraFieldMap, params);
	        
	        addSearchParamsVertica(params, requestData, null);

			final List<com.autonomy.vertica.fields.Field> fields = verticaService.searchParametricFieldValues(params, requestData, process);
	        if (isAggregated) {
	            if (fields != null && !fields.isEmpty()) {
	                fields.get(0).setName(aggregatedFieldname);
	            }
	        }
	        final List<com.autonomy.vertica.fields.Field> viewFields = new ArrayList<com.autonomy.vertica.fields.Field>();
	        final String idolRootElement = config.getSearchViews().get(requestData.getSearchView()).getIdolRootElement();
	        final String viewFieldNamePrefix = FieldUtil.getSearchViewFieldnamePrefix(idolRootElement);

			for (final com.autonomy.vertica.fields.Field field : fields) {
	            final String testFieldName = field.getName().replaceFirst(viewFieldNamePrefix, "");
	            if (filteredFieldNames.contains(testFieldName)) {
	                if (rename && !isAggregated) {
	                    field.setName(testFieldName);
	                }

	                viewFields.add(field);
	            }
			}

			return viewFields;
		}
	 
	/**
	  * This method returns the reference field (Encounter ID of the Patient ID) based on the result view
	 * @param requestData
	 * @return
	 */
	
	 
	public AciParameters addSearchParamsVertica(
	            final AciParameters params,
	            final SearchRequestData requestData,
	            final String maxResultsStr
	    ) {
	        //  Pagination offset
			if (StringUtils.isNotEmpty(maxResultsStr)) {
				final int maxResults = Integer.parseInt(maxResultsStr);
				final int offset = maxResults * requestData.getPage();
				params.put("start", offset);
				params.put("maxresults", maxResults);
			}		

	        return params;
	    }
	 
	   @Cacheable(cacheName="SearchService.search_trendingtotals")
	    public TrendingTotalsResponse trendingTotalsVertica(
	            final SearchRequestData requestData,
	            final String field,
	            final String graphType,
	            final String dateField1,
	            final String dateField2,
	            final String dateStart,
	            final String dateEnd,
	            final String sortType,
	            final String sortOrder
	    ) {
	        final Map<String, FilterField> fieldNames = parametricService.getFilterFieldNames(requestData.getSearchView());

	        if (!fieldNames.containsKey(field)) {
	            throw new IllegalArgumentException("Illegal fieldname specified");
	        }
	        
	        // Get the baseline Totals from Vertica
	        final TrendingTotalsData tdata = verticaService.getTrendingTotalsData(requestData, field,graphType,dateField1,dateField2,dateStart,dateEnd,sortType,sortOrder);
	        
		        
	        // Generate Totals Response
	        final TrendingTotalsResponse response = new TrendingTotalsResponse();

	        response.setData(tdata);
	        return response;
	    }

}
