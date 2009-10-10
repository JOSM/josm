package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSource;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.DateUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parser for the Osm Api. Read from an input stream and construct a dataset out of it.
 *
 */
public class OsmReader {
    static private final Logger logger = Logger.getLogger(OsmReader.class.getName());

    /**
     * The dataset to add parsed objects to.
     */
    private DataSet ds = new DataSet();

    /**
     * Replies the parsed data set
     * 
     * @return the parsed data set
     */
    public DataSet getDataSet() {
        return ds;
    }

    /** the map from external ids to read OsmPrimitives. External ids are
     * longs too, but in contrast to internal ids negative values are used
     * to identify primitives unknown to the OSM server
     * 
     * The keys are strings composed as follows
     * <ul>
     *   <li>"n" + id  for nodes</li>
     *   <li>"w" + id  for nodes</li>
     *   <li>"r" + id  for nodes</li>
     * </ul>
     */
    private Map<String, OsmPrimitive> externalIdMap = new HashMap<String, OsmPrimitive>();


    /**
     * constructor (for private use only)
     *
     * @see #parseDataSet(InputStream, DataSet, ProgressMonitor)
     * @see #parseDataSetOsm(InputStream, DataSet, ProgressMonitor)
     */
    private OsmReader() {
        externalIdMap = new HashMap<String, OsmPrimitive>();
    }

    private static class OsmPrimitiveData {
        public long id = 0;
        public boolean modified = false;
        public boolean deleted = false;
        public Date timestamp = new Date();
        public User user = null;
        public boolean visible = true;
        public int version = 0;
        public LatLon latlon = new LatLon(0,0);
        private OsmPrimitive primitive;

        public void copyTo(OsmPrimitive osm) {
            //  id < 0 possible if read from a file
            if (id <= 0) {
                osm.clearOsmId();
            } else {
                osm.setOsmId(id, version);
            }
            osm.setDeleted(deleted);
            osm.setModified(modified | deleted);
            osm.setTimestamp(timestamp);
            osm.user = user;
            osm.setVisible(visible);
            osm.mappaintStyle = null;
        }

        public Node createNode() {
            Node node = new Node();
            node.setCoor(latlon);
            copyTo(node);
            primitive = node;
            return node;
        }

        public Way createWay() {
            Way way = new Way();
            copyTo(way);
            primitive = way;
            return way;
        }
        public Relation createRelation() {
            Relation relation= new Relation();
            copyTo(relation);
            primitive = relation;
            return relation;
        }

        public void rememberTag(String key, String value) {
            primitive.put(key, value);
        }
    }

    /**
     * Used as a temporary storage for relation members, before they
     * are resolved into pointers to real objects.
     */
    private static class RelationMemberData {
        public String type;
        public long id;
        public String role;
    }

    /**
     * Data structure for the remaining way objects
     */
    private Map<Long, Collection<Long>> ways = new HashMap<Long, Collection<Long>>();

    /**
     * Data structure for relation objects
     */
    private Map<Long, Collection<RelationMemberData>> relations = new HashMap<Long, Collection<RelationMemberData>>();

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
         * The current osm primitive to be read.
         */
        private OsmPrimitiveData current;
        private String generator;

