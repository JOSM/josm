// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
        final Document document = getDocument(url);
        final XPath xPath = XPathFactory.newInstance().newXPath();
        for (String page : distinctPages) {
            String normalized = xPath.evaluate("/api/query/normalized/n[@from='" + page + "']/@to", document);
            if (Utils.isEmpty(normalized)) {
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

    private Document getDocument(URL url) throws IOException, ParserConfigurationException, SAXException {
        final HttpClient.Response conn = HttpClient.create(url).connect();
        try (InputStream content = conn.getContent()) {
            return XmlUtils.parseSafeDOM(content);
        } finally {
            conn.disconnect();
        }
    }

    /**
     * Searches geocoded images from <a href="https://commons.wikimedia.org/">Wikimedia Commons</a> for the given bounding box.
     * @param bounds the bounds to load
     * @param imageConsumer a consumer to receive the file title and the coordinates for every geocoded image
     * @throws IOException if any I/O error occurs
     * @throws ParserConfigurationException if a parser cannot be created
     * @throws SAXException if any XML error occurs
     * @throws XPathExpressionException if any error in an XPath expression occurs
     */
    public void searchGeoImages(Bounds bounds, BiConsumer<String, LatLon> imageConsumer)
            throws IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        final URL url = new URL(getGeoImagesUrl(baseUrl, bounds));
        final Document document = getDocument(url);
        final XPath xPath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList) xPath.evaluate("/api/query/geosearch/gs", document, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            NamedNodeMap attributes = nodes.item(i).getAttributes();
            String title = attributes.getNamedItem("title").getNodeValue();
            double lat = Double.parseDouble(attributes.getNamedItem("lat").getNodeValue());
            double lon = Double.parseDouble(attributes.getNamedItem("lon").getNodeValue());
            imageConsumer.accept(title, new LatLon(lat, lon));
        }
    }

    /**
     * Returns the URL for searching geolocated images in given bounds.
     * @param baseUrl The wiki base URL
     * @param bounds the bounds of the search area
     * @return the URL for searching geolocated images in given bounds
     * @since 18046
     */
    public static String getGeoImagesUrl(String baseUrl, Bounds bounds) {
        String sep = Utils.encodeUrl("|");
        return baseUrl +
                "?format=xml" +
                "&action=query" +
                "&list=geosearch" +
                "&gsnamespace=6" +
                "&gslimit=500" +
                "&gsprop=type" + sep + "name" +
                "&gsbbox=" + bounds.getMaxLat() + sep + bounds.getMinLon() + sep + bounds.getMinLat() + sep + bounds.getMaxLon();
    }

    /**
     * Computes the URL for the given filename on the MediaWiki server
     * @param fileBaseUrl the base URL of the file MediaWiki storage, such as {@code "https://upload.wikimedia.org/wikipedia/commons/"}
     * @param filename    the filename
     * @return the URL for the given filename on the MediaWiki server
     * @see <a href="https://www.mediawiki.org/wiki/Manual:$wgHashedUploadDirectory">MediaWiki $wgHashedUploadDirectory</a>
     */
    public static String getImageUrl(String fileBaseUrl, String filename) {
        final String md5 = Utils.md5Hex(filename);
        return String.join("/", Utils.strip(fileBaseUrl, "/"), md5.substring(0, 1), md5.substring(0, 2), filename);
    }
}
