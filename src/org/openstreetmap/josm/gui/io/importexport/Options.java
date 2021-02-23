// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

/**
 * Options for ImportExport classes. Not all ImportExport classes support all options.
 * @author Taylor Smock
 * @since 17534
 */
public enum Options {
    /** Allow import/export of web resources */
    ALLOW_WEB_RESOURCES,
    /** Record history. Primarily used in OpenFileAction */
    RECORD_HISTORY
}
