// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPanel;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.actions.ExpertToggleAction.ExpertModeChangeListener;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Test {@link ExpertToggleAction}
 * @author Michael Zangl
 * @since 11224
 */
public class ExpertToggleActionTest {
    /**
     * We need prefs to store expert mode state.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Test {@link ExpertToggleAction#addVisibilitySwitcher(java.awt.Component)}
     * and {@link ExpertToggleAction#removeVisibilitySwitcher(java.awt.Component)}
     */
    @Test
    public void testVisibilitySwitcher() {
        ExpertToggleAction.getInstance().setExpert(false);
        JPanel c = new JPanel();

        ExpertToggleAction.addVisibilitySwitcher(c);
        assertFalse(c.isVisible());

        ExpertToggleAction.getInstance().setExpert(true);
        assertTrue(c.isVisible());

        ExpertToggleAction.removeVisibilitySwitcher(c);
        ExpertToggleAction.getInstance().setExpert(false);
        assertTrue(c.isVisible());

        // null should not be a problem
        ExpertToggleAction.addVisibilitySwitcher(null);
        ExpertToggleAction.removeVisibilitySwitcher(null);
    }

    /**
     * Test {@link ExpertToggleAction#addExpertModeChangeListener(ExpertModeChangeListener)}
     * and {@link ExpertToggleAction#removeExpertModeChangeListener(ExpertModeChangeListener)}
     */
    @Test
    public void testExpertModeListener() {
        AtomicBoolean value = new AtomicBoolean(false);
        ExpertToggleAction.getInstance().setExpert(true);
        ExpertModeChangeListener listener = value::set;

        ExpertToggleAction.addExpertModeChangeListener(listener);
        assertFalse(value.get());

        ExpertToggleAction.getInstance().setExpert(false);
        ExpertToggleAction.getInstance().setExpert(true);
        assertTrue(value.get());

        ExpertToggleAction.getInstance().setExpert(false);
        assertFalse(value.get());

        ExpertToggleAction.removeExpertModeChangeListener(listener);
        ExpertToggleAction.getInstance().setExpert(true);
        assertFalse(value.get());

        ExpertToggleAction.addExpertModeChangeListener(listener, true);
        assertTrue(value.get());

        // null should not be a problem
        ExpertToggleAction.addExpertModeChangeListener(null);
        ExpertToggleAction.removeExpertModeChangeListener(null);
    }

}
