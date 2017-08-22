/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openstreetmap.josm.data.validation.routines;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.data.validation.routines.DomainValidator.ArrayType;
import org.openstreetmap.josm.tools.Logging;

/**
 * Tests for the DomainValidator.
 *
 * @version $Revision: 1741724 $
 */
public class DomainValidatorTest {

    private DomainValidator validator;

    /**
     * Setup test.
     */
    @Before
    public void setUp() {
        validator = DomainValidator.getInstance();
        DomainValidator.clearTLDOverrides(); // N.B. this clears the inUse flag, allowing overrides
    }

    /**
     * Test valid domains.
     */
    @Test
    public void testValidDomains() {
        assertTrue("apache.org should validate", validator.isValid("apache.org"));
        assertTrue("www.google.com should validate", validator.isValid("www.google.com"));

        assertTrue("test-domain.com should validate", validator.isValid("test-domain.com"));
        assertTrue("test---domain.com should validate", validator.isValid("test---domain.com"));
        assertTrue("test-d-o-m-ain.com should validate", validator.isValid("test-d-o-m-ain.com"));
        assertTrue("two-letter domain label should validate", validator.isValid("as.uk"));

        assertTrue("case-insensitive ApAchE.Org should validate", validator.isValid("ApAchE.Org"));

        assertTrue("single-character domain label should validate", validator.isValid("z.com"));

        assertTrue("i.have.an-example.domain.name should validate", validator.isValid("i.have.an-example.domain.name"));
    }

    /**
     * Test invalid domains.
     */
    @Test
    public void testInvalidDomains() {
        assertFalse("bare TLD .org shouldn't validate", validator.isValid(".org"));
        assertFalse("domain name with spaces shouldn't validate", validator.isValid(" apache.org "));
        assertFalse("domain name containing spaces shouldn't validate", validator.isValid("apa che.org"));
        assertFalse("domain name starting with dash shouldn't validate", validator.isValid("-testdomain.name"));
        assertFalse("domain name ending with dash shouldn't validate", validator.isValid("testdomain-.name"));
        assertFalse("domain name starting with multiple dashes shouldn't validate", validator.isValid("---c.com"));
        assertFalse("domain name ending with multiple dashes shouldn't validate", validator.isValid("c--.com"));
        assertFalse("domain name with invalid TLD shouldn't validate", validator.isValid("apache.rog"));

        assertFalse("URL shouldn't validate", validator.isValid("http://www.apache.org"));
        assertFalse("Empty string shouldn't validate as domain name", validator.isValid(" "));
        assertFalse("Null shouldn't validate as domain name", validator.isValid(null));
    }

    /**
     * Test top-level domains.
     */
    @Test
    public void testTopLevelDomains() {
        // infrastructure TLDs
        assertTrue(".arpa should validate as iTLD", validator.isValidInfrastructureTld(".arpa"));
        assertFalse(".com shouldn't validate as iTLD", validator.isValidInfrastructureTld(".com"));

        // generic TLDs
        assertTrue(".name should validate as gTLD", validator.isValidGenericTld(".name"));
        assertFalse(".us shouldn't validate as gTLD", validator.isValidGenericTld(".us"));

        // country code TLDs
        assertTrue(".uk should validate as ccTLD", validator.isValidCountryCodeTld(".uk"));
        assertFalse(".org shouldn't validate as ccTLD", validator.isValidCountryCodeTld(".org"));

        // case-insensitive
        assertTrue(".COM should validate as TLD", validator.isValidTld(".COM"));
        assertTrue(".BiZ should validate as TLD", validator.isValidTld(".BiZ"));

        // corner cases
        assertFalse("invalid TLD shouldn't validate", validator.isValid(".nope")); // TODO this is not guaranteed invalid forever
        assertFalse("empty string shouldn't validate as TLD", validator.isValid(""));
        assertFalse("null shouldn't validate as TLD", validator.isValid(null));
    }

    /**
     * Test "allow local" parameter.
     */
    @Test
    public void testAllowLocal() {
       DomainValidator noLocal = DomainValidator.getInstance(false);
       DomainValidator allowLocal = DomainValidator.getInstance(true);

       // Default is false, and should use singletons
       assertEquals(noLocal, validator);

       // Default won't allow local
       assertFalse("localhost.localdomain should validate", noLocal.isValid("localhost.localdomain"));
       assertFalse("localhost should validate", noLocal.isValid("localhost"));

       // But it may be requested
       assertTrue("localhost.localdomain should validate", allowLocal.isValid("localhost.localdomain"));
       assertTrue("localhost should validate", allowLocal.isValid("localhost"));
       assertTrue("hostname should validate", allowLocal.isValid("hostname"));
       assertTrue("machinename should validate", allowLocal.isValid("machinename"));

       // Check the localhost one with a few others
       assertTrue("apache.org should validate", allowLocal.isValid("apache.org"));
       assertFalse("domain name with spaces shouldn't validate", allowLocal.isValid(" apache.org "));
    }

