// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import java.awt.Cursor;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

class MapViewMock extends MapView {
    private final transient OsmDataLayer layer;
    private final transient DataSet currentDataSet;

    MapViewMock(DataSet dataSet, OsmDataLayer layer) {
        super(Main.getLayerManager(), null, null);
        this.layer = layer;
        this.currentDataSet = dataSet;
    }

    @Override
    public EastNorth getEastNorth(int x, int y) {
        return new EastNorth(x, y);
    }

    @Override
    public void addMouseListener(MouseListener ml) {}

    @Override
    public void removeMouseListener(MouseListener ml) {}

    @Override
    public void setVirtualNodesEnabled(boolean enabled) {}

    @Override
    public void setNewCursor(Cursor cursor, Object reference) {}

    @Override
    public void setNewCursor(int cursor, Object reference) {}

    @Override
    public boolean isActiveLayerVisible() {
        return true;
    }

    @Override
    public void requestClearRect() {}

    @Override
    public Point2D getPoint2D(EastNorth p) {
        return p != null ? new Point2D.Double(p.getX(), p.getY()) : null;
    }

    @Override
    public void setActiveLayer(Layer layer) {}

    @Override
    public Layer getActiveLayer() {
        return layer;
    }

    @Override
    protected DataSet getCurrentDataSet() {
        return currentDataSet;
    }
}
