// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.ChangesetDataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.ChangesetDataSet.ChangesetModificationType;
import org.openstreetmap.josm.data.osm.history.HistoryNode;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.data.osm.history.HistoryRelation;
import org.openstreetmap.josm.data.osm.history.HistoryWay;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.DateUtils;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parser for OSM changeset content.
 *
 */
public class OsmChangesetContentParser {

    private InputSource source;
    private ChangesetDataSet data;

    private class Parser extends DefaultHandler {

        /** the current primitive to be read */
        private HistoryOsmPrimitive currentPrimitive;
        /** the current change modification type */
        private ChangesetDataSet.ChangesetModificationType currentModificationType;

        private Locator locator;

        @Override
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        protected void throwException(String message) throws OsmDataParsingException {
            throw new OsmDataParsingException(
                    message
            ).rememberLocation(locator);
        }

        protected void throwException(Exception e) throws OsmDataParsingException {
            throw new OsmDataParsingException(
                    e
            ).rememberLocation(locator);
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
            if (v.equals("true")) return true;
            if (v.equals("false")) return false;
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
            currentPrimitive= createPrimitive(atts, OsmPrimitiveType.NODE);
        }

        protected void startWay(Attributes atts) throws SAXException {
            currentPrimitive= createPrimitive(atts, OsmPrimitiveType.WAY);
        }
        protected void startRelation(Attributes atts) throws SAXException {
            currentPrimitive= createPrimitive(atts, OsmPrimitiveType.RELATION);
        }

        protected void handleTag(Attributes atts) throws SAXException {
            String key= getMandatoryAttributeString(atts, "k");
            String value= getMandatoryAttributeString(atts, "v");
            currentPrimitive.put(key,value);
        }

        protected void handleNodeReference(Attributes atts) throws SAXException {
            long ref = getMandatoryAttributeLong(atts, "ref");
            ((HistoryWay)currentPrimitive).addNode(ref);
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
            ((HistoryRelation)currentPrimitive).addMember(member);
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
            } else if (qName.equals("osmChange")) {
                // do nothing
            } else if (qName.equals("create")) {
                currentModificationType = ChangesetModificationType.CREATED;
            } else if (qName.equals("modify")) {
                currentModificationType = ChangesetModificationType.UPDATED;
            } else if (qName.equals("delete")) {
                currentModificationType = ChangesetModificationType.DELETED;
            } else {
                System.err.println(tr("Warning: unsupported start element ''{0}'' in changeset content at position ({1},{2}). Skipping.", qName, locator.getLineNumber(), locator.getColumnNumber()));
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (qName.equals("node")
                    || qName.equals("way")
                    || qName.equals("relation")) {
                if (currentModificationType == null) {
                    throwException(tr("Illegal document structure. Found node, way, or relation outside of 'create', 'modify', or 'delete'."));
                }
                data.put(currentPrimitive, currentModificationType);
            } else if (qName.equals("osmChange")) {
                // do nothing
            } else if (qName.equals("create")) {
                currentModificationType = null;
            } else if (qName.equals("modify")) {
                currentModificationType = null;
            } else if (qName.equals("delete")) {
                currentModificationType = null;
            } else if (qName.equals("tag")) {
                // do nothing
            } else if (qName.equals("nd")) {
                // do nothing
            } else if (qName.equals("member")) {
                // do nothing
            } else {
                System.err.println(tr("Warning: unsupported end element ''{0}'' in changeset content at position ({1},{2}). Skipping.", qName, locator.getLineNumber(), locator.getColumnNumber()));
            }
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            throwException(e);
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            throwException(e);
        }
    }

    /**
     * Create a parser
     *
     * @param source the input stream with the changeset content as XML document. Must not be null.
     * @throws IllegalArgumentException thrown if source is null.
     */
    public OsmChangesetContentParser(InputStream source) throws UnsupportedEncodingException {
        CheckParameterUtil.ensureParameterNotNull(source, "source");
        this.source = new InputSource(new InputStreamReader(source, "UTF-8"));
        data = new ChangesetDataSet();
    }

    public OsmChangesetContentParser(String source) {
        CheckParameterUtil.ensureParameterNotNull(source, "source");
        this.source = new InputSource(new StringReader(source));
        data = new ChangesetDataSet();
    }

    /**
     * Parses the content
     *
     * @param progressMonitor the progress monitor. Set to {@see NullProgressMonitor#INSTANCE}
     * if null
     * @return the parsed data
     * @throws OsmDataParsingException thrown if something went wrong. Check for chained
     * exceptions.
     */
    public ChangesetDataSet parse(ProgressMonitor progressMonitor) throws OsmDataParsingException {
        if (progressMonitor == null) {
            progressMonitor = NullProgressMonitor.INSTANCE;
        }
        try {
            progressMonitor.beginTask("");
            progressMonitor.indeterminateSubTask(tr("Parsing changeset content ..."));
            SAXParserFactory.newInstance().newSAXParser().parse(source, new Parser());
        } catch(OsmDataParsingException e){
            throw e;
        } catch (ParserConfigurationException e) {
            throw new OsmDataParsingException(e);
        } catch(SAXException e) {
            throw new OsmDataParsingException(e);
        } catch(IOException e) {
            throw new OsmDataParsingException(e);
        } finally {
            progressMonitor.finishTask();
        }
        return data;
    }

    /**
     * Parses the content from the input source
     *
     * @return the parsed data
     * @throws OsmDataParsingException thrown if something went wrong. Check for chained
     * exceptions.
     */
    public ChangesetDataSet parse() throws OsmDataParsingException {
        return parse(null);
    }
}