    /**
     * Test IDN.
     */
    @Test
    public void testIDN() {
       assertTrue("b\u00fccher.ch in IDN should validate", validator.isValid("www.xn--bcher-kva.ch"));
    }

    /**
     * Test IDN with Java >= 6.
     */
    @Test
    public void testIDNJava6OrLater() {
        String version = System.getProperty("java.version");
        if (version.compareTo("1.6") < 0) {
            System.out.println("Cannot run Unicode IDN tests");
            return; // Cannot run the test
        } // xn--d1abbgf6aiiy.xn--p1ai http://президент.рф
       assertTrue("b\u00fccher.ch should validate", validator.isValid("www.b\u00fccher.ch"));
       assertTrue("xn--d1abbgf6aiiy.xn--p1ai should validate", validator.isValid("xn--d1abbgf6aiiy.xn--p1ai"));
       assertTrue("президент.рф should validate", validator.isValid("президент.рф"));
       assertFalse("www.\uFFFD.ch FFFD should fail", validator.isValid("www.\uFFFD.ch"));
    }

    /**
     * RFC2396: domainlabel   = alphanum | alphanum *( alphanum | "-" ) alphanum
     */
    @Test
    public void testRFC2396domainlabel() { // use fixed valid TLD
        assertTrue("a.ch should validate", validator.isValid("a.ch"));
        assertTrue("9.ch should validate", validator.isValid("9.ch"));
        assertTrue("az.ch should validate", validator.isValid("az.ch"));
        assertTrue("09.ch should validate", validator.isValid("09.ch"));
        assertTrue("9-1.ch should validate", validator.isValid("9-1.ch"));
        assertFalse("91-.ch should not validate", validator.isValid("91-.ch"));
        assertFalse("-.ch should not validate", validator.isValid("-.ch"));
    }

    /**
     * RFC2396 toplabel = alpha | alpha *( alphanum | "-" ) alphanum
     */
    @Test
    public void testRFC2396toplabel() {
        // These tests use non-existent TLDs so currently need to use a package protected method
        assertTrue("a.c (alpha) should validate", validator.isValidDomainSyntax("a.c"));
        assertTrue("a.cc (alpha alpha) should validate", validator.isValidDomainSyntax("a.cc"));
        assertTrue("a.c9 (alpha alphanum) should validate", validator.isValidDomainSyntax("a.c9"));
        assertTrue("a.c-9 (alpha - alphanum) should validate", validator.isValidDomainSyntax("a.c-9"));
        assertTrue("a.c-z (alpha - alpha) should validate", validator.isValidDomainSyntax("a.c-z"));

        assertFalse("a.9c (alphanum alpha) should fail", validator.isValidDomainSyntax("a.9c"));
        assertFalse("a.c- (alpha -) should fail", validator.isValidDomainSyntax("a.c-"));
        assertFalse("a.- (-) should fail", validator.isValidDomainSyntax("a.-"));
        assertFalse("a.-9 (- alphanum) should fail", validator.isValidDomainSyntax("a.-9"));
    }

    /**
     * rfc1123
     */
    @Test
    public void testDomainNoDots() {
        assertTrue("a (alpha) should validate", validator.isValidDomainSyntax("a"));
        assertTrue("9 (alphanum) should validate", validator.isValidDomainSyntax("9"));
        assertTrue("c-z (alpha - alpha) should validate", validator.isValidDomainSyntax("c-z"));

        assertFalse("c- (alpha -) should fail", validator.isValidDomainSyntax("c-"));
        assertFalse("-c (- alpha) should fail", validator.isValidDomainSyntax("-c"));
        assertFalse("- (-) should fail", validator.isValidDomainSyntax("-"));
    }

    /**
     * Non-regression test for VALIDATOR-297
     */
    @Test
    public void testValidator297() {
        assertTrue("xn--d1abbgf6aiiy.xn--p1ai should validate", validator.isValid("xn--d1abbgf6aiiy.xn--p1ai")); // This uses a valid TLD
     }

    /**
     * Non-regression test for VALIDATOR-306
     * labels are a max of 63 chars and domains 253
     */
    @Test
    public void testValidator306() {
        final String longString = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz0123456789A";
        assertEquals(63, longString.length()); // 26 * 2 + 11

        assertTrue("63 chars label should validate", validator.isValidDomainSyntax(longString+".com"));
        assertFalse("64 chars label should fail", validator.isValidDomainSyntax(longString+"x.com"));

        assertTrue("63 chars TLD should validate", validator.isValidDomainSyntax("test."+longString));
        assertFalse("64 chars TLD should fail", validator.isValidDomainSyntax("test.x"+longString));

        final String longDomain =
                longString
                + "." + longString
                + "." + longString
                + "." + longString.substring(0, 61);
        assertEquals(253, longDomain.length());
        assertTrue("253 chars domain should validate", validator.isValidDomainSyntax(longDomain));
        assertFalse("254 chars domain should fail", validator.isValidDomainSyntax(longDomain+"x"));
    }

