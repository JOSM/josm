// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.projection;

import java.util.Arrays;
import java.util.ArrayList;

/**
 * Class to handle projections
 *
 */
public class Projections {
    /**
     * List of all available projections.
     */
    private static ArrayList<Projection> allProjections =
    new ArrayList<Projection>(Arrays.asList(new Projection[] {
        // global projections
        new Epsg4326(),
        new Mercator(),
        new UTM(),
        // regional - alphabetical order by country name
        new LambertEST(), // Still needs proper default zoom
        new Lambert(),    // Still needs proper default zoom
        new LambertCC9Zones(),    // Still needs proper default zoom
        new UTM_France_DOM(),
        new TransverseMercatorLV(),
        new Puwg(),
        new Epsg3008(), // SWEREF99 13 30
        new SwissGrid(),
    }));

    public static ArrayList<Projection> getProjections() {
        return allProjections;
    }

    /**
     * Adds a new projection to the list of known projections.
     * 
     * For Plugins authors: make sure your plugin is an early plugin, i.e. put
     * Plugin-Early=true in your Manifest.
     */
    public static void addProjection(Projection proj) {
        allProjections.add(proj);
    }
}
