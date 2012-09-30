// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
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
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.mapmode.DrawAction;
import org.openstreetmap.josm.actions.search.SearchAction.SearchMode;
import org.openstreetmap.josm.actions.search.SearchAction.SearchSetting;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListenerAdapter;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager;
import org.openstreetmap.josm.data.osm.event.DatasetEventManager.FireMode;
import org.openstreetmap.josm.data.osm.event.SelectionEventManager;
import org.openstreetmap.josm.gui.DefaultNameFormatter;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.SideButton;
import org.openstreetmap.josm.gui.dialogs.ToggleDialog;
import org.openstreetmap.josm.gui.dialogs.properties.PresetListPanel.PresetHandler;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationMemberTask;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.PresetType;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingComboBox;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionListItem;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.InputMapUtils;
import org.openstreetmap.josm.tools.LanguageInfo;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * This dialog displays the properties of the current selected primitives.
 *
 * If no object is selected, the dialog list is empty.
 * If only one is selected, all properties of this object are selected.
 * If more than one object are selected, the sum of all properties are displayed. If the
 * different objects share the same property, the shared value is displayed. If they have
 * different values, all of them are put in a combo box and the string "&lt;different&gt;"
 * is displayed in italic.
 *
 * Below the list, the user can click on an add, modify and delete property button to
 * edit the table selection value.
 *
 * The command is applied to all selected entries.
 *
 * @author imi
 */
