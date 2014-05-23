// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import javax.swing.JLabel;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.util.Collections;

public class PresetLabel extends JLabel {

    protected final TaggingPreset t;

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
        final protected JLabel label;
        final protected Font hover;
        final protected Font normal;

        public PresetLabelMouseListener(JLabel lbl) {
            label = lbl;
            lbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
            normal = label.getFont();
            hover = normal.deriveFont(Collections.singletonMap(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_DOTTED));
        }
        @Override
        public void mouseClicked(MouseEvent arg0) {
        }

        @Override
        public void mouseEntered(MouseEvent arg0) {
            label.setFont(hover);
        }
        @Override
        public void mouseExited(MouseEvent arg0) {
            label.setFont(normal);
        }
        @Override
        public void mousePressed(MouseEvent arg0) {}
        @Override
        public void mouseReleased(MouseEvent arg0) {}
    }
}
