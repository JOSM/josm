// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.download;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.text.JTextComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.AbstractTextComponentValidator;
import org.openstreetmap.josm.gui.widgets.DefaultTextComponentValidator;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.gui.widgets.SearchTextResultListPanel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;

/**
 * A component to select user saved Overpass queries.
 * @since 12574
 */
public final class OverpassQueryList extends SearchTextResultListPanel<OverpassQueryList.SelectorItem> {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss, dd-MM-yyyy");

    /*
     * GUI elements
     */
    private final JTextComponent target;
    private final Component componentParent;

    /*
     * All loaded elements within the list.
     */
    private final transient Map<String, SelectorItem> items;

    /*
     * Preferences
     */
    private static final String KEY_KEY = "key";
    private static final String QUERY_KEY = "query";
    private static final String USE_COUNT_KEY = "useCount";
    private static final String PREFERENCE_ITEMS = "download.overpass.query";

    /**
     * Constructs a new {@code OverpassQueryList}.
     * @param parent The parent of this component.
     * @param target The text component to which the queries must be added.
     */
    public OverpassQueryList(Component parent, JTextComponent target) {
        this.target = target;
        this.componentParent = parent;
        this.items = restorePreferences();

        OverpassQueryListMouseAdapter mouseHandler = new OverpassQueryListMouseAdapter(lsResult, lsResultModel);
        super.lsResult.setCellRenderer(new OverpassQueryCellRendered());
        super.setDblClickListener(this::getDblClickListener);
        super.lsResult.addMouseListener(mouseHandler);
        super.lsResult.addMouseMotionListener(mouseHandler);

        filterItems();
    }

    /**
     * Returns currently selected element from the list.
     * @return An {@link Optional#empty()} if nothing is selected, otherwise
     * the idem is returned.
     */
    public synchronized Optional<SelectorItem> getSelectedItem() {
        int idx = lsResult.getSelectedIndex();
        if (lsResultModel.getSize() == 0 || idx == -1) {
            return Optional.empty();
        }

        SelectorItem item = lsResultModel.getElementAt(idx);
        item.increaseUsageCount();

        this.items.values().stream()
                .filter(it -> !it.getKey().equals(item.getKey()))
                .forEach(SelectorItem::decreaseUsageCount);

        filterItems();

        return Optional.of(item);
    }

    /**
     * Adds a new historic item to the list. The key has form 'history {current date}'.
     * Note, the item is not saved if there is already a historic item with the same query.
     * @param query The query of the item.
     * @exception IllegalArgumentException if the query is empty.
     * @exception NullPointerException if the query is {@code null}.
     */
    public synchronized void saveHistoricItem(String query) {
        boolean historicExist = this.items.values().stream()
                .filter(it -> it.getKey().contains("history"))
                .map(SelectorItem::getQuery)
                .anyMatch(q -> q.equals(query));

        if (!historicExist) {
            SelectorItem item = new SelectorItem(
                    "history " + LocalDateTime.now().format(FORMAT),
                    query);

            this.items.put(item.getKey(), item);

            savePreferences();
            filterItems();
        }
    }

    /**
     * Removes currently selected item, saves the current state to preferences and
     * updates the view.
     */
    private synchronized void removeSelectedItem() {
        Optional<SelectorItem> it = this.getSelectedItem();

        if (!it.isPresent()) {
            JOptionPane.showMessageDialog(
                    componentParent,
                    tr("Please select an item first"));
            return;
        }

        SelectorItem item = it.get();
        if (this.items.remove(item.getKey(), item)) {
            savePreferences();
            filterItems();
        }
    }

    /**
     * Opens {@link EditItemDialog} for the selected item, saves the current state
     * to preferences and updates the view.
     */
    private synchronized void editSelectedItem() {
        Optional<SelectorItem> it = this.getSelectedItem();

        if (!it.isPresent()) {
            JOptionPane.showMessageDialog(
                    componentParent,
                    tr("Please select an item first"));
            return;
        }

        SelectorItem item = it.get();

        EditItemDialog dialog = new EditItemDialog(
                componentParent,
                tr("Edit item"),
                item.getKey(),
                item.getQuery(),
                new String[] {tr("Save")});
        dialog.showDialog();

        Optional<SelectorItem> editedItem = dialog.getOutputItem();
        editedItem.ifPresent(i -> {
            this.items.remove(item.getKey(), item);
            this.items.put(i.getKey(), i);

            savePreferences();
            filterItems();
        });
    }

