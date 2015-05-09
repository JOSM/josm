// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import javax.swing.JTable;

import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType;
import org.openstreetmap.josm.gui.dialogs.relation.sort.WayConnectionType.Direction;
import org.openstreetmap.josm.tools.ImageProvider;

public class MemberTableLinkedCellRenderer extends MemberTableCellRenderer {

    private static final Image arrowUp = ImageProvider.get("dialogs/relation", "arrowup").getImage();
    private static final Image arrowDown = ImageProvider.get("dialogs/relation", "arrowdown").getImage();
    private static final Image corners = ImageProvider.get("dialogs/relation", "roundedcorners").getImage();
    private static final Image roundabout_right = ImageProvider.get("dialogs/relation", "roundabout_right_tiny").getImage();
    private static final Image roundabout_left = ImageProvider.get("dialogs/relation", "roundabout_left_tiny").getImage();
    private transient WayConnectionType value = new WayConnectionType();

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        reset();
        if (value == null)
            return this;

        this.value = (WayConnectionType) value;
        renderForeground(isSelected);
        setToolTipText(((WayConnectionType)value).getToolTip());
        renderBackground(getModel(table), null, isSelected);
        return this;
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (value == null || !value.isValid())
            return;

        int ymax=this.getSize().height - 1;
        int xloop = 10;
        int xowloop = 0;
        if(value.isOnewayLoopForwardPart) {
            xowloop = -3;
        }
        if(value.isOnewayLoopBackwardPart) {
            xowloop = 3;
        }

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
            if(value.isOnewayHead) {
                g.fillRect(xoff - 1, 0, 3, 1);
            } else {
                g.fillRect(xoff - 1 + xowloop, 0, 3, 1);
            }
            y1 = 0;
        } else {
            if (value.isLoop) {
                g.setColor(Color.black);
                y1 = 5;
                g.drawImage(corners,xoff,y1-3,xoff+3,y1, 0,0,3,3, new Color(0,0,0,0), null);
                g.drawImage(corners,xoff+xloop-2,y1-3,xoff+xloop+1,y1, 2,0,5,3, new Color(0,0,0,0), null);
                g.drawLine(xoff+3,y1-3,xoff+xloop-3,y1-3);
            } else {
                g.setColor(Color.red);
                if(value.isOnewayHead) {
                    g.drawRect(xoff-1, p - 3 - w, w, w);
                } else {
                    g.drawRect(xoff-1 + xowloop, p - 1 - w, w, w);
                }
                y1 = p;
            }
        }

        if (value.linkNext) {
            g.setColor(Color.black);
            if(value.isOnewayTail) {
                g.fillRect(xoff - 1, ymax, 3, 1);
            } else {
                g.fillRect(xoff - 1 + xowloop, ymax, 3, 1);
            }
            y2 = ymax;
        } else {
            if (value.isLoop) {
                g.setColor(Color.black);
                y2 = ymax - 5;
                g.fillRect(xoff-1, y2+2, 3, 3);
                g.drawLine(xoff, y2, xoff, y2+2);
                g.drawImage(corners,xoff+xloop-2,y2+1,xoff+xloop+1,y2+4, 2,2,5,5, new Color(0,0,0,0), null);
                g.drawLine(xoff+3-1,y2+3,xoff+xloop-3,y2+3);
            } else {
                g.setColor(Color.red);
                if(value.isOnewayTail) {
                    g.drawRect(xoff-1, ymax - p + 3, w, w);
                } else {
                    g.drawRect(xoff-1 + xowloop, ymax - p + 1, w, w);
                }
                y2 = ymax - p;
            }
        }

        /* vertical lines */
        g.setColor(Color.black);
        if (value.isLoop) {
            g.drawLine(xoff+xloop, y1, xoff+xloop, y2);
        }

        if (value.isOnewayHead) {
            setDotted(g);
            y1 = 7;

            int[] xValues  = {xoff - xowloop + 1, xoff - xowloop + 1, xoff};
            int[] yValues  = {ymax, y1+1, 1};
            g.drawPolyline(xValues, yValues, 3);
            unsetDotted(g);
            g.drawLine(xoff + xowloop, y1+1, xoff, 1);
        }

        if(value.isOnewayTail){
            setDotted(g);
            y2 = ymax - 7;

            int[] xValues  = {xoff+1, xoff - xowloop + 1, xoff - xowloop + 1};
            int[] yValues  = {ymax-1, y2, y1};
            g.drawPolyline(xValues, yValues, 3);
            unsetDotted(g);
            g.drawLine(xoff + xowloop, y2, xoff, ymax-1);
        }

        if ((value.isOnewayLoopForwardPart || value.isOnewayLoopBackwardPart) && !value.isOnewayTail && !value.isOnewayHead) {
            setDotted(g);
            g.drawLine(xoff - xowloop+1, y1, xoff - xowloop+1, y2 + 1);
            unsetDotted(g);
        }

        if (!value.isOnewayLoopForwardPart && !value.isOnewayLoopBackwardPart){
            g.drawLine(xoff, y1, xoff, y2);
        }

        g.drawLine(xoff+xowloop, y1, xoff+xowloop, y2);

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
        if (value.direction == Direction.ROUNDABOUT_LEFT) {
            g.drawImage(roundabout_left, xoff-6, 1, null);
        } else if (value.direction == Direction.ROUNDABOUT_RIGHT) {
            g.drawImage(roundabout_right, xoff-6, 1, null);
        }

        if (!value.isOnewayLoopForwardPart && !value.isOnewayLoopBackwardPart &&
                (arrow != null)) {
            g.drawImage(arrow, xoff-3, (y1 + y2) / 2 - 2, null);
        }

        if (value.isOnewayLoopBackwardPart && value.isOnewayLoopForwardPart) {
            if(arrow == arrowDown) {
                arrow = arrowUp;
            } else if (arrow == arrowUp) {
                arrow = arrowDown;
            }
        }

        if ((arrow != null)) {
            g.drawImage(arrow, xoff+xowloop-3, (y1 + y2) / 2 - 2, null);
        }
    }

    private void setDotted(Graphics g) {
        ((Graphics2D)g).setStroke(new BasicStroke(
                1f,
                BasicStroke.CAP_BUTT,
                BasicStroke.CAP_BUTT,
                5f,
                new float[] {1f, 2f},
                0f));
    }

    private void unsetDotted(Graphics g) {
        ((Graphics2D)g).setStroke(new BasicStroke());
    }
}
