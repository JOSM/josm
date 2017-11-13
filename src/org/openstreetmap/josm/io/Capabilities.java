// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Represents the OSM API server capabilities.
 *
 * Example capabilities document:
 * <pre>
 * &lt;osm version="0.6" generator="OpenStreetMap server"&gt;
 *   &lt;api&gt;
 *     &lt;version minimum="0.6" maximum="0.6"/&gt;
 *     &lt;area maximum="0.25"/&gt;
 *     &lt;tracepoints per_page="5000"/&gt;
 *     &lt;waynodes maximum="2000"/&gt;
 *     &lt;changesets maximum_elements="10000"/&gt;
 *     &lt;timeout seconds="300"/&gt;
 *     &lt;status database="online" api="online" gpx="online"/&gt;
 *   &lt;/api&gt;
 *   &lt;policy&gt;
 *     &lt;imagery&gt;
 *       &lt;blacklist regex=".*\.google\.com/.*"/&gt;
 *       &lt;blacklist regex=".*209\.85\.2\d\d.*"/&gt;
 *       &lt;blacklist regex=".*209\.85\.1[3-9]\d.*"/&gt;
 *       &lt;blacklist regex=".*209\.85\.12[89].*"/&gt;
 *     &lt;/imagery&gt;
 *   &lt;/policy&gt;
 * &lt;/osm&gt;
 * </pre>
 * This class is used in conjunction with a very primitive parser
 * and simply stuffs the each tag and its attributes into a hash
 * of hashes, with the exception of the "blacklist" tag which gets
 * a list of its own. The DOM hierarchy is disregarded.
 */
public class Capabilities {

    private final Map<String, Map<String, String>> capabilities;
    private final List<String> imageryBlacklist;

    /**
     * Constructs new {@code Capabilities}.
     */
    public Capabilities() {
        capabilities = new HashMap<>();
        imageryBlacklist = new ArrayList<>();
    }

    /**
     * Determines if given element and attribute are defined.
     *
     * @param element the name of the element
     * @param attribute the name of the attribute
     * @return {@code true} if defined, {@code false} otherwise
     */
    public boolean isDefined(String element, String attribute) {
        if (!capabilities.containsKey(element)) return false;
        Map<String, String> e = capabilities.get(element);
        if (e == null) return false;
        return e.get(attribute) != null;
    }

    /**
     * Returns the value of configuration item in the capabilities as string value.
     *
     * @param element the name of the element
     * @param attribute the name of the attribute
     * @return the value; {@code null}, if the respective configuration item does not exist
     */
    public String get(String element, String attribute) {
        if (!capabilities.containsKey(element)) return null;
        Map<String, String> e = capabilities.get(element);
        if (e == null) return null;
        return e.get(attribute);
    }

    /**
     * Returns the value of configuration item in the capabilities as double value.
     *
     * @param element the name of the element
     * @param attribute the name of the attribute
     * @return the value; {@code null}, if the respective configuration item does not exist
     * @throws NumberFormatException if the value is not a valid double
     */
    public Double getDouble(String element, String attribute) {
        String s = get(element, attribute);
        if (s == null) return null;
        return Double.valueOf(s);
    }

    /**
     * Returns the value of configuration item in the capabilities as long value.
     *
     * @param element the name of the element
     * @param attribute the name of the attribute
     * @return the value; {@code null}, if the respective configuration item does not exist
     * @throws NumberFormatException if the value is not a valid long
     */
    public Long getLong(String element, String attribute) {
        String s = get(element, attribute);
        if (s == null) return null;
        return Long.valueOf(s);
    }

    /**
     * Adds a new configuration item.
     *
     * @param element the name of the element
     * @param attribute the name of the attribute
     * @param value the value as string
     */
    public void put(String element, String attribute, String value) {
        if ("blacklist".equals(element)) {
            if ("regex".equals(attribute)) {
                imageryBlacklist.add(value);
            }
        } else {
            if (!capabilities.containsKey(element)) {
                capabilities.put(element, new HashMap<>());
            }
            capabilities.get(element).put(attribute, value);
        }
    }

