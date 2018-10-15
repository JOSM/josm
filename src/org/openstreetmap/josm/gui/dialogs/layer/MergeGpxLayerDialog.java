// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.layer;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;

import org.openstreetmap.josm.data.SystemOfMeasurement;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;

/**
 * The Dialog asking the user to prioritize GPX layers when cutting overlapping tracks.
 * Shows a checkbox asking whether to combine the tracks on cuts.
 * @since 14338
 */
public class MergeGpxLayerDialog extends ExtendedDialog {

    private final GpxLayersTableModel model;
    private final JTable t;
    private final JCheckBox c;
    private final Component parent;
    private final JButton btnUp;
    private final JButton btnDown;

    /**
     * Constructs a new {@code MergeGpxLayerDialog}
     * @param parent the parent
     * @param layers the GpxLayers to choose from
     */
    public MergeGpxLayerDialog(Component parent, List<GpxLayer> layers) {
        super(parent, tr("Merge GPX layers"), tr("Merge"), tr("Cancel"));
        setButtonIcons("dialogs/mergedown", "cancel");
        this.parent = parent;

        JPanel p = new JPanel(new GridBagLayout());
        p.add(new JLabel("<html>" +
                tr("Please select the order of the selected layers:<br>Tracks will be cut, when timestamps of higher layers are overlapping.") +
                "</html>"), GBC.std(0, 0).fill(GBC.HORIZONTAL).span(2));

        c = new JCheckBox(tr("Connect overlapping tracks on cuts"));
        c.setSelected(Config.getPref().getBoolean("mergelayer.gpx.connect", true));
        p.add(c, GBC.std(0, 1).fill(GBC.HORIZONTAL).span(2));

        model = new GpxLayersTableModel(layers);
        t = new JTable(model);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setRowSelectionInterval(0, 0);

        JScrollPane sp = new JScrollPane(t);
        p.add(sp, GBC.std(0, 2).fill().span(2));

        t.getSelectionModel().addListSelectionListener(new RowSelectionChangedListener());
        TableColumnModel cmodel = t.getColumnModel();
        cmodel.getColumn(0).setPreferredWidth((int) (sp.getPreferredSize().getWidth() - 150));
        cmodel.getColumn(1).setPreferredWidth(75);
        cmodel.getColumn(2).setPreferredWidth(75);

        btnUp = new JButton(tr("Move layer up"));
        btnUp.setIcon(ImageProvider.get("dialogs", "up", ImageSizes.SMALLICON));
        btnUp.setEnabled(false);

        btnDown = new JButton(tr("Move layer down"));
        btnDown.setIcon(ImageProvider.get("dialogs", "down", ImageSizes.SMALLICON));

        p.add(btnUp, GBC.std(0, 3).fill(GBC.HORIZONTAL));
        p.add(btnDown, GBC.std(1, 3).fill(GBC.HORIZONTAL));

        btnUp.addActionListener(new MoveLayersActionListener(true));
        btnDown.addActionListener(new MoveLayersActionListener(false));

        setContent(p);
    }

    @Override
    public MergeGpxLayerDialog showDialog() {
        super.showDialog();
        if (getValue() == 1) {
            Config.getPref().putBoolean("mergelayer.gpx.connect", c.isSelected());
        }
        return this;
    }

    /**
     * Whether the user chose to connect the tracks on cuts
     * @return the checkbox state
     */
    public boolean connectCuts() {
        return c.isSelected();
    }

    /**
     * The {@code List<GpxLayer>} as sorted by the user
     * @return the list
     */
    public List<GpxLayer> getSortedLayers() {
        return model.getSortedLayers();
    }

    private class MoveLayersActionListener implements ActionListener {

        private final boolean moveUp;

        MoveLayersActionListener(boolean up) {
            moveUp = up;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int row = t.getSelectedRow();
            int newRow = row + (moveUp ? -1 : 1);

            if ((row == 0 || newRow == 0)
                    && (!ConditionalOptionPaneUtil.showConfirmationDialog(
                            "gpx_target_change",
                            parent,
                            new JLabel("<html>" +
                                    tr("This will change the target layer to \"{0}\".<br>Would you like to continue?",
                                    model.getValueAt(1, 0).toString()) + "</html>"),
                            tr("Information"),
                            JOptionPane.OK_CANCEL_OPTION,
                            JOptionPane.INFORMATION_MESSAGE,
                            JOptionPane.OK_OPTION))) {
                return;
            }

            model.moveRow(row, newRow);
            t.getSelectionModel().setSelectionInterval(newRow, newRow);
            t.repaint();
        }
    }

    private class RowSelectionChangedListener implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent e) {
            btnUp.setEnabled(t.getSelectedRow() > 0);
            btnDown.setEnabled(t.getSelectedRow() < model.getRowCount() - 1);
        }
    }

    private static class GpxLayersTableModel extends AbstractTableModel {

        private final String[] cols = {tr("GPX layer"), tr("Length"), tr("Segments")};
        private final List<GpxLayer> layers;

        GpxLayersTableModel(List<GpxLayer> l) {
            layers = l;
        }

        @Override
        public String getColumnName(int column) {
            return cols[column];
        }

        @Override
        public int getColumnCount() {
            return cols.length;
        }

        @Override
        public int getRowCount() {
            return layers.size();

        }

        public void moveRow(int row, int newRow) {
            Collections.swap(layers, row, newRow);
        }

        public List<GpxLayer> getSortedLayers() {
            return layers;
        }

        @Override
        public Object getValueAt(int row, int col) {
            switch (col) {
            case 0:
                String n = layers.get(row).getName();
                if (row == 0) {
                    return tr("{0} (target layer)", n);
                } else {
                    return n;
                }
            case 1:
                return SystemOfMeasurement.getSystemOfMeasurement().getDistText(layers.get(row).data.length());
            case 2:
                return layers.get(row).data.getTrackSegsCount();
            }
            throw new IndexOutOfBoundsException(Integer.toString(col));
        }
    }
}
