// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils.mockers;

import org.openstreetmap.josm.tools.bugreport.BugReport;
import org.openstreetmap.josm.tools.bugreport.ReportedException;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

/**
 * Intercept bug report interceptions. This is useful if you have some code paths that should call {@link BugReport#intercept(Throwable)},
 * and you want to verify that the "right" exception was thrown.
 */
public class BugReportMock extends MockUp<BugReport> {
    private Throwable throwable;

    @Mock
    public ReportedException intercept(Invocation invocation, Throwable t) {
        this.throwable = t;
        return invocation.proceed(t);
    }

    /**
     * Get the throwable that was intercepted
     * @return The intercepted throwable
     */
    public Throwable throwable() {
        return this.throwable;
    }
}
