// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Date;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.history.HistoryNode;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.data.osm.history.HistoryRelation;
import org.openstreetmap.josm.data.osm.history.HistoryWay;
import org.openstreetmap.josm.tools.date.DateUtils;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Base class of {@link OsmChangesetContentParser} and {@link OsmHistoryReader} internal parsers.
 * @since 6201
 */
public abstract class AbstractParser extends DefaultHandler {

    /** the current primitive to be read */
    protected HistoryOsmPrimitive currentPrimitive;
    protected Locator locator;

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    protected abstract void throwException(String message) throws SAXException;

    protected abstract void throwException(String message, Exception e) throws SAXException;

    protected final long getMandatoryAttributeLong(Attributes attr, String name) throws SAXException {
        String v = attr.getValue(name);
        if (v == null) {
            throwException(tr("Missing mandatory attribute ''{0}''.", name));
        }
        long l = 0L;
        try {
            l = Long.parseLong(v);
        } catch (NumberFormatException e) {
            throwException(tr("Illegal value for mandatory attribute ''{0}'' of type long. Got ''{1}''.", name, v), e);
        }
        if (l < 0) {
            throwException(tr("Illegal value for mandatory attribute ''{0}'' of type long (>=0). Got ''{1}''.", name, v));
        }
        return l;
    }

    protected final Long getAttributeLong(Attributes attr, String name) throws SAXException {
        String v = attr.getValue(name);
        if (v == null)
            return null;
        Long l = 0L;
        try {
            l = Long.valueOf(v);
        } catch (NumberFormatException e) {
            throwException(tr("Illegal value for mandatory attribute ''{0}'' of type long. Got ''{1}''.", name, v), e);
        }
        if (l < 0) {
            throwException(tr("Illegal value for mandatory attribute ''{0}'' of type long (>=0). Got ''{1}''.", name, v));
        }
        return l;
    }

    protected final Double getAttributeDouble(Attributes attr, String name) throws SAXException {
        String v = attr.getValue(name);
        if (v == null) {
            return null;
        }
        double d = 0.0;
        try {
            d = Double.parseDouble(v);
        } catch (NumberFormatException e) {
            throwException(tr("Illegal value for attribute ''{0}'' of type double. Got ''{1}''.", name, v), e);
        }
        return d;
    }

    protected final String getMandatoryAttributeString(Attributes attr, String name) throws SAXException {
        String v = attr.getValue(name);
        if (v == null) {
            throwException(tr("Missing mandatory attribute ''{0}''.", name));
        }
        return v;
    }

    protected boolean getMandatoryAttributeBoolean(Attributes attr, String name) throws SAXException {
        String v = attr.getValue(name);
        if (v == null) {
            throwException(tr("Missing mandatory attribute ''{0}''.", name));
        }
        if ("true".equals(v)) return true;
        if ("false".equals(v)) return false;
        throwException(tr("Illegal value for mandatory attribute ''{0}'' of type boolean. Got ''{1}''.", name, v));
        return false; // not reached
    }

    protected final HistoryOsmPrimitive createPrimitive(Attributes atts, OsmPrimitiveType type) throws SAXException {
        long id = getMandatoryAttributeLong(atts, "id");
        long version = getMandatoryAttributeLong(atts, "version");
        Long changeset = getAttributeLong(atts, "changeset");
        long changesetId = changeset != null ? changeset : 0L;
        boolean visible = getMandatoryAttributeBoolean(atts, "visible");

        Long uid = getAttributeLong(atts, "uid");
        String userStr = atts.getValue("user");
        User user;
        if (userStr != null) {
            if (uid != null) {
                user = User.createOsmUser(uid, userStr);
                user.setPreferredName(userStr);
            } else {
                user = User.createLocalUser(userStr);
            }
        } else {
            user = User.getAnonymous();
        }

        String v = getMandatoryAttributeString(atts, "timestamp");
        Date timestamp = DateUtils.fromString(v);
        HistoryOsmPrimitive primitive = null;
        if (type.equals(OsmPrimitiveType.NODE)) {
            Double lat = getAttributeDouble(atts, "lat");
            Double lon = getAttributeDouble(atts, "lon");
            LatLon coor = (lat != null && lon != null) ? new LatLon(lat, lon) : null;
            primitive = new HistoryNode(id, version, visible, user, changesetId, timestamp, coor, changeset != null);

        } else if (type.equals(OsmPrimitiveType.WAY)) {
            primitive = new HistoryWay(id, version, visible, user, changesetId, timestamp, changeset != null);
        } else if (type.equals(OsmPrimitiveType.RELATION)) {
            primitive = new HistoryRelation(id, version, visible, user, changesetId, timestamp, changeset != null);
        }
        return primitive;
    }

    protected final void startNode(Attributes atts) throws SAXException {
        currentPrimitive = createPrimitive(atts, OsmPrimitiveType.NODE);
    }

    protected final void startWay(Attributes atts) throws SAXException {
        currentPrimitive = createPrimitive(atts, OsmPrimitiveType.WAY);
    }

    protected final void startRelation(Attributes atts) throws SAXException {
        currentPrimitive = createPrimitive(atts, OsmPrimitiveType.RELATION);
    }

    protected final void handleTag(Attributes atts) throws SAXException {
        String key = getMandatoryAttributeString(atts, "k");
        String value = getMandatoryAttributeString(atts, "v");
        currentPrimitive.put(key, value);
    }

    protected final void handleNodeReference(Attributes atts) throws SAXException {
        long ref = getMandatoryAttributeLong(atts, "ref");
        ((HistoryWay) currentPrimitive).addNode(ref);
    }

    protected void handleMember(Attributes atts) throws SAXException {
        long ref = getMandatoryAttributeLong(atts, "ref");
        String v = getMandatoryAttributeString(atts, "type");
        OsmPrimitiveType type = null;
        try {
            type = OsmPrimitiveType.fromApiTypeName(v);
        } catch (IllegalArgumentException e) {
            throwException(tr("Illegal value for mandatory attribute ''{0}'' of type OsmPrimitiveType. Got ''{1}''.", "type", v), e);
        }
        String role = getMandatoryAttributeString(atts, "role");
        RelationMemberData member = new RelationMemberData(role, type, ref);
        ((HistoryRelation) currentPrimitive).addMember(member);
    }

    protected final boolean doStartElement(String qName, Attributes atts) throws SAXException {
        switch (qName) {
        case "node":
            startNode(atts);
            return true;
        case "way":
            startWay(atts);
            return true;
        case "relation":
            startRelation(atts);
            return true;
        case "tag":
            handleTag(atts);
            return true;
        case "nd":
            handleNodeReference(atts);
            return true;
        case "member":
            handleMember(atts);
            return true;
        default:
            return false;
        }
    }
}
