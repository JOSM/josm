// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.Main;

/**
 * ProjectionChoice for various French overseas territories (EPSG:2969,2970,2972,2973,2975).
 * <p>
 * @see <a href="https://fr.wikipedia.org/wiki/Système_de_coordonnées_(cartographie)#Dans_les_d.C3.A9partements_d.27Outre-mer">DOM</a>
 */
public class UTMFranceDOMProjectionChoice extends ListProjectionChoice {

    private static final String FortMarigotName = tr("Guadeloupe Fort-Marigot 1949");
    private static final String SainteAnneName = tr("Guadeloupe Ste-Anne 1948");
    private static final String MartiniqueName = tr("Martinique Fort Desaix 1952");
    private static final String Reunion92Name = tr("Reunion RGR92");
    private static final String Guyane92Name = tr("Guyane RGFG95");
    private static final String[] utmGeodesicsNames = {FortMarigotName, SainteAnneName, MartiniqueName, Reunion92Name, Guyane92Name};

    private static final Integer FortMarigotEPSG = 2969;
    private static final Integer SainteAnneEPSG = 2970;
    private static final Integer MartiniqueEPSG = 2973;
    private static final Integer ReunionEPSG = 2975;
    private static final Integer GuyaneEPSG = 2972;
    private static final Integer[] utmEPSGs = {FortMarigotEPSG, SainteAnneEPSG, MartiniqueEPSG, ReunionEPSG, GuyaneEPSG };

    /**
     * Constructs a new {@code UTMFranceDOMProjectionChoice}.
     */
    public UTMFranceDOMProjectionChoice() {
        super(tr("UTM France (DOM)"), /* NO-ICON */ "core:utmfrancedom", utmGeodesicsNames, tr("UTM Geodesic system"));
    }

    @Override
    protected String indexToZone(int index) {
        return Integer.toString(index + 1);
    }

    @Override
    protected int zoneToIndex(String zone) {
        try {
            return Integer.parseInt(zone) - 1;
        } catch (NumberFormatException e) {
            Main.warn(e);
        }
        return defaultIndex;
    }

    @Override
    public String getProjectionName() {
        return utmGeodesicsNames[index];
    }

    @Override
    public String getCurrentCode() {
        return "EPSG:" + utmEPSGs[index];
    }

    @Override
    public String[] allCodes() {
        String[] res = new String[utmEPSGs.length];
        for (int i = 0; i < utmEPSGs.length; ++i) {
            res[i] = "EPSG:" + utmEPSGs[i];
        }
        return res;
    }

    @Override
    public Collection<String> getPreferencesFromCode(String code) {
        for (int i = 0; i < utmEPSGs.length; i++) {
            if (("EPSG:" + utmEPSGs[i]).equals(code))
                return Collections.singleton(Integer.toString(i+1));
        }
        return null;
    }
}
