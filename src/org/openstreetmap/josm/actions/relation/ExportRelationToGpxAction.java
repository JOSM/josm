// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.relation;

import static org.openstreetmap.josm.actions.relation.ExportRelationToGpxAction.Mode.FROM_FIRST_MEMBER;
import static org.openstreetmap.josm.actions.relation.ExportRelationToGpxAction.Mode.TO_FILE;
import static org.openstreetmap.josm.actions.relation.ExportRelationToGpxAction.Mode.TO_LAYER;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;

import org.openstreetmap.josm.actions.GpxExportAction;
import org.openstreetmap.josm.actions.OsmPrimitiveAction;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.ImmutableGpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionTypeCalculator;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;

/**
 * Exports the current relation to a single GPX track,
 * currently for type=route and type=superroute relations only.
 *
 * @since 13210
 */
public class ExportRelationToGpxAction extends GpxExportAction
    implements OsmPrimitiveAction {

    /** Enumeration of export variants */
    public enum Mode {
        /** concatenate members from first to last element */
        FROM_FIRST_MEMBER,
        /** concatenate members from last to first element */
        FROM_LAST_MEMBER,
        /** export to GPX layer and add to LayerManager */
        TO_LAYER,
        /** export to GPX file and open FileChooser */
        TO_FILE
    }

    /** Mode of this ExportToGpxAction */
    protected final EnumSet<Mode> mode;

    /** Primitives this action works on */
    protected Collection<Relation> relations = Collections.<Relation>emptySet();

    /** Construct a new ExportRelationToGpxAction with default mode */
    public ExportRelationToGpxAction() {
        this(EnumSet.of(FROM_FIRST_MEMBER, TO_FILE));
    }

    /**
     * Constructs a new {@code ExportRelationToGpxAction}
     *
     * @param mode which mode to use, see {@code ExportRelationToGpxAction.Mode}
     */
    public ExportRelationToGpxAction(EnumSet<Mode> mode) {
        super(name(mode), mode.contains(TO_FILE) ? "exportgpx" : "dialogs/layerlist", tooltip(mode),
                null, false, null, false);
        putValue("help", ht("/Action/ExportRelationToGpx"));
        this.mode = mode;
    }

    private static String name(EnumSet<Mode> mode) {
        if (mode.contains(TO_FILE)) {
            if (mode.contains(FROM_FIRST_MEMBER)) {
                return tr("Export GPX file starting from first member");
            } else {
                return tr("Export GPX file starting from last member");
            }
        } else {
            if (mode.contains(FROM_FIRST_MEMBER)) {
                return tr("Convert to GPX layer starting from first member");
            } else {
                return tr("Convert to GPX layer starting from last member");
            }
        }
    }

    private static String tooltip(EnumSet<Mode> mode) {
        if (mode.contains(FROM_FIRST_MEMBER)) {
            return tr("Flatten this relation to a single gpx track recursively, " +
                    "starting with the first member, successively continuing to the last.");
        } else {
            return tr("Flatten this relation to a single gpx track recursively, " +
                    "starting with the last member, successively continuing to the first.");
        }
    }

    private static final class BidiIterableList {
        private final List<RelationMember> l;

        private BidiIterableList(List<RelationMember> l) {
            this.l = l;
        }

        public Iterator<RelationMember> iterator() {
            return l.iterator();
        }

        public Iterator<RelationMember> reverseIterator() {
            ListIterator<RelationMember> li = l.listIterator(l.size());
            return new Iterator<RelationMember>() {
                @Override
                public boolean hasNext() {
                    return li.hasPrevious();
                }

                @Override
                public RelationMember next() {
                    return li.previous();
                }

                @Override
                public void remove() {
                    li.remove();
                }
            };
        }
    }

    @Override
    protected Layer getLayer() {
        List<RelationMember> flat = new ArrayList<>();

        List<RelationMember> init = new ArrayList<>();
        relations.forEach(t -> init.add(new RelationMember("", t)));
        BidiIterableList l = new BidiIterableList(init);

        Stack<Iterator<RelationMember>> stack = new Stack<>();
        stack.push(mode.contains(FROM_FIRST_MEMBER) ? l.iterator() : l.reverseIterator());

        List<Relation> relsFound = new ArrayList<>();
        do {
            Iterator<RelationMember> i = stack.peek();
            if (!i.hasNext())
                stack.pop();
            while (i.hasNext()) {
                RelationMember m = i.next();
                if (m.isRelation() && !m.getRelation().isIncomplete()) {
                    l = new BidiIterableList(m.getRelation().getMembers());
                    stack.push(mode.contains(FROM_FIRST_MEMBER) ? l.iterator() : l.reverseIterator());
                    relsFound.add(m.getRelation());
                    break;
                }
                if (m.isWay()) {
                    flat.add(m);
                }
            }
        } while (!stack.isEmpty());

        GpxData gpxData = new GpxData();
        String layerName = " (GPX export)";
        long time = System.currentTimeMillis()-24*3600*1000;

        if (!flat.isEmpty()) {
            Map<String, Object> trkAttr = new HashMap<>();
            Collection<Collection<WayPoint>> trk = new ArrayList<>();
            List<WayPoint> trkseg = new ArrayList<>();
            trk.add(trkseg);

            List<WayConnectionType> wct = new WayConnectionTypeCalculator().updateLinks(flat);
            final HashMap<String, Integer> names = new HashMap<>();
            for (int i = 0; i < flat.size(); i++) {
                if (!wct.get(i).isOnewayLoopBackwardPart) {
                    if (!wct.get(i).direction.isRoundabout()) {
                        if (!wct.get(i).linkPrev && !trkseg.isEmpty()) {
                            gpxData.addTrack(new ImmutableGpxTrack(trk, trkAttr));
                            trkAttr.clear();
                            trk.clear();
                            trkseg.clear();
                            trk.add(trkseg);
                        }
                        if (trkAttr.isEmpty()) {
                            Relation r = Way.getParentRelations(Arrays.asList(flat.get(i).getWay()))
                                    .stream().filter(relsFound::contains).findFirst().orElseGet(null);
                            if (r != null)
                                trkAttr.put("name", r.getName() != null ? r.getName() : r.getId());
                            GpxData.ensureUniqueName(trkAttr, names);
                        }
                        List<Node> ln = flat.get(i).getWay().getNodes();
                        if (wct.get(i).direction == WayConnectionType.Direction.BACKWARD)
                            Collections.reverse(ln);
                        for (Node n: ln) {
                            trkseg.add(OsmDataLayer.nodeToWayPoint(n, time));
                            time += 1000;
                        }
                    }
                }
            }
            gpxData.addTrack(new ImmutableGpxTrack(trk, trkAttr));

            String lprefix = relations.iterator().next().getName();
            if (lprefix == null || relations.size() > 1)
                lprefix = tr("Selected Relations");
            layerName = lprefix + layerName;
        }

        return new GpxLayer(gpxData, layerName, true);
    }

    /**
     *
     * @param e the ActionEvent
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (mode.contains(TO_LAYER))
            MainApplication.getLayerManager().addLayer(getLayer());
        if (mode.contains(TO_FILE))
            super.actionPerformed(e);
    }

    @Override
    public void setPrimitives(Collection<? extends OsmPrimitive> primitives) {
        relations = Collections.<Relation>emptySet();
        if (primitives != null && !primitives.isEmpty()) {
            relations = new SubclassFilteredCollection<>(primitives,
                r -> r instanceof Relation && r.hasTag("type", Arrays.asList("route", "superroute")));
        }
        updateEnabledState();
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(!relations.isEmpty());
    }
}
