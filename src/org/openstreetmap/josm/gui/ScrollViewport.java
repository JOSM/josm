// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
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

/**
 * A viewport with UP and DOWN arrow buttons, so that the user can make the
 * content scroll.
 *
 * This should be used for long, vertical toolbars.
 */
public class ScrollViewport extends JPanel {

    private static final int NO_SCROLL = 0;

    /**
     * Direction flag for upwards
     */
    public static final int UP_DIRECTION = 1;
    /**
     * Direction flag for downwards
     */
    public static final int DOWN_DIRECTION = 2;
    /**
     * Direction flag for left
     */
    public static final int LEFT_DIRECTION = 4;
    /**
     * Direction flag for right
     */
    public static final int RIGHT_DIRECTION = 8;
    /**
     * Allow vertical scrolling
     */
    public static final int VERTICAL_DIRECTION = UP_DIRECTION | DOWN_DIRECTION;

    /**
     * Allow horizontal scrolling
     */
    public static final int HORIZONTAL_DIRECTION = LEFT_DIRECTION | RIGHT_DIRECTION;

    /**
     * Allow scrolling in both directions
     */
    public static final int ALL_DIRECTION = HORIZONTAL_DIRECTION | VERTICAL_DIRECTION;

    private class ScrollViewPortMouseListener extends MouseAdapter {
        private final int direction;

        ScrollViewPortMouseListener(int direction) {
            this.direction = direction;
        }

        @Override
        public void mouseExited(MouseEvent e) {
            mouseReleased(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            ScrollViewport.this.scrollDirection = NO_SCROLL;
            timer.stop();
        }

        @Override
        public void mousePressed(MouseEvent e) {
            ScrollViewport.this.scrollDirection = direction;
            scroll();
            timer.restart();
        }
    }

    private final JViewport vp = new JViewport();
    private JComponent component;

    private final List<JButton> buttons = new ArrayList<>();

    private final Timer timer = new Timer(100, evt -> scroll());

    private int scrollDirection = NO_SCROLL;

    private final int allowedScrollDirections;

    private final ComponentAdapter refreshButtonsOnResize = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            showOrHideButtons();
        }
    };

    /**
     * Create a new scroll viewport
     * @param c The component to display as content.
     * @param direction The direction to scroll.
     *        Should be one of {@link #VERTICAL_DIRECTION}, {@link #HORIZONTAL_DIRECTION}, {@link #ALL_DIRECTION}
     */
    public ScrollViewport(JComponent c, int direction) {
        this(direction);
        add(c);
    }

    /**
     * Create a new scroll viewport
     * @param direction The direction to scroll.
     *        Should be one of {@link #VERTICAL_DIRECTION}, {@link #HORIZONTAL_DIRECTION}, {@link #ALL_DIRECTION}
     */
    public ScrollViewport(int direction) {
        super(new BorderLayout());
        this.allowedScrollDirections = direction;

        // UP
        if ((direction & UP_DIRECTION) != 0) {
            addScrollButton(UP_DIRECTION, /* ICON */ "svpUp", BorderLayout.NORTH);
        }

        // DOWN
        if ((direction & DOWN_DIRECTION) != 0) {
            addScrollButton(DOWN_DIRECTION, /* ICON */ "svpDown", BorderLayout.SOUTH);
        }

        // LEFT
        if ((direction & LEFT_DIRECTION) != 0) {
            addScrollButton(LEFT_DIRECTION, /* ICON */ "svpLeft", BorderLayout.WEST);
        }

        // RIGHT
        if ((direction & RIGHT_DIRECTION) != 0) {
            addScrollButton(RIGHT_DIRECTION, /* ICON */ "svpRight", BorderLayout.EAST);
        }

        add(vp, BorderLayout.CENTER);

        this.addComponentListener(refreshButtonsOnResize);

        showOrHideButtons();

        if ((direction & VERTICAL_DIRECTION) != 0) {
            addMouseWheelListener(e -> scroll(0, e.getUnitsToScroll() * 5));
        } else if ((direction & HORIZONTAL_DIRECTION) != 0) {
            addMouseWheelListener(e -> scroll(e.getUnitsToScroll() * 5, 0));
        }

        timer.setRepeats(true);
        timer.setInitialDelay(400);
    }

    private void addScrollButton(int direction, String icon, String borderLayoutPosition) {
        JButton button = new JButton();
        button.addMouseListener(new ScrollViewPortMouseListener(direction));
        button.setPreferredSize(new Dimension(10, 10));
        button.setIcon(ImageProvider.get(icon));
        add(button, borderLayoutPosition);
        buttons.add(button);
    }

    /**
     * Scrolls in the currently selected scroll direction.
     */
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
        default: // Do nothing
        }

        scroll(deltaX, deltaY);
    }

    /**
     * Scrolls by the given offset
     * @param deltaX offset x
     * @param deltaY offset y
     */
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
        boolean needButtons = false;
        if ((allowedScrollDirections & VERTICAL_DIRECTION) != 0) {
            needButtons |= getViewSize().height > getViewRect().height;
        }
        if ((allowedScrollDirections & HORIZONTAL_DIRECTION) != 0) {
            needButtons |= getViewSize().width > getViewRect().width;
        }
        for (JButton b : buttons) {
            b.setVisible(needButtons);
        }
    }

    /**
     * Gets the current visible part of the view
     * @return The current view rect
     */
    public Rectangle getViewRect() {
        return vp.getViewRect();
    }

    /**
     * Gets the size of the view
     * @return The size
     */
    public Dimension getViewSize() {
        return vp.getViewSize();
    }

    /**
     * Gets the position (offset) of the view area
     * @return The offset
     */
    public Point getViewPosition() {
        return vp.getViewPosition();
    }

    @Override
    public Dimension getPreferredSize() {
        if (component == null) {
            return vp.getPreferredSize();
        } else {
            return component.getPreferredSize();
        }
    }

    @Override
    public Dimension getMinimumSize() {
        if (component == null) {
            return vp.getMinimumSize();
        } else {
            Dimension minSize = component.getMinimumSize();
            if ((allowedScrollDirections & HORIZONTAL_DIRECTION) != 0) {
                minSize = new Dimension(20, minSize.height);
            }
            if ((allowedScrollDirections & VERTICAL_DIRECTION) != 0) {
                minSize = new Dimension(minSize.width, 20);
            }
            return minSize;
        }
    }

    /**
     * Sets the component to be used as content.
     * @param c The component
     */
    public void add(JComponent c) {
        vp.removeAll();
        if (this.component != null) {
            this.component.removeComponentListener(refreshButtonsOnResize);
        }
        this.component = c;
        c.addComponentListener(refreshButtonsOnResize);
        vp.add(c);
    }
}
