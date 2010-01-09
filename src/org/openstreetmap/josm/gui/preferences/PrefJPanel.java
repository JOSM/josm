/*
 * prefJPanel.java
 *
 * Created on 28. September 2008, 17:47
 */

package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.KeyEvent;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This is the keyboard preferences content.
 * If someone wants to merge it with ShortcutPreference.java, feel free.
 */
public class PrefJPanel extends javax.swing.JPanel {

        // table of shortcuts
        private TableModel model;
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
            KeyEvent.getKeyModifiersText(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[2]).getModifiers()),
            KeyEvent.getKeyModifiersText(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[3]).getModifiers()),
            KeyEvent.getKeyModifiersText(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[4]).getModifiers()),
            KeyEvent.getKeyModifiersText(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[5]).getModifiers()),
            KeyEvent.getKeyModifiersText(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[6]).getModifiers()),
            KeyEvent.getKeyModifiersText(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[7]).getModifiers()),
            KeyEvent.getKeyModifiersText(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[8]).getModifiers()),
            KeyEvent.getKeyModifiersText(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[9]).getModifiers()),
            KeyEvent.getKeyModifiersText(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[10]).getModifiers()),
            KeyEvent.getKeyModifiersText(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[11]).getModifiers()),
            KeyEvent.getKeyModifiersText(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[12]).getModifiers()),
            KeyEvent.getKeyModifiersText(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_A, modifInts[13]).getModifiers())
        };
        // this are the display(!) texts for the checkboxes. Let the JVM do the i18n for us <g>.
        // Ok, there's a real reason for this: The JVM should know best how the keys are labelled
        // on the physical keyboard. What language pack is installed in JOSM is completely
        // independent from the keyboard's labelling. But the operation system's locale
        // usually matches the keyboard. This even works with my English Windows and my German
        // keyboard.
        private static String SHIFT = KeyEvent.getKeyModifiersText(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.SHIFT_DOWN_MASK).getModifiers());
        private static String CTRL  = KeyEvent.getKeyModifiersText(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK).getModifiers());
        private static String ALT   = KeyEvent.getKeyModifiersText(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.ALT_DOWN_MASK).getModifiers());
        private static String META  = KeyEvent.getKeyModifiersText(javax.swing.KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.META_DOWN_MASK).getModifiers());

        // A list of keys to present the user. Sadly this really is a list of keys Java knows about,
        // not a list of real physical keys. If someone knows how to get that list?
        private static Map<Integer, String> keyList = setKeyList();

        private static Map<Integer, String> setKeyList() {
            Map<Integer, String> list = new LinkedHashMap<Integer, String>();
            // I hate this, but I found no alternative...
            for (int i = 0; i < 65534; i++) {
                String s = KeyEvent.getKeyText(i);
                if (s != null && s.length() > 0 && !s.contains("Unknown")) {
                    list.put(Integer.valueOf(i), s);
                    //System.out.println(i+": "+s);
                }
            }
            list.put(Integer.valueOf(-1), "");
            return list;
        }

    /** Creates new form prefJPanel */
    // Ain't those auto-generated comments helpful or what? <g>
    public PrefJPanel(TableModel model) {
        this.model = model;
        initComponents();
    }

    private void initComponents() {

        // Did I mention auto-generated? That's the reason we
        // have lots of properties here and not some arrays...
        prefTabPane = new javax.swing.JTabbedPane();
        shortcutTab = new javax.swing.JPanel();
        listPane = new javax.swing.JPanel();
        listScrollPane = new javax.swing.JScrollPane();
        shortcutTable = new javax.swing.JTable();
        shortcutEditPane = new javax.swing.JPanel();
        cbDefault = new javax.swing.JCheckBox();
        jLabel4 = new javax.swing.JLabel();
        cbShift = new javax.swing.JCheckBox();
        cbDisable = new javax.swing.JCheckBox();
        cbCtrl = new javax.swing.JCheckBox();
        tfKeyLabel = new javax.swing.JLabel();
        cbAlt = new javax.swing.JCheckBox();
        tfKey = new javax.swing.JComboBox();
        cbMeta = new javax.swing.JCheckBox();
        jLabel6 = new javax.swing.JLabel();
        modifierTab = new javax.swing.JPanel();
        editGroupPane = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        bxPrim1 = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        bxSec1 = new javax.swing.JComboBox();
        jLabel3 = new javax.swing.JLabel();
        bxTer1 = new javax.swing.JComboBox();
        menuGroupPane = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        bxPrim2 = new javax.swing.JComboBox();
        jLabel8 = new javax.swing.JLabel();
        bxSec2 = new javax.swing.JComboBox();
        jLabel9 = new javax.swing.JLabel();
        bxTer2 = new javax.swing.JComboBox();
        hotkeyGroupPane = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        bxPrim3 = new javax.swing.JComboBox();
        jLabel11 = new javax.swing.JLabel();
        bxSec3 = new javax.swing.JComboBox();
        jLabel12 = new javax.swing.JLabel();
        bxTer3 = new javax.swing.JComboBox();
        subwindowGroupPane = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        bxPrim4 = new javax.swing.JComboBox();
        jLabel14 = new javax.swing.JLabel();
        bxSec4 = new javax.swing.JComboBox();
        jLabel15 = new javax.swing.JLabel();
        bxTer4 = new javax.swing.JComboBox();
        infoTab = new javax.swing.JPanel();
        cbAction action = new cbAction(this);
        bxAction action2 = new bxAction();

        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));

                // If someone wants to move this text into some resource, feel free.
                infoTab.setLayout(new javax.swing.BoxLayout(shortcutTab, javax.swing.BoxLayout.Y_AXIS));
                JEditorPane editor = new JEditorPane();
                editor.setEditable(false);
                editor.setContentType("text/html");
                editor.setText(
                    tr("<h1><a name=\"top\">Keyboard Shortcuts</a></h1>")+
                    tr("<p>Please note that shortcuts keys are assigned to the actions when JOSM is started. So you need to <b>restart</b> "
                      +"JOSM to see your changes.</p>")+
                    tr("<p>Furthermore, the shortcuts are activated when the actions are assigned to a menu entry of button for the first "
                      +"time. So some of your changes may become active even without restart --- but also without collistion handling. "
                      +"This is another reason to <b>restart</b> JOSM after making any changes here.</p>")+
                    tr("<p>You may notice that the key selection list on the next page lists all keys that exist on all kinds of keyboards "
                      +"Java knows about, not just those keys that exist on your keyboard. Please use only those values that correspond to "
                      +"a real key on your keyboard. So if your keyboard has no 'Copy' key (PC keyboard don't have them, Sun keyboards do), "
                      +"the do not use it. Also there will be 'keys' listed that correspond to a shortcut on your keyboard (e.g. ':'/Colon). "
                      +"Please also do not use them, use the base key (';'/Semicolon on US keyboards, '.'/Period on German keyboards, ...) "
                      +"instead. Not doing so may result in conflicts, as there is no way for JOSM to know that Ctrl+Shift+; and Ctrl+: "
                      +"actually is the same thing on an US keyboard...</p>")+
                    tr("<p>Thank you for your understanding</p>")+
                    tr("<h1>Modifier Groups</h1>")+
                    tr("<p>The last page lists the modifier keys JOSM will automatically assign to shortcuts. For every of the four kinds "
                      +"of shortcuts there are three alternatives. JOSM will try those alternative in the listed order when managing a "
                      +"conflict. If all alternatives would result in shortcuts that are already taken, it will assign a random shortcut "
                      +"instead.</p>")+
                    tr("<p>The pseudo-modifier 'disabled' will disable the shortcut when encountered.</p>")
                );
                editor.setCaretPosition(0); // scroll up
                prefTabPane.addTab(tr("Read First"), new JScrollPane(editor));

        shortcutTab.setLayout(new javax.swing.BoxLayout(shortcutTab, javax.swing.BoxLayout.Y_AXIS));

        listPane.setLayout(new java.awt.GridLayout());

        // This is the list of shortcuts:
        shortcutTable.setModel(model);
        shortcutTable.getSelectionModel().addListSelectionListener(new cbAction(this));
        //shortcutTable.setFillsViewportHeight(true); Java 1.6
        shortcutTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        listScrollPane.setViewportView(shortcutTable);

        listPane.add(listScrollPane);

        shortcutTab.add(listPane);

        // and here follows the edit area. I won't object to someone re-designing it, it looks, um, "minimalistic" ;)
        shortcutEditPane.setLayout(new java.awt.GridLayout(5, 2));

        cbDefault.setAction(action);
        cbDefault.setText(tr("Use default"));
        shortcutEditPane.add(cbDefault);

        shortcutEditPane.add(jLabel4);

        cbShift.setAction(action);
        cbShift.setText(SHIFT); // see above for why no tr()
        shortcutEditPane.add(cbShift);

        cbDisable.setAction(action);
        cbDisable.setText(tr("Disable"));
        shortcutEditPane.add(cbDisable);

        cbCtrl.setAction(action);
        cbCtrl.setText(CTRL); // see above for why no tr()
        shortcutEditPane.add(cbCtrl);

        tfKeyLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        tfKeyLabel.setText(tr("Key:"));
        shortcutEditPane.add(tfKeyLabel);

        cbAlt.setAction(action);
        cbAlt.setText(ALT); // see above for why no tr()
        shortcutEditPane.add(cbAlt);

        tfKey.setAction(action);
        tfKey.setModel(new javax.swing.DefaultComboBoxModel(keyList.values().toArray()));
        shortcutEditPane.add(tfKey);

        cbMeta.setAction(action);
        cbMeta.setText(META); // see above for why no tr()
        shortcutEditPane.add(cbMeta);

        jLabel6.setText(tr("Attention: Use real keyboard keys only!"));
        shortcutEditPane.add(jLabel6);

        action.actionPerformed(null); // init checkboxes

        shortcutTab.add(shortcutEditPane);

        prefTabPane.addTab(tr("Keyboard Shortcuts"), shortcutTab);

        // next is the modfier group tab.
        // Would be a nice array if I had done it by hand. But then, it would be finished next year or so...
        modifierTab.setLayout(new java.awt.GridLayout(0, 1));
        JScrollPane modifierScroller = new JScrollPane(modifierTab);

        editGroupPane.setBorder(javax.swing.BorderFactory.createTitledBorder(tr("Edit Shortcuts")));
        editGroupPane.setLayout(new java.awt.GridLayout(3, 5));

        jLabel1.setText(tr("Primary modifier:"));
        editGroupPane.add(jLabel1);

        bxPrim1.setModel(new javax.swing.DefaultComboBoxModel(modifList));
        editGroupPane.add(bxPrim1);

        jLabel2.setText(tr("Secondary modifier:"));
        editGroupPane.add(jLabel2);

        bxSec1.setModel(new javax.swing.DefaultComboBoxModel(modifList));
        editGroupPane.add(bxSec1);

        jLabel3.setText(tr("Tertiary modifier:"));
        editGroupPane.add(jLabel3);

        bxTer1.setModel(new javax.swing.DefaultComboBoxModel(modifList));
        editGroupPane.add(bxTer1);

        modifierTab.add(editGroupPane);

        menuGroupPane.setBorder(javax.swing.BorderFactory.createTitledBorder(tr("Menu Shortcuts")));
        menuGroupPane.setLayout(new java.awt.GridLayout(3, 5));

        jLabel7.setText(tr("Primary modifier:"));
        menuGroupPane.add(jLabel7);

        bxPrim2.setModel(new javax.swing.DefaultComboBoxModel(modifList));
        menuGroupPane.add(bxPrim2);

        jLabel8.setText(tr("Secondary modifier:"));
        menuGroupPane.add(jLabel8);

        bxSec2.setModel(new javax.swing.DefaultComboBoxModel(modifList));
        menuGroupPane.add(bxSec2);

        jLabel9.setText(tr("Tertiary modifier:"));
        menuGroupPane.add(jLabel9);

        bxTer2.setModel(new javax.swing.DefaultComboBoxModel(modifList));
        menuGroupPane.add(bxTer2);

        modifierTab.add(menuGroupPane);

        hotkeyGroupPane.setBorder(javax.swing.BorderFactory.createTitledBorder(tr("Hotkey Shortcuts")));
        hotkeyGroupPane.setLayout(new java.awt.GridLayout(3, 5));

        jLabel10.setText(tr("Primary modifier:"));
        hotkeyGroupPane.add(jLabel10);

        bxPrim3.setModel(new javax.swing.DefaultComboBoxModel(modifList));
        hotkeyGroupPane.add(bxPrim3);

        jLabel11.setText(tr("Secondary modifier:"));
        hotkeyGroupPane.add(jLabel11);

        bxSec3.setModel(new javax.swing.DefaultComboBoxModel(modifList));
        hotkeyGroupPane.add(bxSec3);

        jLabel12.setText(tr("Tertiary modifier:"));
        hotkeyGroupPane.add(jLabel12);

        bxTer3.setModel(new javax.swing.DefaultComboBoxModel(modifList));
        hotkeyGroupPane.add(bxTer3);

        modifierTab.add(hotkeyGroupPane);

        subwindowGroupPane.setBorder(javax.swing.BorderFactory.createTitledBorder(tr("Subwindow Shortcuts")));
        subwindowGroupPane.setLayout(new java.awt.GridLayout(3, 5));

        jLabel13.setText(tr("Primary modifier:"));
        subwindowGroupPane.add(jLabel13);

        bxPrim4.setModel(new javax.swing.DefaultComboBoxModel(modifList));
        subwindowGroupPane.add(bxPrim4);

        jLabel14.setText(tr("Secondary modifier:"));
        subwindowGroupPane.add(jLabel14);

        bxSec4.setModel(new javax.swing.DefaultComboBoxModel(modifList));
        subwindowGroupPane.add(bxSec4);

        jLabel15.setText(tr("Tertiary modifier:"));
        subwindowGroupPane.add(jLabel15);

        bxTer4.setModel(new javax.swing.DefaultComboBoxModel(modifList));
        subwindowGroupPane.add(bxTer4);

                initbx();
                bxPrim1.setAction(action2);
                bxSec1.setAction(action2);
                bxTer1.setAction(action2);
                bxPrim2.setAction(action2);
                bxSec2.setAction(action2);
                bxTer2.setAction(action2);
                bxPrim3.setAction(action2);
                bxSec3.setAction(action2);
                bxTer3.setAction(action2);
                bxPrim4.setAction(action2);
                bxSec4.setAction(action2);
                bxTer4.setAction(action2);

        modifierTab.add(subwindowGroupPane);

        prefTabPane.addTab(tr("Modifier Groups"), modifierScroller);

        add(prefTabPane);
    }

    // this allows to edit shortcuts. it:
    //  * sets the edit controls to the selected shortcut
    //  * enabled/disables the controls as needed
    //  * writes the user's changes to the shortcut
    // And after I finally had it working, I realized that those two methods
    // are playing ping-pong (politically correct: table tennis, I know) and
    // even have some duplicated code. Feel free to refactor, If you have
    // more expirience with GUI coding than I have.
    private class cbAction extends javax.swing.AbstractAction implements ListSelectionListener {
        private PrefJPanel panel;
            public cbAction (PrefJPanel panel) {
                this.panel = panel;
        }
        public void valueChanged(ListSelectionEvent e) {
            ListSelectionModel lsm = panel.shortcutTable.getSelectionModel(); // can't use e here
            if (!lsm.isSelectionEmpty()) {
                int row = lsm.getMinSelectionIndex();
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
                    panel.cbDefault.setEnabled(false);
                    panel.cbDisable.setEnabled(false);
                    panel.cbShift.setEnabled(false);
                    panel.cbCtrl.setEnabled(false);
                    panel.cbAlt.setEnabled(false);
                    panel.cbMeta.setEnabled(false);
                    panel.tfKey.setEnabled(false);
                } else {
                    panel.cbDefault.setEnabled(true);
                    actionPerformed(null);
                }
            } else {
                panel.cbDefault.setEnabled(false);
                panel.cbDisable.setEnabled(false);
                panel.cbShift.setEnabled(false);
                panel.cbCtrl.setEnabled(false);
                panel.cbAlt.setEnabled(false);
                panel.cbMeta.setEnabled(false);
                panel.tfKey.setEnabled(false);
            }
        }
        public void actionPerformed(java.awt.event.ActionEvent e) {
            ListSelectionModel lsm = panel.shortcutTable.getSelectionModel();
            if (lsm != null && !lsm.isSelectionEmpty()) {
                if (e != null) { // only if we've been called by a user action
                    int row = lsm.getMinSelectionIndex();
                    Shortcut sc = (Shortcut)panel.model.getValueAt(row, -1);
                    sc.setAssignedUser(!panel.cbDefault.isSelected());
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
                panel.cbDefault.setEnabled(false);
                panel.cbDisable.setEnabled(false);
                panel.cbShift.setEnabled(false);
                panel.cbCtrl.setEnabled(false);
                panel.cbAlt.setEnabled(false);
                panel.cbMeta.setEnabled(false);
                panel.tfKey.setEnabled(false);
            }
        }
    }

    // this handles the modifier groups
    private class bxAction extends javax.swing.AbstractAction {
        public void actionPerformed(java.awt.event.ActionEvent e) {
            Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_EDIT),    Integer.toString( modifInts[bxPrim1.getSelectedIndex()] ));
            Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT1   +Shortcut.GROUP_EDIT),    Integer.toString( modifInts[ bxSec1.getSelectedIndex()] ));
            Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT2   +Shortcut.GROUP_EDIT),    Integer.toString( modifInts[ bxTer1.getSelectedIndex()] ));

            Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_MENU),    Integer.toString( modifInts[bxPrim2.getSelectedIndex()] ));
            Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT1   +Shortcut.GROUP_MENU),    Integer.toString( modifInts[ bxSec2.getSelectedIndex()] ));
            Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT2   +Shortcut.GROUP_MENU),    Integer.toString( modifInts[ bxTer2.getSelectedIndex()] ));

            Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_HOTKEY),  Integer.toString( modifInts[bxPrim3.getSelectedIndex()] ));
            Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT1   +Shortcut.GROUP_HOTKEY),  Integer.toString( modifInts[ bxSec3.getSelectedIndex()] ));
            Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT2   +Shortcut.GROUP_HOTKEY),  Integer.toString( modifInts[ bxTer3.getSelectedIndex()] ));

            Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_LAYER),   Integer.toString( modifInts[bxPrim4.getSelectedIndex()] ));
            Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT1   +Shortcut.GROUP_LAYER),   Integer.toString( modifInts[ bxSec4.getSelectedIndex()] ));
            Main.pref.put("shortcut.groups."+(Shortcut.GROUPS_ALT2   +Shortcut.GROUP_LAYER),   Integer.toString( modifInts[ bxTer4.getSelectedIndex()] ));
        }
    }

    private void initbx() {
        setBx(bxPrim1, "shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_EDIT));
        setBx(bxSec1,  "shortcut.groups."+(Shortcut.GROUPS_ALT1   +Shortcut.GROUP_EDIT));
        setBx(bxTer1,  "shortcut.groups."+(Shortcut.GROUPS_ALT2   +Shortcut.GROUP_EDIT));

        setBx(bxPrim2, "shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_MENU));
        setBx(bxSec2,  "shortcut.groups."+(Shortcut.GROUPS_ALT1   +Shortcut.GROUP_MENU));
        setBx(bxTer2,  "shortcut.groups."+(Shortcut.GROUPS_ALT2   +Shortcut.GROUP_MENU));

        setBx(bxPrim3, "shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_HOTKEY));
        setBx(bxSec3,  "shortcut.groups."+(Shortcut.GROUPS_ALT1   +Shortcut.GROUP_HOTKEY));
        setBx(bxTer3,  "shortcut.groups."+(Shortcut.GROUPS_ALT2   +Shortcut.GROUP_HOTKEY));

        setBx(bxPrim4, "shortcut.groups."+(Shortcut.GROUPS_DEFAULT+Shortcut.GROUP_LAYER));
        setBx(bxSec4,  "shortcut.groups."+(Shortcut.GROUPS_ALT1   +Shortcut.GROUP_LAYER));
        setBx(bxTer4,  "shortcut.groups."+(Shortcut.GROUPS_ALT2   +Shortcut.GROUP_LAYER));
    }
    private void setBx(javax.swing.JComboBox bx, String key) {
        int target = Main.pref.getInteger(key, -1);
        for (int i = 0; i < modifInts.length; i++) {
            if (modifInts[i] == target) {
                bx.setSelectedIndex(i);
            }
        }
    }

    private javax.swing.JComboBox bxPrim1;
    private javax.swing.JComboBox bxPrim2;
    private javax.swing.JComboBox bxPrim3;
    private javax.swing.JComboBox bxPrim4;
    private javax.swing.JComboBox bxSec1;
    private javax.swing.JComboBox bxSec2;
    private javax.swing.JComboBox bxSec3;
    private javax.swing.JComboBox bxSec4;
    private javax.swing.JComboBox bxTer1;
    private javax.swing.JComboBox bxTer2;
    private javax.swing.JComboBox bxTer3;
    private javax.swing.JComboBox bxTer4;
    private javax.swing.JCheckBox cbAlt;
    private javax.swing.JCheckBox cbCtrl;
    private javax.swing.JCheckBox cbDefault;
    private javax.swing.JCheckBox cbDisable;
    private javax.swing.JCheckBox cbMeta;
    private javax.swing.JCheckBox cbShift;
    private javax.swing.JPanel editGroupPane;
    private javax.swing.JPanel hotkeyGroupPane;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel listPane;
    private javax.swing.JScrollPane listScrollPane;
    private javax.swing.JPanel menuGroupPane;
    private javax.swing.JPanel modifierTab;
    private javax.swing.JTabbedPane prefTabPane;
    private javax.swing.JPanel shortcutEditPane;
    private javax.swing.JPanel shortcutTab;
    private javax.swing.JTable shortcutTable;
    private javax.swing.JPanel subwindowGroupPane;
    private javax.swing.JComboBox tfKey;
    private javax.swing.JLabel tfKeyLabel;
    private javax.swing.JPanel infoTab;
}
