// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.routines.AbstractValidator;
import org.openstreetmap.josm.data.validation.routines.EmailValidator;
import org.openstreetmap.josm.data.validation.routines.UrlValidator;

/**
 * JUnit Test of "Internet Tags" validation test.
 */
class InternetTagsTest {

    private static final InternetTags TEST = new InternetTags();

    /**
     * Test of valid URLs.
     */
    @Test
    void testValidUrls() {
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
     * Test multiple URLs.
     */
    @Test
    void testMultipleUrls() {
        testUrl("url", "http://www.domain-a.com;https://www.domain-b.com", true); // multiple values
    }

    /**
     * Test of invalid URLs.
     */
    @Test
    void testInvalidUrls() {
        testUrl("url", "something://www.domain.com", false);                   // invalid protocol
        testUrl("url", "http://www.domain.invalidtld", false);                 // invalid TLD
    }

    /**
     * Test of valid e-mails.
     */
    @Test
    void testValidEmails() {
        testEmail("email", "contact@www.domain.com", true);                    // Simple email
        testEmail("contact:email", "john.doe@other-domain.org", true);         // Key with : + dash in domain
    }

    /**
     * Test of invalid e-mails.
     */
    @Test
    void testInvalidEmails() {
        testEmail("email", "contact at www.domain.com", false);                // No @
        testEmail("contact:email", "john.doe@other-domain.invalidtld", false); // invalid TLD
    }

    /**
     * Test of invalid slashes.
     */
    @Test
    void testInvalidSlashes() {
        TestError error = testUrl("website", "http:\\\\www.sjoekurs.no", false).get(0);
        assertEquals(tr("''{0}'': {1}", "website", tr("URL contains backslashes instead of slashes")), error.getDescription());
        assertNotNull(error.getFix());
    }

    private static List<TestError> testKey(String key, String value, boolean valid, AbstractValidator validator, int code) {
        List<TestError> errors = TEST.validateTag(TestUtils.addFakeDataSet(TestUtils.newNode(key+"="+value+"")), key, validator, code);
        if (valid) {
            assertTrue(errors.isEmpty());
        } else {
            assertFalse(errors.isEmpty());
            assertNotNull(errors.get(0));
        }
        return errors;
    }

    private static List<TestError> testUrl(String key, String value, boolean valid) {
        return testKey(key, value, valid, UrlValidator.getInstance(), InternetTags.INVALID_URL);
    }

    private static List<TestError> testEmail(String key, String value, boolean valid) {
        return testKey(key, value, valid, EmailValidator.getInstance(), InternetTags.INVALID_EMAIL);
    }
}