    /**
     *  Check that IDN.toASCII behaves as it should (when wrapped by DomainValidator.unicodeToASCII)
     *  Tests show that method incorrectly trims a trailing "." character
     */
    @Test
    public void testUnicodeToASCII() {
        String[] asciidots = {
                "",
                ",",
                ".", // fails IDN.toASCII, but should pass wrapped version
                "a.", // ditto
                "a.b",
                "a..b",
                "a...b",
                ".a",
                "..a",
        };
        for (String s : asciidots) {
            assertEquals(s, DomainValidator.unicodeToASCII(s));
        }
        // RFC3490 3.1. 1)
        // Whenever dots are used as label separators, the following
        // characters MUST be recognized as dots: U+002E (full stop), U+3002
        // (ideographic full stop), U+FF0E (fullwidth full stop), U+FF61
        // (halfwidth ideographic full stop).
        final String[][] otherDots = {
                {"b\u3002", "b."},
                {"b\uFF0E", "b."},
                {"b\uFF61", "b."},
                {"\u3002", "."},
                {"\uFF0E", "."},
                {"\uFF61", "."},
        };
        for (String[] s : otherDots) {
            assertEquals(s[1], DomainValidator.unicodeToASCII(s[0]));
        }
    }

    /**
     * Check array is sorted and is lower-case
     * @throws Exception if an error occurs
     */
    @Test
    public void test_INFRASTRUCTURE_TLDS_sortedAndLowerCase() throws Exception {
        final boolean sorted = isSortedLowerCase("INFRASTRUCTURE_TLDS");
        assertTrue(sorted);
    }

    /**
     * Check array is sorted and is lower-case
     * @throws Exception if an error occurs
     */
    @Test
    public void test_COUNTRY_CODE_TLDS_sortedAndLowerCase() throws Exception {
        final boolean sorted = isSortedLowerCase("COUNTRY_CODE_TLDS");
        assertTrue(sorted);
    }

    /**
     * Check array is sorted and is lower-case
     * @throws Exception if an error occurs
     */
    @Test
    public void test_GENERIC_TLDS_sortedAndLowerCase() throws Exception {
        final boolean sorted = isSortedLowerCase("GENERIC_TLDS");
        assertTrue(sorted);
    }

    /**
     * Check array is sorted and is lower-case
     * @throws Exception if an error occurs
     */
    @Test
    public void test_LOCAL_TLDS_sortedAndLowerCase() throws Exception {
        final boolean sorted = isSortedLowerCase("LOCAL_TLDS");
        assertTrue(sorted);
    }

    /**
     * Test enum visibility
     */
    @Test
    public void testEnumIsPublic() {
        assertTrue(Modifier.isPublic(DomainValidator.ArrayType.class.getModifiers()));
    }

    /**
     * Test update base arrays
     */
    @Test
    public void testUpdateBaseArrays() {
        try {
            DomainValidator.updateTLDOverride(ArrayType.COUNTRY_CODE_RO, new String[]{"com"});
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            // expected
            Logging.debug(iae.getMessage());
        }
        try {
            DomainValidator.updateTLDOverride(ArrayType.GENERIC_RO, new String[]{"com"});
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            // expected
            Logging.debug(iae.getMessage());
        }
        try {
            DomainValidator.updateTLDOverride(ArrayType.INFRASTRUCTURE_RO, new String[]{"com"});
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            // expected
            Logging.debug(iae.getMessage());
        }
        try {
            DomainValidator.updateTLDOverride(ArrayType.LOCAL_RO, new String[]{"com"});
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException iae) {
            // expected
            Logging.debug(iae.getMessage());
        }
    }

    /**
     * Test get array.
     */
    @Test
    public void testGetArray() {
        assertNotNull(DomainValidator.getTLDEntries(ArrayType.COUNTRY_CODE_MINUS));
        assertNotNull(DomainValidator.getTLDEntries(ArrayType.COUNTRY_CODE_PLUS));
        assertNotNull(DomainValidator.getTLDEntries(ArrayType.GENERIC_MINUS));
        assertNotNull(DomainValidator.getTLDEntries(ArrayType.GENERIC_PLUS));
        assertNotNull(DomainValidator.getTLDEntries(ArrayType.COUNTRY_CODE_RO));
        assertNotNull(DomainValidator.getTLDEntries(ArrayType.GENERIC_RO));
        assertNotNull(DomainValidator.getTLDEntries(ArrayType.INFRASTRUCTURE_RO));
        assertNotNull(DomainValidator.getTLDEntries(ArrayType.LOCAL_RO));
    }

