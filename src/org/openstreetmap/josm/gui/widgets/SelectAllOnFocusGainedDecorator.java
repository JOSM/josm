// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.text.JTextComponent;

public class SelectAllOnFocusGainedDecorator extends FocusAdapter{

    public static void decorate(JTextComponent tc) {
        if (tc == null) return;
        tc.addFocusListener(new SelectAllOnFocusGainedDecorator());
    }

    @Override
    public void focusGained(FocusEvent e) {
        Component c = e.getComponent();
        if (c instanceof JTextComponent) {
            JTextComponent tc = (JTextComponent)c;
            tc.selectAll();
        }
    }
}
