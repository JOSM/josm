package org.openstreetmap.josm.gui.mappaint;
import java.awt.Color;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;

public class AreaElemStyle extends ElemStyle
{
    public Color color;
    public boolean closed;
    private LineElemStyle line;

    public AreaElemStyle (AreaElemStyle a, long maxScale, long minScale) {
        this.color = a.color;
        this.closed = a.closed;
        this.priority = a.priority;
        this.maxScale = maxScale;
        this.minScale = minScale;
        this.rules = a.rules;
        this.line = new LineElemStyle();
        this.line.color = a.color;
    }

    public AreaElemStyle(AreaElemStyle a, LineElemStyle l)
    {
        this.color = a.color;
        this.closed = a.closed;
        this.priority = a.priority;
        this.maxScale = a.maxScale;
        this.minScale = a.minScale;
        this.rules = a.rules;
        this.line = l;
        this.code = a.code;
    }

    public AreaElemStyle() { init(); }

    public void init()
    {
        closed = false;
        color = null;
        priority = 0;
    }

    public ElemStyle getLineStyle() {
        return line;
    }

    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings paintSettings, MapPainter painter, boolean selected, boolean member) {
        // TODO
        /*Way way = (Way)primitive;
        String name = painter.isShowNames() ? painter.getWayName(way) : null;
        painter.drawArea(getPolygon(way), selected ? paintSettings.getSelectedColor() : color, name);
        line.paintPrimitive(way, paintSettings, painter, selected);*/
    }
}
