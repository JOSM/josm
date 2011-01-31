// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.awt.Color;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;
import org.openstreetmap.josm.tools.Utils;

public class AreaElemStyle extends ElemStyle
{
    public Color color;

    protected AreaElemStyle(Cascade c, Color color) {
        super(c);
        this.color = color;
    }

    public static AreaElemStyle create(Cascade c) {
        Color color = c.get("fill-color", null, Color.class);
        if (color == null)
            return null;
        return new AreaElemStyle(c, color);
    }

    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings paintSettings, MapPainter painter, boolean selected, boolean member) {
        if (primitive instanceof Way) {
            Way w = (Way) primitive;
            String name = painter.isShowNames() ? painter.getAreaName(w) : null;
            painter.drawArea(w, w.isSelected() ? paintSettings.getSelectedColor() : color, name);
        } else if (primitive instanceof Relation) {
            painter.drawArea((Relation) primitive, selected ? paintSettings.getRelationSelectedColor() : color, painter.getAreaName(primitive));
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        if (!super.equals(obj))
            return false;
        return Utils.equal(color, ((AreaElemStyle) obj).color);
    }

    @Override
    public int hashCode() {
        return 11 * super.hashCode() + color.hashCode();
    }

    @Override
    public String toString() {
        return "AreaElemStyle{" + super.toString() + "color=" + color + '}';
    }
}
