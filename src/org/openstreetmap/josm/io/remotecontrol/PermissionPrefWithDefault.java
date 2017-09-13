// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.List;

import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Contains a preference name to control permission for the operation
 * implemented by the RequestHandler, and an error message to be displayed if
 * not permitted.
 *
 * @author Bodo Meissner
 */
public class PermissionPrefWithDefault {

    public static final PermissionPrefWithDefault LOAD_DATA =
            new PermissionPrefWithDefault("remotecontrol.permission.load-data", true, tr("Load data from API"));
    public static final PermissionPrefWithDefault IMPORT_DATA =
            new PermissionPrefWithDefault("remotecontrol.permission.import", true, tr("Import data from URL"));
    public static final PermissionPrefWithDefault OPEN_FILES =
            new PermissionPrefWithDefault("remotecontrol.permission.open-files", false, tr("Open local files"));
    public static final PermissionPrefWithDefault LOAD_IMAGERY =
            new PermissionPrefWithDefault("remotecontrol.permission.imagery", true, tr("Load imagery layers"));
    public static final PermissionPrefWithDefault CHANGE_SELECTION =
            new PermissionPrefWithDefault("remotecontrol.permission.change-selection", true, tr("Change the selection"));
    public static final PermissionPrefWithDefault CHANGE_VIEWPORT =
            new PermissionPrefWithDefault("remotecontrol.permission.change-viewport", true, tr("Change the viewport"));
    public static final PermissionPrefWithDefault CREATE_OBJECTS =
            new PermissionPrefWithDefault("remotecontrol.permission.create-objects", true, tr("Create new objects"));
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

    public PermissionPrefWithDefault(String pref, boolean defaultVal, String preferenceText) {
        this.pref = pref;
        this.defaultVal = defaultVal;
        this.preferenceText = preferenceText;
    }

    public boolean isAllowed() {
        return Config.getPref().getBoolean(pref, defaultVal);
    }

    public static List<PermissionPrefWithDefault> getPermissionPrefs() {
        return Arrays.asList(
                LOAD_DATA, IMPORT_DATA, OPEN_FILES, LOAD_IMAGERY,
                CHANGE_SELECTION, CHANGE_VIEWPORT,
                CREATE_OBJECTS, READ_PROTOCOL_VERSION);
    }
}
