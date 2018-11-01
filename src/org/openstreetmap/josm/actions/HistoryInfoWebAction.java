// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;

import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Display history information about OSM ways, nodes, or relations in web browser.
 * @since 4408
 */
public class HistoryInfoWebAction extends AbstractInfoAction {

    /**
     * Constructs a new {@code HistoryInfoWebAction}.
     */
    public HistoryInfoWebAction() {
        super(tr("History (web)"), "dialogs/history",
                tr("Display history information about OSM ways, nodes, or relations in web browser."),
                Shortcut.registerShortcut("core:historyinfoweb",
                        tr("History (web)"), KeyEvent.VK_H, Shortcut.CTRL_SHIFT),
                true, "action/historyinfoweb", true);
        setHelpId(ht("/Action/ObjectHistoryWeb"));
    }

    @Override
    protected String createInfoUrl(Object infoObject) {
        if (infoObject instanceof IPrimitive) {
            IPrimitive primitive = (IPrimitive) infoObject;
            return Config.getUrls().getBaseBrowseUrl()
                    + '/' + OsmPrimitiveType.from(primitive).getAPIName() + '/' + primitive.getOsmId() + "/history";
        } else {
            return null;
        }
    }
}
