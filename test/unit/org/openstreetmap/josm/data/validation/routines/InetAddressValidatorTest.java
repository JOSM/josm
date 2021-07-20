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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test cases for InetAddressValidator.
 *
 * @version $Revision: 1741724 $
 */
class InetAddressValidatorTest {

    private InetAddressValidator validator;

    /**
     * Setup
     */
    @BeforeEach
    public void setUp() {
        validator = new InetAddressValidator();
    }

    /**
     * Test IPs that point to real, well-known hosts (without actually looking them up).
     */
    @Test
    void testInetAddressesFromTheWild() {
        // CHECKSTYLE.OFF: SingleSpaceSeparator
        assertTrue(validator.isValid("140.211.11.130"), "www.apache.org IP should be valid");
        assertTrue(validator.isValid("72.14.253.103"), "www.l.google.com IP should be valid");
        assertTrue(validator.isValid("199.232.41.5"), "fsf.org IP should be valid");
        assertTrue(validator.isValid("216.35.123.87"), "appscs.ign.com IP should be valid");
        // CHECKSTYLE.ON: SingleSpaceSeparator
    }

    /**
     * Non-regression test for VALIDATOR-335
     */
    @Test
    void testVALIDATOR_335() {
        assertTrue(validator.isValid("2001:0438:FFFE:0000:0000:0000:0000:0A35"),
                "2001:0438:FFFE:0000:0000:0000:0000:0A35 should be valid");
    }

    /**
     * Test valid and invalid IPs from each address class.
     */
    @Test
    void testInetAddressesByClass() {
        // CHECKSTYLE.OFF: SingleSpaceSeparator
        assertTrue(validator.isValid("24.25.231.12"), "class A IP should be valid");
        assertFalse(validator.isValid("2.41.32.324"), "illegal class A IP should be invalid");

        assertTrue(validator.isValid("135.14.44.12"), "class B IP should be valid");
        assertFalse(validator.isValid("154.123.441.123"), "illegal class B IP should be invalid");

        assertTrue(validator.isValid("213.25.224.32"), "class C IP should be valid");
        assertFalse(validator.isValid("201.543.23.11"), "illegal class C IP should be invalid");

        assertTrue(validator.isValid("229.35.159.6"), "class D IP should be valid");
        assertFalse(validator.isValid("231.54.11.987"), "illegal class D IP should be invalid");

        assertTrue(validator.isValid("248.85.24.92"), "class E IP should be valid");
        assertFalse(validator.isValid("250.21.323.48"), "illegal class E IP should be invalid");
        // CHECKSTYLE.ON: SingleSpaceSeparator
    }

    /**
     * Test reserved IPs.
     */
    @Test
    void testReservedInetAddresses() {
        assertTrue(validator.isValid("127.0.0.1"), "localhost IP should be valid");
        assertTrue(validator.isValid("255.255.255.255"), "broadcast IP should be valid");
    }

    /**
     * Test obviously broken IPs.
     */
    @Test
    void testBrokenInetAddresses() {
        // CHECKSTYLE.OFF: SingleSpaceSeparator
        assertFalse(validator.isValid("124.14.32.abc"), "IP with characters should be invalid");
        assertFalse(validator.isValid("124.14.32.01"), "IP with leading zeroes should be invalid");
        assertFalse(validator.isValid("23.64.12"), "IP with three groups should be invalid");
        assertFalse(validator.isValid("26.34.23.77.234"), "IP with five groups should be invalid");
        // CHECKSTYLE.ON: SingleSpaceSeparator
    }

    // CHECKSTYLE.OFF: LineLength
    // CHECKSTYLE.OFF: MethodLengthCheck

