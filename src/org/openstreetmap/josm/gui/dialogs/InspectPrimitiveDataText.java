// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.conversion.AbstractCoordinateFormat;
import org.openstreetmap.josm.data.coor.conversion.DecimalDegreesCoordinateFormat;
import org.openstreetmap.josm.data.coor.conversion.ProjectedCoordinateFormat;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IRelationMember;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.proj.TransverseMercator;
import org.openstreetmap.josm.data.projection.proj.TransverseMercator.Hemisphere;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Textual representation of primitive contents, used in {@code InspectPrimitiveDialog}.
 * @since 10198
 */
public class InspectPrimitiveDataText {
    private static final String INDENT = "  ";
    private static final char NL = '\n';

    private final StringBuilder s = new StringBuilder();
    private final OsmData<?, ?, ?, ?> ds;

    InspectPrimitiveDataText(OsmData<?, ?, ?, ?> ds) {
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
            return name + tr(" ({0})", /* sic to avoid thousand separators */ Long.toString(id));
        } else {
            return Long.toString(id);
        }
    }

    /**
     * Adds a new OSM primitive.
     * @param o primitive to add
     */
    public void addPrimitive(IPrimitive o) {

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
        if (o instanceof OsmPrimitive) {
            addConflicts((OsmPrimitive) o);
        }
        s.append(NL);
    }

    void addHeadline(IPrimitive o) {
        addType(o);
        addNameAndId(o);
    }

    void addType(IPrimitive o) {
        if (o instanceof INode) {
            s.append(tr("Node: "));
        } else if (o instanceof IWay) {
            s.append(tr("Way: "));
        } else if (o instanceof IRelation) {
            s.append(tr("Relation: "));
        }
    }

    void addNameAndId(IPrimitive o) {
        String name = o.get("name");
        if (name == null) {
            s.append(o.getUniqueId());
        } else {
            s.append(getNameAndId(name, o.getUniqueId()));
        }
    }

    void addState(IPrimitive o) {
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

    void addCommon(IPrimitive o) {
        add(tr("Data Set: "), Integer.toHexString(o.getDataSet().hashCode()));
        add(tr("Edited at: "), o.isTimestampEmpty() ? tr("<new object>")
                : DateUtils.fromTimestamp(o.getRawTimestamp()));
        add(tr("Edited by: "), o.getUser() == null ? tr("<new object>")
                : getNameAndId(o.getUser().getName(), o.getUser().getId()));
        add(tr("Version:"), " ", Integer.toString(o.getVersion()));
        add(tr("In changeset: "), Integer.toString(o.getChangesetId()));
    }

    void addAttributes(IPrimitive o) {
        if (o.hasKeys()) {
            add(tr("Tags: "));
            for (String key : o.keySet()) {
                s.append(INDENT).append(INDENT);
                s.append(String.format("\"%s\"=\"%s\"%n", key, o.get(key)));
            }
        }
    }

    void addSpecial(IPrimitive o) {
        if (o instanceof INode) {
            addCoordinates((INode) o);
        } else if (o instanceof IWay) {
            addBbox(o);
            add(tr("Centroid: "), toStringCSV(false,
                    ProjectionRegistry.getProjection().eastNorth2latlon(Geometry.getCentroid(((IWay<?>) o).getNodes()))));
            addWayNodes((IWay<?>) o);
        } else if (o instanceof IRelation) {
            addBbox(o);
            addRelationMembers((IRelation<?>) o);
        }
    }

    void addRelationMembers(IRelation<?> r) {
        add(trn("{0} Member: ", "{0} Members: ", r.getMembersCount(), r.getMembersCount()));
        for (IRelationMember<?> m : r.getMembers()) {
            s.append(INDENT).append(INDENT);
            addHeadline(m.getMember());
            s.append(tr(" as \"{0}\"", m.getRole()));
            s.append(NL);
        }
    }

    void addWayNodes(IWay<?> w) {
        add(tr("{0} Nodes: ", w.getNodesCount()));
        for (INode n : w.getNodes()) {
            s.append(INDENT).append(INDENT);
            addNameAndId(n);
            s.append(NL);
        }
    }

    void addBbox(IPrimitive o) {
        BBox bbox = o.getBBox();
        if (bbox != null) {
            final LatLon bottomRight = bbox.getBottomRight();
            final LatLon topLeft = bbox.getTopLeft();
            add(tr("Bounding box: "), toStringCSV(false, bottomRight, topLeft));
            add(tr("Bounding box (projected): "), toStringCSV(true, bottomRight, topLeft));
            add(tr("Center of bounding box: "), toStringCSV(false, bbox.getCenter()));
        }
    }

    void addCoordinates(INode n) {
        if (n.isLatLonKnown()) {
            add(tr("Coordinates:"), " ", toStringCSV(false, n));
            add(tr("Coordinates (projected): "), toStringCSV(true, n));
            Pair<Integer, Hemisphere> utmZone = TransverseMercator.locateUtmZone(n.getCoor());
            String utmLabel = tr("UTM Zone");
            add(utmLabel, utmLabel.endsWith(":") ? " " : ": ", Integer.toString(utmZone.a), utmZone.b.name().substring(0, 1));
        }
    }

    void addReferrers(StringBuilder s, IPrimitive o) {
        List<? extends IPrimitive> refs = o.getReferrers();
        if (!refs.isEmpty()) {
            add(tr("Part of: "));
            for (IPrimitive p : refs) {
                s.append(INDENT).append(INDENT);
                addHeadline(p);
                s.append(NL);
            }
        }
    }

    void addConflicts(OsmPrimitive o) {
        Conflict<?> c = ((DataSet) ds).getConflicts().getConflictForMy(o);
        if (c != null) {
            add(tr("In conflict with: "));
            addNameAndId(c.getTheir());
        }
    }

    /**
     * Returns the coordinates in human-readable format.
     * @param projected whether to use projected coordinates
     * @param coordinates the coordinates to format
     * @return String in the format {@code "1.23456, 2.34567"}
     */
    private static String toStringCSV(boolean projected, ILatLon... coordinates) {
        final AbstractCoordinateFormat format = projected
                ? ProjectedCoordinateFormat.INSTANCE
                : DecimalDegreesCoordinateFormat.INSTANCE;
        return Arrays.stream(coordinates)
                .flatMap(ll -> Stream.of(format.latToString(ll), format.lonToString(ll)))
                .collect(Collectors.joining(", "));
    }

    @Override
    public String toString() {
        return s.toString();
    }
}
