// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.spi.preferences.Config;

/**
 * This is a UI widget for resolving tag conflicts, i.e. differences of the tag values
 * of multiple {@link org.openstreetmap.josm.data.osm.OsmPrimitive}s.
 * @since 2008
 */
public class TagConflictResolver extends JPanel {

    /** the model for the tag conflict resolver */
    private final TagConflictResolverModel model;
    /** selects whether only tags with conflicts are displayed */
    private final JCheckBox cbShowTagsWithConflictsOnly = new JCheckBox(tr("Show tags with conflicts only"));
    private final JCheckBox cbShowTagsWithMultiValuesOnly = new JCheckBox(tr("Show tags with multiple values only"));

    /**
     * Constructs a new {@code TagConflictResolver}.
     * @param model tag conflict resolver model
     * @since 11772
     */
    public TagConflictResolver(TagConflictResolverModel model) {
        this.model = model;
        build();
    }

    protected JPanel buildInfoPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.BOTH;
        gc.weighty = 1.0;
        gc.weightx = 1.0;
        gc.anchor = GridBagConstraints.LINE_START;
        gc.gridwidth = 2;
        pnl.add(new JLabel(tr("<html>Please select the values to keep for the following tags.</html>")), gc);

        gc.gridwidth = 1;
        gc.gridy = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weighty = 0.0;
        pnl.add(cbShowTagsWithConflictsOnly, gc);
        pnl.add(cbShowTagsWithMultiValuesOnly, gc);
        cbShowTagsWithConflictsOnly.addChangeListener(e -> {
                model.setShowTagsWithConflictsOnly(cbShowTagsWithConflictsOnly.isSelected());
                cbShowTagsWithMultiValuesOnly.setEnabled(cbShowTagsWithConflictsOnly.isSelected());
        });
        cbShowTagsWithConflictsOnly.setSelected(Config.getPref().getBoolean(getClass().getName() + ".showTagsWithConflictsOnly", false)
        );
        cbShowTagsWithMultiValuesOnly.addChangeListener(
                e -> model.setShowTagsWithMultiValuesOnly(cbShowTagsWithMultiValuesOnly.isSelected())
        );
        cbShowTagsWithMultiValuesOnly.setSelected(Config.getPref().getBoolean(getClass().getName() + ".showTagsWithMultiValuesOnly", false)
        );
        cbShowTagsWithMultiValuesOnly.setEnabled(cbShowTagsWithConflictsOnly.isSelected());
        return pnl;
    }

    /**
     * Remembers the current settings in the global preferences
     *
     */
    public void rememberPreferences() {
        Config.getPref().putBoolean(getClass().getName() + ".showTagsWithConflictsOnly", cbShowTagsWithConflictsOnly.isSelected());
        Config.getPref().putBoolean(getClass().getName() + ".showTagsWithMultiValuesOnly", cbShowTagsWithMultiValuesOnly.isSelected());
    }

    protected final void build() {
        setLayout(new BorderLayout());
        add(buildInfoPanel(), BorderLayout.NORTH);
        add(new JScrollPane(new TagConflictResolverTable(model)), BorderLayout.CENTER);
    }

    /**
     * Replies the model used by this dialog
     *
     * @return the model
     */
    public TagConflictResolverModel getModel() {
        return model;
    }
}
