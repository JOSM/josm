// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.OpenBrowser;
import org.openstreetmap.josm.tools.WindowGeometry;

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
        super(tr("Choose visible tracks"), ImageProvider.get("dialogs/filter"));
        this.layer = layer;
        putValue("help", ht("/Action/ChooseTrackVisibility"));
    }

    /**
     * Class to format a length according to SystemOfMesurement.
     */
    private static final class TrackLength {
        private double value;

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
            return NavigatableComponent.getSystemOfMeasurement().getDistText(value);
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
            if(l0.value < l1.value)
                return -1;
            else if(l0.value > l1.value)
                return 1;
            return 0;
        }
    }

    /**
     * gathers all available data for the tracks and returns them as array of arrays
     * in the expected column order  */
    private Object[][] buildTableContents() {
        Object[][] tracks = new Object[layer.data.tracks.size()][5];
        int i = 0;
        for (GpxTrack trk : layer.data.tracks) {
            Map<String, Object> attr = trk.getAttributes();
            String name = (String) (attr.containsKey(GpxConstants.GPX_NAME) ? attr.get(GpxConstants.GPX_NAME) : "");
            String desc = (String) (attr.containsKey(GpxConstants.GPX_DESC) ? attr.get(GpxConstants.GPX_DESC) : "");
            String time = GpxLayer.getTimespanForTrack(trk);
            TrackLength length = new TrackLength(trk.length());
            String url = (String) (attr.containsKey("url") ? attr.get("url") : "");
            tracks[i] = new Object[]{name, desc, time, length, url};
            i++;
        }
        return tracks;
    }

    /**
     * Builds an non-editable table whose 5th column will open a browser when double clicked.
     * The table will fill its parent. */
    private JTable buildTable(Object[][] content) {
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

    private boolean noUpdates=false;

    /** selects all rows (=tracks) in the table that are currently visible on the layer*/
    private void selectVisibleTracksInTable() {
        // don't select any tracks if the layer is not visible
        if (!layer.isVisible()) {
            return;
        }
        ListSelectionModel s = table.getSelectionModel();
        s.clearSelection();
        for (int i = 0; i < layer.trackVisibility.length; i++) {
            if (layer.trackVisibility[i]) {
                s.addSelectionInterval(i, i);
            }
        }
    }

    /** listens to selection changes in the table and redraws the map */
    private void listenToSelectionChanges() {
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (noUpdates || !(e.getSource() instanceof ListSelectionModel)) {
                    return;
                }
                updateVisibilityFromTable();
            }
        });
    }

    private void updateVisibilityFromTable() {
        ListSelectionModel s = table.getSelectionModel();
        for (int i = 0; i < layer.trackVisibility.length; i++) {
            layer.trackVisibility[table.convertRowIndexToModel(i)] = s.isSelectedIndex(i);
        }
        Main.map.mapView.preferenceChanged(null);
        Main.map.repaint(100);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
        final JPanel msg = new JPanel(new GridBagLayout());

        dateFilter = new DateFilterPanel(layer, "gpx.traces", false);
        dateFilter.setFilterAppliedListener(new ActionListener(){
            @Override public void actionPerformed(ActionEvent e) {
                noUpdates = true;
                selectVisibleTracksInTable();
                noUpdates = false;
                Main.map.mapView.preferenceChanged(null);
                Main.map.repaint(100);
            }
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
        msg.add(b, GBC.std().insets(0,0,5,0));
        msg.add(dateFilter, GBC.eol().insets(0,0,10,0).fill(GBC.HORIZONTAL));

        msg.add(new JLabel(tr("<html>Select all tracks that you want to be displayed. You can drag select a " + "range of tracks or use CTRL+Click to select specific ones. The map is updated live in the " + "background. Open the URLs by double clicking them.</html>")), GBC.eop().fill(GBC.HORIZONTAL));
        // build table
        final boolean[] trackVisibilityBackup = layer.trackVisibility.clone();
        table = buildTable(buildTableContents());
        selectVisibleTracksInTable();
        listenToSelectionChanges();
        // make the table scrollable
        JScrollPane scrollPane = new JScrollPane(table);
        msg.add(scrollPane, GBC.eol().fill(GBC.BOTH));

        // build dialog
        ExtendedDialog ed = new ExtendedDialog(Main.parent, tr("Set track visibility for {0}", layer.getName()),
                new String[]{tr("Show all"), tr("Show selected only"), tr("Cancel")});
        ed.setButtonIcons(new String[]{"eye", "dialogs/filter", "cancel"});
        ed.setContent(msg, false);
        ed.setDefaultButton(2);
        ed.setCancelButton(3);
        ed.configureContextsensitiveHelp("/Action/ChooseTrackVisibility", true);
        ed.setRememberWindowGeometry(getClass().getName() + ".geometry", WindowGeometry.centerInWindow(Main.parent, new Dimension(1000, 500)));
        ed.showDialog();
        dateFilter.saveInPrefs();
        int v = ed.getValue();
        // cancel for unknown buttons and copy back original settings
        if (v != 1 && v != 2) {
            layer.trackVisibility = Arrays.copyOf(trackVisibilityBackup, layer.trackVisibility.length);
            Main.map.repaint();
            return;
        }
        // set visibility (1 = show all, 2 = filter). If no tracks are selected
        // set all of them visible and...
        ListSelectionModel s = table.getSelectionModel();
        final boolean all = v == 1 || s.isSelectionEmpty();
        for (int i = 0; i < layer.trackVisibility.length; i++) {
            layer.trackVisibility[table.convertRowIndexToModel(i)] = all || s.isSelectedIndex(i);
        }
        // ...sync with layer visibility instead to avoid having two ways to hide everything
        layer.setVisible(v == 1 || !s.isSelectionEmpty());

        Main.map.mapView.preferenceChanged(null);
        Main.map.repaint();
    }

}
