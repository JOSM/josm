// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import static org.junit.Assert.assertEquals;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Test {@link MultiLineFlowLayout}
 * @author Michael Zangl
 */
public class MultiLineFlowLayoutTest {
    /**
     * No special rules.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    private static final int TEST_WIDHT = 500;
    private JPanel container;

    /**
     * Prepare test container.
     */
    @Before
    public void setUp() {
        JPanel parent = new JPanel();
        parent.setBounds(0, 0, TEST_WIDHT, 500);

        container = new JPanel(new MultiLineFlowLayout(FlowLayout.CENTER, 0, 0));
        parent.add(container);
    }

    /**
     * Test that one line is layed out correctly
     */
    @Test
    public void testOneLine() {
        fillOneLine();

        container.invalidate();
        Dimension preferredSize = container.getPreferredSize();
        assertEquals(TEST_WIDHT, preferredSize.width);
        assertEquals(100, preferredSize.height);

        Dimension minimum = container.getMinimumSize();
        assertEquals(TEST_WIDHT, minimum.width);
        assertEquals(50, minimum.height);
    }

    /**
     * Test that insets are respected
     */
    @Test
    public void testInsets() {
        fillOneLine();

        container.setBorder(BorderFactory.createEmptyBorder(3, 0, 7, 0));
        container.invalidate();
        Dimension preferredSize = container.getPreferredSize();
        assertEquals(TEST_WIDHT, preferredSize.width);
        assertEquals(110, preferredSize.height);

        // This should force wrapping
        container.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 40));
        container.invalidate();
        preferredSize = container.getPreferredSize();
        assertEquals(TEST_WIDHT, preferredSize.width);
        assertEquals(200, preferredSize.height);
    }

    /**
     * Test that gaps are respected
     */
    @Test
    public void testGaps() {
        fillOneLine();

        container.setLayout(new MultiLineFlowLayout(FlowLayout.LEADING, 20, 10));
        container.invalidate();
        Dimension preferredSize = container.getPreferredSize();
        assertEquals(TEST_WIDHT, preferredSize.width);
        assertEquals(230, preferredSize.height);
    }

    /**
     * Test that it behaves the same as FlowLayout for one line.
     */
    @Test
    public void testSameAsFlowLayout() {
        fillOneLine();
        JPanel childx = new JPanel();
        childx.setPreferredSize(new Dimension(300, 100));
        childx.setMinimumSize(new Dimension(200, 50));
        childx.setVisible(false);
        container.add(childx);
        container.setBorder(BorderFactory.createEmptyBorder(3, 4, 5, 6));

        container.setLayout(new MultiLineFlowLayout(FlowLayout.LEADING, 2, 1));
        container.invalidate();
        Dimension is = container.getPreferredSize();

        container.setLayout(new FlowLayout(FlowLayout.LEADING, 2, 1));
        container.invalidate();
        Dimension should = container.getPreferredSize();

        assertEquals(should.height, is.height);
    }

    private void fillOneLine() {
        JPanel child1 = new JPanel();
        child1.setPreferredSize(new Dimension(300, 100));
        child1.setMinimumSize(new Dimension(200, 50));
        container.add(child1);
        JPanel child2 = new JPanel();
        child2.setPreferredSize(new Dimension(100, 100));
        child1.setMinimumSize(new Dimension(100, 50));
        container.add(child2);
        JPanel child3 = new JPanel();
        child3.setPreferredSize(new Dimension(50, 100));
        child1.setMinimumSize(new Dimension(50, 50));
        container.add(child3);
    }
}
