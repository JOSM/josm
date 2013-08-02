// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.Utils.equal;

import java.awt.Color;
import java.awt.Rectangle;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Text style attached to a style with a bounding box, like an icon or a symbol.
 */
public class BoxTextElemStyle extends ElemStyle {

    public enum HorizontalTextAlignment { LEFT, CENTER, RIGHT }
    public enum VerticalTextAlignment { ABOVE, TOP, CENTER, BOTTOM, BELOW }

    public static interface BoxProvider {
        BoxProviderResult get();
    }

    public static class BoxProviderResult {
        private Rectangle box;
        private boolean temporary;

        public BoxProviderResult(Rectangle box, boolean temporary) {
            this.box = box;
            this.temporary = temporary;
        }

        /**
         * The box
         */
        public Rectangle getBox() {
            return box;
        }

        /**
         * True, if the box can change in future calls of the BoxProvider get() method
         */
        public boolean isTemporary() {
            return temporary;
        }
    }

    public static class SimpleBoxProvider implements BoxProvider {
        private Rectangle box;

        public SimpleBoxProvider(Rectangle box) {
            this.box = box;
        }

        @Override
        public BoxProviderResult get() {
            return new BoxProviderResult(box, false);
        }

        @Override
        public int hashCode() {
            return box.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof BoxProvider))
                return false;
            final BoxProvider other = (BoxProvider) obj;
            BoxProviderResult resultOther = other.get();
            if (resultOther.isTemporary()) return false;
            return box.equals(resultOther.getBox());
        }
    }

    public static final Rectangle ZERO_BOX = new Rectangle(0, 0, 0, 0);

    public TextElement text;
    // Either boxProvider or box is not null. If boxProvider is different from
    // null, this means, that the box can still change in future, otherwise
    // it is fixed.
    protected BoxProvider boxProvider;
    protected Rectangle box;
    public HorizontalTextAlignment hAlign;
    public VerticalTextAlignment vAlign;

    public BoxTextElemStyle(Cascade c, TextElement text, BoxProvider boxProvider, Rectangle box, HorizontalTextAlignment hAlign, VerticalTextAlignment vAlign) {
        super(c, 5f);
        CheckParameterUtil.ensureParameterNotNull(text);
        CheckParameterUtil.ensureParameterNotNull(hAlign);
        CheckParameterUtil.ensureParameterNotNull(vAlign);
        this.text = text;
        this.boxProvider = boxProvider;
        this.box = box == null ? ZERO_BOX : box;
        this.hAlign = hAlign;
        this.vAlign = vAlign;
    }

    public static BoxTextElemStyle create(Environment env, BoxProvider boxProvider) {
        return create(env, boxProvider, null);
    }

    public static BoxTextElemStyle create(Environment env, Rectangle box) {
        return create(env, null, box);
    }

    public static BoxTextElemStyle create(Environment env, BoxProvider boxProvider, Rectangle box) {
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

        return new BoxTextElemStyle(c, text, boxProvider, box, hAlign, vAlign);
    }

    public Rectangle getBox() {
        if (boxProvider != null) {
            BoxProviderResult result = boxProvider.get();
            if (!result.isTemporary()) {
                box = result.getBox();
                boxProvider = null;
            }
            return result.getBox();
        }
        return box;
    }

    public static final BoxTextElemStyle SIMPLE_NODE_TEXT_ELEMSTYLE;
    static {
        MultiCascade mc = new MultiCascade();
        Cascade c = mc.getOrCreateCascade("default");
        c.put(TEXT, Keyword.AUTO);
        Node n = new Node();
        n.put("name", "dummy");
        SIMPLE_NODE_TEXT_ELEMSTYLE = create(new Environment(n, mc, "default", null), NodeElemStyle.SIMPLE_NODE_ELEMSTYLE.getBoxProvider());
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
    public void paintPrimitive(OsmPrimitive osm, MapPaintSettings settings, StyledMapRenderer painter, boolean selected, boolean member) {
        if (osm instanceof Node) {
            painter.drawBoxText((Node) osm, this);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj))
            return false;
        if (obj == null || getClass() != obj.getClass())
            return false;
        final BoxTextElemStyle other = (BoxTextElemStyle) obj;
        if (!text.equals(other.text)) return false;
        if (boxProvider != null) {
            if (!boxProvider.equals(other.boxProvider)) return false;
        } else if (other.boxProvider != null)
            return false;
        else {
            if (!box.equals(other.box)) return false;
        }
        if (hAlign != other.hAlign) return false;
        if (vAlign != other.vAlign) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 97 * hash + text.hashCode();
        if (boxProvider != null) {
            hash = 97 * hash + boxProvider.hashCode();
        } else {
            hash = 97 * hash + box.hashCode();
        }
        hash = 97 * hash + hAlign.hashCode();
        hash = 97 * hash + vAlign.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "BoxTextElemStyle{" + super.toString() + " " + text.toStringImpl() + " box=" + box + " hAlign=" + hAlign + " vAlign=" + vAlign + '}';
    }

}
