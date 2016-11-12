// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Unit tests of {@link RightAndLefthandTraffic} class.
 */
public class RightAndLefthandTrafficTest {
    /**
     * Test rules.
     */
    @Rule
    public JOSMTestRules rules = new JOSMTestRules().platform().projection().commands();

    /**
     * Test of {@link RightAndLefthandTraffic#isRightHandTraffic} method.
     */
    @Test
    public void testIsRightHandTraffic() {
        assertTrue("Paris", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(48.8567, 2.3508)));
        assertTrue("Berlin", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(52.5167, 13.383)));
        assertTrue("New York", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(40.7127, -74.0059)));
        assertTrue("Papeete", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-17.5419, -149.5617)));
        assertTrue("Guantanamo", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(19.912, -75.209)));
        assertTrue("Guadeloupe", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(16.243, -61.533)));
        assertTrue("Martinique", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(14.604, -61.069)));
        assertTrue("Madagascar", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-20.119, 46.316)));
        assertTrue("Shangai", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(31.196, 121.36)));

        assertFalse("London", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(51.507222, -0.1275)));
        assertFalse("Valetta (Malta)", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(35.897778, 14.5125)));
        assertFalse("Jersey", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(49.19, -2.11)));
        assertFalse("Isle of Man", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(54.25, -4.5)));
        assertFalse("Dublin (Ireland)", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(53.347778, -6.259722)));
        assertFalse("Guernsey", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(49.45, -2.6)));
        assertFalse("Nicosia (Cyprus)", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(35.166667, 33.366667)));
        assertFalse("Georgetown (Guyana)", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(6.8, -58.166667)));
        assertFalse("Paramaribo (Suriname)", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(5.866667, -55.166667)));
        assertFalse("Anguilla", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(18.22723, -63.04899)));
        assertFalse("Antigua and Barbuda", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(17.05, -61.8)));
        assertFalse("Bahamas", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(24.25, -76)));
        assertFalse("Barbados", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(13.166667, -59.55)));
        assertFalse("British Virgin Islands", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(18.5, -64.5)));
        assertFalse("Cayman Islands", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(19.5, -80.5)));
        assertFalse("Dominica", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(15.416667, -61.333333)));
        assertFalse("Grenada", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(12.116667, -61.666667)));
        assertFalse("Jamaica", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(18, -77)));
        assertFalse("Montserrat", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(16.75, -62.2)));
        assertFalse("Saint Kitts and Nevis", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(17.333333, -62.75)));
        assertFalse("Saint Lucia", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(13.883333, -60.966667)));
        assertFalse("Saint Vincent and the Grenadines", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(13.25, -61.2)));
        assertFalse("Trinidad and Tobago", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(11.249285, -60.652557)));
        assertFalse("Turks and Caicos Islands", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(21.75, -71.583333)));
        assertFalse("United States Virgin Islands", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(18.35, -64.933333)));
        assertFalse("Bermuda", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(32.333333, -64.75)));
        assertFalse("Falkland Islands", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-51.683333, -59.166667)));
        assertFalse("Saint Helena, Ascension and Tristan da Cunha", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-15.933, -5.717)));
        assertFalse("South Georgia and the South Sandwich Islands", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-54.25, -36.75)));
        assertFalse("Maldives", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(3.2, 73.22)));
        assertFalse("Mauritius", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-20.2, 57.5)));
        assertFalse("Seychelles", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-4.583333, 55.666667)));
        assertFalse("Bangladesh", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(23.8, 90.3)));
        assertFalse("Bhutan", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(27.417, 90.435)));
        assertFalse("Brunei", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(4.5, 114.666667)));
        assertFalse("East Timor", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-8.833333, 125.916667)));
        assertFalse("Hong Kong", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(22.3, 114.2)));
        assertFalse("Indonesia", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-5, 120)));
        assertFalse("India", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(21, 78)));
        assertFalse("Japan", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(35, 136)));
        assertFalse("Macau", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(22.166667, 113.55)));
        assertFalse("Malaysia", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(2.5, 112.5)));
        assertFalse("Nepal", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(28.166667, 84.25)));
        assertFalse("Pakistan", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(30, 70)));
        assertFalse("Singapore", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(1.3, 103.8)));
        assertFalse("Sri Lanka", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(7, 81)));
        assertFalse("Thailand", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(15.4, 101.3)));
        assertFalse("Botswana", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-24.658333, 25.908333)));
        assertFalse("Kenya", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(1, 38)));
        assertFalse("Lesotho", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-29.6, 28.3)));
        assertFalse("Malawi", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-13.5, 34)));
        assertFalse("Mauritius", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-20.2, 57.5)));
        assertFalse("Mozambique", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-18.25, 35)));
        assertFalse("Namibia", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-22, 17)));
        assertFalse("South Africa", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-30, 25)));
        assertFalse("Swaziland", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-26.5, 31.5)));
        assertFalse("Tanzania", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-6.307, 34.854)));
        assertFalse("Uganda", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(1, 32)));
        assertFalse("Zambia", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-15, 30)));
        assertFalse("Zimbabwe", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-20, 30)));
        assertFalse("Australia", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-27, 133)));
        assertFalse("Christmas Island", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-10.483333, 105.633333)));
        assertFalse("Cocos (Keeling) Islands", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-12.116667, 96.9)));
        assertFalse("Cook Islands", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-21.233333, -159.766667)));
        assertFalse("Fiji", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-18, 179)));
        assertFalse("Kiribati", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(1.416667, 173)));
        assertFalse("Nauru", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-0.533333, 166.933333)));
        assertFalse("New Zealand", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-42, 174)));
        assertFalse("Niue", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-19.05, -169.916667)));
        assertFalse("Norfolk Island", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-29.033333, 167.95)));
        assertFalse("Papua New Guinea", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-6, 147)));
        assertFalse("Pitcairn Islands", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-25.066667, -130.1)));
        assertFalse("Solomon Islands", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-8, 159)));
        assertFalse("Samoa", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-13.583333, -172.333333)));
        assertFalse("Tokelau", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-9.166667, -171.833333)));
        assertFalse("Tonga", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-20, -175)));
        assertFalse("Tuvalu", RightAndLefthandTraffic.isRightHandTraffic(new LatLon(-8, 178)));
    }
}
