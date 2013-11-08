//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.tools.Shortcut;

public class HistoryInfoWebAction extends AbstractInfoAction {

    public HistoryInfoWebAction() {
        super(tr("History (web)"), "about",
                tr("Display history information about OSM ways, nodes, or relations in web browser."),
                Shortcut.registerShortcut("core:historyinfoweb",
                        tr("History (web)"), KeyEvent.VK_H, Shortcut.CTRL_SHIFT),
                true, "action/historyinfoweb", true);
        putValue("help", ht("/Action/ObjectHistoryWeb"));
    }

    @Override
    protected  String createInfoUrl(Object infoObject) {
        OsmPrimitive primitive = (OsmPrimitive) infoObject;
        return getBaseBrowseUrl() + "/" + OsmPrimitiveType.from(primitive).getAPIName() + "/" + primitive.getId() + "/history";
    }
}
