package org.openstreetmap.josm.gui.mappaint;
import javax.swing.ImageIcon;

public class IconElemStyle extends ElemStyle
{
    public ImageIcon icon;
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

    public void init()
    {
        icon = null;
        priority = 0;
        annotate = true;
    }
}
