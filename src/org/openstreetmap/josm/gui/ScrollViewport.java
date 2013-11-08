// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Timer;

import org.openstreetmap.josm.tools.ImageProvider;

/** A viewport with UP and DOWN arrow buttons, so that the user can make the
 * content scroll.
 */
public class ScrollViewport extends JPanel {

    private static final int NO_SCROLL = 0;

    public static final int UP_DIRECTION = 1;
    public static final int DOWN_DIRECTION = 2;
    public static final int LEFT_DIRECTION = 4;
    public static final int RIGHT_DIRECTION = 8;
    public static final int VERTICAL_DIRECTION = UP_DIRECTION | DOWN_DIRECTION;
    public static final int HORIZONTAL_DIRECTION = LEFT_DIRECTION | RIGHT_DIRECTION;
    public static final int ALL_DIRECTION = HORIZONTAL_DIRECTION | VERTICAL_DIRECTION;

    private class ScrollViewPortMouseListener extends MouseAdapter {
        private int direction;

        public ScrollViewPortMouseListener(int direction) {
            this.direction = direction;
        }

        @Override public void mouseExited(MouseEvent arg0) {
            ScrollViewport.this.scrollDirection = NO_SCROLL;
            timer.stop();
        }

        @Override public void mouseReleased(MouseEvent arg0) {
            ScrollViewport.this.scrollDirection = NO_SCROLL;
            timer.stop();
        }

        @Override public void mousePressed(MouseEvent arg0) {
            ScrollViewport.this.scrollDirection = direction;
            scroll();
            timer.restart();
        }

    }

    private JViewport vp = new JViewport();
    private JComponent component = null;

    private List<JButton> buttons = new ArrayList<JButton>();

    private Timer timer = new Timer(100, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            ScrollViewport.this.scroll();
        }
    });

    private int scrollDirection = NO_SCROLL;

    public ScrollViewport(JComponent c, int direction) {
        this(direction);
        add(c);
    }

    public ScrollViewport(int direction) {
        setLayout(new BorderLayout());

        JButton button;

        // UP
        if ((direction & UP_DIRECTION) > 0) {
            button = new JButton();
            button.addMouseListener(new ScrollViewPortMouseListener(UP_DIRECTION));
            button.setPreferredSize(new Dimension(10,10));
            button.setIcon(ImageProvider.get("svpUp"));
            add(button, BorderLayout.NORTH);
            buttons.add(button);
        }

        // DOWN
        if ((direction & DOWN_DIRECTION) > 0) {
            button = new JButton();
            button.addMouseListener(new ScrollViewPortMouseListener(DOWN_DIRECTION));
            button.setPreferredSize(new Dimension(10,10));
            button.setIcon(ImageProvider.get("svpDown"));
            add(button, BorderLayout.SOUTH);
            buttons.add(button);
        }

        // LEFT
        if ((direction & LEFT_DIRECTION) > 0) {
            button = new JButton();
            button.addMouseListener(new ScrollViewPortMouseListener(LEFT_DIRECTION));
            button.setPreferredSize(new Dimension(10,10));
            button.setIcon(ImageProvider.get("svpLeft"));
            add(button, BorderLayout.WEST);
            buttons.add(button);
        }

        // RIGHT
        if ((direction & RIGHT_DIRECTION) > 0) {
            button = new JButton();
            button.addMouseListener(new ScrollViewPortMouseListener(RIGHT_DIRECTION));
            button.setPreferredSize(new Dimension(10,10));
            button.setIcon(ImageProvider.get("svpRight"));
            add(button, BorderLayout.EAST);
            buttons.add(button);
        }

        add(vp, BorderLayout.CENTER);

        this.addComponentListener(new ComponentAdapter() {
            @Override public void  componentResized(ComponentEvent e) {
                showOrHideButtons();
            }
        });

        showOrHideButtons();

        timer.setRepeats(true);
        timer.setInitialDelay(400);
    }

    public synchronized void scroll() {
        int direction = scrollDirection;

        if (component == null || direction == NO_SCROLL)
            return;

        Rectangle viewRect = vp.getViewRect();

        int deltaX = 0;
        int deltaY = 0;

        if (direction < LEFT_DIRECTION) {
            deltaY = viewRect.height * 2 / 7;
        } else {
            deltaX = viewRect.width * 2 / 7;
        }

        switch (direction) {
        case UP_DIRECTION :
            deltaY *= -1;
            break;
        case LEFT_DIRECTION :
            deltaX *= -1;
            break;
        }

        scroll(deltaX, deltaY);
    }
    public synchronized void scroll(int deltaX, int deltaY) {
        if (component == null)
            return;
        Dimension compSize = component.getSize();
        Rectangle viewRect = vp.getViewRect();

        int newX = viewRect.x + deltaX;
        int newY = viewRect.y + deltaY;

        if (newY < 0) {
            newY = 0;
        }
        if (newY > compSize.height - viewRect.height) {
            newY = compSize.height - viewRect.height;
        }
        if (newX < 0) {
            newX = 0;
        }
        if (newX > compSize.width - viewRect.width) {
            newX = compSize.width - viewRect.width;
        }

        vp.setViewPosition(new Point(newX, newY));
    }

    /**
     * Update the visibility of the buttons
     * Only show them if the Viewport is too small for the content.
     */
    public void showOrHideButtons() {
        boolean needButtons = vp.getViewSize().height > vp.getViewRect().height ||
        vp.getViewSize().width > vp.getViewRect().width;
        for (JButton b : buttons) {
            b.setVisible(needButtons);
        }
    }

    public Rectangle getViewRect() {
        return vp.getViewRect();
    }

    public Dimension getViewSize() {
        return vp.getViewSize();
    }

    public Point getViewPosition() {
        return vp.getViewPosition();
    }

    public void add(JComponent c) {
        vp.removeAll();
        this.component = c;
        vp.add(c);
    }
}
