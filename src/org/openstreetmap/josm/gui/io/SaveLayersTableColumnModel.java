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

class SaveLayersTableColumnModel extends DefaultTableColumnModel {
    /** small renderer class that handles the "should be uploaded/saved" texts. */
    private static class RecommendedActionsTableCell implements TableCellRenderer {
        private final JPanel pnlEmpty = new JPanel();
        private final JLabel needsUpload = new JLabel(tr("should be uploaded"));
        private final JLabel needsSave = new JLabel(tr("should be saved"));
        private final static GBC defaultCellStyle = GBC.eol().fill(GBC.HORIZONTAL).insets(2, 0, 2, 0);

        public RecommendedActionsTableCell() {
            pnlEmpty.setPreferredSize(new Dimension(1, 19));
            needsUpload.setPreferredSize(new Dimension(needsUpload.getPreferredSize().width, 19));
            needsSave.setPreferredSize(new Dimension(needsSave.getPreferredSize().width, 19));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            JPanel panel = new JPanel(new GridBagLayout());
            SaveLayerInfo info = (SaveLayerInfo)value;
            StringBuilder sb = new StringBuilder();
            sb.append("<html>");
            if (info.getLayer().requiresUploadToServer() && !info.getLayer().isUploadDiscouraged()) {
                panel.add(needsUpload, defaultCellStyle);
                sb.append(tr("Layer ''{0}'' has modifications which should be uploaded to the server.", info.getName()));

            } else {
                panel.add(pnlEmpty, defaultCellStyle);
                if (info.getLayer().requiresUploadToServer()) {
                    sb.append(tr("Layer ''{0}'' has modifications which are discouraged to be uploaded.", info.getName()));
                } else {
                    sb.append(tr("Layer ''{0}'' has no modifications to be uploaded.", info.getName()));
                }
            }
            sb.append("<br/>");

            if (info.getLayer().requiresSaveToFile()) {
                panel.add(needsSave, defaultCellStyle);
                sb.append(tr("Layer ''{0}'' has modifications which should be saved to its associated file ''{1}''.", info.getName(), info.getFile().toString()));
            } else {
                panel.add(pnlEmpty, defaultCellStyle);
                sb.append(tr("Layer ''{0}'' has no modifications to be saved.", info.getName()));
            }
            sb.append("</html>");
            panel.setToolTipText(sb.toString());
            return panel;
        }
    }

    protected void build() {
        TableColumn col = null;

        // column 0 - layer name, save path editor
        LayerNameAndFilePathTableCell lnafptc = new LayerNameAndFilePathTableCell();
        col = new TableColumn(0); // keep in sync with SaveLayersModel#columnFilename
        col.setHeaderValue(tr("Layer Name and File Path"));
        col.setResizable(true);
        col.setCellRenderer(lnafptc);
        col.setCellEditor(lnafptc);
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

    public SaveLayersTableColumnModel() {
        build();
    }
}
