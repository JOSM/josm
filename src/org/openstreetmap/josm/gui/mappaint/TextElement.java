// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.Utils.equal;

import java.awt.Color;
import java.awt.Font;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;

import org.openstreetmap.josm.tools.CheckParameterUtil;

public class TextElement {
    // textKey == null means automatic generation of text string, otherwise
    // the corresponding tag value is used
    public String textKey;
    public Font font;
    public int xOffset;
    public int yOffset;
    public Color color;

    public TextElement(String textKey, Font font, int xOffset, int yOffset, Color color) {
        CheckParameterUtil.ensureParameterNotNull(font);
        CheckParameterUtil.ensureParameterNotNull(color);
        this.textKey = textKey;
        this.font = font;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.color = color;
    }

    public static TextElement create(Cascade c, Color defTextColor) {

        String textKey = null;
        Keyword textKW = c.get("text", null, Keyword.class, true);
        if (textKW == null) {
            textKey = c.get("text", null, String.class);
            if (textKey == null)
                return null;
        } else if (!textKW.val.equals("auto"))
            return null;

        Font font = ElemStyle.getFont(c);

        float xOffset = 0;
        float yOffset = 0;
        float[] offset = c.get("text-offset", null, float[].class);
        if (offset != null) {
            if (offset.length == 1) {
                yOffset = offset[0];
            } else if (offset.length >= 2) {
                xOffset = offset[0];
                yOffset = offset[1];
            }
        }
        xOffset = c.get("text-offset-x", xOffset, Float.class);
        yOffset = c.get("text-offset-y", yOffset, Float.class);
        
        Color color = c.get("text-color", defTextColor, Color.class);
        
        return new TextElement(textKey, font, (int) xOffset, - (int) yOffset, color);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        final TextElement other = (TextElement) obj;
        return  equal(textKey, other.textKey) &&
                equal(font, other.font) &&
                xOffset == other.xOffset &&
                yOffset == other.yOffset &&
                equal(color, other.color);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + (textKey != null ? textKey.hashCode() : 0);
        hash = 79 * hash + font.hashCode();
        hash = 79 * hash + xOffset;
        hash = 79 * hash + yOffset;
        hash = 79 * hash + color.hashCode();
        return hash;
    }

    public String getString(OsmPrimitive osm, MapPainter painter) {
        if (textKey == null)
            return painter.getAreaName(osm);
        else
            return osm.get(textKey);
    }


}
