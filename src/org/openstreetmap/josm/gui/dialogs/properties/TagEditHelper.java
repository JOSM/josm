// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
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
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.OsmDataManager;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.search.SearchParseError;
import org.openstreetmap.josm.data.osm.search.SearchSetting;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.EnumProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.preferences.ListProperty;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.data.tagging.ac.AutoCompletionItem;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.IExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.datatransfer.ClipboardUtils;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingComboBox;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.io.XmlWriter;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * Class that helps PropertiesDialog add and edit tag values.
 * @since 5633
 */
public class TagEditHelper {

    private final JTable tagTable;
    private final DefaultTableModel tagData;
    private final Map<String, Map<String, Integer>> valueCount;

    // Selection that we are editing by using both dialogs
    protected Collection<OsmPrimitive> sel;

    private String changedKey;
    private String objKey;

    static final Comparator<AutoCompletionItem> DEFAULT_AC_ITEM_COMPARATOR =
            (o1, o2) -> String.CASE_INSENSITIVE_ORDER.compare(o1.getValue(), o2.getValue());

    /** Default number of recent tags */
    public static final int DEFAULT_LRU_TAGS_NUMBER = 5;
    /** Maximum number of recent tags */
    public static final int MAX_LRU_TAGS_NUMBER = 30;

    /** Autocomplete keys by default */
    public static final BooleanProperty AUTOCOMPLETE_KEYS = new BooleanProperty("properties.autocomplete-keys", true);
    /** Autocomplete values by default */
    public static final BooleanProperty AUTOCOMPLETE_VALUES = new BooleanProperty("properties.autocomplete-values", true);
    /** Use English language for tag by default */
    public static final BooleanProperty PROPERTY_FIX_TAG_LOCALE = new BooleanProperty("properties.fix-tag-combobox-locale", false);
    /** Whether recent tags must be remembered */
    public static final BooleanProperty PROPERTY_REMEMBER_TAGS = new BooleanProperty("properties.remember-recently-added-tags", true);
    /** Number of recent tags */
    public static final IntegerProperty PROPERTY_RECENT_TAGS_NUMBER = new IntegerProperty("properties.recently-added-tags",
            DEFAULT_LRU_TAGS_NUMBER);
    /** The preference storage of recent tags */
    public static final ListProperty PROPERTY_RECENT_TAGS = new ListProperty("properties.recent-tags",
            Collections.<String>emptyList());
    /** The preference list of tags which should not be remembered, since r9940 */
    public static final StringProperty PROPERTY_TAGS_TO_IGNORE = new StringProperty("properties.recent-tags.ignore",
            new SearchSetting().writeToString());

    /**
     * What to do with recent tags where keys already exist
     */
    private enum RecentExisting {
        ENABLE,
        DISABLE,
        HIDE
    }

    /**
     * Preference setting for popup menu item "Recent tags with existing key"
     */
    public static final EnumProperty<RecentExisting> PROPERTY_RECENT_EXISTING = new EnumProperty<>(
        "properties.recently-added-tags-existing-key", RecentExisting.class, RecentExisting.DISABLE);

    /**
     * What to do after applying tag
     */
    private enum RefreshRecent {
        NO,
        STATUS,
        REFRESH
    }

    /**
     * Preference setting for popup menu item "Refresh recent tags list after applying tag"
     */
    public static final EnumProperty<RefreshRecent> PROPERTY_REFRESH_RECENT = new EnumProperty<>(
        "properties.refresh-recently-added-tags", RefreshRecent.class, RefreshRecent.STATUS);

    final RecentTagCollection recentTags = new RecentTagCollection(MAX_LRU_TAGS_NUMBER);
    SearchSetting tagsToIgnore;

    /**
     * Copy of recently added tags in sorted from newest to oldest order.
     *
     * We store the maximum number of recent tags to allow dynamic change of number of tags shown in the preferences.
     * Used to cache initial status.
     */
    private List<Tag> tags;

    static {
        // init user input based on recent tags
        final RecentTagCollection recentTags = new RecentTagCollection(MAX_LRU_TAGS_NUMBER);
        recentTags.loadFromPreference(PROPERTY_RECENT_TAGS);
        recentTags.toList().forEach(tag -> AutoCompletionManager.rememberUserInput(tag.getKey(), tag.getValue(), false));
    }

    /**
     * Constructs a new {@code TagEditHelper}.
     * @param tagTable tag table
     * @param propertyData table model
     * @param valueCount tag value count
     */
    public TagEditHelper(JTable tagTable, DefaultTableModel propertyData, Map<String, Map<String, Integer>> valueCount) {
        this.tagTable = tagTable;
        this.tagData = propertyData;
        this.valueCount = valueCount;
    }

    /**
     * Finds the key from given row of tag editor.
     * @param viewRow index of row
     * @return key of tag
     */
    public final String getDataKey(int viewRow) {
        return tagData.getValueAt(tagTable.convertRowIndexToModel(viewRow), 0).toString();
    }