public class PropertiesDialog extends ToggleDialog implements SelectionChangedListener, MapView.EditLayerChangeListener, DataSetListenerAdapter.Listener {
    /**
     * Watches for mouse clicks
     * @author imi
     */
    public class MouseClickWatch extends MouseAdapter {
        @Override public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() < 2)
            {
                // single click, clear selection in other table not clicked in
                if (e.getSource() == propertyTable) {
                    membershipTable.clearSelection();
                } else if (e.getSource() == membershipTable) {
                    propertyTable.clearSelection();
                }
            }
            // double click, edit or add property
            else if (e.getSource() == propertyTable)
            {
                int row = propertyTable.rowAtPoint(e.getPoint());
                if (row > -1) {
                    editProperty(row);
                } else {
                    addProperty();
                }
            } else if (e.getSource() == membershipTable) {
                int row = membershipTable.rowAtPoint(e.getPoint());
                if (row > -1) {
                    editMembership(row);
                }
            }
            else
            {
                addProperty();
            }
        }
        @Override public void mousePressed(MouseEvent e) {
            if (e.getSource() == propertyTable) {
                membershipTable.clearSelection();
            } else if (e.getSource() == membershipTable) {
                propertyTable.clearSelection();
            }
        }
    }

    // hook for roadsigns plugin to display a small
    // button in the upper right corner of this dialog
    public static final JPanel pluginHook = new JPanel();

    private JPopupMenu propertyMenu;
    private JPopupMenu membershipMenu;

    private final Map<String, Map<String, Integer>> valueCount = new TreeMap<String, Map<String, Integer>>();

    Comparator<AutoCompletionListItem> defaultACItemComparator = new Comparator<AutoCompletionListItem>() {
        public int compare(AutoCompletionListItem o1, AutoCompletionListItem o2) {
            return String.CASE_INSENSITIVE_ORDER.compare(o1.getValue(), o2.getValue());
        }
    };

    private final DataSetListenerAdapter dataChangedAdapter = new DataSetListenerAdapter(this);
    private final HelpAction helpAction = new HelpAction();
    private final CopyValueAction copyValueAction = new CopyValueAction();
    private final CopyKeyValueAction copyKeyValueAction = new CopyKeyValueAction();
    private final CopyAllKeyValueAction copyAllKeyValueAction = new CopyAllKeyValueAction();
    private final SearchAction searchActionSame = new SearchAction(true);
    private final SearchAction searchActionAny = new SearchAction(false);
    private final AddAction addAction = new AddAction();
    private final EditAction editAction = new EditAction();
    private final DeleteAction deleteAction = new DeleteAction();
    private final JosmAction[] josmActions = new JosmAction[]{addAction, editAction, deleteAction};

    @Override
    public void showNotify() {
        DatasetEventManager.getInstance().addDatasetListener(dataChangedAdapter, FireMode.IN_EDT_CONSOLIDATED);
        SelectionEventManager.getInstance().addSelectionListener(this, FireMode.IN_EDT_CONSOLIDATED);
        MapView.addEditLayerChangeListener(this);
        for (JosmAction action : josmActions) {
            Main.registerActionShortcut(action);
        }
        updateSelection();
    }

    @Override
    public void hideNotify() {
        DatasetEventManager.getInstance().removeDatasetListener(dataChangedAdapter);
        SelectionEventManager.getInstance().removeSelectionListener(this);
        MapView.removeEditLayerChangeListener(this);
        for (JosmAction action : josmActions) {
            Main.unregisterActionShortcut(action);
        }
    }

    /**
     * Edit the value in the properties table row
     * @param row The row of the table from which the value is edited.
     */
    @SuppressWarnings("unchecked")
    private void editProperty(int row) {
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
        dlg.setModalityType(ModalityType.DOCUMENT_MODAL);
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
                for (Entry<String, Vector<OsmPrimitive>> e: map.entrySet()) {
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

        if(!key.equals(newkey)) {
            for(int i=0; i < propertyTable.getRowCount(); i++)
                if(propertyData.getValueAt(i, 0).toString().equals(newkey)) {
                    row=i;
                    break;
                }
        }
        propertyTable.changeSelection(row, 0, false, false);
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

    /**
     * This simply fires up an {@link RelationEditor} for the relation shown; everything else
     * is the editor's business.
     *
     * @param row
     */
    private void editMembership(int row) {
        Relation relation = (Relation)membershipData.getValueAt(row, 0);
        Main.map.relationListDialog.selectRelation(relation);
        RelationEditor.getEditor(
                Main.map.mapView.getEditLayer(),
                relation,
                ((MemberInfo) membershipData.getValueAt(row, 1)).role).setVisible(true);
    }

    private static String lastAddKey = null;
    private static String lastAddValue = null;
    
    public static final int DEFAULT_LRU_TAGS_NUMBER = 5;
    public static final int MAX_LRU_TAGS_NUMBER = 9;
    
    // LRU cache for recently added tags (http://java-planet.blogspot.com/2005/08/how-to-set-up-simple-lru-cache-using.html) 
    private static final Map<Tag, Void> recentTags = new LinkedHashMap<Tag, Void>(MAX_LRU_TAGS_NUMBER+1, 1.1f, true) {
        @Override
        protected boolean removeEldestEntry(Entry<Tag, Void> eldest) {
            return size() > MAX_LRU_TAGS_NUMBER;
        }
    };
    
    /**
     * Open the add selection dialog and add a new key/value to the table (and
     * to the dataset, of course).
     */
    private void addProperty() {
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
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
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
        btnAdd.requestFocusInWindow();
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
    private String objKey;

    /**
     * The property data of selected objects.
     */
    private final DefaultTableModel propertyData = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int column) {
            return false;
        }
        @Override public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
    };

    /**
     * The membership data of selected objects.
     */
    private final DefaultTableModel membershipData = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int column) {
            return false;
        }
        @Override public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
    };

    /**
     * The properties table.
     */
    private final JTable propertyTable = new JTable(propertyData);
    /**
     * The membership table.
     */
    private final JTable membershipTable = new JTable(membershipData);

    /**
     * The Add button (needed to be able to disable it)
     */
    private final SideButton btnAdd;
    /**
     * The Edit button (needed to be able to disable it)
     */
    private final SideButton btnEdit;
    /**
     * The Delete button (needed to be able to disable it)
     */
    private final SideButton btnDel;
    /**
     * Matching preset display class
     */
    private final PresetListPanel presets = new PresetListPanel();

    /**
     * Text to display when nothing selected.
     */
    private final JLabel selectSth = new JLabel("<html><p>"
            + tr("Select objects for which to change properties.") + "</p></html>");

    static class MemberInfo {
        List<RelationMember> role = new ArrayList<RelationMember>();
        List<Integer> position = new ArrayList<Integer>();
        private String positionString = null;
        void add(RelationMember r, Integer p) {
            role.add(r);
            position.add(p);
        }
        String getPositionString() {
            if (positionString == null) {
                Collections.sort(position);
                positionString = String.valueOf(position.get(0));
                int cnt = 0;
                int last = position.get(0);
                for (int i = 1; i < position.size(); ++i) {
                    int cur = position.get(i);
                    if (cur == last + 1) {
                        ++cnt;
                    } else if (cnt == 0) {
                        positionString += "," + String.valueOf(cur);
                    } else {
                        positionString += "-" + String.valueOf(last);
                        positionString += "," + String.valueOf(cur);
                        cnt = 0;
                    }
                    last = cur;
                }
                if (cnt >= 1) {
                    positionString += "-" + String.valueOf(last);
                }
            }
            if (positionString.length() > 20) {
                positionString = positionString.substring(0, 17) + "...";
            }
            return positionString;
        }
    }

    /**
     * Create a new PropertiesDialog
     */
    public PropertiesDialog(MapFrame mapFrame) {
        super(tr("Properties/Memberships"), "propertiesdialog", tr("Properties for selected objects."),
                Shortcut.registerShortcut("subwindow:properties", tr("Toggle: {0}", tr("Properties/Memberships")), KeyEvent.VK_P,
                        Shortcut.ALT_SHIFT), 150, true);

        // setting up the properties table
        propertyMenu = new JPopupMenu();
        propertyMenu.add(copyValueAction);
        propertyMenu.add(copyKeyValueAction);
        propertyMenu.add(copyAllKeyValueAction);
        propertyMenu.addSeparator();
        propertyMenu.add(searchActionAny);
        propertyMenu.add(searchActionSame);
        propertyMenu.addSeparator();
        propertyMenu.add(helpAction);

        propertyData.setColumnIdentifiers(new String[]{tr("Key"),tr("Value")});
        propertyTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        propertyTable.getTableHeader().setReorderingAllowed(false);
        propertyTable.addMouseListener(new PopupMenuLauncher() {
            @Override
            public void launch(MouseEvent evt) {
                Point p = evt.getPoint();
                int row = propertyTable.rowAtPoint(p);
                if (row > -1) {
                    propertyTable.changeSelection(row, 0, false, false);
                    propertyMenu.show(propertyTable, p.x, p.y-3);
                }
            }
        });

        propertyTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer(){
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                if (value == null)
                    return this;
                if (c instanceof JLabel) {
                    String str = null;
                    if (value instanceof String) {
                        str = (String) value;
                    } else if (value instanceof Map<?, ?>) {
                        Map<?, ?> v = (Map<?, ?>) value;
                        if (v.size() != 1) {
                            str=tr("<different>");
                            c.setFont(c.getFont().deriveFont(Font.ITALIC));
                        } else {
                            final Map.Entry<?, ?> entry = v.entrySet().iterator().next();
                            str = (String) entry.getKey();
                        }
                    }
                    ((JLabel)c).setText(str);
                }
                return c;
            }
        });

        // setting up the membership table
        membershipMenu = new JPopupMenu();
        membershipMenu.add(new SelectRelationAction(true));
        membershipMenu.add(new SelectRelationAction(false));
        membershipMenu.add(new SelectRelationMembersAction());
        membershipMenu.add(new DownloadIncompleteMembersAction());
        membershipMenu.addSeparator();
        membershipMenu.add(helpAction);

        membershipData.setColumnIdentifiers(new String[]{tr("Member Of"),tr("Role"),tr("Position")});
        membershipTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        membershipTable.addMouseListener(new PopupMenuLauncher() {
            @Override
            public void launch(MouseEvent evt) {
                Point p = evt.getPoint();
                int row = membershipTable.rowAtPoint(p);
                if (row > -1) {
                    membershipTable.changeSelection(row, 0, false, false);
                    Relation relation = (Relation)membershipData.getValueAt(row, 0);
                    for (Component c : membershipMenu.getComponents()) {
                        if (c instanceof JMenuItem) {
                            Action action = ((JMenuItem) c).getAction();
                            if (action instanceof RelationRelated) {
                                ((RelationRelated)action).setRelation(relation);
                            }
                        }
                    }
                    membershipMenu.show(membershipTable, p.x, p.y-3);
                }
            }
        });

        TableColumnModel mod = membershipTable.getColumnModel();
        membershipTable.getTableHeader().setReorderingAllowed(false);
        mod.getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                if (value == null)
                    return this;
                if (c instanceof JLabel) {
                    JLabel label = (JLabel)c;
                    Relation r = (Relation)value;
                    label.setText(r.getDisplayName(DefaultNameFormatter.getInstance()));
                    if (r.isDisabledAndHidden()) {
                        label.setFont(label.getFont().deriveFont(Font.ITALIC));
                    }
                }
                return c;
            }
        });

        mod.getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                if (value == null)
                    return this;
                Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                boolean isDisabledAndHidden = (((Relation)table.getValueAt(row, 0))).isDisabledAndHidden();
                if (c instanceof JLabel) {
                    JLabel label = (JLabel)c;
                    MemberInfo col = (MemberInfo) value;

                    String text = null;
                    for (RelationMember r : col.role) {
                        if (text == null) {
                            text = r.getRole();
                        }
                        else if (!text.equals(r.getRole())) {
                            text = tr("<different>");
                            break;
                        }
                    }

                    label.setText(text);
                    if (isDisabledAndHidden) {
                        label.setFont(label.getFont().deriveFont(Font.ITALIC));
                    }
                }
                return c;
            }
        });

        mod.getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                boolean isDisabledAndHidden = (((Relation)table.getValueAt(row, 0))).isDisabledAndHidden();
                if (c instanceof JLabel) {
                    JLabel label = (JLabel)c;
                    label.setText(((MemberInfo) table.getValueAt(row, 1)).getPositionString());
                    if (isDisabledAndHidden) {
                        label.setFont(label.getFont().deriveFont(Font.ITALIC));
                    }
                }
                return c;
            }
        });
        mod.getColumn(2).setPreferredWidth(20);
        mod.getColumn(1).setPreferredWidth(40);
        mod.getColumn(0).setPreferredWidth(200);

        // combine both tables and wrap them in a scrollPane
        JPanel bothTables = new JPanel();
        boolean top = Main.pref.getBoolean("properties.presets.top", true);
        bothTables.setLayout(new GridBagLayout());
        if(top) {
            bothTables.add(presets, GBC.std().fill(GBC.HORIZONTAL).insets(5, 2, 5, 2).anchor(GBC.NORTHWEST));
            double epsilon = Double.MIN_VALUE; // need to set a weight or else anchor value is ignored
            bothTables.add(pluginHook, GBC.eol().insets(0,1,1,1).anchor(GBC.NORTHEAST).weight(epsilon, epsilon));
        }
        bothTables.add(selectSth, GBC.eol().fill().insets(10, 10, 10, 10));
        bothTables.add(propertyTable.getTableHeader(), GBC.eol().fill(GBC.HORIZONTAL));
        bothTables.add(propertyTable, GBC.eol().fill(GBC.BOTH));
        bothTables.add(membershipTable.getTableHeader(), GBC.eol().fill(GBC.HORIZONTAL));
        bothTables.add(membershipTable, GBC.eol().fill(GBC.BOTH));
        if(!top) {
            bothTables.add(presets, GBC.eol().fill(GBC.HORIZONTAL).insets(5, 2, 5, 2));
        }
        
        // Open edit dialog whe enter pressed in tables
        propertyTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),"onTableEnter");
        propertyTable.getActionMap().put("onTableEnter",editAction);
        membershipTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),"onTableEnter");
        membershipTable.getActionMap().put("onTableEnter",editAction);
        
        // Open add property dialog when INS is pressed in tables
        propertyTable.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_INSERT, 0),"onTableInsert");
        propertyTable.getActionMap().put("onTableInsert",addAction);
        
        //  unassign some standard shortcuts for JTable to allow upload / download
        InputMapUtils.unassignCtrlShiftUpDown(propertyTable, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        
        // -- add action and shortcut
        this.btnAdd = new SideButton(addAction);
        InputMapUtils.enableEnter(this.btnAdd);

        // -- edit action
        //
        propertyTable.getSelectionModel().addListSelectionListener(editAction);
        membershipTable.getSelectionModel().addListSelectionListener(editAction);
        this.btnEdit = new SideButton(editAction);

        // -- delete action
        //
        this.btnDel = new SideButton(deleteAction);
        membershipTable.getSelectionModel().addListSelectionListener(deleteAction);
        propertyTable.getSelectionModel().addListSelectionListener(deleteAction);
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),"delete"
                );
        getActionMap().put("delete", deleteAction);

        JScrollPane scrollPane = (JScrollPane) createLayout(bothTables, true, Arrays.asList(new SideButton[] {
                this.btnAdd, this.btnEdit, this.btnDel
        }));

        MouseClickWatch mouseClickWatch = new MouseClickWatch();
        propertyTable.addMouseListener(mouseClickWatch);
        membershipTable.addMouseListener(mouseClickWatch);
        scrollPane.addMouseListener(mouseClickWatch);

        selectSth.setPreferredSize(scrollPane.getSize());
        presets.setSize(scrollPane.getSize());

        // -- help action
        //
        getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "onHelp");
        getActionMap().put("onHelp", helpAction);
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);
        if (b && Main.main.getCurrentDataSet() != null) {
            selectionChanged(Main.main.getCurrentDataSet().getSelected());
        }
    }

    private int findRow(TableModel model, Object value) {
        for (int i=0; i<model.getRowCount(); i++) {
            if (model.getValueAt(i, 0).equals(value))
                return i;
        }
        return -1;
    }

    private PresetHandler presetHandler = new PresetHandler() {

        @Override
        public void updateTags(List<Tag> tags) {
            Command command = TaggingPreset.createCommand(getSelection(), tags);
            if (command != null) {
                Main.main.undoRedo.add(command);
            }
        }

        @Override
        public Collection<OsmPrimitive> getSelection() {
            if (Main.main == null) return null;
            if (Main.main.getCurrentDataSet() == null) return null;

            return Main.main.getCurrentDataSet().getSelected();
        }
    };

    @Override
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        if (!isVisible())
            return;
        if (propertyTable == null)
            return; // selection changed may be received in base class constructor before init
        if (propertyTable.getCellEditor() != null) {
            propertyTable.getCellEditor().cancelCellEditing();
        }

        String selectedTag = null;
        Relation selectedRelation = null;
        if (propertyTable.getSelectedRowCount() == 1) {
            selectedTag = (String)propertyData.getValueAt(propertyTable.getSelectedRow(), 0);
        }
        if (membershipTable.getSelectedRowCount() == 1) {
            selectedRelation = (Relation)membershipData.getValueAt(membershipTable.getSelectedRow(), 0);
        }

        // re-load property data
        propertyData.setRowCount(0);

        final Map<String, Integer> keyCount = new HashMap<String, Integer>();
        final Map<String, String> tags = new HashMap<String, String>();
        valueCount.clear();
        EnumSet<PresetType> types = EnumSet.noneOf(TaggingPreset.PresetType.class);
        for (OsmPrimitive osm : newSelection) {
            types.add(PresetType.forPrimitive(osm));
            for (String key : osm.keySet()) {
                String value = osm.get(key);
                keyCount.put(key, keyCount.containsKey(key) ? keyCount.get(key) + 1 : 1);
                if (valueCount.containsKey(key)) {
                    Map<String, Integer> v = valueCount.get(key);
                    v.put(value, v.containsKey(value) ? v.get(value) + 1 : 1);
                } else {
                    TreeMap<String, Integer> v = new TreeMap<String, Integer>();
                    v.put(value, 1);
                    valueCount.put(key, v);
                }
            }
        }
        for (Entry<String, Map<String, Integer>> e : valueCount.entrySet()) {
            int count = 0;
            for (Entry<String, Integer> e1 : e.getValue().entrySet()) {
                count += e1.getValue();
            }
            if (count < newSelection.size()) {
                e.getValue().put("", newSelection.size() - count);
            }
            propertyData.addRow(new Object[]{e.getKey(), e.getValue()});
            tags.put(e.getKey(), e.getValue().size() == 1
                    ? e.getValue().keySet().iterator().next() : tr("<different>"));
        }

        membershipData.setRowCount(0);

        Map<Relation, MemberInfo> roles = new HashMap<Relation, MemberInfo>();
        for (OsmPrimitive primitive: newSelection) {
            for (OsmPrimitive ref: primitive.getReferrers()) {
                if (ref instanceof Relation && !ref.isIncomplete() && !ref.isDeleted()) {
                    Relation r = (Relation) ref;
                    MemberInfo mi = roles.get(r);
                    if(mi == null) {
                        mi = new MemberInfo();
                    }
                    roles.put(r, mi);
                    int i = 1;
                    for (RelationMember m : r.getMembers()) {
                        if (m.getMember() == primitive) {
                            mi.add(m, i);
                        }
                        ++i;
                    }
                }
            }
        }

        List<Relation> sortedRelations = new ArrayList<Relation>(roles.keySet());
        Collections.sort(sortedRelations, new Comparator<Relation>() {
            public int compare(Relation o1, Relation o2) {
                int comp = Boolean.valueOf(o1.isDisabledAndHidden()).compareTo(o2.isDisabledAndHidden());
                if (comp == 0) {
                    comp = o1.getDisplayName(DefaultNameFormatter.getInstance()).compareTo(o2.getDisplayName(DefaultNameFormatter.getInstance()));
                }
                return comp;
            }}
                );

        for (Relation r: sortedRelations) {
            membershipData.addRow(new Object[]{r, roles.get(r)});
        }

        presets.updatePresets(types, tags, presetHandler);

        membershipTable.getTableHeader().setVisible(membershipData.getRowCount() > 0);
        membershipTable.setVisible(membershipData.getRowCount() > 0);

        boolean hasSelection = !newSelection.isEmpty();
        boolean hasTags = hasSelection && propertyData.getRowCount() > 0;
        boolean hasMemberships = hasSelection && membershipData.getRowCount() > 0;
        btnAdd.setEnabled(hasSelection);
        btnEdit.setEnabled(hasTags || hasMemberships);
        btnDel.setEnabled(hasTags || hasMemberships);
        propertyTable.setVisible(hasTags);
        propertyTable.getTableHeader().setVisible(hasTags);
        selectSth.setVisible(!hasSelection);
        pluginHook.setVisible(hasSelection);

        int selectedIndex;
        if (selectedTag != null && (selectedIndex = findRow(propertyData, selectedTag)) != -1) {
            propertyTable.changeSelection(selectedIndex, 0, false, false);
        } else if (selectedRelation != null && (selectedIndex = findRow(membershipData, selectedRelation)) != -1) {
            membershipTable.changeSelection(selectedIndex, 0, false, false);
        } else if(hasTags) {
            propertyTable.changeSelection(0, 0, false, false);
        } else if(hasMemberships) {
            membershipTable.changeSelection(0, 0, false, false);
        }

        if(propertyData.getRowCount() != 0 || membershipData.getRowCount() != 0) {
            setTitle(tr("Properties: {0} / Memberships: {1}",
                    propertyData.getRowCount(), membershipData.getRowCount()));
        } else {
            setTitle(tr("Properties / Memberships"));
        }
    }

    /**
     * Update selection status, call @{link #selectionChanged} function.
     */
    private void updateSelection() {
        if (Main.main.getCurrentDataSet() == null) {
            selectionChanged(Collections.<OsmPrimitive>emptyList());
        } else {
            selectionChanged(Main.main.getCurrentDataSet().getSelected());
        }
    }

    /* ---------------------------------------------------------------------------------- */
    /* EditLayerChangeListener                                                            */
    /* ---------------------------------------------------------------------------------- */
    @Override
    public void editLayerChanged(OsmDataLayer oldLayer, OsmDataLayer newLayer) {
        updateSelection();
    }

    @Override
    public void processDatasetEvent(AbstractDatasetChangedEvent event) {
        updateSelection();
    }

    /**
     * Action handling delete button press in properties dialog.
     */
    class DeleteAction extends JosmAction implements ListSelectionListener {

        public DeleteAction() {
            super(tr("Delete"), "dialogs/delete", tr("Delete the selected key in all objects"),
                    Shortcut.registerShortcut("properties:delete", tr("Delete Properties"), KeyEvent.VK_D,
                            Shortcut.ALT_CTRL_SHIFT), false);
            updateEnabledState();
        }

        protected void deleteProperties(int[] rows){
            // convert list of rows to HashMap (and find gap for nextKey)
            HashMap<String, String> tags = new HashMap<String, String>(rows.length);
            int nextKeyIndex = rows[0];
            for (int row : rows) {
                String key = propertyData.getValueAt(row, 0).toString();
                if (row == nextKeyIndex + 1) {
                    nextKeyIndex = row; // no gap yet
                }
                tags.put(key, null);
            }

            // find key to select after deleting other properties
            String nextKey = null;
            int rowCount = propertyData.getRowCount();
            if (rowCount > rows.length) {
                if (nextKeyIndex == rows[rows.length-1]) {
                    // no gap found, pick next or previous key in list
                    nextKeyIndex = (nextKeyIndex + 1 < rowCount ? nextKeyIndex + 1 : rows[0] - 1);
                } else {
                    // gap found
                    nextKeyIndex++;
                }
                nextKey = (String)propertyData.getValueAt(nextKeyIndex, 0);
            }

            Collection<OsmPrimitive> sel = Main.main.getCurrentDataSet().getSelected();
            Main.main.undoRedo.add(new ChangePropertyCommand(sel, tags));

            membershipTable.clearSelection();
            if (nextKey != null) {
                propertyTable.changeSelection(findRow(propertyData, nextKey), 0, false, false);
            }
        }

        protected void deleteFromRelation(int row) {
            Relation cur = (Relation)membershipData.getValueAt(row, 0);

            Relation nextRelation = null;
            int rowCount = membershipTable.getRowCount();
            if (rowCount > 1) {
                nextRelation = (Relation)membershipData.getValueAt((row + 1 < rowCount ? row + 1 : row - 1), 0);
            }

            ExtendedDialog ed = new ExtendedDialog(Main.parent,
                    tr("Change relation"),
                    new String[] {tr("Delete from relation"), tr("Cancel")});
            ed.setButtonIcons(new String[] {"dialogs/delete.png", "cancel.png"});
            ed.setContent(tr("Really delete selection from relation {0}?", cur.getDisplayName(DefaultNameFormatter.getInstance())));
            ed.toggleEnable("delete_from_relation");
            ed.showDialog();

            if(ed.getValue() != 1)
                return;

            Relation rel = new Relation(cur);
            Collection<OsmPrimitive> sel = Main.main.getCurrentDataSet().getSelected();
            for (OsmPrimitive primitive: sel) {
                rel.removeMembersFor(primitive);
            }
            Main.main.undoRedo.add(new ChangeCommand(cur, rel));

            propertyTable.clearSelection();
            if (nextRelation != null) {
                membershipTable.changeSelection(findRow(membershipData, nextRelation), 0, false, false);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (propertyTable.getSelectedRowCount() > 0) {
                int[] rows = propertyTable.getSelectedRows();
                deleteProperties(rows);
            } else if (membershipTable.getSelectedRowCount() > 0) {
                int row = membershipTable.getSelectedRow();
                deleteFromRelation(row);
            }
        }

        @Override
        protected void updateEnabledState() {
            setEnabled(
                    (propertyTable != null && propertyTable.getSelectedRowCount() >= 1)
                    || (membershipTable != null && membershipTable.getSelectedRowCount() == 1)
                    );
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    /**
     * Action handling add button press in properties dialog.
     */
    class AddAction extends JosmAction {
        public AddAction() {
            super(tr("Add"), "dialogs/add", tr("Add a new key/value pair to all objects"),
                    Shortcut.registerShortcut("properties:add", tr("Add Property"), KeyEvent.VK_A,
                            Shortcut.ALT), false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            addProperty();
        }
    }

    /**
     * Action handling edit button press in properties dialog.
     */
    class EditAction extends JosmAction implements ListSelectionListener {
        public EditAction() {
            super(tr("Edit"), "dialogs/edit", tr("Edit the value of the selected key for all objects"),
                    Shortcut.registerShortcut("properties:edit", tr("Edit Properties"), KeyEvent.VK_S,
                            Shortcut.ALT), false);
            updateEnabledState();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            if (propertyTable.getSelectedRowCount() == 1) {
                int row = propertyTable.getSelectedRow();
                editProperty(row);
            } else if (membershipTable.getSelectedRowCount() == 1) {
                int row = membershipTable.getSelectedRow();
                editMembership(row);
            }
        }

        @Override
        protected void updateEnabledState() {
            setEnabled(
                    (propertyTable != null && propertyTable.getSelectedRowCount() == 1)
                    ^ (membershipTable != null && membershipTable.getSelectedRowCount() == 1)
                    );
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            updateEnabledState();
        }
    }

    class HelpAction extends AbstractAction {
        public HelpAction() {
            putValue(NAME, tr("Go to OSM wiki for tag help (F1)"));
            putValue(SHORT_DESCRIPTION, tr("Launch browser with wiki help for selected object"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs", "search"));
        }

        public void actionPerformed(ActionEvent e) {
            try {
                String base = Main.pref.get("url.openstreetmap-wiki", "http://wiki.openstreetmap.org/wiki/");
                String lang = LanguageInfo.getWikiLanguagePrefix();
                final List<URI> uris = new ArrayList<URI>();
                int row;
                if (propertyTable.getSelectedRowCount() == 1) {
                    row = propertyTable.getSelectedRow();
                    String key = URLEncoder.encode(propertyData.getValueAt(row, 0).toString(), "UTF-8");
                    String val = URLEncoder.encode(
                            ((Map<String,Integer>)propertyData.getValueAt(row, 1))
                            .entrySet().iterator().next().getKey(), "UTF-8"
                            );

                    uris.add(new URI(String.format("%s%sTag:%s=%s", base, lang, key, val)));
                    uris.add(new URI(String.format("%sTag:%s=%s", base, key, val)));
                    uris.add(new URI(String.format("%s%sKey:%s", base, lang, key)));
                    uris.add(new URI(String.format("%sKey:%s", base, key)));
                    uris.add(new URI(String.format("%s%sMap_Features", base, lang)));
                    uris.add(new URI(String.format("%sMap_Features", base)));
                } else if (membershipTable.getSelectedRowCount() == 1) {
                    row = membershipTable.getSelectedRow();
                    String type = URLEncoder.encode(
                            ((Relation)membershipData.getValueAt(row, 0)).get("type"), "UTF-8"
                            );

                    if (type != null && !type.equals("")) {
                        uris.add(new URI(String.format("%s%sRelation:%s", base, lang, type)));
                        uris.add(new URI(String.format("%sRelation:%s", base, type)));
                    }

                    uris.add(new URI(String.format("%s%sRelations", base, lang)));
                    uris.add(new URI(String.format("%sRelations", base)));
                } else {
                    // give the generic help page, if more than one element is selected
                    uris.add(new URI(String.format("%s%sMap_Features", base, lang)));
                    uris.add(new URI(String.format("%sMap_Features", base)));
                }

                Main.worker.execute(new Runnable(){
                    public void run() {
                        try {
                            // find a page that actually exists in the wiki
                            HttpURLConnection conn;
                            for (URI u : uris) {
                                conn = (HttpURLConnection) u.toURL().openConnection();
                                conn.setConnectTimeout(Main.pref.getInteger("socket.timeout.connect",15)*1000);

                                if (conn.getResponseCode() != 200) {
                                    Main.info("INFO: {0} does not exist", u);
                                    conn.disconnect();
                                } else {
                                    int osize = conn.getContentLength();
                                    conn.disconnect();

                                    conn = (HttpURLConnection) new URI(u.toString()
                                            .replace("=", "%3D") /* do not URLencode whole string! */
                                            .replaceFirst("/wiki/", "/w/index.php?redirect=no&title=")
                                            ).toURL().openConnection();
                                    conn.setConnectTimeout(Main.pref.getInteger("socket.timeout.connect",15)*1000);

                                    /* redirect pages have different content length, but retrieving a "nonredirect"
                                     *  page using index.php and the direct-link method gives slightly different
                                     *  content lengths, so we have to be fuzzy.. (this is UGLY, recode if u know better)
                                     */
                                    if (Math.abs(conn.getContentLength() - osize) > 200) {
                                        Main.info("INFO: {0} is a mediawiki redirect", u);
                                        conn.disconnect();
                                    } else {
                                        Main.info("INFO: browsing to {0}", u);
                                        conn.disconnect();

                                        OpenBrowser.displayUrl(u.toString());
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
    }

    public void addPropertyPopupMenuSeparator() {
        propertyMenu.addSeparator();
    }

    public JMenuItem addPropertyPopupMenuAction(Action a) {
        return propertyMenu.add(a);
    }

    public void addPropertyPopupMenuListener(PopupMenuListener l) {
        propertyMenu.addPopupMenuListener(l);
    }

    public void removePropertyPopupMenuListener(PopupMenuListener l) {
        propertyMenu.addPopupMenuListener(l);
    }

    @SuppressWarnings("unchecked")
    public Tag getSelectedProperty() {
        int row = propertyTable.getSelectedRow();
        if (row == -1) return null;
        TreeMap<String, Integer> map = (TreeMap<String, Integer>) propertyData.getValueAt(row, 1);
        return new Tag(
                propertyData.getValueAt(row, 0).toString(),
                map.size() > 1 ? "" : map.keySet().iterator().next());
    }

    public void addMembershipPopupMenuSeparator() {
        membershipMenu.addSeparator();
    }

    public JMenuItem addMembershipPopupMenuAction(Action a) {
        return membershipMenu.add(a);
    }

    public void addMembershipPopupMenuListener(PopupMenuListener l) {
        membershipMenu.addPopupMenuListener(l);
    }

    public void removeMembershipPopupMenuListener(PopupMenuListener l) {
        membershipMenu.addPopupMenuListener(l);
    }

    public IRelation getSelectedMembershipRelation() {
        int row = membershipTable.getSelectedRow();
        return row > -1 ? (IRelation) membershipData.getValueAt(row, 0) : null;
    }

    public static interface RelationRelated {
        public Relation getRelation();
        public void setRelation(Relation relation);
    }

    static abstract class AbstractRelationAction extends AbstractAction implements RelationRelated {
        protected Relation relation;
        public Relation getRelation() {
            return this.relation;
        }
        public void setRelation(Relation relation) {
            this.relation = relation;
        }
    }

    static class SelectRelationAction extends AbstractRelationAction {
        boolean selectionmode;
        public SelectRelationAction(boolean select) {
            selectionmode = select;
            if(select) {
                putValue(NAME, tr("Select relation"));
                putValue(SHORT_DESCRIPTION, tr("Select relation in main selection."));
                putValue(SMALL_ICON, ImageProvider.get("dialogs", "select"));
            } else {
                putValue(NAME, tr("Select in relation list"));
                putValue(SHORT_DESCRIPTION, tr("Select relation in relation list."));
                putValue(SMALL_ICON, ImageProvider.get("dialogs", "relationlist"));
            }
        }

        public void actionPerformed(ActionEvent e) {
            if(selectionmode) {
                Main.map.mapView.getEditLayer().data.setSelected(relation);
            } else {
                Main.map.relationListDialog.selectRelation(relation);
                Main.map.relationListDialog.unfurlDialog();
            }
        }
    }


    /**
     * Sets the current selection to the members of selected relation
     *
     */
    class SelectRelationMembersAction extends AbstractRelationAction {
        public SelectRelationMembersAction() {
            putValue(SHORT_DESCRIPTION,tr("Select the members of selected relation"));
            putValue(SMALL_ICON, ImageProvider.get("selectall"));
            putValue(NAME, tr("Select members"));
        }

        public void actionPerformed(ActionEvent e) {
            HashSet<OsmPrimitive> members = new HashSet<OsmPrimitive>();
            members.addAll(relation.getMemberPrimitives());
            Main.map.mapView.getEditLayer().data.setSelected(members);
        }

    }

    /**
     * Action for downloading incomplete members of selected relation
     *
     */
    class DownloadIncompleteMembersAction extends AbstractRelationAction {
        public DownloadIncompleteMembersAction() {
            putValue(SHORT_DESCRIPTION, tr("Download incomplete members of selected relations"));
            putValue(SMALL_ICON, ImageProvider.get("dialogs/relation", "downloadincompleteselected"));
            putValue(NAME, tr("Download incomplete members"));
        }

        public Set<OsmPrimitive> buildSetOfIncompleteMembers(Relation r) {
            Set<OsmPrimitive> ret = new HashSet<OsmPrimitive>();
            ret.addAll(r.getIncompleteMembers());
            return ret;
        }

        public void actionPerformed(ActionEvent e) {
            if (!relation.hasIncompleteMembers()) return;
            ArrayList<Relation> rels = new ArrayList<Relation>();
            rels.add(relation);
            Main.worker.submit(new DownloadRelationMemberTask(
                    rels,
                    buildSetOfIncompleteMembers(relation),
                    Main.map.mapView.getEditLayer()
                    ));
        }
    }

    abstract class AbstractCopyAction extends AbstractAction {

        protected abstract Collection<String> getString(OsmPrimitive p, String key);

        @Override
        public void actionPerformed(ActionEvent ae) {
            if (propertyTable.getSelectedRowCount() != 1)
                return;
            String key = propertyData.getValueAt(propertyTable.getSelectedRow(), 0).toString();
            Collection<OsmPrimitive> sel = Main.main.getCurrentDataSet().getSelected();
            if (sel.isEmpty())
                return;
            Set<String> values = new TreeSet<String>();
            for (OsmPrimitive p : sel) {
                Collection<String> s = getString(p,key);
                if (s != null) {
                    values.addAll(s);
                }
            }
            Utils.copyToClipboard(Utils.join("\n", values));
        }
    }

    class CopyValueAction extends AbstractCopyAction {

        public CopyValueAction() {
            putValue(NAME, tr("Copy Value"));
            putValue(SHORT_DESCRIPTION, tr("Copy the value of the selected tag to clipboard"));
        }

        @Override
        protected Collection<String> getString(OsmPrimitive p, String key) {
            String v = p.get(key);
            return v == null ? null : Collections.singleton(v);
        }
    }

    class CopyKeyValueAction extends AbstractCopyAction {

        public CopyKeyValueAction() {
            putValue(NAME, tr("Copy Key/Value"));
            putValue(SHORT_DESCRIPTION, tr("Copy the key and value of the selected tag to clipboard"));
        }

        @Override
        protected Collection<String> getString(OsmPrimitive p, String key) {
            String v = p.get(key);
            return v == null ? null : Collections.singleton(new Tag(key, v).toString());
        }
    }

    class CopyAllKeyValueAction extends AbstractCopyAction {

        public CopyAllKeyValueAction() {
            putValue(NAME, tr("Copy all Keys/Values"));
            putValue(SHORT_DESCRIPTION, tr("Copy the key and value of the all tags to clipboard"));
        }

        @Override
        protected Collection<String> getString(OsmPrimitive p, String key) {
            List<String> r = new LinkedList<String>();
            for (Entry<String, String> kv : p.getKeys().entrySet()) {
                r.add(new Tag(kv.getKey(), kv.getValue()).toString());
            }
            return r;
        }
    }

    class SearchAction extends AbstractAction {
        final boolean sameType;

        public SearchAction(boolean sameType) {
            this.sameType = sameType;
            if (sameType) {
                putValue(NAME, tr("Search Key/Value/Type"));
                putValue(SHORT_DESCRIPTION, tr("Search with the key and value of the selected tag, restrict to type (i.e., node/way/relation)"));
            } else {
                putValue(NAME, tr("Search Key/Value"));
                putValue(SHORT_DESCRIPTION, tr("Search with the key and value of the selected tag"));
            }
        }

        public void actionPerformed(ActionEvent e) {
            if (propertyTable.getSelectedRowCount() != 1)
                return;
            String key = propertyData.getValueAt(propertyTable.getSelectedRow(), 0).toString();
            Collection<OsmPrimitive> sel = Main.main.getCurrentDataSet().getSelected();
            if (sel.isEmpty())
                return;
            String sep = "";
            String s = "";
            for (OsmPrimitive p : sel) {
                String val = p.get(key);
                if (val == null) {
                    continue;
                }
                String t = "";
                if (!sameType) {
                    t = "";
                } else if (p instanceof Node) {
                    t = "type:node ";
                } else if (p instanceof Way) {
                    t = "type:way ";
                } else if (p instanceof Relation) {
                    t = "type:relation ";
                }
                s += sep + "(" + t + "\"" +
                        org.openstreetmap.josm.actions.search.SearchAction.escapeStringForSearch(key) + "\"=\"" +
                        org.openstreetmap.josm.actions.search.SearchAction.escapeStringForSearch(val) + "\")";
                sep = " OR ";
            }

            SearchSetting ss = new SearchSetting(s, SearchMode.replace, true, false, false);
            org.openstreetmap.josm.actions.search.SearchAction.searchWithoutHistory(ss);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        for (JosmAction action : josmActions) {
            action.destroy();
        }
        Container parent = pluginHook.getParent();
        if (parent != null) {
            parent.remove(pluginHook);
        }
    }
}
