// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import javax.swing.GrayFilter;
import javax.swing.ImageIcon;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;

public class IconElemStyle extends NodeElemStyle {
    public ImageIcon icon;
    private ImageIcon disabledIcon;

    public IconElemStyle(long minScale, long maxScale, ImageIcon icon) {
        super(minScale, maxScale);
        this.icon = icon;
    }

    public ImageIcon getDisabledIcon() {
        if (disabledIcon != null)
            return disabledIcon;
        if (icon == null)
            return null;
        return disabledIcon = new ImageIcon(GrayFilter.createDisabledImage(icon.getImage()));
    }

    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings settings, MapPainter painter, boolean selected, boolean member) {
        if (painter.isShowIcons()) {
            Node n = (Node) primitive;
            painter.drawNodeIcon(n, (painter.isInactive() || n.isDisabled())?getDisabledIcon():icon, selected, member, getName(n, painter));
        } else {
            SimpleNodeElemStyle.INSTANCE.paintPrimitive(primitive, settings, painter, selected, member);
        }

    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        if (!super.equals(obj))
            return false;
        
        final IconElemStyle other = (IconElemStyle) obj;
        // we should get the same image object due to caching
        return this.icon.getImage() == other.icon.getImage();
    }

    @Override
    public int hashCode() {
        return icon.getImage().hashCode();
    }

    @Override
    public String toString() {
        return "IconElemStyle{" + "icon=" + icon + '}';
    }
}
