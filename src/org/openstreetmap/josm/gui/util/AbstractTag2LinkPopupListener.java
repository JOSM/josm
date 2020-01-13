// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.openstreetmap.josm.tools.Tag2Link;

/**
 * A popup listener which adds web links based on tags of OSM primitives.
 *
 * @since 15673
 */
public abstract class AbstractTag2LinkPopupListener implements PopupMenuListener {

    private final List<Component> itemList;

    protected AbstractTag2LinkPopupListener() {
        this.itemList = new ArrayList<>();
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        JPopupMenu popup = (JPopupMenu) e.getSource();
        itemList.forEach(popup::remove);
        itemList.clear();
    }

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {
    }

    protected void addLinks(JPopupMenu popup, String key, String value) {
        Tag2Link.getLinksForTag(key, value, (name, url) -> {
            if (itemList.isEmpty()) {
                itemList.add(popup.add(new JPopupMenu.Separator()));
            }

            itemList.add(popup.add(new OpenBrowserAction(name, url)));
        });
    }
}
