// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link ImageProvider} class for manual execution.
 */
@BasicPreferences
class ImageProviderTestManual {
    /**
     * Test getting a cursor
     */
    @Test
    void testGetCursor() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "This test cannot be run without a graphics environment and a human");
        JFrame frame = new JFrame();
        frame.setSize(500, 500);
        frame.setLayout(new GridLayout(2, 2));
        frame.setTitle("Cursor check -- please move cursor to the four quadrants and click to check placement");
        JPanel leftUpperPanel = new JPanel(), rightUpperPanel = new JPanel(), leftLowerPanel = new JPanel(), rightLowerPanel = new JPanel();
        leftUpperPanel.setBackground(Color.DARK_GRAY);
        rightUpperPanel.setBackground(Color.DARK_GRAY);
        leftLowerPanel.setBackground(Color.DARK_GRAY);
        rightLowerPanel.setBackground(Color.DARK_GRAY);
        frame.add(leftUpperPanel);
        frame.add(rightUpperPanel);
        frame.add(leftLowerPanel);
        frame.add(rightLowerPanel);

        leftUpperPanel.setCursor(ImageProvider.getCursor("normal", "select_add")); // contains diagonal sensitive to alpha blending
        rightUpperPanel.setCursor(ImageProvider.getCursor("crosshair", "joinway")); // combination of overlay and hotspot not top left
        leftLowerPanel.setCursor(ImageProvider.getCursor("hand", "parallel_remove")); // reasonably nice bitmap cursor
        rightLowerPanel.setCursor(ImageProvider.getCursor("rotate", null)); // ugly bitmap cursor, cannot do much here

        frame.setVisible(true);

        // hover over the four quadrant to observe different cursors
        final Map<Component, Point> clickPoints = new HashMap<>(frame.getComponentCount());

        // draw red dot at hotspot when clicking
        frame.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Graphics graphics = frame.getGraphics();
                graphics.setColor(Color.RED);
                graphics.drawRect(e.getX(), e.getY(), 1, 1);
                clickPoints.put(e.getComponent(), e.getPoint());
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        try {
            // The condition will never be true. This gives the user time to click and pan around.
            Awaitility.await().atLeast(Duration.ofMillis(9000)).atMost(Duration.ofMillis(9000)).until(() -> clickPoints.size() > 4);
        } catch (ConditionTimeoutException timeoutException) {
            Logging.info(timeoutException);
        }
        assumeFalse(clickPoints.isEmpty(), "The tester has to interact with the panel");
    }
}
