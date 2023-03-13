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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.PatternSyntaxException;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.tools.Logging;

/**
 * Test Case for RegexValidatorTest.
 *
 * @version $Revision: 1741724 $
 * @since Validator 1.4
 */
class RegexValidatorTest {

    private static final String REGEX = "^([abc]*)(?:\\-)([DEF]*)(?:\\-)([123]*)$";

    private static final String COMPONENT_1 = "([abc]{3})";
    private static final String COMPONENT_2 = "([DEF]{3})";
    private static final String COMPONENT_3 = "([123]{3})";
    private static final String SEPARATOR_1 = "(?:\\-)";
    private static final String SEPARATOR_2 = "(?:\\s)";
    private static final String REGEX_1 = "^" + COMPONENT_1 + SEPARATOR_1 + COMPONENT_2 + SEPARATOR_1 + COMPONENT_3 + "$";
    private static final String REGEX_2 = "^" + COMPONENT_1 + SEPARATOR_2 + COMPONENT_2 + SEPARATOR_2 + COMPONENT_3 + "$";
    private static final String REGEX_3 = "^" + COMPONENT_1 + COMPONENT_2 + COMPONENT_3 + "$";
    private static final String[] MULTIPLE_REGEX = new String[] {REGEX_1, REGEX_2, REGEX_3};

    // CHECKSTYLE.OFF: SingleSpaceSeparator

    /**
     * Test instance methods with single regular expression.
     */
    @Test
    void testSingle() {
        RegexValidator sensitive   = new RegexValidator(REGEX);
        RegexValidator insensitive = new RegexValidator(REGEX, false);

        // isValid()
        assertTrue(sensitive.isValid("ac-DE-1"), "Sensitive isValid() valid");
        assertFalse(sensitive.isValid("AB-de-1"), "Sensitive isValid() invalid");
        assertTrue(insensitive.isValid("AB-de-1"), "Insensitive isValid() valid");
        assertFalse(insensitive.isValid("ABd-de-1"), "Insensitive isValid() invalid");

        // validate()
        assertEquals("acDE1", sensitive.validate("ac-DE-1"), "Sensitive validate() valid");
        assertNull(sensitive.validate("AB-de-1"), "Sensitive validate() invalid");
        assertEquals("ABde1", insensitive.validate("AB-de-1"), "Insensitive validate() valid");
        assertNull(insensitive.validate("ABd-de-1"), "Insensitive validate() invalid");

        // match()
        String[] result2 = sensitive.match("ac-DE-1");
        assertArrayEquals(new String[] {"ac", "DE", "1"}, result2, "Sensitive match() valid");
        assertNull(sensitive.match("AB-de-1"), "Sensitive match() invalid");
        String[] result1 = insensitive.match("AB-de-1");
        assertArrayEquals(new String[] {"AB", "de", "1"}, result1, "Insensitive match() valid");
        assertNull(insensitive.match("ABd-de-1"), "Insensitive match() invalid");
        assertEquals("ABC", (new RegexValidator("^([A-Z]*)$")).validate("ABC"), "validate one");
        String[] result = (new RegexValidator("^([A-Z]*)$")).match("ABC");
        assertArrayEquals(new String[] {"ABC"}, result, "match one");
    }

    /**
     * Test with multiple regular expressions (case sensitive).
     */
    @Test
    void testMultipleSensitive() {

        // ------------ Set up Sensitive Validators
        RegexValidator multiple = new RegexValidator(MULTIPLE_REGEX);
        RegexValidator single1  = new RegexValidator(REGEX_1);
        RegexValidator single2  = new RegexValidator(REGEX_2);
        RegexValidator single3  = new RegexValidator(REGEX_3);

        // ------------ Set up test values
        String value = "aac FDE 321";
        String expect = "aacFDE321";
        String[] array = new String[] {"aac", "FDE", "321"};

        // isValid()
        assertTrue(multiple.isValid(value), "Sensitive isValid() Multiple");
        assertFalse(single1.isValid(value), "Sensitive isValid() 1st");
        assertTrue(single2.isValid(value), "Sensitive isValid() 2nd");
        assertFalse(single3.isValid(value), "Sensitive isValid() 3rd");

        // validate()
        assertEquals(expect, multiple.validate(value), "Sensitive validate() Multiple");
        assertNull(single1.validate(value), "Sensitive validate() 1st");
        assertEquals(expect, single2.validate(value), "Sensitive validate() 2nd");
        assertNull(single3.validate(value), "Sensitive validate() 3rd");

        // match()
        assertArrayEquals(array, multiple.match(value), "Sensitive match() Multiple");
        assertNull(single1.match(value), "Sensitive match() 1st");
        assertArrayEquals(array, single2.match(value), "Sensitive match() 2nd");
        assertNull(single3.match(value), "Sensitive match() 3rd");

        // All invalid
        value = "AAC*FDE*321";
        assertFalse(multiple.isValid(value), "isValid() Invalid");
        assertNull(multiple.validate(value), "validate() Invalid");
        assertNull(multiple.match(value), "match() Multiple");
    }

