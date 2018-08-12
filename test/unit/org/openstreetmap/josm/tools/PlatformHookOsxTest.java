// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Unit tests of {@link PlatformHookOsx} class.
 */
public class PlatformHookOsxTest {

    static PlatformHookOsx hook;

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
        hook = new PlatformHookOsx();
    }

    /**
     * Test method for {@code PlatformHookOsx#startupHook}
     */
    @Test
    public void testStartupHook() {
        hook.startupHook((a, b, c, d) -> System.out.println("callback"));
    }

    /**
     * Test method for {@code PlatformHookOsx#setupHttpsCertificate}
     * @throws Exception if an error occurs
     */
    @Test
    public void testSetupHttpsCertificate() throws Exception {
        assertFalse(hook.setupHttpsCertificate(null, null));
    }

    /**
     * Test method for {@code PlatformHookOsx#afterPrefStartupHook}
     */
    @Test
    public void testAfterPrefStartupHook() {
        hook.afterPrefStartupHook();
    }

    /**
     * Test method for {@code PlatformHookOsx#openUrl}
     * @throws IOException if an error occurs
     */
    @Test
    public void testOpenUrl() throws IOException {
        if (!PlatformManager.isPlatformWindows()) {
            hook.openUrl(Config.getUrls().getJOSMWebsite());
        } else {
            try {
                hook.openUrl(Config.getUrls().getJOSMWebsite());
                fail("Expected IOException");
            } catch (IOException e) {
                Logging.info(e.getMessage());
            }
        }
    }

    /**
     * Test method for {@code PlatformHookOsx#getDefaultCacheDirectory}
     */
    @Test
    public void testGetDefaultCacheDirectory() {
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
    public void testGetDefaultPrefDirectory() {
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
    public void testGetDefaultStyle() {
        assertEquals("com.apple.laf.AquaLookAndFeel", hook.getDefaultStyle());
    }

    /**
     * Test method for {@code PlatformHookOsx#getOSDescription}
     */
    @Test
    public void testGetOSDescription() {
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
    public void testInitSystemShortcuts() {
        hook.initSystemShortcuts();
    }
}
