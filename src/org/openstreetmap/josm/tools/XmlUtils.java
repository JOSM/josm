// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.SchemaFactoryConfigurationError;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * XML utils, mainly used to construct safe factories.
 * @since 13901
 */
public final class XmlUtils {

    private XmlUtils() {
        // Hide default constructor for utils classes
    }

    /**
     * Returns the W3C XML Schema factory implementation. Robust method dealing with ContextClassLoader problems.
     * @return the W3C XML Schema factory implementation
     */
    public static SchemaFactory newXmlSchemaFactory() {
        try {
            return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        } catch (SchemaFactoryConfigurationError e) {
            Logging.debug(e);
            // Can happen with icedtea-web. Use workaround from https://issues.apache.org/jira/browse/GERONIMO-6185
            Thread currentThread = Thread.currentThread();
            ClassLoader old = currentThread.getContextClassLoader();
            currentThread.setContextClassLoader(null);
            try {
                return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            } finally {
                currentThread.setContextClassLoader(old);
            }
        }
    }

    /**
     * Returns a new secure DOM builder, supporting XML namespaces.
     * @return a new secure DOM builder, supporting XML namespaces
     * @throws ParserConfigurationException if a parser cannot be created which satisfies the requested configuration.
     */
    public static DocumentBuilder newSafeDOMBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        builderFactory.setNamespaceAware(true);
        builderFactory.setValidating(false);
        return builderFactory.newDocumentBuilder();
    }

    /**
     * Parse the content given {@link InputStream} as XML.
     * This method uses a secure DOM builder, supporting XML namespaces.
     *
     * @param is The InputStream containing the content to be parsed.
     * @return the result DOM document
     * @throws ParserConfigurationException if a parser cannot be created which satisfies the requested configuration.
     * @throws IOException if any IO errors occur.
     * @throws SAXException for SAX errors.
     */
    public static Document parseSafeDOM(InputStream is) throws ParserConfigurationException, IOException, SAXException {
        long start = System.currentTimeMillis();
        Logging.debug("Starting DOM parsing of {0}", is);
        Document result = newSafeDOMBuilder().parse(is);
        if (Logging.isDebugEnabled()) {
            Logging.debug("DOM parsing done in {0}", Utils.getDurationString(System.currentTimeMillis() - start));
        }
        return result;
    }

    /**
     * Returns a new secure SAX parser, supporting XML namespaces.
     * @return a new secure SAX parser, supporting XML namespaces
     * @throws ParserConfigurationException if a parser cannot be created which satisfies the requested configuration.
     * @throws SAXException for SAX errors.
     */
    public static SAXParser newSafeSAXParser() throws ParserConfigurationException, SAXException {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        parserFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        parserFactory.setNamespaceAware(true);
        return parserFactory.newSAXParser();
    }

    /**
     * Parse the content given {@link org.xml.sax.InputSource} as XML using the specified {@link org.xml.sax.helpers.DefaultHandler}.
     * This method uses a secure SAX parser, supporting XML namespaces.
     *
     * @param is The InputSource containing the content to be parsed.
     * @param dh The SAX DefaultHandler to use.
     * @throws ParserConfigurationException if a parser cannot be created which satisfies the requested configuration.
     * @throws SAXException for SAX errors.
     * @throws IOException if any IO errors occur.
     */
    public static void parseSafeSAX(InputSource is, DefaultHandler dh) throws ParserConfigurationException, SAXException, IOException {
        long start = System.currentTimeMillis();
        Logging.debug("Starting SAX parsing of {0} using {1}", is, dh);
        newSafeSAXParser().parse(is, dh);
        if (Logging.isDebugEnabled()) {
            Logging.debug("SAX parsing done in {0}", Utils.getDurationString(System.currentTimeMillis() - start));
        }
    }

    /**
     * Returns a new secure {@link XMLInputFactory}.
     * @return a new secure {@code XMLInputFactory}, for which external entities are not loaded
     */
    public static XMLInputFactory newSafeXMLInputFactory() {
        XMLInputFactory factory = XMLInputFactory.newInstance();
        // do not try to load external entities, nor validate the XML
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        return factory;
    }

    /**
     * Returns a new secure {@link TransformerFactory}.
     * @return a new secure {@link TransformerFactory}
     * @throws TransformerConfigurationException if the factory or the Transformers or Templates it creates cannot support this feature.
     */
    public static TransformerFactory newSafeTransformerFactory() throws TransformerConfigurationException {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        return factory;
    }
}
