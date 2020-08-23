// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bbox;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.tools.PlatformManager;

/**
 * This class controls the user input by listening to mouse and key events.
 * Currently implemented is: - zooming in and out with scrollwheel - zooming in
 * and centering by double clicking - selecting an area by clicking and dragging
 * the mouse
 *
 * @author Tim Haussmann
 */
public class SlippyMapControler extends MouseAdapter {

    /** A Timer for smoothly moving the map area */
    private static final Timer TIMER = new Timer(true);

    /** Does the moving */
    private MoveTask moveTask = new MoveTask();

    /** How often to do the moving (milliseconds) */
    private static final long timerInterval = 20;

    /** The maximum speed (pixels per timer interval) */
    private static final double MAX_SPEED = 20;

    /** The speed increase per timer interval when a cursor button is clicked */
    private static final double ACCELERATION = 0.10;

    private static final int MAC_MOUSE_BUTTON3_MASK = MouseEvent.CTRL_DOWN_MASK | MouseEvent.BUTTON1_DOWN_MASK;

    private static final String[] N = {
            ",", ".", "up", "right", "down", "left"};
    private static final int[] K = {
            KeyEvent.VK_COMMA, KeyEvent.VK_PERIOD, KeyEvent.VK_UP, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT};

    // start and end point of selection rectangle
    private Point iStartSelectionPoint;
    private Point iEndSelectionPoint;

    private final SlippyMapBBoxChooser iSlippyMapChooser;

    private boolean isSelecting;

    /**
     * Constructs a new {@code SlippyMapControler}.
     * @param navComp navigatable component
     * @param contentPane content pane
     */
    public SlippyMapControler(SlippyMapBBoxChooser navComp, JPanel contentPane) {
        iSlippyMapChooser = navComp;
        iSlippyMapChooser.addMouseListener(this);
        iSlippyMapChooser.addMouseMotionListener(this);

        if (contentPane != null) {
            for (int i = 0; i < N.length; ++i) {
                contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                        KeyStroke.getKeyStroke(K[i], KeyEvent.CTRL_DOWN_MASK), "MapMover.Zoomer." + N[i]);
            }
        }
        isSelecting = false;

        InputMap inputMap = navComp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = navComp.getActionMap();

