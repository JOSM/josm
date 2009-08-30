// License: GPL. For details, see LICENSE file.

// This class was taken from
// http://forum.java.sun.com/thread.jspa?threadID=459705&messageID=2104021
// - Removed hardcoded margin
// -  Added constructor

package org.openstreetmap.josm.gui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;

import javax.swing.JComponent;

public class JMultilineLabel extends JComponent {
    private String text;
    private int maxWidth = Integer.MAX_VALUE;
    private boolean justify;
    private final FontRenderContext frc = new FontRenderContext(null, false, false);

    public JMultilineLabel(String description) {
        super();
        setText(description);
    }

    private void morph() {
        revalidate();
        repaint();
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        String old = this.text;
        this.text = text;
        firePropertyChange("text", old, this.text);
        if ((old == null) ? text!=null : !old.equals(text))
            morph();
    }

    public int getMaxWidth() {
        return maxWidth;
    }

    public void setMaxWidth(int maxWidth) {
        if (maxWidth <= 0)
            throw new IllegalArgumentException();
        int old = this.maxWidth;
        this.maxWidth = maxWidth;
        firePropertyChange("maxWidth", old, this.maxWidth);
        if (old !=  this.maxWidth)
            morph();
    }

    public boolean isJustified() {
        return justify;
    }

    public void setJustified(boolean justify) {
        boolean old = this.justify;
        this.justify = justify;
        firePropertyChange("justified", old, this.justify);
        if (old != this.justify)
            repaint();
    }

    public Dimension getPreferredSize() {
        return paintOrGetSize(null, getMaxWidth());
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        paintOrGetSize((Graphics2D)g, getWidth());
    }

    private Dimension paintOrGetSize(Graphics2D g, int width) {
        Insets insets = getInsets();
        width -= insets.left + insets.right;
        float w = insets.left + insets.right;
        float x = insets.left, y=insets.top;

        if (width > 0 && text != null && text.length() > 0) {
            String[] lines = getText().split("\n");
            for(String line : lines) {
                // Insert a space so new lines get rendered
                if(line.length() == 0) line = " ";
                AttributedString as = new AttributedString(line);
                as.addAttribute(TextAttribute.FONT, getFont());
                AttributedCharacterIterator aci = as.getIterator();
                LineBreakMeasurer lbm = new LineBreakMeasurer(aci, frc);
                float max = 0;
                while (lbm.getPosition() < aci.getEndIndex()) {
                    TextLayout textLayout = lbm.nextLayout(width);
                    if (g != null && isJustified() && textLayout.getVisibleAdvance() > 0.80 * width)
                        textLayout = textLayout.getJustifiedLayout(width);
                    if (g != null)
                        textLayout.draw(g, x, y + textLayout.getAscent());
                    y += textLayout.getDescent() + textLayout.getLeading() + textLayout.getAscent();
                    max = Math.max(max, textLayout.getVisibleAdvance());
                }
                w = Math.max(max, w);
            }
        }
        return new Dimension((int)Math.ceil(w), (int)Math.ceil(y) + insets.bottom);
    }
}
