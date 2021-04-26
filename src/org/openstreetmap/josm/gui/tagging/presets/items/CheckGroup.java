// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import java.awt.GridLayout;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItem;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemGuiSupport;
import org.openstreetmap.josm.tools.GBC;

/**
 * A group of {@link Check}s.
 * @since 6114
 */
public class CheckGroup extends TaggingPresetItem {

    /**
     * Number of columns (positive integer)
     */
    public short columns = 1; // NOSONAR

    /**
     * List of checkboxes
     */
    public final List<Check> checks = new LinkedList<>();

    @Override
    public boolean addToPanel(JPanel p, TaggingPresetItemGuiSupport support) {
        int rows = (int) Math.ceil(checks.size() / ((double) columns));
        JPanel panel = new JPanel(new GridLayout(rows, columns));

        int i = 0;
        for (Check check : checks) {
            check.addToPanel(panel, support);
            i++;
        }
        for (; i < rows * columns; i++) {
            // fill remaining cells, see #20792
            panel.add(new JLabel());
        }

        p.add(panel, GBC.eol());
        return false;
    }

    @Override
    public void addCommands(List<Tag> changedTags) {
        for (Check check : checks) {
            check.addCommands(changedTags);
        }
    }

    @Override
    public Boolean matches(Map<String, String> tags) {
        for (Check check : checks) {
            if (Boolean.TRUE.equals(check.matches(tags))) {
                return Boolean.TRUE;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "CheckGroup [columns=" + columns + ']';
    }
}