    /**
     * Test with multiple regular expressions (case in-sensitive).
     */
    @Test
    void testMultipleInsensitive() {

        // ------------ Set up In-sensitive Validators
        RegexValidator multiple = new RegexValidator(MULTIPLE_REGEX, false);
        RegexValidator single1  = new RegexValidator(REGEX_1, false);
        RegexValidator single2  = new RegexValidator(REGEX_2, false);
        RegexValidator single3  = new RegexValidator(REGEX_3, false);

        // ------------ Set up test values
        String value = "AAC FDE 321";
        String expect = "AACFDE321";
        String[] array = new String[] {"AAC", "FDE", "321"};

        // isValid()
        assertTrue(multiple.isValid(value), "isValid() Multiple");
        assertFalse(single1.isValid(value), "isValid() 1st");
        assertTrue(single2.isValid(value), "isValid() 2nd");
        assertFalse(single3.isValid(value), "isValid() 3rd");

        // validate()
        assertEquals(expect, multiple.validate(value), "validate() Multiple");
        assertNull(single1.validate(value), "validate() 1st");
        assertEquals(expect, single2.validate(value), "validate() 2nd");
        assertNull(single3.validate(value), "validate() 3rd");

        // match()
        assertArrayEquals(array, multiple.match(value), "match() Multiple");
        assertNull(single1.match(value), "match() 1st");
        assertArrayEquals(array, single2.match(value), "match() 2nd");
        assertNull(single3.match(value), "match() 3rd");

        // All invalid
        value = "AAC*FDE*321";
        assertFalse(multiple.isValid(value), "isValid() Invalid");
        assertNull(multiple.validate(value), "validate() Invalid");
        assertNull(multiple.match(value), "match() Multiple");
    }

    /**
     * Test Null value
     */
    @Test
    void testNullValue() {
        RegexValidator validator = new RegexValidator(REGEX);
        assertFalse(validator.isValid(null), "Instance isValid()");
        assertNull(validator.validate(null), "Instance validate()");
        assertNull(validator.match(null), "Instance match()");
    }

    // CHECKSTYLE.ON: SingleSpaceSeparator

    /**
     * Test exceptions
     */
    @Test
    void testMissingRegex() {
        // Single Regular Expression - null
        IllegalArgumentException iae = assertThrows(IllegalArgumentException.class, () -> new RegexValidator((String) null),
                "Single Null - expected IllegalArgumentException");
        assertEquals("Regular expression[0] is missing", iae.getMessage(), "Single Null");

        // Single Regular Expression - Zero Length
        iae = assertThrows(IllegalArgumentException.class, () -> new RegexValidator(""),
                "Single Zero Length - expected IllegalArgumentException");
        assertEquals("Regular expression[0] is missing", iae.getMessage(), "Single Zero Length");

        // Multiple Regular Expression - Null array
        iae = assertThrows(IllegalArgumentException.class, () -> new RegexValidator((String[]) null),
                "Null Array - expected IllegalArgumentException");
        assertEquals("Regular expressions are missing", iae.getMessage(), "Null Array");

        // Multiple Regular Expression - Zero Length array
        iae = assertThrows(IllegalArgumentException.class, RegexValidator::new,
                "Zero Length Array - expected IllegalArgumentException");
        assertEquals("Regular expressions are missing", iae.getMessage(), "Zero Length Array");

        // Multiple Regular Expression - Array has Null
        iae = assertThrows(IllegalArgumentException.class, () -> new RegexValidator("ABC", null),
                "Array has Null - expected IllegalArgumentException");
        assertEquals("Regular expression[1] is missing", iae.getMessage(), "Array has Null");

        // Multiple Regular Expression - Array has Zero Length
        iae = assertThrows(IllegalArgumentException.class, () -> new RegexValidator("", "ABC"),
                "Array has Zero Length - expected IllegalArgumentException");
        assertEquals("Regular expression[0] is missing", iae.getMessage(), "Array has Zero Length");
    }

    /**
     * Test exceptions
     */
    @Test
    void testExceptions() {
        String invalidRegex = "^([abCD12]*$";
        try {
            new RegexValidator(invalidRegex);
        } catch (PatternSyntaxException e) {
            // expected
            Logging.debug(e.getMessage());
        }
    }

    /**
     * Test toString() method
     */
    @Test
    void testToString() {
        RegexValidator single = new RegexValidator(REGEX);
        assertEquals("RegexValidator{" + REGEX + "}", single.toString(), "Single");

        RegexValidator multiple = new RegexValidator(REGEX, REGEX);
        assertEquals("RegexValidator{" + REGEX + "," + REGEX + "}", multiple.toString(), "Multiple");
    }

    /**
     * Unit test of {@link RegexValidator#getValidatorName}.
     */
    @Test
    void testValidatorName() {
        assertNull(new RegexValidator(".*").getValidatorName());
    }

}