        // map moving
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), "MOVE_RIGHT");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false), "MOVE_LEFT");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, false), "MOVE_UP");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, false), "MOVE_DOWN");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, true), "STOP_MOVE_HORIZONTALLY");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, true), "STOP_MOVE_HORIZONTALLY");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true), "STOP_MOVE_VERTICALLY");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true), "STOP_MOVE_VERTICALLY");

        // zooming. To avoid confusion about which modifier key to use,
        // we just add all keys left of the space bar
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.CTRL_DOWN_MASK, false), "ZOOM_IN");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.META_DOWN_MASK, false), "ZOOM_IN");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, InputEvent.ALT_DOWN_MASK, false), "ZOOM_IN");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, 0, false), "ZOOM_IN");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0, false), "ZOOM_IN");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, 0, false), "ZOOM_IN");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.SHIFT_DOWN_MASK, false), "ZOOM_IN");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.CTRL_DOWN_MASK, false), "ZOOM_OUT");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.META_DOWN_MASK, false), "ZOOM_OUT");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK, false), "ZOOM_OUT");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, 0, false), "ZOOM_OUT");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0, false), "ZOOM_OUT");

        // action mapping
        actionMap.put("MOVE_RIGHT", new MoveXAction(1));
        actionMap.put("MOVE_LEFT", new MoveXAction(-1));
        actionMap.put("MOVE_UP", new MoveYAction(-1));
        actionMap.put("MOVE_DOWN", new MoveYAction(1));
        actionMap.put("STOP_MOVE_HORIZONTALLY", new MoveXAction(0));
        actionMap.put("STOP_MOVE_VERTICALLY", new MoveYAction(0));
        actionMap.put("ZOOM_IN", new ZoomInAction());
        actionMap.put("ZOOM_OUT", new ZoomOutAction());
    }

    /**
     * Start drawing the selection rectangle if it was the 1st button (left button)
     */
    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1 && !(PlatformManager.isPlatformOsx() && e.getModifiersEx() == MAC_MOUSE_BUTTON3_MASK)) {
            iStartSelectionPoint = e.getPoint();
            iEndSelectionPoint = e.getPoint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (iStartSelectionPoint != null && (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == MouseEvent.BUTTON1_DOWN_MASK
                && !(PlatformManager.isPlatformOsx() && e.getModifiersEx() == MAC_MOUSE_BUTTON3_MASK)) {
            iEndSelectionPoint = e.getPoint();
            iSlippyMapChooser.setSelection(iStartSelectionPoint, iEndSelectionPoint);
            isSelecting = true;
        }
    }

    /**
     * When dragging the map change the cursor back to it's pre-move cursor. If
     * a double-click occurs center and zoom the map on the clicked location.
     */
    @Override
    public void mouseReleased(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON1) {

            if (isSelecting && e.getClickCount() == 1) {
                iSlippyMapChooser.setSelection(iStartSelectionPoint, e.getPoint());

                // reset the selections start and end
                iEndSelectionPoint = null;
                iStartSelectionPoint = null;
                isSelecting = false;

            } else {
                iSlippyMapChooser.handleAttribution(e.getPoint(), true);
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        iSlippyMapChooser.handleAttribution(e.getPoint(), false);
    }

    private class MoveXAction extends AbstractAction {

        private final int direction;

        MoveXAction(int direction) {
            this.direction = direction;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            moveTask.setDirectionX(direction);
        }
    }

    private class MoveYAction extends AbstractAction {

        private final int direction;

        MoveYAction(int direction) {
            this.direction = direction;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            moveTask.setDirectionY(direction);
        }
    }

    /** Moves the map depending on which cursor keys are pressed (or not) */
    private class MoveTask extends TimerTask {
        /** The current x speed (pixels per timer interval) */
        private double speedX = 1;

        /** The current y speed (pixels per timer interval) */
        private double speedY = 1;

        /** The horizontal direction of movement, -1:left, 0:stop, 1:right */
        private int directionX;

        /** The vertical direction of movement, -1:up, 0:stop, 1:down */
        private int directionY;

        /**
         * Indicated if <code>moveTask</code> is currently enabled (periodically
         * executed via timer) or disabled
         */
        protected boolean scheduled;

        protected void setDirectionX(int directionX) {
            this.directionX = directionX;
            updateScheduleStatus();
        }

        protected void setDirectionY(int directionY) {
            this.directionY = directionY;
            updateScheduleStatus();
        }

        private void updateScheduleStatus() {
            boolean newMoveTaskState = !(directionX == 0 && directionY == 0);

            if (newMoveTaskState != scheduled) {
                scheduled = newMoveTaskState;
                if (newMoveTaskState) {
                    TIMER.schedule(this, 0, timerInterval);
                } else {
                    // We have to create a new instance because rescheduling a
                    // once canceled TimerTask is not possible
                    moveTask = new MoveTask();
                    cancel(); // Stop this TimerTask
                }
            }
        }

        @Override
        public void run() {
            // update the x speed
            switch (directionX) {
            case -1:
                if (speedX > -1) {
                    speedX = -1;
                }
                if (speedX > -1 * MAX_SPEED) {
                    speedX -= ACCELERATION;
                }
                break;
            case 0:
                speedX = 0;
                break;
            case 1:
                if (speedX < 1) {
                    speedX = 1;
                }
                if (speedX < MAX_SPEED) {
                    speedX += ACCELERATION;
                }
                break;
            default:
                throw new IllegalStateException(Integer.toString(directionX));
            }

            // update the y speed
            switch (directionY) {
            case -1:
                if (speedY > -1) {
                    speedY = -1;
                }
                if (speedY > -1 * MAX_SPEED) {
                    speedY -= ACCELERATION;
                }
                break;
            case 0:
                speedY = 0;
                break;
            case 1:
                if (speedY < 1) {
                    speedY = 1;
                }
                if (speedY < MAX_SPEED) {
                    speedY += ACCELERATION;
                }
                break;
            default:
                throw new IllegalStateException(Integer.toString(directionY));
            }

            // move the map
            int moveX = (int) Math.floor(speedX);
            int moveY = (int) Math.floor(speedY);
            if (moveX != 0 || moveY != 0) {
                iSlippyMapChooser.moveMap(moveX, moveY);
            }
        }
    }

    private class ZoomInAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            iSlippyMapChooser.zoomIn();
        }
    }

    private class ZoomOutAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            iSlippyMapChooser.zoomOut();
        }
    }
}
