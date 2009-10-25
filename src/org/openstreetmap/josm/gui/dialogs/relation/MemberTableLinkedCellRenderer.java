// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JTable;

import org.openstreetmap.josm.tools.ImageProvider;

public class MemberTableLinkedCellRenderer extends MemberTableCellRenderer {

    final static Image arrowUp = ImageProvider.get("dialogs", "arrowup").getImage();
    final static Image arrowDown = ImageProvider.get("dialogs", "arrowdown").getImage();
    private WayConnectionType value = new WayConnectionType();

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        reset();
        this.value = (WayConnectionType) value;
        renderForeground(isSelected);
        //setText(value.toString());
        setToolTipText(((WayConnectionType)value).getToolTip());
        renderBackground(getModel(table), null, isSelected);
        return this;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (value == null || value.invalid) {
            return;
        }

        Image image = null;
        switch (value.direction) {
            case 1:
                image = arrowDown;
                break;
            case -1:
                image = arrowUp;
                break;
        }

        int ymax=this.getSize().height - 1;
        int xoff = this.getSize().width / 2;
        int w = 2;
        int p = 2 + w + 1;
        int y1 = 0;
        int y2 = 0;

        if (image != null && (value.connectedToPrevious || value.connectedToNext)) {
            g.drawImage(image, xoff-3, ymax / 2 - 2, null);
        }

        if (value.connectedToPrevious) {
            g.setColor(Color.black);
            g.fillRect(xoff - 2, 0, 5, 2);
            y1 = 0;
        } else {
            g.setColor(Color.red);
            g.drawRect(xoff-1, p - 1 - w, w, w);
            y1 = p;
        }

        if (value.connectedToNext) {
            g.setColor(Color.black);
            g.fillRect(xoff - 2, ymax - 1, 5, 2);
            y2 = ymax;
        } else {
            g.setColor(Color.red);
            g.drawRect(xoff-1, ymax - p + 1, w, w);
            y2 = ymax - p;
        }
        g.setColor(Color.black);
        g.drawLine(xoff, y1, xoff, y2);
    }
}