    /**
     * Test update country code.
     */
    @Test
    public void testUpdateCountryCode() {
        assertFalse(validator.isValidCountryCodeTld("com")); // cannot be valid
        DomainValidator.updateTLDOverride(ArrayType.COUNTRY_CODE_PLUS, new String[]{"com"});
        assertTrue(validator.isValidCountryCodeTld("com")); // it is now!
        DomainValidator.updateTLDOverride(ArrayType.COUNTRY_CODE_MINUS, new String[]{"com"});
        assertFalse(validator.isValidCountryCodeTld("com")); // show that minus overrides the rest

        assertTrue(validator.isValidCountryCodeTld("ch"));
        DomainValidator.updateTLDOverride(ArrayType.COUNTRY_CODE_MINUS, new String[]{"ch"});
        assertFalse(validator.isValidCountryCodeTld("ch"));
        DomainValidator.updateTLDOverride(ArrayType.COUNTRY_CODE_MINUS, new String[]{"xx"});
        assertTrue(validator.isValidCountryCodeTld("ch"));
    }

    /**
     * Test update generic.
     */
    @Test
    public void testUpdateGeneric() {
        assertFalse(validator.isValidGenericTld("ch")); // cannot be valid
        DomainValidator.updateTLDOverride(ArrayType.GENERIC_PLUS, new String[]{"ch"});
        assertTrue(validator.isValidGenericTld("ch")); // it is now!
        DomainValidator.updateTLDOverride(ArrayType.GENERIC_MINUS, new String[]{"ch"});
        assertFalse(validator.isValidGenericTld("ch")); // show that minus overrides the rest

        assertTrue(validator.isValidGenericTld("com"));
        DomainValidator.updateTLDOverride(ArrayType.GENERIC_MINUS, new String[]{"com"});
        assertFalse(validator.isValidGenericTld("com"));
        DomainValidator.updateTLDOverride(ArrayType.GENERIC_MINUS, new String[]{"xx"}); // change the minus list
        assertTrue(validator.isValidGenericTld("com"));
    }

    /**
     * Test cannot update.
     */
    @Test
    public void testCannotUpdate() {
        DomainValidator.updateTLDOverride(ArrayType.GENERIC_PLUS, new String[]{"ch"}); // OK
        DomainValidator dv = DomainValidator.getInstance();
        assertNotNull(dv);
        try {
            DomainValidator.updateTLDOverride(ArrayType.GENERIC_PLUS, new String[]{"ch"});
            fail("Expected IllegalStateException");
        } catch (IllegalStateException ise) {
            // expected
            Logging.debug(ise.getMessage());
        }
    }

    private static boolean isSortedLowerCase(String arrayName) throws Exception {
        Field f = DomainValidator.class.getDeclaredField(arrayName);
        final boolean isPrivate = Modifier.isPrivate(f.getModifiers());
        if (isPrivate) {
            f.setAccessible(true);
        }
        String[] array = (String[]) f.get(null);
        try {
            return isSortedLowerCase(arrayName, array);
        } finally {
            if (isPrivate) {
                f.setAccessible(false);
            }
        }
    }

    private static boolean isLowerCase(String string) {
        return string.equals(string.toLowerCase(Locale.ENGLISH));
    }

    // Check if an array is strictly sorted - and lowerCase
    private static boolean isSortedLowerCase(String name, String[] array) {
        boolean sorted = true;
        boolean strictlySorted = true;
        final int length = array.length;
        boolean lowerCase = isLowerCase(array[length-1]); // Check the last entry
        for (int i = 0; i < length-1; i++) { // compare all but last entry with next
            final String entry = array[i];
            final String nextEntry = array[i+1];
            final int cmp = entry.compareTo(nextEntry);
            if (cmp > 0) { // out of order
                System.out.println("Out of order entry: " + entry + " < " + nextEntry + " in " + name);
                sorted = false;
            } else if (cmp == 0) {
                strictlySorted = false;
                System.out.println("Duplicated entry: " + entry + " in " + name);
            }
            if (!isLowerCase(entry)) {
                System.out.println("Non lowerCase entry: " + entry + " in " + name);
                lowerCase = false;
            }
        }
        return sorted && strictlySorted && lowerCase;
    }

    /**
     * Unit test of {@link DomainValidator#getValidatorName}.
     */
    @Test
    public void testValidatorName() {
        assertNull(DomainValidator.getInstance().getValidatorName());
    }
}
