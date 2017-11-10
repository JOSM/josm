// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Optional;

import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.ListCellRenderer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.widgets.SearchTextResultListPanel;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * A dialog that allows you to search for a menu item. The user can input part of the menu item name.
 */
public final class MenuItemSearchDialog extends ExtendedDialog {

    private final Selector selector;
    private static final MenuItemSearchDialog INSTANCE = new MenuItemSearchDialog(MainApplication.getMenu());

    private MenuItemSearchDialog(MainMenu menu) {
        super(Main.parent, tr("Search menu items"), tr("Select"), tr("Cancel"));
        this.selector = new Selector(menu);
        this.selector.setDblClickListener(e -> buttonAction(0, null));
        setContent(selector, false);
        setPreferredSize(new Dimension(600, 300));
    }

    /**
     * Returns the unique instance of {@code MenuItemSearchDialog}.
     *
     * @return the unique instance of {@code MenuItemSearchDialog}.
     */
    public static synchronized MenuItemSearchDialog getInstance() {
        return INSTANCE;
    }

    @Override
    public ExtendedDialog showDialog() {
        selector.init();
        super.showDialog();
        selector.clearSelection();
        return this;
    }

    @Override
    protected void buttonAction(int buttonIndex, ActionEvent evt) {
        super.buttonAction(buttonIndex, evt);
        if (buttonIndex == 0 && selector.getSelectedItem() != null && selector.getSelectedItem().isEnabled()) {
            selector.getSelectedItem().getAction().actionPerformed(evt);
        }
    }

    private static class Selector extends SearchTextResultListPanel<JMenuItem> {

        private final MainMenu menu;

        Selector(MainMenu menu) {
            super();
            this.menu = menu;
            lsResult.setCellRenderer(new CellRenderer());
        }

        public JMenuItem getSelectedItem() {
            final JMenuItem selected = lsResult.getSelectedValue();
            if (selected != null) {
                return selected;
            } else if (!lsResultModel.isEmpty()) {
                return lsResultModel.getElementAt(0);
            } else {
                return null;
            }
        }

        @Override
        protected void filterItems() {
            lsResultModel.setItems(menu.findMenuItems(edSearchText.getText(), true));
        }
    }

    private static class CellRenderer implements ListCellRenderer<JMenuItem> {

        @Override
        public Component getListCellRendererComponent(JList<? extends JMenuItem> list, JMenuItem value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            final JMenuItem item = new JMenuItem(value.getText());
            item.setAction(value.getAction());
            Optional.ofNullable(value.getAction())
                    .filter(JosmAction.class::isInstance)
                    .map(JosmAction.class::cast)
                    .map(JosmAction::getShortcut)
                    .map(Shortcut::getKeyStroke)
                    .ifPresent(item::setAccelerator);
            if (isSelected) {
                item.setBackground(list.getSelectionBackground());
                item.setForeground(list.getSelectionForeground());
            } else {
                item.setBackground(list.getBackground());
                item.setForeground(list.getForeground());
            }
            return item;
        }
    }

    /**
     * The action that opens the menu item search dialog.
     */
    public static class Action extends JosmAction {

        // CHECKSTYLE.OFF: LineLength
        /** Action shortcut (ctrl / space by default), made public in order to be used from {@code GettingStarted} page. */
        public static final Shortcut SHORTCUT = Shortcut.registerShortcut("help:search-items", "Search menu items", KeyEvent.VK_SPACE, Shortcut.CTRL);
        // CHECKSTYLE.ON: LineLength

        /**
         * Constructs a new {@code Action}.
         */
        public Action() {
            super(tr("Search menu items"), "dialogs/search", null,
                    SHORTCUT,
                    true, "dialogs/search-items", false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            MenuItemSearchDialog.getInstance().showDialog();
        }
    }
}
