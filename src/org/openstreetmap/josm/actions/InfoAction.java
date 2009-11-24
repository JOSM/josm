//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

import java.awt.event.KeyEvent;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.tools.Shortcut;

public class InfoAction extends AbstractInfoAction {

    public InfoAction() {
        super(tr("Info about Element"), "about",
                tr("Display object information about OSM nodes, ways, or relations."),
                Shortcut.registerShortcut("core:information",
                        tr("Info about Element"), KeyEvent.VK_I, Shortcut.GROUP_HOTKEY), true);
        putValue("help", ht("/Action/Info"));
    }

    @Override
    protected  String createInfoUrl(Object infoObject) {
        OsmPrimitive primitive = (OsmPrimitive)infoObject;
        return getBaseBrowseUrl() + "/" + OsmPrimitiveType.from(primitive).getAPIName() + "/" + primitive.getId();
    }
}
