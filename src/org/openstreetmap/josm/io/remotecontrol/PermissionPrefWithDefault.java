// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Contains a preference name to control permission for the operation
 * implemented by the RequestHandler, and an error message to be displayed if
 * not permitted.
 *
 * @author Bodo Meissner
 */
public class PermissionPrefWithDefault {
    private static final List<PermissionPrefWithDefault> PREFS = new ArrayList<>();

    /** Load data from API */
    public static final PermissionPrefWithDefault LOAD_DATA =
            new PermissionPrefWithDefault("remotecontrol.permission.load-data", true, tr("Load data from API"));
    /** Import data from URL */
    public static final PermissionPrefWithDefault IMPORT_DATA =
            new PermissionPrefWithDefault("remotecontrol.permission.import", true, tr("Import data from URL"));
    /** Open local files */
    public static final PermissionPrefWithDefault OPEN_FILES =
            new PermissionPrefWithDefault("remotecontrol.permission.open-files", false, tr("Open local files"));
    /** Load imagery layers */
    public static final PermissionPrefWithDefault LOAD_IMAGERY =
            new PermissionPrefWithDefault("remotecontrol.permission.imagery", true, tr("Load imagery layers"));
    /** Change the selection */
    public static final PermissionPrefWithDefault CHANGE_SELECTION =
            new PermissionPrefWithDefault("remotecontrol.permission.change-selection", true, tr("Change the selection"));
    /** Change the viewport */
    public static final PermissionPrefWithDefault CHANGE_VIEWPORT =
            new PermissionPrefWithDefault("remotecontrol.permission.change-viewport", true, tr("Change the viewport"));
    /** Create new objects */
    public static final PermissionPrefWithDefault CREATE_OBJECTS =
            new PermissionPrefWithDefault("remotecontrol.permission.create-objects", true, tr("Create new objects"));
    /** Read protocol version */
    public static final PermissionPrefWithDefault READ_PROTOCOL_VERSION =
            new PermissionPrefWithDefault("remotecontrol.permission.read-protocolversion", true, tr("Read protocol version"));
    /**
     * name of the preference setting to permit the remote operation
     */
    public final String pref;
    /**
     * default preference setting
     */
    public final boolean defaultVal;
    /**
     * text for the preference dialog checkbox
     */
    public final String preferenceText;

    /**
     * Create a new {@code PermissionPrefWithDefault}
     *
     * @param pref           The preference key for the permission
     * @param defaultVal     The default value of the preference
     * @param preferenceText The text to show in UI objects
     */
    public PermissionPrefWithDefault(String pref, boolean defaultVal, String preferenceText) {
        this.pref = pref;
        this.defaultVal = defaultVal;
        this.preferenceText = preferenceText;
    }

    /**
     * Determines if the action is allowed.
     * @return true if the action is allowed
     */
    public boolean isAllowed() {
        return Config.getPref().getBoolean(pref, defaultVal);
    }

    /**
     * Returns a non-modifiable list of permission preferences for Remote Control.
     * @return A non-modifiable list of permission preferences for Remote Control
     */
    public static List<PermissionPrefWithDefault> getPermissionPrefs() {
        if (PREFS.isEmpty())
            RequestProcessor.initialize();
        return Collections.unmodifiableList(PREFS);
    }

    /**
     * Adds a permission preference.
     * @param pref The preference to add to the list returned by
     *             {@link PermissionPrefWithDefault#getPermissionPrefs}
     * @since 15500
     */
    public static void addPermissionPref(PermissionPrefWithDefault pref) {
        if (pref.pref != null && PREFS.parallelStream().noneMatch(tPref -> pref.pref.equals(tPref.pref)))
            PREFS.add(pref);
    }

    /**
     * Removes a permission preference.
     * @param pref The preference to remove from the list returned by
     *             {@link PermissionPrefWithDefault#getPermissionPrefs}
     *
     * @return see {@link List#removeAll}
     * @since 15500
     */
    public static boolean removePermissionPref(PermissionPrefWithDefault pref) {
        List<PermissionPrefWithDefault> toRemove = Collections.emptyList();
        if (pref.pref != null)
            toRemove = PREFS.parallelStream().filter(tPref -> pref.pref.equals(tPref.pref))
                    .collect(Collectors.toList());
        return PREFS.removeAll(toRemove);
    }
}
