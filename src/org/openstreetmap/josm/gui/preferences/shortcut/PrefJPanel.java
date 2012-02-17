// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.shortcut;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;

import java.util.regex.PatternSyntaxException;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import javax.swing.table.TableRowSorter;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.widgets.SelectAllOnFocusGainedDecorator;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This is the keyboard preferences content.
 * If someone wants to merge it with ShortcutPreference.java, feel free.
 */
public class PrefJPanel extends JPanel {

    // table of shortcuts
    private AbstractTableModel model;
    // comboboxes of modifier groups, mapping selectedIndex to real data
    private static int[] modifInts = new int[]{
        -1,
        0,
        KeyEvent.SHIFT_DOWN_MASK,
        KeyEvent.CTRL_DOWN_MASK,
        KeyEvent.ALT_DOWN_MASK,
        KeyEvent.META_DOWN_MASK,
        KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK,
        KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK,
        KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK,
        KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK,
        KeyEvent.CTRL_DOWN_MASK | KeyEvent.META_DOWN_MASK,
        KeyEvent.ALT_DOWN_MASK | KeyEvent.META_DOWN_MASK,
        KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK | KeyEvent.ALT_DOWN_MASK,
        KeyEvent.META_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK | KeyEvent.ALT_DOWN_MASK
    };
    // and here are the texts fro the comboboxes
    private static String[] modifList = new String[] {
        tr("disabled"),
        tr("no modifier"),
        KeyEvent.getKeyModifiersText(KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[2]).getModifiers()),
        KeyEvent.getKeyModifiersText(KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[3]).getModifiers()),
        KeyEvent.getKeyModifiersText(KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[4]).getModifiers()),
        KeyEvent.getKeyModifiersText(KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[5]).getModifiers()),
        KeyEvent.getKeyModifiersText(KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[6]).getModifiers()),
        KeyEvent.getKeyModifiersText(KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[7]).getModifiers()),
        KeyEvent.getKeyModifiersText(KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[8]).getModifiers()),
        KeyEvent.getKeyModifiersText(KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[9]).getModifiers()),
        KeyEvent.getKeyModifiersText(KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[10]).getModifiers()),
        KeyEvent.getKeyModifiersText(KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[11]).getModifiers()),
        KeyEvent.getKeyModifiersText(KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[12]).getModifiers()),
        KeyEvent.getKeyModifiersText(KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[13]).getModifiers())
    };
    // this are the display(!) texts for the checkboxes. Let the JVM do the i18n for us <g>.
    // Ok, there's a real reason for this: The JVM should know best how the keys are labelled
    // on the physical keyboard. What language pack is installed in JOSM is completely
    // independent from the keyboard's labelling. But the operation system's locale
    // usually matches the keyboard. This even works with my English Windows and my German
    // keyboard.
    private static String SHIFT = KeyEvent.getKeyModifiersText(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.SHIFT_DOWN_MASK).getModifiers());
    private static String CTRL  = KeyEvent.getKeyModifiersText(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK).getModifiers());
    private static String ALT   = KeyEvent.getKeyModifiersText(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.ALT_DOWN_MASK).getModifiers());
    private static String META  = KeyEvent.getKeyModifiersText(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.META_DOWN_MASK).getModifiers());

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
                        //System.out.println(i+": "+s);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        list.put(Integer.valueOf(-1), "");
        return list;
    }

    private JComboBox bxPrim1 = new JComboBox();
    private JComboBox bxPrim2 = new JComboBox();
    private JComboBox bxPrim3 = new JComboBox();
    private JComboBox bxPrim4 = new JComboBox();
    private JComboBox bxSec1 = new JComboBox();
    private JComboBox bxSec2 = new JComboBox();
    private JComboBox bxSec3 = new JComboBox();
    private JComboBox bxSec4 = new JComboBox();
    private JComboBox bxTer1 = new JComboBox();
    private JComboBox bxTer2 = new JComboBox();
    private JComboBox bxTer3 = new JComboBox();
    private JComboBox bxTer4 = new JComboBox();
    private JCheckBox cbAlt = new JCheckBox();
    private JCheckBox cbCtrl = new JCheckBox();
    private JCheckBox cbMeta = new JCheckBox();
    private JCheckBox cbShift = new JCheckBox();
    private JCheckBox cbDefault = new JCheckBox();
    private JCheckBox cbDisable = new JCheckBox();
    private JComboBox tfKey = new JComboBox();

    JTable shortcutTable = new JTable();

    private JTextField filterField = new JTextField();

    /** Creates new form prefJPanel */
    // Ain't those auto-generated comments helpful or what? <g>
    public PrefJPanel(AbstractTableModel model) {
        this.model = model;
        initComponents();
    }

    private void initComponents() {
        JPanel editGroupPane = new JPanel();
        JPanel hotkeyGroupPane = new JPanel();

        JPanel listPane = new JPanel();
        JScrollPane listScrollPane = new JScrollPane();
        JPanel menuGroupPane = new JPanel();
        JPanel modifierTab = new JPanel();
        JTabbedPane prefTabPane = new JTabbedPane();
        JPanel shortcutEditPane = new JPanel();
        JPanel shortcutTab = new JPanel();
        JPanel subwindowGroupPane = new JPanel();
        JPanel infoTab = new JPanel();

        CbAction action = new CbAction(this);
        BxAction action2 = new BxAction();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        // If someone wants to move this text into some resource, feel free.
        infoTab.setLayout(new BoxLayout(shortcutTab, BoxLayout.Y_AXIS));
        JEditorPane editor = new JEditorPane();
        editor.setEditable(false);
        editor.setContentType("text/html");
        editor.setText(
                tr("<h1><a name=\"top\">Keyboard Shortcuts</a></h1>")+
                tr("<p>Please note that shortcut keys are assigned to the actions when JOSM is started. So you need to <b>restart</b> "
                        +"JOSM to see your changes.</p>")+
                        tr("<p>Furthermore, the shortcuts are activated when the actions are assigned to a menu entry of a button for the first "
                                +"time. So some of your changes may become active even without restart --- but also without collision handling. "
                                +"This is another reason to <b>restart</b> JOSM after making any changes here.</p>")+
                                tr("<p>You may notice that the key selection list on the next page lists all keys that exist on all kinds of keyboards "
                                        +"Java knows about, not just those keys that exist on your keyboard. Please only use values that correspond to "
                                        +"a real key on your keyboard. If your keyboard has no ''Copy'' key (PC keyboard do not have them, Sun keyboards do), "
                                        +"then do not use it. Also there are ''keys'' listed that correspond to a shortcut on your keyboard (e.g. '':''/Colon). "
                                        +"Please do not use them either, use the base key ('';''/Semicolon on US keyboards, ''.''/Period on German keyboards, etc.) "
                                        +"instead. Not doing so may result in conflicts, as there is no way for JOSM to know that Ctrl+Shift+; and Ctrl+: "
                                        +"actually is the same thing on an US keyboard.</p>")+
                                        tr("<h1>Modifier Groups</h1>")+
                                        tr("<p>The last page lists the modifier keys JOSM will automatically assign to shortcuts. For every of the four kinds "
                                                +"of shortcuts there are three alternatives. JOSM will try those alternatives in the listed order when managing a "
                                                +"conflict. If all alternatives result in shortcuts that are already taken, it will assign a random shortcut "
                                                +"instead.</p>")+
                                                tr("<p>The pseudo-modifier ''disabled'' will disable the shortcut when encountered.</p>")
        );
        editor.setCaretPosition(0); // scroll up
        prefTabPane.addTab(tr("Read First"), new JScrollPane(editor));

        shortcutTab.setLayout(new BoxLayout(shortcutTab, BoxLayout.Y_AXIS));

        shortcutTab.add(buildFilterPanel());
        listPane.setLayout(new java.awt.GridLayout());

        // This is the list of shortcuts:
        shortcutTable.setModel(model);
        shortcutTable.getSelectionModel().addListSelectionListener(new CbAction(this));
        shortcutTable.setFillsViewportHeight(true);
        shortcutTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        shortcutTable.setAutoCreateRowSorter(true);
        listScrollPane.setViewportView(shortcutTable);

        listPane.add(listScrollPane);

        shortcutTab.add(listPane);

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

        shortcutTab.add(shortcutEditPane);

        prefTabPane.addTab(tr("Keyboard Shortcuts"), shortcutTab);

        // next is the modfier group tab.
        // Would be a nice array if I had done it by hand. But then, it would be finished next year or so...
        modifierTab.setLayout(new java.awt.GridLayout(0, 1));
        JScrollPane modifierScroller = new JScrollPane(modifierTab);

        editGroupPane.setBorder(BorderFactory.createTitledBorder(tr("Edit Shortcuts")));
        editGroupPane.setLayout(new java.awt.GridLayout(3, 5));

        JComboBox[] bxArray = new JComboBox[] {
                    bxPrim1,bxSec1,bxTer1,bxPrim2,bxSec2,bxTer2,
                    bxPrim3,bxSec3,bxTer3,bxPrim4,bxSec4,bxTer4};
        for (JComboBox bxi: bxArray) bxi.setModel(new DefaultComboBoxModel(modifList));

        editGroupPane.add(new JLabel(tr("Primary modifier:")));
        editGroupPane.add(bxPrim1);
        editGroupPane.add(new JLabel(tr("Secondary modifier:")));
        editGroupPane.add(bxSec1);
        editGroupPane.add(new JLabel(tr("Tertiary modifier:")));
        editGroupPane.add(bxTer1);
        modifierTab.add(editGroupPane);

        menuGroupPane.setBorder(BorderFactory.createTitledBorder(tr("Menu Shortcuts")));
        menuGroupPane.setLayout(new java.awt.GridLayout(3, 5));
        menuGroupPane.add(new JLabel(tr("Primary modifier:")));
        menuGroupPane.add(bxPrim2);
        menuGroupPane.add(new JLabel(tr("Secondary modifier:")));
        menuGroupPane.add(bxSec2);
        menuGroupPane.add(new JLabel(tr("Tertiary modifier:")));
        menuGroupPane.add(bxTer2);
        modifierTab.add(menuGroupPane);

        hotkeyGroupPane.setBorder(BorderFactory.createTitledBorder(tr("Hotkey Shortcuts")));
        hotkeyGroupPane.setLayout(new java.awt.GridLayout(3, 5));
        hotkeyGroupPane.add(new JLabel(tr("Primary modifier:")));
        hotkeyGroupPane.add(bxPrim3);
        hotkeyGroupPane.add(new JLabel((tr("Secondary modifier:"))));
        hotkeyGroupPane.add(bxSec3);
        hotkeyGroupPane.add(new JLabel(tr("Tertiary modifier:")));
        hotkeyGroupPane.add(bxTer3);
        modifierTab.add(hotkeyGroupPane);

        subwindowGroupPane.setBorder(BorderFactory.createTitledBorder(tr("Subwindow Shortcuts")));
        subwindowGroupPane.setLayout(new java.awt.GridLayout(3, 5));
        subwindowGroupPane.add(new JLabel(tr("Primary modifier:")));
        subwindowGroupPane.add(bxPrim4);
        subwindowGroupPane.add(new JLabel(tr("Secondary modifier:")));
        subwindowGroupPane.add(bxSec4);
        subwindowGroupPane.add(new JLabel(tr("Tertiary modifier:")));
        subwindowGroupPane.add(bxTer4);

        initbx();
        for (JComboBox bxi: bxArray) bxi.setAction(action2);

        modifierTab.add(subwindowGroupPane);

        prefTabPane.addTab(tr("Modifier Groups"), modifierScroller);

        add(prefTabPane);
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
                model.fireTableCellUpdated(row, 1);
            } else {
                panel.disableAllModifierCheckboxes();
                panel.tfKey.setEnabled(false);
            }
        }
        public void actionPerformed(java.awt.event.ActionEvent e) {
            ListSelectionModel lsm = panel.shortcutTable.getSelectionModel();
            if (lsm != null && !lsm.isSelectionEmpty()) {
                if (e != null) { // only if we've been called by a user action
                    int row = panel.shortcutTable.convertRowIndexToModel(lsm.getMinSelectionIndex());
                    Shortcut sc = (Shortcut)panel.model.getValueAt(row, -1);
                    if (panel.cbDisable.isSelected()) {
                        sc.setAssignedModifier(-1);
                    } else if (panel.tfKey.getSelectedItem().equals("")) {
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

    // this handles the modifier groups
    private class BxAction extends AbstractAction {
        public void actionPerformed(java.awt.event.ActionEvent e) {
            Main.pref.putInteger("shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_EDIT),    modifInts[bxPrim1.getSelectedIndex()]);
            Main.pref.putInteger("shortcut.groups."+(Shortcut.GROUPS_ALT1   +Shortcut.GROUP_EDIT),    modifInts[ bxSec1.getSelectedIndex()]);
            Main.pref.putInteger("shortcut.groups."+(Shortcut.GROUPS_ALT2   +Shortcut.GROUP_EDIT),    modifInts[ bxTer1.getSelectedIndex()]);

            Main.pref.putInteger("shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_MENU),    modifInts[bxPrim2.getSelectedIndex()]);
            Main.pref.putInteger("shortcut.groups."+(Shortcut.GROUPS_ALT1   +Shortcut.GROUP_MENU),    modifInts[ bxSec2.getSelectedIndex()]);
            Main.pref.putInteger("shortcut.groups."+(Shortcut.GROUPS_ALT2   +Shortcut.GROUP_MENU),    modifInts[ bxTer2.getSelectedIndex()]);

            Main.pref.putInteger("shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_HOTKEY),  modifInts[bxPrim3.getSelectedIndex()]);
            Main.pref.putInteger("shortcut.groups."+(Shortcut.GROUPS_ALT1   +Shortcut.GROUP_HOTKEY),  modifInts[ bxSec3.getSelectedIndex()]);
            Main.pref.putInteger("shortcut.groups."+(Shortcut.GROUPS_ALT2   +Shortcut.GROUP_HOTKEY),  modifInts[ bxTer3.getSelectedIndex()]);

            Main.pref.putInteger("shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_LAYER),   modifInts[bxPrim4.getSelectedIndex()]);
            Main.pref.putInteger("shortcut.groups."+(Shortcut.GROUPS_ALT1   +Shortcut.GROUP_LAYER),   modifInts[ bxSec4.getSelectedIndex()]);
            Main.pref.putInteger("shortcut.groups."+(Shortcut.GROUPS_ALT2   +Shortcut.GROUP_LAYER),   modifInts[ bxTer4.getSelectedIndex()]);
        }
    }

    private void initbx() {
        HashMap<Integer, Integer> groups = Main.platform.initShortcutGroups(false);
        setBx(bxPrim1, groups, Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_EDIT);
        setBx(bxSec1,  groups, Shortcut.GROUPS_ALT1   +Shortcut.GROUP_EDIT);
        setBx(bxTer1,  groups, Shortcut.GROUPS_ALT2   +Shortcut.GROUP_EDIT);

        setBx(bxPrim2, groups, Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_MENU);
        setBx(bxSec2,  groups, Shortcut.GROUPS_ALT1   +Shortcut.GROUP_MENU);
        setBx(bxTer2,  groups, Shortcut.GROUPS_ALT2   +Shortcut.GROUP_MENU);

        setBx(bxPrim3, groups, Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_HOTKEY);
        setBx(bxSec3,  groups, Shortcut.GROUPS_ALT1   +Shortcut.GROUP_HOTKEY);
        setBx(bxTer3,  groups, Shortcut.GROUPS_ALT2   +Shortcut.GROUP_HOTKEY);

        setBx(bxPrim4, groups, Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_LAYER);
        setBx(bxSec4,  groups, Shortcut.GROUPS_ALT1   +Shortcut.GROUP_LAYER);
        setBx(bxTer4,  groups, Shortcut.GROUPS_ALT2   +Shortcut.GROUP_LAYER);
    }
    private void setBx(JComboBox bx, HashMap<Integer, Integer> groups, int key) {
        int target = Main.pref.getInteger("shortcut.groups."+key, groups.get(key));
        for (int i = 0; i < modifInts.length; i++) {
            if (modifInts[i] == target) {
                bx.setSelectedIndex(i);
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
                    // split search string on whitespace, do case-insensitive AND search
                    ArrayList<RowFilter<Object, Object>> andFilters = new ArrayList<RowFilter<Object, Object>>();
                    for (String word : expr.split("\\s+")) {
                        andFilters.add(RowFilter.regexFilter("(?i)" + word));
                    }
                    sorter.setRowFilter(RowFilter.andFilter(andFilters));
                }
            }
            catch (PatternSyntaxException ex) { }
            catch (ClassCastException ex2) { /* eliminate warning */  }
        }

        public void changedUpdate(DocumentEvent arg0) { filter(); }
        public void insertUpdate(DocumentEvent arg0) {  filter(); }
        public void removeUpdate(DocumentEvent arg0) { filter(); }
    }

}
