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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openstreetmap.josm.data.validation.routines.DomainValidator.ArrayType;
import org.openstreetmap.josm.tools.Logging;

/**
 * Tests for the DomainValidator.
 *
 * @version $Revision: 1741724 $
 */
class DomainValidatorTest {

    private DomainValidator validator;

    /**
     * Setup test.
     */
    @BeforeEach
    public void setUp() {
        validator = DomainValidator.getInstance();
        DomainValidator.clearTLDOverrides(); // N.B. this clears the inUse flag, allowing overrides
    }

    /**
     * Test valid domains.
     */
    @Test
    void testValidDomains() {
        assertTrue(validator.isValid("apache.org"), "apache.org should validate");
        assertTrue(validator.isValid("www.google.com"), "www.google.com should validate");

        assertTrue(validator.isValid("test-domain.com"), "test-domain.com should validate");
        assertTrue(validator.isValid("test---domain.com"), "test---domain.com should validate");
        assertTrue(validator.isValid("test-d-o-m-ain.com"), "test-d-o-m-ain.com should validate");
        assertTrue(validator.isValid("as.uk"), "two-letter domain label should validate");

        assertTrue(validator.isValid("ApAchE.Org"), "case-insensitive ApAchE.Org should validate");

        assertTrue(validator.isValid("z.com"), "single-character domain label should validate");

        assertTrue(validator.isValid("i.have.an-example.domain.name"), "i.have.an-example.domain.name should validate");
    }

    /**
     * Test invalid domains.
     */
    @Test
    void testInvalidDomains() {
        assertFalse(validator.isValid(".org"), "bare TLD .org shouldn't validate");
        assertFalse(validator.isValid(" apache.org "), "domain name with spaces shouldn't validate");
        assertFalse(validator.isValid("apa che.org"), "domain name containing spaces shouldn't validate");
        assertFalse(validator.isValid("-testdomain.name"), "domain name starting with dash shouldn't validate");
        assertFalse(validator.isValid("testdomain-.name"), "domain name ending with dash shouldn't validate");
        assertFalse(validator.isValid("---c.com"), "domain name starting with multiple dashes shouldn't validate");
        assertFalse(validator.isValid("c--.com"), "domain name ending with multiple dashes shouldn't validate");
        assertFalse(validator.isValid("apache.rog"), "domain name with invalid TLD shouldn't validate");

        assertFalse(validator.isValid("http://www.apache.org"), "URL shouldn't validate");
        assertFalse(validator.isValid(" "), "Empty string shouldn't validate as domain name");
        assertFalse(validator.isValid(null), "Null shouldn't validate as domain name");
    }

    /**
     * Test top-level domains.
     */
    @Test
    void testTopLevelDomains() {
        // infrastructure TLDs
        assertTrue(validator.isValidInfrastructureTld(".arpa"), ".arpa should validate as iTLD");
        assertFalse(validator.isValidInfrastructureTld(".com"), ".com shouldn't validate as iTLD");

        // generic TLDs
        assertTrue(validator.isValidGenericTld(".name"), ".name should validate as gTLD");
        assertFalse(validator.isValidGenericTld(".us"), ".us shouldn't validate as gTLD");

        // country code TLDs
        assertTrue(validator.isValidCountryCodeTld(".uk"), ".uk should validate as ccTLD");
        assertFalse(validator.isValidCountryCodeTld(".org"), ".org shouldn't validate as ccTLD");

        // case-insensitive
        assertTrue(validator.isValidTld(".COM"), ".COM should validate as TLD");
        assertTrue(validator.isValidTld(".BiZ"), ".BiZ should validate as TLD");

        // corner cases
        assertFalse(validator.isValid(".nope"), "invalid TLD shouldn't validate"); // TODO this is not guaranteed invalid forever
        assertFalse(validator.isValid(""), "empty string shouldn't validate as TLD");
        assertFalse(validator.isValid(null), "null shouldn't validate as TLD");
    }

    /**
     * Test "allow local" parameter.
     */
    @Test
    void testAllowLocal() {
       DomainValidator noLocal = DomainValidator.getInstance(false);
       DomainValidator allowLocal = DomainValidator.getInstance(true);

       // Should use singletons
       assertSame(noLocal, validator);

       // Default won't allow local
       assertFalse(noLocal.isValid("localhost.localdomain"), "localhost.localdomain should validate");
       assertFalse(noLocal.isValid("localhost"), "localhost should validate");

       // But it may be requested
       assertTrue(allowLocal.isValid("localhost.localdomain"), "localhost.localdomain should validate");
       assertTrue(allowLocal.isValid("localhost"), "localhost should validate");
       assertTrue(allowLocal.isValid("hostname"), "hostname should validate");
       assertTrue(allowLocal.isValid("machinename"), "machinename should validate");

       // Check the localhost one with a few others
       assertTrue(allowLocal.isValid("apache.org"), "apache.org should validate");
       assertFalse(allowLocal.isValid(" apache.org "), "domain name with spaces shouldn't validate");
    }

