// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Deque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.tools.ReflectionUtils;

/**
 * Unit tests of {@link NotificationManager} class.
 */
@BasicPreferences
class NotificationManagerTest {

    private static final int MARGIN = 10;
    private static final Dimension NOTIFICATION_SIZE = new Dimension(300, 60);

    /**
     * Builds a layered pane holding a content pane below the menu bar, which holds the map view right of the side
     * toolbar and above the status line.
     * @param layeredPane the layered pane to fill
     * @return the map view stand-in
     */
    private static JPanel buildMainWindowHierarchy(JLayeredPane layeredPane) {
        layeredPane.setBounds(0, 0, 1000, 800);
        JPanel contentPane = new JPanel(null);
        contentPane.setBounds(0, 25, 1000, 775);
        layeredPane.add(contentPane);
        JPanel mapView = new JPanel();
        mapView.setBounds(30, 0, 970, 740);
        contentPane.add(mapView);
        return mapView;
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(NotificationManager manager, String name) throws ReflectiveOperationException {
        Field field = NotificationManager.class.getDeclaredField(name);
        ReflectionUtils.setObjectsAccessible(field);
        return (T) field.get(manager);
    }

    private static void stopHideTimer(NotificationManager manager, Notification expected) throws ReflectiveOperationException {
        Method method = NotificationManager.class.getDeclaredMethod("stopHideTimer", Notification.class);
        ReflectionUtils.setObjectsAccessible(method);
        method.invoke(manager, expected);
    }

    /**
     * The notification is aligned to the bottom left corner of the map view.
     */
    @Test
    void testGetNotificationPositionMapView() {
        JLayeredPane layeredPane = new JLayeredPane();
        JPanel mapView = buildMainWindowHierarchy(layeredPane);

        assertEquals(new Point(30 + MARGIN, 25 + 740 - NOTIFICATION_SIZE.height - MARGIN),
                NotificationManager.getNotificationPosition(mapView, layeredPane, NOTIFICATION_SIZE, MARGIN));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/23002">#23002</a>: the position does not
     * depend on the window decorations, which are only there when not in fullscreen mode.
     */
    @Test
    void testTicket23002() {
        JLayeredPane layeredPane = new JLayeredPane();
        JPanel mapView = buildMainWindowHierarchy(layeredPane);
        Point fullscreen = NotificationManager.getNotificationPosition(mapView, layeredPane, NOTIFICATION_SIZE, MARGIN);

        // within the frame, the layered pane is offset by the window decorations
        JPanel frame = new JPanel(null);
        frame.setBounds(0, 0, 1010, 840);
        JPanel rootPane = new JPanel(null);
        rootPane.setBounds(5, 35, 1000, 800);
        frame.add(rootPane);
        rootPane.add(layeredPane);

        Point windowed = NotificationManager.getNotificationPosition(mapView, layeredPane, NOTIFICATION_SIZE, MARGIN);
        assertEquals(fullscreen, windowed);

        Rectangle notification = new Rectangle(windowed, NOTIFICATION_SIZE);
        Rectangle mapViewBounds = SwingUtilities.convertRectangle(mapView.getParent(), mapView.getBounds(), layeredPane);
        assertTrue(mapViewBounds.contains(notification), notification + " is not inside the map view " + mapViewBounds);
    }

    /**
     * A notification taller than the map view is aligned to its top, so that the beginning of the message stays
     * readable.
     */
    @Test
    void testGetNotificationPositionTallerThanAnchor() {
        JLayeredPane layeredPane = new JLayeredPane();
        JPanel mapView = buildMainWindowHierarchy(layeredPane);

        assertEquals(new Point(30 + MARGIN, 25 + MARGIN),
                NotificationManager.getNotificationPosition(mapView, layeredPane, new Dimension(480, 800), MARGIN));
    }

    /**
     * Tries to take the monitor of {@code lock} from another thread.
     * @param lock the object to synchronize on
     * @return {@code false} if some other thread holds the monitor for longer than a second
     * @throws InterruptedException if interrupted while waiting
     */
    private static boolean canTakeMonitorOf(Object lock) throws InterruptedException {
        CountDownLatch taken = new CountDownLatch(1);
        Thread probe = new Thread(() -> {
            synchronized (lock) {
                taken.countDown();
            }
        }, "notification-manager-test-monitor");
        probe.setDaemon(true);
        probe.start();
        return taken.await(1, TimeUnit.SECONDS);
    }

    /**
     * Showing a notification from a worker thread must not hold the monitor of the queue while waiting for the EDT,
     * which takes it as well. The monitor is checked from the EDT, as a real deadlock would wedge it for good.
     * @throws Exception in case of error
     */
    @Test
    void testShowNotificationFromWorkerThreadDoesNotFreezeEdt() throws Exception {
        NotificationManager manager = new NotificationManager();
        Object queue = field(manager, "queue");
        AtomicBoolean monitorWasHeld = new AtomicBoolean();
        // getContent() is called on the EDT, while showNotification() is still running on the worker thread
        Notification note = new Notification("shown from a worker thread") {
            @Override
            public Component getContent() {
                try {
                    monitorWasHeld.set(!canTakeMonitorOf(queue));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return super.getContent();
            }
        };

        Thread worker = new Thread(() -> manager.showNotification(note), "notification-manager-test");
        worker.setDaemon(true);
        worker.start();
        worker.join(TimeUnit.SECONDS.toMillis(10));

        assertFalse(worker.isAlive(), "showNotification() never returned");
        assertFalse(monitorWasHeld.get(), "showNotification() held the monitor of the queue while waiting for the EDT");
    }

    /**
     * AWT still delivers mouse events to a notification panel that has just been removed.
     * @throws Exception in case of error
     */
    @Test
    void testMouseEnteredAfterNotificationHidden() throws Exception {
        NotificationManager manager = new NotificationManager();
        manager.showNotification(new Notification("hover me").setDuration(Notification.TIME_LONG));
        Component panel = field(manager, "currentNotificationPanel");
        assertNotNull(panel);

        stopHideTimer(manager, null);
        assertNull(field(manager, "currentNotificationPanel"));

        MouseEvent event = new MouseEvent(panel, MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(),
                0, 1, 1, 0, false, MouseEvent.NOBUTTON);
        for (MouseListener listener : panel.getMouseListeners()) {
            assertDoesNotThrow(() -> listener.mouseEntered(event));
        }
    }

    /**
     * The mouse leaving a notification panel that has just been removed must not restart the hide timer, which
     * would only delay the next notification.
     * @throws Exception in case of error
     */
    @Test
    void testUnfreezeAfterNotificationHidden() throws Exception {
        NotificationManager manager = new NotificationManager();
        manager.showNotification(new Notification("hover me").setDuration(Notification.TIME_LONG));
        assertNotNull(field(manager, "currentNotificationPanel"));

        stopHideTimer(manager, null);
        Timer hideTimer = field(manager, "hideTimer");
        assertFalse(hideTimer.isRunning());

        Timer unfreezeDelayTimer = field(manager, "unfreezeDelayTimer");
        ActionEvent event = new ActionEvent(unfreezeDelayTimer, ActionEvent.ACTION_PERFORMED, null);
        for (ActionListener listener : unfreezeDelayTimer.getActionListeners()) {
            listener.actionPerformed(event);
        }
        assertFalse(hideTimer.isRunning(), "the hide timer was restarted for a notification that is gone");
    }

    /**
     * Replacing a notification that is no longer displayed must not hide the one that took its place.
     * @throws Exception in case of error
     */
    @Test
    void testReplaceNotificationThatIsNoLongerDisplayed() throws Exception {
        NotificationManager manager = new NotificationManager();
        manager.showNotification(new Notification("displayed"));
        Component panel = field(manager, "currentNotificationPanel");
        assertNotNull(panel);

        stopHideTimer(manager, new Notification("replaced while the queue moved on"));
        assertSame(panel, field(manager, "currentNotificationPanel"), "the displayed notification was hidden instead");

        stopHideTimer(manager, field(manager, "currentNotification"));
        assertNull(field(manager, "currentNotificationPanel"));
    }

    /**
     * A notification equal to any queued one is dropped, not only one equal to the last.
     * @throws Exception in case of error
     */
    @Test
    void testDuplicateAnywhereInQueueIsDropped() throws Exception {
        NotificationManager manager = new NotificationManager();
        manager.showNotification(new Notification("displayed"));
        manager.showNotification(new Notification("queued first"));
        manager.showNotification(new Notification("queued second"));
        manager.showNotification(new Notification("queued first"));

        Deque<Notification> queue = field(manager, "queue");
        assertEquals(2, queue.size(), "duplicate was queued again: " + queue);
    }

    /**
     * The same message shown right after the previous one disappeared is not a duplicate.
     * @throws Exception in case of error
     */
    @Test
    void testSameNotificationRightAfterTheLastOneDisappeared() throws Exception {
        NotificationManager manager = new NotificationManager();
        manager.showNotification(new Notification("same message"));
        assertNotNull(field(manager, "currentNotificationPanel"));

        stopHideTimer(manager, null);
        assertNull(field(manager, "currentNotification"));

        manager.showNotification(new Notification("same message"));
        Deque<Notification> queue = field(manager, "queue");
        assertEquals(1, queue.size(), "the notification was dropped as a duplicate");
    }

    /**
     * The rounded panel paints with antialiasing, without changing the hint of the graphics context it is given.
     */
    @Test
    void testRoundedPanelKeepsRenderingHints() {
        NotificationManager.RoundedPanel panel = new NotificationManager.RoundedPanel();
        panel.setSize(100, 50);

        BufferedImage image = new BufferedImage(100, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            panel.paintComponent(g);
            assertEquals(RenderingHints.VALUE_ANTIALIAS_OFF, g.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
        } finally {
            g.dispose();
        }
    }
}
