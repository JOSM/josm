// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.xml;

import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.IconReference;
import org.openstreetmap.josm.gui.mappaint.Range;

public class IconPrototype extends Prototype {

    public IconReference icon;
    public Boolean annotate;

    public IconPrototype (IconPrototype i, Range range) {
        super(range);
        this.icon = i.icon;
        this.annotate = i.annotate;
        this.priority = i.priority;
        this.conditions = i.conditions;
    }

    public IconPrototype() { init(); }

    public void init() {
        priority = 0;
        range = Range.ZERO_TO_INFINITY;
        icon = null;
        annotate = null;
    }
}
