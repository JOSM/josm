// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;

/**
 * Table column model for the {@link SaveLayersTable} in the {@link SaveLayersDialog}.
 */
class SaveLayersTableColumnModel extends DefaultTableColumnModel {
    /** small renderer class that handles the "should be uploaded/saved" texts. */
    private static class RecommendedActionsTableCell implements TableCellRenderer {
        private final JPanel pnlEmpty = new JPanel();
        private final JLabel needsUpload = new JLabel(tr("should be uploaded"));
        private final JLabel needsSave = new JLabel(tr("should be saved"));
        private static final GBC DEFAULT_CELL_STYLE = GBC.eol().fill(GBC.HORIZONTAL).insets(2, 0, 2, 0);

        /**
         * Constructs a new {@code RecommendedActionsTableCell}.
         */
        RecommendedActionsTableCell() {
            pnlEmpty.setPreferredSize(new Dimension(1, 19));
            needsUpload.setPreferredSize(new Dimension(needsUpload.getPreferredSize().width, 19));
            needsSave.setPreferredSize(new Dimension(needsSave.getPreferredSize().width, 19));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            JPanel panel = new JPanel(new GridBagLayout());
            SaveLayerInfo info = (SaveLayerInfo) value;
            StringBuilder sb = new StringBuilder(24);
            sb.append("<html>");
            if (info != null) {
                String htmlInfoName = Utils.escapeReservedCharactersHTML(info.getName());
                if (info.getLayer().requiresUploadToServer() && !info.getLayer().isUploadDiscouraged()) {
                    panel.add(needsUpload, DEFAULT_CELL_STYLE);
                    sb.append(tr("Layer ''{0}'' has modifications which should be uploaded to the server.", htmlInfoName));

                } else {
                    if (info.isUploadable()) {
                        panel.add(pnlEmpty, DEFAULT_CELL_STYLE);
                    }
                    if (info.getLayer().requiresUploadToServer()) {
                        sb.append(tr("Layer ''{0}'' has modifications which are discouraged to be uploaded.", htmlInfoName));
                    } else {
                        sb.append(tr("Layer ''{0}'' has no modifications to be uploaded.", htmlInfoName));
                    }
                }
                sb.append("<br/>");

                if (info.getLayer().requiresSaveToFile()) {
                    panel.add(needsSave, DEFAULT_CELL_STYLE);
                    sb.append(tr("Layer ''{0}'' has modifications which should be saved to its associated file ''{1}''.",
                            htmlInfoName, info.getFile().toString()));
                } else {
                    if (info.isSavable()) {
                        panel.add(pnlEmpty, DEFAULT_CELL_STYLE);
                    }
                    sb.append(tr("Layer ''{0}'' has no modifications to be saved.", htmlInfoName));
                }
            }
            sb.append("</html>");
            panel.setToolTipText(sb.toString());
            return panel;
        }
    }

    /**
     * Constructs a new {@code SaveLayersTableColumnModel}.
     */
    SaveLayersTableColumnModel() {
        build();
    }

    protected void build() {
        // column 0 - layer name, save path editor
        LayerNameAndFilePathTableCell lnfpRenderer = new LayerNameAndFilePathTableCell();
        LayerNameAndFilePathTableCell lnfpEditor = new LayerNameAndFilePathTableCell();
        TableColumn col = new TableColumn(0); // keep in sync with SaveLayersModel#columnFilename
        col.setHeaderValue(tr("Layer Name and File Path"));
        col.setResizable(true);
        col.setCellRenderer(lnfpRenderer);
        col.setCellEditor(lnfpEditor);
        col.setPreferredWidth(324);
        addColumn(col);

        // column 1 - actions required
        col = new TableColumn(1);
        col.setHeaderValue(tr("Recommended Actions"));
        col.setResizable(true);
        col.setCellRenderer(new RecommendedActionsTableCell());
        col.setPreferredWidth(150);
        addColumn(col);

        // column 2- actions to take
        ActionFlagsTableCell aftc = new ActionFlagsTableCell();
        col = new TableColumn(2); // keep in sync with SaveLayersModel#columnActions
        col.setHeaderValue(tr("Actions To Take"));
        col.setResizable(true);
        col.setCellRenderer(aftc);
        col.setCellEditor(aftc);
        col.setPreferredWidth(100);

        addColumn(col);
    }
}
