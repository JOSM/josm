// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.SystemOfMeasurement;

/**
 * Unit tests of {@link SystemOfMeasurement} class.
 */
class SystemOfMeasurementTest {
    /**
     * Test of {@link SystemOfMeasurement#getDistText} method.
     */
    @Test
    void testGetDistText() {

        assertEquals("< 0.01 m", SystemOfMeasurement.METRIC.getDistText(-1));
        assertEquals("< 0.01 m", SystemOfMeasurement.METRIC.getDistText(-0.99));
        assertEquals("< 0.01 m", SystemOfMeasurement.METRIC.getDistText(-0));
        assertEquals("< 0.01 m", SystemOfMeasurement.METRIC.getDistText(0));

        assertEquals("0.01 m", SystemOfMeasurement.METRIC.getDistText(0.01));

        assertEquals("0.99 m", SystemOfMeasurement.METRIC.getDistText(0.99));
        assertEquals("1.00 m", SystemOfMeasurement.METRIC.getDistText(1.0));
        assertEquals("1.01 m", SystemOfMeasurement.METRIC.getDistText(1.01));

        assertEquals("9.99 m", SystemOfMeasurement.METRIC.getDistText(9.99));
        assertEquals("10.0 m", SystemOfMeasurement.METRIC.getDistText(10.0));
        assertEquals("10.0 m", SystemOfMeasurement.METRIC.getDistText(10.01));
        assertEquals("10.0 m", SystemOfMeasurement.METRIC.getDistText(10.049));
        assertEquals("10.1 m", SystemOfMeasurement.METRIC.getDistText(10.050));
        assertEquals("10.1 m", SystemOfMeasurement.METRIC.getDistText(10.051));

        assertEquals("100.0 m", SystemOfMeasurement.METRIC.getDistText(99.99));
        assertEquals("100.0 m", SystemOfMeasurement.METRIC.getDistText(100.0));
        assertEquals("100.0 m", SystemOfMeasurement.METRIC.getDistText(100.01));

        assertEquals("1000.0 m", SystemOfMeasurement.METRIC.getDistText(999.99)); // TODO ? 1.00 km should be better
        assertEquals("1000.0 m", SystemOfMeasurement.METRIC.getDistText(1000.0)); // TODO ? 1.00 km should be better
        assertEquals("1.00 km", SystemOfMeasurement.METRIC.getDistText(1000.01));

        assertEquals("10.00 km", SystemOfMeasurement.METRIC.getDistText(9999.99)); // TODO ? 10.0 km should be better
        assertEquals("10.0 km", SystemOfMeasurement.METRIC.getDistText(10000.0));
        assertEquals("10.0 km", SystemOfMeasurement.METRIC.getDistText(10000.01));

        assertEquals("100.0 km", SystemOfMeasurement.METRIC.getDistText(99999.99));
        assertEquals("100.0 km", SystemOfMeasurement.METRIC.getDistText(100000.0));
        assertEquals("100.0 km", SystemOfMeasurement.METRIC.getDistText(100000.01));
    }

    /**
     * Test of {@link SystemOfMeasurement#getDistText} method with a non-English locale.
     */
    @Test
    void testGetDistTextLocalized() {
        final DecimalFormat format = new DecimalFormat("0.000", DecimalFormatSymbols.getInstance(Locale.GERMAN));
        assertEquals("0,001 m", SystemOfMeasurement.METRIC.getDistText(0.001, format, 1e-6));
        assertEquals("< 0,010 m", SystemOfMeasurement.METRIC.getDistText(0.001, format, 0.01));
        assertEquals("10,051 m", SystemOfMeasurement.METRIC.getDistText(10.0514, format, 0.01));
        assertEquals("10,052 m", SystemOfMeasurement.METRIC.getDistText(10.0515, format, 0.01));
        assertEquals("100,000 km", SystemOfMeasurement.METRIC.getDistText(100000.0, format, 0.01));
    }

    /**
     * Test of {@link SystemOfMeasurement#getAreaText} method.
     */
    @Test
    void testGetAreaText() {
        assertEquals("< 0.01 m²", SystemOfMeasurement.METRIC.getAreaText(-1));
        assertEquals("< 0.01 m²", SystemOfMeasurement.METRIC.getAreaText(-0.99));
        assertEquals("< 0.01 m²", SystemOfMeasurement.METRIC.getAreaText(-0));
        assertEquals("< 0.01 m²", SystemOfMeasurement.METRIC.getAreaText(0));

        assertEquals("0.01 m²", SystemOfMeasurement.METRIC.getAreaText(0.01));

        assertEquals("0.99 m²", SystemOfMeasurement.METRIC.getAreaText(0.99));
        assertEquals("1.00 m²", SystemOfMeasurement.METRIC.getAreaText(1.0));
        assertEquals("1.01 m²", SystemOfMeasurement.METRIC.getAreaText(1.01));

        assertEquals("9.99 m²", SystemOfMeasurement.METRIC.getAreaText(9.99));
        assertEquals("10.0 m²", SystemOfMeasurement.METRIC.getAreaText(10.0));
        assertEquals("10.0 m²", SystemOfMeasurement.METRIC.getAreaText(10.01));
        assertEquals("10.0 m²", SystemOfMeasurement.METRIC.getAreaText(10.049));
        assertEquals("10.1 m²", SystemOfMeasurement.METRIC.getAreaText(10.050));
        assertEquals("10.1 m²", SystemOfMeasurement.METRIC.getAreaText(10.051));

        assertEquals("100.0 m²", SystemOfMeasurement.METRIC.getAreaText(99.99));
        assertEquals("100.0 m²", SystemOfMeasurement.METRIC.getAreaText(100.0));
        assertEquals("100.0 m²", SystemOfMeasurement.METRIC.getAreaText(100.01));

        assertEquals("1000.0 m²", SystemOfMeasurement.METRIC.getAreaText(999.99));
        assertEquals("1000.0 m²", SystemOfMeasurement.METRIC.getAreaText(1000.0));
        assertEquals("1000.0 m²", SystemOfMeasurement.METRIC.getAreaText(1000.01));

        assertEquals("10000.0 m²", SystemOfMeasurement.METRIC.getAreaText(9999.99)); // TODO ? 1.00 ha should be better
        assertEquals("10000.0 m²", SystemOfMeasurement.METRIC.getAreaText(10000.0)); // TODO ? 1.00 ha should be better
        assertEquals("1.00 ha", SystemOfMeasurement.METRIC.getAreaText(10000.01));

        assertEquals("10.0 ha", SystemOfMeasurement.METRIC.getAreaText(99999.99));
        assertEquals("10.0 ha", SystemOfMeasurement.METRIC.getAreaText(100000.0));
        assertEquals("10.0 ha", SystemOfMeasurement.METRIC.getAreaText(100000.01));

        assertEquals("100.0 ha", SystemOfMeasurement.METRIC.getAreaText(999999.99)); // TODO ? 1.00 km² should be better
        assertEquals("1.00 km²", SystemOfMeasurement.METRIC.getAreaText(1000000.0));
        assertEquals("1.00 km²", SystemOfMeasurement.METRIC.getAreaText(1000000.01));

        assertEquals("10.0 km²", SystemOfMeasurement.METRIC.getAreaText(9999999.99));
        assertEquals("10.0 km²", SystemOfMeasurement.METRIC.getAreaText(10000000.0));
        assertEquals("10.0 km²", SystemOfMeasurement.METRIC.getAreaText(10000000.01));
    }
}
