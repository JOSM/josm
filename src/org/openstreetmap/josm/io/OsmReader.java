// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
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
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parser for the Osm Api. Read from an input stream and construct a dataset out of it.
 *
 * Reading process takes place in three phases. During the first phase (including xml parse),
 * all nodes are read and stored. Other information than nodes are stored in a raw list
 *
 * The second phase read all ways out of the remaining objects in the raw list.
 *
 * @author Imi
 */
public class OsmReader {
    static private final Logger logger = Logger.getLogger(OsmReader.class.getName());

    /**
     * The dataset to add parsed objects to.
     */
    private DataSet ds = new DataSet();
    public DataSet getDs() { return ds; }

    /**
     * All read nodes after phase 1.
     */
    private Map<Long, Node> nodes = new HashMap<Long, Node>();


    /**
     * constructor (for private use only)
     *
     * @see #parseDataSet(InputStream, DataSet, ProgressMonitor)
     * @see #parseDataSetOsm(InputStream, DataSet, ProgressMonitor)
     */
    private OsmReader() {
    }

    private static class OsmPrimitiveData {
        public long id = 0;
        public Map<String,String> keys = new HashMap<String, String>();
        public boolean modified = false;
        public boolean selected = false;
        public boolean deleted = false;
        public Date timestamp = new Date();
        public User user = null;
        public boolean visible = true;
        public int version = -1;
        public LatLon latlon = new LatLon(0,0);

        public void copyTo(OsmPrimitive osm) {
            osm.id = id;
            osm.keys = keys;
            osm.modified = modified;
            osm.selected = selected;
            osm.deleted = deleted;
            osm.setTimestamp(timestamp);
            osm.user = user;
            osm.visible = visible;
            osm.version = version;
            osm.mappaintStyle = null;
        }

        public Node createNode() {
            Node node = new Node(latlon);
            copyTo(node);
            return node;
        }

        public Way createWay() {
            Way way = new Way(id);
            copyTo(way);
            return way;
        }

        public Relation createRelation() {
            Relation rel = new Relation(id);
            copyTo(rel);
            return rel;
        }
    }

    /**
     * Used as a temporary storage for relation members, before they
     * are resolved into pointers to real objects.
     */
    private static class RelationMemberData {
        public String type;
        public long id;
        public RelationMember relationMember;
    }

    /**
     * Data structure for the remaining way objects
     */
    private Map<OsmPrimitiveData, Collection<Long>> ways = new HashMap<OsmPrimitiveData, Collection<Long>>();

    /**
     * Data structure for relation objects
     */
    private Map<OsmPrimitiveData, Collection<RelationMemberData>> relations = new HashMap<OsmPrimitiveData, Collection<RelationMemberData>>();

    private class Parser extends DefaultHandler {
        /**
         * The current osm primitive to be read.
         */
        private OsmPrimitiveData current;
        private String generator;

