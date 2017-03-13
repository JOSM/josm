// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.styleelement;

import java.util.Objects;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.Keyword;

/**
 * A text that is only on the line
 * @deprecated since 11722. To be removed summer 2017
 */
@Deprecated
public class LineTextElement extends StyleElement {

    private final TextLabel text;

    protected LineTextElement(Cascade c, TextLabel text) {
        super(c, 4.9f);
        this.text = text;
    }

    public static LineTextElement create(final Environment env) {
        final Cascade c = env.mc.getCascade(env.layer);

        Keyword textPos = c.get(TEXT_POSITION, null, Keyword.class);
        if (textPos != null && !"line".equals(textPos.val))
            return null;

        TextLabel text = TextLabel.create(env, PaintColors.TEXT.get(), false);
        if (text == null)
            return null;
        return new LineTextElement(c, text);
    }

    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings paintSettings, StyledMapRenderer painter,
            boolean selected, boolean outermember, boolean member) {
        Way w = (Way) primitive;
        painter.drawTextOnPath(w, text);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        LineTextElement that = (LineTextElement) obj;
        return Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), text);
    }

    @Override
    public String toString() {
        return "LineTextElemStyle{" + super.toString() + "text=" + text + '}';
    }
}
