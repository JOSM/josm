// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link ImageProvider} class for manual execution.
 */
public class ImageProviderTestManual {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test getting a cursor
     */
    @Ignore("manual execution only, as the look of the cursor cannot be checked automatedly")
    @Test
    public void testGetCursor() throws InterruptedException {
        JFrame frame = new JFrame();
        frame.setSize(500, 500);
        frame.setLayout(new GridLayout(2, 2));
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

        // draw red dot at hotspot when clicking
        frame.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Graphics graphics = frame.getGraphics();
                graphics.setColor(Color.RED);
                graphics.drawRect(e.getX(), e.getY(), 1, 1);
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
        Thread.sleep(9000); // test would time out after 10s
    }
}
