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

    @Override
    protected JComponent addToPanelAnchor(JPanel p, String def, TaggingPresetItemGuiSupport support) {
        if (!usage.unused()) {
            for (String s : usage.values) {
                presetListEntries.add(new PresetListEntry(s));
            }
        }
        if (def != null) {
            presetListEntries.add(new PresetListEntry(def));
        }
        presetListEntries.add(new PresetListEntry(""));

        dropDownModel = new AutoCompComboBoxModel<PresetListEntry>(Comparator.naturalOrder());
        autoCompModel = new AutoCompComboBoxModel<AutoCompletionItem>(Comparator.naturalOrder());
        presetListEntries.forEach(dropDownModel::addElement);

        combobox = new JosmComboBox<>(dropDownModel);
        AutoCompComboBoxEditor<AutoCompletionItem> editor = new AutoCompComboBoxEditor<>();
        combobox.setEditor(editor);

        // The default behaviour of JComboBox is to size the editor according to the tallest item in
        // the dropdown list.  We don't want that to happen because we want to show taller items in
        // the list than in the editor.  We can't use
        // {@code combobox.setPrototypeDisplayValue(new PresetListEntry(" "));} because that would
        // set a fixed cell height in JList.
        combobox.setPreferredHeight(combobox.getPreferredSize().height);

        // a custom cell renderer capable of displaying a short description text along with the
        // value
        combobox.setRenderer(new ComboMultiSelectListCellRenderer(combobox, combobox.getRenderer(), 200, key));
        combobox.setEditable(editable);

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

        if (key != null && ("colour".equals(key) || key.startsWith("colour:") || key.endsWith(":colour"))) {
            p.add(combobox, GBC.std().fill(GBC.HORIZONTAL));
            JButton button = new JButton(new ChooseColorAction());
            button.setOpaque(true);
            button.setBorderPainted(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            p.add(button, GBC.eol().fill(GBC.VERTICAL));
            ActionListener updateColor = ignore -> button.setBackground(getColor());
            updateColor.actionPerformed(null);
            combobox.addActionListener(updateColor);
        } else {
            p.add(combobox, GBC.eol().fill(GBC.HORIZONTAL));
        }

        Object itemToSelect = getItemToSelect(default_, support, false);
        combobox.setSelectedItemText(itemToSelect == null ? null : itemToSelect.toString());
        combobox.addActionListener(l -> support.fireItemValueModified(this, key, getSelectedValue()));
        combobox.addComponentListener(new ComponentListener());
        return combobox;
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
        String colorString = String.valueOf(getSelectedValue());
        return colorString.startsWith("#")
                ? ColorHelper.html2color(colorString)
                : CSSColors.get(colorString);
    }

    @Override
    protected Object getSelectedItem() {
        return combobox.getSelectedItem();
    }

    @Override
    protected String getDisplayIfNull() {
        if (combobox.isEditable())
            return combobox.getEditor().getItem().toString();
        else
            return null;
    }
}
