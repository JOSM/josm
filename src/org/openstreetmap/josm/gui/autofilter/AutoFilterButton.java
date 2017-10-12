// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.autofilter;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.gui.MainApplication;

/**
 * A button associated to an auto filter. If clicked twice, the filter is reset.
 * @since 12400
 */
public class AutoFilterButton extends JButton {

    private static final NamedColorProperty PROP_COLOR = new NamedColorProperty("auto.filter.button.color", new Color(0, 160, 160));

    private final AutoFilter filter;

    /**
     * Constructs a new {@code AutoFilterButton}.
     * @param filter auto filter associated to this button
     */
    public AutoFilterButton(final AutoFilter filter) {
        super(new JosmAction(filter.getLabel(), null, filter.getDescription(), null, false) {
            @Override
            public synchronized void actionPerformed(ActionEvent e) {
                AutoFilterManager afm = AutoFilterManager.getInstance();
                if (filter.equals(afm.getCurrentAutoFilter())) {
                    afm.setCurrentAutoFilter(null);
                    MainApplication.getMap().filterDialog.getFilterModel().executeFilters();
                } else {
                    afm.setCurrentAutoFilter(filter);
                }
            }
        });
        this.filter = filter;
        setForeground(Color.WHITE);
        setContentAreaFilled(false);
        setBorder(BorderFactory.createEmptyBorder(7, 7, 7, 7));
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (getModel().isPressed()) {
            g.setColor(PROP_COLOR.get().darker().darker());
        } else if (getModel().isRollover() || AutoFilterManager.getInstance().getCurrentAutoFilter() == filter) {
            g.setColor(PROP_COLOR.get().darker());
        } else {
            g.setColor(PROP_COLOR.get());
        }
        if (g instanceof Graphics2D) {
            ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        g.fillRoundRect(0, 0, getWidth(), getHeight(), 3, 3);
        super.paintComponent(g);
    }
}
