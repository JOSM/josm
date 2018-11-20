// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.function.BiPredicate;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.XmlUtils;

/**
 * Helper class for handling OGC GetCapabilities documents
 * @since 10993
 */
public final class GetCapabilitiesParseHelper {
    enum TransferMode {
        KVP("KVP"),
        REST("RESTful");

        private final String typeString;

        TransferMode(String urlString) {
            this.typeString = urlString;
        }

        private String getTypeString() {
            return typeString;
        }

        static TransferMode fromString(String s) {
            for (TransferMode type : TransferMode.values()) {
                if (type.getTypeString().equals(s)) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * OWS namespace address
     */
    public static final String OWS_NS_URL = "http://www.opengis.net/ows/1.1";
    /**
     * XML xlink namespace address
     */
    public static final String XLINK_NS_URL = "http://www.w3.org/1999/xlink";

    /**
     * QNames in OWS namespace
     */
    // CHECKSTYLE.OFF: SingleSpaceSeparator
    static final QName QN_OWS_ALLOWED_VALUES      = new QName(OWS_NS_URL, "AllowedValues");
    static final QName QN_OWS_CONSTRAINT          = new QName(OWS_NS_URL, "Constraint");
    static final QName QN_OWS_DCP                 = new QName(OWS_NS_URL, "DCP");
    static final QName QN_OWS_GET                 = new QName(OWS_NS_URL, "Get");
    static final QName QN_OWS_HTTP                = new QName(OWS_NS_URL, "HTTP");
    static final QName QN_OWS_IDENTIFIER          = new QName(OWS_NS_URL, "Identifier");
    static final QName QN_OWS_OPERATION           = new QName(OWS_NS_URL, "Operation");
    static final QName QN_OWS_OPERATIONS_METADATA = new QName(OWS_NS_URL, "OperationsMetadata");
    static final QName QN_OWS_SUPPORTED_CRS       = new QName(OWS_NS_URL, "SupportedCRS");
    static final QName QN_OWS_TITLE               = new QName(OWS_NS_URL, "Title");
    static final QName QN_OWS_VALUE               = new QName(OWS_NS_URL, "Value");
    // CHECKSTYLE.ON: SingleSpaceSeparator

    private GetCapabilitiesParseHelper() {
        // Hide default constructor for utilities classes
    }

    /**
     * Returns reader with properties set for parsing WM(T)S documents
     *
     * @param in InputStream with pointing to GetCapabilities XML stream
     * @return safe XMLStreamReader, that is not validating external entities, nor loads DTD's
     * @throws XMLStreamException if any XML stream error occurs
     */
    public static XMLStreamReader getReader(InputStream in) throws XMLStreamException {
        return XmlUtils.newSafeXMLInputFactory().createXMLStreamReader(in);
    }

    /**
     * Moves the reader to the closing tag of current tag.
     * @param reader XMLStreamReader which should be moved
     * @throws XMLStreamException when parse exception occurs
     */
    public static void moveReaderToEndCurrentTag(XMLStreamReader reader) throws XMLStreamException {
        int level = 0;
        QName tag = reader.getName();
        for (int event = reader.getEventType(); reader.hasNext(); event = reader.next()) {
            if (XMLStreamReader.START_ELEMENT == event) {
                level += 1;
            } else if (XMLStreamReader.END_ELEMENT == event) {
                level -= 1;
                if (level == 0 && tag.equals(reader.getName())) {
                    return;
                }
            }
            if (level < 0) {
                throw new IllegalStateException("WMTS Parser error - moveReaderToEndCurrentTag failed to find closing tag");
            }
        }
        throw new IllegalStateException("WMTS Parser error - moveReaderToEndCurrentTag failed to find closing tag");
    }

    /**
     * Returns whole content of the element that reader is pointing at, including other XML elements within (with their tags).
     *
     * @param reader XMLStreamReader that should point to start of element
     * @return content of current tag
     * @throws XMLStreamException if any XML stream error occurs
     */
    public static String getElementTextWithSubtags(XMLStreamReader reader) throws XMLStreamException {
        StringBuilder ret = new StringBuilder();
        int level = 0;
        QName tag = reader.getName();
        for (int event = reader.getEventType(); reader.hasNext(); event = reader.next()) {
            if (XMLStreamReader.START_ELEMENT == event) {
                if (level > 0) {
                    ret.append('<').append(reader.getLocalName()).append('>');
                }
                level += 1;
            } else if (XMLStreamReader.END_ELEMENT == event) {
                level -= 1;
                if (level == 0 && tag.equals(reader.getName())) {
                    return ret.toString();
                }
                ret.append("</").append(reader.getLocalName()).append('>');
            } else if (XMLStreamReader.CHARACTERS == event) {
                ret.append(reader.getText());
            }
            if (level < 0) {
                throw new IllegalStateException("WMTS Parser error - moveReaderToEndCurrentTag failed to find closing tag");
            }
        }
        throw new IllegalStateException("WMTS Parser error - moveReaderToEndCurrentTag failed to find closing tag");
    }


    /**
     * Moves reader to first occurrence of the structure equivalent of Xpath tags[0]/tags[1]../tags[n]. If fails to find
     * moves the reader to the closing tag of current tag
     *
     * @param tags array of tags
     * @param reader XMLStreamReader which should be moved
     * @return true if tag was found, false otherwise
     * @throws XMLStreamException See {@link XMLStreamReader}
     */
    public static boolean moveReaderToTag(XMLStreamReader reader, QName... tags) throws XMLStreamException {
        return moveReaderToTag(reader, QName::equals, tags);
    }

    /**
     * Moves reader to first occurrence of the structure equivalent of Xpath tags[0]/tags[1]../tags[n]. If fails to find
     * moves the reader to the closing tag of current tag
     *
     * @param tags array of tags
     * @param reader XMLStreamReader which should be moved
     * @param equalsFunc function to check equality of the tags
     * @return true if tag was found, false otherwise
     * @throws XMLStreamException See {@link XMLStreamReader}
     */
    public static boolean moveReaderToTag(XMLStreamReader reader,
            BiPredicate<QName, QName> equalsFunc, QName... tags) throws XMLStreamException {
        QName stopTag = reader.getName();
        int currentLevel = 0;
        QName searchTag = tags[currentLevel];
        QName parentTag = null;
        QName skipTag = null;

        for (int event = 0; //skip current element, so we will not skip it as a whole
                reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT && equalsFunc.test(stopTag, reader.getName()));
                event = reader.next()) {
            if (event == XMLStreamReader.END_ELEMENT && skipTag != null && equalsFunc.test(skipTag, reader.getName())) {
                skipTag = null;
            }
            if (skipTag == null) {
                if (event == XMLStreamReader.START_ELEMENT) {
                    if (equalsFunc.test(searchTag, reader.getName())) {
                        currentLevel += 1;
                        if (currentLevel >= tags.length) {
                            return true; // found!
                        }
                        parentTag = searchTag;
                        searchTag = tags[currentLevel];
                    } else {
                        skipTag = reader.getName();
                    }
                }

                if (event == XMLStreamReader.END_ELEMENT && parentTag != null && equalsFunc.test(parentTag, reader.getName())) {
                    currentLevel -= 1;
                    searchTag = parentTag;
                    if (currentLevel >= 0) {
                        parentTag = tags[currentLevel];
                    } else {
                        parentTag = null;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Parses Operation[@name='GetTile']/DCP/HTTP/Get section. Returns when reader is on Get closing tag.
     * @param reader StAX reader instance
     * @return TransferMode coded in this section
     * @throws XMLStreamException See {@link XMLStreamReader}
     */
    public static TransferMode getTransferMode(XMLStreamReader reader) throws XMLStreamException {
        QName getQname = QN_OWS_GET;

        Utils.ensure(getQname.equals(reader.getName()), "WMTS Parser state invalid. Expected element %s, got %s",
                getQname, reader.getName());
        for (int event = reader.getEventType();
                reader.hasNext() && !(event == XMLStreamReader.END_ELEMENT && getQname.equals(reader.getName()));
                event = reader.next()) {
            if (event == XMLStreamReader.START_ELEMENT && QN_OWS_CONSTRAINT.equals(reader.getName())
             && "GetEncoding".equals(reader.getAttributeValue("", "name"))) {
                moveReaderToTag(reader, QN_OWS_ALLOWED_VALUES, QN_OWS_VALUE);
                return TransferMode.fromString(reader.getElementText());
            }
        }
        return null;
    }

    /**
     * Normalize url
     *
     * @param url URL
     * @return normalized URL
     * @throws MalformedURLException in case of malformed URL
     * @since 10993
     */
    public static String normalizeCapabilitiesUrl(String url) throws MalformedURLException {
        URL inUrl = new URL(url);
        URL ret = new URL(inUrl.getProtocol(), inUrl.getHost(), inUrl.getPort(), inUrl.getFile());
        return ret.toExternalForm();
    }

    /**
     * Convert CRS identifier to plain code
     * @param crsIdentifier CRS identifier
     * @return CRS Identifier as it is used within JOSM (without prefix)
     * @see <a href="https://portal.opengeospatial.org/files/?artifact_id=24045">
     *     Definition identifier URNs in OGC namespace, chapter 7.2: URNs for single objects</a>
     */
    public static String crsToCode(String crsIdentifier) {
        if (crsIdentifier.startsWith("urn:ogc:def:crs:")) {
            return crsIdentifier.replaceFirst("urn:ogc:def:crs:([^:]*)(?::.*)?:(.*)$", "$1:$2").toUpperCase(Locale.ENGLISH);
        }
        return crsIdentifier;
    }
}
