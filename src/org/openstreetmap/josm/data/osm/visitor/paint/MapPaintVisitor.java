// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.AbstractVisitor;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon.PolyData;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.mappaint.AreaElemStyle;
import org.openstreetmap.josm.gui.mappaint.ElemStyle;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.IconElemStyle;
import org.openstreetmap.josm.gui.mappaint.LineElemStyle;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.StyleCache;

public class MapPaintVisitor implements PaintVisitor {

    private Graphics2D g;
    private NavigatableComponent nc;

    private boolean zoomLevelDisplay;
    private boolean drawMultipolygon;
    private boolean drawRestriction;
    private boolean leftHandTraffic;
    private ElemStyles styles;
    private double circum;
    private double dist;
    private static int paintid = 0;
    private EastNorth minEN;
    private EastNorth maxEN;
    private MapPainter painter;
    private MapPaintSettings paintSettings;

    private boolean inactive;

    protected boolean isZoomOk(ElemStyle e) {
        if (!zoomLevelDisplay) /* show everything if the user wishes so */
            return true;

        if(e == null) /* the default for things that don't have a rule (show, if scale is smaller than 1500m) */
            return (circum < 1500);

        return !(circum >= e.maxScale || circum < e.minScale);
    }

    public StyleCache getPrimitiveStyle(OsmPrimitive osm, boolean nodefault) {
        if(osm.mappaintStyle == null)
        {
            if(styles != null) {
                osm.mappaintStyle = styles.get(osm);
                if(osm instanceof Way) {
                    ((Way)osm).isMappaintArea = styles.isArea(osm);
                }
            }
            if (osm.mappaintStyle.equals(StyleCache.EMPTY_STYLECACHE)) {
                if(osm instanceof Node)
                    osm.mappaintStyle = StyleCache.SIMPLE_NODE_STYLECACHE;
                else if (osm instanceof Way)
                    osm.mappaintStyle = StyleCache.UNTAGGED_WAY_STYLECACHE;
            }
        }
        if (nodefault && osm.mappaintStyle.equals(StyleCache.UNTAGGED_WAY_STYLECACHE))
            return StyleCache.EMPTY_STYLECACHE;
        return osm.mappaintStyle;
    }

    public IconElemStyle getPrimitiveNodeStyle(OsmPrimitive osm) {
        if(osm.mappaintStyle == null && styles != null) {
            IconElemStyle icon = styles.getIcon(osm);
            osm.mappaintStyle = StyleCache.create(icon);
            return icon;
        }
        for (ElemStyle s : osm.mappaintStyle.getStyles()) {
            if (s instanceof IconElemStyle)
                return (IconElemStyle) s;
        }
        return null;
    }

    public boolean isPrimitiveArea(Way osm) {
        if(osm.mappaintStyle == null && styles != null) {
            osm.mappaintStyle = styles.get(osm);
            osm.isMappaintArea = styles.isArea(osm);
        }
        return osm.isMappaintArea;
    }

    public void drawNode(Node n) {
        /* check, if the node is visible at all */
        EastNorth en = n.getEastNorth();
        if((en.east()  > maxEN.east() ) ||
                (en.north() > maxEN.north()) ||
                (en.east()  < minEN.east() ) ||
                (en.north() < minEN.north()))
            return;

        StyleCache sc = getPrimitiveStyle(n, false);

        for (ElemStyle s : sc.getStyles()) {
            if (isZoomOk(s)) {
                s.paintPrimitive(n, paintSettings, painter, data.isSelected(n), false);
            }

        }
    }

    public void drawWay(Way w, int fillAreas) {
        if(w.getNodesCount() < 2)
            return;

        if (w.hasIncompleteNodes())
            return;

        /* check, if the way is visible at all */
        double minx = 10000;
        double maxx = -10000;
        double miny = 10000;
        double maxy = -10000;

        for (Node n : w.getNodes())
        {
            if(n.getEastNorth().east() > maxx) {
                maxx = n.getEastNorth().east();
            }
            if(n.getEastNorth().north() > maxy) {
                maxy = n.getEastNorth().north();
            }
            if(n.getEastNorth().east() < minx) {
                minx = n.getEastNorth().east();
            }
            if(n.getEastNorth().north() < miny) {
                miny = n.getEastNorth().north();
            }
        }

        if ((minx > maxEN.east()) ||
                (miny > maxEN.north()) ||
                (maxx < minEN.east()) ||
                (maxy < minEN.north()))
            return;

        StyleCache sc = getPrimitiveStyle(w, false);
        for (ElemStyle s : sc.getStyles()) {
            if(!isZoomOk(s))
                return;
            if (fillAreas > dist || !(s instanceof AreaElemStyle)) {
                s.paintPrimitive(w, paintSettings, painter, data.isSelected(w), false);
            }
        }
    }

