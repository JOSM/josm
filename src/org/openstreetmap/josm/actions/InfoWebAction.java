//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.tools.Shortcut;

public class InfoWebAction extends AbstractInfoAction {

    public InfoWebAction() {
        super(tr("Advanced info (web)"), "about",
                tr("Display object information about OSM nodes, ways, or relations in web browser."),
                Shortcut.registerShortcut("core:infoweb",
                        tr("Advanced info (web)"), KeyEvent.VK_I, Shortcut.GROUP_HOTKEY, Shortcut.SHIFT_DEFAULT), false);
        putValue("help", ht("/Action/InfoAboutElementsWeb"));
        putValue("toolbar", "action/infoweb");
        Main.toolbar.register(this);
    }

    @Override
    protected  String createInfoUrl(Object infoObject) {
        OsmPrimitive primitive = (OsmPrimitive)infoObject;
        return getBaseBrowseUrl() + "/" + OsmPrimitiveType.from(primitive).getAPIName() + "/" + primitive.getId();
    }
}