    /**
     * Determines if the given tag key is already used (by all selected primitives, not just some of them)
     * @param key the key to check
     * @return {@code true} if the key is used by all selected primitives (key not unset for at least one primitive)
     */
    @SuppressWarnings("unchecked")
    boolean containsDataKey(String key) {
        return IntStream.range(0, tagData.getRowCount())
                .anyMatch(i -> key.equals(tagData.getValueAt(i, 0)) /* sic! do not use getDataKey*/
                    && !((Map<String, Integer>) tagData.getValueAt(i, 1)).containsKey("") /* sic! do not use getDataValues*/);
    }

    /**
     * Finds the values from given row of tag editor.
     * @param viewRow index of row
     * @return map of values and number of occurrences
     */
    @SuppressWarnings("unchecked")
    public final Map<String, Integer> getDataValues(int viewRow) {
        return (Map<String, Integer>) tagData.getValueAt(tagTable.convertRowIndexToModel(viewRow), 1);
    }

    /**
     * Open the add selection dialog and add a new key/value to the table (and
     * to the dataset, of course).
     */
    public void addTag() {
        changedKey = null;
        DataSet activeDataSet = OsmDataManager.getInstance().getActiveDataSet();
        try {
            activeDataSet.beginUpdate();
            sel = OsmDataManager.getInstance().getInProgressSelection();
            if (sel == null || sel.isEmpty())
                return;

            final AddTagsDialog addDialog = getAddTagsDialog();

            addDialog.showDialog();

            addDialog.destroyActions();
            if (addDialog.getValue() == 1)
                addDialog.performTagAdding();
            else
                addDialog.undoAllTagsAdding();
        } finally {
            activeDataSet.endUpdate();
        }
    }

    /**
     * Returns a new {@code AddTagsDialog}.
     * @return a new {@code AddTagsDialog}
     */
    protected AddTagsDialog getAddTagsDialog() {
        return new AddTagsDialog();
    }

    /**
    * Edit the value in the tags table row.
    * @param row The row of the table from which the value is edited.
    * @param focusOnKey Determines if the initial focus should be set on key instead of value
    * @since 5653
    */
    public void editTag(final int row, boolean focusOnKey) {
        changedKey = null;
        sel = OsmDataManager.getInstance().getInProgressSelection();
        if (sel == null || sel.isEmpty())
            return;

        String key = getDataKey(row);
        objKey = key;

        final IEditTagDialog editDialog = getEditTagDialog(row, focusOnKey, key);
        editDialog.showDialog();
        if (editDialog.getValue() != 1)
            return;
        editDialog.performTagEdit();
    }

    /**
     * Extracted interface of {@link EditTagDialog}.
     */
    protected interface IEditTagDialog extends IExtendedDialog {
        /**
         * Edit tags of multiple selected objects according to selected ComboBox values
         * If value == "", tag will be deleted
         * Confirmations may be needed.
         */
        void performTagEdit();
    }

    protected IEditTagDialog getEditTagDialog(int row, boolean focusOnKey, String key) {
        return new EditTagDialog(key, getDataValues(row), focusOnKey);
    }

    /**
     * If during last editProperty call user changed the key name, this key will be returned
     * Elsewhere, returns null.
     * @return The modified key, or {@code null}
     */
    public String getChangedKey() {
        return changedKey;
    }

    /**
     * Reset last changed key.
     */
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
     * Load recently used tags from preferences if needed.
     */
    public void loadTagsIfNeeded() {
        loadTagsToIgnore();
        if (PROPERTY_REMEMBER_TAGS.get() && recentTags.isEmpty()) {
            recentTags.loadFromPreference(PROPERTY_RECENT_TAGS);
        }
    }

    void loadTagsToIgnore() {
        final SearchSetting searchSetting = Utils.firstNonNull(
                SearchSetting.readFromString(PROPERTY_TAGS_TO_IGNORE.get()), new SearchSetting());
        if (!Objects.equals(tagsToIgnore, searchSetting)) {
            try {
                tagsToIgnore = searchSetting;
                recentTags.setTagsToIgnore(tagsToIgnore);
            } catch (SearchParseError parseError) {
                warnAboutParseError(parseError);
                tagsToIgnore = new SearchSetting();
                recentTags.setTagsToIgnore(SearchCompiler.Never.INSTANCE);
            }
        }
    }

    private static void warnAboutParseError(SearchParseError parseError) {
        Logging.warn(parseError);
        JOptionPane.showMessageDialog(
                MainApplication.getMainFrame(),
                parseError.getMessage(),
                tr("Error"),
                JOptionPane.ERROR_MESSAGE
        );
    }

