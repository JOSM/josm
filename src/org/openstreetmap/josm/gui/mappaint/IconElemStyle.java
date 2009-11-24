package org.openstreetmap.josm.gui.mappaint;

import javax.swing.GrayFilter;
import javax.swing.ImageIcon;

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
}
