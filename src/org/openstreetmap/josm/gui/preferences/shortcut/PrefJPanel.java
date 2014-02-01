// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.shortcut;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This is the keyboard preferences content.
 */
public class PrefJPanel extends JPanel {

    // table of shortcuts
    private AbstractTableModel model;
    // this are the display(!) texts for the checkboxes. Let the JVM do the i18n for us <g>.
    // Ok, there's a real reason for this: The JVM should know best how the keys are labelled
    // on the physical keyboard. What language pack is installed in JOSM is completely
    // independent from the keyboard's labelling. But the operation system's locale
    // usually matches the keyboard. This even works with my English Windows and my German keyboard.
    private static final String SHIFT = KeyEvent.getKeyModifiersText(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.SHIFT_DOWN_MASK).getModifiers());
    private static final String CTRL  = KeyEvent.getKeyModifiersText(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK).getModifiers());
    private static final String ALT   = KeyEvent.getKeyModifiersText(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.ALT_DOWN_MASK).getModifiers());
    private static final String META  = KeyEvent.getKeyModifiersText(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.META_DOWN_MASK).getModifiers());

    // A list of keys to present the user. Sadly this really is a list of keys Java knows about,
    // not a list of real physical keys. If someone knows how to get that list?
    private static Map<Integer, String> keyList = setKeyList();

    private static Map<Integer, String> setKeyList() {
        Map<Integer, String> list = new LinkedHashMap<Integer, String>();
        String unknown = Toolkit.getProperty("AWT.unknown", "Unknown");
        // Assume all known keys are declared in KeyEvent as "public static int VK_*"
        for (Field field : KeyEvent.class.getFields()) {
            if (field.getName().startsWith("VK_")) {
                try {
                    int i = field.getInt(null);
                    String s = KeyEvent.getKeyText(i);
                    if (s != null && s.length() > 0 && !s.contains(unknown)) {
                        list.put(Integer.valueOf(i), s);
                    }
                } catch (Exception e) {
                    Main.error(e);
                }
            }
        }
        list.put(Integer.valueOf(-1), "");
        return list;
    }

    private JCheckBox cbAlt = new JCheckBox();
    private JCheckBox cbCtrl = new JCheckBox();
    private JCheckBox cbMeta = new JCheckBox();
    private JCheckBox cbShift = new JCheckBox();
    private JCheckBox cbDefault = new JCheckBox();
    private JCheckBox cbDisable = new JCheckBox();
    private JosmComboBox tfKey = new JosmComboBox();

    JTable shortcutTable = new JTable();

    private JosmTextField filterField = new JosmTextField();

    /** Creates new form prefJPanel */
    public PrefJPanel() {
        this.model = new ScListModel();
        initComponents();
    }

    /**
     * Show only shortcuts with descriptions containing given substring
     * @param substring The substring used to filter
     */
    public void filter(String substring) {
        filterField.setText(substring);
    }

    private static class ScListModel extends AbstractTableModel {
        private String[] columnNames = new String[]{tr("Action"), tr("Shortcut")};
        private List<Shortcut> data;

        public ScListModel() {
            data = Shortcut.listAll();
        }
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        @Override
        public int getRowCount() {
            return data.size();
        }
        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }
        @Override
        public Object getValueAt(int row, int col) {
            return (col==0)?  data.get(row).getLongText() : data.get(row);
        }
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    }

    private class ShortcutTableCellRenderer extends DefaultTableCellRenderer {

        private boolean name;

        public ShortcutTableCellRenderer(boolean name) {
            this.name = name;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean
                isSelected, boolean hasFocus, int row, int column) {
            int row1 = shortcutTable.convertRowIndexToModel(row);
            Shortcut sc = (Shortcut)model.getValueAt(row1, -1);
            if (sc==null) return null;
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, name ? sc.getLongText() : sc.getKeyText(), isSelected, hasFocus, row, column);
            label.setBackground(Main.pref.getUIColor("Table.background"));
            if (isSelected) {
                label.setForeground(Main.pref.getUIColor("Table.foreground"));
            }
            if(sc.getAssignedUser()) {
                label.setBackground(Main.pref.getColor(
                        marktr("Shortcut Background: User"),
                        new Color(200,255,200)));
            } else if(!sc.getAssignedDefault()) {
                label.setBackground(Main.pref.getColor(
                        marktr("Shortcut Background: Modified"),
                        new Color(255,255,200)));
            }
            return label;
        }
    }

    private void initComponents() {
        JPanel listPane = new JPanel();
        JScrollPane listScrollPane = new JScrollPane();
        JPanel shortcutEditPane = new JPanel();

        CbAction action = new CbAction(this);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(buildFilterPanel());
        listPane.setLayout(new java.awt.GridLayout());

        // This is the list of shortcuts:
        shortcutTable.setModel(model);
        shortcutTable.getSelectionModel().addListSelectionListener(new CbAction(this));
        shortcutTable.setFillsViewportHeight(true);
        shortcutTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        shortcutTable.setAutoCreateRowSorter(true);
        TableColumnModel mod = shortcutTable.getColumnModel();
        mod.getColumn(0).setCellRenderer(new ShortcutTableCellRenderer(true));
        mod.getColumn(1).setCellRenderer(new ShortcutTableCellRenderer(false));
        listScrollPane.setViewportView(shortcutTable);

        listPane.add(listScrollPane);

        add(listPane);

        // and here follows the edit area. I won't object to someone re-designing it, it looks, um, "minimalistic" ;)
        shortcutEditPane.setLayout(new java.awt.GridLayout(5, 2));

        cbDefault.setAction(action);
        cbDefault.setText(tr("Use default"));
        cbShift.setAction(action);
        cbShift.setText(SHIFT); // see above for why no tr()
        cbDisable.setAction(action);
        cbDisable.setText(tr("Disable"));
        cbCtrl.setAction(action);
        cbCtrl.setText(CTRL); // see above for why no tr()
        cbAlt.setAction(action);
        cbAlt.setText(ALT); // see above for why no tr()
        tfKey.setAction(action);
        tfKey.setModel(new DefaultComboBoxModel(keyList.values().toArray()));
        cbMeta.setAction(action);
        cbMeta.setText(META); // see above for why no tr()

        shortcutEditPane.add(cbDefault);
        shortcutEditPane.add(new JLabel());
        shortcutEditPane.add(cbShift);
        shortcutEditPane.add(cbDisable);
        shortcutEditPane.add(cbCtrl);
        shortcutEditPane.add(new JLabel(tr("Key:"), SwingConstants.LEFT));
        shortcutEditPane.add(cbAlt);
        shortcutEditPane.add(tfKey);
        shortcutEditPane.add(cbMeta);

        shortcutEditPane.add(new JLabel(tr("Attention: Use real keyboard keys only!")));

        action.actionPerformed(null); // init checkboxes

        add(shortcutEditPane);
    }

    private JPanel buildFilterPanel() {
        // copied from PluginPreference
        JPanel pnl  = new JPanel(new GridBagLayout());
        pnl.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        GridBagConstraints gc = new GridBagConstraints();

        gc.anchor = GridBagConstraints.NORTHWEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.0;
        gc.insets = new Insets(0,0,0,5);
        pnl.add(new JLabel(tr("Search:")), gc);

        gc.gridx = 1;
        gc.weightx = 1.0;
        pnl.add(filterField, gc);
        filterField.setToolTipText(tr("Enter a search expression"));
        SelectAllOnFocusGainedDecorator.decorate(filterField);
        filterField.getDocument().addDocumentListener(new FilterFieldAdapter());
        pnl.setMaximumSize(new Dimension(300,10));
        return pnl;
    }

    private void disableAllModifierCheckboxes() {
        cbDefault.setEnabled(false);
        cbDisable.setEnabled(false);
        cbShift.setEnabled(false);
        cbCtrl.setEnabled(false);
        cbAlt.setEnabled(false);
        cbMeta.setEnabled(false);
    }

    // this allows to edit shortcuts. it:
    //  * sets the edit controls to the selected shortcut
    //  * enabled/disables the controls as needed
    //  * writes the user's changes to the shortcut
    // And after I finally had it working, I realized that those two methods
    // are playing ping-pong (politically correct: table tennis, I know) and
    // even have some duplicated code. Feel free to refactor, If you have
    // more expirience with GUI coding than I have.
    private class CbAction extends AbstractAction implements ListSelectionListener {
        private PrefJPanel panel;
        public CbAction (PrefJPanel panel) {
            this.panel = panel;
        }
        @Override
        public void valueChanged(ListSelectionEvent e) {
            ListSelectionModel lsm = panel.shortcutTable.getSelectionModel(); // can't use e here
            if (!lsm.isSelectionEmpty()) {
                int row = panel.shortcutTable.convertRowIndexToModel(lsm.getMinSelectionIndex());
                Shortcut sc = (Shortcut)panel.model.getValueAt(row, -1);
                panel.cbDefault.setSelected(!sc.getAssignedUser());
                panel.cbDisable.setSelected(sc.getKeyStroke() == null);
                panel.cbShift.setSelected(sc.getAssignedModifier() != -1 && (sc.getAssignedModifier() & KeyEvent.SHIFT_DOWN_MASK) != 0);
                panel.cbCtrl.setSelected(sc.getAssignedModifier() != -1 && (sc.getAssignedModifier() & KeyEvent.CTRL_DOWN_MASK) != 0);
                panel.cbAlt.setSelected(sc.getAssignedModifier() != -1 && (sc.getAssignedModifier() & KeyEvent.ALT_DOWN_MASK) != 0);
                panel.cbMeta.setSelected(sc.getAssignedModifier() != -1 && (sc.getAssignedModifier() & KeyEvent.META_DOWN_MASK) != 0);
                if (sc.getKeyStroke() != null) {
                    tfKey.setSelectedItem(keyList.get(sc.getKeyStroke().getKeyCode()));
                } else {
                    tfKey.setSelectedItem(keyList.get(-1));
                }
                if (!sc.isChangeable()) {
                    disableAllModifierCheckboxes();
                    panel.tfKey.setEnabled(false);
                } else {
                    panel.cbDefault.setEnabled(true);
                    actionPerformed(null);
                }
                model.fireTableRowsUpdated(row, row);
            } else {
                panel.disableAllModifierCheckboxes();
                panel.tfKey.setEnabled(false);
            }
        }
        @Override
        public void actionPerformed(java.awt.event.ActionEvent e) {
            ListSelectionModel lsm = panel.shortcutTable.getSelectionModel();
            if (lsm != null && !lsm.isSelectionEmpty()) {
                if (e != null) { // only if we've been called by a user action
                    int row = panel.shortcutTable.convertRowIndexToModel(lsm.getMinSelectionIndex());
                    Shortcut sc = (Shortcut)panel.model.getValueAt(row, -1);
                    if (panel.cbDisable.isSelected()) {
                        sc.setAssignedModifier(-1);
                    } else if (panel.tfKey.getSelectedItem() == null || panel.tfKey.getSelectedItem().equals("")) {
                        sc.setAssignedModifier(KeyEvent.VK_CANCEL);
                    } else {
                        sc.setAssignedModifier(
                                (panel.cbShift.isSelected() ? KeyEvent.SHIFT_DOWN_MASK : 0) |
                                (panel.cbCtrl.isSelected() ? KeyEvent.CTRL_DOWN_MASK : 0) |
                                (panel.cbAlt.isSelected() ? KeyEvent.ALT_DOWN_MASK : 0) |
                                (panel.cbMeta.isSelected() ? KeyEvent.META_DOWN_MASK : 0)
                        );
                        for (Map.Entry<Integer, String> entry : keyList.entrySet()) {
                            if (entry.getValue().equals(panel.tfKey.getSelectedItem())) {
                                sc.setAssignedKey(entry.getKey());
                            }
                        }
                    }
                    sc.setAssignedUser(!panel.cbDefault.isSelected());
                    valueChanged(null);
                }
                boolean state = !panel.cbDefault.isSelected();
                panel.cbDisable.setEnabled(state);
                state = state && !panel.cbDisable.isSelected();
                panel.cbShift.setEnabled(state);
                panel.cbCtrl.setEnabled(state);
                panel.cbAlt.setEnabled(state);
                panel.cbMeta.setEnabled(state);
                panel.tfKey.setEnabled(state);
            } else {
                panel.disableAllModifierCheckboxes();
                panel.tfKey.setEnabled(false);
            }
        }
    }

    class FilterFieldAdapter implements DocumentListener {
        public void filter() {
            String expr = filterField.getText().trim();
            if (expr.length()==0) { expr=null; }
            try {
                final TableRowSorter<? extends TableModel> sorter =
                    ((TableRowSorter<? extends TableModel> )shortcutTable.getRowSorter());
                if (expr == null) {
                    sorter.setRowFilter(null);
                } else {
                    expr = expr.replace("+", "\\+");
                    // split search string on whitespace, do case-insensitive AND search
                    List<RowFilter<Object, Object>> andFilters = new ArrayList<RowFilter<Object, Object>>();
                    for (String word : expr.split("\\s+")) {
                        andFilters.add(RowFilter.regexFilter("(?i)" + word));
                    }
                    sorter.setRowFilter(RowFilter.andFilter(andFilters));
                }
                model.fireTableDataChanged();
            } catch (PatternSyntaxException ex) {
                Main.warn(ex);
            } catch (ClassCastException ex2) {
                Main.warn(ex2);
            }
        }

        @Override
        public void changedUpdate(DocumentEvent arg0) { filter(); }
        @Override
        public void insertUpdate(DocumentEvent arg0) {  filter(); }
        @Override
        public void removeUpdate(DocumentEvent arg0) { filter(); }
    }
}