    /**
     * Store recently used tags in preferences if needed.
     */
    public void saveTagsIfNeeded() {
        if (PROPERTY_REMEMBER_TAGS.get() && !recentTags.isEmpty()) {
            recentTags.saveToPreference(PROPERTY_RECENT_TAGS);
        }
    }

    /**
     * Forget recently selected primitives to allow GC.
     * @since 14509
     */
    public void resetSelection() {
        sel = null;
    }

    /**
     * Update cache of recent tags used for displaying tags.
     */
    private void cacheRecentTags() {
        tags = recentTags.toList();
        Collections.reverse(tags);
    }

    /**
     * Warns user about a key being overwritten.
     * @param action The action done by the user. Must state what key is changed
     * @param togglePref  The preference to save the checkbox state to
     * @return {@code true} if the user accepts to overwrite key, {@code false} otherwise
     */
    private static boolean warnOverwriteKey(String action, String togglePref) {
        return new ExtendedDialog(
                MainApplication.getMainFrame(),
                tr("Overwrite key"),
                tr("Replace"), tr("Cancel"))
            .setButtonIcons("purge", "cancel")
            .setContent(action+'\n'+ tr("The new key is already used, overwrite values?"))
            .setCancelButton(2)
            .toggleEnable(togglePref)
            .showDialog().getValue() == 1;
    }

    protected class EditTagDialog extends AbstractTagsDialog implements IEditTagDialog {
        private final String key;
        private final transient Map<String, Integer> m;
        private final transient Comparator<AutoCompletionItem> usedValuesAwareComparator;

        private final transient ListCellRenderer<AutoCompletionItem> cellRenderer = new ListCellRenderer<AutoCompletionItem>() {
            private final DefaultListCellRenderer def = new DefaultListCellRenderer();
            @Override
            public Component getListCellRendererComponent(JList<? extends AutoCompletionItem> list,
                    AutoCompletionItem value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = def.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (c instanceof JLabel) {
                    String str = value.getValue();
                    if (valueCount.containsKey(objKey)) {
                        Map<String, Integer> map = valueCount.get(objKey);
                        if (map.containsKey(str)) {
                            str = tr("{0} ({1})", str, map.get(str));
                            c.setFont(c.getFont().deriveFont(Font.ITALIC + Font.BOLD));
                        }
                    }
                    ((JLabel) c).setText(str);
                }
                return c;
            }
        };

        protected EditTagDialog(String key, Map<String, Integer> map, final boolean initialFocusOnKey) {
            super(MainApplication.getMainFrame(), trn("Change value?", "Change values?", map.size()), tr("OK"), tr("Cancel"));
            setButtonIcons("ok", "cancel");
            setCancelButton(2);
            configureContextsensitiveHelp("/Dialog/EditValue", true /* show help button */);
            this.key = key;
            this.m = map;

            usedValuesAwareComparator = (o1, o2) -> {
                boolean c1 = m.containsKey(o1.getValue());
                boolean c2 = m.containsKey(o2.getValue());
                if (c1 == c2)
                    return String.CASE_INSENSITIVE_ORDER.compare(o1.getValue(), o2.getValue());
                else if (c1)
                    return -1;
                else
                    return +1;
            };

            JPanel mainPanel = new JPanel(new BorderLayout());

            String msg = "<html>"+trn("This will change {0} object.",
                    "This will change up to {0} objects.", sel.size(), sel.size())
                    +"<br><br>("+tr("An empty value deletes the tag.", key)+")</html>";

            mainPanel.add(new JLabel(msg), BorderLayout.NORTH);

            JPanel p = new JPanel(new GridBagLayout());
            mainPanel.add(p, BorderLayout.CENTER);

            AutoCompletionManager autocomplete = AutoCompletionManager.of(OsmDataManager.getInstance().getActiveDataSet());
            List<AutoCompletionItem> keyList = autocomplete.getTagKeys(DEFAULT_AC_ITEM_COMPARATOR);

            keys = new AutoCompletingComboBox(key);
            keys.setPossibleAcItems(keyList);
            keys.setEditable(true);
            keys.setSelectedItem(key);

            p.add(Box.createVerticalStrut(5), GBC.eol());
            p.add(new JLabel(tr("Key")), GBC.std());
            p.add(Box.createHorizontalStrut(10), GBC.std());
            p.add(keys, GBC.eol().fill(GBC.HORIZONTAL));

            List<AutoCompletionItem> valueList = autocomplete.getTagValues(getAutocompletionKeys(key), usedValuesAwareComparator);

            final String selection = m.size() != 1 ? tr("<different>") : m.entrySet().iterator().next().getKey();

            values = new AutoCompletingComboBox(selection);
            values.setRenderer(cellRenderer);

            values.setEditable(true);
            values.setPossibleAcItems(valueList);
            values.setSelectedItem(selection);
            values.getEditor().setItem(selection);
            p.add(Box.createVerticalStrut(5), GBC.eol());
            p.add(new JLabel(tr("Value")), GBC.std());
            p.add(Box.createHorizontalStrut(10), GBC.std());
            p.add(values, GBC.eol().fill(GBC.HORIZONTAL));
            values.getEditor().addActionListener(e -> buttonAction(0, null));
            addFocusAdapter(autocomplete, usedValuesAwareComparator);

            addUpdateIconListener();

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

        @Override
        public void performTagEdit() {
            String value = values.getEditItem();
            value = Normalizer.normalize(value, Normalizer.Form.NFC);
            if (value.isEmpty()) {
                value = null; // delete the key
            }
            String newkey = keys.getEditItem();
            newkey = Normalizer.normalize(newkey, Normalizer.Form.NFC);
            if (newkey.isEmpty()) {
                newkey = key;
                value = null; // delete the key instead
            }
            if (key.equals(newkey) && tr("<different>").equals(value))
                return;
            if (key.equals(newkey) || value == null) {
                UndoRedoHandler.getInstance().add(new ChangePropertyCommand(sel, newkey, value));
                if (value != null) {
                    AutoCompletionManager.rememberUserInput(newkey, value, true);
                    recentTags.add(new Tag(key, value));
                }
            } else {
                for (OsmPrimitive osm: sel) {
                    if (osm.get(newkey) != null) {
                        if (!warnOverwriteKey(tr("You changed the key from ''{0}'' to ''{1}''.", key, newkey),
                                "overwriteEditKey"))
                            return;
                        break;
                    }
                }
                Collection<Command> commands = new ArrayList<>();
                commands.add(new ChangePropertyCommand(sel, key, null));
                if (value.equals(tr("<different>"))) {
                    String newKey = newkey;
                    sel.stream()
                            .filter(osm -> osm.hasKey(key))
                            .collect(Collectors.groupingBy(osm -> osm.get(key)))
                            .forEach((newValue, osmPrimitives) -> commands.add(new ChangePropertyCommand(osmPrimitives, newKey, newValue)));
                } else {
                    commands.add(new ChangePropertyCommand(sel, newkey, value));
                    AutoCompletionManager.rememberUserInput(newkey, value, false);
                }
                UndoRedoHandler.getInstance().add(new SequenceCommand(
                        trn("Change properties of up to {0} object",
                                "Change properties of up to {0} objects", sel.size(), sel.size()),
                                commands));
            }

            changedKey = newkey;
        }
    }

