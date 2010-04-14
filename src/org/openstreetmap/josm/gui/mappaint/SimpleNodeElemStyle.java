// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.awt.Color;
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

    private static final int max(int a, int b, int c, int d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings settings, MapPainter painter,
            boolean selected) {
        Node n = (Node)primitive;
        String name = painter.isShowNames()?painter.getNodeName(n):null;


        if (n.isHighlighted()) {
            painter.drawNode(n, settings.getHighlightColor(), settings.getSelectedNodeSize(), settings.isFillSelectedNode(), name);
        } else {

            Color color;

            if (painter.isInactive() || n.isDisabled()) {
                color = settings.getInactiveColor();
            } else if (selected) {
                color = settings.getSelectedColor();
            } else if (n.isConnectionNode()) {
                if (n.isTagged()) {
                    color = settings.getTaggedConnectionColor();
                } else {
                    color = settings.getConnectionColor();
                }
            } else {
                if (n.isTagged()) {
                    color = settings.getTaggedColor();
                } else {
                    color = settings.getNodeColor();
                }
            }

            final int size = max((selected ? settings.getSelectedNodeSize() : 0),
                                    (n.isTagged() ? settings.getTaggedNodeSize() : 0),
                                    (n.isConnectionNode() ? settings.getConnectionNodeSize() : 0),
                                    settings.getUnselectedNodeSize());

            final boolean fill = (selected && settings.isFillSelectedNode()) ||
                                    (n.isTagged() && settings.isFillTaggedNode()) ||
                                    (n.isConnectionNode() && settings.isFillConnectionNode()) ||
                                    settings.isFillUnselectedNode();

            painter.drawNode(n, color, size, fill, name);
        }
    }

}
