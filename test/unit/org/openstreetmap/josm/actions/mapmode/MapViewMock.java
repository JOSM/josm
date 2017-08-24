// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import java.awt.Cursor;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.MainLayerManager;

class MapViewMock extends MapView {
    MapViewMock() {
        this(MainApplication.getLayerManager());
    }

    MapViewMock(MainLayerManager layerManager) {
        super(layerManager, null);
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
    public Point2D getPoint2D(EastNorth p) {
        return p != null ? new Point2D.Double(p.getX(), p.getY()) : null;
    }
}
