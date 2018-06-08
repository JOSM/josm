// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.tools.HttpClient;
import org.openstreetmap.josm.tools.HttpClient.Response;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.OsmUrlToBounds;
import org.openstreetmap.josm.tools.UncheckedParseException;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.XmlUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Search for names and related items.
 * @since 11002
 */
public final class NameFinder {

    /**
     * Nominatim default URL.
     */
    public static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search?format=xml&q=";

    /**
     * Nominatim URL property.
     * @since 12557
     */
    public static final StringProperty NOMINATIM_URL_PROP = new StringProperty("nominatim-url", NOMINATIM_URL);

    private NameFinder() {
    }

    /**
     * Performs a Nominatim search.
     * @param searchExpression Nominatim search expression
     * @return search results
     * @throws IOException if any IO error occurs.
     */
    public static List<SearchResult> queryNominatim(final String searchExpression) throws IOException {
        return query(new URL(NOMINATIM_URL_PROP.get() + Utils.encodeUrl(searchExpression)));
    }

    /**
     * Performs a custom search.
     * @param url search URL to any Nominatim instance
     * @return search results
     * @throws IOException if any IO error occurs.
     */
    public static List<SearchResult> query(final URL url) throws IOException {
        final HttpClient connection = HttpClient.create(url);
        Response response = connection.connect();
        if (response.getResponseCode() >= 400) {
            throw new IOException(response.getResponseMessage() + ": " + response.fetchContent());
        }
        try (Reader reader = response.getContentReader()) {
            return parseSearchResults(reader);
        } catch (ParserConfigurationException | SAXException ex) {
            throw new UncheckedParseException(ex);
        }
    }

    /**
     * Parse search results as returned by Nominatim.
     * @param reader reader
     * @return search results
     * @throws ParserConfigurationException if a parser cannot be created which satisfies the requested configuration.
     * @throws SAXException for SAX errors.
     * @throws IOException if any IO error occurs.
     */
    public static List<SearchResult> parseSearchResults(Reader reader) throws IOException, ParserConfigurationException, SAXException {
        InputSource inputSource = new InputSource(reader);
        NameFinderResultParser parser = new NameFinderResultParser();
        XmlUtils.parseSafeSAX(inputSource, parser);
        return parser.getResult();
    }

    /**
     * Data storage for search results.
     */
    public static class SearchResult {
        private String name;
        private String info;
        private String nearestPlace;
        private String description;
        private double lat;
        private double lon;
        private int zoom;
        private Bounds bounds;
        private PrimitiveId osmId;

        /**
         * Returns the name.
         * @return the name
         */
        public final String getName() {
            return name;
        }

        /**
         * Returns the info.
         * @return the info
         */
        public final String getInfo() {
            return info;
        }

        /**
         * Returns the nearest place.
         * @return the nearest place
         */
        public final String getNearestPlace() {
            return nearestPlace;
        }

        /**
         * Returns the description.
         * @return the description
         */
        public final String getDescription() {
            return description;
        }

        /**
         * Returns the latitude.
         * @return the latitude
         */
        public final double getLat() {
            return lat;
        }

        /**
         * Returns the longitude.
         * @return the longitude
         */
        public final double getLon() {
            return lon;
        }

        /**
         * Returns the zoom.
         * @return the zoom
         */
        public final int getZoom() {
            return zoom;
        }

        /**
         * Returns the bounds.
         * @return the bounds
         */
        public final Bounds getBounds() {
            return bounds;
        }

        /**
         * Returns the OSM id.
         * @return the OSM id
         */
        public final PrimitiveId getOsmId() {
            return osmId;
        }

        /**
         * Returns the download area.
         * @return the download area
         */
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
                } else if (depth == 2 && "named".equals(qName)) {
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
                } else if (depth == 3 && "description".equals(qName)) {
                    description = new StringBuilder();
                } else if (depth == 4 && "named".equals(qName)) {
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
            } catch (NumberFormatException ex) {
                Logging.error(ex); // SAXException does not chain correctly
                throw new SAXException(ex.getMessage(), ex);
            } catch (NullPointerException ex) { // NOPMD
                Logging.error(ex); // SAXException does not chain correctly
                throw new SAXException(tr("Null pointer exception, possibly some missing tags."), ex);
            }
        }

        /**
         * Detect ending elements.
         */
        @Override
        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            if (description != null && "description".equals(qName)) {
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
            return Collections.unmodifiableList(data);
        }
    }
}
