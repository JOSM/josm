// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;

import org.openstreetmap.josm.Main;

/** The error severity */
public enum Severity {
    /** Error messages */
    ERROR(tr("Errors"), /* ICON(data/) */"error",       Main.pref.getColor(marktr("validation error"), Color.RED)),
    /** Warning messages */
    WARNING(tr("Warnings"), /* ICON(data/) */"warning", Main.pref.getColor(marktr("validation warning"), Color.YELLOW)),
    /** Other messages */
    OTHER(tr("Other"), /* ICON(data/) */"other",        Main.pref.getColor(marktr("validation other"), Color.CYAN));

    /** Description of the severity code */
    private final String message;

    /** Associated icon */
    private final String icon;

    /** Associated color */
    private final Color color;

    /**
     * Constructor
     *
     * @param message Description
     * @param icon Associated icon
     * @param color The color of this severity
     */
    Severity(String message, String icon, Color color) {
        this.message = message;
        this.icon = icon;
        this.color = color;
    }

    public static void getColors() {
        for (Severity c:values()) {
            c.getColor();
        }
    }

    @Override
    public String toString() {
        return message;
    }

    /**
     * Gets the associated icon
     * @return the associated icon
     */
    public String getIcon() {
        return icon;
    }

    /**
     * Gets the associated color
     * @return The associated color
     */
    public Color getColor() {
        return color;
    }


}
