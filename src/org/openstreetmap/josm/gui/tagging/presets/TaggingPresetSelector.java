// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.tagging.presets.items.ComboMultiSelect;
import org.openstreetmap.josm.gui.tagging.presets.items.Key;
import org.openstreetmap.josm.gui.tagging.presets.items.KeyedItem;
import org.openstreetmap.josm.gui.tagging.presets.items.Roles;
import org.openstreetmap.josm.gui.tagging.presets.items.Roles.Role;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Utils;

/**
 * GUI component to select tagging preset: the list with filter and two checkboxes
 * @since 6068
 */
public class TaggingPresetSelector extends JPanel implements SelectionChangedListener {

    private static final int CLASSIFICATION_IN_FAVORITES = 300;
    private static final int CLASSIFICATION_NAME_MATCH = 300;
    private static final int CLASSIFICATION_GROUP_MATCH = 200;
    private static final int CLASSIFICATION_TAGS_MATCH = 100;

    private static final BooleanProperty SEARCH_IN_TAGS = new BooleanProperty("taggingpreset.dialog.search-in-tags", true);
    private static final BooleanProperty ONLY_APPLICABLE  = new BooleanProperty("taggingpreset.dialog.only-applicable-to-selection", true);

    private final JosmTextField edSearchText;
    private final JList<TaggingPreset> lsResult;
    private final JCheckBox ckOnlyApplicable;
    private final JCheckBox ckSearchInTags;
    private final Set<TaggingPresetType> typesInSelection = EnumSet.noneOf(TaggingPresetType.class);
    private boolean typesInSelectionDirty = true;
    private final transient PresetClassifications classifications = new PresetClassifications();
    private final ResultListModel lsResultModel = new ResultListModel();

    private final transient List<ListSelectionListener> listSelectionListeners = new ArrayList<>();

    private transient ActionListener dblClickListener;
    private transient ActionListener clickListener;

    private static class ResultListCellRenderer implements ListCellRenderer<TaggingPreset> {
        private final DefaultListCellRenderer def = new DefaultListCellRenderer();
        @Override
        public Component getListCellRendererComponent(JList<? extends TaggingPreset> list, TaggingPreset tp, int index,
                boolean isSelected, boolean cellHasFocus) {
            JLabel result = (JLabel) def.getListCellRendererComponent(list, tp, index, isSelected, cellHasFocus);
            result.setText(tp.getName());
            result.setIcon((Icon) tp.getValue(Action.SMALL_ICON));
            return result;
        }
    }

    private static class ResultListModel extends AbstractListModel<TaggingPreset> {

        private transient List<PresetClassification> presets = new ArrayList<>();

        public synchronized void setPresets(List<PresetClassification> presets) {
            this.presets = presets;
            fireContentsChanged(this, 0, Integer.MAX_VALUE);
        }

        @Override
        public synchronized TaggingPreset getElementAt(int index) {
            return presets.get(index).preset;
        }

        @Override
        public synchronized int getSize() {
            return presets.size();
        }

        public synchronized boolean isEmpty() {
            return presets.isEmpty();
        }
    }

    /**
     * Computes the match ration of a {@link TaggingPreset} wrt. a searchString.
     */
    public static class PresetClassification implements Comparable<PresetClassification> {
        public final TaggingPreset preset;
        public int classification;
        public int favoriteIndex;
        private final Collection<String> groups = new HashSet<>();
        private final Collection<String> names = new HashSet<>();
        private final Collection<String> tags = new HashSet<>();

        PresetClassification(TaggingPreset preset) {
            this.preset = preset;
            TaggingPreset group = preset.group;
            while (group != null) {
                Collections.addAll(groups, group.getLocaleName().toLowerCase(Locale.ENGLISH).split("\\s"));
                group = group.group;
            }
            Collections.addAll(names, preset.getLocaleName().toLowerCase(Locale.ENGLISH).split("\\s"));
            for (TaggingPresetItem item: preset.data) {
                if (item instanceof KeyedItem) {
                    tags.add(((KeyedItem) item).key);
                    if (item instanceof ComboMultiSelect) {
                        final ComboMultiSelect cms = (ComboMultiSelect) item;
                        if (Boolean.parseBoolean(cms.values_searchable)) {
                            tags.addAll(cms.getDisplayValues());
                        }
                    }
                    if (item instanceof Key && ((Key) item).value != null) {
                        tags.add(((Key) item).value);
                    }
                } else if (item instanceof Roles) {
                    for (Role role : ((Roles) item).roles) {
                        tags.add(role.key);
                    }
                }
            }
        }