    static Stream<Arguments> testIPv6() {
        return Stream.of(
                Arguments.of("", false), // empty string
        Arguments.of("::1", true), // loopback, compressed, non-routable
        Arguments.of("::", true), // unspecified, compressed, non-routable
        Arguments.of("0:0:0:0:0:0:0:1", true), // loopback, full
        Arguments.of("0:0:0:0:0:0:0:0", true), // unspecified, full
        Arguments.of("2001:DB8:0:0:8:800:200C:417A", true), // unicast, full
        Arguments.of("FF01:0:0:0:0:0:0:101", true), // multicast, full
        Arguments.of("2001:DB8::8:800:200C:417A", true), // unicast, compressed
        Arguments.of("FF01::101", true), // multicast, compressed
        Arguments.of("2001:DB8:0:0:8:800:200C:417A:221", false), // unicast, full
        Arguments.of("FF01::101::2", false), // multicast, compressed
        Arguments.of("fe80::217:f2ff:fe07:ed62", true),
        Arguments.of("2001:0000:1234:0000:0000:C1C0:ABCD:0876", true),
        Arguments.of("3ffe:0b00:0000:0000:0001:0000:0000:000a", true),
        Arguments.of("FF02:0000:0000:0000:0000:0000:0000:0001", true),
        Arguments.of("0000:0000:0000:0000:0000:0000:0000:0001", true),
        Arguments.of("0000:0000:0000:0000:0000:0000:0000:0000", true),
        Arguments.of("02001:0000:1234:0000:0000:C1C0:ABCD:0876", false), // extra 0 not allowed!
        Arguments.of("2001:0000:1234:0000:00001:C1C0:ABCD:0876", false), // extra 0 not allowed!
        Arguments.of("2001:0000:1234:0000:0000:C1C0:ABCD:0876 0", false), // junk after valid address
        Arguments.of("2001:0000:1234: 0000:0000:C1C0:ABCD:0876", false), // internal space
        Arguments.of("3ffe:0b00:0000:0001:0000:0000:000a", false), // seven segments
        Arguments.of("FF02:0000:0000:0000:0000:0000:0000:0000:0001", false), // nine segments
        Arguments.of("3ffe:b00::1::a", false), // double "::"
        Arguments.of("::1111:2222:3333:4444:5555:6666::", false), // double "::"
        Arguments.of("2::10", true),
        Arguments.of("ff02::1", true),
        Arguments.of("fe80::", true),
        Arguments.of("2002::", true),
        Arguments.of("2001:db8::", true),
        Arguments.of("2001:0db8:1234::", true),
        Arguments.of("::ffff:0:0", true),
        Arguments.of("1:2:3:4:5:6:7:8", true),
        Arguments.of("1:2:3:4:5:6::8", true),
        Arguments.of("1:2:3:4:5::8", true),
        Arguments.of("1:2:3:4::8", true),
        Arguments.of("1:2:3::8", true),
        Arguments.of("1:2::8", true),
        Arguments.of("1::8", true),
        Arguments.of("1::2:3:4:5:6:7", true),
        Arguments.of("1::2:3:4:5:6", true),
        Arguments.of("1::2:3:4:5", true),
        Arguments.of("1::2:3:4", true),
        Arguments.of("1::2:3", true),
        Arguments.of("::2:3:4:5:6:7:8", true),
        Arguments.of("::2:3:4:5:6:7", true),
        Arguments.of("::2:3:4:5:6", true),
        Arguments.of("::2:3:4:5", true),
        Arguments.of("::2:3:4", true),
        Arguments.of("::2:3", true),
        Arguments.of("::8", true),
        Arguments.of("1:2:3:4:5:6::", true),
        Arguments.of("1:2:3:4:5::", true),
        Arguments.of("1:2:3:4::", true),
        Arguments.of("1:2:3::", true),
        Arguments.of("1:2::", true),
        Arguments.of("1::", true),
        Arguments.of("1:2:3:4:5::7:8", true),
        Arguments.of("1:2:3::4:5::7:8", false), // Double "::"
        Arguments.of("12345::6:7:8", false),
        Arguments.of("1:2:3:4::7:8", true),
        Arguments.of("1:2:3::7:8", true),
        Arguments.of("1:2::7:8", true),
        Arguments.of("1::7:8", true),
        // IPv4 addresses as dotted-quads
        Arguments.of("1:2:3:4:5:6:1.2.3.4", true),
        Arguments.of("1:2:3:4:5::1.2.3.4", true),
        Arguments.of("1:2:3:4::1.2.3.4", true),
        Arguments.of("1:2:3::1.2.3.4", true),
        Arguments.of("1:2::1.2.3.4", true),
        Arguments.of("1::1.2.3.4", true),
        Arguments.of("1:2:3:4::5:1.2.3.4", true),
        Arguments.of("1:2:3::5:1.2.3.4", true),
        Arguments.of("1:2::5:1.2.3.4", true),
        Arguments.of("1::5:1.2.3.4", true),
        Arguments.of("1::5:11.22.33.44", true),
        Arguments.of("1::5:400.2.3.4", false),
        Arguments.of("1::5:260.2.3.4", false),
        Arguments.of("1::5:256.2.3.4", false),
        Arguments.of("1::5:1.256.3.4", false),
        Arguments.of("1::5:1.2.256.4", false),
        Arguments.of("1::5:1.2.3.256", false),
        Arguments.of("1::5:300.2.3.4", false),
        Arguments.of("1::5:1.300.3.4", false),
        Arguments.of("1::5:1.2.300.4", false),
        Arguments.of("1::5:1.2.3.300", false),
        Arguments.of("1::5:900.2.3.4", false),
        Arguments.of("1::5:1.900.3.4", false),
        Arguments.of("1::5:1.2.900.4", false),
        Arguments.of("1::5:1.2.3.900", false),
        Arguments.of("1::5:300.300.300.300", false),
        Arguments.of("1::5:3000.30.30.30", false),
        Arguments.of("1::400.2.3.4", false),
        Arguments.of("1::260.2.3.4", false),
        Arguments.of("1::256.2.3.4", false),
        Arguments.of("1::1.256.3.4", false),
        Arguments.of("1::1.2.256.4", false),
        Arguments.of("1::1.2.3.256", false),
        Arguments.of("1::300.2.3.4", false),
        Arguments.of("1::1.300.3.4", false),
        Arguments.of("1::1.2.300.4", false),
        Arguments.of("1::1.2.3.300", false),
        Arguments.of("1::900.2.3.4", false),
        Arguments.of("1::1.900.3.4", false),
        Arguments.of("1::1.2.900.4", false),
        Arguments.of("1::1.2.3.900", false),
        Arguments.of("1::300.300.300.300", false),
        Arguments.of("1::3000.30.30.30", false),
        Arguments.of("::400.2.3.4", false),
        Arguments.of("::260.2.3.4", false),
        Arguments.of("::256.2.3.4", false),
        Arguments.of("::1.256.3.4", false),
        Arguments.of("::1.2.256.4", false),
        Arguments.of("::1.2.3.256", false),
        Arguments.of("::300.2.3.4", false),
        Arguments.of("::1.300.3.4", false),
        Arguments.of("::1.2.300.4", false),
        Arguments.of("::1.2.3.300", false),
        Arguments.of("::900.2.3.4", false),
        Arguments.of("::1.900.3.4", false),
        Arguments.of("::1.2.900.4", false),
        Arguments.of("::1.2.3.900", false),
        Arguments.of("::300.300.300.300", false),
        Arguments.of("::3000.30.30.30", false),
        Arguments.of("fe80::217:f2ff:254.7.237.98", true),
        Arguments.of("::ffff:192.168.1.26", true),
        Arguments.of("2001:1:1:1:1:1:255Z255X255Y255", false), // garbage instead of "." in IPv4
        Arguments.of("::ffff:192x168.1.26", false), // ditto
        Arguments.of("::ffff:192.168.1.1", true),
        Arguments.of("0:0:0:0:0:0:13.1.68.3", true), // IPv4-compatible IPv6 address, full, deprecated
        Arguments.of("0:0:0:0:0:FFFF:129.144.52.38", true), // IPv4-mapped IPv6 address, full
        Arguments.of("::13.1.68.3", true), // IPv4-compatible IPv6 address, compressed, deprecated
        Arguments.of("::FFFF:129.144.52.38", true), // IPv4-mapped IPv6 address, compressed
        Arguments.of("fe80:0:0:0:204:61ff:254.157.241.86", true),
        Arguments.of("fe80::204:61ff:254.157.241.86", true),
        Arguments.of("::ffff:12.34.56.78", true),
        Arguments.of("::ffff:2.3.4", false),
        Arguments.of("::ffff:257.1.2.3", false),
        Arguments.of("1.2.3.4", false),
        Arguments.of("1.2.3.4:1111:2222:3333:4444::5555", false),
        Arguments.of("1.2.3.4:1111:2222:3333::5555", false),
        Arguments.of("1.2.3.4:1111:2222::5555", false),
        Arguments.of("1.2.3.4:1111::5555", false),
        Arguments.of("1.2.3.4::5555", false),
        Arguments.of("1.2.3.4::", false),
        // Testing IPv4 addresses represented as dotted-quads
        // Leading zeroes in IPv4 addresses not allowed: some systems treat the leading "0" in ".086" as the start of an octal number
        // Update: The BNF in RFC-3986 explicitly defines the dec-octet (for IPv4 addresses) not to have a leading zero
        Arguments.of("fe80:0000:0000:0000:0204:61ff:254.157.241.086", false),
        Arguments.of("::ffff:192.0.2.128", true), // but this is OK, since there's a single digit
        Arguments.of("XXXX:XXXX:XXXX:XXXX:XXXX:XXXX:1.2.3.4", false),
        Arguments.of("1111:2222:3333:4444:5555:6666:00.00.00.00", false),
        Arguments.of("1111:2222:3333:4444:5555:6666:000.000.000.000", false),
        Arguments.of("1111:2222:3333:4444:5555:6666:256.256.256.256", false),
        Arguments.of("fe80:0000:0000:0000:0204:61ff:fe9d:f156", true),
        Arguments.of("fe80:0:0:0:204:61ff:fe9d:f156", true),
        Arguments.of("fe80::204:61ff:fe9d:f156", true),
        Arguments.of(":", false),
        Arguments.of("::ffff:c000:280", true),
        Arguments.of("1111:2222:3333:4444::5555:", false),
        Arguments.of("1111:2222:3333::5555:", false),
        Arguments.of("1111:2222::5555:", false),
        Arguments.of("1111::5555:", false),
        Arguments.of("::5555:", false),
        Arguments.of(":::", false),
        Arguments.of("1111:", false),
        Arguments.of(":1111:2222:3333:4444::5555", false),
        Arguments.of(":1111:2222:3333::5555", false),
        Arguments.of(":1111:2222::5555", false),
        Arguments.of(":1111::5555", false),
        Arguments.of(":::5555", false),
        Arguments.of("2001:0db8:85a3:0000:0000:8a2e:0370:7334", true),
        Arguments.of("2001:db8:85a3:0:0:8a2e:370:7334", true),
        Arguments.of("2001:db8:85a3::8a2e:370:7334", true),
        Arguments.of("2001:0db8:0000:0000:0000:0000:1428:57ab", true),
        Arguments.of("2001:0db8:0000:0000:0000::1428:57ab", true),
        Arguments.of("2001:0db8:0:0:0:0:1428:57ab", true),
        Arguments.of("2001:0db8:0:0::1428:57ab", true),
        Arguments.of("2001:0db8::1428:57ab", true),
        Arguments.of("2001:db8::1428:57ab", true),
        Arguments.of("::ffff:0c22:384e", true),
        Arguments.of("2001:0db8:1234:0000:0000:0000:0000:0000", true),
        Arguments.of("2001:0db8:1234:ffff:ffff:ffff:ffff:ffff", true),
        Arguments.of("2001:db8:a::123", true),
        Arguments.of("123", false),
        Arguments.of("ldkfj", false),
        Arguments.of("2001::FFD3::57ab", false),
        Arguments.of("2001:db8:85a3::8a2e:37023:7334", false),
        Arguments.of("2001:db8:85a3::8a2e:370k:7334", false),
        Arguments.of("1:2:3:4:5:6:7:8:9", false),
        Arguments.of("1::2::3", false),
        Arguments.of("1:::3:4:5", false),
        Arguments.of("1:2:3::4:5:6:7:8:9", false),
        Arguments.of("1111:2222:3333:4444:5555:6666:7777:8888", true),
        Arguments.of("1111:2222:3333:4444:5555:6666:7777::", true),
        Arguments.of("1111:2222:3333:4444:5555:6666::", true),
        Arguments.of("1111:2222:3333:4444:5555::", true),
        Arguments.of("1111:2222:3333:4444::", true),
        Arguments.of("1111:2222:3333::", true),
        Arguments.of("1111:2222::", true),
        Arguments.of("1111::", true),
        Arguments.of("1111:2222:3333:4444:5555:6666::8888", true),
        Arguments.of("1111:2222:3333:4444:5555::8888", true),
        Arguments.of("1111:2222:3333:4444::8888", true),
        Arguments.of("1111:2222:3333::8888", true),
        Arguments.of("1111:2222::8888", true),
        Arguments.of("1111::8888", true),
        Arguments.of("::8888", true),
        Arguments.of("1111:2222:3333:4444:5555::7777:8888", true),
        Arguments.of("1111:2222:3333:4444::7777:8888", true),
        Arguments.of("1111:2222:3333::7777:8888", true),
        Arguments.of("1111:2222::7777:8888", true),
        Arguments.of("1111::7777:8888", true),
        Arguments.of("::7777:8888", true),
        Arguments.of("1111:2222:3333:4444::6666:7777:8888", true),
        Arguments.of("1111:2222:3333::6666:7777:8888", true),
        Arguments.of("1111:2222::6666:7777:8888", true),
        Arguments.of("1111::6666:7777:8888", true),
        Arguments.of("::6666:7777:8888", true),
        Arguments.of("1111:2222:3333::5555:6666:7777:8888", true),
        Arguments.of("1111:2222::5555:6666:7777:8888", true),
        Arguments.of("1111::5555:6666:7777:8888", true),
        Arguments.of("::5555:6666:7777:8888", true),
        Arguments.of("1111:2222::4444:5555:6666:7777:8888", true),
        Arguments.of("1111::4444:5555:6666:7777:8888", true),
        Arguments.of("::4444:5555:6666:7777:8888", true),
        Arguments.of("1111::3333:4444:5555:6666:7777:8888", true),
        Arguments.of("::3333:4444:5555:6666:7777:8888", true),
        Arguments.of("::2222:3333:4444:5555:6666:7777:8888", true),
        Arguments.of("1111:2222:3333:4444:5555:6666:123.123.123.123", true),
        Arguments.of("1111:2222:3333:4444:5555::123.123.123.123", true),
        Arguments.of("1111:2222:3333:4444::123.123.123.123", true),
        Arguments.of("1111:2222:3333::123.123.123.123", true),
        Arguments.of("1111:2222::123.123.123.123", true),
        Arguments.of("1111::123.123.123.123", true),
        Arguments.of("::123.123.123.123", true),
        Arguments.of("1111:2222:3333:4444::6666:123.123.123.123", true),
        Arguments.of("1111:2222:3333::6666:123.123.123.123", true),
        Arguments.of("1111:2222::6666:123.123.123.123", true),
        Arguments.of("1111::6666:123.123.123.123", true),
        Arguments.of("::6666:123.123.123.123", true),
        Arguments.of("1111:2222:3333::5555:6666:123.123.123.123", true),
        Arguments.of("1111:2222::5555:6666:123.123.123.123", true),
        Arguments.of("1111::5555:6666:123.123.123.123", true),
        Arguments.of("::5555:6666:123.123.123.123", true),
        Arguments.of("1111:2222::4444:5555:6666:123.123.123.123", true),
        Arguments.of("1111::4444:5555:6666:123.123.123.123", true),
        Arguments.of("::4444:5555:6666:123.123.123.123", true),
        Arguments.of("1111::3333:4444:5555:6666:123.123.123.123", true),
        Arguments.of("::2222:3333:4444:5555:6666:123.123.123.123", true),
        // Trying combinations of "0" and "::"
        // These are all syntactically correct, but are bad form
        // because "0" adjacent to "::" should be combined into "::"
        Arguments.of("::0:0:0:0:0:0:0", true),
        Arguments.of("::0:0:0:0:0:0", true),
        Arguments.of("::0:0:0:0:0", true),
        Arguments.of("::0:0:0:0", true),
        Arguments.of("::0:0:0", true),
        Arguments.of("::0:0", true),
        Arguments.of("::0", true),
        Arguments.of("0:0:0:0:0:0:0::", true),
        Arguments.of("0:0:0:0:0:0::", true),
        Arguments.of("0:0:0:0:0::", true),
        Arguments.of("0:0:0:0::", true),
        Arguments.of("0:0:0::", true),
        Arguments.of("0:0::", true),
        Arguments.of("0::", true),
        // Invalid data
        Arguments.of("XXXX:XXXX:XXXX:XXXX:XXXX:XXXX:XXXX:XXXX", false),
        // Too many components
        Arguments.of("1111:2222:3333:4444:5555:6666:7777:8888:9999", false),
        Arguments.of("1111:2222:3333:4444:5555:6666:7777:8888::", false),
        Arguments.of("::2222:3333:4444:5555:6666:7777:8888:9999", false),
        // Too few components
        Arguments.of("1111:2222:3333:4444:5555:6666:7777", false),
        Arguments.of("1111:2222:3333:4444:5555:6666", false),
        Arguments.of("1111:2222:3333:4444:5555", false),
        Arguments.of("1111:2222:3333:4444", false),
        Arguments.of("1111:2222:3333", false),
        Arguments.of("1111:2222", false),
        Arguments.of("1111", false),
        // Missing :
        Arguments.of("11112222:3333:4444:5555:6666:7777:8888", false),
        Arguments.of("1111:22223333:4444:5555:6666:7777:8888", false),
        Arguments.of("1111:2222:33334444:5555:6666:7777:8888", false),
        Arguments.of("1111:2222:3333:44445555:6666:7777:8888", false),
        Arguments.of("1111:2222:3333:4444:55556666:7777:8888", false),
        Arguments.of("1111:2222:3333:4444:5555:66667777:8888", false),
        Arguments.of("1111:2222:3333:4444:5555:6666:77778888", false),
        // Missing : intended for ::
        Arguments.of("1111:2222:3333:4444:5555:6666:7777:8888:", false),
        Arguments.of("1111:2222:3333:4444:5555:6666:7777:", false),
        Arguments.of("1111:2222:3333:4444:5555:6666:", false),
        Arguments.of("1111:2222:3333:4444:5555:", false),
        Arguments.of("1111:2222:3333:4444:", false),
        Arguments.of("1111:2222:3333:", false),
        Arguments.of("1111:2222:", false),
        Arguments.of(":8888", false),
        Arguments.of(":7777:8888", false),
        Arguments.of(":6666:7777:8888", false),
        Arguments.of(":5555:6666:7777:8888", false),
        Arguments.of(":4444:5555:6666:7777:8888", false),
        Arguments.of(":3333:4444:5555:6666:7777:8888", false),
        Arguments.of(":2222:3333:4444:5555:6666:7777:8888", false),
        Arguments.of(":1111:2222:3333:4444:5555:6666:7777:8888", false),
        // :::
        Arguments.of(":::2222:3333:4444:5555:6666:7777:8888", false),
        Arguments.of("1111:::3333:4444:5555:6666:7777:8888", false),
        Arguments.of("1111:2222:::4444:5555:6666:7777:8888", false),
        Arguments.of("1111:2222:3333:::5555:6666:7777:8888", false),
        Arguments.of("1111:2222:3333:4444:::6666:7777:8888", false),
        Arguments.of("1111:2222:3333:4444:5555:::7777:8888", false),
        Arguments.of("1111:2222:3333:4444:5555:6666:::8888", false),
        Arguments.of("1111:2222:3333:4444:5555:6666:7777:::", false),
        // Double ::
        Arguments.of("::2222::4444:5555:6666:7777:8888", false),
        Arguments.of("::2222:3333::5555:6666:7777:8888", false),
        Arguments.of("::2222:3333:4444::6666:7777:8888", false),
        Arguments.of("::2222:3333:4444:5555::7777:8888", false),
        Arguments.of("::2222:3333:4444:5555:7777::8888", false),
        Arguments.of("::2222:3333:4444:5555:7777:8888::", false),
        Arguments.of("1111::3333::5555:6666:7777:8888", false),
        Arguments.of("1111::3333:4444::6666:7777:8888", false),
        Arguments.of("1111::3333:4444:5555::7777:8888", false),
        Arguments.of("1111::3333:4444:5555:6666::8888", false),
        Arguments.of("1111::3333:4444:5555:6666:7777::", false),
        Arguments.of("1111:2222::4444::6666:7777:8888", false),
        Arguments.of("1111:2222::4444:5555::7777:8888", false),
        Arguments.of("1111:2222::4444:5555:6666::8888", false),
        Arguments.of("1111:2222::4444:5555:6666:7777::", false),
        Arguments.of("1111:2222:3333::5555::7777:8888", false),
        Arguments.of("1111:2222:3333::5555:6666::8888", false),
        Arguments.of("1111:2222:3333::5555:6666:7777::", false),
        Arguments.of("1111:2222:3333:4444::6666::8888", false),
        Arguments.of("1111:2222:3333:4444::6666:7777::", false),
        Arguments.of("1111:2222:3333:4444:5555::7777::", false),
        // Too many components"
        Arguments.of("1111:2222:3333:4444:5555:6666:7777:8888:1.2.3.4", false),
        Arguments.of("1111:2222:3333:4444:5555:6666:7777:1.2.3.4", false),
        Arguments.of("1111:2222:3333:4444:5555:6666::1.2.3.4", false),
        Arguments.of("::2222:3333:4444:5555:6666:7777:1.2.3.4", false),
        Arguments.of("1111:2222:3333:4444:5555:6666:1.2.3.4.5", false),
        // Too few components
        Arguments.of("1111:2222:3333:4444:5555:1.2.3.4", false),
        Arguments.of("1111:2222:3333:4444:1.2.3.4", false),
        Arguments.of("1111:2222:3333:1.2.3.4", false),
        Arguments.of("1111:2222:1.2.3.4", false),
        Arguments.of("1111:1.2.3.4", false),
        Arguments.of("1.2.3.4", false),
        // Missing :
        Arguments.of("11112222:3333:4444:5555:6666:1.2.3.4", false),
        Arguments.of("1111:22223333:4444:5555:6666:1.2.3.4", false),
        Arguments.of("1111:2222:33334444:5555:6666:1.2.3.4", false),
        Arguments.of("1111:2222:3333:44445555:6666:1.2.3.4", false),
        Arguments.of("1111:2222:3333:4444:55556666:1.2.3.4", false),
        Arguments.of("1111:2222:3333:4444:5555:66661.2.3.4", false),
        // Missing .
        Arguments.of("1111:2222:3333:4444:5555:6666:255255.255.255", false),
        Arguments.of("1111:2222:3333:4444:5555:6666:255.255255.255", false),
        Arguments.of("1111:2222:3333:4444:5555:6666:255.255.255255", false),
        // Missing : intended for ::
        Arguments.of(":1.2.3.4", false),
        Arguments.of(":6666:1.2.3.4", false),
        Arguments.of(":5555:6666:1.2.3.4", false),
        Arguments.of(":4444:5555:6666:1.2.3.4", false),
        Arguments.of(":3333:4444:5555:6666:1.2.3.4", false),
        Arguments.of(":2222:3333:4444:5555:6666:1.2.3.4", false),
        Arguments.of(":1111:2222:3333:4444:5555:6666:1.2.3.4", false),
        // :::
        Arguments.of(":::2222:3333:4444:5555:6666:1.2.3.4", false),
        Arguments.of("1111:::3333:4444:5555:6666:1.2.3.4", false),
        Arguments.of("1111:2222:::4444:5555:6666:1.2.3.4", false),
        Arguments.of("1111:2222:3333:::5555:6666:1.2.3.4", false),
        Arguments.of("1111:2222:3333:4444:::6666:1.2.3.4", false),
        Arguments.of("1111:2222:3333:4444:5555:::1.2.3.4", false),
        // Double ::
        Arguments.of("::2222::4444:5555:6666:1.2.3.4", false),
        Arguments.of("::2222:3333::5555:6666:1.2.3.4", false),
        Arguments.of("::2222:3333:4444::6666:1.2.3.4", false),
        Arguments.of("::2222:3333:4444:5555::1.2.3.4", false),
        Arguments.of("1111::3333::5555:6666:1.2.3.4", false),
        Arguments.of("1111::3333:4444::6666:1.2.3.4", false),
        Arguments.of("1111::3333:4444:5555::1.2.3.4", false),
        Arguments.of("1111:2222::4444::6666:1.2.3.4", false),
        Arguments.of("1111:2222::4444:5555::1.2.3.4", false),
        Arguments.of("1111:2222:3333::5555::1.2.3.4", false),
        // Missing parts
        Arguments.of("::.", false),
        Arguments.of("::..", false),
        Arguments.of("::...", false),
        Arguments.of("::1...", false),
        Arguments.of("::1.2..", false),
        Arguments.of("::1.2.3.", false),
        Arguments.of("::.2..", false),
        Arguments.of("::.2.3.", false),
        Arguments.of("::.2.3.4", false),
        Arguments.of("::..3.", false),
        Arguments.of("::..3.4", false),
        Arguments.of("::...4", false),
        // Extra : in front
        Arguments.of(":1111:2222:3333:4444:5555:6666:7777::", false),
        Arguments.of(":1111:2222:3333:4444:5555:6666::", false),
        Arguments.of(":1111:2222:3333:4444:5555::", false),
        Arguments.of(":1111:2222:3333:4444::", false),
        Arguments.of(":1111:2222:3333::", false),
        Arguments.of(":1111:2222::", false),
        Arguments.of(":1111::", false),
        Arguments.of(":1111:2222:3333:4444:5555:6666::8888", false),
        Arguments.of(":1111:2222:3333:4444:5555::8888", false),
        Arguments.of(":1111:2222:3333:4444::8888", false),
        Arguments.of(":1111:2222:3333::8888", false),
        Arguments.of(":1111:2222::8888", false),
        Arguments.of(":1111::8888", false),
        Arguments.of(":::8888", false),
        Arguments.of(":1111:2222:3333:4444:5555::7777:8888", false),
        Arguments.of(":1111:2222:3333:4444::7777:8888", false),
        Arguments.of(":1111:2222:3333::7777:8888", false),
        Arguments.of(":1111:2222::7777:8888", false),
        Arguments.of(":1111::7777:8888", false),
        Arguments.of(":::7777:8888", false),
        Arguments.of(":1111:2222:3333:4444::6666:7777:8888", false),
        Arguments.of(":1111:2222:3333::6666:7777:8888", false),
        Arguments.of(":1111:2222::6666:7777:8888", false),
        Arguments.of(":1111::6666:7777:8888", false),
        Arguments.of(":::6666:7777:8888", false),
        Arguments.of(":1111:2222:3333::5555:6666:7777:8888", false),
        Arguments.of(":1111:2222::5555:6666:7777:8888", false),
        Arguments.of(":1111::5555:6666:7777:8888", false),
        Arguments.of(":::5555:6666:7777:8888", false),
        Arguments.of(":1111:2222::4444:5555:6666:7777:8888", false),
        Arguments.of(":1111::4444:5555:6666:7777:8888", false),
        Arguments.of(":::4444:5555:6666:7777:8888", false),
        Arguments.of(":1111::3333:4444:5555:6666:7777:8888", false),
        Arguments.of(":::3333:4444:5555:6666:7777:8888", false),
        Arguments.of(":::2222:3333:4444:5555:6666:7777:8888", false),
        Arguments.of(":1111:2222:3333:4444:5555:6666:1.2.3.4", false),
        Arguments.of(":1111:2222:3333:4444:5555::1.2.3.4", false),
        Arguments.of(":1111:2222:3333:4444::1.2.3.4", false),
        Arguments.of(":1111:2222:3333::1.2.3.4", false),
        Arguments.of(":1111:2222::1.2.3.4", false),
        Arguments.of(":1111::1.2.3.4", false),
        Arguments.of(":::1.2.3.4", false),
        Arguments.of(":1111:2222:3333:4444::6666:1.2.3.4", false),
        Arguments.of(":1111:2222:3333::6666:1.2.3.4", false),
        Arguments.of(":1111:2222::6666:1.2.3.4", false),
        Arguments.of(":1111::6666:1.2.3.4", false),
        Arguments.of(":::6666:1.2.3.4", false),
        Arguments.of(":1111:2222:3333::5555:6666:1.2.3.4", false),
        Arguments.of(":1111:2222::5555:6666:1.2.3.4", false),
        Arguments.of(":1111::5555:6666:1.2.3.4", false),
        Arguments.of(":::5555:6666:1.2.3.4", false),
        Arguments.of(":1111:2222::4444:5555:6666:1.2.3.4", false),
        Arguments.of(":1111::4444:5555:6666:1.2.3.4", false),
        Arguments.of(":::4444:5555:6666:1.2.3.4", false),
        Arguments.of(":1111::3333:4444:5555:6666:1.2.3.4", false),
        Arguments.of(":::2222:3333:4444:5555:6666:1.2.3.4", false),
        // Extra : at end
        Arguments.of("1111:2222:3333:4444:5555:6666:7777:::", false),
        Arguments.of("1111:2222:3333:4444:5555:6666:::", false),
        Arguments.of("1111:2222:3333:4444:5555:::", false),
        Arguments.of("1111:2222:3333:4444:::", false),
        Arguments.of("1111:2222:3333:::", false),
        Arguments.of("1111:2222:::", false),
        Arguments.of("1111:::", false),
        Arguments.of("1111:2222:3333:4444:5555:6666::8888:", false),
        Arguments.of("1111:2222:3333:4444:5555::8888:", false),
        Arguments.of("1111:2222:3333:4444::8888:", false),
        Arguments.of("1111:2222:3333::8888:", false),
        Arguments.of("1111:2222::8888:", false),
        Arguments.of("1111::8888:", false),
        Arguments.of("::8888:", false),
        Arguments.of("1111:2222:3333:4444:5555::7777:8888:", false),
        Arguments.of("1111:2222:3333:4444::7777:8888:", false),
        Arguments.of("1111:2222:3333::7777:8888:", false),
        Arguments.of("1111:2222::7777:8888:", false),
        Arguments.of("1111::7777:8888:", false),
        Arguments.of("::7777:8888:", false),
        Arguments.of("1111:2222:3333:4444::6666:7777:8888:", false),
        Arguments.of("1111:2222:3333::6666:7777:8888:", false),
        Arguments.of("1111:2222::6666:7777:8888:", false),
        Arguments.of("1111::6666:7777:8888:", false),
        Arguments.of("::6666:7777:8888:", false),
        Arguments.of("1111:2222:3333::5555:6666:7777:8888:", false),
        Arguments.of("1111:2222::5555:6666:7777:8888:", false),
        Arguments.of("1111::5555:6666:7777:8888:", false),
        Arguments.of("::5555:6666:7777:8888:", false),
        Arguments.of("1111:2222::4444:5555:6666:7777:8888:", false),
        Arguments.of("1111::4444:5555:6666:7777:8888:", false),
        Arguments.of("::4444:5555:6666:7777:8888:", false),
        Arguments.of("1111::3333:4444:5555:6666:7777:8888:", false),
        Arguments.of("::3333:4444:5555:6666:7777:8888:", false),
        Arguments.of("::2222:3333:4444:5555:6666:7777:8888:", false),
        Arguments.of("0:a:b:c:d:e:f::", true),
        Arguments.of("::0:a:b:c:d:e:f", true), // syntactically correct, but bad form (::0:... could be combined)
        Arguments.of("a:b:c:d:e:f:0::", true),
        Arguments.of("':10.0.0.1", false)
        );
    }
    // CHECKSTYLE.ON: MethodLengthCheck

    /**
     * Test IPv6 addresses.
     * <p>These tests were ported from a
     * <a href="http://download.dartware.com/thirdparty/test-ipv6-regex.pl">Perl script</a>.</p>
     */
    @ParameterizedTest
    @MethodSource
    void testIPv6(final String IPv6Address, final boolean isValid) {
        assertEquals(isValid, this.validator.isValidInet6Address(IPv6Address), "IPV6 " + IPv6Address + " should be " + (isValid ? "valid" : "invalid"));
    }

    // CHECKSTYLE.ON: LineLength

    /**
     * Unit test of {@link InetAddressValidator#getValidatorName}.
     */
    @Test
    void testValidatorName() {
        assertNull(new InetAddressValidator().getValidatorName());
    }
}
