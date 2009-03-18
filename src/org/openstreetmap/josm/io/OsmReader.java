// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.openstreetmap.josm.Main;
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
import org.openstreetmap.josm.data.osm.visitor.AddVisitor;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.gui.PleaseWaitDialog;
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

//     static long tagsN = 0;
//     static long nodesN = 0;
//     static long waysN = 0;
//     static long relationsN = 0;
//     static long membersN = 0;

     static InputStream currSource;

     /**
      * This is used as (readonly) source for finding missing references when not transferred in the
      * file.
      */
     private DataSet references;

     /**
      * The dataset to add parsed objects to.
      */
     private DataSet ds = new DataSet();
     public DataSet getDs() { return ds; }

     /**
      * Record warnings.  If there were any data inconsistencies, append
      * a newline-terminated string.
      */
     private String parseNotes = new String();
     private int parseNotesCount = 0;
     public String getParseNotes() {
         return parseNotes;
     }

     /**
      * The visitor to use to add the data to the set.
      */
     private AddVisitor adder = new AddVisitor(ds);

     /**
      * All read nodes after phase 1.
      */
     private Map<Long, Node> nodes = new HashMap<Long, Node>();

     // TODO: What the hack? Is this really from me? Please, clean this up!
     private static class OsmPrimitiveData extends OsmPrimitive {
          @Override public void visit(Visitor visitor) {}
          public int compareTo(OsmPrimitive o) {return 0;}

          public void copyTo(OsmPrimitive osm) {
               osm.id = id;
               osm.keys = keys;
               osm.modified = modified;
               osm.selected = selected;
               osm.deleted = deleted;
               osm.setTimestamp(getTimestamp());
               osm.user = user;
               osm.visible = visible;
               osm.version = version;
               osm.mappaintStyle = null;
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

     /**
      * List of protocol versions that will be accepted on reading
      */
     private HashSet<String> allowedVersions = new HashSet<String>();

     private class Parser extends DefaultHandler {
          /**
           * The current osm primitive to be read.
           */
          private OsmPrimitive current;
          private String generator;
          private Map<String, String> keys = new HashMap<String, String>();
//          int n = 0;

          @Override public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
               try {
//                    if(n%100000 == 0) {
//                        try {
//                            FileInputStream fis = (FileInputStream)currSource;
//                            FileChannel channel = fis.getChannel();
//                            double perc = (((double)channel.position()) / ((double)channel.size()) * 100.0);
//                            System.out.format(" " + (int)perc + "%%");
//                        }
//                        catch(java.lang.ClassCastException cce) {
//                        }
//                        catch(IOException e) {
//                            System.out.format("Error reading file position " + e);
//                        }
//                    }
//                    n++;

                    if (qName.equals("osm")) {
                         if (atts == null)
                              throw new SAXException(tr("Unknown version"));
                         if (!allowedVersions.contains(atts.getValue("version")))
                              throw new SAXException(tr("Unknown version")+": "+atts.getValue("version"));
                         // save generator attribute for later use when creating DataSource objects
                         generator = atts.getValue("generator");


                    } else if (qName.equals("bound")) {
                         // old style bounds.
                         // TODO: remove this around 1st October 2008.
                         // - this is a bit of a hack; since we still write out old style bound objects,
                         // we don't want to load them both. so when writing, we add a "note" tag the our
                         // old-style bound, and when reading, ignore those with a "note".
                         String note = atts.getValue("note");
                         if (note == null) {
                             System.out.println("Notice: old style <bound> element detected; support for these will be dropped in a future release.");
                             String bbox = atts.getValue("box");
                             String origin = atts.getValue("origin");
                             if (origin == null) origin = "";
                             if (bbox != null) {
                                  String[] b = bbox.split(",");
                                  Bounds bounds = new Bounds();
                                  if (b.length == 4)
                                       bounds = new Bounds(
                                                 new LatLon(Double.parseDouble(b[0]),Double.parseDouble(b[1])),
                                                 new LatLon(Double.parseDouble(b[2]),Double.parseDouble(b[3])));
                                  DataSource src = new DataSource(bounds, origin);
                                  ds.dataSources.add(src);
                             }
                         }
                    } else if (qName.equals("bounds")) {
                         // new style bounds.
                         String minlon = atts.getValue("minlon");
                         String minlat = atts.getValue("minlat");
                         String maxlon = atts.getValue("maxlon");
                         String maxlat = atts.getValue("maxlat");
                         String origin = atts.getValue("origin");
                         if (minlon != null && maxlon != null && minlat != null && maxlat != null) {
                              if (origin == null) origin = generator;
                              Bounds bounds = new Bounds(
                                  new LatLon(Double.parseDouble(minlat), Double.parseDouble(minlon)),
                                  new LatLon(Double.parseDouble(maxlat), Double.parseDouble(maxlon)));
                              DataSource src = new DataSource(bounds, origin);
                              ds.dataSources.add(src);
                         }

                    // ---- PARSING NODES AND WAYS ----

                    } else if (qName.equals("node")) {
//                         nodesN++;
                         current = new Node(new LatLon(getDouble(atts, "lat"), getDouble(atts, "lon")));
                         readCommon(atts, current);
                         nodes.put(current.id, (Node)current);
                    } else if (qName.equals("way")) {
//                         waysN++;
                         current = new OsmPrimitiveData();
                         readCommon(atts, current);
                         ways.put((OsmPrimitiveData)current, new ArrayList<Long>());
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
//                         relationsN++;
                         current = new OsmPrimitiveData();
                         readCommon(atts, current);
                         relations.put((OsmPrimitiveData)current, new LinkedList<RelationMemberData>());
                    } else if (qName.equals("member")) {
//                         membersN++;
                         Collection<RelationMemberData> list = relations.get(current);
                         if (list == null)
                              throw new SAXException(tr("Found <member> element in non-relation."));
                         RelationMemberData emd = new RelationMemberData();
                         emd.relationMember = new RelationMember();
                         emd.id = getLong(atts, "ref");
                         emd.type=atts.getValue("type");
                         emd.relationMember.role = atts.getValue("role");

                         if (emd.id == 0)
                              throw new SAXException(tr("Incomplete <member> specification with ref=0"));

                         list.add(emd);

                    // ---- PARSING TAGS (applicable to all objects) ----

                    } else if (qName.equals("tag")) {
//                         tagsN++;
                        String key = atts.getValue("k");
                        String internedKey = keys.get(key);
                        if (internedKey == null) {
                            internedKey = key;
                            keys.put(key, key);
                        }
                         current.put(internedKey, atts.getValue("v"));
                    }
               } catch (NumberFormatException x) {
                    x.printStackTrace(); // SAXException does not chain correctly
                    throw new SAXException(x.getMessage(), x);
               } catch (NullPointerException x) {
                    x.printStackTrace(); // SAXException does not chain correctly
                    throw new SAXException(tr("NullPointerException, possibly some missing tags."), x);
               }
          }

          private double getDouble(Attributes atts, String value) {
               return Double.parseDouble(atts.getValue(value));
          }
     }

     /**
      * Constructor initializes list of allowed protocol versions.
      */
     public OsmReader() {
          // first add the main server version
          allowedVersions.add(Main.pref.get("osm-server.version", "0.5"));
          // now also add all compatible versions
          String[] additionalVersions =
               Main.pref.get("osm-server.additional-versions", "").split("/,/");
          if (additionalVersions.length == 1 && additionalVersions[0].length() == 0)
               additionalVersions = new String[] {};
          allowedVersions.addAll(Arrays.asList(additionalVersions));
     }

     /**
      * Read out the common attributes from atts and put them into this.current.
      */
     void readCommon(Attributes atts, OsmPrimitive current) throws SAXException {
          current.id = getLong(atts, "id");
          if (current.id == 0)
               throw new SAXException(tr("Illegal object with id=0"));

          String time = atts.getValue("timestamp");
          if (time != null && time.length() != 0) {
               current.setTimestamp(DateUtils.fromString(time));
          }

          // user attribute added in 0.4 API
          String user = atts.getValue("user");
          if (user != null) {
               // do not store literally; get object reference for string
               current.user = User.get(user);
          }

          // visible attribute added in 0.4 API
          String visible = atts.getValue("visible");
          if (visible != null) {
               current.visible = Boolean.parseBoolean(visible);
          }

          // oldversion attribute added in 0.6 API

          // Note there is an asymmetry here: the server will send
          // the version as "version" the client sends it as
          // "oldversion". So we take both since which we receive will
          // depend on reading from a file or reading from the server

          String version = atts.getValue("version");
          if (version != null) {
               current.version = Integer.parseInt(version);
          }
          version = atts.getValue("old_version");
          if (version != null) {
               current.version = Integer.parseInt(version);
          }

          String action = atts.getValue("action");
          if (action == null)
               return;
          if (action.equals("delete"))
               current.delete(true);
          else if (action.startsWith("modify"))
               current.modified = true;
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
         for (Node node : references.nodes)
              if (node.id == id)
                   return node;
         // TODO: This has to be changed to support multiple layers.
         for (Node node : Main.ds.nodes)
              if (node.id == id)
                   return new Node(node);
         return null;
    }

     private void createWays() {
          for (Entry<OsmPrimitiveData, Collection<Long>> e : ways.entrySet()) {
               Way w = new Way();
               boolean failed = false;
               for (long id : e.getValue()) {
                    Node n = findNode(id);
                    if (n == null) {
                         /* don't report ALL of them, just a few */
                         if (parseNotesCount++ < 6) {
                             parseNotes += tr("Skipping a way because it includes a node that doesn''t exist: {0}\n", id);
                         } else if (parseNotesCount == 6) {
                             parseNotes += "...\n";
                         }
                         failed = true;
                         break;
                    }
                    w.nodes.add(n);
               }
               if (failed) continue;
               e.getKey().copyTo(w);
               adder.visit(w);
          }

     }

     /**
      * Return the Way object with the given id, or null if it doesn't
      * exist yet. This method only looks at ways stored in the data set.
      *
      * @param id
      * @return way object or null
      */
     private Way findWay(long id) {
          for (Way wy : Main.ds.ways)
               if (wy.id == id)
                    return wy;
          return null;
     }

     /**
      * Return the Relation object with the given id, or null if it doesn't
      * exist yet. This method only looks at relations stored in the data set.
      *
      * @param id
      * @return relation object or null
      */
     private Relation findRelation(long id) {
          for (Relation e : ds.relations)
               if (e.id == id)
                    return e;
          for (Relation e : Main.ds.relations)
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
               adder.visit(en);
          }

          // Cache the ways here for much better search performance
          HashMap<Long, Way> hm = new HashMap<Long, Way>(10000);
          for (Way wy : ds.ways)
            hm.put(wy.id, wy);

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
                              adder.visit((Node)em.member);
                         }
                    } else if (emd.type.equals("way")) {
                         em.member = hm.get(emd.id);
                         if (em.member == null)
                            em.member = findWay(emd.id);
                         if (em.member == null) {
                              em.member = new Way(emd.id);
                              adder.visit((Way)em.member);
                         }
                    } else if (emd.type.equals("relation")) {
                         em.member = findRelation(emd.id);
                         if (em.member == null) {
                              em.member = new Relation(emd.id);
                              adder.visit((Relation)em.member);
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
     public static DataSet parseDataSet(InputStream source, DataSet ref, PleaseWaitDialog pleaseWaitDlg) throws SAXException, IOException {
          return parseDataSetOsm(source, ref, pleaseWaitDlg).ds;
     }

     public static OsmReader parseDataSetOsm(InputStream source, DataSet ref, PleaseWaitDialog pleaseWaitDlg) throws SAXException, IOException {
          OsmReader osm = new OsmReader();
          osm.references = ref == null ? new DataSet() : ref;


          currSource = source;

          // phase 1: Parse nodes and read in raw ways
          InputSource inputSource = new InputSource(new InputStreamReader(source, "UTF-8"));
          try {
             SAXParserFactory.newInstance().newSAXParser().parse(inputSource, osm.new Parser());
          } catch (ParserConfigurationException e1) {
             e1.printStackTrace(); // broken SAXException chaining
             throw new SAXException(e1);
          }

          Main.pleaseWaitDlg.currentAction.setText(tr("Prepare OSM data..."));
          Main.pleaseWaitDlg.setIndeterminate(true); 

//          System.out.println("Parser finished: Tags " + tagsN + " Nodes " + nodesN + " Ways " + waysN +
//            " Relations " + relationsN + " Members " + membersN);

          for (Node n : osm.nodes.values())
               osm.adder.visit(n);

          try {
               osm.createWays();
               osm.createRelations();
          } catch (NumberFormatException e) {
               e.printStackTrace();
               throw new SAXException(tr("Ill-formed node id"));
          }

          // clear all negative ids (new to this file)
          for (OsmPrimitive o : osm.ds.allPrimitives())
               if (o.id < 0)
                    o.id = 0;

//          System.out.println("Data loaded!");
          Main.pleaseWaitDlg.setIndeterminate(false); 
          Main.pleaseWaitDlg.progress.setValue(0); 

          return osm;
     }
}
