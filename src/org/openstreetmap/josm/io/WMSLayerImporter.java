// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.actions.ExtensionFileFilter;

public class WMSLayerImporter extends FileImporter{

    public WMSLayerImporter() {
        super(new ExtensionFileFilter("wms", "wms", tr("WMS Files (*.wms)")));
    }

}
