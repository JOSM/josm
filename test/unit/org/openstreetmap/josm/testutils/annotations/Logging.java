// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.logging.Handler;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Reset logging for each test
 */
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Logging {
    class LoggingExtension implements BeforeEachCallback {

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            // Force log handlers to reacquire reference to (junit's fake) stdout/stderr
            for (Handler handler : org.openstreetmap.josm.tools.Logging.getLogger().getHandlers()) {
                if (handler instanceof org.openstreetmap.josm.tools.Logging.ReacquiringConsoleHandler) {
                    handler.flush();
                    ((org.openstreetmap.josm.tools.Logging.ReacquiringConsoleHandler) handler).reacquireOutputStream();
                }
            }
            // Set log level to info
            org.openstreetmap.josm.tools.Logging.setLogLevel(org.openstreetmap.josm.tools.Logging.LEVEL_INFO);
        }
    }
}