    public void paintUnselectedRelation(Relation r) {
        if (drawMultipolygon && "multipolygon".equals(r.get("type")))
            drawMultipolygon(r);
        else if (drawRestriction && "restriction".equals(r.get("type"))) {
            IconElemStyle nodeStyle = getPrimitiveNodeStyle(r);
            if (nodeStyle != null) {
                painter.drawRestriction(r, leftHandTraffic, nodeStyle);
            }
        }
    }

    public boolean drawMultipolygon(Relation r) {
        boolean drawn = false;

        Multipolygon multipolygon = new Multipolygon(nc);
        multipolygon.load(r);

        AreaElemStyle areaStyle = null;
        LineElemStyle lineStyle = null;
        for (ElemStyle s : getPrimitiveStyle(r, false).getStyles()) {
            if (s instanceof AreaElemStyle) {
                areaStyle = (AreaElemStyle) s;
            } else if (s instanceof LineElemStyle) {
                lineStyle = (LineElemStyle) s;
            }
        }

        boolean disabled = r.isDisabled();
        // If area style was not found for relation then use style of ways
        if(styles != null && areaStyle == null) {
            for (Way w : multipolygon.getOuterWays()) {
                for (ElemStyle s : styles.getArea(w).getStyles()) {
                    if (s instanceof AreaElemStyle) {
                        areaStyle = (AreaElemStyle) s;
                    } else if (s instanceof LineElemStyle) {
                        lineStyle = (LineElemStyle) s;
                    }
                }
                disabled = disabled || w.isDisabled();
                if(areaStyle != null) {
                    break;
                }
            }
        }

        if (areaStyle != null) {
            boolean zoomok = isZoomOk(areaStyle);
            boolean visible = false;

            drawn = true;

            if(zoomok && !disabled && !multipolygon.getOuterWays().isEmpty()) {
                for (PolyData pd : multipolygon.getCombinedPolygons()) {
                    Polygon p = pd.get();
                    if(!isPolygonVisible(p)) {
                        continue;
                    }

                    boolean selected = pd.selected || data.isSelected(r);
                    painter.drawArea(p, selected ? paintSettings.getRelationSelectedColor()
                                : areaStyle.color, painter.getAreaName(r));
                    visible = true;
                }
            }

            if(!visible)
                return drawn;
            for (Way wInner : multipolygon.getInnerWays()) {
                StyleCache inner = getPrimitiveStyle(wInner, true);
                AreaElemStyle innerArea = null;
                for (ElemStyle s : inner.getStyles()) {
                    if (s instanceof AreaElemStyle) {
                        innerArea = (AreaElemStyle) s;
                        break;
                    }
                }

                if(inner.getStyles().isEmpty()) {
                    if (data.isSelected(wInner) || disabled)
                        continue;
                    if(zoomok && (wInner.mappaintDrawnCode != paintid || multipolygon.getOuterWays().isEmpty())) {
                        lineStyle.paintPrimitive(wInner, paintSettings,
                                painter, (data.isSelected(wInner) || data.isSelected(r)), false);
                    }
                    wInner.mappaintDrawnCode = paintid;
                }
                else {
                    if(areaStyle.equals(innerArea)) {
                        wInner.mappaintDrawnAreaCode = paintid;
                        
                        if(!data.isSelected(wInner)) {
                            wInner.mappaintDrawnCode = paintid;
                            drawWay(wInner, 0);
                        }
                    }
                }
            }
            for (Way wOuter : multipolygon.getOuterWays()) {
                StyleCache outer = getPrimitiveStyle(wOuter, true);
                boolean hasOuterArea = false;
                for (ElemStyle s : outer.getStyles()) {
                    if (s instanceof AreaElemStyle) {
                        hasOuterArea = true;
                        break;
                    }
                }

                if (outer.getStyles().isEmpty()) {
                    // Selected ways are drawn at the very end
                    if (data.isSelected(wOuter))
                        continue;
                    if(zoomok) {
                        lineStyle.paintPrimitive(wOuter, paintSettings, painter,
                            (data.isSelected(wOuter) || data.isSelected(r)), r.isSelected());
                    }
                    wOuter.mappaintDrawnCode = paintid;
                } else if (hasOuterArea) {
                    wOuter.mappaintDrawnAreaCode = paintid;
                    if(!data.isSelected(wOuter)) {
                        wOuter.mappaintDrawnCode = paintid;
                        drawWay(wOuter, 0);
                    }
                }
            }
        }
        return drawn;
    }

