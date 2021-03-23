// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import org.openstreetmap.josm.tools.GBC;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Allows using an icon as well as a text on a {@link JCheckBox}
 */
public interface IconTextCheckBox {

    /**
     * Wraps the checkbox to display an icon as well as a text
     * @param check the checkbox
     * @param text the label text to display
     * @param icon the icon to display
     * @return a wrapping component
     */
    static JPanel wrap(JCheckBox check, String text, Icon icon) {
        JPanel panel = new JPanel(new GridBagLayout());
        JLabel label = new JLabel(text, icon, SwingConstants.LEADING);

        panel.add(check, GBC.std());
        panel.add(label);
        panel.add(new JLabel(), GBC.eol().fill());

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    return;
                }
                if (check instanceof QuadStateCheckBox) {
                    ((QuadStateCheckBox) check).nextState();
                } else {
                    check.setSelected(!check.isSelected());
                }
            }
        });

        return panel;
    }
}