    /**
     * Opens {@link EditItemDialog}, saves the state to preferences if a new item is added
     * and updates the view.
     */
    private synchronized void createNewItem() {
        EditItemDialog dialog = new EditItemDialog(componentParent, tr("Add snippet"), tr("Add"));
        dialog.showDialog();

        Optional<SelectorItem> newItem = dialog.getOutputItem();
        newItem.ifPresent(i -> {
            items.put(i.getKey(), new SelectorItem(i.getKey(), i.getQuery()));
            savePreferences();
            filterItems();
        });
    }

    @Override
    public void setDblClickListener(ActionListener dblClickListener) {
        // this listener is already set within this class
    }

    @Override
    protected void filterItems() {
        String text = edSearchText.getText().toLowerCase(Locale.ENGLISH);

        super.lsResultModel.setItems(this.items.values().stream()
                .filter(item -> item.getKey().contains(text))
                .collect(Collectors.toList()));
    }

    private void getDblClickListener(ActionEvent e) {
        Optional<SelectorItem> selectedItem = this.getSelectedItem();

        if (!selectedItem.isPresent()) {
            return;
        }

        SelectorItem item = selectedItem.get();
        this.target.setText(item.getQuery());
    }

    /**
     * Saves all elements from the list to {@link Main#pref}.
     */
    private void savePreferences() {
        Collection<Map<String, String>> toSave = new ArrayList<>(this.items.size());
        for (SelectorItem item : this.items.values()) {
            Map<String, String> it = new HashMap<>();
            it.put(KEY_KEY, item.getKey());
            it.put(QUERY_KEY, item.getQuery());
            it.put(USE_COUNT_KEY, Integer.toString(item.getUsageCount()));

            toSave.add(it);
        }

        Main.pref.putListOfStructs(PREFERENCE_ITEMS, toSave);
    }

    /**
     * Loads the user saved items from {@link Main#pref}.
     * @return A set of the user saved items.
     */
    private static Map<String, SelectorItem> restorePreferences() {
        Collection<Map<String, String>> toRetrieve =
                Main.pref.getListOfStructs(PREFERENCE_ITEMS, Collections.emptyList());
        Map<String, SelectorItem> result = new HashMap<>();

        for (Map<String, String> entry : toRetrieve) {
            String key = entry.get(KEY_KEY);
            String query = entry.get(QUERY_KEY);
            int usageCount = Integer.parseInt(entry.get(USE_COUNT_KEY));

            result.put(key, new SelectorItem(key, query, usageCount));
        }

        return result;
    }

    private class OverpassQueryListMouseAdapter extends MouseAdapter {

        private final JList<SelectorItem> list;
        private final ResultListModel<SelectorItem> model;
        private final JPopupMenu emptySelectionPopup = new JPopupMenu();
        private final JPopupMenu elementPopup = new JPopupMenu();

        OverpassQueryListMouseAdapter(JList<SelectorItem> list, ResultListModel<SelectorItem> listModel) {
            this.list = list;
            this.model = listModel;

            this.initPopupMenus();
        }