    protected boolean isPolygonVisible(Polygon polygon) {
        Rectangle bounds = polygon.getBounds();
        if (bounds.width == 0 && bounds.height == 0) return false;
        if (bounds.x > nc.getWidth()) return false;
        if (bounds.y > nc.getHeight()) return false;
        if (bounds.x + bounds.width < 0) return false;
        if (bounds.y + bounds.height < 0) return false;
        return true;
    }

    protected Point2D getCentroid(Polygon p)
    {
        double cx = 0.0, cy = 0.0, a = 0.0;

        // usually requires points[0] == points[npoints] and can then use i+1 instead of j.
        // Faked it slowly using j.  If this is really gets used, this should be fixed.
        for (int i = 0;  i < p.npoints;  i++) {
            int j = i+1 == p.npoints ? 0 : i+1;
            a += (p.xpoints[i] * p.ypoints[j]) - (p.ypoints[i] * p.xpoints[j]);
            cx += (p.xpoints[i] + p.xpoints[j]) * (p.xpoints[i] * p.ypoints[j] - p.ypoints[i] * p.xpoints[j]);
            cy += (p.ypoints[i] + p.ypoints[j]) * (p.xpoints[i] * p.ypoints[j] - p.ypoints[i] * p.xpoints[j]);
        }
        return new Point2D.Double(cx / (3.0*a), cy / (3.0*a));
    }

    protected double getArea(Polygon p)
    {
        double sum = 0.0;

        // usually requires points[0] == points[npoints] and can then use i+1 instead of j.
        // Faked it slowly using j.  If this is really gets used, this should be fixed.
        for (int i = 0;  i < p.npoints;  i++) {
            int j = i+1 == p.npoints ? 0 : i+1;
            sum = sum + (p.xpoints[i] * p.ypoints[j]) - (p.ypoints[i] * p.xpoints[j]);
        }
        return Math.abs(sum/2.0);
    }

    DataSet data;

    <T extends OsmPrimitive> Collection<T> selectedLast(final DataSet data, Collection <T> prims) {
        ArrayList<T> sorted = new ArrayList<T>(prims);
        Collections.sort(sorted,
                new Comparator<T>() {
            public int compare(T o1, T o2) {
                boolean s1 = data.isSelected(o1);
                boolean s2 = data.isSelected(o2);
                if (s1 && !s2)
                    return 1;
                if (!s1 && s2)
                    return -1;
                return o1.compareTo(o2);
            }
        });
        return sorted;
    }

