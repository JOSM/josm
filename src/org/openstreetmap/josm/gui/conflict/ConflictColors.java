// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict;

import static org.openstreetmap.josm.tools.I18n.marktr;

import java.awt.Color;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.ColorKey;

public enum ConflictColors implements ColorKey {

    BGCOLOR_NO_CONFLICT(marktr("Conflict background: no conflict"), new Color(234,234,234)),
    BGCOLOR_DECIDED(marktr("Conflict background: decided"), new Color(217,255,217)),
    BGCOLOR_UNDECIDED(marktr("Conflict background: undecided"), new Color(255,197,197)),
    BGCOLOR_DROP(marktr("Conflict background: drop"), Color.white),
    BGCOLOR_KEEP(marktr("Conflict background: keep"), new Color(217,255,217)),
    BGCOLOR_COMBINED(marktr("Conflict background: combined"), new Color(217,255,217)),
    BGCOLOR_SELECTED(marktr("Conflict background: selected"), new Color(143,170,255)),

    FGCOLOR_UNDECIDED(marktr("Conflict foreground: undecided"), Color.black),
    FGCOLOR_DROP(marktr("Conflict foreground: drop"), Color.lightGray),
    FGCOLOR_KEEP(marktr("Conflict foreground: keep"), Color.black),

    BGCOLOR_EMPTY_ROW(marktr("Conflict background: empty row"), new Color(234,234,234)),
    BGCOLOR_FROZEN(marktr("Conflict background: frozen"), new Color(234,234,234)),
    BGCOLOR_PARTICIPAING_IN_COMPARISON(marktr("Conflict background: in comparison"), Color.black),
    FGCOLOR_PARTICIPAING_IN_COMPARISON(marktr("Conflict foreground: in comparison"), Color.white),
    BGCOLOR(marktr("Conflict background"), Color.white),
    FGCOLOR(marktr("Conflict foreground"), Color.black),

    BGCOLOR_NOT_IN_OPPOSITE(marktr("Conflict background: not in opposite"), new Color(255,197,197)),
    BGCOLOR_IN_OPPOSITE(marktr("Conflict background: in opposite"), new Color(255,234,213)),
    BGCOLOR_SAME_POSITION_IN_OPPOSITE(marktr("Conflict background: same position in opposite"), new Color(217,255,217));

    private final String name;
    private final Color defaultColor;

    private static Color backgroundColorCache = null;

    private ConflictColors(String name, Color defaultColor) {
        this.name = name;
        this.defaultColor = defaultColor;
    }

    public String getColorName() {
        return name;
    }

    public Color getDefault() {
        return defaultColor;
    }

    public String getSpecialName() {
        return null;
    }

    public Color get() {
        return Main.pref.getColor(this);
    }

    public static void getColors() {
        for (ConflictColors c:values()) {
            c.get();
        }
    }
}
