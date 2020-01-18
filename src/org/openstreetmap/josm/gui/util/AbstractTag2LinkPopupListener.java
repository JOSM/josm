// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.openstreetmap.josm.actions.OpenBrowserAction;
import org.openstreetmap.josm.tools.Tag2Link;

/**
 * A popup listener which adds web links based on tags of OSM primitives.
 *
 * @since 15673
 */
public abstract class AbstractTag2LinkPopupListener implements PopupMenuListener {

    private final Map<String, OpenBrowserAction> browserActions = new HashMap<>();
    private final Collection<Component> itemList = new ArrayList<>();

    protected AbstractTag2LinkPopupListener() {
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        JPopupMenu popup = (JPopupMenu) e.getSource();
        browserActions.clear();
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

            if (browserActions.containsKey(name)) {
                browserActions.get(name).addUrl(url);
            } else {
                final OpenBrowserAction action = new OpenBrowserAction(name, url);
                browserActions.put(name, action);
                itemList.add(popup.add(action));
            }
        });
    }
}
