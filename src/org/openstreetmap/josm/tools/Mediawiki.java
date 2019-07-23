// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Interaction with Mediawiki instances, such as the OSM wiki.
 * @since 14641
 */
public class Mediawiki {

    private final String baseUrl;

    /**
     * Constructs a new {@code Mediawiki} for the given base URL.
     * @param baseUrl The wiki base URL
     */
    public Mediawiki(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * Determines which page exists on the Mediawiki instance.
     * @param pages the pages to check
     * @return the first existing page
     * @throws IOException if any I/O error occurs
     * @throws ParserConfigurationException if a parser cannot be created
     * @throws SAXException if any XML error occurs
     * @throws XPathExpressionException if any error in an XPath expression occurs
     */
    public Optional<String> findExistingPage(List<String> pages)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        List<String> distinctPages = pages.stream().distinct().collect(Collectors.toList());
        // find a page that actually exists in the wiki
        // API documentation: https://wiki.openstreetmap.org/w/api.php?action=help&modules=query
        final URL url = new URL(baseUrl + "/w/api.php?action=query&format=xml&titles=" + distinctPages.stream()
                .map(Utils::encodeUrl)
                .collect(Collectors.joining(Utils.encodeUrl("|")))
        );
        final HttpClient.Response conn = HttpClient.create(url).connect();
        final Document document;
        try (InputStream content = conn.getContent()) {
            document = XmlUtils.parseSafeDOM(content);
        }
        conn.disconnect();
        final XPath xPath = XPathFactory.newInstance().newXPath();
        for (String page : distinctPages) {
            String normalized = xPath.evaluate("/api/query/normalized/n[@from='" + page + "']/@to", document);
            if (normalized == null || normalized.isEmpty()) {
                normalized = page;
            }
            final Node node = (Node) xPath.evaluate("/api/query/pages/page[@title='" + normalized + "']", document, XPathConstants.NODE);
            if (node != null
                    && node.getAttributes().getNamedItem("missing") == null
                    && node.getAttributes().getNamedItem("invalid") == null) {
                return Optional.of(page);
            }
        }
        return Optional.empty();
    }
}