    protected abstract class AbstractTagsDialog extends ExtendedDialog {
        protected AutoCompletingComboBox keys;
        protected AutoCompletingComboBox values;

        AbstractTagsDialog(Component parent, String title, String... buttonTexts) {
            super(parent, title, buttonTexts);
            addMouseListener(new PopupMenuLauncher(popupMenu));
        }

        @Override
        public void setupDialog() {
            super.setupDialog();
            buttons.get(0).setEnabled(!OsmDataManager.getInstance().getActiveDataSet().isLocked());
            final Dimension size = getSize();
            // Set resizable only in width
            setMinimumSize(size);
            setPreferredSize(size);
            // setMaximumSize does not work, and never worked, but still it seems not to bother Oracle to fix this 10-year-old bug
            // https://bugs.openjdk.java.net/browse/JDK-6200438
            // https://bugs.openjdk.java.net/browse/JDK-6464548

            setRememberWindowGeometry(getClass().getName() + ".geometry",
                WindowGeometry.centerInWindow(MainApplication.getMainFrame(), size));
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
                updateOkButtonIcon();
            }
            super.setVisible(visible);
        }

        private void selectACComboBoxSavingUnixBuffer(AutoCompletingComboBox cb) {
            // select combobox with saving unix system selection (middle mouse paste)
            Clipboard sysSel = ClipboardUtils.getSystemSelection();
            if (sysSel != null) {
                Transferable old = ClipboardUtils.getClipboardContent(sysSel);
                cb.requestFocusInWindow();
                cb.getEditor().selectAll();
                if (old != null) {
                    sysSel.setContents(old, null);
                }
            } else {
                cb.requestFocusInWindow();
                cb.getEditor().selectAll();
            }
        }

        public void selectKeysComboBox() {
            selectACComboBoxSavingUnixBuffer(keys);
        }

        public void selectValuesCombobox() {
            selectACComboBoxSavingUnixBuffer(values);
        }