        private int isMatching(Collection<String> values, String[] searchString) {
            int sum = 0;
            for (String word: searchString) {
                boolean found = false;
                boolean foundFirst = false;
                for (String value: values) {
                    int index = value.toLowerCase(Locale.ENGLISH).indexOf(word);
                    if (index == 0) {
                        foundFirst = true;
                        break;
                    } else if (index > 0) {
                        found = true;
                    }
                }
                if (foundFirst) {
                    sum += 2;
                } else if (found) {
                    sum += 1;
                } else
                    return 0;
            }
            return sum;
        }

        int isMatchingGroup(String[] words) {
            return isMatching(groups, words);
        }

        int isMatchingName(String[] words) {
            return isMatching(names, words);
        }

        int isMatchingTags(String[] words) {
            return isMatching(tags, words);
        }

        @Override
        public int compareTo(PresetClassification o) {
            int result = o.classification - classification;
            if (result == 0)
                return preset.getName().compareTo(o.preset.getName());
            else
                return result;
        }

        @Override
        public String toString() {
            return classification + " " + preset;
        }
    }

    /**
     * Constructs a new {@code TaggingPresetSelector}.
     */
    public TaggingPresetSelector(boolean displayOnlyApplicable, boolean displaySearchInTags) {
        super(new BorderLayout());
        classifications.loadPresets(TaggingPresets.getTaggingPresets());

        edSearchText = new JosmTextField();
        edSearchText.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                filterPresets();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                filterPresets();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterPresets();
            }
        });
        edSearchText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                case KeyEvent.VK_DOWN:
                    selectPreset(lsResult.getSelectedIndex() + 1);
                    break;
                case KeyEvent.VK_UP:
                    selectPreset(lsResult.getSelectedIndex() - 1);
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                    selectPreset(lsResult.getSelectedIndex() + 10);
                    break;
                case KeyEvent.VK_PAGE_UP:
                    selectPreset(lsResult.getSelectedIndex() - 10);
                    break;
                case KeyEvent.VK_HOME:
                    selectPreset(0);
                    break;
                case KeyEvent.VK_END:
                    selectPreset(lsResultModel.getSize());
                    break;
                }
            }
        });
        add(edSearchText, BorderLayout.NORTH);

        lsResult = new JList<>(lsResultModel);
        lsResult.setCellRenderer(new ResultListCellRenderer());
        lsResult.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    if (dblClickListener != null)
                        dblClickListener.actionPerformed(null);
                } else {
                    if (clickListener != null)
                        clickListener.actionPerformed(null);
                }
            }
        });
        add(new JScrollPane(lsResult), BorderLayout.CENTER);

        JPanel pnChecks = new JPanel();
        pnChecks.setLayout(new BoxLayout(pnChecks, BoxLayout.Y_AXIS));

        if (displayOnlyApplicable) {
            ckOnlyApplicable = new JCheckBox();
            ckOnlyApplicable.setText(tr("Show only applicable to selection"));
            pnChecks.add(ckOnlyApplicable);
            ckOnlyApplicable.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    filterPresets();
                }
            });
        } else {
            ckOnlyApplicable = null;
        }

        if (displaySearchInTags) {
            ckSearchInTags = new JCheckBox();
            ckSearchInTags.setText(tr("Search in tags"));
            ckSearchInTags.setSelected(SEARCH_IN_TAGS.get());
            ckSearchInTags.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    filterPresets();
                }
            });
            pnChecks.add(ckSearchInTags);
        } else {
            ckSearchInTags = null;
        }

        add(pnChecks, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(400, 300));
        filterPresets();
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(new AbstractAction(tr("Add toolbar button")) {
            @Override
            public void actionPerformed(ActionEvent ae) {
                String res = getSelectedPreset().getToolbarString();
                Main.toolbar.addCustomButton(res, -1, false);
            }
        });
        lsResult.addMouseListener(new PopupMenuLauncher(popupMenu));
    }

    private synchronized void selectPreset(int newIndex) {
        if (newIndex < 0) {
            newIndex = 0;
        }
        if (newIndex > lsResultModel.getSize() - 1) {
            newIndex = lsResultModel.getSize() - 1;
        }
        lsResult.setSelectedIndex(newIndex);
        lsResult.ensureIndexIsVisible(newIndex);
    }

    /**
     * Search expression can be in form: "group1/group2/name" where names can contain multiple words
     */
    private synchronized void filterPresets() {
        //TODO Save favorites to file
        String text = edSearchText.getText().toLowerCase(Locale.ENGLISH);
        boolean onlyApplicable = ckOnlyApplicable != null && ckOnlyApplicable.isSelected();
        boolean inTags = ckSearchInTags != null && ckSearchInTags.isSelected();

        DataSet ds = Main.main.getCurrentDataSet();
        Collection<OsmPrimitive> selected = (ds == null) ? Collections.<OsmPrimitive>emptyList() : ds.getSelected();
        final List<PresetClassification> result = classifications.getMatchingPresets(
                text, onlyApplicable, inTags, getTypesInSelection(), selected);

        TaggingPreset oldPreset = getSelectedPreset();
        lsResultModel.setPresets(result);
        TaggingPreset newPreset = getSelectedPreset();
        if (!Objects.equals(oldPreset, newPreset)) {
            int[] indices = lsResult.getSelectedIndices();
            for (ListSelectionListener listener : listSelectionListeners) {
                listener.valueChanged(new ListSelectionEvent(lsResult, lsResult.getSelectedIndex(),
                        indices.length > 0 ? indices[indices.length-1] : -1, false));
            }
        }
    }

    /**
     * A collection of {@link PresetClassification}s with the functionality of filtering wrt. searchString.
     */
    public static class PresetClassifications implements Iterable<PresetClassification> {

        private final List<PresetClassification> classifications = new ArrayList<>();

        public List<PresetClassification> getMatchingPresets(String searchText, boolean onlyApplicable, boolean inTags,
                Set<TaggingPresetType> presetTypes, final Collection<? extends OsmPrimitive> selectedPrimitives) {
            final String[] groupWords;
            final String[] nameWords;

            if (searchText.contains("/")) {
                groupWords = searchText.substring(0, searchText.lastIndexOf('/')).split("[\\s/]");
                nameWords = searchText.substring(searchText.indexOf('/') + 1).split("\\s");
            } else {
                groupWords = null;
                nameWords = searchText.split("\\s");
            }

            return getMatchingPresets(groupWords, nameWords, onlyApplicable, inTags, presetTypes, selectedPrimitives);
        }

        public List<PresetClassification> getMatchingPresets(String[] groupWords, String[] nameWords, boolean onlyApplicable,
                boolean inTags, Set<TaggingPresetType> presetTypes, final Collection<? extends OsmPrimitive> selectedPrimitives) {

            final List<PresetClassification> result = new ArrayList<>();
            for (PresetClassification presetClassification : classifications) {
                TaggingPreset preset = presetClassification.preset;
                presetClassification.classification = 0;

                if (onlyApplicable) {
                    boolean suitable = preset.typeMatches(presetTypes);

                    if (!suitable && preset.types.contains(TaggingPresetType.RELATION)
                            && preset.roles != null && !preset.roles.roles.isEmpty()) {
                        final Predicate<Role> memberExpressionMatchesOnePrimitive = new Predicate<Role>() {
                            @Override
                            public boolean evaluate(Role object) {
                                return object.memberExpression != null
                                        && Utils.exists(selectedPrimitives, object.memberExpression);
                            }
                        };
                        suitable = Utils.exists(preset.roles.roles, memberExpressionMatchesOnePrimitive);
                        // keep the preset to allow the creation of new relations
                    }
                    if (!suitable) {
                        continue;
                    }
                }

                if (groupWords != null && presetClassification.isMatchingGroup(groupWords) == 0) {
                    continue;
                }

                int matchName = presetClassification.isMatchingName(nameWords);

                if (matchName == 0) {
                    if (groupWords == null) {
                        int groupMatch = presetClassification.isMatchingGroup(nameWords);
                        if (groupMatch > 0) {
                            presetClassification.classification = CLASSIFICATION_GROUP_MATCH + groupMatch;
                        }
                    }
                    if (presetClassification.classification == 0 && inTags) {
                        int tagsMatch = presetClassification.isMatchingTags(nameWords);
                        if (tagsMatch > 0) {
                            presetClassification.classification = CLASSIFICATION_TAGS_MATCH + tagsMatch;
                        }
                    }
                } else {
                    presetClassification.classification = CLASSIFICATION_NAME_MATCH + matchName;
                }

                if (presetClassification.classification > 0) {
                    presetClassification.classification += presetClassification.favoriteIndex;
                    result.add(presetClassification);
                }
            }

            Collections.sort(result);
            return result;

        }

        public void clear() {
            classifications.clear();
        }

        public void loadPresets(Collection<TaggingPreset> presets) {
            for (TaggingPreset preset : presets) {
                if (preset instanceof TaggingPresetSeparator || preset instanceof TaggingPresetMenu) {
                    continue;
                }
                classifications.add(new PresetClassification(preset));
            }
        }

        @Override
        public Iterator<PresetClassification> iterator() {
            return classifications.iterator();
        }
    }

    private Set<TaggingPresetType> getTypesInSelection() {
        if (typesInSelectionDirty) {
            synchronized (typesInSelection) {
                typesInSelectionDirty = false;
                typesInSelection.clear();
                if (Main.main == null || Main.main.getCurrentDataSet() == null) return typesInSelection;
                for (OsmPrimitive primitive : Main.main.getCurrentDataSet().getSelected()) {
                    typesInSelection.add(TaggingPresetType.forPrimitive(primitive));
                }
            }
        }
        return typesInSelection;
    }

    @Override
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        typesInSelectionDirty = true;
    }

    public synchronized void init() {
        if (ckOnlyApplicable != null) {
            ckOnlyApplicable.setEnabled(!getTypesInSelection().isEmpty());
            ckOnlyApplicable.setSelected(!getTypesInSelection().isEmpty() && ONLY_APPLICABLE.get());
        }
        listSelectionListeners.clear();
        edSearchText.setText("");
        filterPresets();
    }

    public void init(Collection<TaggingPreset> presets) {
        classifications.clear();
        classifications.loadPresets(presets);
        init();
    }

    public synchronized void clearSelection() {
        lsResult.getSelectionModel().clearSelection();
    }

    /**
     * Save checkbox values in preferences for future reuse
     */
    public void savePreferences() {
        if (ckSearchInTags != null) {
            SEARCH_IN_TAGS.put(ckSearchInTags.isSelected());
        }
        if (ckOnlyApplicable != null && ckOnlyApplicable.isEnabled()) {
            ONLY_APPLICABLE.put(ckOnlyApplicable.isSelected());
        }
    }

    /**
     * Determines, which preset is selected at the current moment
     * @return selected preset (as action)
     */
    public synchronized TaggingPreset getSelectedPreset() {
        if (lsResultModel.isEmpty()) return null;
        int idx = lsResult.getSelectedIndex();
        if (idx < 0 || idx >= lsResultModel.getSize()) {
            idx = 0;
        }
        TaggingPreset preset = lsResultModel.getElementAt(idx);
        for (PresetClassification pc: classifications) {
            if (pc.preset == preset) {
                pc.favoriteIndex = CLASSIFICATION_IN_FAVORITES;
            } else if (pc.favoriteIndex > 0) {
                pc.favoriteIndex--;
            }
        }
        return preset;
    }

    public synchronized void setSelectedPreset(TaggingPreset p) {
        lsResult.setSelectedValue(p, true);
    }

    public synchronized int getItemCount() {
        return lsResultModel.getSize();
    }

    public void setDblClickListener(ActionListener dblClickListener) {
        this.dblClickListener = dblClickListener;
    }

    public void setClickListener(ActionListener clickListener) {
        this.clickListener = clickListener;
    }

    /**
     * Adds a selection listener to the presets list.
     * @param selectListener The list selection listener
     * @since 7412
     */
    public synchronized void addSelectionListener(ListSelectionListener selectListener) {
        lsResult.getSelectionModel().addListSelectionListener(selectListener);
        listSelectionListeners.add(selectListener);
    }

    /**
     * Removes a selection listener from the presets list.
     * @param selectListener The list selection listener
     * @since 7412
     */
    public synchronized void removeSelectionListener(ListSelectionListener selectListener) {
        listSelectionListeners.remove(selectListener);
        lsResult.getSelectionModel().removeListSelectionListener(selectListener);
    }
}
