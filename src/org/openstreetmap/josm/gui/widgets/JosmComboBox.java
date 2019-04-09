// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.accessibility.Accessible;
import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.gui.util.GuiHelper;

/**
 * Class overriding each {@link JComboBox} in JOSM to control consistently the number of displayed items at once.<br>
 * This is needed because of the default Java behaviour that may display the top-down list off the screen (see #7917).
 * @param <E> the type of the elements of this combo box
 *
 * @since 5429 (creation)
 * @since 7015 (generics for Java 7)
 */
public class JosmComboBox<E> extends JComboBox<E> {

    private final ContextMenuHandler handler = new ContextMenuHandler();

    /**
     * Creates a <code>JosmComboBox</code> with a default data model.
     * The default data model is an empty list of objects.
     * Use <code>addItem</code> to add items. By default the first item
     * in the data model becomes selected.
     *
     * @see DefaultComboBoxModel
     */
    public JosmComboBox() {
        init(null);
    }

    /**
     * Creates a <code>JosmComboBox</code> with a default data model and
     * the specified prototype display value.
     * The default data model is an empty list of objects.
     * Use <code>addItem</code> to add items. By default the first item
     * in the data model becomes selected.
     *
     * @param prototypeDisplayValue the <code>Object</code> used to compute
     *      the maximum number of elements to be displayed at once before
     *      displaying a scroll bar
     *
     * @see DefaultComboBoxModel
     * @since 5450
     */
    public JosmComboBox(E prototypeDisplayValue) {
        init(prototypeDisplayValue);
    }

    /**
     * Creates a <code>JosmComboBox</code> that takes its items from an
     * existing <code>ComboBoxModel</code>. Since the
     * <code>ComboBoxModel</code> is provided, a combo box created using
     * this constructor does not create a default combo box model and
     * may impact how the insert, remove and add methods behave.
     *
     * @param aModel the <code>ComboBoxModel</code> that provides the
     *      displayed list of items
     * @see DefaultComboBoxModel
     */
    public JosmComboBox(ComboBoxModel<E> aModel) {
        super(aModel);
        List<E> list = new ArrayList<>(aModel.getSize());
        for (int i = 0; i < aModel.getSize(); i++) {
            list.add(aModel.getElementAt(i));
        }
        init(findPrototypeDisplayValue(list));
    }

    /**
     * Creates a <code>JosmComboBox</code> that contains the elements
     * in the specified array. By default the first item in the array
     * (and therefore the data model) becomes selected.
     *
     * @param items  an array of objects to insert into the combo box
     * @see DefaultComboBoxModel
     */
    public JosmComboBox(E[] items) {
        super(items);
        init(findPrototypeDisplayValue(Arrays.asList(items)));
    }

    /**
     * Returns the editor component
     * @return the editor component
     * @see ComboBoxEditor#getEditorComponent()
     * @since 9484
     */
    public JTextField getEditorComponent() {
        return (JTextField) getEditor().getEditorComponent();
    }

