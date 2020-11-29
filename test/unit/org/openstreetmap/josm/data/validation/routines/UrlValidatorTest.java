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
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Performs Validation Test for url validations.
 *
 * @version $Revision: 1741724 $
 */
class UrlValidatorTest {

    private static final boolean printStatus = false;
    private static final boolean printIndex = false; //print index that indicates current scheme,host,port,path, query test were using.

    /**
     * Setup
     */
    @BeforeEach
    public void setUp() {
        for (int index = 0; index < testPartsIndex.length - 1; index++) {
            testPartsIndex[index] = 0;
        }
    }

    /**
     * Test is valid
     */
    @Test
    public void testIsValid() {
        testIsValid(testUrlParts, UrlValidator.ALLOW_ALL_SCHEMES);
        setUp();
        long options =
                UrlValidator.ALLOW_2_SLASHES
                        + UrlValidator.ALLOW_ALL_SCHEMES
                        + UrlValidator.NO_FRAGMENTS;

        testIsValid(testUrlPartsOptions, options);
    }

    /**
     * Test is valid scheme
     */
    @Test
    public void testIsValidScheme() {
        if (printStatus) {
            System.out.print("\n testIsValidScheme() ");
        }
        //UrlValidator urlVal = new UrlValidator(schemes,false,false,false);
        UrlValidator urlVal = new UrlValidator(schemes, 0);
        for (ResultPair testPair : testScheme) {
            boolean result = urlVal.isValidScheme(testPair.item);
            assertEquals(testPair.item, testPair.valid, result);
            if (printStatus) {
                if (result == testPair.valid) {
                    System.out.print('.');
                } else {
                    System.out.print('X');
                }
            }
        }
        if (printStatus) {
            System.out.println();
        }
    }

    /**
     * Create set of tests by taking the testUrlXXX arrays and
     * running through all possible permutations of their combinations.
     *
     * @param testObjects Used to create a url.
     * @param options     options
     */
    private void testIsValid(Object[] testObjects, long options) {
        UrlValidator urlVal = new UrlValidator(null, null, options);
        assertTrue(urlVal.isValid("http://www.google.com"));
        assertTrue(urlVal.isValid("http://www.google.com/"));
        int statusPerLine = 60;
        int printed = 0;
        if (printIndex) {
            statusPerLine = 6;
        }
        do {
            StringBuilder testBuffer = new StringBuilder();
            boolean expected = true;
            for (int testPartsIndexIndex = 0; testPartsIndexIndex < testPartsIndex.length; ++testPartsIndexIndex) {
                int index = testPartsIndex[testPartsIndexIndex];
                ResultPair[] part = (ResultPair[]) testObjects[testPartsIndexIndex];
                testBuffer.append(part[index].item);
                expected &= part[index].valid;
            }
            String url = testBuffer.toString();
            boolean result = urlVal.isValid(url);
            assertEquals(url, expected, result);
            if (printStatus) {
                if (printIndex) {
                    System.out.print(testPartsIndextoString());
                } else {
                    if (result == expected) {
                        System.out.print('.');
                    } else {
                        System.out.print('X');
                    }
                }
                printed++;
                if (printed == statusPerLine) {
                    System.out.println();
                    printed = 0;
                }
            }
        } while (incrementTestPartsIndex(testPartsIndex, testObjects));
        if (printStatus) {
            System.out.println();
        }
    }

    /**
     * Non-regression test for VALIDATOR-202
     */
    @Test
    void testValidator202() {
        String[] schemes = {"http", "https"};
        UrlValidator urlValidator = new UrlValidator(schemes, UrlValidator.NO_FRAGMENTS);
        assertTrue(urlValidator.isValid(
                "http://l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.l.org"));
    }

    /**
     * Non-regression test for VALIDATOR-204
     */
    @Test
    void testValidator204() {
        String[] schemes = {"http", "https"};
        UrlValidator urlValidator = new UrlValidator(schemes);
        assertTrue(urlValidator.isValid("http://tech.yahoo.com/rc/desktops/102;_ylt=Ao8yevQHlZ4On0O3ZJGXLEQFLZA5"));
    }

