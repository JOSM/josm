// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.DateUtils;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parser for a list of changesets, encapsulated in an OSM data set structure.
 * Example:
 * <pre>
 * &lt;osm version="0.6" generator="OpenStreetMap server"&gt;
 *     &lt;changeset id="143" user="guggis" uid="1" created_at="2009-09-08T20:35:39Z" closed_at="2009-09-08T21:36:12Z" open="false" min_lon="7.380925" min_lat="46.9215164" max_lon="7.3984718" max_lat="46.9226502"&gt;
 *         &lt;tag k="asdfasdf" v="asdfasdf"/&gt;
 *         &lt;tag k="created_by" v="JOSM/1.5 (UNKNOWN de)"/&gt;
 *         &lt;tag k="comment" v="1234"/&gt;
 *     &lt;/changeset&gt;
 * &lt;/osm&gt;
 * </pre>
 *
 */
public final class OsmChangesetParser {
    private List<Changeset> changesets;

    private OsmChangesetParser() {
        changesets = new LinkedList<Changeset>();
    }

    public List<Changeset> getChangesets() {
        return changesets;
    }

    private class Parser extends DefaultHandler {
        private Locator locator;

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        protected void throwException(String msg) throws OsmDataParsingException{
            throw new OsmDataParsingException(msg).rememberLocation(locator);
        }
        /**
         * The current changeset
         */
        private Changeset current = null;

        protected void parseChangesetAttributes(Changeset cs, Attributes atts) throws OsmDataParsingException {
            // -- id
            String value = atts.getValue("id");
            if (value == null) {
                throwException(tr("Missing mandatory attribute ''{0}''.", "id"));
            }
            int id = 0;
            try {
                id = Integer.parseInt(value);
            } catch(NumberFormatException e) {
                throwException(tr("Illegal value for attribute ''{0}''. Got ''{1}''.", "id", value));
            }
            if (id <= 0) {
                throwException(tr("Illegal numeric value for attribute ''{0}''. Got ''{1}''.", "id", id));
            }
            current.setId(id);

            // -- user
            String user = atts.getValue("user");
            String uid = atts.getValue("uid");
            current.setUser(createUser(uid, user));

            // -- created_at
            value = atts.getValue("created_at");
            if (value == null) {
                current.setCreatedAt(null);
            } else {
                current.setCreatedAt(DateUtils.fromString(value));
            }

            // -- closed_at
            value = atts.getValue("closed_at");
            if (value == null) {
                current.setClosedAt(null);
            } else {
                current.setClosedAt(DateUtils.fromString(value));
            }

            //  -- open
            value = atts.getValue("open");
            if (value == null) {
                throwException(tr("Missing mandatory attribute ''{0}''.", "open"));
            } else if (value.equals("true")) {
                current.setOpen(true);
            } else if (value.equals("false")) {
                current.setOpen(false);
            } else {
                throwException(tr("Illegal boolean value for attribute ''{0}''. Got ''{1}''.", "open", value));
            }

            // -- min_lon and min_lat
            String min_lon = atts.getValue("min_lon");
            String min_lat = atts.getValue("min_lat");
            String max_lon = atts.getValue("max_lon");
            String max_lat = atts.getValue("max_lat");
            if (min_lon != null && min_lat != null && max_lon != null && max_lat != null) {
                double minLon = 0;
                try {
                    minLon = Double.parseDouble(min_lon);
                } catch(NumberFormatException e) {
                    throwException(tr("Illegal value for attribute ''{0}''. Got ''{1}''.", "min_lon", min_lon));
                }
                double minLat = 0;
                try {
                    minLat = Double.parseDouble(min_lat);
                } catch(NumberFormatException e) {
                    throwException(tr("Illegal value for attribute ''{0}''. Got ''{1}''.", "min_lat", min_lat));
                }
                current.setMin(new LatLon(minLat, minLon));

                // -- max_lon and max_lat

                double maxLon = 0;
                try {
                    maxLon = Double.parseDouble(max_lon);
                } catch(NumberFormatException e) {
                    throwException(tr("Illegal value for attribute ''{0}''. Got ''{1}''.", "max_lon", max_lon));
                }
                double maxLat = 0;
                try {
                    maxLat = Double.parseDouble(max_lat);
                } catch(NumberFormatException e) {
                    throwException(tr("Illegal value for attribute ''{0}''. Got ''{1}''.", "max_lat", max_lat));
                }
                current.setMax(new LatLon(maxLon, maxLat));
            }
        }

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (qName.equals("osm")) {
                if (atts == null) {
                    throwException(tr("Missing mandatory attribute ''{0}'' of XML element {1}.", "version", "osm"));
                }
                String v = atts.getValue("version");
                if (v == null) {
                    throwException(tr("Missing mandatory attribute ''{0}''.", "version"));
                }
                if (!(v.equals("0.6"))) {
                    throwException(tr("Unsupported version: {0}", v));
                }
            } else if (qName.equals("changeset")) {
                current = new Changeset();
                parseChangesetAttributes(current, atts);
            } else if (qName.equals("tag")) {
                String key = atts.getValue("k");
                String value = atts.getValue("v");
                current.put(key, value);
            } else {
                throwException(tr("Undefined element ''{0}'' found in input stream. Aborting.", qName));
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("changeset")) {
                changesets.add(current);
            }
        }

        protected User createUser(String uid, String name) throws OsmDataParsingException {
            if (uid == null) {
                if (name == null)
                    return null;
                return User.createLocalUser(name);
            }
            try {
                long id = Long.parseLong(uid);
                return User.createOsmUser(id, name);
            } catch(NumberFormatException e) {
                throwException(MessageFormat.format("Illegal value for attribute ''uid''. Got ''{0}''.", uid));
            }
            return null;
        }
    }

    /**
     * Parse the given input source and return the list of changesets
     *
     * @param source the source input stream
     * @param progressMonitor  the progress monitor
     *
     * @return the list of changesets
     * @throws IllegalDataException thrown if the an error was found while parsing the data from the source
     */
    public static List<Changeset> parse(InputStream source, ProgressMonitor progressMonitor) throws IllegalDataException {
        OsmChangesetParser parser = new OsmChangesetParser();
        try {
            progressMonitor.beginTask("");
            progressMonitor.indeterminateSubTask(tr("Parsing list of changesets..."));
            InputSource inputSource = new InputSource(new InvalidXmlCharacterFilter(new InputStreamReader(source, Utils.UTF_8)));
            SAXParserFactory.newInstance().newSAXParser().parse(inputSource, parser.new Parser());
            return parser.getChangesets();
        } catch(ParserConfigurationException e) {
            throw new IllegalDataException(e.getMessage(), e);
        } catch(SAXException e) {
            throw new IllegalDataException(e.getMessage(), e);
        } catch(Exception e) {
            throw new IllegalDataException(e);
        } finally {
            progressMonitor.finishTask();
        }
    }
}