    /**
     * Finds the prototype display value to use among the given possible candidates.
     * @param possibleValues The possible candidates that will be iterated.
     * @return The value that needs the largest display height on screen.
     * @since 5558
     */
    protected final E findPrototypeDisplayValue(Collection<E> possibleValues) {
        E result = null;
        int maxHeight = -1;
        if (possibleValues != null) {
            // Remind old prototype to restore it later
            E oldPrototype = getPrototypeDisplayValue();
            // Get internal JList to directly call the renderer
            @SuppressWarnings("rawtypes")
            JList list = getList();
            try {
                // Index to give to renderer
                int i = 0;
                for (E value : possibleValues) {
                    if (value != null) {
                        // With a "classic" renderer, we could call setPrototypeDisplayValue(value) + getPreferredSize()
                        // but not with TaggingPreset custom renderer that return a dummy height if index is equal to -1
                        // So we explicitly call the renderer by simulating a correct index for the current value
                        @SuppressWarnings("unchecked")
                        Component c = getRenderer().getListCellRendererComponent(list, value, i, true, true);
                        if (c != null) {
                            // Get the real preferred size for the current value
                            Dimension dim = c.getPreferredSize();
                            if (dim.height > maxHeight) {
                                // Larger ? This is our new prototype
                                maxHeight = dim.height;
                                result = value;
                            }
                        }
                    }
                    i++;
                }
            } finally {
                // Restore original prototype
                setPrototypeDisplayValue(oldPrototype);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    protected final JList<Object> getList() {
        for (int i = 0; i < getUI().getAccessibleChildrenCount(this); i++) {
            Accessible child = getUI().getAccessibleChild(this, i);
            if (child instanceof ComboPopup) {
                return ((ComboPopup) child).getList();
            }
        }
        return null;
    }

    protected final void init(E prototype) {
        init(prototype, true);
    }

    protected final void init(E prototype, boolean registerPropertyChangeListener) {
        if (prototype != null) {
            setPrototypeDisplayValue(prototype);
            int screenHeight = GuiHelper.getScreenSize().height;
            // Compute maximum number of visible items based on the preferred size of the combo box.
            // This assumes that items have the same height as the combo box, which is not granted by the look and feel
            int maxsize = (screenHeight/getPreferredSize().height) / 2;
            // If possible, adjust the maximum number of items with the real height of items
            // It is not granted this works on every platform (tested OK on Windows)
            JList<Object> list = getList();
            if (list != null) {
                if (!prototype.equals(list.getPrototypeCellValue())) {
                    list.setPrototypeCellValue(prototype);
                }
                int height = list.getFixedCellHeight();
                if (height > 0) {
                    maxsize = (screenHeight/height) / 2;
                }
            }
            setMaximumRowCount(Math.max(getMaximumRowCount(), maxsize));
        }
        // Handle text contextual menus for editable comboboxes
        if (registerPropertyChangeListener) {
            addPropertyChangeListener("editable", handler);
            addPropertyChangeListener("editor", handler);
        }
    }

    protected class ContextMenuHandler extends MouseAdapter implements PropertyChangeListener {

        private JTextComponent component;
        private PopupMenuLauncher launcher;

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if ("editable".equals(evt.getPropertyName())) {
                if (evt.getNewValue().equals(Boolean.TRUE)) {
                    enableMenu();
                } else {
                    disableMenu();
                }
            } else if ("editor".equals(evt.getPropertyName())) {
                disableMenu();
                if (isEditable()) {
                    enableMenu();
                }
            }
        }

        private void enableMenu() {
            if (launcher == null && editor != null) {
                Component editorComponent = editor.getEditorComponent();
                if (editorComponent instanceof JTextComponent) {
                    component = (JTextComponent) editorComponent;
                    component.addMouseListener(this);
                    launcher = TextContextualPopupMenu.enableMenuFor(component, true);
                }
            }
        }

        private void disableMenu() {
            if (launcher != null) {
                TextContextualPopupMenu.disableMenuFor(component, launcher);
                launcher = null;
                component.removeMouseListener(this);
                component = null;
            }
        }

        private void discardAllUndoableEdits() {
            if (launcher != null) {
                launcher.discardAllUndoableEdits();
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            processEvent(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            processEvent(e);
        }

        private void processEvent(MouseEvent e) {
            if (launcher != null && !e.isPopupTrigger() && launcher.getMenu().isShowing()) {
                launcher.getMenu().setVisible(false);
            }
        }
    }

    /**
     * Reinitializes this {@link JosmComboBox} to the specified values. This may be needed if a custom renderer is used.
     * @param values The values displayed in the combo box.
     * @since 5558
     */
    public final void reinitialize(Collection<E> values) {
        init(findPrototypeDisplayValue(values), false);
        discardAllUndoableEdits();
    }

    /**
     * Empties the internal undo manager, if any.
     * @since 14977
     */
    public final void discardAllUndoableEdits() {
        handler.discardAllUndoableEdits();
    }
}