    /**
     * Test IDN.
     */
    @Test
    void testIDN() {
       assertTrue(validator.isValid("www.xn--bcher-kva.ch"), "b\u00fccher.ch in IDN should validate");
       assertTrue(validator.isValid("www.b\u00fccher.ch"), "b\u00fccher.ch should validate");
       // xn--d1abbgf6aiiy.xn--p1ai http://президент.рф
       assertTrue(validator.isValid("xn--d1abbgf6aiiy.xn--p1ai"), "xn--d1abbgf6aiiy.xn--p1ai should validate");
       assertTrue(validator.isValid("президент.рф"), "президент.рф should validate");
       assertFalse(validator.isValid("www.\uFFFD.ch"), "www.\uFFFD.ch FFFD should fail");
    }

    /**
     * RFC2396: domainlabel   = alphanum | alphanum *( alphanum | "-" ) alphanum
     */
    @Test
    void testRFC2396domainlabel() { // use fixed valid TLD
        assertTrue(validator.isValid("a.ch"), "a.ch should validate");
        assertTrue(validator.isValid("9.ch"), "9.ch should validate");
        assertTrue(validator.isValid("az.ch"), "az.ch should validate");
        assertTrue(validator.isValid("09.ch"), "09.ch should validate");
        assertTrue(validator.isValid("9-1.ch"), "9-1.ch should validate");
        assertFalse(validator.isValid("91-.ch"), "91-.ch should not validate");
        assertFalse(validator.isValid("-.ch"), "-.ch should not validate");
    }

    /**
     * RFC2396 toplabel = alpha | alpha *( alphanum | "-" ) alphanum
     */
    @Test
    void testRFC2396toplabel() {
        // These tests use non-existent TLDs so currently need to use a package protected method
        assertTrue(validator.isValidDomainSyntax("a.c"), "a.c (alpha) should validate");
        assertTrue(validator.isValidDomainSyntax("a.cc"), "a.cc (alpha alpha) should validate");
        assertTrue(validator.isValidDomainSyntax("a.c9"), "a.c9 (alpha alphanum) should validate");
        assertTrue(validator.isValidDomainSyntax("a.c-9"), "a.c-9 (alpha - alphanum) should validate");
        assertTrue(validator.isValidDomainSyntax("a.c-z"), "a.c-z (alpha - alpha) should validate");

        assertFalse(validator.isValidDomainSyntax("a.9c"), "a.9c (alphanum alpha) should fail");
        assertFalse(validator.isValidDomainSyntax("a.c-"), "a.c- (alpha -) should fail");
        assertFalse(validator.isValidDomainSyntax("a.-"), "a.- (-) should fail");
        assertFalse(validator.isValidDomainSyntax("a.-9"), "a.-9 (- alphanum) should fail");
    }

    /**
     * rfc1123
     */
    @Test
    void testDomainNoDots() {
        assertTrue(validator.isValidDomainSyntax("a"), "a (alpha) should validate");
        assertTrue(validator.isValidDomainSyntax("9"), "9 (alphanum) should validate");
        assertTrue(validator.isValidDomainSyntax("c-z"), "c-z (alpha - alpha) should validate");

        assertFalse(validator.isValidDomainSyntax("c-"), "c- (alpha -) should fail");
        assertFalse(validator.isValidDomainSyntax("-c"), "-c (- alpha) should fail");
        assertFalse(validator.isValidDomainSyntax("-"), "- (-) should fail");
    }

    /**
     * Non-regression test for VALIDATOR-297
     */
    @Test
    void testValidator297() {
        assertTrue(validator.isValid("xn--d1abbgf6aiiy.xn--p1ai"), "xn--d1abbgf6aiiy.xn--p1ai should validate"); // This uses a valid TLD
     }

