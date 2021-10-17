// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;


import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ComboBoxEditor;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.border.Border;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Base class for all comboboxes in JOSM.
 * <p>
 * This combobox will show as many rows as possible without covering the combox itself. It makes
 * sure the list will never go outside the screen (see #7917). You may limit the number of rows
 * shown with the configuration: {@code gui.combobox.maximum-row-count}.
 * <p>
 * This combobox uses a {@link JosmTextField} for its editor component.
 *
 * @param <E> the type of the elements of this combo box
 * @since 5429 (creation)
 * @since 7015 (generics for Java 7)
 */
public class JosmComboBox<E> extends JComboBox<E> implements PopupMenuListener, PropertyChangeListener {
    /**
     * Limits the number of rows that this combobox will show.
     */
    public static final String PROP_MAXIMUM_ROW_COUNT = "gui.combobox.maximum-row-count";

    /** the configured maximum row count or null */
    private Integer configMaximumRowCount;

    /**
     * The preferred height of the combobox when closed.  Use if the items in the list dropdown are
     * taller than the item in the editor, as in some comboboxes in the preset dialog.  -1 to use
     * the height of the tallest item in the list.
     */
    private int preferredHeight = -1;

    /** greyed text to display in the editor when the selected value is empty */
    private String hint;

    /**
     * Creates a {@code JosmComboBox} with a {@link JosmComboBoxModel} data model.
     * The default data model is an empty list of objects.
     * Use <code>addItem</code> to add items. By default the first item
     * in the data model becomes selected.
     */
    public JosmComboBox() {
        super(new JosmComboBoxModel<E>());
        init();
    }

    /**
     * Creates a {@code JosmComboBox} with a {@link JosmComboBoxModel} data model and
     * the specified prototype display value.
     * The default data model is an empty list of objects.
     * Use <code>addItem</code> to add items. By default the first item
     * in the data model becomes selected.
     *
     * @param prototypeDisplayValue the <code>Object</code> used to compute
     *      the maximum number of elements to be displayed at once before
     *      displaying a scroll bar
     *
     * @since 5450
     * @deprecated use {@link #setPrototypeDisplayValue} instead.
     */
    @Deprecated
    public JosmComboBox(E prototypeDisplayValue) {
        super(new JosmComboBoxModel<E>());
        setPrototypeDisplayValue(prototypeDisplayValue);
        init();
    }

    /**
     * Creates a {@code JosmComboBox} that takes it items from an existing {@link JosmComboBoxModel}
     * data model.
     *
     * @param aModel the model that provides the displayed list of items
     */
    public JosmComboBox(JosmComboBoxModel<E> aModel) {
        super(aModel);
        init();
    }

    /**
     * Creates a {@code JosmComboBox} that takes it items from an existing {@link JosmComboBoxModel}
     * data model and sets the specified prototype display value.
     *
     * @param aModel the model that provides the displayed list of items
     * @param prototypeDisplayValue use this item to size the combobox (may be null)
     * @deprecated use {@link #setPrototypeDisplayValue} instead.
     */
    @Deprecated
    public JosmComboBox(JosmComboBoxModel<E> aModel, E prototypeDisplayValue) {
        super(aModel);
        setPrototypeDisplayValue(prototypeDisplayValue);
        init();
    }

    /**
     * Creates a {@code JosmComboBox} that contains the elements
     * in the specified array. By default the first item in the array
     * (and therefore the data model) becomes selected.
     *
     * @param items  an array of objects to insert into the combo box
     */
    public JosmComboBox(E[] items) {
        super(new JosmComboBoxModel<E>());
        init();
        for (E elem : items) {
            getModel().addElement(elem);
        }
    }

    private void init() {
        configMaximumRowCount = Config.getPref().getInt(PROP_MAXIMUM_ROW_COUNT, 9999);
        setEditor(new JosmComboBoxEditor());
        // listen when the popup shows up so we can maximize its height
        addPopupMenuListener(this);
    }

    /**
     * Returns the {@link JosmComboBoxModel} currently used.
     *
     * @return the model or null
     */
    @Override
    public JosmComboBoxModel<E> getModel() {
        return (JosmComboBoxModel<E>) dataModel;
    }

    @Override
    public void setEditor(ComboBoxEditor newEditor) {
        if (editor != null) {
            editor.getEditorComponent().removePropertyChangeListener(this);
        }
        super.setEditor(newEditor);
        if (editor != null) {
            // listen to orientation changes in the editor
            editor.getEditorComponent().addPropertyChangeListener(this);
        }
    }

    /**
     * Returns the editor component
     * @return the editor component
     * @see ComboBoxEditor#getEditorComponent()
     * @since 9484
     */
    public JosmTextField getEditorComponent() {
        return (JosmTextField) (editor == null ? null : editor.getEditorComponent());
    }

    /**
     * Returns the text in the combobox editor.
     * @return the text
     * @see JTextComponent#getText
     * @since 18173
     */
    public String getText() {
        JosmTextField tf = getEditorComponent();
        return tf == null ? null : tf.getText();
    }

    /**
     * Sets the text in the combobox editor.
     * @param value the text to set
     * @see JTextComponent#setText
     * @since 18173
     */
    public void setText(String value) {
        JosmTextField tf = getEditorComponent();
        if (tf != null)
            tf.setText(value);
    }

    /**
     * Selects an item and/or sets text
     *
     * Selects the item whose {@code toString()} equals {@code text}. If an item could not be found,
     * selects nothing and sets the text anyway.
     *
     * @param text the text to select and set
     * @return the item or null
     */
    public E setSelectedItemText(String text) {
        E item = getModel().find(text);
        setSelectedItem(item);
        if (text == null || !text.equals(getText()))
            setText(text);
        return item;
    }

    /* Hint handling */

    /**
     * Returns the hint text
     * @return the hint text
     */
    public String getHint() {
        return hint;
    }

    /**
     * Sets the hint to display when no text has been entered.
     *
     * @param hint the hint to set
     * @return the old hint
     * @since 18221
     */
    public String setHint(String hint) {
        String old = hint;
        this.hint = hint;
        JosmTextField tf = getEditorComponent();
        if (tf != null)
            tf.setHint(hint);
        return old;
    }

    @Override
    public void setComponentOrientation(ComponentOrientation o) {
        if (o.isLeftToRight() != getComponentOrientation().isLeftToRight()) {
            super.setComponentOrientation(o);
            getEditorComponent().setComponentOrientation(o);
            // the button doesn't move over without this
            revalidate();
        }
    }

    /**
     * Return true if the combobox should display the hint text.
     *
     * @return whether to display the hint text
     * @since 18221
     */
    public boolean displayHint() {
        return !isEditable() && hint != null && !hint.isEmpty() && getText().isEmpty(); // && !isFocusOwner();
    }

    /**
     * Overrides the calculated height.  See: {@link #setPreferredHeight(int)}.
     *
     * @since 18221
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        if (preferredHeight != -1)
            d.height = preferredHeight;
        return d;
    }

    /**
     * Sets the preferred height of the combobox editor.
     * <p>
     * A combobox editor is automatically sized to accomodate the widest and the tallest items in
     * the list.  In the Preset dialogs we show more of an item in the list than in the editor, so
     * the editor becomes too big.  With this method we can set the editor height to a fixed value.
     * <p>
     * Set this to -1 to get the default behaviour back.
     *
     * See also: #6157
     *
     * @param height the preferred height or -1
     * @return the old preferred height
     * @see #setPreferredSize
     * @since 18221
     */
    public int setPreferredHeight(int height) {
        int old = preferredHeight;
        preferredHeight = height;
        return old;
    }

    /**
     * Get the dropdown list component
     *
     * @return the list or null
     */
    @SuppressWarnings("rawtypes")
    public JList getList() {
        Object popup = getUI().getAccessibleChild(this, 0);
        if (popup != null && popup instanceof javax.swing.plaf.basic.ComboPopup) {
            return ((javax.swing.plaf.basic.ComboPopup) popup).getList();
        }
        return null;
    }

    /**
     * Draw the hint text for read-only comboboxes.
     * <p>
     * The obvious way -- to call {@code setText(hint)} and {@code setForeground(gray)} on the
     * {@code JLabel} returned by the list cell renderer -- unfortunately does not work out well
     * because many UIs change the foreground color or the enabled state of the {@code JLabel} after
     * the list cell renderer has returned ({@code BasicComboBoxUI}).  Other UIs don't honor the
     * label color at all ({@code SynthLabelUI}).
     * <p>
     * We use the same approach as in {@link JosmTextField}. The only problem we face is to get the
     * coordinates of the text inside the combobox.  Fortunately even read-only comboboxes have a
     * (partially configured) editor component, although they don't use it.  We configure that editor
     * just enough to call {@link JTextField#modelToView modelToView} and
     * {@link javax.swing.JComponent#getBaseline getBaseline} on it, thus obtaining the text
     * coordinates.
     *
     * @see javax.swing.plaf.basic.BasicComboBoxUI#paintCurrentValue
     * @see javax.swing.plaf.synth.SynthLabelUI#paint
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        JosmTextField editor = getEditorComponent();
        if (displayHint() && editor != null) {
            if (editor.getSize().width == 0) {
                Dimension dimen = getSize();
                Insets insets = getInsets();
                // a fake configuration not too far from reality
                editor.setSize(dimen.width - insets.left - insets.right,
                               dimen.height - insets.top - insets.bottom);
            }
            editor.drawHint(g);
        }
    }

    /**
     * Empties the internal undo manager, if any.
     * <p>
     * Used in the {@link org.openstreetmap.josm.gui.io.UploadDialog UploadDialog}.
     * @since 14977
     */
    public final void discardAllUndoableEdits() {
        getEditorComponent().discardAllUndoableEdits();
    }

    /**
     * Limits the popup height.
     * <p>
     * Limits the popup height to the available screen space either below or above the combobox,
     * whichever is bigger. To find the maximum number of rows that fit the screen, it does the
     * reverse of the calculation done in
     * {@link javax.swing.plaf.basic.BasicComboPopup#getPopupLocation}.
     *
     * @see javax.swing.plaf.basic.BasicComboBoxUI#getAccessibleChild
     */
    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent ev) {
        // Get the combobox bounds.
        Rectangle bounds = new Rectangle(getLocationOnScreen(), getSize());

        // Get the screen bounds of the screen (of a multi-screen setup) we are on.
        Rectangle screenBounds;
        GraphicsConfiguration gc = getGraphicsConfiguration();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        if (gc != null) {
            Insets screenInsets = toolkit.getScreenInsets(gc);
            screenBounds = gc.getBounds();
            screenBounds.x += screenInsets.left;
            screenBounds.y += screenInsets.top;
            screenBounds.width -= (screenInsets.left + screenInsets.right);
            screenBounds.height -= (screenInsets.top + screenInsets.bottom);
        } else {
            screenBounds = new Rectangle(new Point(), toolkit.getScreenSize());
        }
        int freeAbove = bounds.y - screenBounds.y;
        int freeBelow = (screenBounds.y + screenBounds.height) - (bounds.y + bounds.height);

        try {
            // First try an implementation-dependent method to get the exact number.
            @SuppressWarnings("unchecked")
            JList<E> jList = getList();

            // Calculate the free space available on screen
            Insets insets = jList.getInsets();
            // A small fudge factor that accounts for the displacement of the popup relative to the
            // combobox and the popup shadow.
            int fudge = 4;
            int free = Math.max(freeAbove, freeBelow) - (insets.top + insets.bottom) - fudge;
            if (jList.getParent() instanceof JScrollPane) {
                JScrollPane scroller = (JScrollPane) jList.getParent();
                Border border = scroller.getViewportBorder();
                if (border != null) {
                    insets = border.getBorderInsets(null);
                    free -= insets.top + insets.bottom;
                }
                border = scroller.getBorder();
                if (border != null) {
                    insets = border.getBorderInsets(null);
                    free -= insets.top + insets.bottom;
                }
            }

            // Calculate how many rows fit into the free space.  Rows may have variable heights.
            int rowCount = Math.min(configMaximumRowCount, getItemCount());
            ListCellRenderer<? super E> r = jList.getCellRenderer();  // must take this from list, not combo: flatlaf bug
            int i, h = 0;
            for (i = 0; i < rowCount; ++i) {
                Component c = r.getListCellRendererComponent(jList, getModel().getElementAt(i), i, false, false);
                h += c.getPreferredSize().height;
                if (h >= free)
                    break;
            }
            setMaximumRowCount(i);
            // Logging.debug("free = {0}, h = {1}, i = {2}, bounds = {3}, screenBounds = {4}", free, h, i, bounds, screenBounds);
        } catch (Exception ex) {
            setMaximumRowCount(8); // the default
        }
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        // Who cares?
    }

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {
        // Who cares?
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // follow our editor's orientation
        if ("componentOrientation".equals(evt.getPropertyName())) {
            setComponentOrientation((ComponentOrientation) evt.getNewValue());
        }
    }
}
