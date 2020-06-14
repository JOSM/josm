// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.ChangesetDiscussionComment;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.XmlParsingException;
import org.openstreetmap.josm.tools.XmlUtils;
import org.openstreetmap.josm.tools.date.DateUtils;
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
 *     &lt;changeset id="143" user="guggis" uid="1" created_at="2009-09-08T20:35:39Z" closed_at="2009-09-08T21:36:12Z" open="false"
 *                min_lon="7.380925" min_lat="46.9215164" max_lon="7.3984718" max_lat="46.9226502"&gt;
 *         &lt;tag k="asdfasdf" v="asdfasdf"/&gt;
 *         &lt;tag k="created_by" v="JOSM/1.5 (UNKNOWN de)"/&gt;
 *         &lt;tag k="comment" v="1234"/&gt;
 *     &lt;/changeset&gt;
 * &lt;/osm&gt;
 * </pre>
 *
 */
public final class OsmChangesetParser {
    private final List<Changeset> changesets;

    private OsmChangesetParser() {
        changesets = new LinkedList<>();
    }

    /**
     * Returns the parsed changesets.
     * @return the parsed changesets
     */
    public List<Changeset> getChangesets() {
        return changesets;
    }

    private class Parser extends DefaultHandler {
        private Locator locator;

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        protected void throwException(String msg) throws XmlParsingException {
            throw new XmlParsingException(msg).rememberLocation(locator);
        }

        /** The current changeset */
        private Changeset current;

        /** The current comment */
        private ChangesetDiscussionComment comment;

        /** The current comment text */
        private StringBuilder text;

        protected void parseChangesetAttributes(Attributes atts) throws XmlParsingException {
            // -- id
            String value = atts.getValue("id");
            if (value == null) {
                throwException(tr("Missing mandatory attribute ''{0}''.", "id"));
            }
            current.setId(parseNumericAttribute(value, 1));

            // -- user / uid
            current.setUser(createUser(atts));

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
            } else if ("true".equals(value)) {
                current.setOpen(true);
            } else if ("false".equals(value)) {
                current.setOpen(false);
            } else {
                throwException(tr("Illegal boolean value for attribute ''{0}''. Got ''{1}''.", "open", value));
            }

            // -- min_lon and min_lat
            String minLonStr = atts.getValue("min_lon");
            String minLatStr = atts.getValue("min_lat");
            String maxLonStr = atts.getValue("max_lon");
            String maxLatStr = atts.getValue("max_lat");
            if (minLonStr != null && minLatStr != null && maxLonStr != null && maxLatStr != null) {
                double minLon = 0;
                try {
                    minLon = Double.parseDouble(minLonStr);
                } catch (NumberFormatException e) {
                    throwException(tr("Illegal value for attribute ''{0}''. Got ''{1}''.", "min_lon", minLonStr));
                }
                double minLat = 0;
                try {
                    minLat = Double.parseDouble(minLatStr);
                } catch (NumberFormatException e) {
                    throwException(tr("Illegal value for attribute ''{0}''. Got ''{1}''.", "min_lat", minLatStr));
                }
                current.setMin(new LatLon(minLat, minLon));

                // -- max_lon and max_lat

                double maxLon = 0;
                try {
                    maxLon = Double.parseDouble(maxLonStr);
                } catch (NumberFormatException e) {
                    throwException(tr("Illegal value for attribute ''{0}''. Got ''{1}''.", "max_lon", maxLonStr));
                }
                double maxLat = 0;
                try {
                    maxLat = Double.parseDouble(maxLatStr);
                } catch (NumberFormatException e) {
                    throwException(tr("Illegal value for attribute ''{0}''. Got ''{1}''.", "max_lat", maxLatStr));
                }
                current.setMax(new LatLon(maxLat, maxLon));
            }

            // -- comments_count
            String commentsCount = atts.getValue("comments_count");
            if (commentsCount != null) {
                current.setCommentsCount(parseNumericAttribute(commentsCount, 0));
            }

            // -- changes_count
            String changesCount = atts.getValue("changes_count");
            if (changesCount != null) {
                current.setChangesCount(parseNumericAttribute(changesCount, 0));
            }
        }

        private void parseCommentAttributes(Attributes atts) throws XmlParsingException {
            // -- date
            String value = atts.getValue("date");
            Date date = null;
            if (value != null) {
                date = DateUtils.fromString(value);
            }

            comment = new ChangesetDiscussionComment(date, createUser(atts));
        }

        private int parseNumericAttribute(String value, int minAllowed) throws XmlParsingException {
            int att = 0;
            try {
                att = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throwException(tr("Illegal value for attribute ''{0}''. Got ''{1}''.", "id", value));
            }
            if (att < minAllowed) {
                throwException(tr("Illegal numeric value for attribute ''{0}''. Got ''{1}''.", "id", att));
            }
            return att;
        }

        @Override
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            switch (qName) {
            case "osm":
                if (atts == null) {
                    throwException(tr("Missing mandatory attribute ''{0}'' of XML element {1}.", "version", "osm"));
                    return;
                }
                String v = atts.getValue("version");
                if (v == null) {
                    throwException(tr("Missing mandatory attribute ''{0}''.", "version"));
                }
                if (!"0.6".equals(v)) {
                    throwException(tr("Unsupported version: {0}", v));
                }
                break;
            case "changeset":
                current = new Changeset();
                parseChangesetAttributes(atts);
                break;
            case "tag":
                String key = atts.getValue("k");
                String value = atts.getValue("v");
                current.put(key, value);
                break;
            case "discussion":
                break;
            case "comment":
                parseCommentAttributes(atts);
                break;
            case "text":
                text = new StringBuilder();
                break;
            default:
                throwException(tr("Undefined element ''{0}'' found in input stream. Aborting.", qName));
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (text != null) {
                text.append(ch, start, length);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if ("changeset".equals(qName)) {
                changesets.add(current);
                current = null;
            } else if ("comment".equals(qName)) {
                current.addDiscussionComment(comment);
                comment = null;
            } else if ("text".equals(qName)) {
                comment.setText(text.toString());
                text = null;
            }
        }

        protected User createUser(Attributes atts) throws XmlParsingException {
            String name = atts.getValue("user");
            String uid = atts.getValue("uid");
            if (uid == null) {
                if (name == null)
                    return null;
                return User.createLocalUser(name);
            }
            try {
                long id = Long.parseLong(uid);
                return User.createOsmUser(id, name);
            } catch (NumberFormatException e) {
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
     * @throws IllegalDataException if the an error was found while parsing the data from the source
     */
    @SuppressWarnings("resource")
    public static List<Changeset> parse(InputStream source, ProgressMonitor progressMonitor) throws IllegalDataException {
        OsmChangesetParser parser = new OsmChangesetParser();
        try {
            progressMonitor.beginTask("");
            progressMonitor.indeterminateSubTask(tr("Parsing list of changesets..."));
            InputSource inputSource = new InputSource(new InvalidXmlCharacterFilter(new InputStreamReader(source, StandardCharsets.UTF_8)));
            XmlUtils.parseSafeSAX(inputSource, parser.new Parser());
            return parser.getChangesets();
        } catch (ParserConfigurationException | SAXException e) {
            throw new IllegalDataException(e.getMessage(), e);
        } catch (IOException e) {
            throw new IllegalDataException(e);
        } finally {
            progressMonitor.finishTask();
        }
    }
}
