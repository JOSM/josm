// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.styleelement;

import java.util.Objects;

import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.Keyword;
import org.openstreetmap.josm.gui.mappaint.styleelement.placement.CompletelyInsideAreaStrategy;
import org.openstreetmap.josm.gui.mappaint.styleelement.placement.PositionForAreaStrategy;

/**
 * The text that is drawn for a way/area. It may be drawn along the outline or onto the way.
 *
 * @since 11722
 */
public class TextElement extends StyleElement {

    private final TextLabel text;
    /**
     * The position strategy for this text label.
     */
    private final PositionForAreaStrategy labelPositionStrategy;

    /**
     * Create a new way/area text element definition
     * @param c The cascade
     * @param text The text
     * @param labelPositionStrategy The position in the area.
     */
    protected TextElement(Cascade c, TextLabel text, PositionForAreaStrategy labelPositionStrategy) {
        super(c, 4.9f);
        this.text = Objects.requireNonNull(text, "text");
        this.labelPositionStrategy = Objects.requireNonNull(labelPositionStrategy, "labelPositionStrategy");
    }

    /**
     * Gets the strategy that defines where to place the label.
     * @return The strategy. Never null.
     * @since 12475
     */
    public PositionForAreaStrategy getLabelPositionStrategy() {
        return labelPositionStrategy;
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

        Keyword positionKeyword = c.get(AreaElement.TEXT_POSITION, null, Keyword.class);
        PositionForAreaStrategy position = PositionForAreaStrategy.forKeyword(positionKeyword);
        position = position.withAddedOffset(TextLabel.getTextOffset(c));

        return new TextElement(c, text, position);
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
        return new TextElement(c, text, CompletelyInsideAreaStrategy.INSTANCE);
    }

    @Override
    public void paintPrimitive(IPrimitive primitive, MapPaintSettings paintSettings, StyledMapRenderer painter,
            boolean selected, boolean outermember, boolean member) {
        painter.drawText(primitive, text, getLabelPositionStrategy());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        TextElement that = (TextElement) obj;
        return Objects.equals(labelPositionStrategy, that.labelPositionStrategy)
            && Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), text, labelPositionStrategy);
    }

    @Override
    public String toString() {
        return "TextElement{" + super.toString() + "text=" + text + " labelPositionStrategy=" + labelPositionStrategy + '}';
    }
}
