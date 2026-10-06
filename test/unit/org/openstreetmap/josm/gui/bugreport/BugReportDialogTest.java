// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.bugreport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.InvalidPathException;
import java.util.Collections;

import javax.swing.JOptionPane;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;
import org.openstreetmap.josm.tools.bugreport.BugReport;
import org.openstreetmap.josm.tools.bugreport.BugReportQueue.SuppressionMode;
import org.openstreetmap.josm.tools.bugreport.ReportedException;

/**
 * Unit tests of {@link BugReportDialog}.
 */
class BugReportDialogTest {

    /**
     * A file name the locale cannot represent is explained to the user, once, instead of asking for a bug report.
     * See #14596.
     */
    @Test
    void testUnmappableFileNameIsExplainedOnce() {
        TestUtils.assumeWorkingJMockit();
        final JOptionPaneSimpleMocker mocker = new JOptionPaneSimpleMocker(Collections.singletonMap(
                "<html>JOSM cannot handle the name of a file, because it is running with the character set ANSI_X3.4-1968 "
                        + "for file names, which cannot represent it.<br>"
                        + "This happens when JOSM is started without a UTF-8 locale, for example with <tt>LANG=C</tt>, "
                        + "or with a locale which is not installed.<br><br>"
                        + "Please start JOSM with the environment variable <tt>LC_ALL=C.UTF-8</tt>.</html>",
                JOptionPane.OK_OPTION));
        final ReportedException e = BugReport.intercept(new InvalidPathException("/home/user/Capture d'écran.png",
                "Malformed input or input contains unmappable characters"));
        final String jnuEncoding = System.getProperty("sun.jnu.encoding");
        try {
            System.setProperty("sun.jnu.encoding", "ANSI_X3.4-1968");
            // no bug report dialog: that would fail here, as the tests run headless
            assertEquals(SuppressionMode.NONE, BugReportDialog.showFor(e, 0));
            // browsing another folder throws it again, which is not worth another message
            assertEquals(SuppressionMode.NONE, BugReportDialog.showFor(e, 1));
        } finally {
            System.setProperty("sun.jnu.encoding", jnuEncoding);
        }
        assertEquals(1, mocker.getInvocationLog().size());
        assertEquals("Unsupported file name", mocker.getInvocationLog().get(0)[2]);
    }
}
