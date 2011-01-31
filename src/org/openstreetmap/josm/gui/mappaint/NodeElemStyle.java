// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.awt.Color;

import javax.swing.GrayFilter;
import javax.swing.ImageIcon;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.IconReference;
import org.openstreetmap.josm.tools.Utils;

/**
 * applies for Nodes and turn restriction relations
 */
public class NodeElemStyle extends ElemStyle {
    public boolean annotate;
    public String annotation_key;
    public ImageIcon icon;
    private ImageIcon disabledIcon;

    public static final NodeElemStyle SIMPLE_NODE_ELEMSTYLE = new NodeElemStyle(Cascade.EMPTY_CASCADE, true, null, null);

    protected NodeElemStyle(Cascade c, boolean annotate, String annotation_key, ImageIcon icon) {
        super(c);
        this.annotate = annotate;
        this.annotation_key = annotation_key;
        this.icon = icon;
    }

    public static NodeElemStyle create(Cascade c) {
        IconReference iconRef = c.get("icon-image", null, IconReference.class);
        if (iconRef == null)
            return null;

        ImageIcon icon = MapPaintStyles.getIcon(iconRef);
        String text = c.get("text", null, String.class);

        boolean annotate = text != null;
        String annotation_key = null;

        if (annotate && !"yes".equalsIgnoreCase(text)) {
            annotation_key = text;
        }
        return new NodeElemStyle(c, annotate, annotation_key, icon);
    }

    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings settings, MapPainter painter, boolean selected, boolean member) {
        if (primitive instanceof Node) {
            Node n = (Node) primitive;
            if (icon != null && painter.isShowIcons()) {
                painter.drawNodeIcon(n, (painter.isInactive() || n.isDisabled()) ? getDisabledIcon() : icon,
                        selected, member, getName(n, painter));
            } else {
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

                    final int size = Utils.max((selected ? settings.getSelectedNodeSize() : 0),
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
        } else if (primitive instanceof Relation) {
            painter.drawRestriction((Relation) primitive, this);
        }
    }

    public ImageIcon getDisabledIcon() {
        if (disabledIcon != null)
            return disabledIcon;
        if (icon == null)
            return null;
        return disabledIcon = new ImageIcon(GrayFilter.createDisabledImage(icon.getImage()));
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

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 17 * hash + (annotate ? 1 : 0);
        hash = 17 * hash + (annotation_key != null ? annotation_key.hashCode() : 0);
        hash = 17 * hash + (icon != null ? icon.getImage().hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        if (!super.equals(obj))
            return false;

        final NodeElemStyle other = (NodeElemStyle) obj;
        // we should get the same image object due to caching
        if (icon != other.icon && (icon == null || other.icon == null || icon.getImage() != other.icon.getImage()))
            return false;
        if (annotate != other.annotate)
            return false;
        if (!Utils.equal(annotation_key, annotation_key))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "NodeElemStyle{" + super.toString() + "annotate=" + annotate + " annotation_key=" + annotation_key + " icon=" + icon + '}';
    }

}
