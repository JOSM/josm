// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.awt.Color;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;

public class SimpleNodeElemStyle extends NodeElemStyle {

    public static final SimpleNodeElemStyle INSTANCE = new SimpleNodeElemStyle();

    private SimpleNodeElemStyle() {
        super(0, Long.MAX_VALUE);
        annotate = true;
    }

    private static int max(int a, int b, int c, int d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings settings, MapPainter painter,
            boolean selected, boolean member) {
        Node n = (Node)primitive;

        if (n.isHighlighted()) {
            painter.drawNode(n, settings.getHighlightColor(), settings.getSelectedNodeSize(), settings.isFillSelectedNode(), getName(n, painter));
        } else {

            Color color;
            boolean isConnection = n.isConnectionNode();

            if (painter.isInactive() || n.isDisabled()) {
                color = settings.getInactiveColor();
            } else if (selected) {
                color = settings.getSelectedColor();
            } else if (member) {
                color = settings.getRelationSelectedColor();
            } else if (isConnection) {
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
                                    (isConnection ? settings.getConnectionNodeSize() : 0),
                                    settings.getUnselectedNodeSize());

            final boolean fill = (selected && settings.isFillSelectedNode()) ||
                                    (n.isTagged() && settings.isFillTaggedNode()) ||
                                    (isConnection && settings.isFillConnectionNode()) ||
                                    settings.isFillUnselectedNode();

            painter.drawNode(n, color, size, fill, getName(n, painter));
        }
    }
}
