// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

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
import java.util.LinkedList;
import java.util.List;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences;
import org.openstreetmap.josm.gui.preferences.map.TaggingPresetPreference;
import org.openstreetmap.josm.gui.tagging.TaggingPresetItems.Key;
import org.openstreetmap.josm.gui.tagging.TaggingPresetItems.KeyedItem;
import org.openstreetmap.josm.gui.tagging.TaggingPresetItems.Role;
import org.openstreetmap.josm.gui.tagging.TaggingPresetItems.Roles;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.ListPopupMenu;
import org.openstreetmap.josm.gui.widgets.PopupMenuLauncher;
import static org.openstreetmap.josm.tools.I18n.tr;

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

    
    private JosmTextField edSearchText;
    private JList lsResult;
    private JCheckBox ckOnlyApplicable;
    private JCheckBox ckSearchInTags;
    private final EnumSet<TaggingPresetType> typesInSelection = EnumSet.noneOf(TaggingPresetType.class);
    private boolean typesInSelectionDirty = true;
    private final List<PresetClassification> classifications = new ArrayList<PresetClassification>();
    private ResultListModel lsResultModel = new ResultListModel();
    private JPopupMenu popupMenu;

    private ActionListener dblClickListener;
    private ActionListener clickListener;

    private static class ResultListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            JLabel result = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            TaggingPreset tp = (TaggingPreset)value;
            result.setText(tp.getName());
            result.setIcon((Icon) tp.getValue(Action.SMALL_ICON));
            return result;
        }
    }

    private static class ResultListModel extends AbstractListModel {

        private List<PresetClassification> presets = new ArrayList<PresetClassification>();

        public void setPresets(List<PresetClassification> presets) {
            this.presets = presets;
            fireContentsChanged(this, 0, Integer.MAX_VALUE);
        }

        public List<PresetClassification> getPresets() {
            return presets;
        }

        @Override
        public Object getElementAt(int index) {
            return presets.get(index).preset;
        }

        @Override
        public int getSize() {
            return presets.size();
        }

    }

    private static class PresetClassification implements Comparable<PresetClassification> {
        public final TaggingPreset preset;
        public int classification;
        public int favoriteIndex;
        private final Collection<String> groups = new HashSet<String>();
        private final Collection<String> names = new HashSet<String>();
        private final Collection<String> tags = new HashSet<String>();

        PresetClassification(TaggingPreset preset) {
            this.preset = preset;
            TaggingPreset group = preset.group;
            while (group != null) {
                Collections.addAll(groups, group.getLocaleName().toLowerCase().split("\\s"));
                group = group.group;
            }
            Collections.addAll(names, preset.getLocaleName().toLowerCase().split("\\s"));
            for (TaggingPresetItem item: preset.data) {
                if (item instanceof KeyedItem) {
                    tags.add(((KeyedItem) item).key);
                    if (item instanceof TaggingPresetItems.ComboMultiSelect) {
                        final TaggingPresetItems.ComboMultiSelect cms = (TaggingPresetItems.ComboMultiSelect) item;
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
                    int index = value.toLowerCase().indexOf(word);
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
            return classification + " " + preset.toString();
        }
    }

    /**
     * Constructs a new {@code TaggingPresetSelector}.
     */
    public TaggingPresetSelector(boolean displayOnlyApplicable, boolean displaySearchInTags) {
        super(new BorderLayout());
        if (TaggingPresetPreference.taggingPresets!=null) {
            loadPresets(TaggingPresetPreference.taggingPresets);
        }
        
        edSearchText = new JosmTextField();
        edSearchText.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void removeUpdate(DocumentEvent e) { filterPresets(); }
            @Override public void insertUpdate(DocumentEvent e) { filterPresets(); }
            @Override public void changedUpdate(DocumentEvent e) { filterPresets(); }
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

        lsResult = new JList();
        lsResult.setModel(lsResultModel);
        lsResult.setCellRenderer(new ResultListCellRenderer());
        lsResult.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount()>1) {
                    if (dblClickListener!=null)
                        dblClickListener.actionPerformed(null);
                } else {
                    if (clickListener!=null)
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
        }

        add(pnChecks, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(400, 300));
        filterPresets();
        popupMenu = new JPopupMenu();
        popupMenu.add(new AbstractAction(tr("Add toolbar button")) {
            @Override
            public void actionPerformed(ActionEvent ae) {
                String res = getSelectedPreset().getToolbarString();
                Main.toolbar.addCustomButton(res, -1, false);
            }
        });
        lsResult.addMouseListener(new PopupMenuLauncher(popupMenu));
    }
    
    private void selectPreset(int newIndex) {
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
    private void filterPresets() {
        //TODO Save favorites to file
        String text = edSearchText.getText().toLowerCase();

        String[] groupWords;
        String[] nameWords;

        if (text.contains("/")) {
            groupWords = text.substring(0, text.lastIndexOf('/')).split("[\\s/]");
            nameWords = text.substring(text.indexOf('/') + 1).split("\\s");
        } else {
            groupWords = null;
            nameWords = text.split("\\s");
        }

        boolean onlyApplicable = ckOnlyApplicable != null && ckOnlyApplicable.isSelected();
        boolean inTags = ckSearchInTags != null && ckSearchInTags.isSelected();

        List<PresetClassification> result = new ArrayList<PresetClassification>();
        PRESET_LOOP:
            for (PresetClassification presetClasification: classifications) {
                TaggingPreset preset = presetClasification.preset;
                presetClasification.classification = 0;

                if (onlyApplicable && preset.types != null) {
                    boolean found = false;
                    for (TaggingPresetType type: preset.types) {
                        if (getTypesInSelection().contains(type)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        continue;
                    }
                }

                if (groupWords != null && presetClasification.isMatchingGroup(groupWords) == 0) {
                    continue PRESET_LOOP;
                }

                int matchName = presetClasification.isMatchingName(nameWords);

                if (matchName == 0) {
                    if (groupWords == null) {
                        int groupMatch = presetClasification.isMatchingGroup(nameWords);
                        if (groupMatch > 0) {
                            presetClasification.classification = CLASSIFICATION_GROUP_MATCH + groupMatch;
                        }
                    }
                    if (presetClasification.classification == 0 && inTags) {
                        int tagsMatch = presetClasification.isMatchingTags(nameWords);
                        if (tagsMatch > 0) {
                            presetClasification.classification = CLASSIFICATION_TAGS_MATCH + tagsMatch;
                        }
                    }
                } else {
                    presetClasification.classification = CLASSIFICATION_NAME_MATCH + matchName;
                }

                if (presetClasification.classification > 0) {
                    presetClasification.classification += presetClasification.favoriteIndex;
                    result.add(presetClasification);
                }
            }

        Collections.sort(result);
        lsResultModel.setPresets(result);

    }
    
    private EnumSet<TaggingPresetType> getTypesInSelection() {
        if (typesInSelectionDirty) {
            synchronized (typesInSelection) {
                typesInSelectionDirty = false;
                typesInSelection.clear();
                if (Main.main==null || Main.main.getCurrentDataSet() == null) return typesInSelection;
                for (OsmPrimitive primitive : Main.main.getCurrentDataSet().getSelected()) {
                    if (primitive instanceof Node) {
                        typesInSelection.add(TaggingPresetType.NODE);
                    } else if (primitive instanceof Way) {
                        typesInSelection.add(TaggingPresetType.WAY);
                        if (((Way) primitive).isClosed()) {
                            typesInSelection.add(TaggingPresetType.CLOSEDWAY);
                        }
                    } else if (primitive instanceof Relation) {
                        typesInSelection.add(TaggingPresetType.RELATION);
                    }
                }
            }
        }
        return typesInSelection;
    }
    
    @Override
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        typesInSelectionDirty = true;
    }

    public void init() {
        if (ckOnlyApplicable != null) {
            ckOnlyApplicable.setEnabled(!getTypesInSelection().isEmpty());
            ckOnlyApplicable.setSelected(!getTypesInSelection().isEmpty() && ONLY_APPLICABLE.get());
        }
        edSearchText.setText("");
        filterPresets();
    }
    
    public void init(Collection<TaggingPreset> presets) {
        classifications.clear();
        loadPresets(presets);
        init();
    }

    
    public void clearSelection() {
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
    public TaggingPreset getSelectedPreset() {
        List<PresetClassification> presets = lsResultModel.getPresets();
        if (presets.isEmpty()) return null;
        int idx = lsResult.getSelectedIndex();
        if (idx == -1) {
            idx = 0;
        }
        TaggingPreset preset = presets.get(idx).preset;
        for (PresetClassification pc: classifications) {
            if (pc.preset == preset) {
                pc.favoriteIndex = CLASSIFICATION_IN_FAVORITES;
            } else if (pc.favoriteIndex > 0) {
                pc.favoriteIndex--;
            }
        }
        return preset;
    }

    private void loadPresets(Collection<TaggingPreset> presets) {
        for (TaggingPreset preset: presets) {
            if (preset instanceof TaggingPresetSeparator || preset instanceof TaggingPresetMenu) {
                continue;
            }
            classifications.add(new PresetClassification(preset));
        }
    }

    public void setSelectedPreset(TaggingPreset p) {
        lsResult.setSelectedValue(p, true);
    }
    
    public int getItemCount() {
        return lsResultModel.getSize();
    }
    
    public void setDblClickListener(ActionListener dblClickListener) {
        this.dblClickListener = dblClickListener;
    }
    
    public void setClickListener(ActionListener clickListener) {
        this.clickListener = clickListener;
    }
    
    public void addSelectionListener(final ActionListener selectListener) {
        lsResult.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting())
                    selectListener.actionPerformed(null);
            }
        });
    }
}
