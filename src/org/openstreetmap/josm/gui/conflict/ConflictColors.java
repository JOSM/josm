// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict;

import static org.openstreetmap.josm.tools.I18n.marktr;

import java.awt.Color;

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
    BGCOLOR_PARTICIPATING_IN_COMPARISON(marktr("Conflict background: in comparison"), Color.black),
    FGCOLOR_PARTICIPATING_IN_COMPARISON(marktr("Conflict foreground: in comparison"), Color.white),
    BGCOLOR(marktr("Conflict background"), Color.white),
    FGCOLOR(marktr("Conflict foreground"), Color.black),

    BGCOLOR_NOT_IN_OPPOSITE(marktr("Conflict background: not in opposite"), new Color(255,197,197)),
    BGCOLOR_IN_OPPOSITE(marktr("Conflict background: in opposite"), new Color(255,234,213)),
    BGCOLOR_SAME_POSITION_IN_OPPOSITE(marktr("Conflict background: same position in opposite"), new Color(217,255,217)),

    BGCOLOR_TAG_KEEP_ONE (marktr("Conflict background: keep one tag"), new Color(217,255,217)),
    FGCOLOR_TAG_KEEP_ONE (marktr("Conflict foreground: keep one tag"), Color.black),
    BGCOLOR_TAG_KEEP_NONE(marktr("Conflict background: drop tag"), Color.lightGray),
    FGCOLOR_TAG_KEEP_NONE(marktr("Conflict foreground: drop tag"), Color.black),
    BGCOLOR_TAG_KEEP_ALL (marktr("Conflict background: keep all tags"), new Color(255,234,213)),
    FGCOLOR_TAG_KEEP_ALL (marktr("Conflict foreground: keep all tags"), Color.black),

    BGCOLOR_MEMBER_KEEP  (marktr("Conflict background: keep member"), new Color(217,255,217)),
    FGCOLOR_MEMBER_KEEP  (marktr("Conflict foreground: keep member"), Color.black),
    BGCOLOR_MEMBER_REMOVE(marktr("Conflict background: remove member"), Color.lightGray),
    FGCOLOR_MEMBER_REMOVE(marktr("Conflict foreground: remove member"), Color.black);

    private final String name;
    private final Color defaultColor;

    private ConflictColors(String name, Color defaultColor) {
        this.name = name;
        this.defaultColor = defaultColor;
    }

    @Override
    public String getColorName() {
        return name;
    }

    @Override
    public Color getDefaultValue() {
        return defaultColor;
    }

    @Override
    public String getSpecialName() {
        return null;
    }

    /**
     * Returns the color.
     * @return the color
     */
    public Color get() {
        return Main.pref.getColor(this);
    }

    /**
     * Loads all colors from preferences.
     */
    public static void getColors() {
        for (ConflictColors c : values()) {
            c.get();
        }
    }
}
