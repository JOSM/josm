// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;

/**
 * A projection choice, that offers a list of projections in a combo-box.
 */
abstract public class ListProjectionChoice extends AbstractProjectionChoice {

    protected int index;        // 0-based index
    protected int defaultIndex;
    protected Object[] entries;
    protected String label;

    /**
     * Constructs a new {@code ListProjectionChoice}.
     *
     * @param name the display name
     * @param id the unique id for this ProjectionChoice
     * @param entries the list of display entries for the combo-box
     * @param label a label shown left to the combo-box
     * @param defaultIndex the default index for the combo-box
     */
    public ListProjectionChoice(String name, String id, Object[] entries, String label, int defaultIndex) {
        super(name, id);
        this.entries = Utils.copyArray(entries);
        this.label = label;
        this.defaultIndex = defaultIndex;
    }

    /**
     * Constructs a new {@code ListProjectionChoice}.
     * @param name the display name
     * @param id the unique id for this ProjectionChoice
     * @param entries the list of display entries for the combo-box
     * @param label a label shown left to the combo-box
     */
    public ListProjectionChoice(String name, String id, Object[] entries, String label) {
        this(name, id, entries, label, 0);
    }

    /**
     * Convert 0-based index to preference value.
     */
    abstract protected String indexToZone(int index);

    /**
     * Convert preference value to 0-based index.
     */
    abstract protected int zoneToIndex(String zone);

    @Override
    public void setPreferences(Collection<String> args) {
        String zone = null;
        if (args != null && args.size() >= 1) {
            zone = args.iterator().next();
        }
        int index;
        if (zone == null) {
            index = defaultIndex;
        } else {
            index = zoneToIndex(zone);
            if (index < 0 || index >= entries.length) {
                index = defaultIndex;
            }
        }
        this.index = index;
    }

    protected class CBPanel extends JPanel {
        public JosmComboBox prefcb;

        public CBPanel(Object[] entries, int initialIndex, String label, final ActionListener listener) {
            prefcb = new JosmComboBox(entries);

            prefcb.setSelectedIndex(initialIndex);
            this.setLayout(new GridBagLayout());
            this.add(new JLabel(label), GBC.std().insets(5,5,0,5));
            this.add(GBC.glue(1, 0), GBC.std().fill(GBC.HORIZONTAL));
            this.add(prefcb, GBC.eop().fill(GBC.HORIZONTAL));
            this.add(GBC.glue(1, 1), GBC.eol().fill(GBC.BOTH));

            if (listener != null) {
                prefcb.addActionListener(listener);
            }
        }
    }

    @Override
    public JPanel getPreferencePanel(ActionListener listener) {
        return new CBPanel(entries, index, label, listener);
    }

    @Override
    public Collection<String> getPreferences(JPanel panel) {
        if (!(panel instanceof CBPanel)) {
            throw new IllegalArgumentException();
        }
        CBPanel p = (CBPanel) panel;
        int index = p.prefcb.getSelectedIndex();
        return Collections.singleton(indexToZone(index));
    }

}