    /**
     * Non-regression test for VALIDATOR-218
     */
    @Test
    void testValidator218() {
        UrlValidator validator = new UrlValidator(UrlValidator.ALLOW_2_SLASHES);
        assertTrue("parentheses should be valid in URLs",
                validator.isValid("http://somewhere.com/pathxyz/file(1).html"));
    }

    /**
     * Non-regression test for VALIDATOR-235
     */
    @Test
    void testValidator235() {
        String version = System.getProperty("java.version");
        if (version.compareTo("1.6") < 0) {
            System.out.println("Cannot run Unicode IDN tests");
            return; // Cannot run the test
        }
        UrlValidator validator = new UrlValidator();
        assertTrue("xn--d1abbgf6aiiy.xn--p1ai should validate", validator.isValid("http://xn--d1abbgf6aiiy.xn--p1ai"));
        assertTrue("президент.рф should validate", validator.isValid("http://президент.рф"));
        assertTrue("www.b\u00fccher.ch should validate", validator.isValid("http://www.b\u00fccher.ch"));
        assertFalse("www.\uFFFD.ch FFFD should fail", validator.isValid("http://www.\uFFFD.ch"));
        assertTrue("www.b\u00fccher.ch should validate", validator.isValid("ftp://www.b\u00fccher.ch"));
        assertFalse("www.\uFFFD.ch FFFD should fail", validator.isValid("ftp://www.\uFFFD.ch"));
    }

    /**
     * Non-regression test for VALIDATOR-248
     */
    @Test
    void testValidator248() {
        RegexValidator regex = new RegexValidator("localhost", ".*\\.my-testing");
        UrlValidator validator = new UrlValidator(regex, 0);

        assertTrue("localhost URL should validate",
                validator.isValid("http://localhost/test/index.html"));
        assertTrue("first.my-testing should validate",
                validator.isValid("http://first.my-testing/test/index.html"));
        assertTrue("sup3r.my-testing should validate",
                validator.isValid("http://sup3r.my-testing/test/index.html"));

        assertFalse("broke.my-test should not validate",
                validator.isValid("http://broke.my-test/test/index.html"));

        assertTrue("www.apache.org should still validate",
                validator.isValid("http://www.apache.org/test/index.html"));

        // Now check using options
        validator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);

        assertTrue("localhost URL should validate",
                validator.isValid("http://localhost/test/index.html"));

        assertTrue("machinename URL should validate",
                validator.isValid("http://machinename/test/index.html"));

