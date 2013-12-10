package org.openstreetmap.josm.gui;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.gui.NavigatableComponent.SystemOfMeasurement;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Unit tests of {@link SystemOfMeasurement} class.
 */
public class SystemOfMeasurementTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        Main.initApplicationPreferences();
    }
    
    /**
     * Test of {@link SystemOfMeasurement#getDistText} method.
     */
    @Test
    public void testGetDistText() {
        
        assertEquals("< 0.01 m", NavigatableComponent.METRIC_SOM.getDistText(-1));
        assertEquals("< 0.01 m", NavigatableComponent.METRIC_SOM.getDistText(-0.99));
        assertEquals("< 0.01 m", NavigatableComponent.METRIC_SOM.getDistText(-0));
        assertEquals("< 0.01 m", NavigatableComponent.METRIC_SOM.getDistText(0));

        assertEquals("0.01 m", NavigatableComponent.METRIC_SOM.getDistText(0.01));
        
        assertEquals("0.99 m", NavigatableComponent.METRIC_SOM.getDistText(0.99));
        assertEquals("1.00 m", NavigatableComponent.METRIC_SOM.getDistText(1.0));
        assertEquals("1.01 m", NavigatableComponent.METRIC_SOM.getDistText(1.01));

        assertEquals("9.99 m", NavigatableComponent.METRIC_SOM.getDistText(9.99));
        assertEquals("10.0 m", NavigatableComponent.METRIC_SOM.getDistText(10.0));
        assertEquals("10.0 m", NavigatableComponent.METRIC_SOM.getDistText(10.01));
        assertEquals("10.0 m", NavigatableComponent.METRIC_SOM.getDistText(10.049));
        assertEquals("10.1 m", NavigatableComponent.METRIC_SOM.getDistText(10.050));
        assertEquals("10.1 m", NavigatableComponent.METRIC_SOM.getDistText(10.051));

        assertEquals("100.0 m", NavigatableComponent.METRIC_SOM.getDistText(99.99));
        assertEquals("100.0 m", NavigatableComponent.METRIC_SOM.getDistText(100.0));
        assertEquals("100.0 m", NavigatableComponent.METRIC_SOM.getDistText(100.01));

        assertEquals("1000.0 m", NavigatableComponent.METRIC_SOM.getDistText(999.99)); // TODO ? 1.00 km should be better
        assertEquals("1000.0 m", NavigatableComponent.METRIC_SOM.getDistText(1000.0)); // TODO ? 1.00 km should be better
        assertEquals("1.00 km", NavigatableComponent.METRIC_SOM.getDistText(1000.01));

        assertEquals("10.00 km", NavigatableComponent.METRIC_SOM.getDistText(9999.99)); // TODO ? 10.0 km should be better
        assertEquals("10.0 km", NavigatableComponent.METRIC_SOM.getDistText(10000.0));
        assertEquals("10.0 km", NavigatableComponent.METRIC_SOM.getDistText(10000.01));

        assertEquals("100.0 km", NavigatableComponent.METRIC_SOM.getDistText(99999.99));
        assertEquals("100.0 km", NavigatableComponent.METRIC_SOM.getDistText(100000.0));
        assertEquals("100.0 km", NavigatableComponent.METRIC_SOM.getDistText(100000.01));
    }

    @Test
    public void testGetDistTextLocalized() {
        final DecimalFormat format = new DecimalFormat("0.000", DecimalFormatSymbols.getInstance(Locale.GERMAN));
        assertEquals("0,001 m", NavigatableComponent.METRIC_SOM.getDistText(0.001, format, 1e-6));
        assertEquals("< 0,010 m", NavigatableComponent.METRIC_SOM.getDistText(0.001, format, 0.01));
        assertEquals("10,051 m", NavigatableComponent.METRIC_SOM.getDistText(10.0514, format, 0.01));
        assertEquals("10,052 m", NavigatableComponent.METRIC_SOM.getDistText(10.0515, format, 0.01));
        assertEquals("100,000 km", NavigatableComponent.METRIC_SOM.getDistText(100000.0, format, 0.01));
    }

    /**
     * Test of {@link SystemOfMeasurement#getAreaText} method.
     */
    @Test
    public void testGetAreaText() {
        assertEquals("< 0.01 m²", NavigatableComponent.METRIC_SOM.getAreaText(-1));
        assertEquals("< 0.01 m²", NavigatableComponent.METRIC_SOM.getAreaText(-0.99));
        assertEquals("< 0.01 m²", NavigatableComponent.METRIC_SOM.getAreaText(-0));
        assertEquals("< 0.01 m²", NavigatableComponent.METRIC_SOM.getAreaText(0));

        assertEquals("0.01 m²", NavigatableComponent.METRIC_SOM.getAreaText(0.01));
        
        assertEquals("0.99 m²", NavigatableComponent.METRIC_SOM.getAreaText(0.99));
        assertEquals("1.00 m²", NavigatableComponent.METRIC_SOM.getAreaText(1.0));
        assertEquals("1.01 m²", NavigatableComponent.METRIC_SOM.getAreaText(1.01));

        assertEquals("9.99 m²", NavigatableComponent.METRIC_SOM.getAreaText(9.99));
        assertEquals("10.0 m²", NavigatableComponent.METRIC_SOM.getAreaText(10.0));
        assertEquals("10.0 m²", NavigatableComponent.METRIC_SOM.getAreaText(10.01));
        assertEquals("10.0 m²", NavigatableComponent.METRIC_SOM.getAreaText(10.049));
        assertEquals("10.1 m²", NavigatableComponent.METRIC_SOM.getAreaText(10.050));
        assertEquals("10.1 m²", NavigatableComponent.METRIC_SOM.getAreaText(10.051));

        assertEquals("100.0 m²", NavigatableComponent.METRIC_SOM.getAreaText(99.99));
        assertEquals("100.0 m²", NavigatableComponent.METRIC_SOM.getAreaText(100.0));
        assertEquals("100.0 m²", NavigatableComponent.METRIC_SOM.getAreaText(100.01));

        assertEquals("1000.0 m²", NavigatableComponent.METRIC_SOM.getAreaText(999.99));
        assertEquals("1000.0 m²", NavigatableComponent.METRIC_SOM.getAreaText(1000.0));
        assertEquals("1000.0 m²", NavigatableComponent.METRIC_SOM.getAreaText(1000.01));

        assertEquals("10000.0 m²", NavigatableComponent.METRIC_SOM.getAreaText(9999.99)); // TODO ? 1.00 ha should be better
        assertEquals("10000.0 m²", NavigatableComponent.METRIC_SOM.getAreaText(10000.0)); // TODO ? 1.00 ha should be better
        assertEquals("1.00 ha", NavigatableComponent.METRIC_SOM.getAreaText(10000.01));

        assertEquals("10.0 ha", NavigatableComponent.METRIC_SOM.getAreaText(99999.99));
        assertEquals("10.0 ha", NavigatableComponent.METRIC_SOM.getAreaText(100000.0));
        assertEquals("10.0 ha", NavigatableComponent.METRIC_SOM.getAreaText(100000.01));

        assertEquals("100.0 ha", NavigatableComponent.METRIC_SOM.getAreaText(999999.99)); // TODO ? 1.00 km² should be better
        assertEquals("1.00 km²", NavigatableComponent.METRIC_SOM.getAreaText(1000000.0));
        assertEquals("1.00 km²", NavigatableComponent.METRIC_SOM.getAreaText(1000000.01));

        assertEquals("10.0 km²", NavigatableComponent.METRIC_SOM.getAreaText(9999999.99));
        assertEquals("10.0 km²", NavigatableComponent.METRIC_SOM.getAreaText(10000000.0));
        assertEquals("10.0 km²", NavigatableComponent.METRIC_SOM.getAreaText(10000000.01));
    }
}
