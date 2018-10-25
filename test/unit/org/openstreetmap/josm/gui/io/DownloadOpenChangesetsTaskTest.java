// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.GraphicsEnvironment;
import java.net.URL;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.gui.oauth.OAuthAuthorizationWizard;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;
import org.openstreetmap.josm.testutils.mockers.WindowMocker;
import org.openstreetmap.josm.tools.UserCancelException;

import com.google.common.collect.ImmutableMap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

/**
 * Unit tests of {@link DownloadOpenChangesetsTask} class.
 */
public class DownloadOpenChangesetsTaskTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().devAPI();

    /**
     * OAuth wizard mocker.
     */
    public static class OAuthWizardMocker extends MockUp<OAuthAuthorizationWizard> {
        /** {@code true} if wizard has been called */
        public boolean called;

        @Mock
        void showDialog() throws UserCancelException {
            this.called = true;
            throw new UserCancelException();
        }

        @Mock
        void obtainAccessToken(final Invocation invocation, final URL serverUrl) {
            if (GraphicsEnvironment.isHeadless()) {
                // we can't really let execution proceed any further as construction of the ui
                // elements will fail with a mocked Window
                this.called = true;
                return;
            }
            // else we can allow a bit more of the code to be covered before we raise
            // UserCancelException in showDialog
            invocation.proceed(serverUrl);
        }
    }

    /**
     * Test of {@link DownloadOpenChangesetsTask} class when anonymous.
     */
    @Test
    public void testAnonymous() {
        TestUtils.assumeWorkingJMockit();
        if (GraphicsEnvironment.isHeadless()) {
            new WindowMocker();
        }
        final OAuthWizardMocker oaWizardMocker = new OAuthWizardMocker();
        final JOptionPaneSimpleMocker jopsMocker = new JOptionPaneSimpleMocker(
            ImmutableMap.<String, Object>of(
                "<html>Could not retrieve the list of your open changesets because<br>JOSM does not know "
                + "your identity.<br>You have either chosen to work anonymously or you are not "
                + "entitled<br>to know the identity of the user on whose behalf you are working.</html>", JOptionPane.OK_OPTION
            )
        );

        DownloadOpenChangesetsTask task = new DownloadOpenChangesetsTask(new JPanel());
        assertNull(task.getChangesets());

        assertTrue(UserIdentityManager.getInstance().isAnonymous());
        task.run();
        assertNull(task.getChangesets());

        assertEquals(1, jopsMocker.getInvocationLog().size());
        Object[] invocationLogEntry = jopsMocker.getInvocationLog().get(0);
        assertEquals(JOptionPane.OK_OPTION, (int) invocationLogEntry[0]);
        assertEquals("Missing user identity", invocationLogEntry[2]);

        assertTrue(oaWizardMocker.called);
    }

    /**
     * Test of {@link DownloadOpenChangesetsTask} class when "partially identified".
     */
    @Test
    public void testPartiallyIdentified() {
        TestUtils.assumeWorkingJMockit();
        if (GraphicsEnvironment.isHeadless()) {
            new WindowMocker();
        }
        final OAuthWizardMocker oaWizardMocker = new OAuthWizardMocker();
        final JOptionPaneSimpleMocker jopsMocker = new JOptionPaneSimpleMocker(
            ImmutableMap.<String, Object>of("There are no open changesets", JOptionPane.OK_OPTION)
        );

        DownloadOpenChangesetsTask task = new DownloadOpenChangesetsTask(new JPanel());
        UserIdentityManager.getInstance().setPartiallyIdentified(System.getProperty("osm.username", "josm_test"));
        assertTrue(UserIdentityManager.getInstance().isPartiallyIdentified());
        task.run();
        assertNotNull(task.getChangesets());

        assertEquals(1, jopsMocker.getInvocationLog().size());
        Object[] invocationLogEntry = jopsMocker.getInvocationLog().get(0);
        assertEquals(JOptionPane.OK_OPTION, (int) invocationLogEntry[0]);
        assertEquals("No open changesets", invocationLogEntry[2]);

        assertTrue(oaWizardMocker.called);
    }
}
