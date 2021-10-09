// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.tagging.ac.AutoCompletionItem;
import org.openstreetmap.josm.data.tagging.ac.AutoCompletionPriority;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.mappaint.mapcss.CSSColors;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompComboBoxEditor;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompComboBoxModel;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompTextField;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItem;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemGuiSupport;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.gui.widgets.OrientationAction;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.GBC;

/**
 * Combobox type.
 */
public class Combo extends ComboMultiSelect {

    /**
     * Whether the combo box is editable, which means that the user can add other values as text.
     * Default is {@code true}. If {@code false} it is readonly, which means that the user can only select an item in the list.
     */
    public boolean editable = true; // NOSONAR
    /** The length of the combo box (number of characters allowed). */
    public int length; // NOSONAR

    protected JosmComboBox<PresetListEntry> combobox;
    protected AutoCompComboBoxModel<PresetListEntry> dropDownModel;
    protected AutoCompComboBoxModel<AutoCompletionItem> autoCompModel;

    class ComponentListener extends ComponentAdapter {
        @Override
        public void componentResized(ComponentEvent e) {
            // Make multi-line JLabels the correct size
            // Only needed if there is any short_description
            JComponent component = (JComponent) e.getSource();
            int width = component.getWidth();
            if (width == 0)
                width = 200;
            Insets insets = component.getInsets();
            width -= insets.left + insets.right + 10;
            ComboMultiSelectListCellRenderer renderer = (ComboMultiSelectListCellRenderer) combobox.getRenderer();
            renderer.setWidth(width);
            combobox.setRenderer(null); // needed to make prop change fire
            combobox.setRenderer(renderer);
        }
    }

    /**
     * Constructs a new {@code Combo}.
     */
    public Combo() {
        delimiter = ',';
    }

    private void addEntry(PresetListEntry entry) {
        if (!seenValues.containsKey(entry.value)) {
            dropDownModel.addElement(entry);
            seenValues.put(entry.value, entry);
        }
    }

    @Override
    protected boolean addToPanel(JPanel p, TaggingPresetItemGuiSupport support) {
        initializeLocaleText(null);
        usage = determineTextUsage(support.getSelected(), key);
        seenValues.clear();
        // get the standard values from the preset definition
        initListEntries();

        // init the model
        dropDownModel = new AutoCompComboBoxModel<>(Comparator.<PresetListEntry>naturalOrder());

        if (!usage.hasUniqueValue() && !usage.unused()) {
            addEntry(PresetListEntry.ENTRY_DIFFERENT);
        }
        presetListEntries.forEach(this::addEntry);
        if (default_ != null) {
            addEntry(new PresetListEntry(default_, this));
        }
        addEntry(PresetListEntry.ENTRY_EMPTY);

        usage.map.forEach((value, count) -> {
            addEntry(new PresetListEntry(value, this));
        });

        combobox = new JosmComboBox<>(dropDownModel);
        AutoCompComboBoxEditor<AutoCompletionItem> editor = new AutoCompComboBoxEditor<>();
        combobox.setEditor(editor);

        // The default behaviour of JComboBox is to size the editor according to the tallest item in
        // the dropdown list.  We don't want that to happen because we want to show taller items in
        // the list than in the editor.  We can't use
        // {@code combobox.setPrototypeDisplayValue(PresetListEntry.ENTRY_EMPTY);} because that would
        // set a fixed cell height in JList.
        combobox.setPreferredHeight(combobox.getPreferredSize().height);

        // a custom cell renderer capable of displaying a short description text along with the
        // value
        combobox.setRenderer(new ComboMultiSelectListCellRenderer(combobox, combobox.getRenderer(), 200, key));
        combobox.setEditable(editable);

        autoCompModel = new AutoCompComboBoxModel<>(Comparator.<AutoCompletionItem>naturalOrder());
        getAllForKeys(Arrays.asList(key)).forEach(autoCompModel::addElement);
        getDisplayValues().forEach(s -> autoCompModel.addElement(new AutoCompletionItem(s, AutoCompletionPriority.IS_IN_STANDARD)));

        AutoCompTextField<AutoCompletionItem> tf = editor.getEditorComponent();
        tf.setModel(autoCompModel);

        if (TaggingPresetItem.DISPLAY_KEYS_AS_HINT.get()) {
            combobox.setHint(key);
        }
        if (length > 0) {
            tf.setMaxTextLength(length);
        }

        JLabel label = addLabel(p);

        if (key != null && ("colour".equals(key) || key.startsWith("colour:") || key.endsWith(":colour"))) {
            p.add(combobox, GBC.std().fill(GBC.HORIZONTAL)); // NOSONAR
            JButton button = new JButton(new ChooseColorAction());
            button.setOpaque(true);
            button.setBorderPainted(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            p.add(button, GBC.eol().fill(GBC.VERTICAL)); // NOSONAR
            ActionListener updateColor = ignore -> button.setBackground(getColor());
            updateColor.actionPerformed(null);
            combobox.addActionListener(updateColor);
        } else {
            p.add(combobox, GBC.eol().fill(GBC.HORIZONTAL)); // NOSONAR
        }

        String initialValue = getInitialValue(usage, support);
        PresetListEntry selItem = find(initialValue);
        if (selItem != null) {
            combobox.setSelectedItem(selItem);
        } else {
            combobox.setText(initialValue);
        }

        combobox.addActionListener(l -> support.fireItemValueModified(this, key, getSelectedItem().value));
        combobox.addComponentListener(new ComponentListener());

        label.setLabelFor(combobox);
        combobox.setToolTipText(getKeyTooltipText());
        combobox.applyComponentOrientation(OrientationAction.getValueOrientation(key));

        return true;
    }

    /**
     * Finds the PresetListEntry that matches value.
     * <p>
     * Looks in the model for an element whose {@code value} matches {@code value}.
     *
     * @param value The value to match.
     * @return The entry or null
     */
    private PresetListEntry find(String value) {
        return dropDownModel.asCollection().stream().filter(o -> o.value.equals(value)).findAny().orElse(null);
    }

    class ChooseColorAction extends AbstractAction {
        ChooseColorAction() {
            putValue(SHORT_DESCRIPTION, tr("Choose a color"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Color color = getColor();
            color = JColorChooser.showDialog(MainApplication.getMainPanel(), tr("Choose a color"), color);
            setColor(color);
        }
    }

    protected void setColor(Color color) {
        if (color != null) {
            combobox.setSelectedItem(ColorHelper.color2html(color));
        }
    }

    protected Color getColor() {
        String colorString = getSelectedItem().value;
        return colorString.startsWith("#")
                ? ColorHelper.html2color(colorString)
                : CSSColors.get(colorString);
    }

    @Override
    protected PresetListEntry getSelectedItem() {
        Object sel = combobox.getSelectedItem();
        if (sel instanceof PresetListEntry)
            // selected from the dropdown
            return (PresetListEntry) sel;
        if (sel instanceof String) {
            // free edit.  If the free edit corresponds to a known entry, use that entry.  This is
            // to avoid that we write a display_value to the tag's value, eg. if the user did an
            // undo.
            PresetListEntry selItem = dropDownModel.find((String) sel);
            if (selItem != null)
                return selItem;
            return new PresetListEntry((String) sel, this);
        }
        return PresetListEntry.ENTRY_EMPTY;
    }
}
