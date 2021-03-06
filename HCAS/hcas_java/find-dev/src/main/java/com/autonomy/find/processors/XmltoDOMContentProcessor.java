package com.autonomy.find.processors;

/*
 */

import com.autonomy.aci.client.services.AciErrorException;
import com.autonomy.aci.client.services.Processor;
import com.autonomy.aci.client.services.ProcessorException;
import com.autonomy.aci.client.transport.AciResponseInputStream;
import com.autonomy.aci.client.util.DateTimeUtils;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * <tt>Processor</tt> implementation that converts an ACI response into a DOM <tt>Document</tt> for further processing
 * either directly via DOM or via XPath.
 *
 * @author boba
 * @version $Revision: #10 $ $Date: 2009/12/14 $
 */
public class XmltoDOMContentProcessor implements Processor<Document> {

    private static final long serialVersionUID = -1757174558649421058L;

    private static final Logger LOGGER = LoggerFactory.getLogger(XmltoDOMContentProcessor.class);

    /**
     * Converts the <tt>InputStream</tt> into a DOM <tt>Document</tt>.
     *
     * @param response The <tt>InputStream to convert</tt>
     * @return The resulting DOM <tt>Document</tt>
     * @throws ProcessorException If there was an exception while parsing the input
     *         stream or configuring the <tt>DocumentBuilderFactory</tt>.
     */
    private Document convertACIResponseToDOM(final InputStream response) {
        LOGGER.trace("convertACIResponseToDOM() called...");

        try {
            // Create the factory...
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            // Should speed things up a bit...
            factory.setNamespaceAware(true);
            factory.setValidating(false);

            LOGGER.debug("Converting ACI response to DOM Document...");

            // Get a document builder and convert the document...
            return factory.newDocumentBuilder().parse(new InputSource(response));
        }
        catch(ParserConfigurationException pce) {
            throw new ProcessorException("Unable to parse the ACI response into a DOM document.", pce);
        }
        catch(SAXException saxe) {
            throw new ProcessorException("Unable to parse the ACI response into a DOM document.", saxe);
        }
        catch(IOException ioe) {
            throw new ProcessorException("Unable to parse the ACI response into a DOM document.", ioe);
        }
    }

    /**
     * Checks the DOM <tt>Document</tt> to see if it is an ACI Server error response. If it is, it pulls all the
     * information contained in the response into an <tt>AciErrorException</tt> and throws it, otherwise it does
     * nothing.
     *
     * @param response The DOM <tt>Document</tt> to check.
     * @throws com.autonomy.aci.client.services.AciErrorException If an error response was detected.
     * @throws ProcessorException If there was any other kind of exception caught during processing.
     */
    private void checkACIResponseForError(final Document response) {
        LOGGER.trace("checkACIResponseForError() called...");

        // Create an xpath object for getting stuff with...
        final XPath xpath = XPathFactory.newInstance().newXPath();

        try {
            // Get the value of the <response> tag...
            final String value = xpath.evaluate("/autnresponse/response", response);

            LOGGER.debug("Response tag has value - {}...", value);

            if("ERROR".equals(value)) {
                LOGGER.debug("Error response detected, creating an AciErrorException...");

                // Create an exception to throw...
                final AciErrorException error = new AciErrorException();

                // Get the error properties...
                error.setErrorId(xpath.evaluate("/autnresponse/responsedata/error/errorid", response));
                error.setRawErrorId(xpath.evaluate("/autnresponse/responsedata/error/rawerrorid", response));
                error.setErrorString(xpath.evaluate("/autnresponse/responsedata/error/errorstring", response));
                error.setErrorDescription(xpath.evaluate("/autnresponse/responsedata/error/errordescription", response));
                error.setErrorCode(xpath.evaluate("/autnresponse/responsedata/error/errorcode", response));
                try {
                    error.setErrorTime(DateTimeUtils.getInstance().parseDate(xpath.evaluate("/autnresponse/responsedata/error/errortime", response), "dd MMM yy HH:mm:ss"));
                }
                catch(ParseException pe) {
                    LOGGER.error("ParseException caught while trying to convert errortime tag into java.util.Date.", pe);
                }

                // Throw the error...
                throw error;
            }
        }
        catch(XPathExpressionException xpee) {
            // Throw a wobbler, as this should never happen...
            throw new ProcessorException("XPathExpressionException caught while trying to check ACI response for ERROR flag.", xpee);
        }
    }

    /**
     * Process the ACI response input into a DOM <tt>Document</tt>.
     *
     * @param aciResponse The ACI response to process
     * @return A DOM <tt>Document</tt>
     * @throws com.autonomy.aci.client.services.AciErrorException If the ACI response was an error response
     * @throws ProcessorException If an error occurred during the processing of the ACI response
     */
    public Document process(final AciResponseInputStream aciResponse) {
        LOGGER.trace("process() called...");

        // Convert the stream into a DOM Document...
        final Document document = convertACIResponseToDOM(aciResponse);

        LOGGER.debug("Checking for ERROR response...");

        // Check the response for an error flag...
        checkACIResponseForError(document);

        LOGGER.debug("Returning the DOM Document to caller...");

        // Return the document...
        return document;
    }

} // End of class DocumentProcessor... 
