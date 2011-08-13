// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Collections;
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

/**
 * <p>A map renderer which renders a map according to style rules in a set of style sheets.</p>
 * 
 */
public class StyledMapRenderer extends AbstractMapRenderer{

    private ElemStyles styles;
    private double circum;
    private MapPainter painter;
    private MapPaintSettings paintSettings;

    private static int FLAG_NORMAL = 0;
    private static int FLAG_DISABLED = 1;
    private static int FLAG_SELECTED = 2;
    private static int FLAG_MEMBER_OF_SELECTED = 4;

    private static class StyleRecord implements Comparable<StyleRecord> {
        final ElemStyle style;
        final OsmPrimitive osm;
        final int flags;

        public StyleRecord(ElemStyle style, OsmPrimitive osm, int flags) {
            this.style = style;
            this.osm = osm;
            this.flags = flags;
        }

        @Override
        public int compareTo(StyleRecord other) {
            if ((this.flags & FLAG_DISABLED) != 0 && (other.flags & FLAG_DISABLED) == 0)
                return -1;
            if ((this.flags & FLAG_DISABLED) == 0 && (other.flags & FLAG_DISABLED) != 0)
                return 1;
            float z_index1 = this.style.z_index;
            if ((this.flags & FLAG_SELECTED) != 0) {
                z_index1 += 700f;
            } else if ((this.flags & FLAG_MEMBER_OF_SELECTED) != 0) {
                z_index1 += 600f;
            }
            float z_index2 = other.style.z_index;
            if ((other.flags & FLAG_SELECTED) != 0) {
                z_index2 += 700f;
            } else if ((other.flags & FLAG_MEMBER_OF_SELECTED) != 0) {
                z_index2 += 600f;
            }

            int d1 = Float.compare(z_index1, z_index2);
            if (d1 != 0)
                return d1;

            // simple node on top of icons and shapes
            if (this.style == NodeElemStyle.SIMPLE_NODE_ELEMSTYLE && other.style != NodeElemStyle.SIMPLE_NODE_ELEMSTYLE)
                return 1;
            if (this.style != NodeElemStyle.SIMPLE_NODE_ELEMSTYLE && other.style == NodeElemStyle.SIMPLE_NODE_ELEMSTYLE)
                return -1;

            // newer primitives to the front
            long id = this.osm.getUniqueId() - other.osm.getUniqueId();
            if (id > 0)
                return 1;
            if (id < 0)
                return -1;

            return Float.compare(this.style.object_z_index, other.style.object_z_index);
        }
    }

    private class StyleCollector {
        private final boolean drawArea;
        private final boolean drawMultipolygon;
        private final boolean drawRestriction;

        private final List<StyleRecord> styleElems;

        public StyleCollector(boolean drawArea, boolean drawMultipolygon, boolean drawRestriction) {
            this.drawArea = drawArea;
            this.drawMultipolygon = drawMultipolygon;
            this.drawRestriction = drawRestriction;
            styleElems = new ArrayList<StyleRecord>();
        }

        public void add(Node osm, int flags) {
            StyleList sl = styles.get(osm, circum, nc);
            for (ElemStyle s : sl) {
                styleElems.add(new StyleRecord(s, osm, flags));
            }
        }

        public void add(Way osm, int flags) {
            StyleList sl = styles.get(osm, circum, nc);
            for (ElemStyle s : sl) {
                if (!(drawArea && (flags & FLAG_DISABLED) == 0) && s instanceof AreaElemStyle) {
                    continue;
                }
                styleElems.add(new StyleRecord(s, osm, flags));
            }
        }

        public void add(Relation osm, int flags) {
            StyleList sl = styles.get(osm, circum, nc);
            for (ElemStyle s : sl) {
                if (drawMultipolygon && drawArea && s instanceof AreaElemStyle && (flags & FLAG_DISABLED) == 0) {
                    styleElems.add(new StyleRecord(s, osm, flags));
                } else if (drawRestriction && s instanceof NodeElemStyle) {
                    styleElems.add(new StyleRecord(s, osm, flags));
                }
            }
        }

        public void drawAll() {
            Collections.sort(styleElems);
            for (StyleRecord r : styleElems) {
                r.style.paintPrimitive(
                        r.osm,
                        paintSettings,
                        painter,
                        (r.flags & FLAG_SELECTED) != 0,
                        (r.flags & FLAG_MEMBER_OF_SELECTED) != 0
                );
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public StyledMapRenderer(Graphics2D g, NavigatableComponent nc, boolean isInactiveMode) {
        super(g, nc, isInactiveMode);
    }

    private void collectNodeStyles(DataSet data, StyleCollector sc, BBox bbox) {
        for (final Node n: data.searchNodes(bbox)) {
            if (n.isDrawable()) {
                if (n.isDisabled()) {
                    sc.add(n, FLAG_DISABLED);
                } else if (data.isSelected(n)) {
                    sc.add(n, FLAG_SELECTED);
                } else if (n.isMemberOfSelected()) {
                    sc.add(n, FLAG_MEMBER_OF_SELECTED);
                } else {
                    sc.add(n, FLAG_NORMAL);
                }
            }
        }
    }

    private void collectWayStyles(DataSet data, StyleCollector sc, BBox bbox) {
        for (final Way w : data.searchWays(bbox)) {
            if (w.isDrawable()) {
                if (w.isDisabled()) {
                    sc.add(w, FLAG_DISABLED);
                } else if (data.isSelected(w)) {
                    sc.add(w, FLAG_SELECTED);
                } else if (w.isMemberOfSelected()) {
                    sc.add(w, FLAG_MEMBER_OF_SELECTED);
                } else {
                    sc.add(w, FLAG_NORMAL);
                }
            }
        }
    }

    private void collectRelationStyles(DataSet data, StyleCollector sc, BBox bbox) {
        for (Relation r: data.searchRelations(bbox)) {
            if (r.isDrawable()) {
                if (r.isDisabled()) {
                    sc.add(r, FLAG_DISABLED);
                } else if (data.isSelected(r)) {
                    sc.add(r, FLAG_SELECTED);
                } else {
                    sc.add(r, FLAG_NORMAL);
                }
            }
        }
    }

    @Override
    public void render(final DataSet data, boolean renderVirtualNodes, Bounds bounds) {
        long start = System.currentTimeMillis();
        BBox bbox = new BBox(bounds);

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

        this.painter = new MapPainter(paintSettings, g, isInactiveMode, nc, renderVirtualNodes, circum, leftHandTraffic);

        StyleCollector sc = new StyleCollector(drawArea, drawMultipolygon, drawRestriction);
        collectNodeStyles(data, sc, bbox);
        collectWayStyles(data, sc, bbox);
        collectRelationStyles(data, sc, bbox);
        long phase1 = System.currentTimeMillis();
        sc.drawAll();
        sc = null;
        painter.drawVirtualNodes(data.searchWays(bbox));

        long now = System.currentTimeMillis();
        System.err.println(String.format("PAINTING TOOK %d [PHASE1 took %d] (at scale %s)", now - start, phase1 - start, circum));
    }
}
