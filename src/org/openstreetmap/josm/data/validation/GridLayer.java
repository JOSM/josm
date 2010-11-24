// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;

import javax.swing.Action;
import javax.swing.Icon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.RenameLayerAction;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.AbstractVisitor;
import org.openstreetmap.josm.data.osm.visitor.BoundingXYVisitor;
import org.openstreetmap.josm.data.validation.util.ValUtil;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.dialogs.LayerListPopup;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.preferences.ValidatorPreference;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A debug layer for testing the grid cells a way crosses.
 *
 * @author frsantos
 */
public class GridLayer extends Layer
{
    /**
     * Constructor
     * @param name
     */
    public GridLayer(String name)
    {
        super(name);
    }

    /**
     * Return a static icon.
     */
    @Override public Icon getIcon() {
        return ImageProvider.get("layer", "validator");
    }

    /**
     * Draw the grid and highlight all cells acuppied by any selected primitive.
     */
    @Override
    public void paint(final Graphics2D g, final MapView mv, Bounds bounds)
    {
        if( !Main.pref.hasKey(ValidatorPreference.PREF_DEBUG + ".grid") )
            return;

        int gridWidth = Integer.parseInt(Main.pref.get(ValidatorPreference.PREF_DEBUG + ".grid") );
        int width = mv.getWidth();
        int height = mv.getHeight();

        EastNorth origin = mv.getEastNorth(0, 0);
        EastNorth border = mv.getEastNorth(width, height);

        if( border.east() * gridWidth > 50 )
            return;

        g.setColor(Color.RED.darker().darker());
        HighlightCellVisitor visitor = new HighlightCellVisitor(g, mv, gridWidth);
        for(OsmPrimitive p : Main.main.getCurrentDataSet().getSelected() )
            p.visit(visitor);

        long x0 = (long)Math.floor(origin.east()  * gridWidth);
        long x1 = (long)Math.floor(border.east()  * gridWidth);
        long y0 = (long)Math.floor(origin.north() * gridWidth) + 1;
        long y1 = (long)Math.floor(border.north() * gridWidth) + 1;
        long aux;
        if( x0 > x1 ) { aux = x0; x0 = x1; x1 = aux; }
        if( y0 > y1 ) { aux = y0; y0 = y1; y1 = aux; }

        g.setColor(Color.RED.brighter().brighter());
        for( double x = x0; x <= x1; x++)
        {
            Point point = mv.getPoint( new EastNorth(x/gridWidth, 0));
            g.drawLine(point.x, 0, point.x, height);
        }

        for( double y = y0; y <= y1; y++)
        {
            Point point = mv.getPoint( new EastNorth(0, y/gridWidth));
            g.drawLine(0, point.y, width, point.y);
        }
    }

    @Override
    public String getToolTipText()
    {
        return null;
    }

    @Override
    public void mergeFrom(Layer from) {}

    @Override
    public boolean isMergable(Layer other) {
        return false;
    }

    @Override
    public void visitBoundingBox(BoundingXYVisitor v) {}

    @Override
    public Object getInfoComponent()
    {
        return getToolTipText();
    }

    @Override
    public Action[] getMenuEntries()
    {
        return new Action[] {
                LayerListDialog.getInstance().createShowHideLayerAction(),
                LayerListDialog.getInstance().createDeleteLayerAction(),
                SeparatorLayerAction.INSTANCE,
                new RenameLayerAction(null, this),
                SeparatorLayerAction.INSTANCE,
                new LayerListPopup.InfoAction(this)};
    }

    @Override public void destroy() { }

    /**
     * Visitor that highlights all cells the selected primitives go through
     */
    static class HighlightCellVisitor extends AbstractVisitor
    {
        /** The MapView */
        private final MapView mv;
        /** The graphics */
        private final Graphics g;
        /** The grid width */
        private final int gridDetail;
        /** The width of a cell */
        private int cellWidth;

        /**
         * Constructor
         * @param g the graphics
         * @param mv The MapView
         * @param gridDetail The grid detail
         */
        public HighlightCellVisitor(final Graphics g, final MapView mv, int gridDetail)
        {
            this.g = g;
            this.mv = mv;
            this.gridDetail = gridDetail;

            Point p = mv.getPoint( new EastNorth(0, 0) );
            Point p2 = mv.getPoint( new EastNorth(1d/gridDetail, 1d/gridDetail) );
            cellWidth = Math.abs(p2.x - p.x);
        }

        public void visit(Node n)
        {
            double x = n.getEastNorth().east() * gridDetail;
            double y = n.getEastNorth().north()* gridDetail + 1;

            drawCell( Math.floor(x), Math.floor(y) );
        }

        public void visit(Way w)
        {
            Node lastN = null;
            for (Node n : w.getNodes()) {
                if (lastN == null) {
                    lastN = n;
                    continue;
                }
                for (Point2D p : ValUtil.getSegmentCells(lastN, n, gridDetail)) {
                    drawCell( p.getX(), p.getY() );
                }
                lastN = n;
            }
        }

        public void visit(Relation r) {}

        /**
         * Draws a solid cell at the (x,y) location
         * @param x
         * @param y
         */
        protected void drawCell(double x, double y)
        {
            Point p = mv.getPoint( new EastNorth(x/gridDetail, y/gridDetail) );
            g.fillRect(p.x, p.y, cellWidth, cellWidth);
        }
    }
}
