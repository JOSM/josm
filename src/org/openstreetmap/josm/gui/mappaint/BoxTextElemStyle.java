// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.awt.Color;
import static org.openstreetmap.josm.tools.Utils.equal;

import java.awt.Rectangle;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;

import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Text style attached to a style with a bounding box, like an icon or a symbol.
 */
public class BoxTextElemStyle extends ElemStyle {

    public enum HorizontalTextAlignment { LEFT, CENTER, RIGHT }
    public enum VerticalTextAlignment { ABOVE, TOP, CENTER, BOTTOM, BELOW }
    
    public static Rectangle ZERO_BOX = new Rectangle(0, 0, 0, 0);
    
    public TextElement text;
    public Rectangle box;
    public HorizontalTextAlignment hAlign;
    public VerticalTextAlignment vAlign;

    public BoxTextElemStyle(Cascade c, TextElement text, Rectangle box, HorizontalTextAlignment hAlign, VerticalTextAlignment vAlign) {
        super(c, 2000f);
        CheckParameterUtil.ensureParameterNotNull(text);
        CheckParameterUtil.ensureParameterNotNull(hAlign);
        CheckParameterUtil.ensureParameterNotNull(vAlign);
        this.text = text;
        this.box = box == null ? ZERO_BOX : box;
        this.hAlign = hAlign;
        this.vAlign = vAlign;
    }
    
    public static BoxTextElemStyle create(Environment env, Rectangle box) {
        initDefaultParameters();
        Cascade c = env.mc.getCascade(env.layer);

        TextElement text = TextElement.create(c, DEFAULT_TEXT_COLOR, false);
        if (text == null) return null;
        // Skip any primtives that don't have text to draw. (Styles are recreated for any tag change.)
        // The concrete text to render is not cached in this object, but computed for each
        // repaint. This way, one BoxTextElemStyle object can be used by multiple primitives (to save memory).
        if (text.labelCompositionStrategy.compose(env.osm) == null) return null;

        HorizontalTextAlignment hAlign = HorizontalTextAlignment.RIGHT;
        Keyword hAlignKW = c.get("text-anchor-horizontal", Keyword.RIGHT, Keyword.class);
        if (equal(hAlignKW.val, "left")) {
            hAlign = HorizontalTextAlignment.LEFT;
        } else if (equal(hAlignKW.val, "center")) {
            hAlign = HorizontalTextAlignment.CENTER;
        } else if (equal(hAlignKW.val, "right")) {
            hAlign = HorizontalTextAlignment.RIGHT;
        }
        VerticalTextAlignment vAlign = VerticalTextAlignment.BOTTOM;
        String vAlignStr = c.get("text-anchor-vertical", Keyword.BOTTOM, Keyword.class).val;
        if (equal(vAlignStr, "above")) {
            vAlign = VerticalTextAlignment.ABOVE;
        } else if (equal(vAlignStr, "top")) {
            vAlign = VerticalTextAlignment.TOP;
        } else if (equal(vAlignStr, "center")) {
            vAlign = VerticalTextAlignment.CENTER;
        } else if (equal(vAlignStr, "bottom")) {
            vAlign = VerticalTextAlignment.BOTTOM;
        } else if (equal(vAlignStr, "below")) {
            vAlign = VerticalTextAlignment.BELOW;
        }
        
        return new BoxTextElemStyle(c, text, box, hAlign, vAlign);
    }
    
    public static final BoxTextElemStyle SIMPLE_NODE_TEXT_ELEMSTYLE;
    static {
        MultiCascade mc = new MultiCascade();
        Cascade c = mc.getOrCreateCascade("default");
        c.put("text", Keyword.AUTO);
        Node n = new Node();
        n.put("name", "dummy");
        SIMPLE_NODE_TEXT_ELEMSTYLE = create(new Environment(n, mc, "default", null), NodeElemStyle.SIMPLE_NODE_ELEMSTYLE.getBox());
        if (SIMPLE_NODE_TEXT_ELEMSTYLE == null) throw new AssertionError();
    }
    /*
     * Caches the default text color from the preferences.
     *
     * FIXME: the cache isn't updated if the user changes the preference during a JOSM
     * session. There should be preference listener updating this cache.
     */
    static private Color DEFAULT_TEXT_COLOR = null;
    static private void initDefaultParameters() {
        if (DEFAULT_TEXT_COLOR != null) return;
        DEFAULT_TEXT_COLOR = PaintColors.TEXT.get();
    }

    @Override
    public void paintPrimitive(OsmPrimitive osm, MapPaintSettings settings, MapPainter painter, boolean selected, boolean member) {
        if (osm instanceof Node) {
            painter.drawBoxText((Node) osm, this);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final BoxTextElemStyle other = (BoxTextElemStyle) obj;
        return text.equals(other.text) &&
                box.equals(other.box) && 
                hAlign == other.hAlign && 
                vAlign == other.vAlign;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 97 * hash + text.hashCode();
        hash = 97 * hash + box.hashCode();
        hash = 97 * hash + hAlign.hashCode();
        hash = 97 * hash + vAlign.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "BoxTextElemStyle{" + super.toString() + " " + text.toStringImpl() + " box=" + box + " hAlign=" + hAlign + " vAlign=" + vAlign + '}';
    }

}
