// License: GPL. For details, see LICENSE file.
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.stream.IntStream;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.openstreetmap.josm.data.preferences.JosmUrls;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetReader;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This script generates the wiki content for https://josm.openstreetmap.de/wiki/TaggingPresets#Attributes
 */
public final class TaggingPresetSchemeWikiGenerator {

    private static Document document;
    private static XPath xPath;

    private TaggingPresetSchemeWikiGenerator() {
        // Hide public constructor for utility class
    }

    public static void main(String[] args) throws Exception {
        document = parseTaggingPresetSchema();
        xPath = XPathFactory.newInstance().newXPath();
        xPath.setNamespaceContext(new TaggingNamespaceContext());
        printAttributes();
    }

    private static Document parseTaggingPresetSchema() throws IOException, ParserConfigurationException, SAXException {
        Config.setUrlsProvider(JosmUrls.getInstance());
        Document document;
        try (CachedFile file = new CachedFile(TaggingPresetReader.SCHEMA_SOURCE);
             InputStream in = file.getInputStream()) {
            document = XmlUtils.parseSafeDOM(in);
        }
        return document;
    }

    private static void printAttributes() throws XPathExpressionException {
        NodeList attributes = (NodeList) xPath.compile("/xs:schema/xs:attributeGroup/xs:attribute").evaluate(document, XPathConstants.NODESET);
        System.out.println("=== Attributes ===");
        System.out.println("The attributes of the tags have the following meaning:");
        IntStream.range(0, attributes.getLength())
                .mapToObj(attributes::item)
                .forEach(node -> System.out.format(" `%s` (type: %s)%n  %s%n",
                        node.getAttributes().getNamedItem("name").getTextContent(),
                        node.getAttributes().getNamedItem("type").getTextContent(),
                        node.getTextContent().trim()));
    }

    private static class TaggingNamespaceContext implements NamespaceContext {
        @Override
        public String getNamespaceURI(String prefix) {
            switch (prefix) {
                case "tns":
                    return TaggingPresetReader.NAMESPACE;
                case "xs":
                    return "http://www.w3.org/2001/XMLSchema";
                default:
                    return XMLConstants.NULL_NS_URI;
            }
        }

        @Override
        public String getPrefix(String namespaceURI) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<String> getPrefixes(String namespaceURI) {
            throw new UnsupportedOperationException();
        }
    }
}
