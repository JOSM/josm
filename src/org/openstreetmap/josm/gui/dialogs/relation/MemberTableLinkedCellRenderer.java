// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JTable;

import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.gui.dialogs.relation.WayConnectionType.Direction;

public class MemberTableLinkedCellRenderer extends MemberTableCellRenderer {

    final static Image arrowUp = ImageProvider.get("dialogs/relation", "arrowup").getImage();
    final static Image arrowDown = ImageProvider.get("dialogs/relation", "arrowdown").getImage();
    final static Image corners = ImageProvider.get("dialogs/relation", "roundedcorners").getImage();
    final static Image roundabout_right = ImageProvider.get("dialogs/relation", "roundabout_right_tiny").getImage();
    final static Image roundabout_left = ImageProvider.get("dialogs/relation", "roundabout_left_tiny").getImage();
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
        if (value == null || !value.isValid()) {
            return;
        }

        int ymax=this.getSize().height - 1;
        int xloop = 8;
        int xoff = this.getSize().width / 2;
        if (value.isLoop) {
            xoff -= xloop / 2 - 1;
        }
        int w = 2;
        int p = 2 + w + 1;
        int y1 = 0;
        int y2 = 0;

        if (value.linkPrev) {
            g.setColor(Color.black);
            g.fillRect(xoff - 1, 0, 3, 1);
            y1 = 0;
        } else {
            if (value.isLoop) {
                g.setColor(Color.black);
                y1 = 5;
                g.drawImage(corners,xoff,y1-3,xoff+3,y1, 0,0,3,3, new Color(0,0,0,0), null);
                g.drawImage(corners,xoff+xloop-2,y1-3,xoff+xloop+1,y1, 2,0,5,3, new Color(0,0,0,0), null);
                g.drawLine(xoff+3,y1-3,xoff+xloop-3,y1-3);
            }
            else {
                g.setColor(Color.red);
                g.drawRect(xoff-1, p - 1 - w, w, w);
                y1 = p;
            }
        }

        if (value.linkNext) {
            g.setColor(Color.black);
            g.fillRect(xoff - 1, ymax, 3, 1);
            y2 = ymax;
        } else {
            if (value.isLoop) {
                g.setColor(Color.black);
                y2 = ymax - 5;
                g.fillRect(xoff-1, y2+2, 3, 3);
                g.drawLine(xoff, y2, xoff, y2+2);
                g.drawImage(corners,xoff+xloop-2,y2+1,xoff+xloop+1,y2+4, 2,2,5,5, new Color(0,0,0,0), null);
                g.drawLine(xoff+3-1,y2+3,xoff+xloop-3,y2+3);
            }
            else {
                g.setColor(Color.red);
                g.drawRect(xoff-1, ymax - p + 1, w, w);
                y2 = ymax - p;
            }
        }

        /* vertical lines */
        g.setColor(Color.black);
        g.drawLine(xoff, y1, xoff, y2);
        if (value.isLoop) {
            g.drawLine(xoff+xloop, y1, xoff+xloop, y2);
        }

        /* special icons */
        Image arrow = null;
        switch (value.direction) {
            case FORWARD:
                arrow = arrowDown;
                break;
            case BACKWARD:
                arrow = arrowUp;
                break;
        }
        if ((arrow != null) && (value.linkPrev || value.linkNext)) {
            g.drawImage(arrow, xoff-3, (y1 + y2) / 2 - 2, null);
        }
        else if (value.direction == Direction.ROUNDABOUT_LEFT) {
            g.drawImage(roundabout_left, xoff-6, 1, null);
        } else if (value.direction == Direction.ROUNDABOUT_RIGHT) {
            g.drawImage(roundabout_right, xoff-6, 1, null);
        }
    }
}
