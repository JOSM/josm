// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.mockers;

import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Window;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

/**
 * MockUp for a {@link Window} which simply (and naively) makes its constructor(s) a no-op. This has
 * the advantage of removing the isHeadless check. Though if course it also leaves you with
 * uninintialized objects, and so of course they don't *necessarily* work properly. But often they
 * work *just enough* to behave how a test needs them to. Exercise left to the reader to discover
 * the limits here.
 */
public class WindowMocker extends MockUp<Window> {
    @Mock
    private void $init(final Invocation invocation) {
    }

    @Mock
    private void $init(final Invocation invocation, final Window window) {
    }

    @Mock
    private void $init(final Invocation invocation, final Frame frame) {
    }

    @Mock
    private void $init(final Invocation invocation, final GraphicsConfiguration gc) {
    }

    @Mock
    private void $init(final Invocation invocation, final Window window, final GraphicsConfiguration gc) {
    }
}
