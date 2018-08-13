// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedList;
import java.util.Queue;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
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
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Manages {@link Notification}s, i.e.&nbsp;displays them on screen.
 *
 * Don't use this class directly, but use {@link Notification#show()}.
 *
 * If multiple messages are sent in a short period of time, they are put in
 * a queue and displayed one after the other.
 *
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
    private final Queue<Notification> queue;

    private static IntegerProperty pauseTime = new IntegerProperty("notification-default-pause-time-ms", 300); // milliseconds

    private long displayTimeStart;
    private long elapsedTime;

    private static NotificationManager instance;

    private static final Color PANEL_SEMITRANSPARENT = new Color(224, 236, 249, 230);
    private static final Color PANEL_OPAQUE = new Color(224, 236, 249);

    NotificationManager() {
        queue = new LinkedList<>();
        hideTimer = new Timer(Notification.TIME_DEFAULT, e -> this.stopHideTimer());
        hideTimer.setRepeats(false);
        pauseTimer = new Timer(pauseTime.get(), new PauseFinishedEvent());
        pauseTimer.setRepeats(false);
        unfreezeDelayTimer = new Timer(10, new UnfreezeEvent());
        unfreezeDelayTimer.setRepeats(false);
    }

    /**
     * Show the given notification
     * @param note The note to show.
     * @see Notification#show()
     */
    public void showNotification(Notification note) {
        synchronized (queue) {
            queue.add(note);
            processQueue();
        }
    }

    private void processQueue() {
        if (running) return;

        currentNotification = queue.poll();
        if (currentNotification == null) return;

        GuiHelper.runInEDTAndWait(() -> {
            currentNotificationPanel = new NotificationPanel(currentNotification, new FreezeMouseListener(), e -> this.stopHideTimer());
            currentNotificationPanel.validate();

            int margin = 5;
            JFrame parentWindow = MainApplication.getMainFrame();
            Dimension size = currentNotificationPanel.getPreferredSize();
            if (parentWindow != null) {
                int x;
                int y;
                MapFrame map = MainApplication.getMap();
                if (MainApplication.isDisplayingMapView() && map.mapView.getHeight() > 0) {
                    MapView mv = map.mapView;
                    Point mapViewPos = SwingUtilities.convertPoint(mv.getParent(), mv.getX(), mv.getY(), MainApplication.getMainFrame());
                    x = mapViewPos.x + margin;
                    y = mapViewPos.y + mv.getHeight() - map.statusLine.getHeight() - size.height - margin;
                } else {
                    x = margin;
                    y = parentWindow.getHeight() - MainApplication.getToolbar().control.getSize().height - size.height - margin;
                }
                parentWindow.getLayeredPane().add(currentNotificationPanel, JLayeredPane.POPUP_LAYER, 0);

                currentNotificationPanel.setLocation(x, y);
            }
            currentNotificationPanel.setSize(size);
            currentNotificationPanel.setVisible(true);
        });

        running = true;
        elapsedTime = 0;

        startHideTimer();
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

    private void stopHideTimer() {
        hideTimer.stop();
        if (currentNotificationPanel != null) {
            currentNotificationPanel.setVisible(false);
            JFrame parent = MainApplication.getMainFrame();
            if (parent != null) {
                parent.getLayeredPane().remove(currentNotificationPanel);
            }
            currentNotificationPanel = null;
        }
        pauseTimer.restart();
    }

    private class PauseFinishedEvent implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            synchronized (queue) {
                running = false;
                processQueue();
            }
        }
    }

    private class UnfreezeEvent implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentNotificationPanel != null) {
                currentNotificationPanel.setNotificationBackground(PANEL_SEMITRANSPARENT);
                currentNotificationPanel.repaint();
            }
            startHideTimer();
        }
    }

    private static class NotificationPanel extends JPanel {

        static final class ShowNoteHelpAction extends AbstractAction {
            private final Notification note;

            ShowNoteHelpAction(Notification note) {
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
            JButton btnClose = new JButton();
            btnClose.addActionListener(hideListener);
            btnClose.setIcon(ImageProvider.get("misc", "grey_x"));
            btnClose.setPreferredSize(new Dimension(50, 50));
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
                JButton btnHelp = new JButton(tr("Help"));
                btnHelp.setIcon(ImageProvider.get("help"));
                btnHelp.setToolTipText(tr("Show help information"));
                HelpUtil.setHelpContext(btnHelp, note.getHelpTopic());
                btnHelp.addActionListener(new ShowNoteHelpAction(note));
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

            innerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            add(innerPanel);

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
            } else {
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
            Graphics2D g = (Graphics2D) graphics;
            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(getBackground());
            float lineWidth = 1.4f;
            Shape rect = new RoundRectangle2D.Double(
                    lineWidth/2d + getInsets().left,
                    lineWidth/2d + getInsets().top,
                    getWidth() - lineWidth/2d - getInsets().left - getInsets().right,
                    getHeight() - lineWidth/2d - getInsets().top - getInsets().bottom,
                    20, 20);

            g.fill(rect);
            g.setColor(getForeground());
            g.setStroke(new BasicStroke(lineWidth));
            g.draw(rect);
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
