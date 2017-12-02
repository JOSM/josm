// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Arrays;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.conversion.DecimalDegreesCoordinateFormat;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.proj.TransverseMercator;
import org.openstreetmap.josm.data.projection.proj.TransverseMercator.Hemisphere;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Textual representation of primitive contents, used in {@code InspectPrimitiveDialog}.
 * @since 10198
 */
public class InspectPrimitiveDataText {
    private static final String INDENT = "  ";
    private static final char NL = '\n';

    private final StringBuilder s = new StringBuilder();
    private final DataSet ds;

    InspectPrimitiveDataText(DataSet ds) {
        this.ds = ds;
    }

    private InspectPrimitiveDataText add(String title, String... values) {
        s.append(INDENT).append(title);
        for (String v : values) {
            s.append(v);
        }
        s.append(NL);
        return this;
    }

    private static String getNameAndId(String name, long id) {
        if (name != null) {
            return name + tr(" ({0})", /* sic to avoid thousand seperators */ Long.toString(id));
        } else {
            return Long.toString(id);
        }
    }

    /**
     * Adds a new OSM primitive.
     * @param o primitive to add
     */
    public void addPrimitive(OsmPrimitive o) {

        addHeadline(o);

        if (!(o.getDataSet() != null && o.getDataSet().getPrimitiveById(o) != null)) {
            s.append(NL).append(INDENT).append(tr("not in data set")).append(NL);
            return;
        }
        if (o.isIncomplete()) {
            s.append(NL).append(INDENT).append(tr("incomplete")).append(NL);
            return;
        }
        s.append(NL);

        addState(o);
        addCommon(o);
        addAttributes(o);
        addSpecial(o);
        addReferrers(s, o);
        addConflicts(o);
        s.append(NL);
    }

    void addHeadline(OsmPrimitive o) {
        addType(o);
        addNameAndId(o);
    }

    void addType(OsmPrimitive o) {
        if (o instanceof Node) {
            s.append(tr("Node: "));
        } else if (o instanceof Way) {
            s.append(tr("Way: "));
        } else if (o instanceof Relation) {
            s.append(tr("Relation: "));
        }
    }

    void addNameAndId(OsmPrimitive o) {
        String name = o.get("name");
        if (name == null) {
            s.append(o.getUniqueId());
        } else {
            s.append(getNameAndId(name, o.getUniqueId()));
        }
    }

    void addState(OsmPrimitive o) {
        StringBuilder sb = new StringBuilder(INDENT);
        /* selected state is left out: not interesting as it is always selected */
        if (o.isDeleted()) {
            sb.append(tr("deleted")).append(INDENT);
        }
        if (!o.isVisible()) {
            sb.append(tr("deleted-on-server")).append(INDENT);
        }
        if (o.isModified()) {
            sb.append(tr("modified")).append(INDENT);
        }
        if (o.isDisabledAndHidden()) {
            sb.append(tr("filtered/hidden")).append(INDENT);
        }
        if (o.isDisabled()) {
            sb.append(tr("filtered/disabled")).append(INDENT);
        }
        if (o.hasDirectionKeys()) {
            if (o.reversedDirection()) {
                sb.append(tr("has direction keys (reversed)")).append(INDENT);
            } else {
                sb.append(tr("has direction keys")).append(INDENT);
            }
        }
        String state = sb.toString().trim();
        if (!state.isEmpty()) {
            add(tr("State: "), sb.toString().trim());
        }
    }

    void addCommon(OsmPrimitive o) {
        add(tr("Data Set: "), Integer.toHexString(o.getDataSet().hashCode()));
        add(tr("Edited at: "), o.isTimestampEmpty() ? tr("<new object>")
                : DateUtils.fromTimestamp(o.getRawTimestamp()));
        add(tr("Edited by: "), o.getUser() == null ? tr("<new object>")
                : getNameAndId(o.getUser().getName(), o.getUser().getId()));
        add(tr("Version: "), Integer.toString(o.getVersion()));
        add(tr("In changeset: "), Integer.toString(o.getChangesetId()));
    }

