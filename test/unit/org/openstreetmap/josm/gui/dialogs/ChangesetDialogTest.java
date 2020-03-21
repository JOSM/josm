// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.gui.dialogs.ChangesetDialog.LaunchChangesetManager;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests of {@link ChangesetDialog} class.
 */
public class ChangesetDialogTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Tests that {@code LaunchChangesetManager} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    public void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(LaunchChangesetManager.class);
    }
}
