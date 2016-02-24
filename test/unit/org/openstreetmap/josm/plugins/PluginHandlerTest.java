// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.plugins.PluginHandler.DeprecatedPlugin;
import org.openstreetmap.josm.tools.Utils;

import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit tests of {@link PluginHandler} class.
 */
public class PluginHandlerTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of methods {@link DeprecatedPlugin#equals} and {@link DeprecatedPlugin#hashCode}.
     */
    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(DeprecatedPlugin.class).usingGetClass().verify();
    }

    /**
     * Unit test of {@link PluginHandler#buildListOfPluginsToLoad}.
     */
    @Test
    public void testBuildListOfPluginsToLoad() {
        final String old = System.getProperty("josm.plugins");
        try {
            System.setProperty("josm.plugins",
                    Utils.join(",", PluginHandler.DEPRECATED_PLUGINS) + "," +
                    Utils.join(",", Arrays.asList(PluginHandler.UNMAINTAINED_PLUGINS)));
            List<PluginInformation> list = PluginHandler.buildListOfPluginsToLoad(null, null);
            assertNotNull(list);
            assertTrue(list.isEmpty());
        } finally {
            if (old != null) {
                System.setProperty("josm.plugins", old);
            } else {
                System.clearProperty("josm.plugins");
            }
        }
    }
}
