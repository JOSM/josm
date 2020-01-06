// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.util.Arrays;

import javax.swing.JMenu;

import org.openstreetmap.josm.actions.ExpertToggleAction;
import org.openstreetmap.josm.actions.ExpertToggleAction.ExpertModeChangeListener;

/**
 * Window menu, holding entries for changeset manager, toggle dialogs.
 * @since 15649
 */
public class WindowMenu extends JMenu implements ContainerListener, ExpertModeChangeListener {

    /**
     * The possible item groups of the Windows menu.
     * @see MainMenu#addWithCheckbox
     */
    public enum WINDOW_MENU_GROUP {
        /** Entries always displayed, at the top */
        ALWAYS,
        /** Entries displayed only for visible toggle dialogs */
        TOGGLE_DIALOG,
        /** Volatile entries displayed at the end */
        VOLATILE
    }

    /**
     * Constructs a new {@code WindowMenu}
     */
    public WindowMenu() {
        /* I18N: mnemonic: W */
        super(trc("menu", "Windows"));
        ExpertToggleAction.addExpertModeChangeListener(this);
        getPopupMenu().addContainerListener(this);
        setEnabledState();
    }

    @Override
    public void expertChanged(boolean isExpert) {
        setEnabledState();
    }

    @Override
    public void componentAdded(ContainerEvent e) {
        setEnabledState();
    }

    @Override
    public void componentRemoved(ContainerEvent e) {
        setEnabledState();
    }

    protected void setEnabledState() {
        setEnabled(ExpertToggleAction.isExpert()
                || Arrays.stream(getMenuComponents()).anyMatch(c -> !ExpertToggleAction.hasVisibilitySwitcher(c)));
    }
}
