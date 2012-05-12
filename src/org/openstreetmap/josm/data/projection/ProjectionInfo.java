// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.josm.gui.preferences.projection.ProjectionChoice;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionPreference;

public class ProjectionInfo {
    private static Map<String, ProjectionChoice> allCodesPC = new HashMap<String, ProjectionChoice>();
    private static Map<String, Projection> allCodes = new HashMap<String, Projection>();

    static {
        for (ProjectionChoice pc : ProjectionPreference.getProjectionChoices()) {
            for (String code : pc.allCodes()) {
                allCodesPC.put(code, pc);
            }
        }
    }

    public static Projection getProjectionByCode(String code) {
        Projection p = allCodes.get(code);
        if (p != null) return p;
        ProjectionChoice pc = allCodesPC.get(code);
        if (pc == null) return null;
        Collection<String> pref = pc.getPreferencesFromCode(code);
        pc.setPreferences(pref);
        p = pc.getProjection();
        allCodes.put(code, p);
        return p;
    }
}
