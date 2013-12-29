// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.xml;

import java.awt.Color;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.gui.mappaint.Range;
import org.openstreetmap.josm.tools.I18n;

public class LinePrototype extends Prototype {

    protected int width;
    public Integer realWidth; // the real width of this line in meter
    public Color color;
    protected List<Float> dashed;
    public Color dashedColor;

    public LinePrototype(LinePrototype s, Range range) {
        super(range);
        this.width = s.width;
        this.realWidth = s.realWidth;
        this.color = s.color;
        this.dashed = s.dashed;
        this.dashedColor = s.dashedColor;
        this.priority = s.priority;
        this.conditions = s.conditions;
    }

    public LinePrototype() { init(); }

    public void init()
    {
        priority = 0;
        range = Range.ZERO_TO_INFINITY;
        width = -1;
        realWidth = null;
        dashed = null;
        dashedColor = null;
        color = PaintColors.UNTAGGED.get();
    }

    public List<Float> getDashed() {
        return dashed;
    }

    public void setDashed(List<Float> dashed) {
        if (dashed == null || dashed.isEmpty()) {
            this.dashed = null;
            return;
        }

        boolean found = false;
        for (Float f : dashed) {
            if (f == null) {
                this.dashed = null;
                return;
            }
            if (f > 0) {
                found = true;
            }
            if (f < 0) {
                Main.error(I18n.tr("Illegal dash pattern, values must be positive"));
                this.dashed = null;
                return;
            }
        }
        if (found) {
            this.dashed = dashed;
        } else {
            Main.error(I18n.tr("Illegal dash pattern, at least one value must be > 0"));
        }
    }

    public int getWidth() {
        if (width == -1)
            return MapPaintSettings.INSTANCE.getDefaultSegmentWidth();
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
}