        @Override public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (qName.equals("osm")) {
                if (atts == null) {
                    throwException(tr("Missing mandatory attribute ''{0}'' of XML element {1}.", "version", "osm"));
                }
                String v = atts.getValue("version");
                if (v == null) {
                    throwException(tr("Missing mandatory attribute ''{0}''.", "version"));
                }
                if (!(v.equals("0.5") || v.equals("0.6"))) {
                    throwException(tr("Unsupported version: {0}", v));
                }
                // save generator attribute for later use when creating DataSource objects
                generator = atts.getValue("generator");
                ds.version = v;

            } else if (qName.equals("bounds")) {
                // new style bounds.
                String minlon = atts.getValue("minlon");
                String minlat = atts.getValue("minlat");
                String maxlon = atts.getValue("maxlon");
                String maxlat = atts.getValue("maxlat");
                String origin = atts.getValue("origin");
                if (minlon != null && maxlon != null && minlat != null && maxlat != null) {
                    if (origin == null) {
                        origin = generator;
                    }
                    Bounds bounds = new Bounds(
                            new LatLon(Double.parseDouble(minlat), Double.parseDouble(minlon)),
                            new LatLon(Double.parseDouble(maxlat), Double.parseDouble(maxlon)));
                    DataSource src = new DataSource(bounds, origin);
                    ds.dataSources.add(src);
                } else {
                    throwException(tr(
                            "Missing manadatory attributes on element ''bounds''. Got minlon=''{0}'',minlat=''{1}'',maxlon=''{3}'',maxlat=''{4}'', origin=''{5}''.",
                            minlon, minlat, maxlon, maxlat, origin
                    ));
                }

                // ---- PARSING NODES AND WAYS ----

            } else if (qName.equals("node")) {
                current = new OsmPrimitiveData();
                current.latlon = new LatLon(getDouble(atts, "lat"), getDouble(atts, "lon"));
                readCommon(atts, current);
                Node n = current.createNode();
                externalIdMap.put("n"+current.id, n);
            } else if (qName.equals("way")) {
                current = new OsmPrimitiveData();
                readCommon(atts, current);
                Way w = current.createWay();
                externalIdMap.put("w"+current.id, w);
                ways.put(current.id, new ArrayList<Long>());
            } else if (qName.equals("nd")) {
                Collection<Long> list = ways.get(current.id);
                if (list == null) {
                    throwException(
                            tr("Found XML element <nd> not as direct child of element <way>.")
                    );
                }
                if (atts.getValue("ref") == null) {
                    throwException(
                            tr("Missing mandatory attribute ''{0}'' on <nd> of way {1}.", "ref", current.id)
                    );
                }
                long id = getLong(atts, "ref");
                if (id == 0) {
                    throwException(
                            tr("Illegal value of attribute ''ref'' of element <nd>. Got {0}.", id)
                    );
                }
                list.add(id);

                // ---- PARSING RELATIONS ----

            } else if (qName.equals("relation")) {
                current = new OsmPrimitiveData();
                readCommon(atts, current);
                Relation r = current.createRelation();
                externalIdMap.put("r"+current.id, r );
                relations.put(current.id, new LinkedList<RelationMemberData>());
            } else if (qName.equals("member")) {
                Collection<RelationMemberData> list = relations.get(current.id);
                if (list == null) {
                    throwException(
                            tr("Found XML element <member> not as direct child of element <relation>.")
                    );
                }
                RelationMemberData emd = new RelationMemberData();
                String value = atts.getValue("ref");
                if (value == null) {
                    throwException(tr("Missing attribute ''ref'' on member in relation {0}.",current.id));
                }
                try {
                    emd.id = Long.parseLong(value);
                } catch(NumberFormatException e) {
                    throwException(tr("Illegal value for attribute ''ref'' on member in relation {0}. Got {1}", Long.toString(current.id),value));
                }
                value = atts.getValue("type");
                if (value == null) {
                    throwException(tr("Missing attribute ''type'' on member {0} in relation {1}.", Long.toString(emd.id), Long.toString(current.id)));
                }
                if (! (value.equals("way") || value.equals("node") || value.equals("relation"))) {
                    throwException(tr("Illegal value for attribute ''type'' on member {0} in relation {1}. Got {2}.", Long.toString(emd.id), Long.toString(current.id), value));
                }
                emd.type= value;
                value = atts.getValue("role");
                emd.role = value;

                if (emd.id == 0) {
                    throwException(tr("Incomplete <member> specification with ref=0"));
                }

                list.add(emd);

                // ---- PARSING TAGS (applicable to all objects) ----

            } else if (qName.equals("tag")) {
                String key = atts.getValue("k");
                String value = atts.getValue("v");
                current.rememberTag(key, value);
            } else {
                System.out.println(tr("Undefined element ''{0}'' found in input stream. Skipping.", qName));
            }
        }

        private double getDouble(Attributes atts, String value) {
            return Double.parseDouble(atts.getValue(value));
        }

        private User createUser(String uid, String name) throws SAXException {
            if (uid == null) {
                if (name == null)
                    return null;
                return User.createLocalUser(name);
            }
            try {
                long id = Long.parseLong(uid);
                return User.createOsmUser(id, name);
            } catch(NumberFormatException e) {
                throwException(tr("Illegal value for attribute ''uid''. Got ''{0}''.", uid));
            }
            return null;
        }
        /**
         * Read out the common attributes from atts and put them into this.current.
         */
        void readCommon(Attributes atts, OsmPrimitiveData current) throws SAXException {
            current.id = getLong(atts, "id");
            if (current.id == 0) {
                throwException(tr("Illegal object with ID=0."));
            }

            String time = atts.getValue("timestamp");
            if (time != null && time.length() != 0) {
                current.timestamp =  DateUtils.fromString(time);
            }

            // user attribute added in 0.4 API
            String user = atts.getValue("user");
            // uid attribute added in 0.6 API
            String uid = atts.getValue("uid");
            current.user = createUser(uid, user);

            // visible attribute added in 0.4 API
            String visible = atts.getValue("visible");
            if (visible != null) {
                current.visible = Boolean.parseBoolean(visible);
            }

            String version = atts.getValue("version");
            current.version = 0;
            if (version != null) {
                try {
                    current.version = Integer.parseInt(version);
                } catch(NumberFormatException e) {
                    throwException(tr("Illegal value for attribute ''version'' on OSM primitive with ID {0}. Got {1}.", Long.toString(current.id), version));
                }
                if (ds.version.equals("0.6")){
                    if (current.version <= 0 && current.id > 0) {
                        throwException(tr("Illegal value for attribute ''version'' on OSM primitive with ID {0}. Got {1}.", Long.toString(current.id), version));
                    } else if (current.version < 0 && current.id  <=0) {
                        System.out.println(tr("WARNING: Normalizing value of attribute ''version'' of element {0} to {2}, API version is ''{3}''. Got {1}.", current.id, current.version, 0, "0.6"));
                        current.version = 0;
                    }
                } else if (ds.version.equals("0.5")) {
                    if (current.version <= 0 && current.id > 0) {
                        System.out.println(tr("WARNING: Normalizing value of attribute ''version'' of element {0} to {2}, API version is ''{3}''. Got {1}.", current.id, current.version, 1, "0.5"));
                        current.version = 1;
                    } else if (current.version < 0 && current.id  <=0) {
                        System.out.println(tr("WARNING: Normalizing value of attribute ''version'' of element {0} to {2}, API version is ''{3}''. Got {1}.", current.id, current.version, 0, "0.5"));
                        current.version = 0;
                    }
                } else {
                    // should not happen. API version has been checked before
                    throwException(tr("Unknown or unsupported API version. Got {0}.", ds.version));
                }
            } else {
                // version expected for OSM primitives with an id assigned by the server (id > 0), since API 0.6
                //
                if (current.id > 0 && ds.version != null && ds.version.equals("0.6")) {
                    throwException(tr("Missing attribute ''version'' on OSM primitive with ID {0}.", Long.toString(current.id)));
                }
            }

            String action = atts.getValue("action");
            if (action == null)
                return;
            if (action.equals("delete")) {
                current.deleted = true;
            } else if (action.startsWith("modify")) {
                current.modified = true;
            }
        }

        private long getLong(Attributes atts, String name) throws SAXException {
            String value = atts.getValue(name);
            if (value == null) {
                throwException(tr("Missing required attribute ''{0}''.",name));
            }
            try {
                return Long.parseLong(value);
            } catch(NumberFormatException e) {
                throwException(tr("Illegal long value for attribute ''{0}''. Got ''{1}''.",name, value));
            }
            return 0; // should not happen
        }
    }


    /**
     * Processes the ways after parsing. Rebuilds the list of nodes of each way and
     * adds the way to the dataset
     * 
     * @throws IllegalDataException thrown if a data integrity problem is detected
     */
    protected void processWaysAfterParsing() throws IllegalDataException{
        for (Long externalWayId: ways.keySet()) {
            Way w = (Way)externalIdMap.get("w" + externalWayId);
            boolean incomplete = false;
            List<Node> wayNodes = new ArrayList<Node>();
            for (long id : ways.get(externalWayId)) {
                Node n = (Node)externalIdMap.get("n" +id);
                if (n == null) {
                    if (id <= 0)
                        throw new IllegalDataException (
                                tr(
                                        "Way with external ID ''{0}'' includes missing node with external ID ''{1}''.",
                                        externalWayId,
                                        id
                                )
                        );
                    n = new Node(id);
                    n.incomplete = true;
                    incomplete = true;
                }
                wayNodes.add(n);
            }
            w.setNodes(wayNodes);
            if (incomplete) {
                logger.warning(tr("Marked way {0} with {1} nodes incomplete because at least one node was missing in the " +
                        "loaded data and is therefore incomplete too.", externalWayId, w.getNodesCount()));
                w.incomplete = true;
                ds.addPrimitive(w);
            } else {
                w.incomplete = false;
                ds.addPrimitive(w);
            }
        }
    }

    /**
     * Processes the parsed nodes after parsing. Just adds them to
     * the dataset
     * 
     */
    protected void processNodesAfterParsing() {
        for (OsmPrimitive primitive: externalIdMap.values()) {
            if (primitive instanceof Node) {
                this.ds.addPrimitive(primitive);
            }
        }
    }

    /**
     * Completes the parsed relations with its members.
     * 
     * @throws IllegalDataException thrown if a data integrity problem is detected, i.e. if a
     * relation member refers to a local primitive which wasn't available in the data
     * 
     */
    private void processRelationsAfterParsing() throws IllegalDataException {
        for (Long externalRelationId : relations.keySet()) {
            Relation relation = (Relation) externalIdMap.get("r" +externalRelationId);
            List<RelationMember> relationMembers = new ArrayList<RelationMember>();
            for (RelationMemberData rm : relations.get(externalRelationId)) {
                OsmPrimitive primitive = null;

                // lookup the member from the map of already created primitives
                //
                if (rm.type.equals("node")) {
                    primitive = externalIdMap.get("n" + rm.id);
                } else if (rm.type.equals("way")) {
                    primitive = externalIdMap.get("w" + rm.id);
                } else if (rm.type.equals("relation")) {
                    primitive = externalIdMap.get("r" + rm.id);
                } else
                    throw new IllegalDataException(
                            tr("Unknown relation member type ''{0}'' in relation with external id ''{1}''.", rm.type,externalRelationId)
                    );

                if (primitive == null) {
                    if (rm.id <= 0)
                        // relation member refers to a primitive with a negative id which was not
                        // found in the data. This is always a data integrity problem and we abort
                        // with an exception
                        //
                        throw new IllegalDataException(
                                tr(
                                        "Relation with external id ''{0}'' refers to a missing primitive with external id ''{1}''.",
                                        externalRelationId,
                                        rm.id
                                )
                        );

                    // member refers to OSM primitive which was not present in the parsed data
                    // -> create a new incomplete primitive and add it to the dataset
                    //
                    if (rm.type.equals("node")) {
                        primitive = new Node(rm.id);
                    } else if (rm.type.equals("way")) {
                        primitive = new Way(rm.id);
                    } else if (rm.type.equals("relation")) {
                        primitive = new Relation(rm.id);
                    } else {
                        // can't happen, we've been testing for valid member types
                        // at the beginning of this method
                        //
                    }
                    ds.addPrimitive(primitive);
                }
                relationMembers.add(new RelationMember(rm.role, primitive));
            }
            relation.setMembers(relationMembers);
            ds.addPrimitive(relation);
        }
    }

    /**
     * Parse the given input source and return the dataset.
     * 
     * @param source the source input stream
     * @param progressMonitor  the progress monitor
     * 
     * @return the dataset with the parsed data
     * @throws IllegalDataException thrown if the an error was found while parsing the data from the source
     */
    public static DataSet parseDataSet(InputStream source, ProgressMonitor progressMonitor) throws IllegalDataException {
        OsmReader reader = new OsmReader();
        try {
            progressMonitor.beginTask(tr("Prepare OSM data...", 2));
            progressMonitor.subTask(tr("Parsing OSM data..."));
            InputSource inputSource = new InputSource(new InputStreamReader(source, "UTF-8"));
            SAXParserFactory.newInstance().newSAXParser().parse(inputSource, reader.new Parser());
            progressMonitor.worked(1);

            progressMonitor.subTask(tr("Preparing data set..."));
            reader.processNodesAfterParsing();
            reader.processWaysAfterParsing();
            reader.processRelationsAfterParsing();
            progressMonitor.worked(1);
            return reader.getDataSet();
        } catch(IllegalDataException e) {
            throw e;
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
