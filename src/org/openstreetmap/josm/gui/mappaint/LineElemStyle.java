package org.openstreetmap.josm.gui.mappaint;

import java.awt.Color;
import java.util.Collection;

import org.openstreetmap.josm.tools.I18n;

public class LineElemStyle extends ElemStyle implements Comparable<LineElemStyle>
{
    public int width;
    public int realWidth; //the real width of this line in meter
    public Color color;
    private float[] dashed;
    public Color dashedColor;

    public boolean over;
    public enum WidthMode { ABSOLUTE, PERCENT, OFFSET }
    public WidthMode widthMode;

    public Collection<LineElemStyle> overlays;

    public LineElemStyle(LineElemStyle s, long maxScale, long minScale) {
        this.width = s.width;
        this.realWidth = s.realWidth;
        this.color = s.color;
        this.dashed = s.dashed;
        this.dashedColor = s.dashedColor;
        this.over = s.over;
        this.widthMode = s.widthMode;

        this.priority = s.priority;
        this.maxScale = maxScale;
        this.minScale = minScale;
        this.rules = s.rules;
    }

    public LineElemStyle(LineElemStyle s, Collection<LineElemStyle> overlays) {
        this.width = s.width;
        this.realWidth = s.realWidth;
        this.color = s.color;
        this.dashed = s.dashed;
        this.dashedColor = s.dashedColor;
        this.over = s.over;
        this.widthMode = s.widthMode;

        this.priority = s.priority;
        this.maxScale = s.maxScale;
        this.minScale = s.minScale;
        this.rules = s.rules;

        this.overlays = overlays;
        this.code = s.code;
        for (LineElemStyle o : overlays) {
            this.code += o.code;
        }
    }

    public LineElemStyle() { init(); }

    public void init()
    {
        width = 1;
        realWidth = 0;
        dashed = new float[0];
        dashedColor = null;
        priority = 0;
        color = null;
        over = true; // only used for line modifications
        widthMode = WidthMode.ABSOLUTE;
        overlays = null;
    }

    // get width for overlays
    public int getWidth(int ref)
    {
        int res;
        if(widthMode == WidthMode.ABSOLUTE) {
            res = width;
        } else if(widthMode == WidthMode.OFFSET) {
            res = ref + width;
        } else
        {
            if(width < 0) {
                res = 0;
            } else {
                res = ref*width/100;
            }
        }
        return res <= 0 ? 1 : res;
    }

    public int compareTo(LineElemStyle s) {
        if(s.priority != priority)
            return s.priority > priority ? 1 : -1;
            if(!over && s.over)
                return -1;
            // we have no idea how to order other objects :-)
            return 0;
    }

    public float[] getDashed() {
        return dashed;
    }

    public void setDashed(float[] dashed) {
        if (dashed.length == 0) {
            this.dashed = dashed;
            return;
        }

        boolean found = false;
        for (int i=0; i<dashed.length; i++) {
            if (dashed[i] > 0) {
                found = true;
            }
            if (dashed[i] < 0) {
                System.out.println(I18n.tr("Illegal dash pattern, values must be positive"));
            }
        }
        if (found) {
            this.dashed = dashed;
        } else {
            System.out.println(I18n.tr("Illegal dash pattern, at least one value must be > 0"));
        }
    }
}
