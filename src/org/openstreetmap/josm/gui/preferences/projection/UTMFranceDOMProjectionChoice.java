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

    private static final String FORT_MARIGOT_NAME = tr("Guadeloupe Fort-Marigot 1949");
    private static final String SAINTE_ANNE_NAME = tr("Guadeloupe Ste-Anne 1948");
    private static final String MARTINIQUE_NAME = tr("Martinique Fort Desaix 1952");
    private static final String REUNION_92_NAME = tr("Reunion RGR92");
    private static final String GUYANE_92_NAME = tr("Guyane RGFG95");
    private static final String[] UTM_GEODESIC_NAMES = {FORT_MARIGOT_NAME, SAINTE_ANNE_NAME, MARTINIQUE_NAME, REUNION_92_NAME, GUYANE_92_NAME};

    private static final Integer FORT_MARIGOT_EPSG = 2969;
    private static final Integer SAINTE_ANNE_EPSG = 2970;
    private static final Integer MARTINIQUE_EPSG = 2973;
    private static final Integer REUNION_EPSG = 2975;
    private static final Integer GUYANE_EPSG = 2972;
    private static final Integer[] UTM_EPSGS = {FORT_MARIGOT_EPSG, SAINTE_ANNE_EPSG, MARTINIQUE_EPSG, REUNION_EPSG, GUYANE_EPSG };

    /**
     * Constructs a new {@code UTMFranceDOMProjectionChoice}.
     */
    public UTMFranceDOMProjectionChoice() {
        super(tr("UTM France (DOM)"), /* NO-ICON */ "core:utmfrancedom", UTM_GEODESIC_NAMES, tr("UTM Geodesic system"));
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
        return UTM_GEODESIC_NAMES[index];
    }

    @Override
    public String getCurrentCode() {
        return "EPSG:" + UTM_EPSGS[index];
    }

    @Override
    public String[] allCodes() {
        String[] res = new String[UTM_EPSGS.length];
        for (int i = 0; i < UTM_EPSGS.length; ++i) {
            res[i] = "EPSG:" + UTM_EPSGS[i];
        }
        return res;
    }

    @Override
    public Collection<String> getPreferencesFromCode(String code) {
        for (int i = 0; i < UTM_EPSGS.length; i++) {
            if (("EPSG:" + UTM_EPSGS[i]).equals(code))
                return Collections.singleton(Integer.toString(i+1));
        }
        return null;
    }
}
