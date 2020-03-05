// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
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
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.data.osm.DataSelectionListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmDataManager;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.tagging.presets.items.ComboMultiSelect;
import org.openstreetmap.josm.gui.tagging.presets.items.Key;
import org.openstreetmap.josm.gui.tagging.presets.items.KeyedItem;
import org.openstreetmap.josm.gui.tagging.presets.items.Roles;
import org.openstreetmap.josm.gui.tagging.presets.items.Roles.Role;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import org.openstreetmap.josm.gui.widgets.SearchTextResultListPanel;
import org.openstreetmap.josm.tools.Utils;

/**
 * GUI component to select tagging preset: the list with filter and two checkboxes
 * @since 6068
 */
public class TaggingPresetSelector extends SearchTextResultListPanel<TaggingPreset>
        implements DataSelectionListener, TaggingPresetListener {

    private static final int CLASSIFICATION_IN_FAVORITES = 300;
    private static final int CLASSIFICATION_NAME_MATCH = 300;
    private static final int CLASSIFICATION_GROUP_MATCH = 200;
    private static final int CLASSIFICATION_TAGS_MATCH = 100;

    private static final BooleanProperty SEARCH_IN_TAGS = new BooleanProperty("taggingpreset.dialog.search-in-tags", true);
    private static final BooleanProperty ONLY_APPLICABLE = new BooleanProperty("taggingpreset.dialog.only-applicable-to-selection", true);

    private final JCheckBox ckOnlyApplicable;
    private final JCheckBox ckSearchInTags;
    private final Set<TaggingPresetType> typesInSelection = EnumSet.noneOf(TaggingPresetType.class);
    private boolean typesInSelectionDirty = true;
    private final transient PresetClassifications classifications = new PresetClassifications();

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

    /**
     * Computes the match ration of a {@link TaggingPreset} wrt. a searchString.
     */
    public static class PresetClassification implements Comparable<PresetClassification> {
        public final TaggingPreset preset;
        public int classification;
        public int favoriteIndex;
        private final Collection<String> groups;
        private final Collection<String> names;
        private final Collection<String> tags;

        PresetClassification(TaggingPreset preset) {
            this.preset = preset;
            Set<String> groupSet = new HashSet<>();
            Set<String> nameSet = new HashSet<>();
            Set<String> tagSet = new HashSet<>();
            TaggingPreset group = preset.group;
            while (group != null) {
                addLocaleNames(groupSet, group);
                group = group.group;
            }
            addLocaleNames(nameSet, preset);
            for (TaggingPresetItem item: preset.data) {
                if (item instanceof KeyedItem) {
                    tagSet.add(((KeyedItem) item).key);
                    if (item instanceof ComboMultiSelect) {
                        final ComboMultiSelect cms = (ComboMultiSelect) item;
                        if (cms.values_searchable) {
                            tagSet.addAll(cms.getDisplayValues());
                        }
                    }
                    if (item instanceof Key && ((Key) item).value != null) {
                        tagSet.add(((Key) item).value);
                    }
                } else if (item instanceof Roles) {
                    for (Role role : ((Roles) item).roles) {
                        tagSet.add(role.key);
                    }
                }
            }
            this.groups = Utils.toUnmodifiableList(groupSet);
            this.names = Utils.toUnmodifiableList(nameSet);
            this.tags = Utils.toUnmodifiableList(tagSet);
        }

        private static void addLocaleNames(Collection<String> collection, TaggingPreset preset) {
            String locName = preset.getLocaleName();
            if (locName != null) {
                Collections.addAll(collection, locName.toLowerCase(Locale.ENGLISH).split("\\s"));
            }
        }

        private static String simplifyString(String s) {
            return Utils.deAccent(s).toLowerCase(Locale.ENGLISH).replaceAll("\\p{Punct}", "");
        }

        private static int isMatching(Collection<String> values, String... searchString) {
            int sum = 0;
            List<String> deaccentedValues = values.stream()
                    .map(PresetClassification::simplifyString).collect(Collectors.toList());
            for (String word: searchString) {
                boolean found = false;
                boolean foundFirst = false;
                String deaccentedWord = simplifyString(word);
                for (String value: deaccentedValues) {
                    int index = value.indexOf(deaccentedWord);
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

        int isMatchingGroup(String... words) {
            return isMatching(groups, words);
        }

        int isMatchingName(String... words) {
            return isMatching(names, words);
        }

        int isMatchingTags(String... words) {
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
            return Integer.toString(classification) + ' ' + preset;
        }
    }

    /**
     * Constructs a new {@code TaggingPresetSelector}.
     * @param displayOnlyApplicable if {@code true} display "Show only applicable to selection" checkbox
     * @param displaySearchInTags if {@code true} display "Search in tags" checkbox
     */
    public TaggingPresetSelector(boolean displayOnlyApplicable, boolean displaySearchInTags) {
        super();
        lsResult.setCellRenderer(new ResultListCellRenderer());
        classifications.loadPresets(TaggingPresets.getTaggingPresets());
        TaggingPresets.addListener(this);

        JPanel pnChecks = new JPanel();
        pnChecks.setLayout(new BoxLayout(pnChecks, BoxLayout.Y_AXIS));

        if (displayOnlyApplicable) {
            ckOnlyApplicable = new JCheckBox();
            ckOnlyApplicable.setText(tr("Show only applicable to selection"));
            pnChecks.add(ckOnlyApplicable);
            ckOnlyApplicable.addItemListener(e -> filterItems());
        } else {
            ckOnlyApplicable = null;
        }

        if (displaySearchInTags) {
            ckSearchInTags = new JCheckBox();
            ckSearchInTags.setText(tr("Search in tags"));
            ckSearchInTags.setSelected(SEARCH_IN_TAGS.get());
            ckSearchInTags.addItemListener(e -> filterItems());
            pnChecks.add(ckSearchInTags);
        } else {
            ckSearchInTags = null;
        }

        add(pnChecks, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(400, 300));
        filterItems();
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.add(new AbstractAction(tr("Add toolbar button")) {
            @Override
            public void actionPerformed(ActionEvent ae) {
                final TaggingPreset preset = getSelectedPreset();
                if (preset != null) {
                    MainApplication.getToolbar().addCustomButton(preset.getToolbarString(), -1, false);
                }
            }
        });
        lsResult.addMouseListener(new PopupMenuLauncher(popupMenu));
    }

    /**
     * Search expression can be in form: "group1/group2/name" where names can contain multiple words
     */
    @Override
    protected synchronized void filterItems() {
        //TODO Save favorites to file
        String text = edSearchText.getText().toLowerCase(Locale.ENGLISH);
        boolean onlyApplicable = ckOnlyApplicable != null && ckOnlyApplicable.isSelected();
        boolean inTags = ckSearchInTags != null && ckSearchInTags.isSelected();

        DataSet ds = OsmDataManager.getInstance().getEditDataSet();
        Collection<OsmPrimitive> selected = (ds == null) ? Collections.<OsmPrimitive>emptyList() : ds.getSelected();
        final List<PresetClassification> result = classifications.getMatchingPresets(
                text, onlyApplicable, inTags, getTypesInSelection(), selected);

        final TaggingPreset oldPreset = getSelectedPreset();
        lsResultModel.setItems(Utils.transform(result, x -> x.preset));
        final TaggingPreset newPreset = getSelectedPreset();
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
                        suitable = preset.roles.roles.stream().anyMatch(
                                object -> object.memberExpression != null && selectedPrimitives.stream().anyMatch(object.memberExpression));
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
                if (OsmDataManager.getInstance().getEditDataSet() == null) return typesInSelection;
                for (OsmPrimitive primitive : OsmDataManager.getInstance().getEditDataSet().getSelected()) {
                    typesInSelection.add(TaggingPresetType.forPrimitive(primitive));
                }
            }
        }
        return typesInSelection;
    }

    @Override
    public void selectionChanged(SelectionChangeEvent event) {
        typesInSelectionDirty = true;
    }

    @Override
    public synchronized void init() {
        if (ckOnlyApplicable != null) {
            ckOnlyApplicable.setEnabled(!getTypesInSelection().isEmpty());
            ckOnlyApplicable.setSelected(!getTypesInSelection().isEmpty() && ONLY_APPLICABLE.get());
        }
        super.init();
    }

    public void init(Collection<TaggingPreset> presets) {
        classifications.clear();
        classifications.loadPresets(presets);
        init();
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
     * Determines, which preset is selected at the moment.
     * @return selected preset (as action)
     */
    public synchronized TaggingPreset getSelectedPreset() {
        if (lsResultModel.isEmpty()) return null;
        int idx = lsResult.getSelectedIndex();
        if (idx < 0 || idx >= lsResultModel.getSize()) {
            idx = 0;
        }
        return lsResultModel.getElementAt(idx);
    }

    /**
     * Determines, which preset is selected at the moment. Updates {@link PresetClassification#favoriteIndex}!
     * @return selected preset (as action)
     */
    public synchronized TaggingPreset getSelectedPresetAndUpdateClassification() {
        final TaggingPreset preset = getSelectedPreset();
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

    @Override
    public void taggingPresetsModified() {
        classifications.clear();
        classifications.loadPresets(TaggingPresets.getTaggingPresets());
    }
}
