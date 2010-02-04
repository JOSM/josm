// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.history.HistoryDataSet;
import org.openstreetmap.josm.data.osm.history.HistoryNode;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.data.osm.history.HistoryRelation;
import org.openstreetmap.josm.data.osm.history.HistoryWay;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.DateUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parser for OSM history data.
 *
 * It is slightly different from {@see OsmReader} because we don't build an internal graph of
 * {@see OsmPrimitive}s. We use objects derived from {@see HistoryOsmPrimitive} instead and we
 * keep the data in a dedicated {@see HistoryDataSet}.
 *
 */
public class OsmHistoryReader {

    private final InputStream in;
    private final HistoryDataSet data;

    private class Parser extends DefaultHandler {

        /** the current primitive to be read */
        private HistoryOsmPrimitive current;
        private Locator locator;

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        protected String getCurrentPosition() {
            if (locator == null)
                return "";
            return "(" + locator.getLineNumber() + "," + locator.getColumnNumber() + ")";
        }

        protected void throwException(String message) throws SAXException {
            throw new SAXException(
                    getCurrentPosition()
                    +   message
            );
        }

        protected long getMandatoryAttributeLong(Attributes attr, String name) throws SAXException{
            String v = attr.getValue(name);
            if (v == null) {
                throwException(tr("Missing mandatory attribute ''{0}''.", name));
            }
            Long l = 0l;
            try {
                l = Long.parseLong(v);
            } catch(NumberFormatException e) {
                throwException(tr("Illegal value for mandatory attribute ''{0}'' of type long. Got ''{1}''.", name, v));
            }
            if (l < 0) {
                throwException(tr("Illegal value for mandatory attribute ''{0}'' of type long (>=0). Got ''{1}''.", name, v));
            }
            return l;
        }

        protected long getAttributeLong(Attributes attr, String name, long defaultValue) throws SAXException{
            String v = attr.getValue(name);
            if (v == null)
                return defaultValue;
            Long l = 0l;
            try {
                l = Long.parseLong(v);
            } catch(NumberFormatException e) {
                throwException(tr("Illegal value for mandatory attribute ''{0}'' of type long. Got ''{1}''.", name, v));
            }
            if (l < 0) {
                throwException(tr("Illegal value for mandatory attribute ''{0}'' of type long (>=0). Got ''{1}''.", name, v));
            }
            return l;
        }

        protected Double getMandatoryAttributeDouble(Attributes attr, String name) throws SAXException{
            String v = attr.getValue(name);
            if (v == null) {
                throwException(tr("Missing mandatory attribute ''{0}''.", name));
            }
            double d = 0.0;
            try {
                d = Double.parseDouble(v);
            } catch(NumberFormatException e) {
                throwException(tr("Illegal value for mandatory attribute ''{0}'' of type double. Got ''{1}''.", name, v));
            }
            return d;
        }

        protected String getMandatoryAttributeString(Attributes attr, String name) throws SAXException{
            String v = attr.getValue(name);
            if (v == null) {
                throwException(tr("Missing mandatory attribute ''{0}''.", name));
            }
            return v;
        }

        protected String getAttributeString(Attributes attr, String name, String defaultValue) {
            String v = attr.getValue(name);
            if (v == null)
                return defaultValue;
            return v;
        }

        protected boolean getMandatoryAttributeBoolean(Attributes attr, String name) throws SAXException{
            String v = attr.getValue(name);
            if (v == null) {
                throwException(tr("Missing mandatory attribute ''{0}''.", name));
            }
            if ("true".equals(v)) return true;
            if ("false".equals(v)) return false;
            throwException(tr("Illegal value for mandatory attribute ''{0}'' of type boolean. Got ''{1}''.", name, v));
            // not reached
            return false;
        }

