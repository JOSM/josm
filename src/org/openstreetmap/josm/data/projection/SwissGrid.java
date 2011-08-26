//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;

import javax.swing.Box;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.datum.ThreeParameterDatum;
import org.openstreetmap.josm.data.projection.proj.SwissObliqueMercator;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.tools.GBC;

/**
 * SwissGrid CH1903 / L03, see http://de.wikipedia.org/wiki/Swiss_Grid.
 * 
 * Actually, what we have here, is CH1903+ (EPSG:2056), but without
 * the additional false easting of 2000km and false northing 1000 km.
 *
 * To get to CH1903, a shift file is required. So currently, there are errors
 * up to 1.6m (depending on the location).
 * 
 * This projection does not have any parameters, it only implements
 * ProjectionSubPrefs to show a warning that the grid file correction is not done.
 */
public class SwissGrid extends AbstractProjection implements ProjectionSubPrefs {

    public SwissGrid() {
        ellps = Ellipsoid.Bessel1841;
        datum = new ThreeParameterDatum("SwissGrid Datum", null, ellps, 674.374, 15.056, 405.346);
        x_0 = 600000;
        y_0 = 200000;
        lon_0 = 7.0 + 26.0/60 + 22.50/3600;
        double lat_0 = 46.0 + 57.0/60 + 8.66/3600;
        proj = new SwissObliqueMercator(ellps, lat_0);
    }

    @Override
    public String toString() {
        return tr("Swiss Grid (Switzerland)");
    }

    @Override
    public Integer getEpsgCode() {
        return 21781;
    }

    @Override
    public int hashCode() {
        return getClass().getName().hashCode(); // we have no variables
    }

    @Override
    public String getCacheDirectoryName() {
        return "swissgrid";
    }

    @Override
    public Bounds getWorldBoundsLatLon() {
        return new Bounds(new LatLon(45.7, 5.7), new LatLon(47.9, 10.6));
    }

    @Override
    public void setupPreferencePanel(JPanel p, ActionListener listener) {
        p.add(new HtmlPanel(tr("<i>CH1903 / LV03 (without local corrections)</i>")), GBC.eol().fill(GBC.HORIZONTAL));
        p.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.BOTH));
    }

    @Override
    public void setPreferences(Collection<String> args) {
    }

    @Override
    public Collection<String> getPreferences(JPanel p) {
        return Collections.singletonList("CH1903");
    }

    @Override
    public String[] allCodes() {
        return new String[] { "EPSG:21781" };
    }

    @Override
    public Collection<String> getPreferencesFromCode(String code) {
        if ("EPSG:21781".equals(code))
            return Collections.singletonList("CH1903");
        return null;
    }
}
