// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import mockit.Expectations;
import mockit.Mocked;

/**
 * Unit tests of {@link PlatformHookWindows} class.
 */
public class PlatformHookWindowsTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().https();

    static PlatformHookWindows hook;

    /**
     * Setup test.
     */
    @BeforeAll
    public static void setUp() {
        hook = new PlatformHookWindows();
    }

    /**
     * Test method for {@code PlatformHookWindows#startupHook}
     */
    @Test
    public void testStartupHook() {
        hook.startupHook((a, b, c, d) -> System.out.println("callback"));
    }

    /**
     * Test method for {@code PlatformHookWindows#getRootKeystore}
     * @throws Exception if an error occurs
     */
    @Test
    public void testGetRootKeystore() throws Exception {
        if (PlatformManager.isPlatformWindows()) {
            assertNotNull(PlatformHookWindows.getRootKeystore());
        } else {
            try {
                PlatformHookWindows.getRootKeystore();
                fail("Expected KeyStoreException");
            } catch (KeyStoreException e) {
                Logging.info(e.getMessage());
            }
        }
    }

    /**
     * Test method for {@code PlatformHookWindows#afterPrefStartupHook}
     */
    @Test
    public void testAfterPrefStartupHook() {
        hook.afterPrefStartupHook();
    }

    /**
     * Test method for {@code PlatformHookWindows#openUrl} when Desktop works as expected
     * @param mockDesktop desktop mock
     * @throws IOException if an error occurs
     */
    @Test
    public void testOpenUrlSuccess(@Mocked final Desktop mockDesktop) throws IOException {
        TestUtils.assumeWorkingJMockit();
        new Expectations() {{
            // real implementation would raise HeadlessException
            Desktop.getDesktop(); result = mockDesktop; times = 1;
            mockDesktop.browse(withNotNull()); times = 1;
        }};

        hook.openUrl(Config.getUrls().getJOSMWebsite());
    }

    /**
     * Test method for {@code PlatformHookWindows#openUrl} when Desktop fails
     * @param mockDesktop desktop mock
     * @param anyRuntime runtime mock
     * @throws IOException if an error occurs
     */
    @Test
    public void testOpenUrlFallback(@Mocked final Desktop mockDesktop, @Mocked Runtime anyRuntime) throws IOException {
        TestUtils.assumeWorkingJMockit();
        new Expectations() {{
            // real implementation would raise HeadlessException
            Desktop.getDesktop(); result = mockDesktop; times = 1;
            mockDesktop.browse(withNotNull()); result = new IOException(); times = 1;

            // mock rundll32 in Runtime
            Runtime.getRuntime(); result = anyRuntime; times = 1;
            anyRuntime.exec(new String[] {"rundll32", "url.dll,FileProtocolHandler", Config.getUrls().getJOSMWebsite()});
            result = null;
            times = 1;
            // prevent a non-matching invocation being executed
            anyRuntime.exec((String[]) withNotNull()); result = null; times = 0;
        }};

        hook.openUrl(Config.getUrls().getJOSMWebsite());
    }

    /**
     * Test method for {@code PlatformHookWindows#getAdditionalFonts}
     */
    @Test
    public void testGetAdditionalFonts() {
        assertFalse(hook.getAdditionalFonts().isEmpty());
    }

    /**
     * Test method for {@code PlatformHookWindows#getDefaultCacheDirectory}
     */
    @Test
    public void testGetDefaultCacheDirectory() {
        File cache = hook.getDefaultCacheDirectory();
        assertNotNull(cache);
        if (PlatformManager.isPlatformWindows()) {
            assertTrue(cache.toString().contains(":"));
        }
    }

    /**
     * Test method for {@code PlatformHookWindows#getDefaultPrefDirectory}
     */
    @Test
    public void testGetDefaultPrefDirectory() {
        File cache = hook.getDefaultPrefDirectory();
        assertNotNull(cache);
        if (PlatformManager.isPlatformWindows()) {
            assertTrue(cache.toString().contains(":"));
        }
    }

    /**
     * Test method for {@code PlatformHookWindows#getDefaultStyle}
     */
    @Test
    public void testGetDefaultStyle() {
        assertEquals("com.sun.java.swing.plaf.windows.WindowsLookAndFeel", hook.getDefaultStyle());
    }

    /**
     * Test method for {@code PlatformHookWindows#getInstalledFonts}
     */
    @Test
    public void testGetInstalledFonts() {
        Collection<String> fonts = hook.getInstalledFonts();
        if (PlatformManager.isPlatformWindows()) {
            assertFalse(fonts.isEmpty());
        } else {
            assertNull(fonts);
        }
    }

    /**
     * Test method for {@code PlatformHookWindows#getOSDescription}
     */
    @Test
    public void testGetOSDescription() {
        String os = hook.getOSDescription();
        if (PlatformManager.isPlatformWindows()) {
            assertTrue(os.contains("Windows"));
        } else {
            assertFalse(os.contains("Windows"));
        }
    }

    /**
     * Test method for {@code PlatformHookWindows#initSystemShortcuts}
     */
    @Test
    public void testInitSystemShortcuts() {
        hook.initSystemShortcuts();
    }
}
