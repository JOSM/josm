// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.openstreetmap.josm.data.SystemOfMeasurement;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.util.WindowGeometry;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OpenBrowser;

/**
 * allows the user to choose which of the downloaded tracks should be displayed.
 * they can be chosen from the gpx layer context menu.
 */
public class ChooseTrackVisibilityAction extends AbstractAction {
    private final transient GpxLayer layer;

    private DateFilterPanel dateFilter;
    private JTable table;

    /**
     * Constructs a new {@code ChooseTrackVisibilityAction}.
     * @param layer The associated GPX layer
     */
    public ChooseTrackVisibilityAction(final GpxLayer layer) {
        super(tr("Choose visible tracks"));
        new ImageProvider("dialogs/filter").getResource().attachImageIcon(this, true);
        this.layer = layer;
        putValue("help", ht("/Action/ChooseTrackVisibility"));
    }

    /**
     * Class to format a length according to SystemOfMesurement.
     */
    private static final class TrackLength {
        private final double value;

        /**
         * Constructs a new {@code TrackLength} object with a given length.
         * @param value length of the track
         */
        TrackLength(double value) {
            this.value = value;
        }

        /**
         * Provides string representation.
         * @return String representation depending of SystemOfMeasurement
         */
        @Override
        public String toString() {
            return SystemOfMeasurement.getSystemOfMeasurement().getDistText(value);
        }
    }

