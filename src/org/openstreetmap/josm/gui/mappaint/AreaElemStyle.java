package org.openstreetmap.josm.gui.mappaint;
import java.awt.Color;

public class AreaElemStyle extends ElemStyle
{
    public Color color;
    public LineElemStyle line = null;

    public AreaElemStyle (AreaElemStyle a, long maxScale, long minScale) {
        this.color = a.color;
        this.priority = a.priority;
        this.maxScale = maxScale;
        this.minScale = minScale;
    }

    public AreaElemStyle(AreaElemStyle a, LineElemStyle l)
    {
        this.color = a.color;
        this.priority = a.priority;
        this.maxScale = a.maxScale;
        this.minScale = a.minScale;
        this.line = l;
    }

    public AreaElemStyle() { init(); }

    public void init()
    {
        color = null;
        priority = 0;
    }
}
