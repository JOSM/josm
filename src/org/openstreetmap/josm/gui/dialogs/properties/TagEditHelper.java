// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.text.Normalizer;
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

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingComboBox;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionListItem;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.io.XmlWriter;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.WindowGeometry;

/**
 * Class that helps PropertiesDialog add and edit tag values
 */
 class TagEditHelper {
    private final DefaultTableModel tagData;
    private final Map<String, Map<String, Integer>> valueCount;

    // Selection that we are editing by using both dialogs
    Collection<OsmPrimitive> sel;
    
    private String changedKey;
    private String objKey;

    Comparator<AutoCompletionListItem> defaultACItemComparator = new Comparator<AutoCompletionListItem>() {
        @Override
        public int compare(AutoCompletionListItem o1, AutoCompletionListItem o2) {
            return String.CASE_INSENSITIVE_ORDER.compare(o1.getValue(), o2.getValue());
        }
    };

    private String lastAddKey = null;
    private String lastAddValue = null;

    public static final int DEFAULT_LRU_TAGS_NUMBER = 5;
    public static final int MAX_LRU_TAGS_NUMBER = 30;

    // LRU cache for recently added tags (http://java-planet.blogspot.com/2005/08/how-to-set-up-simple-lru-cache-using.html)
    private final Map<Tag, Void> recentTags = new LinkedHashMap<Tag, Void>(MAX_LRU_TAGS_NUMBER+1, 1.1f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Tag, Void> eldest) {
            return size() > MAX_LRU_TAGS_NUMBER;
        }
    };

    TagEditHelper(DefaultTableModel propertyData, Map<String, Map<String, Integer>> valueCount) {
        this.tagData = propertyData;
        this.valueCount = valueCount;
    }

    /**
     * Open the add selection dialog and add a new key/value to the table (and
     * to the dataset, of course).
     */
    public void addTag() {
        changedKey = null;
        sel = Main.main.getInProgressSelection();
        if (sel == null || sel.isEmpty()) return;

        final AddTagsDialog addDialog = new AddTagsDialog();
        
        addDialog.showDialog();
        
        addDialog.destroyActions();
        if (addDialog.getValue() == 1)
            addDialog.performTagAdding();
        else
            addDialog.undoAllTagsAdding();
    }
    
    /**
    * Edit the value in the tags table row
    * @param row The row of the table from which the value is edited.
    * @param focusOnKey Determines if the initial focus should be set on key instead of value
    * @since 5653
    */
    public void editTag(final int row, boolean focusOnKey) {
        changedKey = null;
        sel = Main.main.getInProgressSelection();
        if (sel == null || sel.isEmpty()) return;

        String key = tagData.getValueAt(row, 0).toString();
        objKey=key;
        
        @SuppressWarnings("unchecked")
        final EditTagDialog editDialog = new EditTagDialog(key, row,
                (Map<String, Integer>) tagData.getValueAt(row, 1), focusOnKey);
        editDialog.showDialog();
        if (editDialog.getValue() !=1 ) return;
        editDialog.performTagEdit();
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

    /**
     * Load recently used tags from preferences if needed
     */
    public void loadTagsIfNeeded() {
        if (PROPERTY_REMEMBER_TAGS.get() && recentTags.isEmpty()) {
            recentTags.clear();
            Collection<String> c = Main.pref.getCollection("properties.recent-tags");
            Iterator<String> it = c.iterator();
            String key, value;
            while (it.hasNext()) {
                key = it.next();
                value = it.next();
                recentTags.put(new Tag(key, value), null);
            }
        }
    }

    /**
     * Store recently used tags in preferences if needed
     */
    public void saveTagsIfNeeded() {
        if (PROPERTY_REMEMBER_TAGS.get() && !recentTags.isEmpty()) {
            List<String> c = new ArrayList<String>( recentTags.size()*2 );
            for (Tag t: recentTags.keySet()) {
                c.add(t.getKey());
                c.add(t.getValue());
            }
            Main.pref.putCollection("properties.recent-tags", c);
        }
    }

    public final class EditTagDialog extends AbstractTagsDialog {
        final String key;
        final Map<String, Integer> m;
        final int row;

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
        
        DefaultListCellRenderer cellRenderer = new DefaultListCellRenderer() {
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
            };
        
        private EditTagDialog(String key, int row, Map<String, Integer> map, final boolean initialFocusOnKey) {
            super(Main.parent, trn("Change value?", "Change values?", map.size()), new String[] {tr("OK"),tr("Cancel")});
            setButtonIcons(new String[] {"ok","cancel"});
            setCancelButton(2);
            configureContextsensitiveHelp("/Dialog/EditValue", true /* show help button */);
            this.key = key;
            this.row = row;
            this.m = map;
            
            JPanel mainPanel = new JPanel(new BorderLayout());
                    
            String msg = "<html>"+trn("This will change {0} object.",
                    "This will change up to {0} objects.", sel.size(), sel.size())
                    +"<br><br>("+tr("An empty value deletes the tag.", key)+")</html>";

            mainPanel.add(new JLabel(msg), BorderLayout.NORTH);

            JPanel p = new JPanel(new GridBagLayout());
            mainPanel.add(p, BorderLayout.CENTER);

            AutoCompletionManager autocomplete = Main.main.getEditLayer().data.getAutoCompletionManager();
            List<AutoCompletionListItem> keyList = autocomplete.getKeys();
            Collections.sort(keyList, defaultACItemComparator);

            keys = new AutoCompletingComboBox(key);
            keys.setPossibleACItems(keyList);
            keys.setEditable(true);
            keys.setSelectedItem(key);

            p.add(Box.createVerticalStrut(5),GBC.eol());
            p.add(new JLabel(tr("Key")), GBC.std());
            p.add(Box.createHorizontalStrut(10), GBC.std());
            p.add(keys, GBC.eol().fill(GBC.HORIZONTAL));

            List<AutoCompletionListItem> valueList = autocomplete.getValues(getAutocompletionKeys(key));
            Collections.sort(valueList, usedValuesAwareComparator);

            final String selection= m.size()!=1?tr("<different>"):m.entrySet().iterator().next().getKey();
            
            values = new AutoCompletingComboBox(selection);
            values.setRenderer(cellRenderer);

            values.setEditable(true);
            values.setPossibleACItems(valueList);
            values.setSelectedItem(selection);
            values.getEditor().setItem(selection);
            p.add(Box.createVerticalStrut(5),GBC.eol());
            p.add(new JLabel(tr("Value")), GBC.std());
            p.add(Box.createHorizontalStrut(10), GBC.std());
            p.add(values, GBC.eol().fill(GBC.HORIZONTAL));
            values.getEditor().addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    buttonAction(0, null); // emulate OK button click
                }
            });
            addFocusAdapter(autocomplete, usedValuesAwareComparator);
            
            setContent(mainPanel, false);
            
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    if (initialFocusOnKey) {
                        selectKeysComboBox();
                    } else {
                        selectValuesCombobox();
                    }
                }
            });
        }
        
        /**
         * Edit tags of multiple selected objects according to selected ComboBox values
         * If value == "", tag will be deleted
         * Confirmations may be needed.
         */
        private void performTagEdit() {
            String value = Tag.removeWhiteSpaces(values.getEditor().getItem().toString());
            value = Normalizer.normalize(value, java.text.Normalizer.Form.NFC);
            if (value.isEmpty()) {
                value = null; // delete the key
            }
            String newkey = Tag.removeWhiteSpaces(keys.getEditor().getItem().toString());
            newkey = Normalizer.normalize(newkey, java.text.Normalizer.Form.NFC);
            if (newkey.isEmpty()) {
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
                Collection<Command> commands = new ArrayList<Command>();
                commands.add(new ChangePropertyCommand(sel, key, null));
                if (value.equals(tr("<different>"))) {
                    Map<String, List<OsmPrimitive>> map = new HashMap<String, List<OsmPrimitive>>();
                    for (OsmPrimitive osm: sel) {
                        String val = osm.get(key);
                        if (val != null) {
                            if (map.containsKey(val)) {
                                map.get(val).add(osm);
                            } else {
                                List<OsmPrimitive> v = new ArrayList<OsmPrimitive>();
                                v.add(osm);
                                map.put(val, v);
                            }
                        }
                    }
                    for (Map.Entry<String, List<OsmPrimitive>> e: map.entrySet()) {
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
    }

    public static final BooleanProperty PROPERTY_FIX_TAG_LOCALE = new BooleanProperty("properties.fix-tag-combobox-locale", false);
    public static final BooleanProperty PROPERTY_REMEMBER_TAGS = new BooleanProperty("properties.remember-recently-added-tags", false);
    public static final IntegerProperty PROPERTY_RECENT_TAGS_NUMBER = new IntegerProperty("properties.recently-added-tags", DEFAULT_LRU_TAGS_NUMBER);

    abstract class AbstractTagsDialog extends ExtendedDialog {
        AutoCompletingComboBox keys;
        AutoCompletingComboBox values;
        Component componentUnderMouse;
        
        public AbstractTagsDialog(Component parent, String title, String[] buttonTexts) {
            super(parent, title, buttonTexts);
            addMouseListener(new PopupMenuLauncher(popupMenu));
        }

        @Override
        public void setupDialog() {
            super.setupDialog();
            final Dimension size = getSize();
            // Set resizable only in width
            setMinimumSize(size);
            setPreferredSize(size);
            // setMaximumSize does not work, and never worked, but still it seems not to bother Oracle to fix this 10-year-old bug
            // https://bugs.openjdk.java.net/browse/JDK-6200438
            // https://bugs.openjdk.java.net/browse/JDK-6464548
            
            setRememberWindowGeometry(getClass().getName() + ".geometry",
                WindowGeometry.centerInWindow(Main.parent, size));
        }

        @Override
        public void setVisible(boolean visible) {
            // Do not want dialog to be resizable in height, as its size may increase each time because of the recently added tags
            // So need to modify the stored geometry (size part only) in order to use the automatic positioning mechanism
            if (visible) {
                WindowGeometry geometry = initWindowGeometry();
                Dimension storedSize = geometry.getSize();
                Dimension size = getSize();
                if (!storedSize.equals(size)) {
                    if (storedSize.width < size.width) {
                        storedSize.width = size.width;
                    }
                    if (storedSize.height != size.height) {
                        storedSize.height = size.height;
                    }
                    rememberWindowGeometry(geometry);
                }
                keys.setFixedLocale(PROPERTY_FIX_TAG_LOCALE.get());
            }
            super.setVisible(visible);
        }
        
        private void selectACComboBoxSavingUnixBuffer(AutoCompletingComboBox cb) {
            // select compbobox with saving unix system selection (middle mouse paste)
            Clipboard sysSel = Toolkit.getDefaultToolkit().getSystemSelection();
            if(sysSel != null) {
                Transferable old = sysSel.getContents(null);
                cb.requestFocusInWindow();
                cb.getEditor().selectAll();
                sysSel.setContents(old, null);
            } else {
                cb.requestFocusInWindow();
                cb.getEditor().selectAll();
            }
        }
        
        public void selectKeysComboBox() {
            selectACComboBoxSavingUnixBuffer(keys);
        }
        
        public void selectValuesCombobox()   {
            selectACComboBoxSavingUnixBuffer(values);
        }
        
        /**
        * Create a focus handling adapter and apply in to the editor component of value
        * autocompletion box.
        * @param autocomplete Manager handling the autocompletion
        * @param comparator Class to decide what values are offered on autocompletion
        * @return The created adapter
        */
        protected FocusAdapter addFocusAdapter(final AutoCompletionManager autocomplete, final Comparator<AutoCompletionListItem> comparator) {
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
        
        protected JPopupMenu popupMenu = new JPopupMenu() {
            JCheckBoxMenuItem fixTagLanguageCb = new JCheckBoxMenuItem(
                new AbstractAction(tr("Use English language for tag by default")){
                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean sel=((JCheckBoxMenuItem) e.getSource()).getState();
                    PROPERTY_FIX_TAG_LOCALE.put(sel);
                }
            });
            {
                add(fixTagLanguageCb);
                fixTagLanguageCb.setState(PROPERTY_FIX_TAG_LOCALE.get());
            }
        };
    }

    class AddTagsDialog extends AbstractTagsDialog {
        List<JosmAction> recentTagsActions = new ArrayList<JosmAction>();
        
        // Counter of added commands for possible undo
        private int commandCount;

        public AddTagsDialog() {
            super(Main.parent, tr("Add value?"), new String[] {tr("OK"),tr("Cancel")});
            setButtonIcons(new String[] {"ok","cancel"});
            setCancelButton(2);
            configureContextsensitiveHelp("/Dialog/AddValue", true /* show help button */);
            
            JPanel mainPanel = new JPanel(new GridBagLayout());
            keys = new AutoCompletingComboBox();
            values = new AutoCompletingComboBox();
            
            mainPanel.add(new JLabel("<html>"+trn("This will change up to {0} object.",
                "This will change up to {0} objects.", sel.size(),sel.size())
                +"<br><br>"+tr("Please select a key")), GBC.eol().fill(GBC.HORIZONTAL));

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
                for (int i = 0; i < tagData.getRowCount(); ++i) {
                    if (item.getValue().equals(tagData.getValueAt(i, 0))) {
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

            mainPanel.add(keys, GBC.eop().fill());

            mainPanel.add(new JLabel(tr("Please select a value")), GBC.eol());
            values.setEditable(true);
            mainPanel.add(values, GBC.eop().fill());
            if (itemToSelect != null) {
                keys.setSelectedItem(itemToSelect);
                if (lastAddValue != null) {
                    values.setSelectedItem(lastAddValue);
                }
            }

            FocusAdapter focus = addFocusAdapter(autocomplete, defaultACItemComparator);
            // fire focus event in advance or otherwise the popup list will be too small at first
            focus.focusGained(null);

            int recentTagsToShow = PROPERTY_RECENT_TAGS_NUMBER.get();
            if (recentTagsToShow > MAX_LRU_TAGS_NUMBER) {
                recentTagsToShow = MAX_LRU_TAGS_NUMBER;
            }
            
            // Add tag on Shift-Enter
            mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_MASK), "addAndContinue");
                mainPanel.getActionMap().put("addAndContinue", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        performTagAdding();
                        selectKeysComboBox();
                    }
                });
                    
            suggestRecentlyAddedTags(mainPanel, recentTagsToShow, focus);
            
            setContent(mainPanel, false);
            
            selectKeysComboBox();
            
            popupMenu.add(new AbstractAction(tr("Set number of recently added tags")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    selectNumberOfTags();
                }
            });
            JCheckBoxMenuItem rememberLastTags = new JCheckBoxMenuItem(
                new AbstractAction(tr("Remember last used tags")){
                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean sel=((JCheckBoxMenuItem) e.getSource()).getState();
                    PROPERTY_REMEMBER_TAGS.put(sel);
                    if (sel) saveTagsIfNeeded();
                }
            });
            rememberLastTags.setState(PROPERTY_REMEMBER_TAGS.get());
            popupMenu.add(rememberLastTags);
        }
        
        private void selectNumberOfTags() {
            String s = JOptionPane.showInputDialog(this, tr("Please enter the number of recently added tags to display"));
            if (s!=null) try {
                int v = Integer.parseInt(s);
                if (v>=0 && v<=MAX_LRU_TAGS_NUMBER) {
                    PROPERTY_RECENT_TAGS_NUMBER.put(v);
                    return;
                }
            } catch (NumberFormatException ex) {
                Main.warn(ex);
            }
            JOptionPane.showMessageDialog(this, tr("Please enter integer number between 0 and {0}", MAX_LRU_TAGS_NUMBER));
        }
        
        private void suggestRecentlyAddedTags(JPanel mainPanel, int tagsToShow, final FocusAdapter focus) {
            if (!(tagsToShow > 0 && !recentTags.isEmpty()))
                return;

            mainPanel.add(new JLabel(tr("Recently added tags")), GBC.eol());
            
            int count = 1;
            // We store the maximum number (9) of recent tags to allow dynamic change of number of tags shown in the preferences.
            // This implies to iterate in descending order, as the oldest elements will only be removed after we reach the maximum numbern and not the number of tags to show.
            // However, as Set does not allow to iterate in descending order, we need to copy its elements into a List we can access in reverse order.
            List<Tag> tags = new LinkedList<Tag>(recentTags.keySet());
            for (int i = tags.size()-1; i >= 0 && count <= tagsToShow; i--, count++) {
                final Tag t = tags.get(i);
                // Create action for reusing the tag, with keyboard shortcut Ctrl+(1-5)
                String actionShortcutKey = "properties:recent:"+count;
                String actionShortcutShiftKey = "properties:recent:shift:"+count;
                Shortcut sc = Shortcut.registerShortcut(actionShortcutKey, tr("Choose recent tag {0}", count), KeyEvent.VK_0+count, Shortcut.CTRL);
                final JosmAction action = new JosmAction(actionShortcutKey, null, tr("Use this tag again"), sc, false) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        keys.setSelectedItem(t.getKey());
                        // Update list of values (fix #7951)
                        // fix #8298 - update list of values before setting value (?)
                        focus.focusGained(null);
                        values.setSelectedItem(t.getValue());
                        selectValuesCombobox();
                    }
                };
                Shortcut scShift = Shortcut.registerShortcut(actionShortcutShiftKey, tr("Apply recent tag {0}", count), KeyEvent.VK_0+count, Shortcut.CTRL_SHIFT);
                final JosmAction actionShift = new JosmAction(actionShortcutShiftKey, null, tr("Use this tag again"), scShift, false) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        action.actionPerformed(null);
                        performTagAdding();
                        selectKeysComboBox();
                    }
                };
                recentTagsActions.add(action);
                recentTagsActions.add(actionShift);
                disableTagIfNeeded(t, action);
                // Find and display icon
                ImageIcon icon = MapPaintStyles.getNodeIcon(t, false); // Filters deprecated icon
                if (icon == null) {
                    // If no icon found in map style look at presets
                    Map<String, String> map = new HashMap<String, String>();
                    map.put(t.getKey(), t.getValue());
                    for (TaggingPreset tp : TaggingPreset.getMatchingPresets(null, map, false)) {
                        icon = tp.getIcon();
                        if (icon != null) {
                            break;
                        }
                    }
                    // If still nothing display an empty icon
                    if (icon == null) {
                        icon = new ImageIcon(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB));
                    }
                }
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.ipadx = 5;
                mainPanel.add(new JLabel(action.isEnabled() ? icon : GuiHelper.getDisabledIcon(icon)), gbc);
                // Create tag label
                final String color = action.isEnabled() ? "" : "; color:gray";
                final JLabel tagLabel = new JLabel("<html>"
                    + "<style>td{border:1px solid gray; font-weight:normal"+color+"}</style>"
                    + "<table><tr><td>" + XmlWriter.encode(t.toString(), true) + "</td></tr></table></html>");
                if (action.isEnabled()) {
                    // Register action
                    mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(sc.getKeyStroke(), actionShortcutKey);
                    mainPanel.getActionMap().put(actionShortcutKey, action);
                    mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(scShift.getKeyStroke(), actionShortcutShiftKey);
                    mainPanel.getActionMap().put(actionShortcutShiftKey, actionShift);
                    // Make the tag label clickable and set tooltip to the action description (this displays also the keyboard shortcut)
                    tagLabel.setToolTipText((String) action.getValue(Action.SHORT_DESCRIPTION));
                    tagLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    tagLabel.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            action.actionPerformed(null);
                            // add tags and close window on double-click
                            if (e.getClickCount()>1) {
                                buttonAction(0, null); // emulate OK click and close the dialog
                            }
                            // add tags on Shift-Click
                            if (e.isShiftDown()) {
                                performTagAdding();
                                selectKeysComboBox();
                            }
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
                mainPanel.add(tagPanel, GBC.eol().fill(GBC.HORIZONTAL));
            }
        }

        public void destroyActions() {
            for (JosmAction action : recentTagsActions) {
                action.destroy();
            }
        }

        /**
         * Read tags from comboboxes and add it to all selected objects
         */
        public void performTagAdding() {
            String key = Tag.removeWhiteSpaces(keys.getEditor().getItem().toString());
            String value = Tag.removeWhiteSpaces(values.getEditor().getItem().toString());
            if (key.isEmpty() || value.isEmpty()) return;
            lastAddKey = key;
            lastAddValue = value;
            recentTags.put(new Tag(key, value), null);
            commandCount++;
            Main.main.undoRedo.add(new ChangePropertyCommand(sel, key, value));
            changedKey = key;
        }
        
        public void undoAllTagsAdding() {
            Main.main.undoRedo.undo(commandCount);
        }

        private void disableTagIfNeeded(final Tag t, final JosmAction action) {
            // Disable action if its key is already set on the object (the key being absent from the keys list for this reason
            // performing this action leads to autocomplete to the next key (see #7671 comments)
            for (int j = 0; j < tagData.getRowCount(); ++j) {
                if (t.getKey().equals(tagData.getValueAt(j, 0))) {
                    action.setEnabled(false);
                    break;
                }
            }
        }
    }
 }