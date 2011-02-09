// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.Utils.equal;

import java.awt.Color;
import java.awt.Font;

import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.tools.CheckParameterUtil;

public class TextElement {
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

    public static TextElement create(Cascade c) {
        String textStr = c.get("text", null, String.class);
        if (textStr == null)
            return null;

        String textKey = null;
        if (!"auto".equalsIgnoreCase(textStr)) {
            textKey = textStr;
        }

        Font font = ElemStyle.getFont(c);
        int xOffset = c.get("text-offset-x", 0f, Float.class).intValue();
        int yOffset = -c.get("text-offset-y", 0f, Float.class).intValue();
        Color color = c.get("text-color", PaintColors.TEXT.get(), Color.class);
        return new TextElement(textKey, font, xOffset, yOffset, color);
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

}
