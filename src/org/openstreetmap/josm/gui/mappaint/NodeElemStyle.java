// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;

abstract public class NodeElemStyle extends ElemStyle {
    public boolean annotate;
    public String annotation_key;

    public NodeElemStyle(long minScale, long maxScale) {
        super(minScale, maxScale);
    }

    protected String getName(Node n, MapPainter painter) {
        if (painter.isShowNames() && annotate) {
            if (annotation_key != null) {
                return n.get(annotation_key);
            } else {
                return painter.getNodeName(n);
            }
        }
        return null;
    }
}
