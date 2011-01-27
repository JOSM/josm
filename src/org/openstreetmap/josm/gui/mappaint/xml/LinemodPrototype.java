// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.xml;

import org.openstreetmap.josm.gui.mappaint.LineElemStyle;

public class LinemodPrototype extends LinePrototype implements Comparable<LinemodPrototype> {

    public boolean over;

    public enum WidthMode { ABSOLUTE, PERCENT, OFFSET }
    public WidthMode widthMode;

    public LinemodPrototype(LinemodPrototype s, long maxScale, long minScale) {
        super(s, maxScale, minScale);
        this.over = s.over;
        this.widthMode = s.widthMode;
    }

    public LinemodPrototype() { init(); }

    @Override
    public void init()
    {
        super.init();
        over = true;
        widthMode = WidthMode.ABSOLUTE;
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

    @Override
    public int getWidth() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int compareTo(LinemodPrototype s) {
        if(s.priority != priority)
            return s.priority > priority ? 1 : -1;
            if(!over && s.over)
                return -1;
            // we have no idea how to order other objects :-)
            return 0;
    }

    /**
     * this method cannot be used for LinemodPrototypes
     *  - use createStyle(int) instead
     */
    @Override
    public LineElemStyle createStyle() {
        throw new UnsupportedOperationException();
    }

    public LineElemStyle createStyle(int refWidth) {
        return new LineElemStyle(minScale, maxScale, getWidth(refWidth), realWidth, color, dashed, dashedColor);
    }
}
