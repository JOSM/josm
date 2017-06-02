// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.styleelement;

import java.util.Objects;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.Keyword;
import org.openstreetmap.josm.gui.mappaint.styleelement.placement.CompletelyInsideAreaStrategy;

/**
 * The text that is drawn for a way/area. It may be drawn along the outline or onto the way.
 *
 * @since 11722
 */
public class TextElement extends StyleElement {

    private final TextLabel text;

    protected TextElement(Cascade c, TextLabel text) {
        super(c, 4.9f);
        this.text = text;
    }

    /**
     * Create a new text element
     * @param env The environment to read the text data from
     * @return The text element or <code>null</code> if it could not be created.
     */
    public static TextElement create(final Environment env) {
        TextLabel text = TextLabel.create(env, PaintColors.TEXT.get(), false);
        if (text == null)
            return null;
        final Cascade c = env.mc.getCascade(env.layer);
        return new TextElement(c, text);
    }

    /**
     * JOSM traditionally adds both line and content text elements if a fill style was set.
     *
     * For now, we simulate this by generating a TextElement if no text-position was provided.
     * @param env The environment to read the text data from
     * @return The text element or <code>null</code> if it could not be created.
     */
    public static TextElement createForContent(Environment env) {
        final Cascade c = env.mc.getCascade(env.layer);
        Keyword positionKeyword = c.get(AreaElement.TEXT_POSITION, null, Keyword.class);
        if (positionKeyword != null) {
            return null; // No need for this hack.
        }

        TextLabel text = TextLabel.create(env, PaintColors.TEXT.get(), true);
        if (text == null) {
            return null;
        }
        return new TextElement(c, text.withPosition(CompletelyInsideAreaStrategy.INSTANCE));
    }

    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings paintSettings, StyledMapRenderer painter,
            boolean selected, boolean outermember, boolean member) {
        painter.drawText(primitive, text);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        TextElement that = (TextElement) obj;
        return Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), text);
    }

    @Override
    public String toString() {
        return "TextElement{" + super.toString() + "text=" + text + '}';
    }
}
