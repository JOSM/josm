// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.OsmUrlToBounds;
import org.openstreetmap.josm.tools.UncheckedParseException;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Search for names and related items.
 * @since 11002
 */
public class NameFinder {

    public static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search?format=xml&q=";

    private NameFinder() {
    }

    public static List<SearchResult> queryNominatim(final String searchExpression) throws IOException {
        return query(new URL(NOMINATIM_URL + Utils.encodeUrl(searchExpression)));
    }

    public static List<SearchResult> query(final URL url) throws IOException {
        final HttpClient connection = HttpClient.create(url);
        connection.connect();
        try (Reader reader = connection.getResponse().getContentReader()) {
            return parseSearchResults(reader);
        } catch (ParserConfigurationException | SAXException ex) {
            throw new UncheckedParseException(ex);
        }
    }

    public static List<SearchResult> parseSearchResults(Reader reader) throws IOException, ParserConfigurationException, SAXException {
        InputSource inputSource = new InputSource(reader);
        NameFinderResultParser parser = new NameFinderResultParser();
        Utils.parseSafeSAX(inputSource, parser);
        return parser.getResult();
    }

    /**
     * Data storage for search results.
     */
    public static class SearchResult {
        public String name;
        public String info;
        public String nearestPlace;
        public String description;
        public double lat;
        public double lon;
        public int zoom;
        public Bounds bounds;
        public PrimitiveId osmId;

        public Bounds getDownloadArea() {
            return bounds != null ? bounds : OsmUrlToBounds.positionToBounds(lat, lon, zoom);
        }
    }

    /**
     * A very primitive parser for the name finder's output.
     * Structure of xml described here:  http://wiki.openstreetmap.org/index.php/Name_finder
     */
    private static class NameFinderResultParser extends DefaultHandler {
        private SearchResult currentResult;
        private StringBuilder description;
        private int depth;
        private final List<SearchResult> data = new LinkedList<>();

        /**
         * Detect starting elements.
         */
        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
                throws SAXException {
            depth++;
            try {
                if ("searchresults".equals(qName)) {
                    // do nothing
                } else if ("named".equals(qName) && (depth == 2)) {
                    currentResult = new SearchResult();
                    currentResult.name = atts.getValue("name");
                    currentResult.info = atts.getValue("info");
                    if (currentResult.info != null) {
                        currentResult.info = tr(currentResult.info);
                    }
                    currentResult.lat = Double.parseDouble(atts.getValue("lat"));
                    currentResult.lon = Double.parseDouble(atts.getValue("lon"));
                    currentResult.zoom = Integer.parseInt(atts.getValue("zoom"));
                    data.add(currentResult);
                } else if ("description".equals(qName) && (depth == 3)) {
                    description = new StringBuilder();
                } else if ("named".equals(qName) && (depth == 4)) {
                    // this is a "named" place in the nearest places list.
                    String info = atts.getValue("info");
                    if ("city".equals(info) || "town".equals(info) || "village".equals(info)) {
                        currentResult.nearestPlace = atts.getValue("name");
                    }
                } else if ("place".equals(qName) && atts.getValue("lat") != null) {
                    currentResult = new SearchResult();
                    currentResult.name = atts.getValue("display_name");
                    currentResult.description = currentResult.name;
                    currentResult.info = atts.getValue("class");
                    if (currentResult.info != null) {
                        currentResult.info = tr(currentResult.info);
                    }
                    currentResult.nearestPlace = tr(atts.getValue("type"));
                    currentResult.lat = Double.parseDouble(atts.getValue("lat"));
                    currentResult.lon = Double.parseDouble(atts.getValue("lon"));
                    String[] bbox = atts.getValue("boundingbox").split(",");
                    currentResult.bounds = new Bounds(
                            Double.parseDouble(bbox[0]), Double.parseDouble(bbox[2]),
                            Double.parseDouble(bbox[1]), Double.parseDouble(bbox[3]));
                    final String osmId = atts.getValue("osm_id");
                    final String osmType = atts.getValue("osm_type");
                    if (osmId != null && osmType != null) {
                        currentResult.osmId = new SimplePrimitiveId(Long.parseLong(osmId), OsmPrimitiveType.from(osmType));
                    }
                    data.add(currentResult);
                }
            } catch (NumberFormatException x) {
                Main.error(x); // SAXException does not chain correctly
                throw new SAXException(x.getMessage(), x);
            } catch (NullPointerException x) {
                Main.error(x); // SAXException does not chain correctly
                throw new SAXException(tr("Null pointer exception, possibly some missing tags."), x);
            }
        }

        /**
         * Detect ending elements.
         */
        @Override
        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            if ("description".equals(qName) && description != null) {
                currentResult.description = description.toString();
                description = null;
            }
            depth--;
        }

        /**
         * Read characters for description.
         */
        @Override
        public void characters(char[] data, int start, int length) throws SAXException {
            if (description != null) {
                description.append(data, start, length);
            }
        }

        public List<SearchResult> getResult() {
            return data;
        }
    }
}