        /**
        * Create a focus handling adapter and apply in to the editor component of value
        * autocompletion box.
        * @param autocomplete Manager handling the autocompletion
        * @param comparator Class to decide what values are offered on autocompletion
        * @return The created adapter
        */
        protected FocusAdapter addFocusAdapter(final AutoCompletionManager autocomplete, final Comparator<AutoCompletionItem> comparator) {
           // get the combo box' editor component
           final JTextComponent editor = values.getEditorComponent();
           // Refresh the values model when focus is gained
           FocusAdapter focus = new FocusAdapter() {
               @Override
               public void focusGained(FocusEvent e) {
                   Logging.trace("Focus gained by {0}, e={1}", values, e);
                   String key = keys.getEditor().getItem().toString();
                   List<AutoCompletionItem> correctItems = autocomplete.getTagValues(getAutocompletionKeys(key), comparator);
                   ComboBoxModel<AutoCompletionItem> currentModel = values.getModel();
                   final int size = correctItems.size();
                   boolean valuesOK = size == currentModel.getSize()
                           && IntStream.range(0, size).allMatch(i -> Objects.equals(currentModel.getElementAt(i), correctItems.get(i)));
                   if (!valuesOK) {
                       values.setPossibleAcItems(correctItems);
                   }
                   if (!Objects.equals(key, objKey)) {
                       values.getEditor().selectAll();
                       objKey = key;
                   }
               }
           };
           editor.addFocusListener(focus);
           return focus;
        }

        protected void addUpdateIconListener() {
            keys.addActionListener(ignore -> updateOkButtonIcon());
            values.addActionListener(ignore -> updateOkButtonIcon());
        }

        private void updateOkButtonIcon() {
            if (buttons.isEmpty()) {
                return;
            }
            buttons.get(0).setIcon(findIcon(keys.getSelectedOrEditItem(), values.getSelectedOrEditItem())
                    .orElse(ImageProvider.get("ok", ImageProvider.ImageSizes.LARGEICON)));
        }

        protected Optional<ImageIcon> findIcon(String key, String value) {
            final Iterator<OsmPrimitive> osmPrimitiveIterator = sel.iterator();
            final OsmPrimitive virtual = (osmPrimitiveIterator.hasNext() ? osmPrimitiveIterator.next().getType() : OsmPrimitiveType.NODE)
                    .newInstance(0, false);
            if (virtual instanceof INode) {
                ((INode) virtual).setCoor(LatLon.ZERO);
            }
            virtual.put(key, value);
            final ImageIcon padded = ImageProvider.getPadded(virtual, ImageProvider.ImageSizes.LARGEICON.getImageDimension(),
                    EnumSet.of(ImageProvider.GetPaddedOptions.NO_DEFAULT, ImageProvider.GetPaddedOptions.NO_DEPRECATED));
            return Optional.ofNullable(padded);
        }