    /**
     * Clears the API capabilities.
     */
    public final void clear() {
        capabilities.clear();
        imageryBlacklist.clear();
    }

    /**
     * Determines if a given API version is supported.
     * @param version The API version to check
     * @return {@code true} is version is between the minimum supported version and the maximum one, {@code false} otherwise
     */
    public boolean supportsVersion(String version) {
        String min = get("version", "minimum");
        String max = get("version", "maximum");
        return min != null && max != null
            && min.compareTo(version) <= 0
            && max.compareTo(version) >= 0;
    }

    private static void warnIllegalValue(String attr, String elem, Object val) {
        Logging.warn(tr("Illegal value of attribute ''{0}'' of element ''{1}'' in server capabilities. Got ''{2}''", attr, elem, val));
    }

    /**
     * Returns the max number of objects in a changeset. -1 if either the capabilities
     * don't include this parameter or if the parameter value is illegal (not a number,
     * a negative number)
     *
     * @return the max number of objects in a changeset
     */
    public int getMaxChangesetSize() {
        String v = get("changesets", "maximum_elements");
        if (v != null) {
            try {
                int n = Integer.parseInt(v);
                if (n <= 0) {
                    warnIllegalValue("changesets", "maximum_elements", n);
                } else {
                    return n;
                }
            } catch (NumberFormatException e) {
                warnIllegalValue("changesets", "maximum_elements", v);
            }
        }
        return -1;
    }

    /**
     * Returns the max number of nodes in a way. -1 if either the capabilities
     * don't include this parameter or if the parameter value is illegal (not a number,
     * a negative number)
     *
     * @return the max number of nodes in a way
     */
    public long getMaxWayNodes() {
        String v = get("waynodes", "maximum");
        if (v != null) {
            try {
                long n = Long.parseLong(v);
                if (n <= 0) {
                    warnIllegalValue("waynodes", "maximum", n);
                } else {
                    return n;
                }
            } catch (NumberFormatException e) {
                warnIllegalValue("waynodes", "maximum", v);
            }
        }
        return -1;
    }

    /**
     * Checks if the given URL is blacklisted by one of the of the regular expressions.
     * @param url Imagery URL to check
     * @return {@code true} if URL is blacklisted, {@code false} otherwise
     */
    public boolean isOnImageryBlacklist(String url) {
        if (url != null && imageryBlacklist != null) {
            for (String blacklistRegex : imageryBlacklist) {
                if (url.matches(blacklistRegex))
                    return true;
            }
        }
        return false;
    }

    /**
     * Returns the full list of imagery blacklist regular expressions.
     * @return full list of imagery blacklist regular expressions
     */
    public List<String> getImageryBlacklist() {
        return Collections.unmodifiableList(imageryBlacklist);
    }

    /**
     * A parser for the "capabilities" response XML.
     * @since 7473
     */
    public static final class CapabilitiesParser extends DefaultHandler {

        private Capabilities capabilities;

        @Override
        public void startDocument() {
            capabilities = new Capabilities();
        }

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) {
            for (int i = 0; i < atts.getLength(); i++) {
                capabilities.put(qName, atts.getQName(i), atts.getValue(i));
            }
        }

        /**
         * Returns the read capabilities.
         * @return the read capabilities
         */
        public Capabilities getCapabilities() {
            return capabilities;
        }

        /**
         * Parses and returns capabilities from the given input source.
         *
         * @param inputSource The input source to read capabilities from
         * @return the capabilities
         * @throws SAXException if any SAX errors occur during processing
         * @throws IOException if any I/O errors occur
         * @throws ParserConfigurationException if a parser cannot be created
         */
        public static Capabilities parse(InputSource inputSource) throws SAXException, IOException, ParserConfigurationException {
            CapabilitiesParser parser = new CapabilitiesParser();
            Utils.parseSafeSAX(inputSource, parser);
            return parser.getCapabilities();
        }
    }
}
