// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;

public class SimpleNodeElemStyle extends ElemStyle {

    public static final SimpleNodeElemStyle INSTANCE = new SimpleNodeElemStyle();

    private SimpleNodeElemStyle() {
        minScale = 0;
        maxScale = 1500;
    }

    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings settings, MapPainter painter,
            boolean selected) {
        Node n = (Node)primitive;
        String name = painter.isShowNames()?painter.getNodeName(n):null;
        if (n.highlighted) {
            painter.drawNode(n, settings.getHighlightColor(), settings.getSelectedNodeSize(), settings.isFillSelectedNode(), name);
        } else if (selected) {
            painter.drawNode(n, settings.getSelectedColor(), settings.getSelectedNodeSize(), settings.isFillSelectedNode(), name);
        } else if (n.isTagged()) {
            painter.drawNode(n, settings.getNodeColor(), settings.getTaggedNodeSize(), settings.isFillUnselectedNode(), name);
        } else if (painter.isInactive() || n.isDisabled()) {
            painter.drawNode(n, settings.getInactiveColor(), settings.getUnselectedNodeSize(), settings.isFillUnselectedNode(), name);
        } else {
            painter.drawNode(n, settings.getNodeColor(), settings.getUnselectedNodeSize(), settings.isFillUnselectedNode(), name);
        }
    }

}
