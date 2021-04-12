// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.trajano.commons.testing.UtilityClassTestUtil;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Unit tests of {@link ExpressionFactory}.
 */
class ExpressionFactoryTest {

    /**
     * Setup rule
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Tests that {@code Functions} satisfies utility class criteria.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(Functions.class);
    }

    /**
     * Tests that all functions have been registered to {@link ExpressionFactory#FACTORY_MAP}
     *
     * For instance to register {@link Functions#osm_id}, {@code FACTORY_MAP.put("osm_id", Factory.ofEnv(Functions::osm_id))}
     */
    @Test
    void testNoUnregisteredFunctions() {
        for (Method m : Functions.class.getDeclaredMethods()) {
            if (!Modifier.isPrivate(m.getModifiers()) && !ExpressionFactory.FACTORY_MAP.containsKey(m.getName())) {
                throw new AssertionError(m + " has not registered in ExpressionFactory.FACTORY_MAP");
            }
        }
    }
}