        protected JPopupMenu popupMenu = new JPopupMenu() {
            private final JCheckBoxMenuItem fixTagLanguageCb = new JCheckBoxMenuItem(
                new AbstractAction(tr("Use English language for tag by default")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean use = ((JCheckBoxMenuItem) e.getSource()).getState();
                    PROPERTY_FIX_TAG_LOCALE.put(use);
                    keys.setFixedLocale(use);
                }
            });
            {
                add(fixTagLanguageCb);
                fixTagLanguageCb.setState(PROPERTY_FIX_TAG_LOCALE.get());
            }
        };
    }

    protected class AddTagsDialog extends AbstractTagsDialog {
        private final List<JosmAction> recentTagsActions = new ArrayList<>();
        protected final transient FocusAdapter focus;
        private final JPanel mainPanel;
        private JPanel recentTagsPanel;

        // Counter of added commands for possible undo
        private int commandCount;

        protected AddTagsDialog() {
            super(MainApplication.getMainFrame(), tr("Add tag"), tr("OK"), tr("Cancel"));
            setButtonIcons("ok", "cancel");
            setCancelButton(2);
            configureContextsensitiveHelp("/Dialog/AddValue", true /* show help button */);

            mainPanel = new JPanel(new GridBagLayout());
            keys = new AutoCompletingComboBox();
            values = new AutoCompletingComboBox();
            keys.setAutocompleteEnabled(AUTOCOMPLETE_KEYS.get());
            values.setAutocompleteEnabled(AUTOCOMPLETE_VALUES.get());

            mainPanel.add(new JLabel("<html>"+trn("This will change up to {0} object.",
                "This will change up to {0} objects.", sel.size(), sel.size())
                +"<br><br>"+tr("Please select a key")), GBC.eol().fill(GBC.HORIZONTAL));

            cacheRecentTags();
            AutoCompletionManager autocomplete = AutoCompletionManager.of(OsmDataManager.getInstance().getActiveDataSet());
            List<AutoCompletionItem> keyList = autocomplete.getTagKeys(DEFAULT_AC_ITEM_COMPARATOR);

            // remove the object's tag keys from the list
            keyList.removeIf(item -> containsDataKey(item.getValue()));

            keys.setPossibleAcItems(keyList);
            keys.setEditable(true);

            mainPanel.add(keys, GBC.eop().fill(GBC.HORIZONTAL));

            mainPanel.add(new JLabel(tr("Choose a value")), GBC.eol());
            values.setEditable(true);
            mainPanel.add(values, GBC.eop().fill(GBC.HORIZONTAL));

            // pre-fill first recent tag for which the key is not already present
            tags.stream()
                    .filter(tag -> !containsDataKey(tag.getKey()))
                    .findFirst()
                    .ifPresent(tag -> {
                        keys.setSelectedItem(tag.getKey());
                        values.setSelectedItem(tag.getValue());
                    });

            focus = addFocusAdapter(autocomplete, DEFAULT_AC_ITEM_COMPARATOR);
            // fire focus event in advance or otherwise the popup list will be too small at first
            focus.focusGained(null);

            addUpdateIconListener();

            // Add tag on Shift-Enter
            mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "addAndContinue");
                mainPanel.getActionMap().put("addAndContinue", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        performTagAdding();
                        refreshRecentTags();
                        selectKeysComboBox();
                    }
                });

            suggestRecentlyAddedTags();

            mainPanel.add(Box.createVerticalGlue(), GBC.eop().fill());
            setContent(mainPanel, false);

            selectKeysComboBox();

            popupMenu.add(new AbstractAction(tr("Set number of recently added tags")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    selectNumberOfTags();
                    suggestRecentlyAddedTags();
                }
            });

            popupMenu.add(buildMenuRecentExisting());
            popupMenu.add(buildMenuRefreshRecent());

            JCheckBoxMenuItem rememberLastTags = new JCheckBoxMenuItem(
                new AbstractAction(tr("Remember last used tags after a restart")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean state = ((JCheckBoxMenuItem) e.getSource()).getState();
                    PROPERTY_REMEMBER_TAGS.put(state);
                    if (state)
                        saveTagsIfNeeded();
                }
            });
            rememberLastTags.setState(PROPERTY_REMEMBER_TAGS.get());
            popupMenu.add(rememberLastTags);
        }

        private JMenu buildMenuRecentExisting() {
            JMenu menu = new JMenu(tr("Recent tags with existing key"));
            TreeMap<RecentExisting, String> radios = new TreeMap<>();
            radios.put(RecentExisting.ENABLE, tr("Enable"));
            radios.put(RecentExisting.DISABLE, tr("Disable"));
            radios.put(RecentExisting.HIDE, tr("Hide"));
            ButtonGroup buttonGroup = new ButtonGroup();
            for (final Map.Entry<RecentExisting, String> entry : radios.entrySet()) {
                JRadioButtonMenuItem radio = new JRadioButtonMenuItem(new AbstractAction(entry.getValue()) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        PROPERTY_RECENT_EXISTING.put(entry.getKey());
                        suggestRecentlyAddedTags();
                    }
                });
                buttonGroup.add(radio);
                radio.setSelected(PROPERTY_RECENT_EXISTING.get() == entry.getKey());
                menu.add(radio);
            }
            return menu;
        }

        private JMenu buildMenuRefreshRecent() {
            JMenu menu = new JMenu(tr("Refresh recent tags list after applying tag"));
            TreeMap<RefreshRecent, String> radios = new TreeMap<>();
            radios.put(RefreshRecent.NO, tr("No refresh"));
            radios.put(RefreshRecent.STATUS, tr("Refresh tag status only (enabled / disabled)"));
            radios.put(RefreshRecent.REFRESH, tr("Refresh tag status and list of recently added tags"));
            ButtonGroup buttonGroup = new ButtonGroup();
            for (final Map.Entry<RefreshRecent, String> entry : radios.entrySet()) {
                JRadioButtonMenuItem radio = new JRadioButtonMenuItem(new AbstractAction(entry.getValue()) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        PROPERTY_REFRESH_RECENT.put(entry.getKey());
                    }
                });
                buttonGroup.add(radio);
                radio.setSelected(PROPERTY_REFRESH_RECENT.get() == entry.getKey());
                menu.add(radio);
            }
            return menu;
        }

        @Override
        public void setContentPane(Container contentPane) {
            final int commandDownMask = PlatformManager.getPlatform().getMenuShortcutKeyMaskEx();
            List<String> lines = new ArrayList<>();
            Shortcut.findShortcut(KeyEvent.VK_1, commandDownMask).ifPresent(sc ->
                    lines.add(sc.getKeyText() + ' ' + tr("to apply first suggestion"))
            );
            lines.add(Shortcut.getKeyText(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK)) + ' '
                    +tr("to add without closing the dialog"));
            Shortcut.findShortcut(KeyEvent.VK_1, commandDownMask | KeyEvent.SHIFT_DOWN_MASK).ifPresent(sc ->
                    lines.add(sc.getKeyText() + ' ' + tr("to add first suggestion without closing the dialog"))
            );
            final JLabel helpLabel = new JLabel("<html>" + String.join("<br>", lines) + "</html>");
            helpLabel.setFont(helpLabel.getFont().deriveFont(Font.PLAIN));
            contentPane.add(helpLabel, GBC.eol().fill(GridBagConstraints.HORIZONTAL).insets(5, 5, 5, 5));
            super.setContentPane(contentPane);
        }

        protected void selectNumberOfTags() {
            String s = String.format("%d", PROPERTY_RECENT_TAGS_NUMBER.get());
            while (true) {
                s = JOptionPane.showInputDialog(this, tr("Please enter the number of recently added tags to display"), s);
                if (s == null || s.isEmpty()) {
                    return;
                }
                try {
                    int v = Integer.parseInt(s);
                    if (v >= 0 && v <= MAX_LRU_TAGS_NUMBER) {
                        PROPERTY_RECENT_TAGS_NUMBER.put(v);
                        return;
                    }
                } catch (NumberFormatException ex) {
                    Logging.warn(ex);
                }
                JOptionPane.showMessageDialog(this, tr("Please enter integer number between 0 and {0}", MAX_LRU_TAGS_NUMBER));
            }
        }

        protected void suggestRecentlyAddedTags() {
            if (recentTagsPanel == null) {
                recentTagsPanel = new JPanel(new GridBagLayout());
                buildRecentTagsPanel();
                mainPanel.add(recentTagsPanel, GBC.eol().fill(GBC.HORIZONTAL));
            } else {
                Dimension panelOldSize = recentTagsPanel.getPreferredSize();
                recentTagsPanel.removeAll();
                buildRecentTagsPanel();
                Dimension panelNewSize = recentTagsPanel.getPreferredSize();
                Dimension dialogOldSize = getMinimumSize();
                Dimension dialogNewSize = new Dimension(dialogOldSize.width, dialogOldSize.height-panelOldSize.height+panelNewSize.height);
                setMinimumSize(dialogNewSize);
                setPreferredSize(dialogNewSize);
                setSize(dialogNewSize);
                revalidate();
                repaint();
            }
        }

        protected void buildRecentTagsPanel() {
            final int tagsToShow = Math.min(PROPERTY_RECENT_TAGS_NUMBER.get(), MAX_LRU_TAGS_NUMBER);
            if (!(tagsToShow > 0 && !recentTags.isEmpty()))
                return;
            recentTagsPanel.add(new JLabel(tr("Recently added tags")), GBC.eol());

            int count = 0;
            destroyActions();
            for (int i = 0; i < tags.size() && count < tagsToShow; i++) {
                final Tag t = tags.get(i);
                boolean keyExists = containsDataKey(t.getKey());
                if (keyExists && PROPERTY_RECENT_EXISTING.get() == RecentExisting.HIDE)
                    continue;
                count++;
                // Create action for reusing the tag, with keyboard shortcut
                /* POSSIBLE SHORTCUTS: 1,2,3,4,5,6,7,8,9,0=10 */
                final Shortcut sc = count > 10 ? null : Shortcut.registerShortcut("properties:recent:" + count,
                        tr("Choose recent tag {0}", count), KeyEvent.VK_0 + (count % 10), Shortcut.CTRL);
                final JosmAction action = new JosmAction(
                        tr("Choose recent tag {0}", count), null, tr("Use this tag again"), sc, false) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        keys.setSelectedItem(t.getKey(), true);
                        // fix #7951, #8298 - update list of values before setting value (?)
                        focus.focusGained(null);
                        values.setSelectedItem(t.getValue(), true);
                        selectValuesCombobox();
                    }
                };
                /* POSSIBLE SHORTCUTS: 1,2,3,4,5,6,7,8,9,0=10 */
                final Shortcut scShift = count > 10 ? null : Shortcut.registerShortcut("properties:recent:apply:" + count,
                         tr("Apply recent tag {0}", count), KeyEvent.VK_0 + (count % 10), Shortcut.CTRL_SHIFT);
                final JosmAction actionShift = new JosmAction(
                        tr("Apply recent tag {0}", count), null, tr("Use this tag again"), scShift, false) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        action.actionPerformed(null);
                        performTagAdding();
                        refreshRecentTags();
                        selectKeysComboBox();
                    }
                };
                recentTagsActions.add(action);
                recentTagsActions.add(actionShift);
                if (keyExists && PROPERTY_RECENT_EXISTING.get() == RecentExisting.DISABLE) {
                    action.setEnabled(false);
                }
                ImageIcon icon = findIcon(t.getKey(), t.getValue())
                        // If still nothing display an empty icon

                        .orElseGet(() -> new ImageIcon(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)));
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.ipadx = 5;
                recentTagsPanel.add(new JLabel(action.isEnabled() ? icon : GuiHelper.getDisabledIcon(icon)), gbc);
                // Create tag label
                final String color = action.isEnabled() ? "" : "; color:gray";
                final JLabel tagLabel = new JLabel("<html>"
                        + "<style>td{" + color + "}</style>"
                        + "<table><tr>"
                        + "<td>" + count + ".</td>"
                        + "<td style='border:1px solid gray'>" + XmlWriter.encode(t.toString(), true) + '<' +
                        "/td></tr></table></html>");
                tagLabel.setFont(tagLabel.getFont().deriveFont(Font.PLAIN));
                if (action.isEnabled() && sc != null && scShift != null) {
                    // Register action
                    recentTagsPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(sc.getKeyStroke(), "choose"+count);
                    recentTagsPanel.getActionMap().put("choose"+count, action);
                    recentTagsPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(scShift.getKeyStroke(), "apply"+count);
                    recentTagsPanel.getActionMap().put("apply"+count, actionShift);
                }
                if (action.isEnabled()) {
                    // Make the tag label clickable and set tooltip to the action description (this displays also the keyboard shortcut)
                    tagLabel.setToolTipText((String) action.getValue(Action.SHORT_DESCRIPTION));
                    tagLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    tagLabel.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            action.actionPerformed(null);
                            if (SwingUtilities.isRightMouseButton(e)) {
                                Component component = e.getComponent();
                                if (component.isShowing()) {
                                    new TagPopupMenu(t).show(component, e.getX(), e.getY());
                                }
                            } else if (e.isShiftDown()) {
                                // add tags on Shift-Click
                                performTagAdding();
                                refreshRecentTags();
                                selectKeysComboBox();
                            } else if (e.getClickCount() > 1) {
                                // add tags and close window on double-click
                                buttonAction(0, null); // emulate OK click and close the dialog
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
                recentTagsPanel.add(tagPanel, GBC.eol().fill(GBC.HORIZONTAL));
            }
            // Clear label if no tags were added
            if (count == 0) {
                recentTagsPanel.removeAll();
            }
        }

        class TagPopupMenu extends JPopupMenu {

            TagPopupMenu(Tag t) {
                add(new IgnoreTagAction(tr("Ignore key ''{0}''", t.getKey()), new Tag(t.getKey(), "")));
                add(new IgnoreTagAction(tr("Ignore tag ''{0}''", t), t));
                add(new EditIgnoreTagsAction());
            }
        }

        class IgnoreTagAction extends AbstractAction {
            final transient Tag tag;

            IgnoreTagAction(String name, Tag tag) {
                super(name);
                this.tag = tag;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (tagsToIgnore != null) {
                        recentTags.ignoreTag(tag, tagsToIgnore);
                        PROPERTY_TAGS_TO_IGNORE.put(tagsToIgnore.writeToString());
                    }
                } catch (SearchParseError parseError) {
                    throw new IllegalStateException(parseError);
                }
            }
        }

        class EditIgnoreTagsAction extends AbstractAction {

            EditIgnoreTagsAction() {
                super(tr("Edit ignore list"));
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                final SearchSetting newTagsToIngore = SearchAction.showSearchDialog(tagsToIgnore);
                if (newTagsToIngore == null) {
                    return;
                }
                try {
                    tagsToIgnore = newTagsToIngore;
                    recentTags.setTagsToIgnore(tagsToIgnore);
                    PROPERTY_TAGS_TO_IGNORE.put(tagsToIgnore.writeToString());
                } catch (SearchParseError parseError) {
                    warnAboutParseError(parseError);
                }
            }
        }

        /**
         * Destroy the recentTagsActions.
         */
        public void destroyActions() {
            for (JosmAction action : recentTagsActions) {
                action.destroy();
            }
            recentTagsActions.clear();
        }

        /**
         * Read tags from comboboxes and add it to all selected objects
         */
        public final void performTagAdding() {
            String key = keys.getEditItem();
            String value = values.getEditItem();
            if (key.isEmpty() || value.isEmpty())
                return;
            for (OsmPrimitive osm : sel) {
                String val = osm.get(key);
                if (val != null && !val.equals(value)) {
                    if (!warnOverwriteKey(tr("You changed the value of ''{0}'' from ''{1}'' to ''{2}''.", key, val, value),
                            "overwriteAddKey"))
                        return;
                    break;
                }
            }
            recentTags.add(new Tag(key, value));
            valueCount.put(key, new TreeMap<String, Integer>());
            AutoCompletionManager.rememberUserInput(key, value, false);
            commandCount++;
            UndoRedoHandler.getInstance().add(new ChangePropertyCommand(sel, key, value));
            changedKey = key;
            clearEntries();
        }

        protected void clearEntries() {
            keys.getEditor().setItem("");
            values.getEditor().setItem("");
        }

        public void undoAllTagsAdding() {
            UndoRedoHandler.getInstance().undo(commandCount);
        }

        private void refreshRecentTags() {
            switch (PROPERTY_REFRESH_RECENT.get()) {
                case REFRESH:
                    cacheRecentTags();
                    suggestRecentlyAddedTags();
                    break;
                case STATUS:
                    suggestRecentlyAddedTags();
                    break;
                default: // Do nothing
            }
        }
    }
}