        @Override public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            try {
                if (qName.equals("osm")) {
                    if (atts == null)
                        throw new SAXException(tr("Unknown version"));
                    String v = atts.getValue("version");
                    if (v == null)
                        throw new SAXException(tr("Version number missing from OSM data"));
                    if (!(v.equals("0.5") || v.equals("0.6")))
                        throw new SAXException(tr("Unknown version: {0}", v));
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
                    }

                    // ---- PARSING NODES AND WAYS ----

                } else if (qName.equals("node")) {
                    current = new OsmPrimitiveData();
                    current.latlon = new LatLon(getDouble(atts, "lat"), getDouble(atts, "lon"));
                    readCommon(atts, current);
                } else if (qName.equals("way")) {
                    current = new OsmPrimitiveData();
                    readCommon(atts, current);
                    ways.put(current, new ArrayList<Long>());
                } else if (qName.equals("nd")) {
                    Collection<Long> list = ways.get(current);
                    if (list == null)
                        throw new SAXException(tr("Found <nd> element in non-way."));
                    long id = getLong(atts, "ref");
                    if (id == 0)
                        throw new SAXException(tr("<nd> has zero ref"));
                    list.add(id);

                    // ---- PARSING RELATIONS ----

                } else if (qName.equals("relation")) {
                    current = new OsmPrimitiveData();
                    readCommon(atts, current);
                    relations.put(current, new LinkedList<RelationMemberData>());
                } else if (qName.equals("member")) {
                    Collection<RelationMemberData> list = relations.get(current);
                    if (list == null)
                        throw new SAXException(tr("Found <member> element in non-relation."));
                    RelationMemberData emd = new RelationMemberData();
                    emd.relationMember = new RelationMember();
                    String value = atts.getValue("ref");
                    if (value == null)
                        throw new SAXException(tr("Missing attribute \"ref\" on member in relation {0}",current.id));
                    try {
                        emd.id = Long.parseLong(value);
                    } catch(NumberFormatException e) {
                        throw new SAXException(tr("Illegal value for attribute \"ref\" on member in relation {0}, got {1}", Long.toString(current.id),value));
                    }
                    value = atts.getValue("type");
                    if (value == null)
                        throw new SAXException(tr("Missing attribute \"type\" on member {0} in relation {1}", Long.toString(emd.id), Long.toString(current.id)));
                    if (! (value.equals("way") || value.equals("node") || value.equals("relation")))
                        throw new SAXException(tr("Unexpected \"type\" on member {0} in relation {1}, got {2}.", Long.toString(emd.id), Long.toString(current.id), value));
                    emd.type= value;
                    value = atts.getValue("role");
                    emd.relationMember.role = value;

                    if (emd.id == 0)
                        throw new SAXException(tr("Incomplete <member> specification with ref=0"));

                    list.add(emd);

                    // ---- PARSING TAGS (applicable to all objects) ----

                } else if (qName.equals("tag")) {
                    String key = atts.getValue("k");
                    String value = atts.getValue("v");
                    current.keys.put(key,value);
                }
            } catch (NumberFormatException x) {
                x.printStackTrace(); // SAXException does not chain correctly
                throw new SAXException(x.getMessage(), x);
            } catch (NullPointerException x) {
                x.printStackTrace(); // SAXException does not chain correctly
                throw new SAXException(tr("NullPointerException, possibly some missing tags."), x);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("node")) {
                nodes.put(current.id, current.createNode());
            }
        }

        private double getDouble(Attributes atts, String value) {
            return Double.parseDouble(atts.getValue(value));
        }
    }

    /**
     * Read out the common attributes from atts and put them into this.current.
     */
    void readCommon(Attributes atts, OsmPrimitiveData current) throws SAXException {
        current.id = getLong(atts, "id");
        if (current.id == 0)
            throw new SAXException(tr("Illegal object with id=0"));

        String time = atts.getValue("timestamp");
        if (time != null && time.length() != 0) {
            current.timestamp =  DateUtils.fromString(time);
        }

        // user attribute added in 0.4 API
        String user = atts.getValue("user");
        if (user != null) {
            // do not store literally; get object reference for string
            current.user = User.get(user);
        }

        // uid attribute added in 0.6 API
        String uid = atts.getValue("uid");
        if (uid != null) {
            if (current.user != null) {
                current.user.uid = uid;
            }
        }

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
                throw new SAXException(tr("Illegal value for attribute \"version\" on OSM primitive with id {0}, got {1}", Long.toString(current.id), version));
            }
        } else {
            // version expected for OSM primitives with an id assigned by the server (id > 0), since API 0.6
            //
            if (current.id > 0 && ds.version != null && ds.version.equals("0.6"))
                throw new SAXException(tr("Missing attribute \"version\" on OSM primitive with id {0}", Long.toString(current.id)));
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
    private long getLong(Attributes atts, String value) throws SAXException {
        String s = atts.getValue(value);
        if (s == null)
            throw new SAXException(tr("Missing required attribute \"{0}\".",value));
        return Long.parseLong(s);
    }

    private Node findNode(long id) {
        Node n = nodes.get(id);
        if (n != null)
            return n;
        return null;
    }

    protected void createWays() {
        for (Entry<OsmPrimitiveData, Collection<Long>> e : ways.entrySet()) {
            Way w = new Way(e.getKey().id);
            boolean failed = false;
            for (long id : e.getValue()) {
                Node n = findNode(id);
                if (n == null) {
                    failed = true;
                    break;
                }
                w.nodes.add(n);
            }
            if (failed) {
                logger.warning(tr("marked way {0} incomplete because referred nodes are missing in the loaded data", e.getKey().id));
                e.getKey().copyTo(w);
                w.incomplete = true;
                w.nodes.clear();
                ds.addPrimitive(w);
            } else {
                e.getKey().copyTo(w);
                w.incomplete = false;
                ds.addPrimitive(w);
            }
        }
    }

    /**
     * Return the Way object with the given id, or null if it doesn't
     * exist yet. This method only looks at ways stored in the already parsed
     * ways.
     *
     * @param id
     * @return way object or null
     */
    private Way findWay(long id) {
        for (Way way : ds.ways)
            if (way.id == id)
                return way;
        return null;
    }

    /**
     * Return the Relation object with the given id, or null if it doesn't
     * exist yet. This method only looks at relations in the already parsed
     * relations.
     *
     * @param id
     * @return relation object or null
     */
    private Relation findRelation(long id) {
        for (Relation e : ds.relations)
            if (e.id == id)
                return e;
        return null;
    }

    /**
     * Create relations. This is slightly different than n/s/w because
     * unlike other objects, relations may reference other relations; it
     * is not guaranteed that a referenced relation will have been created
     * before it is referenced. So we have to create all relations first,
     * and populate them later.
     */
    private void createRelations() {

        // pass 1 - create all relations
        for (Entry<OsmPrimitiveData, Collection<RelationMemberData>> e : relations.entrySet()) {
            Relation en = new Relation();
            e.getKey().copyTo(en);
            ds.addPrimitive(en);
        }

        // Cache the ways here for much better search performance
        HashMap<Long, Way> hm = new HashMap<Long, Way>(10000);
        for (Way wy : ds.ways) {
            hm.put(wy.id, wy);
        }

        // pass 2 - sort out members
        for (Entry<OsmPrimitiveData, Collection<RelationMemberData>> e : relations.entrySet()) {
            Relation en = findRelation(e.getKey().id);
            if (en == null) throw new Error("Failed to create relation " + e.getKey().id);

            for (RelationMemberData emd : e.getValue()) {
                RelationMember em = emd.relationMember;
                if (emd.type.equals("node")) {
                    em.member = findNode(emd.id);
                    if (em.member == null) {
                        em.member = new Node(emd.id);
                        ds.addPrimitive(em.member);
                    }
                } else if (emd.type.equals("way")) {
                    em.member = hm.get(emd.id);
                    if (em.member == null) {
                        em.member = findWay(emd.id);
                    }
                    if (em.member == null) {
                        em.member = new Way(emd.id);
                        ds.addPrimitive(em.member);
                    }
                } else if (emd.type.equals("relation")) {
                    em.member = findRelation(emd.id);
                    if (em.member == null) {
                        em.member = new Relation(emd.id);
                        ds.addPrimitive(em.member);
                    }
                } else {
                    // this is an error.
                }
                en.members.add(em);
            }
        }
        hm = null;
    }

    /**
     * Parse the given input source and return the dataset.
     * @param ref The dataset that is search in for references first. If
     *      the Reference is not found here, Main.ds is searched and a copy of the
     *  element found there is returned.
     */
    public static DataSet parseDataSet(InputStream source, ProgressMonitor progressMonitor) throws SAXException, IOException {
        return parseDataSetOsm(source, progressMonitor).ds;
    }

    public static OsmReader parseDataSetOsm(InputStream source, ProgressMonitor progressMonitor) throws SAXException, IOException {
        OsmReader reader = new OsmReader();

        // phase 1: Parse nodes and read in raw ways
        InputSource inputSource = new InputSource(new InputStreamReader(source, "UTF-8"));
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(inputSource, reader.new Parser());
        } catch (ParserConfigurationException e1) {
            e1.printStackTrace(); // broken SAXException chaining
            throw new SAXException(e1);
        }

        progressMonitor.beginTask(tr("Prepare OSM data...", 2));
        try {
            for (Node n : reader.nodes.values()) {
                reader.ds.addPrimitive(n);
            }

            progressMonitor.worked(1);

            try {
                reader.createWays();
                reader.createRelations();
            } catch (NumberFormatException e) {
                e.printStackTrace();
                throw new SAXException(tr("Ill-formed node id"));
            }

            // clear all negative ids (new to this file)
            for (OsmPrimitive o : reader.ds.allPrimitives())
                if (o.id < 0) {
                    o.id = 0;
                }

            return reader;
        } finally {
            progressMonitor.finishTask();
        }
    }
}
