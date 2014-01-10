// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.xml;

import java.awt.Color;

import org.openstreetmap.josm.gui.mappaint.Range;

public class AreaPrototype extends Prototype {
    public Color color;
    public boolean closed; // if true, it does not apply to unclosed ways

    public AreaPrototype (AreaPrototype a, Range range) {
        super(range);
        this.color = a.color;
        this.closed = a.closed;
        this.priority = a.priority;
        this.conditions = a.conditions;
    }

    public AreaPrototype() { init(); }

    public void init()
    {
        priority = 0;
        range = Range.ZERO_TO_INFINITY;
        closed = false;
        color = null;
    }
}
