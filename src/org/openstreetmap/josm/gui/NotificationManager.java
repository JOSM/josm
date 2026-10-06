// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.RoundRectangle2D;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.gui.help.HelpBrowser;
import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.GuiSizesHelper;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * Manages {@link Notification}s, i.e.&nbsp;displays them on screen.
 * <p>
 * Don't use this class directly, but use {@link Notification#show()}.
 * <p>
 * If multiple messages are sent in a short period of time, they are put in
 * a queue and displayed one after the other.
 * <p>
 * The user can stop the timer (freeze the message) by moving the mouse cursor
 * above the panel. As a visual cue, the background color changes from
 * semi-transparent to opaque while the timer is frozen.
 */
class NotificationManager {

    private final Timer hideTimer; // started when message is shown, responsible for hiding the message
    private final Timer pauseTimer; // makes sure, there is a small pause between two consecutive messages
    private final Timer unfreezeDelayTimer; // tiny delay before resuming the timer when mouse cursor is moved off the panel
    private boolean running;

    private Notification currentNotification;
    private NotificationPanel currentNotificationPanel;

    /** the component {@link #currentNotificationPanel} is aligned to, {@code null} while nothing is displayed */
    private Component notificationAnchor;

    /** keeps the displayed notification aligned when the layout around it changes, e.g. in fullscreen mode */
    private final ComponentListener anchorListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            updateNotificationPosition();
        }

        @Override
        public void componentMoved(ComponentEvent e) {
            updateNotificationPosition();
        }
    };

    /** realigns the displayed notification when toggling fullscreen, before the window is painted with it at its old position */
    private final HierarchyListener showingListener = e -> {
        if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && e.getComponent().isShowing()) {
            updateNotificationPosition();
        }
    };

    /** brings the displayed notification over to the map view as soon as one is opened, and back when it is closed */
    private final MapFrameListener mapFrameListener = (oldFrame, newFrame) -> updateNotificationPosition();

    private final Deque<Notification> queue;

    private static final IntegerProperty pauseTime = new IntegerProperty("notification-default-pause-time-ms", 300); // milliseconds

    private long displayTimeStart;
    private long elapsedTime;

    private static NotificationManager instance;

    /** margin between the notification panel and the borders of its anchor, in unscaled pixels */
    private static final int MARGIN = 10;

    private static final Color PANEL_SEMITRANSPARENT = new Color(224, 236, 249, 230);
    private static final Color PANEL_OPAQUE = new Color(224, 236, 249);

    NotificationManager() {
        queue = new LinkedList<>();
        hideTimer = new Timer(Notification.TIME_DEFAULT, e -> this.stopHideTimer(null));
        hideTimer.setRepeats(false);
        pauseTimer = new Timer(pauseTime.get(), new PauseFinishedEvent());
        pauseTimer.setRepeats(false);
        unfreezeDelayTimer = new Timer(10, new UnfreezeEvent());
        unfreezeDelayTimer.setRepeats(false);
    }

    /**
     * Show the given notification (unless a duplicate notification is being shown at the moment or is already queued)
     * @param note The note to show.
     * @see Notification#show()
     */
    void showNotification(Notification note) {
        synchronized (queue) {
            if (Objects.equals(note, currentNotification) || queue.contains(note)) {
                Logging.debug("Dropping duplicate notification {0}", note);
                return;
            }
            queue.add(note);
        }
        // must not run while the monitor is held, see processQueue()
        processQueue();
    }

    /**
     * Show the given notification by replacing the given queued/displaying notification
     * @param oldNotification the notification to replace
     * @param newNotification the notification to show
     */
    void replaceExistingNotification(Notification oldNotification, Notification newNotification) {
        boolean isDisplayed;
        synchronized (queue) {
            isDisplayed = Objects.equals(oldNotification, currentNotification);
            if (!isDisplayed) {
                queue.remove(oldNotification);
            }
        }
        if (isDisplayed) {
            // must not run while the monitor is held either, it waits for the EDT as well
            stopHideTimer(oldNotification);
        }
        showNotification(newNotification);
    }

    /**
     * Displays the next queued notification, unless one is being displayed already or the queue is empty.
     * <p>
     * Only the state transition is guarded by the monitor of {@link #queue}. The rest waits for the EDT, and the EDT
     * takes that very monitor as well, so holding it any longer would deadlock every caller
     * that is not the EDT itself.
     */
    private void processQueue() {
        synchronized (queue) {
            if (running) return;

            currentNotification = queue.poll();
            if (currentNotification == null) return;

            // claim the slot before releasing the monitor, so that no concurrent call displays a second notification
            running = true;
        }

        GuiHelper.runInEDTAndWait(() -> {
            currentNotificationPanel = new NotificationPanel(currentNotification, new FreezeMouseListener(), e -> this.stopHideTimer(null));
            currentNotificationPanel.addHierarchyListener(showingListener);
            currentNotificationPanel.validate();

            currentNotificationPanel.setSize(currentNotificationPanel.getPreferredSize());

            JFrame parentWindow = MainApplication.getMainFrame();
            if (parentWindow != null) {
                parentWindow.getLayeredPane().add(currentNotificationPanel, JLayeredPane.POPUP_LAYER, 0);
                MainApplication.addMapFrameListener(mapFrameListener);
                updateNotificationPosition();
            }
            currentNotificationPanel.setVisible(true);
        });

        elapsedTime = 0;
        startHideTimer();
    }

    /**
     * Aligns the displayed notification to the map view, or to the content pane while no map view is displayed,
     * and keeps listening to that anchor for as long as the notification is displayed.
     */
    private void updateNotificationPosition() {
        JFrame parentWindow = MainApplication.getMainFrame();
        if (currentNotificationPanel == null || parentWindow == null) {
            return;
        }
        Component anchor = MainApplication.isDisplayingMapView() ? MainApplication.getMap().mapView : parentWindow.getContentPane();
        if (anchor != notificationAnchor) {
            // the map view is created and destroyed along with the layers, so the anchor may change while displaying
            detachAnchorListener();
            notificationAnchor = anchor;
            anchor.addComponentListener(anchorListener);
        }
        // a map view that has just been created is not laid out yet; its first resize event brings the notification over
        Component target = anchor.getHeight() > 0 ? anchor : parentWindow.getContentPane();
        currentNotificationPanel.setLocation(getNotificationPosition(target, parentWindow.getLayeredPane(),
                currentNotificationPanel.getSize(), GuiSizesHelper.getSizeDpiAdjusted(MARGIN)));
    }

    /**
     * Stops listening to the anchor of the notification that is no longer displayed.
     */
    private void detachAnchorListener() {
        if (notificationAnchor != null) {
            notificationAnchor.removeComponentListener(anchorListener);
            notificationAnchor = null;
        }
    }

    /**
     * Computes the position of a notification panel in the coordinate system of {@code container}, which unlike
     * the one of the main window does not depend on the window decorations, i.e. on fullscreen mode.
     * <p>
     * The panel is aligned to the bottom left corner of {@code anchor}, or to its top left corner if it is taller,
     * so that the beginning of a long message stays readable.
     *
     * @param anchor the component the notification is aligned to, e.g. the map view
     * @param container the container the notification panel is added to
     * @param size the size of the notification panel
     * @param margin the margin to keep between the notification panel and the borders of {@code anchor}
     * @return the location of the upper left corner of the notification panel
     */
    static Point getNotificationPosition(Component anchor, Container container, Dimension size, int margin) {
        Rectangle bounds = SwingUtilities.convertRectangle(anchor.getParent(), anchor.getBounds(), container);
        int y = Math.max(bounds.y + margin, bounds.y + bounds.height - size.height - margin);
        return new Point(bounds.x + margin, y);
    }

    private void startHideTimer() {
        int remaining = (int) (currentNotification.getDuration() - elapsedTime);
        if (remaining < 300) {
            remaining = 300;
        }
        displayTimeStart = System.currentTimeMillis();
        hideTimer.setInitialDelay(remaining);
        hideTimer.restart();
    }

    /**
     * Hides the displayed notification and starts the pause before the next one.
     *
     * @param expected the notification to hide, or {@code null} for whichever is displayed. Nothing happens if
     *                 another notification is displayed by now.
     */
    private void stopHideTimer(Notification expected) {
        // may be reached from any thread through replaceExistingNotification()
        GuiHelper.runInEDTAndWait(() -> {
            if (currentNotificationPanel == null || (expected != null && !Objects.equals(expected, currentNotification))) {
                return;
            }
            hideTimer.stop();
            detachAnchorListener();
            MainApplication.removeMapFrameListener(mapFrameListener);
            currentNotificationPanel.setVisible(false);
            JFrame parent = MainApplication.getMainFrame();
            if (parent != null) {
                parent.getLayeredPane().remove(currentNotificationPanel);
            }
            currentNotificationPanel = null;
            synchronized (queue) {
                // forget it, or an identical notification shown during the pause is dropped as a duplicate
                currentNotification = null;
            }
            pauseTimer.restart();
        });
    }

    private final class PauseFinishedEvent implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            synchronized (queue) {
                running = false;
            }
            processQueue();
        }
    }

    private final class UnfreezeEvent implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            // AWT still delivers the mouse exit event of a panel that has just been removed
            if (currentNotificationPanel != null) {
                currentNotificationPanel.setNotificationBackground(PANEL_SEMITRANSPARENT);
                currentNotificationPanel.repaint();
                startHideTimer();
            }
        }
    }

    private static class NotificationPanel extends JPanel {

        static final class ShowNoteHelpAction extends AbstractAction {
            private final Notification note;

            ShowNoteHelpAction(Notification note) {
                super(tr("Help"));
                putValue(SHORT_DESCRIPTION, tr("Show help information"));
                new ImageProvider("help").getResource().attachImageIcon(this, true);
                this.note = note;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(() -> HelpBrowser.setUrlForHelpTopic(note.getHelpTopic()));
            }
        }

        private JPanel innerPanel;

        NotificationPanel(Notification note, MouseListener freeze, ActionListener hideListener) {
            setVisible(false);
            build(note, freeze, hideListener);
        }

        public void setNotificationBackground(Color c) {
            innerPanel.setBackground(c);
        }

        private void build(final Notification note, MouseListener freeze, ActionListener hideListener) {
            // the default FlowLayout would add gaps around the visible notification
            setLayout(new BorderLayout());
            JButton btnClose = new JButton();
            btnClose.addActionListener(hideListener);
            btnClose.setIcon(ImageProvider.get("misc", "grey_x"));
            btnClose.setPreferredSize(GuiSizesHelper.getDimensionDpiAdjusted(new Dimension(50, 50)));
            btnClose.setMargin(new Insets(0, 0, 1, 1));
            btnClose.setContentAreaFilled(false);
            // put it in JToolBar to get a better appearance
            JToolBar tbClose = new JToolBar();
            tbClose.setFloatable(false);
            tbClose.setBorderPainted(false);
            tbClose.setOpaque(false);
            tbClose.add(btnClose);

            JToolBar tbHelp = null;
            if (note.getHelpTopic() != null) {
                JButton btnHelp = new JButton(new ShowNoteHelpAction(note));
                HelpUtil.setHelpContext(btnHelp, note.getHelpTopic());
                btnHelp.setOpaque(false);
                tbHelp = new JToolBar();
                tbHelp.setFloatable(false);
                tbHelp.setBorderPainted(false);
                tbHelp.setOpaque(false);
                tbHelp.add(btnHelp);
            }

            setOpaque(false);
            innerPanel = new RoundedPanel();
            innerPanel.setBackground(PANEL_SEMITRANSPARENT);
            innerPanel.setForeground(Color.BLACK);

            GroupLayout layout = new GroupLayout(innerPanel);
            innerPanel.setLayout(layout);
            layout.setAutoCreateGaps(true);
            layout.setAutoCreateContainerGaps(true);

            add(innerPanel, BorderLayout.CENTER);

            JLabel icon = null;
            if (note.getIcon() != null) {
                icon = new JLabel(note.getIcon());
            }
            Component content = note.getContent();
            GroupLayout.SequentialGroup hgroup = layout.createSequentialGroup();
            if (icon != null) {
                hgroup.addComponent(icon);
            }
            if (tbHelp != null) {
                hgroup.addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                        .addComponent(content)
                        .addComponent(tbHelp)
                );
            } else {
                hgroup.addComponent(content);
            }
            hgroup.addComponent(tbClose);
            GroupLayout.ParallelGroup vgroup = layout.createParallelGroup();
            if (icon != null) {
                vgroup.addComponent(icon);
            }
            vgroup.addComponent(content);
            vgroup.addComponent(tbClose);
            layout.setHorizontalGroup(hgroup);

            if (tbHelp != null) {
                layout.setVerticalGroup(layout.createSequentialGroup()
                        .addGroup(vgroup)
                        .addComponent(tbHelp)
                );
            } else {
                layout.setVerticalGroup(vgroup);
            }

            /*
             * The timer stops when the mouse cursor is above the panel.
             *
             * This is not straightforward, because the JPanel will get a
             * mouseExited event when the cursor moves on top of the JButton
             * inside the panel.
             *
             * The current hacky solution is to register the freeze MouseListener
             * not only to the panel, but to all the components inside the panel.
             *
             * Moving the mouse cursor from one component to the next would
             * cause some flickering (timer is started and stopped for a fraction
             * of a second, background color is switched twice), so there is
             * a tiny delay before the timer really resumes.
             */
            addMouseListenerToAllChildComponents(this, freeze);
        }

        private static void addMouseListenerToAllChildComponents(Component comp, MouseListener listener) {
            comp.addMouseListener(listener);
            if (comp instanceof Container) {
                for (Component c: ((Container) comp).getComponents()) {
                    addMouseListenerToAllChildComponents(c, listener);
                }
            }
        }
    }

    class FreezeMouseListener extends MouseAdapter {
        @Override
        public void mouseEntered(MouseEvent e) {
            if (unfreezeDelayTimer.isRunning()) {
                unfreezeDelayTimer.stop();
            } else if (currentNotificationPanel != null) {
                // AWT still delivers events for a panel that has just been removed
                hideTimer.stop();
                elapsedTime += System.currentTimeMillis() - displayTimeStart;
                currentNotificationPanel.setNotificationBackground(PANEL_OPAQUE);
                currentNotificationPanel.repaint();
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            unfreezeDelayTimer.restart();
        }
    }

    /**
     * A panel with rounded edges and line border.
     */
    public static class RoundedPanel extends JPanel {

        RoundedPanel() {
            super();
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            // paint on a copy, so that neither the antialiasing hint nor the stroke leak into the given context
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(getBackground());
                float lineWidth = 1.4f;
                // the outline goes on the content box, not around the empty border
                Insets insets = getInsets();
                Shape rect = new RoundRectangle2D.Double(
                        insets.left + lineWidth/2d,
                        insets.top + lineWidth/2d,
                        getWidth() - insets.left - insets.right - lineWidth,
                        getHeight() - insets.top - insets.bottom - lineWidth,
                        20, 20);

                g.fill(rect);
                g.setColor(getForeground());
                g.setStroke(new BasicStroke(lineWidth));
                g.draw(rect);
            } finally {
                g.dispose();
            }
            super.paintComponent(graphics);
        }
    }

    public static synchronized NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }
}
