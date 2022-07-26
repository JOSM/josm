// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link PlatformHookOsx} class.
 */
@BasicPreferences
class PlatformHookOsxTest {

    static PlatformHookOsx hook;

    /**
     * Setup test.
     */
    @BeforeAll
    public static void setUp() {
        hook = new PlatformHookOsx();
    }

    /**
     * Test method for {@code PlatformHookOsx#startupHook}
     */
    @Test
    void testStartupHook() {
        hook.startupHook((a, b, c, d) -> System.out.println("java callback"), u -> System.out.println("webstart callback"));
    }

    /**
     * Test method for {@code PlatformHookOsx#afterPrefStartupHook}
     */
    @Test
    void testAfterPrefStartupHook() {
        hook.afterPrefStartupHook();
    }

    /**
     * Test method for {@code PlatformHookOsx#openUrl}
     * @throws IOException if an error occurs
     */
    @Test
    void testOpenUrl() throws IOException {
        assumeTrue(PlatformManager.isPlatformOsx());
        hook.openUrl(Config.getUrls().getJOSMWebsite());
    }

    /**
     * Test method for {@code PlatformHookOsx#getDefaultCacheDirectory}
     */
    @Test
    void testGetDefaultCacheDirectory() {
        File cache = hook.getDefaultCacheDirectory();
        assertNotNull(cache);
        if (PlatformManager.isPlatformOsx()) {
            assertTrue(cache.toString().contains("/Library/"));
        }
    }

    /**
     * Test method for {@code PlatformHookOsx#getDefaultPrefDirectory}
     */
    @Test
    void testGetDefaultPrefDirectory() {
        File cache = hook.getDefaultPrefDirectory();
        assertNotNull(cache);
        if (PlatformManager.isPlatformOsx()) {
            assertTrue(cache.toString().contains("/Library/"));
        }
    }

    /**
     * Test method for {@code PlatformHookOsx#getDefaultStyle}
     */
    @Test
    void testGetDefaultStyle() {
        assertEquals("com.apple.laf.AquaLookAndFeel", hook.getDefaultStyle());
    }

    /**
     * Test method for {@code PlatformHookOsx#getOSDescription}
     */
    @Test
    void testGetOSDescription() {
        String os = hook.getOSDescription();
        if (PlatformManager.isPlatformOsx()) {
            assertTrue(os.contains("Mac"));
        } else {
            assertFalse(os.contains("Mac"));
        }
    }

    /**
     * Test method for {@code PlatformHookOsx#initSystemShortcuts}
     */
    @Test
    void testInitSystemShortcuts() {
        hook.initSystemShortcuts();
    }
}
