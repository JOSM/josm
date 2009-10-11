//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.tools.Shortcut;

public class HistoryInfoAction extends AbstractInfoAction {

    public HistoryInfoAction() {
        super(tr("Object history"), "about",
                tr("Display history information about OSM ways, nodes, or relations."),
                Shortcut.registerShortcut("core:history",
                        tr("Object history"), KeyEvent.VK_H, Shortcut.GROUP_HOTKEY), true);
    }

    @Override
    protected  String createInfoUrl(Object infoObject) {
        OsmPrimitive primitive = (OsmPrimitive)infoObject;
        return getBaseBrowseUrl() + "/" + OsmPrimitiveType.from(primitive).getAPIName() + "/" + primitive.getId() + "/history";
    }
}
