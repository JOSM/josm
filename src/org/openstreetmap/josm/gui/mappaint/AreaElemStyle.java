package org.openstreetmap.josm.gui.mappaint;
import java.awt.Color;

public class AreaElemStyle extends ElemStyle
{
    public Color color;
    public boolean closed;
    public LineElemStyle line = null;

    public AreaElemStyle (AreaElemStyle a, long maxScale, long minScale) {
        this.color = a.color;
        this.closed = a.closed;
        this.priority = a.priority;
        this.maxScale = maxScale;
        this.minScale = minScale;
        this.rules = a.rules;
    }

    public AreaElemStyle(AreaElemStyle a, LineElemStyle l)
    {
        this.color = a.color;
        this.closed = a.closed;
        this.priority = a.priority;
        this.maxScale = a.maxScale;
        this.minScale = a.minScale;
        this.rules = a.rules;
        this.line = l;
        this.code = a.code;
    }

    public AreaElemStyle() { init(); }

    public void init()
    {
        color = null;
        priority = 0;
    }
}
