// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.openstreetmap.josm.data.projection.proj.TransverseMercator.Hemisphere;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;

/**
 * ProjectionChoice for UTM.
 * <p>
 * @see <a href="https://en.wikipedia.org/wiki/Universal_Transverse_Mercator_coordinate_system">UTM</a>
 */
public class UTMProjectionChoice extends ListProjectionChoice {

    private static final Hemisphere DEFAULT_HEMISPHERE = Hemisphere.North;

    private Hemisphere hemisphere;

    private static final List<String> cbEntries = new ArrayList<>();
    static {
        for (int i = 1; i <= 60; i++) {
            cbEntries.add(Integer.toString(i));
        }
    }

    /**
     * Constructs a new {@code UTMProjectionChoice}.
     */
    public UTMProjectionChoice() {
        super(tr("UTM"), /* NO-ICON */ "core:utm", cbEntries.toArray(new String[cbEntries.size()]), tr("UTM Zone"));
    }

    private class UTMPanel extends CBPanel {

        public JRadioButton north, south;

        UTMPanel(String[] entries, int initialIndex, String label, ActionListener listener) {
            super(entries, initialIndex, label, listener);

            north = new JRadioButton();
            north.setSelected(hemisphere == Hemisphere.North);
            south = new JRadioButton();
            south.setSelected(hemisphere == Hemisphere.South);

            ButtonGroup group = new ButtonGroup();
            group.add(north);
            group.add(south);

            JPanel bPanel = new JPanel(new GridBagLayout());

            bPanel.add(new JLabel(tr("North")), GBC.std().insets(5, 5, 0, 5));
            bPanel.add(north, GBC.std().fill(GBC.HORIZONTAL));
            bPanel.add(GBC.glue(1, 0), GBC.std().fill(GBC.HORIZONTAL));
            bPanel.add(new JLabel(tr("South")), GBC.std().insets(5, 5, 0, 5));
            bPanel.add(south, GBC.std().fill(GBC.HORIZONTAL));
            bPanel.add(GBC.glue(1, 1), GBC.eol().fill(GBC.BOTH));

            this.add(new JLabel(tr("Hemisphere")), GBC.std().insets(5, 5, 0, 5));
            this.add(GBC.glue(1, 0), GBC.std().fill(GBC.HORIZONTAL));
            this.add(bPanel, GBC.eop().fill(GBC.HORIZONTAL));
            this.add(GBC.glue(1, 1), GBC.eol().fill(GBC.BOTH));

            if (listener != null) {
                north.addActionListener(listener);
                south.addActionListener(listener);
            }
        }
    }

    @Override
    public JPanel getPreferencePanel(ActionListener listener) {
        return new UTMPanel(entries, index, label, listener);
    }

    @Override
    public String getCurrentCode() {
        int zone = index + 1;
        int code = 32600 + zone + (hemisphere == Hemisphere.South ? 100 : 0);
        return "EPSG:" + Integer.toString(code);
    }

    @Override
    public String getProjectionName() {
        return tr("UTM");
    }

    @Override
    public Collection<String> getPreferences(JPanel panel) {
        if (!(panel instanceof UTMPanel)) {
            throw new IllegalArgumentException("Unsupported panel: "+panel);
        }
        UTMPanel p = (UTMPanel) panel;
        int idx = p.prefcb.getSelectedIndex();
        Hemisphere hem = p.south.isSelected() ? Hemisphere.South : Hemisphere.North;
        return Arrays.asList(indexToZone(idx), hem.toString());
    }

    @Override
    public String[] allCodes() {
        List<String> projections = new ArrayList<>(60*4);
        for (int zone = 1; zone <= 60; zone++) {
            for (Hemisphere hem : Hemisphere.values()) {
                projections.add("EPSG:" + (32600 + zone + (hem == Hemisphere.South ? 100 : 0)));
            }
        }
        return projections.toArray(new String[projections.size()]);
    }

    @Override
    public Collection<String> getPreferencesFromCode(String code) {

        if (code.startsWith("EPSG:326") || code.startsWith("EPSG:327")) {
            try {
                Hemisphere hem = code.charAt(7) == '6' ? Hemisphere.North : Hemisphere.South;
                String zonestring = code.substring(8);
                int zoneval = Integer.parseInt(zonestring);
                if (zoneval > 0 && zoneval <= 60)
                    return Arrays.asList(zonestring, hem.toString());
            } catch (NumberFormatException e) {
                Logging.warn(e);
            }
        }
        return null;
    }

    @Override
    public void setPreferences(Collection<String> args) {
        super.setPreferences(args);
        Hemisphere hem = DEFAULT_HEMISPHERE;

        if (args != null) {
            String[] array = args.toArray(new String[args.size()]);

            if (array.length > 1) {
                hem = Hemisphere.valueOf(array[1]);
            }
        }
        this.hemisphere = hem;
    }

    @Override
    protected String indexToZone(int idx) {
        return Integer.toString(idx + 1);
    }

    @Override
    protected int zoneToIndex(String zone) {
        try {
            return Integer.parseInt(zone) - 1;
        } catch (NumberFormatException e) {
            Logging.warn(e);
        }
        return defaultIndex;
    }
}
