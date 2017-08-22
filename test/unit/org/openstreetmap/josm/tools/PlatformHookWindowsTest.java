// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.KeyStoreException;
import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.remotecontrol.RemoteControlHttpsServer;
import org.openstreetmap.josm.io.remotecontrol.RemoteControlTest;

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
        hook.startupHook();
    }

    /**
     * Test method for {@code PlatformHookWindows#getRootKeystore}
     * @throws Exception if an error occurs
     */
    @Test
    public void testGetRootKeystore() throws Exception {
        if (Main.isPlatformWindows()) {
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
        if (Main.isPlatformWindows()) {
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
        RemoteControlTest.deleteKeystore();
        KeyStore ks = RemoteControlHttpsServer.loadJosmKeystore();
        TrustedCertificateEntry trustedCert = new KeyStore.TrustedCertificateEntry(ks.getCertificate(ks.aliases().nextElement()));
        if (Main.isPlatformWindows()) {
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
     * Test method for {@code PlatformHookWindows#openUrl}
     * @throws IOException if an error occurs
     */
    @Test
    public void testOpenUrl() throws IOException {
        if (Main.isPlatformWindows()) {
            hook.openUrl(Main.getJOSMWebsite());
        } else {
            try {
                hook.openUrl(Main.getJOSMWebsite());
                fail("Expected IOException");
            } catch (IOException e) {
                Logging.info(e.getMessage());
            }
        }
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
        if (Main.isPlatformWindows()) {
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
        if (Main.isPlatformWindows()) {
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
        if (Main.isPlatformWindows()) {
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
        if (Main.isPlatformWindows()) {
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
