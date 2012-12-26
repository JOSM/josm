// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.mapmode.DrawAction;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingComboBox;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionListItem;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Class that helps PropertiesDialog add and edit tag values
 */
 class TagEditHelper {
        private final DefaultTableModel propertyData;
        private final Map<String, Map<String, Integer>> valueCount;
        
        private String changedKey;
        
        private String lastAddKey = null;
        private String lastAddValue = null;

        public static final int DEFAULT_LRU_TAGS_NUMBER = 5;
        public static final int MAX_LRU_TAGS_NUMBER = 9;
        
        private String objKey;
    
        Comparator<AutoCompletionListItem> defaultACItemComparator = new Comparator<AutoCompletionListItem>() {
            public int compare(AutoCompletionListItem o1, AutoCompletionListItem o2) {
                return String.CASE_INSENSITIVE_ORDER.compare(o1.getValue(), o2.getValue());
            }
        };

    
    // LRU cache for recently added tags (http://java-planet.blogspot.com/2005/08/how-to-set-up-simple-lru-cache-using.html) 
    private final Map<Tag, Void> recentTags = new LinkedHashMap<Tag, Void>(MAX_LRU_TAGS_NUMBER+1, 1.1f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Tag, Void> eldest) {
            return size() > MAX_LRU_TAGS_NUMBER;
        }
    };

    TagEditHelper(DefaultTableModel propertyData, Map<String, Map<String, Integer>> valueCount) {
        this.propertyData = propertyData;
        this.valueCount = valueCount;
    }

    /**
     * Open the add selection dialog and add a new key/value to the table (and
     * to the dataset, of course).
     */
    public void addProperty() {
        changedKey = null;
        Collection<OsmPrimitive> sel;
        if (Main.map.mapMode instanceof DrawAction) {
            sel = ((DrawAction) Main.map.mapMode).getInProgressSelection();
        } else {
            DataSet ds = Main.main.getCurrentDataSet();
            if (ds == null) return;
            sel = ds.getSelected();
        }
        if (sel.isEmpty()) return;

        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel("<html>"+trn("This will change up to {0} object.",
                "This will change up to {0} objects.", sel.size(),sel.size())
                +"<br><br>"+tr("Please select a key")), GBC.eol().fill(GBC.HORIZONTAL));
        final AutoCompletingComboBox keys = new AutoCompletingComboBox();
        AutoCompletionManager autocomplete = Main.main.getEditLayer().data.getAutoCompletionManager();
        List<AutoCompletionListItem> keyList = autocomplete.getKeys();

        AutoCompletionListItem itemToSelect = null;
        // remove the object's tag keys from the list
        Iterator<AutoCompletionListItem> iter = keyList.iterator();
        while (iter.hasNext()) {
            AutoCompletionListItem item = iter.next();
            if (item.getValue().equals(lastAddKey)) {
                itemToSelect = item;
            }
            for (int i = 0; i < propertyData.getRowCount(); ++i) {
                if (item.getValue().equals(propertyData.getValueAt(i, 0))) {
                    if (itemToSelect == item) {
                        itemToSelect = null;
                    }
                    iter.remove();
                    break;
                }
            }
        }

        Collections.sort(keyList, defaultACItemComparator);
        keys.setPossibleACItems(keyList);
        keys.setEditable(true);

        p.add(keys, GBC.eop().fill());

        p.add(new JLabel(tr("Please select a value")), GBC.eol());
        final AutoCompletingComboBox values = new AutoCompletingComboBox();
        values.setEditable(true);
        p.add(values, GBC.eop().fill());
        if (itemToSelect != null) {
            keys.setSelectedItem(itemToSelect);
            if (lastAddValue != null) {
                values.setSelectedItem(lastAddValue);
            }
        }

        FocusAdapter focus = addFocusAdapter(keys, values, autocomplete, defaultACItemComparator);
        // fire focus event in advance or otherwise the popup list will be too small at first
        focus.focusGained(null);

        int recentTagsToShow = Main.pref.getInteger("properties.recently-added-tags", DEFAULT_LRU_TAGS_NUMBER);
        if (recentTagsToShow > MAX_LRU_TAGS_NUMBER) {
            recentTagsToShow = MAX_LRU_TAGS_NUMBER;
        }
        List<JosmAction> recentTagsActions = new ArrayList<JosmAction>();
        suggestRecentlyAddedTags(p, keys, values, recentTagsActions, recentTagsToShow, focus);

        JOptionPane pane = new JOptionPane(p, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION){
            @Override public void selectInitialValue() {
                // save unix system selection (middle mouse paste)
                Clipboard sysSel = Toolkit.getDefaultToolkit().getSystemSelection();
                if(sysSel != null) {
                    Transferable old = sysSel.getContents(null);
                    keys.requestFocusInWindow();
                    keys.getEditor().selectAll();
                    sysSel.setContents(old, null);
                } else {
                    keys.requestFocusInWindow();
                    keys.getEditor().selectAll();
                }
            }
        };
        JDialog dialog = pane.createDialog(Main.parent, tr("Add value?"));
        dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        dialog.setVisible(true);
        
        for (JosmAction action : recentTagsActions) {
            action.destroy();
        }

        if (!Integer.valueOf(JOptionPane.OK_OPTION).equals(pane.getValue()))
            return;
        String key = keys.getEditor().getItem().toString().trim();
        String value = values.getEditor().getItem().toString().trim();
        if (key.isEmpty() || value.isEmpty())
            return;
        lastAddKey = key;
        lastAddValue = value;
        recentTags.put(new Tag(key, value), null);
        Main.main.undoRedo.add(new ChangePropertyCommand(sel, key, value));
        changedKey = key;
    }
    
    private void suggestRecentlyAddedTags(JPanel p, final AutoCompletingComboBox keys, final AutoCompletingComboBox values, List<JosmAction> tagsActions, int tagsToShow, final FocusAdapter focus) {
        if (tagsToShow > 0 && !recentTags.isEmpty()) {
            p.add(new JLabel(tr("Recently added tags")), GBC.eol());
            
            int count = 1;
            // We store the maximum number (9) of recent tags to allow dynamic change of number of tags shown in the preferences.
            // This implies to iterate in descending order, as the oldest elements will only be removed after we reach the maximum numbern and not the number of tags to show.
            // However, as Set does not allow to iterate in descending order, we need to copy its elements into a List we can access in reverse order.
            List<Tag> tags = new LinkedList<Tag>(recentTags.keySet());
            for (int i = tags.size()-1; i >= 0 && count <= tagsToShow; i--, count++) {
                final Tag t = tags.get(i);
                // Create action for reusing the tag, with keyboard shortcut Ctrl+(1-5)
                String actionShortcutKey = "properties:recent:"+count;
                Shortcut sc = Shortcut.registerShortcut(actionShortcutKey, null, KeyEvent.VK_0+count, Shortcut.CTRL);
                final JosmAction action = new JosmAction(actionShortcutKey, null, tr("Use this tag again"), sc, false) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        keys.setSelectedItem(t.getKey());
                        values.setSelectedItem(t.getValue());
                        // Update list of values (fix #7951) 
                        focus.focusGained(null);
                    }
                };
                tagsActions.add(action);
                // Disable action if its key is already set on the object (the key being absent from the keys list for this reason
                // performing this action leads to autocomplete to the next key (see #7671 comments)
                for (int j = 0; j < propertyData.getRowCount(); ++j) {
                    if (t.getKey().equals(propertyData.getValueAt(j, 0))) {
                        action.setEnabled(false);
                        break;
                    }
                }
                // Find and display icon
                ImageIcon icon = MapPaintStyles.getNodeIcon(t, false); // Filters deprecated icon
                if (icon == null) {
                    icon = new ImageIcon(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
                }
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.ipadx = 5;
                p.add(new JLabel(action.isEnabled() ? icon : GuiHelper.getDisabledIcon(icon)), gbc);
                // Create tag label
                final String color = action.isEnabled() ? "" : "; color:gray";
                final JLabel tagLabel = new JLabel("<html>"
                    + "<style>td{border:1px solid gray; font-weight:normal"+color+"}</style>" 
                    + "<table><tr><td>" + t.toString() + "</td></tr></table></html>");
                if (action.isEnabled()) {
                    // Register action
                    p.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(sc.getKeyStroke(), actionShortcutKey);
                    p.getActionMap().put(actionShortcutKey, action);
                    // Make the tag label clickable and set tooltip to the action description (this displays also the keyboard shortcut)
                    tagLabel.setToolTipText((String) action.getValue(Action.SHORT_DESCRIPTION));
                    tagLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    tagLabel.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            action.actionPerformed(null);
                        }
                    });
                } else {
                    // Disable tag label
                    tagLabel.setEnabled(false);
                    // Explain in the tooltip why
                    tagLabel.setToolTipText(tr("The key ''{0}'' is already used", t.getKey()));
                }
                // Finally add label to the resulting panel
                JPanel tagPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
                tagPanel.add(tagLabel);
                p.add(tagPanel, GBC.eol().fill(GBC.HORIZONTAL));
            }
        }
    }

    /**
     * Create a focus handling adapter and apply in to the editor component of value
     * autocompletion box.
     * @param keys Box for keys entering and autocompletion
     * @param values Box for values entering and autocompletion
     * @param autocomplete Manager handling the autocompletion
     * @param comparator Class to decide what values are offered on autocompletion
     * @return The created adapter
     */
    private FocusAdapter addFocusAdapter(final AutoCompletingComboBox keys, final AutoCompletingComboBox values,
            final AutoCompletionManager autocomplete, final Comparator<AutoCompletionListItem> comparator) {
        // get the combo box' editor component
        JTextComponent editor = (JTextComponent)values.getEditor()
                .getEditorComponent();
        // Refresh the values model when focus is gained
        FocusAdapter focus = new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                String key = keys.getEditor().getItem().toString();

                List<AutoCompletionListItem> valueList = autocomplete.getValues(getAutocompletionKeys(key));
                Collections.sort(valueList, comparator);

                values.setPossibleACItems(valueList);
                objKey=key;
            }
        };
        editor.addFocusListener(focus);
        return focus;
    }
    
        /**
     * Edit the value in the properties table row
     * @param row The row of the table from which the value is edited.
     */
    public void editProperty(final int row) {
        changedKey = null;
        Collection<OsmPrimitive> sel = Main.main.getCurrentDataSet().getSelected();
        if (sel.isEmpty()) return;

        String key = propertyData.getValueAt(row, 0).toString();
        objKey=key;

        String msg = "<html>"+trn("This will change {0} object.",
                "This will change up to {0} objects.", sel.size(), sel.size())
                +"<br><br>("+tr("An empty value deletes the tag.", key)+")</html>";

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel(msg), BorderLayout.NORTH);

        JPanel p = new JPanel(new GridBagLayout());
        panel.add(p, BorderLayout.CENTER);

        AutoCompletionManager autocomplete = Main.main.getEditLayer().data.getAutoCompletionManager();
        List<AutoCompletionListItem> keyList = autocomplete.getKeys();
        Collections.sort(keyList, defaultACItemComparator);

        final AutoCompletingComboBox keys = new AutoCompletingComboBox(key);
        keys.setPossibleACItems(keyList);
        keys.setEditable(true);
        keys.setSelectedItem(key);

        p.add(new JLabel(tr("Key")), GBC.std());
        p.add(Box.createHorizontalStrut(10), GBC.std());
        p.add(keys, GBC.eol().fill(GBC.HORIZONTAL));

        @SuppressWarnings("unchecked")
        final Map<String, Integer> m = (Map<String, Integer>) propertyData.getValueAt(row, 1);

        Comparator<AutoCompletionListItem> usedValuesAwareComparator = new Comparator<AutoCompletionListItem>() {

            @Override
            public int compare(AutoCompletionListItem o1, AutoCompletionListItem o2) {
                boolean c1 = m.containsKey(o1.getValue());
                boolean c2 = m.containsKey(o2.getValue());
                if (c1 == c2)
                    return String.CASE_INSENSITIVE_ORDER.compare(o1.getValue(), o2.getValue());
                else if (c1)
                    return -1;
                else
                    return +1;
            }
        };

        List<AutoCompletionListItem> valueList = autocomplete.getValues(getAutocompletionKeys(key));
        Collections.sort(valueList, usedValuesAwareComparator);

        final String selection= m.size()!=1?tr("<different>"):m.entrySet().iterator().next().getKey();
        
        final AutoCompletingComboBox values = new AutoCompletingComboBox(selection);
        values.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList list,
                    Object value, int index, boolean isSelected,  boolean cellHasFocus){
                Component c = super.getListCellRendererComponent(list, value,
                        index, isSelected, cellHasFocus);
                if (c instanceof JLabel) {
                    String str = ((AutoCompletionListItem) value).getValue();
                    if (valueCount.containsKey(objKey)) {
                        Map<String, Integer> m = valueCount.get(objKey);
                        if (m.containsKey(str)) {
                            str = tr("{0} ({1})", str, m.get(str));
                            c.setFont(c.getFont().deriveFont(Font.ITALIC + Font.BOLD));
                        }
                    }
                    ((JLabel) c).setText(str);
                }
                return c;
            }
        });
        
        values.setEditable(true);
        values.setPossibleACItems(valueList);
        values.setSelectedItem(selection);
        values.getEditor().setItem(selection);
        p.add(new JLabel(tr("Value")), GBC.std());
        p.add(Box.createHorizontalStrut(10), GBC.std());
        p.add(values, GBC.eol().fill(GBC.HORIZONTAL));
        addFocusAdapter(keys, values, autocomplete, usedValuesAwareComparator);

        final JOptionPane optionPane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION) {
            @Override public void selectInitialValue() {
                // save unix system selection (middle mouse paste)
                Clipboard sysSel = Toolkit.getDefaultToolkit().getSystemSelection();
                if(sysSel != null) {
                    Transferable old = sysSel.getContents(null);
                    values.requestFocusInWindow();
                    values.getEditor().selectAll();
                    sysSel.setContents(old, null);
                } else {
                    values.requestFocusInWindow();
                    values.getEditor().selectAll();
                }
            }
        };
        final JDialog dlg = optionPane.createDialog(Main.parent, trn("Change value?", "Change values?", m.size()));
        dlg.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        Dimension dlgSize = dlg.getSize();
        if(dlgSize.width > Main.parent.getSize().width) {
            dlgSize.width = Math.max(250, Main.parent.getSize().width);
            dlg.setSize(dlgSize);
        }
        dlg.setLocationRelativeTo(Main.parent);
        values.getEditor().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                dlg.setVisible(false);
                optionPane.setValue(JOptionPane.OK_OPTION);
            }
        });

        String oldValue = values.getEditor().getItem().toString();
        dlg.setVisible(true);

        Object answer = optionPane.getValue();
        if (answer == null || answer == JOptionPane.UNINITIALIZED_VALUE ||
                (answer instanceof Integer && (Integer)answer != JOptionPane.OK_OPTION)) {
            values.getEditor().setItem(oldValue);
            return;
        }

        String value = values.getEditor().getItem().toString().trim();
        // is not Java 1.5
        //value = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFC);
        if (value.equals("")) {
            value = null; // delete the key
        }
        String newkey = keys.getEditor().getItem().toString().trim();
        //newkey = java.text.Normalizer.normalize(newkey, java.text.Normalizer.Form.NFC);
        if (newkey.equals("")) {
            newkey = key;
            value = null; // delete the key instead
        }
        if (key.equals(newkey) && tr("<different>").equals(value))
            return;
        if (key.equals(newkey) || value == null) {
            Main.main.undoRedo.add(new ChangePropertyCommand(sel, newkey, value));
        } else {
            for (OsmPrimitive osm: sel) {
                if(osm.get(newkey) != null) {
                    ExtendedDialog ed = new ExtendedDialog(
                            Main.parent,
                            tr("Overwrite key"),
                            new String[]{tr("Replace"), tr("Cancel")});
                    ed.setButtonIcons(new String[]{"purge", "cancel"});
                    ed.setContent(tr("You changed the key from ''{0}'' to ''{1}''.\n"
                            + "The new key is already used, overwrite values?", key, newkey));
                    ed.setCancelButton(2);
                    ed.toggleEnable("overwriteEditKey");
                    ed.showDialog();

                    if (ed.getValue() != 1)
                        return;
                    break;
                }
            }
            Collection<Command> commands=new Vector<Command>();
            commands.add(new ChangePropertyCommand(sel, key, null));
            if (value.equals(tr("<different>"))) {
                HashMap<String, Vector<OsmPrimitive>> map=new HashMap<String, Vector<OsmPrimitive>>();
                for (OsmPrimitive osm: sel) {
                    String val=osm.get(key);
                    if(val != null)
                    {
                        if (map.containsKey(val)) {
                            map.get(val).add(osm);
                        } else {
                            Vector<OsmPrimitive> v = new Vector<OsmPrimitive>();
                            v.add(osm);
                            map.put(val, v);
                        }
                    }
                }
                for (Map.Entry<String, Vector<OsmPrimitive>> e: map.entrySet()) {
                    commands.add(new ChangePropertyCommand(e.getValue(), newkey, e.getKey()));
                }
            } else {
                commands.add(new ChangePropertyCommand(sel, newkey, value));
            }
            Main.main.undoRedo.add(new SequenceCommand(
                    trn("Change properties of up to {0} object",
                            "Change properties of up to {0} objects", sel.size(), sel.size()),
                            commands));
        }
        
        changedKey = newkey;
    }
    
    /**
     * If during last editProperty call user changed the key name, this key will be returned
     * Elsewhere, returns null.
     */
    public String getChangedKey() {
        return changedKey;
    }
    
    public void resetChangedKey() {
        changedKey = null;
    }
            
    /**
     * For a given key k, return a list of keys which are used as keys for
     * auto-completing values to increase the search space.
     * @param key the key k
     * @return a list of keys
     */
    private static List<String> getAutocompletionKeys(String key) {
        if ("name".equals(key) || "addr:street".equals(key))
            return Arrays.asList("addr:street", "name");
        else
            return Arrays.asList(key);
    }
    

}
    
   