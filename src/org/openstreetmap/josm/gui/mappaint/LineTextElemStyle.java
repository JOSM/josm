// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.tools.Utils;

public class LineTextElemStyle extends ElemStyle {

    private TextElement text;

    protected LineTextElemStyle(Cascade c, TextElement text) {
        super(c, 4.9f);
        this.text = text;
    }
    public static LineTextElemStyle create(Environment env) {
        Cascade c = env.mc.getCascade(env.layer);

        Keyword textPos = c.get(TEXT_POSITION, null, Keyword.class);
        if (textPos != null && !Utils.equal(textPos.val, "line"))
            return null;

        TextElement text = TextElement.create(c, PaintColors.TEXT.get(), false);
        if (text == null)
            return null;
        return new LineTextElemStyle(c, text);
    }

    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings paintSettings, StyledMapRenderer painter, boolean selected, boolean member) {
        Way w = (Way)primitive;
        painter.drawTextOnPath(w, text);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        if (!super.equals(obj))
            return false;
        final LineTextElemStyle other = (LineTextElemStyle) obj;
        return Utils.equal(text, other.text);
    }

    @Override
    public int hashCode() {
        return text.hashCode();
    }

    @Override
    public String toString() {
        return "LineTextElemStyle{" + super.toString() + "text=" + text + "}";
    }

}
