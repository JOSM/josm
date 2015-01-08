// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.routines.AbstractValidator;
import org.openstreetmap.josm.data.validation.routines.EmailValidator;
import org.openstreetmap.josm.data.validation.routines.UrlValidator;

/**
 * JUnit Test of "Internet Tags" validation test.
 */
public class InternetTagsTest {

    private static InternetTags TEST;

    /**
     * Setup test by initializing JOSM preferences and projection.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
        TEST = new InternetTags();
    }

    /**
     * Test of "Internet Tags" validation test.
     */
    @Test
    public void test() {

        // Valid URLs
        testUrl("url", "www.domain.com", true);                                // No protocol
        testUrl("url", "http://josm.openstreetmap.de", true);                  // Simple HTTP
        testUrl("url", "http://josm.openstreetmap.de/", true);                 // Simple HTTP + slash
        testUrl("website", "https://www.openstreetmap.org", true);             // Simple HTTPS
        testUrl("heritage:website", "http://www.unesco.org", true);            // Key with :
        testUrl("website", "http://www.nu-lounge.today", true);                // see #10810: new TLD
        testUrl("website", "http://xn--80akeqobjv1b0d3a.xn--p1ai", true);      // see #10862: IDN URL in ASCII form
        testUrl("website", "http://xn--80akeqobjv1b0d3a.xn--p1ai/", true);     // see #10862: IDN URL in ASCII form + slash
        testUrl("website", "http://золотаяцепь.рф", true);                     // see #10862: IDN URL in Unicode form
        testUrl("website", "http://золотаяцепь.рф/", true);                    // see #10862: IDN URL in Unicode form + slash

        // Invalid URLs
        testUrl("url", "something://www.domain.com", false);                   // invalid protocol
        testUrl("url", "http://www.domain.invalidtld", false);                 // invalid TLD

        // Valid E-mails
        testEmail("email", "contact@www.domain.com", true);                    // Simple email
        testEmail("contact:email", "john.doe@other-domain.org", true);         // Key with : + dash in domain

        // Invalid E-mails
        testEmail("email", "contact at www.domain.com", false);                // No @
        testEmail("contact:email", "john.doe@other-domain.invalidtld", false); // invalid TLD
    }

    private static void testKey(String key, String value, boolean valid, AbstractValidator validator, int code) {
        TestError error = TEST.validateTag(OsmUtils.createPrimitive("node "+key+"="+value+""), key, validator, code);
        if (valid) {
            assertNull(error != null ? error.getMessage() : null, error);
        } else {
            assertNotNull(error);
        }
    }

    private static void testUrl(String key, String value, boolean valid) {
        testKey(key, value, valid, UrlValidator.getInstance(), InternetTags.INVALID_URL);
    }

    private static void testEmail(String key, String value, boolean valid) {
        testKey(key, value, valid, EmailValidator.getInstance(), InternetTags.INVALID_EMAIL);
    }
}
