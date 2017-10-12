// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict;

import static org.openstreetmap.josm.tools.I18n.marktr;

import java.awt.Color;

import org.openstreetmap.josm.data.preferences.NamedColorProperty;

/**
 * Conflict color constants.
 * @since 4162
 */
public enum ConflictColors {

    /** Conflict background: no conflict */
    BGCOLOR_NO_CONFLICT(marktr("Conflict background: no conflict"), new Color(234, 234, 234)),
    /** Conflict background: decided */
    BGCOLOR_DECIDED(marktr("Conflict background: decided"), new Color(217, 255, 217)),
    /** Conflict background: undecided */
    BGCOLOR_UNDECIDED(marktr("Conflict background: undecided"), new Color(255, 197, 197)),
    /** Conflict background: drop */
    BGCOLOR_DROP(marktr("Conflict background: drop"), Color.white),
    /** Conflict background: keep */
    BGCOLOR_KEEP(marktr("Conflict background: keep"), new Color(217, 255, 217)),
    /** Conflict background: combined */
    BGCOLOR_COMBINED(marktr("Conflict background: combined"), new Color(217, 255, 217)),
    /** Conflict background: selected */
    BGCOLOR_SELECTED(marktr("Conflict background: selected"), new Color(143, 170, 255)),

    /** Conflict foreground: undecided */
    FGCOLOR_UNDECIDED(marktr("Conflict foreground: undecided"), Color.black),
    /** Conflict foreground: drop */
    FGCOLOR_DROP(marktr("Conflict foreground: drop"), Color.lightGray),
    /** Conflict foreground: keep */
    FGCOLOR_KEEP(marktr("Conflict foreground: keep"), Color.black),

    /** Conflict background: empty row */
    BGCOLOR_EMPTY_ROW(marktr("Conflict background: empty row"), new Color(234, 234, 234)),
    /** Conflict background: frozen */
    BGCOLOR_FROZEN(marktr("Conflict background: frozen"), new Color(234, 234, 234)),
    /** Conflict background: in comparison */
    BGCOLOR_PARTICIPATING_IN_COMPARISON(marktr("Conflict background: in comparison"), Color.black),
    /** Conflict foreground: in comparison */
    FGCOLOR_PARTICIPATING_IN_COMPARISON(marktr("Conflict foreground: in comparison"), Color.white),
    /** Conflict background */
    BGCOLOR(marktr("Conflict background"), Color.white),
    /** Conflict foreground */
    FGCOLOR(marktr("Conflict foreground"), Color.black),

    /** Conflict background: not in opposite */
    BGCOLOR_NOT_IN_OPPOSITE(marktr("Conflict background: not in opposite"), new Color(255, 197, 197)),
    /** Conflict background: in opposite */
    BGCOLOR_IN_OPPOSITE(marktr("Conflict background: in opposite"), new Color(255, 234, 213)),
    /** Conflict background: same position in opposite */
    BGCOLOR_SAME_POSITION_IN_OPPOSITE(marktr("Conflict background: same position in opposite"), new Color(217, 255, 217)),

    /** Conflict background: keep one tag */
    BGCOLOR_TAG_KEEP_ONE(marktr("Conflict background: keep one tag"), new Color(217, 255, 217)),
    /** Conflict foreground: keep one tag */
    FGCOLOR_TAG_KEEP_ONE(marktr("Conflict foreground: keep one tag"), Color.black),
    /** Conflict background: drop tag */
    BGCOLOR_TAG_KEEP_NONE(marktr("Conflict background: drop tag"), Color.lightGray),
    /** Conflict foreground: drop tag */
    FGCOLOR_TAG_KEEP_NONE(marktr("Conflict foreground: drop tag"), Color.black),
    /** Conflict background: keep all tags */
    BGCOLOR_TAG_KEEP_ALL(marktr("Conflict background: keep all tags"), new Color(255, 234, 213)),
    /** Conflict foreground: keep all tags */
    FGCOLOR_TAG_KEEP_ALL(marktr("Conflict foreground: keep all tags"), Color.black),
    /** Conflict background: sum all numeric tags */
    BGCOLOR_TAG_SUM_ALL_NUM(marktr("Conflict background: sum all numeric tags"), new Color(255, 234, 213)),
    /** Conflict foreground: sum all numeric tags */
    FGCOLOR_TAG_SUM_ALL_NUM(marktr("Conflict foreground: sum all numeric tags"), Color.black),

    /** Conflict background: keep member */
    BGCOLOR_MEMBER_KEEP(marktr("Conflict background: keep member"), new Color(217, 255, 217)),
    /** Conflict foreground: keep member */
    FGCOLOR_MEMBER_KEEP(marktr("Conflict foreground: keep member"), Color.black),
    /** Conflict background: remove member */
    BGCOLOR_MEMBER_REMOVE(marktr("Conflict background: remove member"), Color.lightGray),
    /** Conflict foreground: remove member */
    FGCOLOR_MEMBER_REMOVE(marktr("Conflict foreground: remove member"), Color.black);

    private final NamedColorProperty property;

    ConflictColors(String name, Color defaultColor) {
        property = new NamedColorProperty(name, defaultColor);
    }

    /**
     * Returns the color.
     * @return the color
     */
    public Color get() {
        return property.get();
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