        assertTrue("www.apache.org should still validate",
                validator.isValid("http://www.apache.org/test/index.html"));
    }

    /**
     * Non-regression test for VALIDATOR-288
     */
    @Test
    void testValidator288() {
        UrlValidator validator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);

        assertTrue("hostname should validate",
                validator.isValid("http://hostname"));

        assertTrue("hostname with path should validate",
                validator.isValid("http://hostname/test/index.html"));

        assertTrue("localhost URL should validate",
                validator.isValid("http://localhost/test/index.html"));

        assertFalse("first.my-testing should not validate",
                validator.isValid("http://first.my-testing/test/index.html"));

        assertFalse("broke.hostname should not validate",
                validator.isValid("http://broke.hostname/test/index.html"));

        assertTrue("www.apache.org should still validate",
                validator.isValid("http://www.apache.org/test/index.html"));

        // Turn it off, and check
        validator = new UrlValidator(0);

        assertFalse("hostname should no longer validate",
                validator.isValid("http://hostname"));

        assertFalse("localhost URL should no longer validate",
                validator.isValid("http://localhost/test/index.html"));

        assertTrue("www.apache.org should still validate",
                validator.isValid("http://www.apache.org/test/index.html"));
    }

    /**
     * Non-regression test for VALIDATOR-276
     */
    @Test
    void testValidator276() {
        // file:// isn't allowed by default
        UrlValidator validator = new UrlValidator();

        assertTrue("http://apache.org/ should be allowed by default",
                validator.isValid("http://www.apache.org/test/index.html"));

        assertFalse("file:///c:/ shouldn't be allowed by default",
                validator.isValid("file:///C:/some.file"));

        assertFalse("file:///c:\\ shouldn't be allowed by default",
                validator.isValid("file:///C:\\some.file"));

        assertFalse("file:///etc/ shouldn't be allowed by default",
                validator.isValid("file:///etc/hosts"));

        assertFalse("file://localhost/etc/ shouldn't be allowed by default",
                validator.isValid("file://localhost/etc/hosts"));

        assertFalse("file://localhost/c:/ shouldn't be allowed by default",
                validator.isValid("file://localhost/c:/some.file"));

        // Turn it on, and check
        // Note - we need to enable local urls when working with file:
        validator = new UrlValidator(new String[]{"http", "file"}, UrlValidator.ALLOW_LOCAL_URLS);

        assertTrue("http://apache.org/ should be allowed by default",
                validator.isValid("http://www.apache.org/test/index.html"));

        assertTrue("file:///c:/ should now be allowed",
                validator.isValid("file:///C:/some.file"));

        // Currently, we don't support the c:\ form
        assertFalse("file:///c:\\ shouldn't be allowed",
                validator.isValid("file:///C:\\some.file"));

        assertTrue("file:///etc/ should now be allowed",
                validator.isValid("file:///etc/hosts"));

        assertTrue("file://localhost/etc/ should now be allowed",
                validator.isValid("file://localhost/etc/hosts"));

        assertTrue("file://localhost/c:/ should now be allowed",
                validator.isValid("file://localhost/c:/some.file"));

        // These are never valid
        assertFalse("file://c:/ shouldn't ever be allowed, needs file:///c:/",
                validator.isValid("file://C:/some.file"));

        assertFalse("file://c:\\ shouldn't ever be allowed, needs file:///c:/",
                validator.isValid("file://C:\\some.file"));
    }

    /**
     * Non-regression test for VALIDATOR-309
     */
    @Test
    void testValidator309() {
        UrlValidator urlValidator = new UrlValidator();
        assertTrue(urlValidator.isValid("http://sample.ondemand.com/"));
        assertTrue(urlValidator.isValid("hTtP://sample.ondemand.CoM/"));
        assertTrue(urlValidator.isValid("httpS://SAMPLE.ONEMAND.COM/"));
        urlValidator = new UrlValidator("HTTP", "HTTPS");
        assertTrue(urlValidator.isValid("http://sample.ondemand.com/"));
        assertTrue(urlValidator.isValid("hTtP://sample.ondemand.CoM/"));
        assertTrue(urlValidator.isValid("httpS://SAMPLE.ONEMAND.COM/"));
    }

    /**
     * Non-regression test for VALIDATOR-339
     */
    @Test
    void testValidator339() {
        UrlValidator urlValidator = new UrlValidator();
        assertTrue(urlValidator.isValid("http://www.cnn.com/WORLD/?hpt=sitenav")); // without
        assertTrue(urlValidator.isValid("http://www.cnn.com./WORLD/?hpt=sitenav")); // with
        assertFalse(urlValidator.isValid("http://www.cnn.com../")); // doubly dotty
        assertFalse(urlValidator.isValid("http://www.cnn.invalid/"));
        assertFalse(urlValidator.isValid("http://www.cnn.invalid./")); // check . does not affect invalid domains
    }

    /**
     * Non-regression test for VALIDATOR-339 - IDN
     */
    @Test
    void testValidator339IDN() {
        UrlValidator urlValidator = new UrlValidator();
        assertTrue(urlValidator.isValid("http://президент.рф/WORLD/?hpt=sitenav")); // without
        assertTrue(urlValidator.isValid("http://президент.рф./WORLD/?hpt=sitenav")); // with
        assertFalse(urlValidator.isValid("http://президент.рф..../")); // very dotty
        assertFalse(urlValidator.isValid("http://президент.рф.../")); // triply dotty
        assertFalse(urlValidator.isValid("http://президент.рф../")); // doubly dotty
    }

    /**
     * Non-regression test for VALIDATOR-342
     */
    @Test
    void testValidator342() {
        UrlValidator urlValidator = new UrlValidator();
        assertTrue(urlValidator.isValid("http://example.rocks/"));
        assertTrue(urlValidator.isValid("http://example.rocks"));
    }

    static boolean incrementTestPartsIndex(int[] testPartsIndex, Object[] testParts) {
        boolean carry = true;  //add 1 to lowest order part.
        boolean maxIndex = true;
        for (int testPartsIndexIndex = testPartsIndex.length - 1; testPartsIndexIndex >= 0; --testPartsIndexIndex) {
            int index = testPartsIndex[testPartsIndexIndex];
            ResultPair[] part = (ResultPair[]) testParts[testPartsIndexIndex];
            if (carry) {
                if (index < part.length - 1) {
                    index++;
                    testPartsIndex[testPartsIndexIndex] = index;
                    carry = false;
                } else {
                    testPartsIndex[testPartsIndexIndex] = 0;
                    carry = true;
                }
            }
            maxIndex &= (index == (part.length - 1));
        }

        return (!maxIndex);
    }

    private String testPartsIndextoString() {
        StringBuilder carryMsg = new StringBuilder("{");
        for (int testPartsIndexIndex = 0; testPartsIndexIndex < testPartsIndex.length; ++testPartsIndexIndex) {
            carryMsg.append(testPartsIndex[testPartsIndexIndex]);
            if (testPartsIndexIndex < testPartsIndex.length - 1) {
                carryMsg.append(',');
            } else {
                carryMsg.append('}');
            }
        }
        return carryMsg.toString();
    }

    /**
     * Non-regression test for VALIDATOR-290
     */
    @Test
    public void testValidator290() {
        UrlValidator validator = new UrlValidator();
        assertTrue(validator.isValid("http://xn--h1acbxfam.idn.icann.org/"));
        // Internationalized country code top-level domains
        assertTrue(validator.isValid("http://test.xn--lgbbat1ad8j")); //Algeria
        assertTrue(validator.isValid("http://test.xn--fiqs8s")); // China
        assertTrue(validator.isValid("http://test.xn--fiqz9s")); // China
        assertTrue(validator.isValid("http://test.xn--wgbh1c")); // Egypt
        assertTrue(validator.isValid("http://test.xn--j6w193g")); // Hong Kong
        assertTrue(validator.isValid("http://test.xn--h2brj9c")); // India
        assertTrue(validator.isValid("http://test.xn--mgbbh1a71e")); // India
        assertTrue(validator.isValid("http://test.xn--fpcrj9c3d")); // India
        assertTrue(validator.isValid("http://test.xn--gecrj9c")); // India
        assertTrue(validator.isValid("http://test.xn--s9brj9c")); // India
        assertTrue(validator.isValid("http://test.xn--xkc2dl3a5ee0h")); // India
        assertTrue(validator.isValid("http://test.xn--45brj9c")); // India
        assertTrue(validator.isValid("http://test.xn--mgba3a4f16a")); // Iran
        assertTrue(validator.isValid("http://test.xn--mgbayh7gpa")); // Jordan
        assertTrue(validator.isValid("http://test.xn--mgbc0a9azcg")); // Morocco
        assertTrue(validator.isValid("http://test.xn--ygbi2ammx")); // Palestinian Territory
        assertTrue(validator.isValid("http://test.xn--wgbl6a")); // Qatar
        assertTrue(validator.isValid("http://test.xn--p1ai")); // Russia
        assertTrue(validator.isValid("http://test.xn--mgberp4a5d4ar")); //  Saudi Arabia
        assertTrue(validator.isValid("http://test.xn--90a3ac")); // Serbia
        assertTrue(validator.isValid("http://test.xn--yfro4i67o")); // Singapore
        assertTrue(validator.isValid("http://test.xn--clchc0ea0b2g2a9gcd")); // Singapore
        assertTrue(validator.isValid("http://test.xn--3e0b707e")); // South Korea
        assertTrue(validator.isValid("http://test.xn--fzc2c9e2c")); // Sri Lanka
        assertTrue(validator.isValid("http://test.xn--xkc2al3hye2a")); // Sri Lanka
        assertTrue(validator.isValid("http://test.xn--ogbpf8fl")); // Syria
        assertTrue(validator.isValid("http://test.xn--kprw13d")); // Taiwan
        assertTrue(validator.isValid("http://test.xn--kpry57d")); // Taiwan
        assertTrue(validator.isValid("http://test.xn--o3cw4h")); // Thailand
        assertTrue(validator.isValid("http://test.xn--pgbs0dh")); // Tunisia
        assertTrue(validator.isValid("http://test.xn--mgbaam7a8h")); // United Arab Emirates
    }

    /**
     * Non-regression test for VALIDATOR-361
     */
    @Test
    public void testValidator361() {
        UrlValidator validator = new UrlValidator();
        assertTrue(validator.isValid("http://hello.tokyo/"));
    }

    /**
     * Non-regression test for VALIDATOR-363
     */
    @Test
    public void testValidator363() {
        UrlValidator urlValidator = new UrlValidator();
        assertTrue(urlValidator.isValid("http://www.example.org/a/b/hello..world"));
        assertTrue(urlValidator.isValid("http://www.example.org/a/hello..world"));
        assertTrue(urlValidator.isValid("http://www.example.org/hello.world/"));
        assertTrue(urlValidator.isValid("http://www.example.org/hello..world/"));
        assertTrue(urlValidator.isValid("http://www.example.org/hello.world"));
        assertTrue(urlValidator.isValid("http://www.example.org/hello..world"));
        assertTrue(urlValidator.isValid("http://www.example.org/..world"));
        assertTrue(urlValidator.isValid("http://www.example.org/.../world"));
        assertFalse(urlValidator.isValid("http://www.example.org/../world"));
        assertFalse(urlValidator.isValid("http://www.example.org/.."));
        assertFalse(urlValidator.isValid("http://www.example.org/../"));
        assertFalse(urlValidator.isValid("http://www.example.org/./.."));
        assertFalse(urlValidator.isValid("http://www.example.org/././.."));
        assertTrue(urlValidator.isValid("http://www.example.org/..."));
        assertTrue(urlValidator.isValid("http://www.example.org/.../"));
        assertTrue(urlValidator.isValid("http://www.example.org/.../.."));
    }

    /**
     * Non-regression test for VALIDATOR-375
     */
    @Test
    public void testValidator375() {
        UrlValidator validator = new UrlValidator();
        String url = "http://[FEDC:BA98:7654:3210:FEDC:BA98:7654:3210]:80/index.html";
        assertTrue("IPv6 address URL should validate: " + url, validator.isValid(url));
        url = "http://[::1]:80/index.html";
        assertTrue("IPv6 address URL should validate: " + url, validator.isValid(url));
        url = "http://FEDC:BA98:7654:3210:FEDC:BA98:7654:3210:80/index.html";
        assertFalse("IPv6 address without [] should not validate: " + url, validator.isValid(url));
    }

    /**
     * Non-regression test for VALIDATOR-353
     */
    @Test
    public void testValidator353() { // userinfo
        UrlValidator validator = new UrlValidator();
        assertTrue(validator.isValid("http://www.apache.org:80/path"));
        assertTrue(validator.isValid("http://user:pass@www.apache.org:80/path"));
        assertTrue(validator.isValid("http://user:@www.apache.org:80/path"));
        assertTrue(validator.isValid("http://us%00er:-._~!$&'()*+,;=@www.apache.org:80/path"));
        assertFalse(validator.isValid("http://:pass@www.apache.org:80/path"));
        assertFalse(validator.isValid("http://:@www.apache.org:80/path"));
        assertFalse(validator.isValid("http://user:pa:ss@www.apache.org/path"));
        assertFalse(validator.isValid("http://user:pa@ss@www.apache.org/path"));
    }

    /**
     * Non-regression test for VALIDATOR-382
     */
    @Test
    public void testValidator382() {
        UrlValidator validator = new UrlValidator();
        assertTrue(validator.isValid("ftp://username:password@example.com:8042/over/there/index.dtb?type=animal&name=narwhal#nose"));
    }

    /**
     * Non-regression test for VALIDATOR-380
     */
    @Test
    public void testValidator380() {
        UrlValidator validator = new UrlValidator();
        assertTrue(validator.isValid("http://www.apache.org:80/path"));
        assertTrue(validator.isValid("http://www.apache.org:8/path"));
        assertTrue(validator.isValid("http://www.apache.org:/path"));
    }

    /**
     * Unit test of {@link UrlValidator#getValidatorName}.
     */
    @Test
    public void testValidatorName() {
        assertEquals("URL validator", UrlValidator.getInstance().getValidatorName());
    }

    //-------------------- Test data for creating a composite URL
    /**
     * The data given below approximates the 4 parts of a URL
     * {@code <scheme>://<authority><path>?<query>} except that the port number
     * is broken out of authority to increase the number of permutations.
     * A complete URL is composed of a scheme+authority+port+path+query,
     * all of which must be individually valid for the entire URL to be considered
     * valid.
     */
    ResultPair[] testUrlScheme = {
            new ResultPair("http://", true),
            new ResultPair("ftp://", true),
            new ResultPair("h3t://", true),
            new ResultPair("3ht://", false),
            new ResultPair("http:/", false),
            new ResultPair("http:", false),
            new ResultPair("http/", false),
            new ResultPair("://", false),
            new ResultPair("", true)
    };

    ResultPair[] testUrlAuthority = {
            new ResultPair("www.google.com", true),
            new ResultPair("go.com", true),
            new ResultPair("go.au", true),
            new ResultPair("0.0.0.0", true),
            new ResultPair("255.255.255.255", true),
            new ResultPair("256.256.256.256", false),
            new ResultPair("255.com", true),
            new ResultPair("1.2.3.4.5", false),
            new ResultPair("1.2.3.4.", false),
            new ResultPair("1.2.3", false),
            new ResultPair(".1.2.3.4", false),
            new ResultPair("go.a", false),
            new ResultPair("go.a1a", false),
            new ResultPair("go.cc", true),
            new ResultPair("go.1aa", false),
            new ResultPair("aaa.", false),
            new ResultPair(".aaa", false),
            new ResultPair("aaa", false),
            new ResultPair("", false)
    };

    ResultPair[] testUrlPort = {
            new ResultPair(":80", true),
            new ResultPair(":65535", true),
            new ResultPair(":0", true),
            new ResultPair("", true),
            new ResultPair(":-1", false),
            new ResultPair(":65636", true),
            new ResultPair(":65a", false)
    };

    ResultPair[] testPath = {
            new ResultPair("/test1", true),
            new ResultPair("/t123", true),
            new ResultPair("/$23", true),
            new ResultPair("/..", false),
            new ResultPair("/../", false),
            new ResultPair("/test1/", true),
            new ResultPair("", true),
            new ResultPair("/test1/file", true),
            new ResultPair("/..//file", false),
            new ResultPair("/test1//file", false)
    };

    //Test allow2slash, noFragment
    ResultPair[] testUrlPathOptions = {
            new ResultPair("/test1", true),
            new ResultPair("/t123", true),
            new ResultPair("/$23", true),
            new ResultPair("/..", false),
            new ResultPair("/../", false),
            new ResultPair("/test1/", true),
            new ResultPair("/#", false),
            new ResultPair("", true),
            new ResultPair("/test1/file", true),
            new ResultPair("/t123/file", true),
            new ResultPair("/$23/file", true),
            new ResultPair("/../file", false),
            new ResultPair("/..//file", false),
            new ResultPair("/test1//file", true),
            new ResultPair("/#/file", false)
    };

    ResultPair[] testUrlQuery = {
            new ResultPair("?action=view", true),
            new ResultPair("?action=edit&mode=up", true),
            new ResultPair("", true)
    };

    Object[] testUrlParts = {testUrlScheme, testUrlAuthority, testUrlPort, testPath, testUrlQuery};
    Object[] testUrlPartsOptions = {testUrlScheme, testUrlAuthority, testUrlPort, testUrlPathOptions, testUrlQuery};
    int[] testPartsIndex = {0, 0, 0, 0, 0};

    //---------------- Test data for individual url parts ----------------
    private final String[] schemes = {
            "http",
            "gopher",
            "g0-To+.",
            "not_valid" // TODO this will need to be dropped if the ctor validates schemes
    };

    ResultPair[] testScheme = {
            new ResultPair("http", true),
            new ResultPair("ftp", false),
            new ResultPair("httpd", false),
            new ResultPair("gopher", true),
            new ResultPair("g0-to+.", true),
            new ResultPair("not_valid", false), // underscore not allowed
            new ResultPair("HtTp", true),
            new ResultPair("telnet", false)
    };
}
