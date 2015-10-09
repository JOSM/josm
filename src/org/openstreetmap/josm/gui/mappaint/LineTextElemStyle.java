// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.util.Objects;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;

public class LineTextElemStyle extends ElemStyle {

    private TextElement text;

    protected LineTextElemStyle(Cascade c, TextElement text) {
        super(c, 4.9f);
        this.text = text;
    }

    public static LineTextElemStyle create(final Environment env) {
        final Cascade c = env.mc.getCascade(env.layer);

        Keyword textPos = c.get(TEXT_POSITION, null, Keyword.class);
        if (textPos != null && !"line".equals(textPos.val))
            return null;

        TextElement text = TextElement.create(env, PaintColors.TEXT.get(), false);
        if (text == null)
            return null;
        return new LineTextElemStyle(c, text);
    }

    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings paintSettings, StyledMapRenderer painter,
            boolean selected, boolean outermember, boolean member) {
        Way w = (Way) primitive;
        painter.drawTextOnPath(w, text);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        if (!super.equals(obj))
            return false;
        final LineTextElemStyle other = (LineTextElemStyle) obj;
        return Objects.equals(text, other.text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    @Override
    public String toString() {
        return "LineTextElemStyle{" + super.toString() + "text=" + text + '}';
    }
}
