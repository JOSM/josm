// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

/**
 * Contains a preference name to control permission for the operation
 * implemented by the RequestHandler, and an error message to be displayed
 * if not permitted.
 *
 * @author Bodo Meissner
 */
public class PermissionPrefWithDefault {

    /** name of the preference setting to permit the remote operation */
    public String pref;
    /** message to be displayed if operation is not permitted */
    public String message;

    public boolean defaultVal = true;

    public PermissionPrefWithDefault(String pref, boolean defaultVal, String message) {
        this.pref = pref;
        this.message = message;
        this.defaultVal = defaultVal;
    }
}
