// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.actions.ExtensionFileFilter;

public class WMSLayerImporter extends FileImporter {

    public static final ExtensionFileFilter FILE_FILTER = new ExtensionFileFilter(
            "wms", "wms", tr("WMS Files (*.wms)"));
    
    public WMSLayerImporter() {
        super(FILE_FILTER);
    }
}
