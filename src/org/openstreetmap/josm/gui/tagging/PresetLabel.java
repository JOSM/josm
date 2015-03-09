// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.util.Collections;

import javax.swing.JLabel;

public class PresetLabel extends JLabel {

    protected final TaggingPreset t;

    /**
     * Constructs a new {@code PresetLabel}.
     * @param t the tagging preset
     */
    public PresetLabel(TaggingPreset t) {
        super(t.getName() + " …");
        setIcon(t.getIcon());
        addMouseListener(new PresetLabelMouseListener(this));
        this.t = t;
    }

    /**
     * Small helper class that manages the highlighting of the label on hover as well as opening
     * the corresponding preset when clicked
     */
    public static class PresetLabelMouseListener implements MouseListener {
        protected final JLabel label;
        protected final Font hover;
        protected final Font normal;

        /**
         * Constructs a new {@code PresetLabelMouseListener}.
         * @param lbl Label to highlight
         */
        public PresetLabelMouseListener(JLabel lbl) {
            label = lbl;
            lbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
            normal = label.getFont();
            hover = normal.deriveFont(Collections.singletonMap(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_DOTTED));
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            // Do nothing
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            label.setFont(hover);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            label.setFont(normal);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            // Do nothing
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            // Do nothing
        }
    }
}
