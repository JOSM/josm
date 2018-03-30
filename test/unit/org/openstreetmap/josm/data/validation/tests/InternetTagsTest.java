// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.openstreetmap.josm.tools.I18n.tr;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.routines.AbstractValidator;
import org.openstreetmap.josm.data.validation.routines.EmailValidator;
import org.openstreetmap.josm.data.validation.routines.UrlValidator;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * JUnit Test of "Internet Tags" validation test.
 */
public class InternetTagsTest {

    private static final InternetTags TEST = new InternetTags();

    /**
     * Setup test by initializing JOSM preferences and projection.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test of valid URLs.
     */
    @Test
    public void testValidUrls() {
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
        testUrl("website", "http://www.dasideenreich.online", true);           // see #12257: new TLD added August 19, 2015
    }

    /**
     * Test of invalid URLs.
     */
    @Test
    public void testInvalidUrls() {
        testUrl("url", "something://www.domain.com", false);                   // invalid protocol
        testUrl("url", "http://www.domain.invalidtld", false);                 // invalid TLD
    }

    /**
     * Test of valid e-mails.
     */
    @Test
    public void testValidEmails() {
        testEmail("email", "contact@www.domain.com", true);                    // Simple email
        testEmail("contact:email", "john.doe@other-domain.org", true);         // Key with : + dash in domain
    }

    /**
     * Test of invalid e-mails.
     */
    @Test
    public void testInvalidEmails() {
        testEmail("email", "contact at www.domain.com", false);                // No @
        testEmail("contact:email", "john.doe@other-domain.invalidtld", false); // invalid TLD
    }

    /**
     * Test of invalid slashes.
     */
    @Test
    public void testInvalidSlashes() {
        TestError error = testUrl("website", "http:\\\\www.sjoekurs.no", false);
        assertEquals(tr("''{0}'': {1}", "website", tr("URL contains backslashes instead of slashes")), error.getDescription());
        assertNotNull(error.getFix());
    }

    private static TestError testKey(String key, String value, boolean valid, AbstractValidator validator, int code) {
        TestError error = TEST.validateTag(TestUtils.addFakeDataSet(TestUtils.newNode(key+"="+value+"")), key, validator, code);
        if (valid) {
            assertNull(error != null ? error.getMessage() : null, error);
        } else {
            assertNotNull(error);
        }
        return error;
    }

    private static TestError testUrl(String key, String value, boolean valid) {
        return testKey(key, value, valid, UrlValidator.getInstance(), InternetTags.INVALID_URL);
    }

    private static TestError testEmail(String key, String value, boolean valid) {
        return testKey(key, value, valid, EmailValidator.getInstance(), InternetTags.INVALID_EMAIL);
    }
}