        /*
         * Do not select the closest element if the user clicked on
         * an empty area within the list.
         */
        private int locationToIndex(Point p) {
            int idx = list.locationToIndex(p);

            if (idx != -1 && !list.getCellBounds(idx, idx).contains(p)) {
                return -1;
            } else {
                return idx;
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            super.mouseClicked(e);
            if (SwingUtilities.isRightMouseButton(e)) {
                int index = locationToIndex(e.getPoint());

                if (model.getSize() == 0 || index == -1) {
                    list.clearSelection();
                    emptySelectionPopup.show(list, e.getX(), e.getY());
                } else {
                    list.setSelectedIndex(index);
                    list.ensureIndexIsVisible(index);
                    elementPopup.show(list, e.getX(), e.getY());
                }
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            super.mouseMoved(e);
            int idx = locationToIndex(e.getPoint());
            if (idx == -1) {
                return;
            }

            SelectorItem item = model.getElementAt(idx);
            list.setToolTipText("<html><pre style='width:300px;'>" +
                    Utils.escapeReservedCharactersHTML(Utils.restrictStringLines(item.getQuery(), 9)));
        }

        private void initPopupMenus() {
            AbstractAction add = new AbstractAction(tr("Add")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    createNewItem();
                }
            };
            AbstractAction edit = new AbstractAction(tr("Edit")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    editSelectedItem();
                }
            };
            AbstractAction remove = new AbstractAction(tr("Remove")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    removeSelectedItem();
                }
            };
            this.emptySelectionPopup.add(add);
            this.elementPopup.add(add);
            this.elementPopup.add(edit);
            this.elementPopup.add(remove);
        }
    }

    /**
     * This class defines the way each element is rendered in the list.
     */
    private static class OverpassQueryCellRendered extends JLabel implements ListCellRenderer<SelectorItem> {

        OverpassQueryCellRendered() {
            setOpaque(true);
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends SelectorItem> list,
                SelectorItem value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {

            Font font = list.getFont();
            if (isSelected) {
                setFont(new Font(font.getFontName(), Font.BOLD, font.getSize() + 2));
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setFont(new Font(font.getFontName(), Font.PLAIN, font.getSize() + 2));
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            setEnabled(list.isEnabled());
            setText(value.getKey());

            if (isSelected && cellHasFocus) {
                setBorder(new CompoundBorder(
                        BorderFactory.createLineBorder(Color.BLACK, 1),
                        BorderFactory.createEmptyBorder(2, 0, 2, 0)));
            } else {
                setBorder(new CompoundBorder(
                        null,
                        BorderFactory.createEmptyBorder(2, 0, 2, 0)));
            }

            return this;
        }
    }

    /**
     * Dialog that provides functionality to add/edit an item from the list.
     */
    private final class EditItemDialog extends ExtendedDialog {

        private final JTextField name;
        private final JosmTextArea query;
        private final int initialNameHash;

        private final transient AbstractTextComponentValidator queryValidator;
        private final transient AbstractTextComponentValidator nameValidator;

        private static final int SUCCESS_BTN = 0;
        private static final int CANCEL_BTN = 1;

        /**
         * Added/Edited object to be returned. If {@link Optional#empty()} then probably
         * the user closed the dialog, otherwise {@link SelectorItem} is present.
         */
        private transient Optional<SelectorItem> outputItem = Optional.empty();

        EditItemDialog(Component parent, String title, String... buttonTexts) {
            this(parent, title, "", "", buttonTexts);
        }

        EditItemDialog(
                Component parent,
                String title,
                String nameToEdit,
                String queryToEdit,
                String... buttonTexts) {
            super(parent, title, buttonTexts);

            this.initialNameHash = nameToEdit.hashCode();

            this.name = new JTextField(nameToEdit);
            this.query = new JosmTextArea(queryToEdit);

            this.queryValidator = new DefaultTextComponentValidator(this.query, "", tr("Query cannot be empty"));
            this.nameValidator = new AbstractTextComponentValidator(this.name) {
                @Override
                public void validate() {
                    if (isValid()) {
                        feedbackValid(tr("This name can be used for the item"));
                    } else {
                        feedbackInvalid(tr("Item with this name already exists"));
                    }
                }

                @Override
                public boolean isValid() {
                    String currentName = name.getText();
                    int currentHash = currentName.hashCode();

                    return !Utils.isStripEmpty(currentName) &&
                            !(currentHash != initialNameHash &&
                                    items.containsKey(currentName));
                }
            };

            this.name.getDocument().addDocumentListener(this.nameValidator);
            this.query.getDocument().addDocumentListener(this.queryValidator);

            JPanel panel = new JPanel(new GridBagLayout());
            JScrollPane queryScrollPane = GuiHelper.embedInVerticalScrollPane(this.query);
            queryScrollPane.getVerticalScrollBar().setUnitIncrement(10); // make scrolling smooth

            GBC constraint = GBC.eol().insets(8, 0, 8, 8).anchor(GBC.CENTER).fill(GBC.HORIZONTAL);
            constraint.ipady = 250;
            panel.add(this.name, GBC.eol().insets(5).anchor(GBC.SOUTHEAST).fill(GBC.HORIZONTAL));
            panel.add(queryScrollPane, constraint);

            setDefaultButton(SUCCESS_BTN);
            setCancelButton(CANCEL_BTN);
            setPreferredSize(new Dimension(400, 400));
            setContent(panel, false);
        }

        /**
         * Gets a new {@link SelectorItem} if one was created/modified.
         * @return A {@link SelectorItem} object created out of the fields of the dialog.
         */
        public Optional<SelectorItem> getOutputItem() {
            return this.outputItem;
        }

        @Override
        protected void buttonAction(int buttonIndex, ActionEvent evt) {
            if (buttonIndex == SUCCESS_BTN) {
                if (!this.nameValidator.isValid()) {
                    JOptionPane.showMessageDialog(
                            componentParent,
                            tr("The item cannot be created with provided name"),
                            tr("Warning"),
                            JOptionPane.WARNING_MESSAGE);
                } else if (!this.queryValidator.isValid()) {
                    JOptionPane.showMessageDialog(
                            componentParent,
                            tr("The item cannot be created with an empty query"),
                            tr("Warning"),
                            JOptionPane.WARNING_MESSAGE);
                } else {
                    this.outputItem = Optional.of(new SelectorItem(this.name.getText(), this.query.getText()));
                    super.buttonAction(buttonIndex, evt);
                }
            } else {
                super.buttonAction(buttonIndex, evt);
            }
        }
    }

    /**
     * This class represents an Overpass query used by the user that can be
     * shown within {@link OverpassQueryList}.
     */
    public static class SelectorItem {
        private final String itemKey;
        private final String query;
        private int usageCount;

        /**
         * Constructs a new {@code SelectorItem}.
         * @param key The key of this item.
         * @param query The query of the item.
         * @exception NullPointerException if any parameter is {@code null}.
         * @exception IllegalArgumentException if any parameter is empty.
         */
        public SelectorItem(String key, String query) {
            this(key, query, 1);
        }

        /**
         * Constructs a new {@code SelectorItem}.
         * @param key The key of this item.
         * @param query The query of the item.
         * @param usageCount The number of times this query was used.
         * @exception NullPointerException if any parameter is {@code null}.
         * @exception IllegalArgumentException if any parameter is empty.
         */
        public SelectorItem(String key, String query, int usageCount) {
            Objects.requireNonNull(key);
            Objects.requireNonNull(query);

            if (Utils.isStripEmpty(key)) {
                throw new IllegalArgumentException("The key of the item cannot be empty");
            }
            if (Utils.isStripEmpty(query)) {
                throw new IllegalArgumentException("The query cannot be empty");
            }

            this.itemKey = key;
            this.query = query;
            this.usageCount = usageCount;
        }

        /**
         * Gets the key (a string that is displayed in the selector) of this item.
         * @return A string representing the key of this item.
         */
        public String getKey() {
            return this.itemKey;
        }

        /**
         * Gets the overpass query of this item.
         * @return A string representing the overpass query of this item.
         */
        public String getQuery() {
            return this.query;
        }

        /**
         * Gets the number of times the query was used by the user.
         * @return The usage count of this item.
         */
        public int getUsageCount() {
            return this.usageCount;
        }

        /**
         * Increments the {@link SelectorItem#usageCount} by one till
         * it reaches {@link Integer#MAX_VALUE}.
         */
        public void increaseUsageCount() {
            if (this.usageCount < Integer.MAX_VALUE) {
                this.usageCount++;
            }
        }

        /**
         * Decrements the {@link SelectorItem#usageCount} ny one till
         * it reaches 0.
         */
        public void decreaseUsageCount() {
            if (this.usageCount > 0) {
                this.usageCount--;
            }
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((itemKey == null) ? 0 : itemKey.hashCode());
            result = prime * result + ((query == null) ? 0 : query.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            SelectorItem other = (SelectorItem) obj;
            if (itemKey == null) {
                if (other.itemKey != null) {
                    return false;
                }
            } else if (!itemKey.equals(other.itemKey)) {
                return false;
            }
            if (query == null) {
                if (other.query != null) {
                    return false;
                }
            } else if (!query.equals(other.query)) {
                return false;
            }
            return true;
        }
    }
}