    /* Shows areas before non-areas */
    public void visitAll(final DataSet data, boolean virtual, Bounds bounds) {
        //long start = System.currentTimeMillis();
        BBox bbox = new BBox(bounds);
        this.data = data;
        ++paintid;

        int fillAreas = Main.pref.getInteger("mappaint.fillareas", 10000000);
        LatLon ll1 = nc.getLatLon(0, 0);
        LatLon ll2 = nc.getLatLon(100, 0);
        dist = ll1.greatCircleDistance(ll2);

        zoomLevelDisplay = Main.pref.getBoolean("mappaint.zoomLevelDisplay", false);
        circum = nc.getDist100Pixel();
        styles = MapPaintStyles.getStyles();
        drawMultipolygon = Main.pref.getBoolean("mappaint.multipolygon", true);
        drawRestriction = Main.pref.getBoolean("mappaint.restriction", true);
        leftHandTraffic = Main.pref.getBoolean("mappaint.lefthandtraffic", false);
        minEN = nc.getEastNorth(0, nc.getHeight() - 1);
        maxEN = nc.getEastNorth(nc.getWidth() - 1, 0);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                Main.pref.getBoolean("mappaint.use-antialiasing", false) ?
                        RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        this.paintSettings = MapPaintSettings.INSTANCE;
        this.painter = new MapPainter(paintSettings, g, inactive, nc, virtual, dist, circum);

        if (fillAreas > dist && styles != null && styles.hasAreas()) {
            Collection<Way> noAreaWays = new LinkedList<Way>();
            final Collection<Way> ways = data.searchWays(bbox);

            /*** disabled ***/
            for (final Way osm : ways) {
                if (osm.isDisabled() && osm.isDrawable() && osm.mappaintDrawnCode != paintid) {
                    drawWay(osm, 0);
                    osm.mappaintDrawnCode = paintid;
                }
            }

            /*** RELATIONS ***/
            for (final Relation osm: data.searchRelations(bbox)) {
                if (osm.isDrawable()) {
                    paintUnselectedRelation(osm);
                }
            }

            /*** AREAS ***/
            for (final Way osm : selectedLast(data, ways)) {
                if (osm.isDrawable() && osm.mappaintDrawnCode != paintid) {
                    if (isPrimitiveArea(osm)) {
                        if(osm.mappaintDrawnAreaCode != paintid)
                            drawWay(osm, fillAreas);
                    } else if(!data.isSelected(osm)) {
                        noAreaWays.add(osm);
                    }
                }
            }

            /*** WAYS ***/
            for (final Way osm : noAreaWays) {
                drawWay(osm, 0);
                osm.mappaintDrawnCode = paintid;
            }
        } else {
            drawMultipolygon = false;
            final Collection<Way> ways = data.searchWays(bbox);

            /*** WAYS (disabled)  ***/
            for (final Way way: ways) {
                if (way.isDisabled() && way.isDrawable() && !data.isSelected(way)) {
                    drawWay(way, 0);
                    way.mappaintDrawnCode = paintid;
                }
            }

            /*** RELATIONS ***/
            for (final Relation osm: data.searchRelations(bbox)) {
                if (osm.isDrawable()) {
                    paintUnselectedRelation(osm);
                }
            }

            /*** WAYS (filling disabled)  ***/
            for (final Way way: ways) {
                if (way.isDrawable() && !data.isSelected(way)) {
                    drawWay(way, 0);
                }
            }
        }

        /*** SELECTED  ***/
        for (final OsmPrimitive osm : data.getSelected()) {
            if (osm.isUsable() && !(osm instanceof Node) && (osm instanceof Relation || osm.mappaintDrawnCode != paintid)) {
                osm.visit(new AbstractVisitor() {
                    public void visit(Way w) {
                        drawWay(w, 0);
                    }

                    public void visit(Node n) {
                        // Selected nodes are painted in following part
                    }

                    public void visit(Relation r) {
                        for (RelationMember m : r.getMembers()) {
                            OsmPrimitive osm = m.getMember();
                            if(osm.isDrawable()) {
                                StyleCache sc = getPrimitiveStyle(m.getMember(), false);
                                if(osm instanceof Way)
                                {
                                    for (ElemStyle s : sc.getStyles()) {
                                        if (!(s instanceof AreaElemStyle)) {
                                            s.paintPrimitive(osm, paintSettings, painter, data.isSelected(osm), true);
                                        }
                                    }
                                }
                                else if(osm instanceof Node)
                                {
                                    for (ElemStyle s : sc.getStyles()) {
                                        if (isZoomOk(s)) {
                                            s.paintPrimitive(osm, paintSettings, painter, data.isSelected(osm), true);
                                        }
                                    }
                                }
                                osm.mappaintDrawnCode = paintid;
                            }
                        }
                    }
                });
            }
        }

        /*** NODES ***/
        for (final Node osm: data.searchNodes(bbox)) {
            if (!osm.isIncomplete() && !osm.isDeleted() && (data.isSelected(osm) || !osm.isDisabledAndHidden())
                    && osm.mappaintDrawnCode != paintid) {
                drawNode(osm);
            }
        }

        painter.drawVirtualNodes(data.searchWays(bbox));
        //System.err.println("PAINTING TOOK "+(System.currentTimeMillis() - start));
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