        protected  HistoryOsmPrimitive createPrimitive(Attributes atts, OsmPrimitiveType type) throws SAXException {
            long id = getMandatoryAttributeLong(atts,"id");
            long version = getMandatoryAttributeLong(atts,"version");
            long changesetId = getMandatoryAttributeLong(atts,"changeset");
            boolean visible= getMandatoryAttributeBoolean(atts, "visible");
            long uid = getAttributeLong(atts, "uid",-1);
            String user = getAttributeString(atts, "user", tr("<anonymous>"));
            String v = getMandatoryAttributeString(atts, "timestamp");
            Date timestamp = DateUtils.fromString(v);
            HistoryOsmPrimitive primitive = null;
            if (type.equals(OsmPrimitiveType.NODE)) {
                double lat = getMandatoryAttributeDouble(atts, "lat");
                double lon = getMandatoryAttributeDouble(atts, "lon");
                primitive = new HistoryNode(
                        id,version,visible,user,uid,changesetId,timestamp, new LatLon(lat,lon)
                );

            } else if (type.equals(OsmPrimitiveType.WAY)) {
                primitive = new HistoryWay(
                        id,version,visible,user,uid,changesetId,timestamp
                );
            }if (type.equals(OsmPrimitiveType.RELATION)) {
                primitive = new HistoryRelation(
                        id,version,visible,user,uid,changesetId,timestamp
                );
            }
            return primitive;
        }

        protected void startNode(Attributes atts) throws SAXException {
            current= createPrimitive(atts, OsmPrimitiveType.NODE);
        }

        protected void startWay(Attributes atts) throws SAXException {
            current= createPrimitive(atts, OsmPrimitiveType.WAY);
        }
        protected void startRelation(Attributes atts) throws SAXException {
            current= createPrimitive(atts, OsmPrimitiveType.RELATION);
        }

        protected void handleTag(Attributes atts) throws SAXException {
            String key= getMandatoryAttributeString(atts, "k");
            String value= getMandatoryAttributeString(atts, "v");
            current.put(key,value);
        }

        protected void handleNodeReference(Attributes atts) throws SAXException {
            long ref = getMandatoryAttributeLong(atts, "ref");
            ((HistoryWay)current).addNode(ref);
        }

        protected void handleMember(Attributes atts) throws SAXException {
            long ref = getMandatoryAttributeLong(atts, "ref");
            String v = getMandatoryAttributeString(atts, "type");
            OsmPrimitiveType type = null;
            try {
                type = OsmPrimitiveType.fromApiTypeName(v);
            } catch(IllegalArgumentException e) {
                throwException(tr("Illegal value for mandatory attribute ''{0}'' of type OsmPrimitiveType. Got ''{1}''.", "type", v));
            }
            String role = getMandatoryAttributeString(atts, "role");
            org.openstreetmap.josm.data.osm.history.RelationMember member = new org.openstreetmap.josm.data.osm.history.RelationMember(role, type,ref);
            ((HistoryRelation)current).addMember(member);
        }

        @Override public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if (qName.equals("node")) {
                startNode(atts);
            } else if (qName.equals("way")) {
                startWay(atts);
            } else if (qName.equals("relation")) {
                startRelation(atts);
            } else if (qName.equals("tag")) {
                handleTag(atts);
            } else if (qName.equals("nd")) {
                handleNodeReference(atts);
            } else if (qName.equals("member")) {
                handleMember(atts);
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("node")
                    || qName.equals("way")
                    || qName.equals("relation")) {
                data.put(current);
            }
        }
    }

    public OsmHistoryReader(InputStream source) {
        this.in = source;
        this.data = new HistoryDataSet();
    }

    public HistoryDataSet parse(ProgressMonitor progressMonitor) throws SAXException, IOException {
        InputSource inputSource = new InputSource(new InputStreamReader(in, "UTF-8"));
        progressMonitor.beginTask(tr("Parsing OSM history data ..."));
        try {
            SAXParserFactory.newInstance().newSAXParser().parse(inputSource, new Parser());
        } catch (ParserConfigurationException e1) {
            e1.printStackTrace(); // broken SAXException chaining
            throw new SAXException(e1);
        } finally {
            progressMonitor.finishTask();
        }
        return data;
    }
}
