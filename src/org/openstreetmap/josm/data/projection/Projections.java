// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.projection;

import java.util.ArrayList;
import java.util.Arrays;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

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
                new GaussKrueger(),
                new LambertEST(),
                new Lambert(),
                new Lambert93(),
                new LambertCC9Zones(),
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

    static public EastNorth project(LatLon ll) {
        if (ll == null) return null;
        return Main.getProjection().latlon2eastNorth(ll);
    }

    static public LatLon inverseProject(EastNorth en) {
        if (en == null) return null;
        return Main.getProjection().eastNorth2latlon(en);
    }
}
