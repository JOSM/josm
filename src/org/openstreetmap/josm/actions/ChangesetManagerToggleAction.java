// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetCacheManager;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This action toggles the visibility of the {@link ChangesetCacheManager} dialog.
 * @since 2685
 */
public class ChangesetManagerToggleAction extends ToggleAction {
    private final transient WindowListener changesetCacheManagerClosedHandler;

    /**
     * Constructs a new {@code ChangesetManagerToggleAction}.
     */
    public ChangesetManagerToggleAction() {
        super(tr("Changeset Manager"),
                new ImageProvider("dialogs/changeset/changesetmanager").setOptional(true),
                tr("Toggle visibility of Changeset Manager window"),
                Shortcut.registerShortcut("menu:windows:changesetdialog",
                        tr("Toggle visibility of Changeset Manager window"), KeyEvent.VK_C, Shortcut.ALT_CTRL),
                true /* register shortcut */, "dialogs/changeset/changesetmanager", false);
        notifySelectedState();
        changesetCacheManagerClosedHandler = new ChangesetCacheManagerClosedHandler();
        setHelpId(ht("/Dialog/ChangesetManager"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        toggleSelectedState(e);
        notifySelectedState();
        if (isSelected()) {
            ChangesetCacheManager.getInstance().addWindowListener(changesetCacheManagerClosedHandler);
            ChangesetCacheManager.getInstance().setVisible(true);
        } else {
            ChangesetCacheManager.destroyInstance();
        }
    }

    private class ChangesetCacheManagerClosedHandler extends WindowAdapter {
        @Override
        public void windowClosed(WindowEvent e) {
            setSelected(false);
            notifySelectedState();
        }
    }
}
