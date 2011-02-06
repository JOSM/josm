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
import org.openstreetmap.josm.gui.mappaint.LineElemStyle;
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
        private List<Pair<ElemStyle, OsmPrimitive>> styleElems;
        protected boolean memberSelected = false;
        private Class klass;

        public StyleCollector(Class<?> klass) {
            styleElems = new ArrayList<Pair<ElemStyle, OsmPrimitive>>();
            this.klass = klass;
        }

        public void add(OsmPrimitive osm) {
            StyleList sl = styles.get(osm, circum, nc);
            for (ElemStyle s : sl) {
                if (klass.isInstance(s)) {
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

        public boolean isMemberSelected() {
            return memberSelected;
        }

        public void setMemberSelected(boolean memberSelected) {
            this.memberSelected = memberSelected;
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
        boolean drawArea = circum <= Main.pref.getInteger("mappaint.fillareas", 10000000) && !paintSettings.isOutlineOnly();
        boolean drawMultipolygon = drawArea && Main.pref.getBoolean("mappaint.multipolygon", true);
        styles.setDrawMultipolygon(drawMultipolygon);
        boolean drawRestriction = Main.pref.getBoolean("mappaint.restriction", true);
        boolean leftHandTraffic = Main.pref.getBoolean("mappaint.lefthandtraffic", false);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                Main.pref.getBoolean("mappaint.use-antialiasing", false) ?
                        RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        this.painter = new MapPainter(paintSettings, g, inactive, nc, virtual, circum, leftHandTraffic);

        StyleCollector scDisabledLines = new StyleCollector(LineElemStyle.class);
        StyleCollector scSelectedLines = new StyleCollector(LineElemStyle.class);
        StyleCollector scSelectedAreas = new StyleCollector(AreaElemStyle.class);
        StyleCollector scMemberLines = new StyleCollector(LineElemStyle.class);
        scMemberLines.setMemberSelected(true);
        StyleCollector scNormalAreas = new StyleCollector(AreaElemStyle.class);
        StyleCollector scNormalLines = new StyleCollector(LineElemStyle.class);
        for (final Way w : data.searchWays(bbox)) {
            if (w.isDrawable()) {
                if (w.isDisabled()) {
                    scDisabledLines.add(w);
                } else if (w.isSelected()) {
                    scSelectedLines.add(w);
                    if (drawArea) {
                        scSelectedAreas.add(w);
                    }
                } else if (w.isMemberOfSelected()) {
                    scMemberLines.add(w);
                    if (drawArea) {
                        scNormalAreas.add(w);
                    }
                } else {
                    scNormalLines.add(w);
                    if (drawArea) {
                        scNormalAreas.add(w);
                    }
                }
            }
        }
        scDisabledLines.drawAll();
        scDisabledLines = null;

        StyleCollector scDisabledNodes = new StyleCollector(NodeElemStyle.class);
        StyleCollector scSelectedNodes = new StyleCollector(NodeElemStyle.class);
        StyleCollector scMemberNodes = new StyleCollector(NodeElemStyle.class);
        scMemberNodes.setMemberSelected(true);
        StyleCollector scNormalNodes = new StyleCollector(NodeElemStyle.class);
        for (final Node n: data.searchNodes(bbox)) {
            if (n.isDrawable()) {
                if (n.isDisabled()) {
                    scDisabledNodes.add(n);
                } else if (n.isSelected()) {
                    scSelectedNodes.add(n);
                } else if (n.isMemberOfSelected()) {
                    scMemberNodes.add(n);
                } else {
                    scNormalNodes.add(n);
                }
            }
        }
        scDisabledNodes.drawAll();
        scDisabledNodes = null;

        StyleCollector scDisabledRestrictions = new StyleCollector(NodeElemStyle.class);
        StyleCollector scNormalRestrictions = new StyleCollector(NodeElemStyle.class);
        StyleCollector scSelectedRestrictions = new StyleCollector(NodeElemStyle.class);
        for (Relation r: data.searchRelations(bbox)) {
            if (r.isDrawable()) {
                if (r.isDisabled()) {
                    if (drawRestriction) {
                        scDisabledRestrictions.add(r);
                    }
                } else if (r.isSelected()) {
                    if (drawMultipolygon) {
                        scSelectedAreas.add(r);
                    }
                    if (drawRestriction) {
                        scSelectedRestrictions.add(r);
                    }
                } else {
                    if (drawMultipolygon) {
                        scNormalAreas.add(r);
                    }
                    if (drawRestriction) {
                        scNormalRestrictions.add(r);
                    }
                }
            }
        }
        scDisabledRestrictions.drawAll();
        scDisabledRestrictions = null;

        scNormalAreas.drawAll();
        scSelectedAreas.drawAll();
        scNormalLines.drawAll();
        scMemberLines.drawAll();
        scSelectedLines.drawAll();
        scNormalNodes.drawAll();
        scNormalRestrictions.drawAll();
        scMemberNodes.drawAll();
        scSelectedRestrictions.drawAll();
        scSelectedNodes.drawAll();

        painter.drawVirtualNodes(data.searchWays(bbox));
        //System.err.println("PAINTING TOOK "+(System.currentTimeMillis() - start)+ " (at scale "+circum+")");
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
