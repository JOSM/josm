// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.junit.Assert.assertEquals;

import javax.swing.ListCellRenderer;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link AuthorizationProcedureComboBox} class.
 */
public class AuthorizationProcedureComboBoxTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@link AuthorizationProcedureComboBox}.
     */
    @Test
    public void testAuthorizationProcedureComboBox() {
        ListCellRenderer<? super AuthorizationProcedure> r = new AuthorizationProcedureComboBox().getRenderer();
        for (AuthorizationProcedure procedure : AuthorizationProcedure.values()) {
            assertEquals(r, r.getListCellRendererComponent(null, procedure, 0, true, false));
            assertEquals(r, r.getListCellRendererComponent(null, procedure, 0, false, false));
        }
    }
}
