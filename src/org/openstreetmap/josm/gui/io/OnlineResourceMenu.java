// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.openstreetmap.josm.actions.ToggleAction;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Show list of {@link OnlineResource} to set online/offline via {@link NetworkManager}.
 * @since 16823
 */
public class OnlineResourceMenu extends JMenu {

    /**
     * Constructs a new {@link OnlineResourceMenu}
     */
    public OnlineResourceMenu() {
        super(tr("Work Offline..."));
        setToolTipText(tr("Block network access to resources"));
        setIcon(ImageProvider.get("offline", ImageProvider.ImageSizes.MENU));
        addMenuListener(new ToggleMenuListener());

        for (OnlineResource onlineResource : OnlineResource.values()) {
            ToggleOnlineResourceAction action = new ToggleOnlineResourceAction(onlineResource);
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(action);
            action.addButtonModel(item.getModel());
            add(item);
        }
    }

    private static class ToggleOnlineResourceAction extends ToggleAction {
        private final OnlineResource onlineResource;

        ToggleOnlineResourceAction(OnlineResource onlineResource) {
            super(onlineResource.getLocName(), onlineResource.getOfflineIcon(), null, null, false);
            this.onlineResource = onlineResource;
            setToolbarId("menu:offline:" + onlineResource.name());
            updateSelectedState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (isOffline()) {
                NetworkManager.setOnline(onlineResource);
            } else {
                NetworkManager.setOffline(onlineResource);
            }
            updateSelectedState();
        }

        private void updateSelectedState() {
            setEnabled(onlineResource == OnlineResource.ALL || !NetworkManager.isOffline(OnlineResource.ALL));
            setSelected(isOffline());
            notifySelectedState();
        }

        private boolean isOffline() {
            return NetworkManager.isOffline(onlineResource);
        }

        @Override
        protected boolean listenToSelectionChange() {
            return false;
        }
    }

    private class ToggleMenuListener implements MenuListener {
        @Override
        public void menuSelected(MenuEvent e) {
            for (Component component : getMenuComponents()) {
                JMenuItem menuItem = (JMenuItem) component;
                ToggleOnlineResourceAction action = (ToggleOnlineResourceAction) menuItem.getAction();
                action.updateSelectedState();
            }
        }

        @Override
        public void menuDeselected(MenuEvent e) {
            // Do nothing
        }

        @Override
        public void menuCanceled(MenuEvent e) {
            // Do nothing
        }
    }
}
