// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition.Context;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.Op;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.PseudoClasses;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests of {@link ConditionFactory}.
 */
public class ConditionFactoryTest {

    /**
     * Setup rule
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Non-regression test for ticket <a href="https://josm.openstreetmap.de/ticket/14368">#14368</a>.
     * @throws Exception if an error occurs
     */
    @Test(expected = MapCSSException.class)
    public void testTicket14368() throws Exception {
        ConditionFactory.createKeyValueCondition("name", "Rodovia ([A-Z]{2,3}-[0-9]{2,4}", Op.REGEX, Context.PRIMITIVE, false);
    }

    /**
     * Tests that {@code PseudoClasses} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    public void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(PseudoClasses.class);
    }
}
