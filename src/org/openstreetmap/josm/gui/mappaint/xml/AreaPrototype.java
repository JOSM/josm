// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.xml;

import java.awt.Color;

import org.openstreetmap.josm.gui.mappaint.AreaElemStyle;

public class AreaPrototype extends Prototype {
    public Color color;
    public boolean closed;

    public AreaPrototype (AreaPrototype a, long maxScale, long minScale) {
        super(maxScale, minScale);
        this.color = a.color;
        this.closed = a.closed;
        this.priority = a.priority;
        this.conditions = a.conditions;
    }

    public AreaPrototype() { init(); }

    public void init()
    {
        closed = false;
        color = null;
        priority = 0;
    }

    public AreaElemStyle createStyle() {
        return new AreaElemStyle(minScale, maxScale, color);
    }
}
