// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;

/**
 * A popup menu designed for text components. It displays the following actions:
 * <ul>
 * <li>Undo</li>
 * <li>Redo</li>
 * <li>Cut</li>
 * <li>Copy</li>
 * <li>Paste</li>
 * <li>Delete</li>
 * <li>Select All</li>
 * </ul>
 * @since 5886
 */
public class TextContextualPopupMenu extends JPopupMenu {

    private static final String EDITABLE = "editable";

    protected JTextComponent component;
    protected boolean undoRedo;
    protected final UndoAction undoAction = new UndoAction();
    protected final RedoAction redoAction = new RedoAction();
    protected final UndoManager undo = new UndoManager();

    protected final transient UndoableEditListener undoEditListener = e -> {
        undo.addEdit(e.getEdit());
        undoAction.updateUndoState();
        redoAction.updateRedoState();
    };

    protected final transient PropertyChangeListener propertyChangeListener = evt -> {
        if (EDITABLE.equals(evt.getPropertyName())) {
            removeAll();
            addMenuEntries();
        }
    };

    /**
     * Creates a new {@link TextContextualPopupMenu}.
     */
    protected TextContextualPopupMenu() {
        // Restricts visibility
    }

    /**
     * Attaches this contextual menu to the given text component.
     * A menu can only be attached to a single component.
     * @param component The text component that will display the menu and handle its actions.
     * @param undoRedo {@code true} if undo/redo must be supported
     * @return {@code this}
     * @see #detach()
     */
    protected TextContextualPopupMenu attach(JTextComponent component, boolean undoRedo) {
        if (component != null && !isAttached()) {
            this.component = component;
            this.undoRedo = undoRedo;
            if (undoRedo && component.isEditable()) {
                component.getDocument().addUndoableEditListener(undoEditListener);
                if (!GraphicsEnvironment.isHeadless()) {
                    component.getInputMap().put(
                            KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), undoAction);
                    component.getInputMap().put(
                            KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), redoAction);
                }
            }
            addMenuEntries();
            component.addPropertyChangeListener(EDITABLE, propertyChangeListener);
        }
        return this;
    }

    private void addMenuEntries() {
        if (component.isEditable()) {
            if (undoRedo) {
                add(new JMenuItem(undoAction));
                add(new JMenuItem(redoAction));
                addSeparator();
            }
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
     * @see #attach(JTextComponent, boolean)
     */
    protected TextContextualPopupMenu detach() {
        if (isAttached()) {
            component.removePropertyChangeListener(EDITABLE, propertyChangeListener);
            removeAll();
            if (undoRedo) {
                component.getDocument().removeUndoableEditListener(undoEditListener);
            }
            component = null;
        }
        return this;
    }

    /**
     * Creates a new {@link TextContextualPopupMenu} and enables it for the given text component.
     * @param component The component that will display the menu and handle its actions.
     * @param undoRedo Enables or not Undo/Redo feature. Not recommended for table cell editors, unless each cell provides its own editor
     * @return The {@link PopupMenuLauncher} responsible of displaying the popup menu.
     *         Call {@link #disableMenuFor} with this object if you want to disable the menu later.
     * @see #disableMenuFor
     */
    public static PopupMenuLauncher enableMenuFor(JTextComponent component, boolean undoRedo) {
        PopupMenuLauncher launcher = new PopupMenuLauncher(new TextContextualPopupMenu().attach(component, undoRedo), true);
        component.addMouseListener(launcher);
        return launcher;
    }

    /**
     * Disables the {@link TextContextualPopupMenu} attached to the given popup menu launcher and text component.
     * @param component The component that currently displays the menu and handles its actions.
     * @param launcher The {@link PopupMenuLauncher} obtained via {@link #enableMenuFor}.
     * @see #enableMenuFor
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

    protected void addMenuEntry(JTextComponent component, String label, String actionName, String iconName) {
        Action action = component.getActionMap().get(actionName);
        if (action != null) {
            JMenuItem mi = new JMenuItem(action);
            mi.setText(label);
            if (iconName != null && Main.pref.getBoolean("text.popupmenu.useicons", true)) {
                ImageIcon icon = ImageProvider.get(iconName, ImageProvider.ImageSizes.SMALLICON);
                if (icon != null) {
                    mi.setIcon(icon);
                }
            }
            add(mi);
        }
    }

    protected class UndoAction extends AbstractAction {

        /**
         * Constructs a new {@code UndoAction}.
         */
        public UndoAction() {
            super(tr("Undo"));
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                undo.undo();
            } catch (CannotUndoException ex) {
                Logging.trace(ex);
            } finally {
                updateUndoState();
                redoAction.updateRedoState();
            }
        }

        public void updateUndoState() {
            if (undo.canUndo()) {
                setEnabled(true);
                putValue(Action.NAME, undo.getUndoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, tr("Undo"));
            }
        }
    }

    protected class RedoAction extends AbstractAction {

        /**
         * Constructs a new {@code RedoAction}.
         */
        public RedoAction() {
            super(tr("Redo"));
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                undo.redo();
            } catch (CannotRedoException ex) {
                Logging.trace(ex);
            } finally {
                updateRedoState();
                undoAction.updateUndoState();
            }
        }

        public void updateRedoState() {
            if (undo.canRedo()) {
                setEnabled(true);
                putValue(Action.NAME, undo.getRedoPresentationName());
            } else {
                setEnabled(false);
                putValue(Action.NAME, tr("Redo"));
            }
        }
    }
}
