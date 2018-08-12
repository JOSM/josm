// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.trajano.commons.testing.UtilityClassTestUtil;

/**
 * Unit tests of {@link RightAndLefthandTraffic} class.
 */
public class RightAndLefthandTrafficTest {
    /**
     * Test rules.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules rules = new JOSMTestRules().projection().rlTraffic();

    /**
     * Tests that {@code RightAndLefthandTraffic} satisfies utility class criterias.
     * @throws ReflectiveOperationException if an error occurs
     */
    @Test
    public void testUtilityClass() throws ReflectiveOperationException {
        UtilityClassTestUtil.assertUtilityClassWellDefined(RightAndLefthandTraffic.class);
    }

    /**
     * Test of {@link RightAndLefthandTraffic#isRightHandTraffic} method.
     */
    @Test
    public void testIsRightHandTraffic() {
        check(true, "Paris", 48.8567, 2.3508);
        check(true, "Berlin", 52.5167, 13.383);
        check(true, "New York", 40.7127, -74.0059);
        check(true, "Papeete", -17.5419, -149.5617);
        check(true, "Guantanamo", 19.912, -75.209);
        check(true, "Guadeloupe", 16.243, -61.533);
        check(true, "Martinique", 14.604, -61.069);
        check(true, "Madagascar", -20.119, 46.316);
        check(true, "Shangai", 31.196, 121.36);
        check(true, "Gibraltar", 36.141244, -5.347369);
        check(true, "British Indian Ocean Territory", -7.3, 72.4);

        check(false, "London", 51.507222, -0.1275);
        check(false, "Valetta (Malta)", 35.897778, 14.5125);
        check(false, "Jersey", 49.19, -2.11);
        check(false, "Isle of Man", 54.25, -4.5);
        check(false, "Dublin (Ireland)", 53.347778, -6.259722);
        check(false, "Guernsey", 49.45, -2.6);
        check(false, "Nicosia (Cyprus)", 35.166667, 33.366667);
        check(false, "Georgetown (Guyana)", 6.8, -58.166667);
        check(false, "Paramaribo (Suriname)", 5.866667, -55.166667);
        check(false, "Anguilla", 18.22723, -63.04899);
        check(false, "Antigua and Barbuda", 17.05, -61.8);
        check(false, "Bahamas", 24.25, -76);
        check(false, "Barbados", 13.166667, -59.55);
        check(false, "British Virgin Islands", 18.5, -64.5);
        check(false, "Cayman Islands", 19.5, -80.5);
        check(false, "Dominica", 15.416667, -61.333333);
        check(false, "Grenada", 12.116667, -61.666667);
        check(false, "Jamaica", 18, -77);
        check(false, "Montserrat", 16.75, -62.2);
        check(false, "Saint Kitts and Nevis", 17.333333, -62.75);
        check(false, "Saint Lucia", 13.883333, -60.966667);
        check(false, "Saint Vincent and the Grenadines", 13.25, -61.2);
        check(false, "Trinidad and Tobago", 11.249285, -60.652557);
        check(false, "Turks and Caicos Islands", 21.75, -71.583333);
        check(false, "United States Virgin Islands", 18.35, -64.933333);
        check(false, "Bermuda", 32.333333, -64.75);
        check(false, "Falkland Islands", -51.683333, -59.166667);
        check(false, "Saint Helena, Ascension and Tristan da Cunha", -15.933, -5.717);
        check(false, "South Georgia and the South Sandwich Islands", -54.25, -36.75);
        check(false, "Maldives", 3.2, 73.22);
        check(false, "Mauritius", -20.2, 57.5);
        check(false, "Seychelles", -4.583333, 55.666667);
        check(false, "Bangladesh", 23.8, 90.3);
        check(false, "Bhutan", 27.417, 90.435);
        check(false, "Brunei", 4.5, 114.666667);
        check(false, "East Timor", -8.833333, 125.916667);
        check(false, "Hong Kong", 22.3, 114.2);
        check(false, "Indonesia", -5, 120);
        check(false, "India", 21, 78);
        check(false, "Japan", 35, 136);
        check(false, "Macau", 22.166667, 113.55);
        check(false, "Malaysia", 2.5, 112.5);
        check(false, "Nepal", 28.166667, 84.25);
        check(false, "Pakistan", 30, 70);
        check(false, "Singapore", 1.3, 103.8);
        check(false, "Sri Lanka", 7, 81);
        check(false, "Thailand", 15.4, 101.3);
        check(false, "Botswana", -24.658333, 25.908333);
        check(false, "Kenya", 1, 38);
        check(false, "Lesotho", -29.6, 28.3);
        check(false, "Malawi", -13.5, 34);
        check(false, "Mauritius", -20.2, 57.5);
        check(false, "Mozambique", -18.25, 35);
        check(false, "Namibia", -22, 17);
        check(false, "South Africa", -30, 25);
        check(false, "Swaziland", -26.5, 31.5);
        check(false, "Tanzania", -6.307, 34.854);
        check(false, "Uganda", 1, 32);
        check(false, "Zambia", -15, 30);
        check(false, "Zimbabwe", -20, 30);
        check(false, "Australia", -27, 133);
        check(false, "Christmas Island", -10.483333, 105.633333);
        check(false, "Cocos (Keeling) Islands", -12.116667, 96.9);
        check(false, "Cook Islands", -21.233333, -159.766667);
        check(false, "Fiji", -18, 179);
        check(false, "Kiribati", 1.416667, 173);
        check(false, "Nauru", -0.533333, 166.933333);
        check(false, "New Zealand", -42, 174);
        check(false, "Niue", -19.05, -169.916667);
        check(false, "Norfolk Island", -29.033333, 167.95);
        check(false, "Papua New Guinea", -6, 147);
        check(false, "Pitcairn Islands", -25.066667, -130.1);
        check(false, "Solomon Islands", -8, 159);
        check(false, "Samoa", -13.583333, -172.333333);
        check(false, "Tokelau", -9.166667, -171.833333);
        check(false, "Tonga", -20, -175);
        check(false, "Tuvalu", -8, 178);
    }

    private static void check(boolean expected, String name, double lat, double lon) {
        boolean actual = RightAndLefthandTraffic.isRightHandTraffic(new LatLon(lat, lon));
        if (actual != expected) {
            fail(name);
        }
    }
}
