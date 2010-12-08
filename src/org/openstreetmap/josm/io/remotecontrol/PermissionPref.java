// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

/**
 * Contains a preference name to control permission for the operation
 * implemented by the RequestHandler, and an error message to be displayed
 * if not permitted.
 *
 * Use @see PermissionPrefWithDefault instead of this class.
 *
 * @author Bodo Meissner
 */
 @Deprecated
public class PermissionPref {
    /** name of the preference setting to permit the remote operation */
    public String pref;
    /** message to be displayed if operation is not permitted */
    public String message;

    public PermissionPref(String pref, String message)
    {
        this.pref = pref;
        this.message = message;
    }
}