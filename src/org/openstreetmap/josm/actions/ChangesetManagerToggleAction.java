// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonModel;

import org.openstreetmap.josm.gui.dialogs.changeset.ChangesetCacheManager;
import org.openstreetmap.josm.tools.Shortcut;
/**
 * This action toggles the visibility of the {@see ChangesetCacheManager} dialog.
 *
 */
public class ChangesetManagerToggleAction extends JosmAction {
    private final List<ButtonModel> buttonModels = new ArrayList<ButtonModel>();
    //FIXME: replace with property Action.SELECTED_KEY when migrating to
    // Java 6
    private boolean selected;
    private WindowListener changesetCacheManagerClosedHandler;

    public ChangesetManagerToggleAction() {
        super(
                tr("Changeset Manager"),
                "dialogs/changeset/changesetmanager",
                tr("Toggle visibility of Changeset Manager window"),
                Shortcut.registerShortcut(
                        "menu:view:changesetdialog",
                        tr("Toggle visibility of Changeset Manager window"),
                        KeyEvent.VK_C,
                        Shortcut.GROUPS_ALT2 + Shortcut.GROUP_HOTKEY
                ),
                true /* register shortcut */
        );
        notifySelectedState();
        changesetCacheManagerClosedHandler = new ChangesetCacheManagerClosedHandler();
        putValue("help", ht("/Action/ToggleChangesetManager"));
    }

    public void addButtonModel(ButtonModel model) {
        if (model != null && !buttonModels.contains(model)) {
            buttonModels.add(model);
        }
    }

    public void removeButtonModel(ButtonModel model) {
        if (model != null && buttonModels.contains(model)) {
            buttonModels.remove(model);
        }
    }

    protected void notifySelectedState() {
        for (ButtonModel model: buttonModels) {
            if (model.isSelected() != selected) {
                model.setSelected(selected);
            }
        }
    }

    protected void toggleSelectedState() {
        selected = !selected;
        notifySelectedState();
        if (selected) {
            ChangesetCacheManager.getInstance().addWindowListener(changesetCacheManagerClosedHandler);
            ChangesetCacheManager.getInstance().setVisible(true);
        } else {
            ChangesetCacheManager.getInstance().removeWindowListener(changesetCacheManagerClosedHandler);
            ChangesetCacheManager.destroyInstance();
        }
    }

    public void actionPerformed(ActionEvent e) {
        toggleSelectedState();
    }

    private class ChangesetCacheManagerClosedHandler extends WindowAdapter {
        @Override
        public void windowClosed(WindowEvent e) {
            selected = false;
            notifySelectedState();
            ChangesetCacheManager.getInstance().removeWindowListener(changesetCacheManagerClosedHandler);
        }
    }
}
