// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A popup menu designed for text components. It displays the following actions:
 * <ul>
 * <li>Undo</li>
 * <li>Cut</li>
 * <li>Copy</li>
 * <li>Paste</li>
 * <li>Delete</li>
 * <li>Select All</li>
 * </ul>
 * @since 5886
 */
public class TextContextualPopupMenu extends JPopupMenu {

    protected JTextComponent component = null;
    protected UndoAction undoAction = null;

    protected final PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
        @Override public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals("editable")) {
                removeAll();
                addMenuEntries();
            }
        }
    };

    /**
     * Creates a new {@link TextContextualPopupMenu}.
     */
    protected TextContextualPopupMenu() {
    }

    /**
     * Attaches this contextual menu to the given text component.
     * A menu can only be attached to a single component.
     * @param component The text component that will display the menu and handle its actions.
     * @return {@code this}
     * @see #detach()
     */
    protected TextContextualPopupMenu attach(JTextComponent component) {
        if (component != null && !isAttached()) {
            this.component = component;
            if (component.isEditable()) {
                undoAction = new UndoAction();
                component.getDocument().addUndoableEditListener(undoAction);
            }
            addMenuEntries();
            component.addPropertyChangeListener("editable", propertyChangeListener);
        }
        return this;
    }

    private void addMenuEntries() {
        if (component.isEditable()) {
            add(new JMenuItem(undoAction));
            addSeparator();
            addMenuEntry(component, tr("Cut"), DefaultEditorKit.cutAction, null);
        }
        addMenuEntry(component, tr("Copy"), DefaultEditorKit.copyAction, "copy");
        if (component.isEditable()) {
            addMenuEntry(component, tr("Paste"), DefaultEditorKit.pasteAction, "paste");
            addMenuEntry(component, tr("Delete"), DefaultEditorKit.deleteNextCharAction, null);
        }
        addSeparator();
        addMenuEntry(component, tr("Select All"), DefaultEditorKit.selectAllAction, null);
    }

    /**
     * Detaches this contextual menu from its text component.
     * @return {@code this}
     * @see #attach(JTextComponent)
     */
    protected TextContextualPopupMenu detach() {
        if (isAttached()) {
            component.removePropertyChangeListener("editable", propertyChangeListener);
            removeAll();
            if (undoAction != null) {
                component.getDocument().removeUndoableEditListener(undoAction);
                undoAction = null;
            }
            this.component = null;
        }
        return this;
    }

    /**
     * Creates a new {@link TextContextualPopupMenu} and enables it for the given text component.
     * @param component The component that will display the menu and handle its actions.
     * @return The {@link PopupMenuLauncher} responsible of displaying the popup menu.
     *         Call {@link #disableMenuFor} with this object if you want to disable the menu later.
     * @see #disableMenuFor(JTextComponent, PopupMenuLauncher)
     */
    public static PopupMenuLauncher enableMenuFor(JTextComponent component) {
        PopupMenuLauncher launcher = new PopupMenuLauncher(new TextContextualPopupMenu().attach(component), true);
        component.addMouseListener(launcher);
        return launcher;
    }

    /**
     * Disables the {@link TextContextualPopupMenu} attached to the given popup menu launcher and text component.
     * @param component The component that currently displays the menu and handles its actions.
     * @param launcher The {@link PopupMenuLauncher} obtained via {@link #enableMenuFor}.
     * @see #enableMenuFor(JTextComponent)
     */
    public static void disableMenuFor(JTextComponent component, PopupMenuLauncher launcher) {
        if (launcher.getMenu() instanceof TextContextualPopupMenu) {
            ((TextContextualPopupMenu) launcher.getMenu()).detach();
            component.removeMouseListener(launcher);
        }
    }

    /**
     * Determines if this popup is currently attached to a component.
     * @return {@code true} if this popup is currently attached to a component, {@code false} otherwise.
     */
    public final boolean isAttached() {
        return component != null;
    }

    protected void addMenuEntry(JTextComponent component,  String label, String actionName, String iconName) {
        Action action = component.getActionMap().get(actionName);
        if (action != null) {
            JMenuItem mi = new JMenuItem(action);
            mi.setText(label);
            if (iconName != null && Main.pref.getBoolean("text.popupmenu.useicons", true)) {
                ImageIcon icon = new ImageProvider(iconName).setWidth(16).get();
                if (icon != null) {
                    mi.setIcon(icon);
                }
            }
            add(mi);
        }
    }

    protected static class UndoAction extends AbstractAction implements UndoableEditListener {

        private final UndoManager undoManager = new UndoManager();

        public UndoAction() {
            super(tr("Undo"));
            setEnabled(false);
        }

        @Override
        public void undoableEditHappened(UndoableEditEvent e) {
            undoManager.addEdit(e.getEdit());
            setEnabled(undoManager.canUndo());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                undoManager.undo();
            } catch (CannotUndoException ex) {
                // Ignored
            } finally {
                setEnabled(undoManager.canUndo());
            }
        }
    }
}
