// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.xml;

import org.openstreetmap.josm.gui.mappaint.IconElemStyle;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.IconReference;
import org.openstreetmap.josm.gui.mappaint.NodeElemStyle;
import org.openstreetmap.josm.gui.mappaint.SimpleNodeElemStyle;

public class IconPrototype extends Prototype {
    
    public IconReference icon;
    public boolean annotate;

    public IconPrototype (IconPrototype i, long maxScale, long minScale) {
        super(maxScale, minScale);
        this.icon = i.icon;
        this.annotate = i.annotate;
        this.priority = i.priority;
        this.conditions = i.conditions;
    }

    public IconPrototype() { init(); }

    public void init() {
        icon = null;
        priority = 0;
        annotate = true;
    }

    public NodeElemStyle createStyle() {
        if (icon == null) {
            return SimpleNodeElemStyle.INSTANCE;
        } else {
            IconElemStyle i = new IconElemStyle(minScale, maxScale, MapPaintStyles.getIcon(icon));
            i.annotate = annotate;
            return i;
        }
    }
}
