package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.actions.ExtensionFileFilter;

public class WMSLayerExporter extends FileExporter{

    public WMSLayerExporter() {
        super(new ExtensionFileFilter("wms", "wms", tr("WMS Files (*.wms)")));
    }
}