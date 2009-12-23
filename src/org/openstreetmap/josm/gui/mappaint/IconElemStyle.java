package org.openstreetmap.josm.gui.mappaint;

import javax.swing.GrayFilter;
import javax.swing.ImageIcon;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;

public class IconElemStyle extends ElemStyle
{
    public ImageIcon icon;
    private ImageIcon disabledIcon;
    public boolean annotate;

    public IconElemStyle (IconElemStyle i, long maxScale, long minScale) {
        this.icon = i.icon;
        this.annotate = i.annotate;
        this.priority = i.priority;
        this.maxScale = maxScale;
        this.minScale = minScale;
        this.rules = i.rules;
    }
    public IconElemStyle() { init(); }

    public void init() {
        icon = null;
        priority = 0;
        annotate = true;
    }

    public ImageIcon getDisabledIcon() {
        if (disabledIcon != null)
            return disabledIcon;
        if (icon == null)
            return null;
        return disabledIcon = new ImageIcon(GrayFilter.createDisabledImage(icon.getImage()));
    }
    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings settings, MapPainter painter, boolean selected) {
        if (painter.isShowIcons()) {
            Node n = (Node) primitive;
            String name = painter.isShowNames()?painter.getNodeName(n):null;
            painter.drawNodeIcon(n, (painter.isInactive() || n.isDisabled())?getDisabledIcon():icon,
                    annotate, selected, name);
        } else {
            SimpleNodeElemStyle.INSTANCE.paintPrimitive(primitive, settings, painter, selected);
        }

    }
}
