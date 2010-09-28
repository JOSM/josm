// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
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
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.preferences.TaggingPresetPreference;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.Check;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.Combo;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.Item;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.Key;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.PresetType;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.Role;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.Roles;
import org.openstreetmap.josm.gui.tagging.TaggingPreset.Text;

public class TaggingPresetSearchDialog extends ExtendedDialog {

    private int CLASSIFICATION_NAME_MATCH = 300;
    private int CLASSIFICATION_GROUP_MATCH = 200;
    private int CLASSIFICATION_TAGS_MATCH = 100;

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

        private List<PresetClasification> presets = new ArrayList<PresetClasification>();

        public void setPresets(List<PresetClasification> presets) {
            this.presets = presets;
            fireContentsChanged(this, 0, Integer.MAX_VALUE);
        }

        public List<PresetClasification> getPresets() {
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

    private static class PresetClasification implements Comparable<PresetClasification> {
        public final TaggingPreset preset;
        public int classification;
        private final Collection<String> groups = new HashSet<String>();
        private final Collection<String> names = new HashSet<String>();
        private final Collection<String> tags = new HashSet<String>();

        PresetClasification(TaggingPreset preset) {
            this.preset = preset;
            TaggingPreset group = preset.group;
            while (group != null) {
                for (String word: group.getLocaleName().toLowerCase().split("\\s")) {
                    groups.add(word);
                }
                group = group.group;
            }
            for (String word: preset.getLocaleName().toLowerCase().split("\\s")) {
                names.add(word);
            }
            for (Item item: preset.data) {
                if (item instanceof Check) {
                    tags.add(((Check)item).key.toLowerCase());
                } else if (item instanceof Combo) {
                    // Should combo values also be added?
                    tags.add(((Combo)item).key);
                } else if (item instanceof Key) {
                    tags.add(((Key) item).key);
                    tags.add(((Key) item).value);
                } else if (item instanceof Text) {
                    tags.add(((Text) item).key);
                } else if (item instanceof Roles) {
                    for (Role role: ((Roles) item).roles) {
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
                    int index = value.indexOf(word);
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
        public int compareTo(PresetClasification o) {
            int result = o.classification - classification;
            if (result == 0)
                return preset.getName().compareTo(o.preset.getName());
            else
                return result;
        }
    }


    private JTextField edSearchText;
    private JList lsResult;
    private JCheckBox ckOnlyApplicable;
    private JCheckBox ckSearchInTags;
    private final EnumSet<PresetType> typesInSelection = EnumSet.noneOf(PresetType.class);
    private final List<PresetClasification> classifications = new ArrayList<PresetClasification>();
    private ResultListModel lsResultModel = new ResultListModel();

    public TaggingPresetSearchDialog(Component parent) {
        super(parent, tr("Presets"), new String[] {tr("Select"), tr("Cancel")});
        getTypesInSelection();

        for (TaggingPreset preset: TaggingPresetPreference.taggingPresets) {
            if (preset instanceof TaggingPresetSeparator || preset instanceof TaggingPresetMenu) {
                continue;
            }

            classifications.add(new PresetClasification(preset));
        }

        build();
        filterPresets("");
    }

    private void build() {
        JPanel content = new JPanel();
        content.setLayout(new BorderLayout());

        edSearchText = new JTextField();
        edSearchText.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterPresets(edSearchText.getText());
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                filterPresets(edSearchText.getText());

            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterPresets(edSearchText.getText());

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
        content.add(edSearchText, BorderLayout.NORTH);

        lsResult = new JList();
        lsResult.setModel(lsResultModel);
        lsResult.setCellRenderer(new ResultListCellRenderer());
        lsResult.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount()>1) {
                    buttonAction(0, null);
                }
            }
        });
        content.add(new JScrollPane(lsResult), BorderLayout.CENTER);

        JPanel pnChecks = new JPanel();
        pnChecks.setLayout(new BoxLayout(pnChecks, BoxLayout.Y_AXIS));

        ckOnlyApplicable = new JCheckBox();
        ckOnlyApplicable.setText(tr("Show only applicable to selection"));
        pnChecks.add(ckOnlyApplicable);

        if (typesInSelection.isEmpty()) {
            ckOnlyApplicable.setSelected(false);
            ckOnlyApplicable.setEnabled(false);
        } else {
            ckOnlyApplicable.setSelected(true);
            ckOnlyApplicable.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    filterPresets(edSearchText.getText());
                }
            });
        }

        ckSearchInTags = new JCheckBox();
        ckSearchInTags.setText(tr("Search in tags"));
        ckSearchInTags.setSelected(true);
        ckSearchInTags.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                filterPresets(edSearchText.getText());
            }
        });
        pnChecks.add(ckSearchInTags);

        content.add(pnChecks, BorderLayout.SOUTH);

        content.setPreferredSize(new Dimension(400, 300));
        setContent(content);
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
     *
     * When groups are given,
     *
     *
     * @param text
     */
    private void filterPresets(String text) {
        //TODO Favorites
        text = text.toLowerCase();

        String[] groupWords;
        String[] nameWords;

        if (text.contains("/")) {
            groupWords = text.substring(0, text.lastIndexOf('/')).split("[\\s/]");
            nameWords = text.substring(text.indexOf('/') + 1).split("\\s");
        } else {
            groupWords = null;
            nameWords = text.split("\\s");
        }

        boolean onlyApplicable = ckOnlyApplicable.isSelected();
        boolean inTags = ckSearchInTags.isSelected();

        List<PresetClasification> result = new ArrayList<PresetClasification>();
        PRESET_LOOP:
            for (PresetClasification presetClasification: classifications) {
                TaggingPreset preset = presetClasification.preset;
                presetClasification.classification = 0;

                if (onlyApplicable && preset.types != null) {
                    boolean found = false;
                    for (PresetType type: preset.types) {
                        if (typesInSelection.contains(type)) {
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
                    result.add(presetClasification);
                }
            }

        Collections.sort(result);
        lsResultModel.setPresets(result);
        if (!buttons.isEmpty()) {
            buttons.get(0).setEnabled(!result.isEmpty());
        }
    }


    private void getTypesInSelection() {
        for (OsmPrimitive primitive: Main.main.getCurrentDataSet().getSelected()) {
            if (primitive instanceof Node) {
                typesInSelection.add(PresetType.NODE);
            } else if (primitive instanceof Way) {
                typesInSelection.add(PresetType.WAY);
                if (((Way)primitive).isClosed()) {
                    typesInSelection.add(PresetType.CLOSEDWAY);
                }
            } else if (primitive instanceof Relation) {
                typesInSelection.add(PresetType.RELATION);
            }
        }
    }

    @Override
    protected void buttonAction(int buttonIndex, ActionEvent evt) {
        super.buttonAction(buttonIndex, evt);
        if (buttonIndex == 0) {
            int selectPreset = lsResult.getSelectedIndex();
            if (selectPreset == -1) {
                selectPreset = 0;
            }
            lsResultModel.getPresets().get(selectPreset).preset.actionPerformed(null);
        }
    }

}
