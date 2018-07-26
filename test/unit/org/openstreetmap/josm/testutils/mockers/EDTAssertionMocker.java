// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.mockers;

import org.openstreetmap.josm.gui.util.GuiHelper;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

/**
 * MockUp that, when applied, should cause calls to the EDT which would normally swallow generated
 * AssertionErrors to instead re-raise them.
 */
public class EDTAssertionMocker extends MockUp<GuiHelper> {
    @Mock
    private static void handleEDTException(final Invocation invocation, final Throwable t) {
        final Throwable cause = t.getCause();
        if (cause instanceof AssertionError) {
            throw (AssertionError) cause;
        }

        invocation.proceed(t);
    }
}
