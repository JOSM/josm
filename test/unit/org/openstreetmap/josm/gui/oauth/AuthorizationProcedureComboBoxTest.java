// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.ListCellRenderer;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link AuthorizationProcedureComboBox} class.
 */
class AuthorizationProcedureComboBoxTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@link AuthorizationProcedureComboBox}.
     */
    @Test
    void testAuthorizationProcedureComboBox() {
        ListCellRenderer<? super AuthorizationProcedure> r = new AuthorizationProcedureComboBox().getRenderer();
        for (AuthorizationProcedure procedure : AuthorizationProcedure.values()) {
            assertEquals(r, r.getListCellRendererComponent(null, procedure, 0, true, false));
            assertEquals(r, r.getListCellRendererComponent(null, procedure, 0, false, false));
        }
    }
}
