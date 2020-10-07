// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.mockers;

import java.awt.Point;

import javax.swing.JComponent;

import org.openstreetmap.josm.gui.MapViewState;

import mockit.Mock;
import mockit.MockUp;

/**
 * MockUp for allowing a {@link MapViewState} to be fully initialized in either headless or
 * windowless tests
 */
public class WindowlessMapViewStateMocker extends MockUp<MapViewState> {
    @Mock
    static Point findTopLeftInWindow(JComponent position) {
        return new Point();
    }

    @Mock
    static Point findTopLeftOnScreen(JComponent position) {
        // in our imaginary universe the window is always (10, 10) from the top left of the screen
        Point topLeftInWindow = findTopLeftInWindow(position);
        return new Point(topLeftInWindow.x + 10, topLeftInWindow.y + 10);
    }
}
