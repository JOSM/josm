// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

/**
 * On screen display label.
 * @since 12389 (extracted from FilterTableModel)
 */
public class OSDLabel extends JLabel {

    /**
     * Constructs a new {@code OSDLabel}.
     * @param text The text to be displayed by the label
     */
    public OSDLabel(String text) {
        super(text);
        setOpaque(true);
        setForeground(Color.black);
        setBackground(new Color(0, 0, 0, 0));
        setFont(getFont().deriveFont(Font.PLAIN));
        setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
    }

    @Override
    public void paintComponent(Graphics g) {
        g.setColor(new Color(255, 255, 255, 140));
        g.fillRoundRect(getX(), getY(), getWidth(), getHeight(), 10, 10);
        super.paintComponent(g);
    }
}