    void addAttributes(OsmPrimitive o) {
        if (o.hasKeys()) {
            add(tr("Tags: "));
            for (String key : o.keySet()) {
                s.append(INDENT).append(INDENT);
                s.append(String.format("\"%s\"=\"%s\"%n", key, o.get(key)));
            }
        }
    }

    void addSpecial(OsmPrimitive o) {
        if (o instanceof Node) {
            addCoordinates((Node) o);
        } else if (o instanceof Way) {
            addBbox(o);
            add(tr("Centroid: "),
                    toStringCSV(", ", Main.getProjection().eastNorth2latlon(
                            Geometry.getCentroid(((Way) o).getNodes()))));
            addWayNodes((Way) o);
        } else if (o instanceof Relation) {
            addBbox(o);
            addRelationMembers((Relation) o);
        }
    }

    void addRelationMembers(Relation r) {
        add(trn("{0} Member: ", "{0} Members: ", r.getMembersCount(), r.getMembersCount()));
        for (RelationMember m : r.getMembers()) {
            s.append(INDENT).append(INDENT);
            addHeadline(m.getMember());
            s.append(tr(" as \"{0}\"", m.getRole()));
            s.append(NL);
        }
    }

    void addWayNodes(Way w) {
        add(tr("{0} Nodes: ", w.getNodesCount()));
        for (Node n : w.getNodes()) {
            s.append(INDENT).append(INDENT);
            addNameAndId(n);
            s.append(NL);
        }
    }

    void addBbox(OsmPrimitive o) {
        BBox bbox = o.getBBox();
        if (bbox != null) {
            add(tr("Bounding box: "), bbox.toStringCSV(", "));
            EastNorth bottomRigth = bbox.getBottomRight().getEastNorth(Main.getProjection());
            EastNorth topLeft = bbox.getTopLeft().getEastNorth(Main.getProjection());
            add(tr("Bounding box (projected): "),
                    Double.toString(topLeft.east()), ", ",
                    Double.toString(bottomRigth.north()), ", ",
                    Double.toString(bottomRigth.east()), ", ",
                    Double.toString(topLeft.north()));
            add(tr("Center of bounding box: "), toStringCSV(", ", bbox.getCenter()));
        }
    }

    void addCoordinates(Node n) {
        if (n.isLatLonKnown()) {
            add(tr("Coordinates: "),
                    Double.toString(n.lat()), ", ",
                    Double.toString(n.lon()));
            EastNorth en = n.getEastNorth();
            add(tr("Coordinates (projected): "),
                    Double.toString(en.east()), ", ",
                    Double.toString(en.north()));
            Pair<Integer, Hemisphere> utmZone = TransverseMercator.locateUtmZone(n.getCoor());
            String utmLabel = tr("UTM Zone");
            add(utmLabel, utmLabel.endsWith(":") ? " " : ": ", Integer.toString(utmZone.a), utmZone.b.name().substring(0, 1));
        }
    }

    void addReferrers(StringBuilder s, OsmPrimitive o) {
        List<OsmPrimitive> refs = o.getReferrers();
        if (!refs.isEmpty()) {
            add(tr("Part of: "));
            for (OsmPrimitive p : refs) {
                s.append(INDENT).append(INDENT);
                addHeadline(p);
                s.append(NL);
            }
        }
    }

    void addConflicts(OsmPrimitive o) {
        Conflict<?> c = ds.getConflicts().getConflictForMy(o);
        if (c != null) {
            add(tr("In conflict with: "));
            addNameAndId(c.getTheir());
        }
    }

    /**
     * Returns lat/lon coordinate in human-readable format separated by {@code separator}.
     * @param separator values separator
     * @param ll the lat/lon
     * @return String in the format {@code "1.23456[separator]2.34567"}
     */
    private static String toStringCSV(String separator, ILatLon ll) {
        return Utils.join(separator, Arrays.asList(
                DecimalDegreesCoordinateFormat.INSTANCE.latToString(ll),
                DecimalDegreesCoordinateFormat.INSTANCE.lonToString(ll)
        ));
    }

    @Override
    public String toString() {
        return s.toString();
    }
}
