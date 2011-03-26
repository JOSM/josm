// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.AreaElemStyle;
import org.openstreetmap.josm.gui.mappaint.ElemStyle;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.NodeElemStyle;
import org.openstreetmap.josm.gui.mappaint.StyleCache.StyleList;
import org.openstreetmap.josm.tools.Pair;

public class MapPaintVisitor implements PaintVisitor {

    private Graphics2D g;
    private boolean inactive;
    private NavigatableComponent nc;

    private ElemStyles styles;
    private double circum;
    private MapPainter painter;
    private MapPaintSettings paintSettings;
    private DataSet data;

    private class StyleCollector {
        private final boolean drawArea;
        private final boolean drawMultipolygon;
        private final boolean drawRestriction;
        private final boolean memberSelected;

        private final List<Pair<ElemStyle, OsmPrimitive>> styleElems;

        public StyleCollector(boolean drawArea, boolean drawMultipolygon, boolean drawRestriction, boolean memberSelected) {
            this.drawArea = drawArea;
            this.drawMultipolygon = drawMultipolygon;
            this.drawRestriction = drawRestriction;
            this.memberSelected = memberSelected;
            styleElems = new ArrayList<Pair<ElemStyle, OsmPrimitive>>();
        }

        public void add(Node osm) {
            StyleList sl = styles.get(osm, circum, nc);
            for (ElemStyle s : sl) {
                styleElems.add(new Pair<ElemStyle, OsmPrimitive>(s, osm));
            }
        }

        public void add(Way osm) {
            StyleList sl = styles.get(osm, circum, nc);
            for (ElemStyle s : sl) {
                if (!drawArea && s instanceof AreaElemStyle) {
                    continue;
                }
                styleElems.add(new Pair<ElemStyle, OsmPrimitive>(s, osm));
            }
        }

        public void add(Relation osm) {
            StyleList sl = styles.get(osm, circum, nc);
            for (ElemStyle s : sl) {
                if (drawMultipolygon && drawArea && s instanceof AreaElemStyle) {
                    styleElems.add(new Pair<ElemStyle, OsmPrimitive>(s, osm));
                } else if (drawRestriction && s instanceof NodeElemStyle) {
                    styleElems.add(new Pair<ElemStyle, OsmPrimitive>(s, osm));
                }
            }
        }

        public void drawAll() {
            Collections.sort(styleElems, STYLE_COMPARATOR);
            for (Pair<ElemStyle, OsmPrimitive> p : styleElems) {
                p.a.paintPrimitive(p.b, paintSettings, painter, data.isSelected(p.b), memberSelected);
            }
        }
    }

    private final static Comparator<Pair<ElemStyle, OsmPrimitive>> STYLE_COMPARATOR = new Comparator<Pair<ElemStyle, OsmPrimitive>>() {
        @Override
        public int compare(Pair<ElemStyle, OsmPrimitive> p1, Pair<ElemStyle, OsmPrimitive> p2) {
            int d1 = Float.compare(p1.a.z_index, p2.a.z_index);
            if (d1 != 0)
                return d1;
            if (p1.a == NodeElemStyle.SIMPLE_NODE_ELEMSTYLE && p2.a != NodeElemStyle.SIMPLE_NODE_ELEMSTYLE)
                return 1;
            if (p1.a != NodeElemStyle.SIMPLE_NODE_ELEMSTYLE && p2.a == NodeElemStyle.SIMPLE_NODE_ELEMSTYLE)
                return -1;
            // newer primitives to the front
            long id = p1.b.getUniqueId() - p2.b.getUniqueId();
            if (id > 0)
                return 1;
            if (id < 0)
                return -1;
            return Float.compare(p1.a.object_z_index, p2.a.object_z_index);
        }
    };

    public void visitAll(final DataSet data, boolean virtual, Bounds bounds) {
        //long start = System.currentTimeMillis();
        BBox bbox = new BBox(bounds);
        this.data = data;

        styles = MapPaintStyles.getStyles();

        this.paintSettings = MapPaintSettings.INSTANCE;

        circum = nc.getDist100Pixel();
        boolean drawArea = circum <= Main.pref.getInteger("mappaint.fillareas", 10000000);
        boolean drawMultipolygon = drawArea && Main.pref.getBoolean("mappaint.multipolygon", true);
        styles.setDrawMultipolygon(drawMultipolygon);
        boolean drawRestriction = Main.pref.getBoolean("mappaint.restriction", true);
        boolean leftHandTraffic = Main.pref.getBoolean("mappaint.lefthandtraffic", false);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                Main.pref.getBoolean("mappaint.use-antialiasing", true) ?
                        RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        this.painter = new MapPainter(paintSettings, g, inactive, nc, virtual, circum, leftHandTraffic);

        StyleCollector scDisabledPrimitives = new StyleCollector(false, false, drawRestriction, false);
        StyleCollector scSelectedPrimitives = new StyleCollector(drawArea, drawMultipolygon, drawRestriction, false);
        StyleCollector scMemberPrimitives = new StyleCollector(drawArea, drawMultipolygon, drawRestriction, true);
        StyleCollector scNormalPrimitives = new StyleCollector(drawArea, drawMultipolygon, drawRestriction, false);

        for (final Node n: data.searchNodes(bbox)) {
            if (n.isDrawable()) {
                if (n.isDisabled()) {
                    scDisabledPrimitives.add(n);
                } else if (n.isSelected()) {
                    scSelectedPrimitives.add(n);
                } else if (n.isMemberOfSelected()) {
                    scMemberPrimitives.add(n);
                } else {
                    scNormalPrimitives.add(n);
                }
            }
        }
        for (final Way w : data.searchWays(bbox)) {
            if (w.isDrawable()) {
                if (w.isDisabled()) {
                    scDisabledPrimitives.add(w);
                } else if (w.isSelected()) {
                    scSelectedPrimitives.add(w);
                } else if (w.isMemberOfSelected()) {
                    scMemberPrimitives.add(w);
                } else {
                    scNormalPrimitives.add(w);
                }
            }
        }
        for (Relation r: data.searchRelations(bbox)) {
            if (r.isDrawable()) {
                if (r.isDisabled()) {
                    scDisabledPrimitives.add(r);
                } else if (r.isSelected()) {
                    scSelectedPrimitives.add(r);
                } else {
                    scNormalPrimitives.add(r);
                }
            }
        }

        //long phase1 = System.currentTimeMillis();

        scDisabledPrimitives.drawAll();
        scDisabledPrimitives = null;
        scNormalPrimitives.drawAll();
        scNormalPrimitives = null;
        scMemberPrimitives.drawAll();
        scMemberPrimitives = null;
        scSelectedPrimitives.drawAll();
        scSelectedPrimitives = null;

        painter.drawVirtualNodes(data.searchWays(bbox));
        
        //long now = System.currentTimeMillis();
        //System.err.println(String.format("PAINTING TOOK %d [PHASE1 took %d] (at scale %s)", now - start, phase1 - start, circum));
    }

    public void setGraphics(Graphics2D g) {
        this.g = g;
    }

    public void setInactive(boolean inactive) {
        this.inactive = inactive;
    }

    public void setNavigatableComponent(NavigatableComponent nc) {
        this.nc = nc;
    }
}
