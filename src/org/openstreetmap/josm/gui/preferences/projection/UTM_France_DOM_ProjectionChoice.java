// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.UTM_France_DOM;

public class UTM_France_DOM_ProjectionChoice extends ListProjectionChoice {

    private final static String FortMarigotName = tr("Guadeloupe Fort-Marigot 1949");
    private final static String SainteAnneName = tr("Guadeloupe Ste-Anne 1948");
    private final static String MartiniqueName = tr("Martinique Fort Desaix 1952");
    private final static String Reunion92Name = tr("Reunion RGR92");
    private final static String Guyane92Name = tr("Guyane RGFG95");
    private final static String[] utmGeodesicsNames = { FortMarigotName, SainteAnneName, MartiniqueName, Reunion92Name, Guyane92Name};

    public UTM_France_DOM_ProjectionChoice() {
        super("core:utmfrancedom", tr("UTM France (DOM)"), utmGeodesicsNames, tr("UTM Geodesic system"));
    }

    @Override
    protected String indexToZone(int index) {
        return Integer.toString(index + 1);
    }

    @Override
    protected int zoneToIndex(String zone) {
        try {
            return Integer.parseInt(zone) - 1;
        } catch(NumberFormatException e) {}
        return defaultIndex;
    }

    @Override
    public Projection getProjection() {
        return new UTM_France_DOM(index);
    }

    @Override
    public String[] allCodes() {
        String[] res = new String[UTM_France_DOM.utmEPSGs.length];
        for (int i=0; i<UTM_France_DOM.utmEPSGs.length; ++i) {
            res[i] = "EPSG:"+UTM_France_DOM.utmEPSGs[i];
        }
        return res;
    }

    @Override
    public Collection<String> getPreferencesFromCode(String code) {
        for (int i=0; i < UTM_France_DOM.utmEPSGs.length; i++ )
            if (("EPSG:"+UTM_France_DOM.utmEPSGs[i]).equals(code))
                return Collections.singleton(Integer.toString(i+1));
        return null;
    }

}