    /**
     * Non-regression test for VALIDATOR-306
     * labels are a max of 63 chars and domains 253
     */
    @Test
    void testValidator306() {
        final String longString = "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz0123456789A";
        assertEquals(63, longString.length()); // 26 * 2 + 11

        assertTrue(validator.isValidDomainSyntax(longString+".com"), "63 chars label should validate");
        assertFalse(validator.isValidDomainSyntax(longString+"x.com"), "64 chars label should fail");

        assertTrue(validator.isValidDomainSyntax("test."+longString), "63 chars TLD should validate");
        assertFalse(validator.isValidDomainSyntax("test.x"+longString), "64 chars TLD should fail");

        final String longDomain =
                longString
                + "." + longString
                + "." + longString
                + "." + longString.substring(0, 61);
        assertEquals(253, longDomain.length());
        assertTrue(validator.isValidDomainSyntax(longDomain), "253 chars domain should validate");
        assertFalse(validator.isValidDomainSyntax(longDomain+"x"), "254 chars domain should fail");
    }

    /**
     *  Check that IDN.toASCII behaves as it should (when wrapped by DomainValidator.unicodeToASCII)
     *  Tests show that method incorrectly trims a trailing "." character
     */
    @Test
    void testUnicodeToASCII() {
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
    @ParameterizedTest
    @ValueSource(strings = {"COUNTRY_CODE_TLDS", "GENERIC_TLDS", "INFRASTRUCTURE_TLDS", "LOCAL_TLDS"})
    void testArraySortedAndLowerCase(String arrayName) throws Exception {
        final boolean sorted = isSortedLowerCase(arrayName);
        assertTrue(sorted);
    }

    /**
     * Test enum visibility
     */
    @Test
    void testEnumIsPublic() {
        assertTrue(Modifier.isPublic(DomainValidator.ArrayType.class.getModifiers()));
    }

    /**
     * Test update base arrays
     */
    @ParameterizedTest
    @EnumSource(value = ArrayType.class, mode = EnumSource.Mode.MATCH_ALL, names = "^.*RO$")
    void testUpdateBaseArrays(ArrayType type) {
        Logging.debug(assertThrows(IllegalArgumentException.class, () -> DomainValidator.updateTLDOverride(type, "com")));
    }

    /**
     * Test get array.
     */
    @ParameterizedTest
    @EnumSource(ArrayType.class)
    void testGetArray(ArrayType type) {
        assertNotNull(DomainValidator.getTLDEntries(type));
    }

    /**
     * Test update country code.
     */
    @Test
    void testUpdateCountryCode() {
        assertFalse(validator.isValidCountryCodeTld("com")); // cannot be valid
        DomainValidator.updateTLDOverride(ArrayType.COUNTRY_CODE_PLUS, "com");
        assertTrue(validator.isValidCountryCodeTld("com")); // it is now!
        DomainValidator.updateTLDOverride(ArrayType.COUNTRY_CODE_MINUS, "com");
        assertFalse(validator.isValidCountryCodeTld("com")); // show that minus overrides the rest

        assertTrue(validator.isValidCountryCodeTld("ch"));
        DomainValidator.updateTLDOverride(ArrayType.COUNTRY_CODE_MINUS, "ch");
        assertFalse(validator.isValidCountryCodeTld("ch"));
        DomainValidator.updateTLDOverride(ArrayType.COUNTRY_CODE_MINUS, "xx");
        assertTrue(validator.isValidCountryCodeTld("ch"));
    }

    /**
     * Test update generic.
     */
    @Test
    void testUpdateGeneric() {
        assertFalse(validator.isValidGenericTld("ch")); // cannot be valid
        DomainValidator.updateTLDOverride(ArrayType.GENERIC_PLUS, "ch");
        assertTrue(validator.isValidGenericTld("ch")); // it is now!
        DomainValidator.updateTLDOverride(ArrayType.GENERIC_MINUS, "ch");
        assertFalse(validator.isValidGenericTld("ch")); // show that minus overrides the rest

        assertTrue(validator.isValidGenericTld("com"));
        DomainValidator.updateTLDOverride(ArrayType.GENERIC_MINUS, "com");
        assertFalse(validator.isValidGenericTld("com"));
        DomainValidator.updateTLDOverride(ArrayType.GENERIC_MINUS, "xx"); // change the minus list
        assertTrue(validator.isValidGenericTld("com"));
    }

    /**
     * Test cannot update.
     */
    @Test
    void testCannotUpdate() {
        DomainValidator.updateTLDOverride(ArrayType.GENERIC_PLUS, "ch"); // OK
        DomainValidator dv = DomainValidator.getInstance();
        assertNotNull(dv);
        try {
            DomainValidator.updateTLDOverride(ArrayType.GENERIC_PLUS, "ch");
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
    void testValidatorName() {
        assertNull(DomainValidator.getInstance().getValidatorName());
    }
}
