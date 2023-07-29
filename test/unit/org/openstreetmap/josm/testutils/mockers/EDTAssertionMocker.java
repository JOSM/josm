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
    static void handleEDTException(final Invocation invocation, final Throwable t) throws Throwable {
        final Throwable cause = t.getCause();
        if (cause instanceof AssertionError) {
            throw cause;
        }

        invocation.proceed(t);
    }
}
