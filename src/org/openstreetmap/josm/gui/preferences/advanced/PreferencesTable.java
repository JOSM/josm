// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.advanced;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.TableHelper;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.spi.preferences.ListListSetting;
import org.openstreetmap.josm.spi.preferences.ListSetting;
import org.openstreetmap.josm.spi.preferences.MapListSetting;
import org.openstreetmap.josm.spi.preferences.Setting;
import org.openstreetmap.josm.spi.preferences.StringSetting;
import org.openstreetmap.josm.tools.GBC;

/**
 * Component for editing list of preferences as a table.
 * @since 6021
 */
public class PreferencesTable extends JTable {
    private final AllSettingsTableModel model;
    private final transient List<PrefEntry> displayData;

    /**
     * Constructs a new {@code PreferencesTable}.
     * @param displayData The list of preferences entries to display
     */
    public PreferencesTable(List<PrefEntry> displayData) {
        this.displayData = displayData;
        model = new AllSettingsTableModel();
        setModel(model);
        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        getColumnModel().getColumn(1).setCellRenderer(new SettingCellRenderer());
        getColumnModel().getColumn(1).setCellEditor(new SettingCellEditor());

        TableHelper.setFont(this, getClass());
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editPreference(PreferencesTable.this);
                }
            }
        });
    }

    /**
     * This method should be called when displayed data was changed form external code
     */
    public void fireDataChanged() {
        model.fireTableDataChanged();
    }

    /**
     * The list of currently selected rows
     * @return newly created list of PrefEntry
     */
    public List<PrefEntry> getSelectedItems() {
        return Arrays.stream(getSelectedRows())
                .mapToObj(row -> (PrefEntry) model.getValueAt(row, -1))
                .collect(Collectors.toList());
    }

    /**
     * Call this to edit selected row in preferences table
     * @param gui - parent component for messagebox
     * @return true if editing was actually performed during this call
     */
    public boolean editPreference(final JComponent gui) {
        if (getSelectedRowCount() != 1) {
            return false;
        }
        final PrefEntry e = (PrefEntry) model.getValueAt(getSelectedRow(), 1);
        Setting<?> stg = e.getValue();
        boolean ok = false;
        if (stg instanceof StringSetting) {
            editCellAt(getSelectedRow(), 1);
            Component editor = getEditorComponent();
            if (editor != null) {
                editor.requestFocus();
            }
        } else if (stg instanceof ListSetting) {
            ok = doAddEditList(gui, e, (ListSetting) stg);
        } else if (stg instanceof ListListSetting) {
            ok = doAddEditListList(gui, e, (ListListSetting) stg);
        } else if (stg instanceof MapListSetting) {
            ok = doAddEditMapList(gui, e, (MapListSetting) stg);
        }
        return ok;
    }

    /**
     * Add new preference to the table
     * @param gui - parent component for asking dialogs
     * @return newly created entry or null if adding was cancelled
     */
    public PrefEntry addPreference(final JComponent gui) {
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel(tr("Key")), GBC.std().insets(0, 0, 5, 0));
        JosmTextField tkey = new JosmTextField("", 50);
        p.add(tkey, GBC.eop().insets(5, 0, 0, 0).fill(GBC.HORIZONTAL));

        p.add(new JLabel(tr("Select Setting Type:")), GBC.eol().insets(5, 15, 5, 0));

        JRadioButton rbString = new JRadioButton(tr("Simple"));
        JRadioButton rbList = new JRadioButton(tr("List"));
        JRadioButton rbListList = new JRadioButton(tr("List of lists"));
        JRadioButton rbMapList = new JRadioButton(tr("List of maps"));

        ButtonGroup group = new ButtonGroup();
        group.add(rbString);
        group.add(rbList);
        group.add(rbListList);
        group.add(rbMapList);

        p.add(rbString, GBC.eol());
        p.add(rbList, GBC.eol());
        p.add(rbListList, GBC.eol());
        p.add(rbMapList, GBC.eol());

        rbString.setSelected(true);

        PrefEntry pe = null;
        boolean ok = false;
        if (askAddSetting(gui, p)) {
            if (rbString.isSelected()) {
                StringSetting sSetting = new StringSetting(null);
                pe = new PrefEntry(tkey.getText(), sSetting, sSetting, false);
                ok = doAddSimple(gui, pe, sSetting);
            } else if (rbList.isSelected()) {
                ListSetting lSetting = new ListSetting(null);
                pe = new PrefEntry(tkey.getText(), lSetting, lSetting, false);
                ok = doAddEditList(gui, pe, lSetting);
            } else if (rbListList.isSelected()) {
                ListListSetting llSetting = new ListListSetting(null);
                pe = new PrefEntry(tkey.getText(), llSetting, llSetting, false);
                ok = doAddEditListList(gui, pe, llSetting);
            } else if (rbMapList.isSelected()) {
                MapListSetting mlSetting = new MapListSetting(null);
                pe = new PrefEntry(tkey.getText(), mlSetting, mlSetting, false);
                ok = doAddEditMapList(gui, pe, mlSetting);
            }
        }
        return ok ? pe : null;
    }

    private static boolean askAddSetting(JComponent gui, JPanel p) {
        return new ExtendedDialog(gui, tr("Add setting"), tr("OK"), tr("Cancel"))
                .setContent(p).setButtonIcons("ok", "cancel").showDialog().getValue() == 1;
    }

    private static boolean doAddSimple(final JComponent gui, PrefEntry pe, StringSetting sSetting) {
        StringEditor sEditor = new StringEditor(gui, pe, sSetting);
        sEditor.showDialog();
        if (sEditor.getValue() == 1) {
            String data = sEditor.getData();
            if (!Objects.equals(sSetting.getValue(), data)) {
                pe.setValue(new StringSetting(data));
                return true;
            }
        }
        return false;
    }

    private static boolean doAddEditList(final JComponent gui, PrefEntry pe, ListSetting lSetting) {
        ListEditor lEditor = new ListEditor(gui, pe, lSetting);
        lEditor.showDialog();
        if (lEditor.getValue() == 1) {
            List<String> data = lEditor.getData();
            if (!lSetting.equalVal(data)) {
                pe.setValue(new ListSetting(data));
                return true;
            }
        }
        return false;
    }

    private static boolean doAddEditListList(final JComponent gui, PrefEntry pe, ListListSetting llSetting) {
        ListListEditor llEditor = new ListListEditor(gui, pe, llSetting);
        llEditor.showDialog();
        if (llEditor.getValue() == 1) {
            List<List<String>> data = llEditor.getData();
            if (!llSetting.equalVal(data)) {
                pe.setValue(new ListListSetting(data));
                return true;
            }
        }
        return false;
    }

    private static boolean doAddEditMapList(final JComponent gui, PrefEntry pe, MapListSetting mlSetting) {
        MapListEditor mlEditor = new MapListEditor(gui, pe, mlSetting);
        mlEditor.showDialog();
        if (mlEditor.getValue() == 1) {
            List<Map<String, String>> data = mlEditor.getData();
            if (!mlSetting.equalVal(data)) {
                pe.setValue(new MapListSetting(data));
                return true;
            }
        }
        return false;
    }

    /**
     * Reset selected preferences to their default values
     * @param gui - parent component to display warning messages
     */
    public void resetPreferences(final JComponent gui) {
        if (getSelectedRowCount() == 0) {
            return;
        }
        for (int row : getSelectedRows()) {
            PrefEntry e = displayData.get(row);
            e.reset();
        }
        fireDataChanged();
    }

    final class AllSettingsTableModel extends DefaultTableModel {

        AllSettingsTableModel() {
            setColumnIdentifiers(new String[]{tr("Key"), tr("Value")});
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == 1 && (displayData.get(row).getValue() instanceof StringSetting);
        }

        @Override
        public int getRowCount() {
            return displayData.size();
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (column == 0)
                return displayData.get(row).getKey();
            else
                return displayData.get(row);
        }

        @Override
        public void setValueAt(Object o, int row, int column) {
            PrefEntry pe = displayData.get(row);
            String s = (String) o;
            if (!s.equals(pe.getValue().getValue())) {
                pe.setValue(new StringSetting(s));
                fireTableCellUpdated(row, column);
            }
        }
    }

    static final class SettingCellRenderer extends DefaultTableCellRenderer {
        private final Color backgroundColor = UIManager.getColor("Table.background");
        private final Color changedColor = new NamedColorProperty(
                         marktr("Advanced Background: Changed"),
                         new Color(200, 255, 200)).get();
        private final Color nonDefaultColor = new NamedColorProperty(
                            marktr("Advanced Background: NonDefault"),
                            new Color(255, 255, 200)).get();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value == null)
                return this;
            PrefEntry pe = (PrefEntry) value;
            Setting<?> setting = pe.getValue();
            Object val = setting.getValue();
            String display = val != null ? val.toString() : "<html><i>&lt;"+tr("unset")+"&gt;</i></html>";

            JLabel label = (JLabel) super.getTableCellRendererComponent(table,
                    display, isSelected, hasFocus, row, column);

            GuiHelper.setBackgroundReadable(label, backgroundColor);
            if (pe.isChanged()) {
                GuiHelper.setBackgroundReadable(label, changedColor);
            } else if (!pe.isDefault()) {
                GuiHelper.setBackgroundReadable(label, nonDefaultColor);
            }

            if (!pe.isDefault()) {
                label.setFont(label.getFont().deriveFont(Font.BOLD));
            }
            val = pe.getDefaultValue().getValue();
            if (val != null) {
                if (pe.isDefault()) {
                    label.setToolTipText(tr("Current value is default."));
                } else {
                    label.setToolTipText(tr("Default value is ''{0}''.", val));
                }
            } else {
                label.setToolTipText(tr("Default value currently unknown (setting has not been used yet)."));
            }
            return label;
        }
    }

    static final class SettingCellEditor extends DefaultCellEditor {
        SettingCellEditor() {
            super(new JosmTextField());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            PrefEntry pe = (PrefEntry) value;
            StringSetting stg = (StringSetting) pe.getValue();
            String s = stg.getValue() == null ? "" : stg.getValue();
            return super.getTableCellEditorComponent(table, s, isSelected, row, column);
        }
    }
}
