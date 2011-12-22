//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.tools.Shortcut;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

public class HistoryInfoWebAction extends AbstractInfoAction {

    public HistoryInfoWebAction() {
        super(tr("History (web)"), "about",
                tr("Display history information about OSM ways, nodes, or relations in web browser."),
                Shortcut.registerShortcut("core:historyinfoweb",
                        tr("History (web)"), KeyEvent.VK_H, Shortcut.GROUP_HOTKEY, Shortcut.SHIFT_DEFAULT), false);
        putValue("help", ht("/Action/ObjectHistoryWeb"));
        putValue("toolbar", "action/historyinfoweb");
        Main.toolbar.register(this);
    }

    @Override
    protected  String createInfoUrl(Object infoObject) {
        OsmPrimitive primitive = (OsmPrimitive) infoObject;
        return getBaseBrowseUrl() + "/" + OsmPrimitiveType.from(primitive).getAPIName() + "/" + primitive.getId() + "/history";
    }
}
