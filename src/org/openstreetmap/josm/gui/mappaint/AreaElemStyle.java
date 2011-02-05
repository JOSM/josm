// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.awt.Color;

import org.openstreetmap.josm.Main;
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
        int alpha = Math.min(255, Math.max(0, Integer.valueOf(Main.pref.getInteger("mappaint.fillalpha", 50))));
        Integer pAlpha = color_float2int(c.get("fill-opacity", null, float.class));
        if (pAlpha != null) {
            alpha = pAlpha;
        }
        color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        return new AreaElemStyle(c, color);
    }

    @Override
    public void paintPrimitive(OsmPrimitive osm, MapPaintSettings paintSettings, MapPainter painter, boolean selected, boolean member) {
        if (osm instanceof Way)
        {
            painter.drawArea((Way) osm,
                    osm.isSelected() ? paintSettings.getSelectedColor(color.getAlpha()) : color,
                    painter.isShowNames() ? painter.getAreaName(osm) : null);
        } else if (osm instanceof Relation)
        {
            painter.drawArea((Relation) osm,
                    selected ? paintSettings.getRelationSelectedColor(color.getAlpha()) : color,
                    painter.getAreaName(osm));
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
        return "AreaElemStyle{" + super.toString() + "color=" + Utils.toString(color) + '}';
    }
}