    /**
     * Comparator for TrackLength objects
     */
    private static final class LengthContentComparator implements Comparator<TrackLength>, Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * Compare 2 TrackLength objects relative to the real length
         */
        @Override
        public int compare(TrackLength l0, TrackLength l1) {
            return Double.compare(l0.value, l1.value);
        }
    }

    /**
     * Gathers all available data for the tracks and returns them as array of arrays
     * in the expected column order.
     * @return table data
     */
    private Object[][] buildTableContents() {
        Object[][] tracks = new Object[layer.data.tracks.size()][5];
        int i = 0;
        for (GpxTrack trk : layer.data.tracks) {
            Map<String, Object> attr = trk.getAttributes();
            String name = (String) Optional.ofNullable(attr.get(GpxConstants.GPX_NAME)).orElse("");
            String desc = (String) Optional.ofNullable(attr.get(GpxConstants.GPX_DESC)).orElse("");
            String time = GpxLayer.getTimespanForTrack(trk);
            TrackLength length = new TrackLength(trk.length());
            String url = (String) Optional.ofNullable(attr.get("url")).orElse("");
            tracks[i] = new Object[]{name, desc, time, length, url};
            i++;
        }
        return tracks;
    }

    /**
     * Builds an non-editable table whose 5th column will open a browser when double clicked.
     * The table will fill its parent.
     * @param content table data
     * @return non-editable table
     */
    private static JTable buildTable(Object[]... content) {
        final String[] headers = {tr("Name"), tr("Description"), tr("Timespan"), tr("Length"), tr("URL")};
        DefaultTableModel model = new DefaultTableModel(content, headers);
        final JTable t = new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int col) {
                Component c = super.prepareRenderer(renderer, row, col);
                if (c instanceof JComponent) {
                    JComponent jc = (JComponent) c;
                    jc.setToolTipText(getValueAt(row, col).toString());
                }
                return c;
            }

            @Override
            public boolean isCellEditable(int rowIndex, int colIndex) {
                return false;
            }
        };
        // define how to sort row
        TableRowSorter<DefaultTableModel> rowSorter = new TableRowSorter<>();
        t.setRowSorter(rowSorter);
        rowSorter.setModel(model);
        rowSorter.setComparator(3, new LengthContentComparator());
        // default column widths
        t.getColumnModel().getColumn(0).setPreferredWidth(220);
        t.getColumnModel().getColumn(1).setPreferredWidth(300);
        t.getColumnModel().getColumn(2).setPreferredWidth(200);
        t.getColumnModel().getColumn(3).setPreferredWidth(50);
        t.getColumnModel().getColumn(4).setPreferredWidth(100);
        // make the link clickable
        final MouseListener urlOpener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) {
                    return;
                }
                JTable t = (JTable) e.getSource();
                int col = t.convertColumnIndexToModel(t.columnAtPoint(e.getPoint()));
                if (col != 4) {
                    return;
                }
                int row = t.rowAtPoint(e.getPoint());
                String url = (String) t.getValueAt(row, col);
                if (url == null || url.isEmpty()) {
                    return;
                }
                OpenBrowser.displayUrl(url);
            }
        };
        t.addMouseListener(urlOpener);
        t.setFillsViewportHeight(true);
        return t;
    }

    private boolean noUpdates;

    /** selects all rows (=tracks) in the table that are currently visible on the layer*/
    private void selectVisibleTracksInTable() {
        // don't select any tracks if the layer is not visible
        if (!layer.isVisible()) {
            return;
        }
        ListSelectionModel s = table.getSelectionModel();
        s.setValueIsAdjusting(true);
        s.clearSelection();
        for (int i = 0; i < layer.trackVisibility.length; i++) {
            if (layer.trackVisibility[i]) {
                s.addSelectionInterval(i, i);
            }
        }
        s.setValueIsAdjusting(false);
    }

    /** listens to selection changes in the table and redraws the map */
    private void listenToSelectionChanges() {
        table.getSelectionModel().addListSelectionListener(e -> {
            if (noUpdates || !(e.getSource() instanceof ListSelectionModel)) {
                return;
            }
            updateVisibilityFromTable();
        });
    }

    private void updateVisibilityFromTable() {
        ListSelectionModel s = table.getSelectionModel();
        for (int i = 0; i < layer.trackVisibility.length; i++) {
            layer.trackVisibility[table.convertRowIndexToModel(i)] = s.isSelectedIndex(i);
        }
        layer.invalidate();
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        final JPanel msg = new JPanel(new GridBagLayout());

        dateFilter = new DateFilterPanel(layer, "gpx.traces", false);
        dateFilter.setFilterAppliedListener(e -> {
            noUpdates = true;
            selectVisibleTracksInTable();
            noUpdates = false;
            layer.invalidate();
        });
        dateFilter.loadFromPrefs();

        final JToggleButton b = new JToggleButton(new AbstractAction(tr("Select by date")) {
            @Override public void actionPerformed(ActionEvent e) {
                if (((JToggleButton) e.getSource()).isSelected()) {
                    dateFilter.setEnabled(true);
                    dateFilter.applyFilter();
                } else {
                    dateFilter.setEnabled(false);
                }
            }
        });
        dateFilter.setEnabled(false);
        msg.add(b, GBC.std().insets(0, 0, 5, 0));
        msg.add(dateFilter, GBC.eol().insets(0, 0, 10, 0).fill(GBC.HORIZONTAL));

        msg.add(new JLabel(tr("<html>Select all tracks that you want to be displayed. " +
                "You can drag select a range of tracks or use CTRL+Click to select specific ones. " +
                "The map is updated live in the background. Open the URLs by double clicking them.</html>")),
                GBC.eop().fill(GBC.HORIZONTAL));
        // build table
        final boolean[] trackVisibilityBackup = layer.trackVisibility.clone();
        table = buildTable(buildTableContents());
        selectVisibleTracksInTable();
        listenToSelectionChanges();
        // make the table scrollable
        JScrollPane scrollPane = new JScrollPane(table);
        msg.add(scrollPane, GBC.eol().fill(GBC.BOTH));

        int v = 1;
        if (!GraphicsEnvironment.isHeadless()) {
            // build dialog
            ExtendedDialog ed = new ExtendedDialog(MainApplication.getMainFrame(), tr("Set track visibility for {0}", layer.getName()),
                    tr("Show all"), tr("Show selected only"), tr("Cancel"));
            ed.setButtonIcons("eye", "dialogs/filter", "cancel");
            ed.setContent(msg, false);
            ed.setDefaultButton(2);
            ed.setCancelButton(3);
            ed.configureContextsensitiveHelp("/Action/ChooseTrackVisibility", true);
            ed.setRememberWindowGeometry(getClass().getName() + ".geometry",
                    WindowGeometry.centerInWindow(MainApplication.getMainFrame(), new Dimension(1000, 500)));
            ed.showDialog();
            dateFilter.saveInPrefs();
            v = ed.getValue();
            // cancel for unknown buttons and copy back original settings
            if (v != 1 && v != 2) {
                layer.trackVisibility = Arrays.copyOf(trackVisibilityBackup, layer.trackVisibility.length);
                MainApplication.getMap().repaint();
                return;
            }
        }
        // set visibility (1 = show all, 2 = filter). If no tracks are selected
        // set all of them visible and...
        ListSelectionModel s = table.getSelectionModel();
        final boolean all = v == 1 || s.isSelectionEmpty();
        for (int i = 0; i < layer.trackVisibility.length; i++) {
            layer.trackVisibility[table.convertRowIndexToModel(i)] = all || s.isSelectedIndex(i);
        }
        // layer has been changed
        layer.invalidate();
        // ...sync with layer visibility instead to avoid having two ways to hide everything
        layer.setVisible(v == 1 || !s.isSelectionEmpty());
    }
}
