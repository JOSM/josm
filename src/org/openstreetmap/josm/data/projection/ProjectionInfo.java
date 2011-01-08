// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.HashMap;

public class ProjectionInfo {
    private static HashMap<String, Projection> allCodes;

    private static ProjectionSubPrefs recreateProj(ProjectionSubPrefs proj) {
        try {
            return proj.getClass().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(
                    tr("Cannot instantiate projection ''{0}''", proj.getClass().toString()), e);
        }
    }

    static {
        allCodes = new HashMap<String, Projection>();
        for (Projection proj : Projection.allProjections) {
            if (proj instanceof ProjectionSubPrefs) {
                ProjectionSubPrefs projSub = recreateProj((ProjectionSubPrefs)proj);
                for (String code : projSub.allCodes()) {
                    allCodes.put(code, projSub);
                }
            } else {
                allCodes.put(proj.toCode(), proj);
            }
        }
    }

    public static Projection getProjectionByCode(String code) {
        Projection proj = allCodes.get(code);
        if (proj == null) return null;
        if (code.equals(proj.toCode())) return proj;
        if (!(proj instanceof ProjectionSubPrefs))
            throw new IllegalStateException(tr(
                    "Projection code mismatch in '{0}': toCode() returns ''{1}'', expected ''{2}''",
                    proj.getClass().toString(), proj.toCode(), code));
        ProjectionSubPrefs projSub = recreateProj((ProjectionSubPrefs)proj);
        Collection<String> prefs = projSub.getPreferencesFromCode(code);
        if (prefs != null) {
            projSub.setPreferences(prefs);
        }
        if (!code.equals(projSub.toCode()))
            throw new IllegalStateException(tr(
                    "Bad implementation of '{0}' projection class: cannot set preferences to match code ''{1}''",
                    projSub.getClass().toString(), code));
        allCodes.put(code, projSub);
        return projSub;
    }
}
