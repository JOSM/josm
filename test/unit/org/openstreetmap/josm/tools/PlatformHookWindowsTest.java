// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assert.fail;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.KeyStoreException;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.io.remotecontrol.RemoteControlHttpsServer;
import org.openstreetmap.josm.io.remotecontrol.RemoteControlTest;
import org.openstreetmap.josm.spi.preferences.Config;

import mockit.Expectations;
import mockit.Injectable;

/**
 * Unit tests of {@link PlatformHookWindows} class.
 */
public class PlatformHookWindowsTest {

    static PlatformHookWindows hook;

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
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
     * Test method for {@code PlatformHookWindows#removeInsecureCertificates}
     * @throws Exception if an error occurs
     */
    @Test
    public void testRemoveInsecureCertificates() throws Exception {
        if (PlatformManager.isPlatformWindows()) {
            PlatformHookWindows.removeInsecureCertificates();
        } else {
            try {
                PlatformHookWindows.removeInsecureCertificates();
                fail("Expected KeyStoreException");
            } catch (KeyStoreException e) {
                Logging.info(e.getMessage());
            }
        }
    }

    /**
     * Test method for {@code PlatformHookWindows#setupHttpsCertificate}
     * @throws Exception if an error occurs
     */
    @Test
    public void testSetupHttpsCertificate() throws Exception {
        // appveyor doesn't like us tinkering with the root keystore
        assumeFalse(PlatformManager.isPlatformWindows() && "True".equals(System.getenv("APPVEYOR")));

        RemoteControlTest.deleteKeystore();
        KeyStore ks = RemoteControlHttpsServer.loadJosmKeystore();
        TrustedCertificateEntry trustedCert = new KeyStore.TrustedCertificateEntry(ks.getCertificate(ks.aliases().nextElement()));
        if (PlatformManager.isPlatformWindows()) {
            hook.setupHttpsCertificate(RemoteControlHttpsServer.ENTRY_ALIAS, trustedCert);
        } else {
            try {
                hook.setupHttpsCertificate(RemoteControlHttpsServer.ENTRY_ALIAS, trustedCert);
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
    public void testOpenUrlSuccess(@Injectable final Desktop mockDesktop) throws IOException {
        TestUtils.assumeWorkingJMockit();
        new Expectations(Desktop.class) {{
            // real implementation would raise HeadlessException
            Desktop.getDesktop(); result = mockDesktop; times = 1;
        }};
        new Expectations() {{
            mockDesktop.browse(withNotNull()); times = 1;
        }};

        hook.openUrl(Config.getUrls().getJOSMWebsite());
    }

    /**
     * Test method for {@code PlatformHookWindows#openUrl} when Desktop fails
     * @param mockDesktop desktop mock
     * @throws IOException if an error occurs
     */
    @Test
    public void testOpenUrlFallback(@Injectable final Desktop mockDesktop) throws IOException {
        TestUtils.assumeWorkingJMockit();
        new Expectations(Desktop.class) {{
            // real implementation would raise HeadlessException
            Desktop.getDesktop(); result = mockDesktop; times = 1;
        }};
        new Expectations() {{
            mockDesktop.browse(withNotNull()); result = new IOException(); times = 1;
        }};
        final Runtime anyRuntime = Runtime.getRuntime();
        new Expectations(Runtime.class) {{
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
