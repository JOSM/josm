// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.styleelement;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Objects;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.Keyword;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Text style attached to a style with a bounding box, like an icon or a symbol.
 */
public class BoxTextElement extends StyleElement {

    public enum HorizontalTextAlignment { LEFT, CENTER, RIGHT }

    public enum VerticalTextAlignment { ABOVE, TOP, CENTER, BOTTOM, BELOW }

    public interface BoxProvider {
        BoxProviderResult get();
    }

    public static class BoxProviderResult {
        private final Rectangle box;
        private final boolean temporary;

        public BoxProviderResult(Rectangle box, boolean temporary) {
            this.box = box;
            this.temporary = temporary;
        }

        /**
         * Returns the box.
         * @return the box
         */
        public Rectangle getBox() {
            return box;
        }

        /**
         * Determines if the box can change in future calls of the {@link BoxProvider#get()} method
         * @return {@code true} if the box can change in future calls of the {@code BoxProvider#get()} method
         */
        public boolean isTemporary() {
            return temporary;
        }
    }

    public static class SimpleBoxProvider implements BoxProvider {
        private final Rectangle box;

        /**
         * Constructs a new {@code SimpleBoxProvider}.
         * @param box the box
         */
        public SimpleBoxProvider(Rectangle box) {
            this.box = box;
        }

        @Override
        public BoxProviderResult get() {
            return new BoxProviderResult(box, false);
        }

        @Override
        public int hashCode() {
            return Objects.hash(box);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            SimpleBoxProvider that = (SimpleBoxProvider) obj;
            return Objects.equals(box, that.box);
        }
    }

    public static final Rectangle ZERO_BOX = new Rectangle(0, 0, 0, 0);

    public TextLabel text;
    // Either boxProvider or box is not null. If boxProvider is different from
    // null, this means, that the box can still change in future, otherwise
    // it is fixed.
    protected BoxProvider boxProvider;
    protected Rectangle box;
    public HorizontalTextAlignment hAlign;
    public VerticalTextAlignment vAlign;

    public BoxTextElement(Cascade c, TextLabel text, BoxProvider boxProvider, Rectangle box,
            HorizontalTextAlignment hAlign, VerticalTextAlignment vAlign) {
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

    public static BoxTextElement create(Environment env, BoxProvider boxProvider) {
        return create(env, boxProvider, null);
    }

    public static BoxTextElement create(Environment env, Rectangle box) {
        return create(env, null, box);
    }

    public static BoxTextElement create(Environment env, BoxProvider boxProvider, Rectangle box) {
        initDefaultParameters();

        TextLabel text = TextLabel.create(env, DEFAULT_TEXT_COLOR, false);
        if (text == null) return null;
        // Skip any primitives that don't have text to draw. (Styles are recreated for any tag change.)
        // The concrete text to render is not cached in this object, but computed for each
        // repaint. This way, one BoxTextElement object can be used by multiple primitives (to save memory).
        if (text.labelCompositionStrategy.compose(env.osm) == null) return null;

        Cascade c = env.mc.getCascade(env.layer);

        HorizontalTextAlignment hAlign;
        switch (c.get(TEXT_ANCHOR_HORIZONTAL, Keyword.RIGHT, Keyword.class).val) {
            case "left":
                hAlign = HorizontalTextAlignment.LEFT;
                break;
            case "center":
                hAlign = HorizontalTextAlignment.CENTER;
                break;
            case "right":
            default:
                hAlign = HorizontalTextAlignment.RIGHT;
        }
        VerticalTextAlignment vAlign;
        switch (c.get(TEXT_ANCHOR_VERTICAL, Keyword.BOTTOM, Keyword.class).val) {
            case "above":
                vAlign = VerticalTextAlignment.ABOVE;
                break;
            case "top":
                vAlign = VerticalTextAlignment.TOP;
                break;
            case "center":
                vAlign = VerticalTextAlignment.CENTER;
                break;
            case "below":
                vAlign = VerticalTextAlignment.BELOW;
                break;
            case "bottom":
            default:
                vAlign = VerticalTextAlignment.BOTTOM;
        }

        return new BoxTextElement(c, text, boxProvider, box, hAlign, vAlign);
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

    public static final BoxTextElement SIMPLE_NODE_TEXT_ELEMSTYLE;
    static {
        MultiCascade mc = new MultiCascade();
        Cascade c = mc.getOrCreateCascade("default");
        c.put(TEXT, Keyword.AUTO);
        Node n = new Node();
        n.put("name", "dummy");
        SIMPLE_NODE_TEXT_ELEMSTYLE = create(new Environment(n, mc, "default", null), NodeElement.SIMPLE_NODE_ELEMSTYLE.getBoxProvider());
        if (SIMPLE_NODE_TEXT_ELEMSTYLE == null) throw new AssertionError();
    }

    /*
     * Caches the default text color from the preferences.
     *
     * FIXME: the cache isn't updated if the user changes the preference during a JOSM
     * session. There should be preference listener updating this cache.
     */
    private static volatile Color DEFAULT_TEXT_COLOR;

    private static void initDefaultParameters() {
        if (DEFAULT_TEXT_COLOR != null) return;
        DEFAULT_TEXT_COLOR = PaintColors.TEXT.get();
    }

    @Override
    public void paintPrimitive(OsmPrimitive osm, MapPaintSettings settings, StyledMapRenderer painter,
            boolean selected, boolean outermember, boolean member) {
        if (osm instanceof Node) {
            painter.drawBoxText((Node) osm, this);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        BoxTextElement that = (BoxTextElement) obj;
        return Objects.equals(text, that.text) &&
                Objects.equals(boxProvider, that.boxProvider) &&
                Objects.equals(box, that.box) &&
                hAlign == that.hAlign &&
                vAlign == that.vAlign;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), text, boxProvider, box, hAlign, vAlign);
    }

    @Override
    public String toString() {
        return "BoxTextElemStyle{" + super.toString() + ' ' + text.toStringImpl()
                + " box=" + box + " hAlign=" + hAlign + " vAlign=" + vAlign + '}';
    }
}
