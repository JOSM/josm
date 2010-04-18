package org.openstreetmap.josm.gui.mappaint;

import java.awt.Color;
import java.util.Collection;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.tools.I18n;

public class LineElemStyle extends ElemStyle implements Comparable<LineElemStyle> {

    public static final LineElemStyle UNTAGGED_WAY;

    static {
        UNTAGGED_WAY = new LineElemStyle();
        UNTAGGED_WAY.color = PaintColors.UNTAGGED.get();
    }

    private int width;
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
        width = -1;
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

    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings paintSettings, MapPainter painter, boolean selected) {
        Way w = (Way)primitive;
        /* show direction arrows, if draw.segment.relevant_directions_only is not set,
        the way is tagged with a direction key
        (even if the tag is negated as in oneway=false) or the way is selected */
        boolean showDirection = selected || ((!paintSettings.isUseRealWidth()) && (paintSettings.isShowDirectionArrow()
                && (!paintSettings.isShowRelevantDirectionsOnly() || w.hasDirectionKeys())));
        boolean reversedDirection = w.reversedDirection();
        /* head only takes over control if the option is true,
        the direction should be shown at all and not only because it's selected */
        boolean showOnlyHeadArrowOnly = showDirection && !selected && paintSettings.isShowHeadArrowOnly();
        Node lastN;

        Color myColor = color;
        int myWidth = getWidth();

        if (realWidth > 0 && paintSettings.isUseRealWidth() && !showDirection) {

            /* if we have a "width" tag, try use it */
            /* (this might be slow and could be improved by caching the value in the Way, on the other hand only used if "real width" is enabled) */
            String widthTag = w.get("width");
            if(widthTag == null) {
                widthTag = w.get("est_width");
            }
            if(widthTag != null) {
                try {
                    realWidth = Integer.parseInt(widthTag);
                }
                catch(NumberFormatException nfe) {
                }
            }

            myWidth = (int) (100 /  (float) (painter.getCircum() / realWidth));
            if (myWidth < getWidth()) {
                myWidth = getWidth();
            }
        }

        if(w.isHighlighted()) {
            myColor = paintSettings.getHighlightColor();
        } else if (selected) {
            myColor = paintSettings.getSelectedColor();
        } else if(w.isDisabled()) {
            myColor = paintSettings.getInactiveColor();
        }

        /* draw overlays under the way */
        if(overlays != null) {
            for(LineElemStyle s : overlays) {
                if(!s.over) {
                    painter.drawWay(w, s.color != null && selected ? myColor: s.color, s.getWidth(myWidth),
                            s.getDashed(), s.dashedColor, false, false, false);
                }
            }
        }

        /* draw the way */
        painter.drawWay(w, myColor, myWidth, dashed, dashedColor, showDirection, selected ? false : reversedDirection, showOnlyHeadArrowOnly);

        /* draw overlays above the way */
        if(overlays != null)  {
            for(LineElemStyle s : overlays) {
                if(s.over) {
                    painter.drawWay(w, s.color != null && selected ? myColor : s.color, s.getWidth(myWidth),
                            s.getDashed(), s.dashedColor, false, false, false);
                }
            }
        }

        if(paintSettings.isShowOrderNumber()) {
            int orderNumber = 0;
            lastN = null;
            for(Node n : w.getNodes()) {
                if(lastN != null) {
                    orderNumber++;
                    painter.drawOrderNumber(lastN, n, orderNumber);
                }
                lastN = n;
            }
        }
    }

    public int getWidth() {
        if (width == -1)
            return MapPaintSettings.INSTANCE.getDefaultSegmentWidth();
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }
}
