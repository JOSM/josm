// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.imagery;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import org.openstreetmap.josm.data.imagery.WMTSTileSource.Layer;
import org.openstreetmap.josm.gui.widgets.JosmTextArea;
import org.openstreetmap.josm.tools.GBC;

/**
 * Class for displaying WMTS layer selection panel.
 * @since 13748
 */
public class WMTSLayerSelection extends JPanel {
    private static final class AbstractTableModelExtension extends AbstractTableModel {
        private final List<Entry<String, List<Layer>>> layers;

        private AbstractTableModelExtension(List<Entry<String, List<Layer>>> layers) {
            this.layers = layers;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            switch (columnIndex) {
            case 0:
                return layers.get(rowIndex).getValue()
                        .stream()
                        .map(Layer::getUserTitle)
                        .collect(Collectors.joining(", ")); //this should be only one
            case 1:
                return layers.get(rowIndex).getValue()
                        .stream()
                        .map(x -> x.getTileMatrixSet().getCrs())
                        .collect(Collectors.joining(", "));
            case 2:
                return layers.get(rowIndex).getValue()
                        .stream()
                        .map(x -> x.getTileMatrixSet().getIdentifier())
                        .collect(Collectors.joining(", ")); //this should be only one
            default:
                throw new IllegalArgumentException();
            }
        }

        @Override
        public int getRowCount() {
            return layers.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0: return tr("Layer name");
            case 1: return tr("Projection");
            case 2: return tr("Matrix set identifier");
            default:
                throw new IllegalArgumentException();
            }
        }
    }

    private final List<Entry<String, List<Layer>>> layers;
    private final JTable list;

    /**
     * Constructs a new {@code WMTSLayerSelection}.
     * @param layers list of grouped layers (by tileMatrixSet and name)
     */
    public WMTSLayerSelection(List<Entry<String, List<Layer>>> layers) {
        super(new GridBagLayout());
        this.layers = layers;
        list = new JTable(
                new AbstractTableModelExtension(layers));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setAutoCreateRowSorter(true);
        list.setRowSelectionAllowed(true);
        list.setColumnSelectionAllowed(false);
        updateColumnPreferredWidth(list.getColumnModel().getColumn(0));
        updateColumnPreferredWidth(list.getColumnModel().getColumn(1));
        updateColumnPreferredWidth(list.getColumnModel().getColumn(2));

        add(new JLabel(tr("Filter layers:")), GBC.eol().fill(GBC.HORIZONTAL));
        final JosmTextArea filter = new JosmTextArea();
        filter.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            @SuppressWarnings({ "unchecked", "rawtypes" })
            private void update() {
                ((TableRowSorter) list.getRowSorter()).setRowFilter(RowFilter.regexFilter("(?i)" + filter.getText()));
            }

        });
        add(filter, GBC.eop().fill(GBC.HORIZONTAL));
        add(new JScrollPane(this.list), GBC.eol().fill());
    }

    /**
     * Returns selected layer.
     * @return selected layer
     */
    public Layer getSelectedLayer() {
        int index = list.getSelectedRow();
        if (index < 0) {
            return null; //nothing selected
        }
        return layers.get(list.convertRowIndexToModel(index)).getValue().get(0);
    }

    private void updateColumnPreferredWidth(TableColumn column) {
        TableCellRenderer renderer = column.getHeaderRenderer();
        if (renderer == null) {
            renderer = list.getTableHeader().getDefaultRenderer();
        }
        Component comp = renderer.getTableCellRendererComponent(list, column.getHeaderValue(), false, false, 0, 0);
        int ret = comp.getPreferredSize().width;

        for (int row = 0; row < list.getRowCount(); row++) {
            renderer = list.getCellRenderer(row, column.getModelIndex());
            comp = list.prepareRenderer(renderer, row, column.getModelIndex());
            ret = Math.max(comp.getPreferredSize().width, ret);
        }
        column.setPreferredWidth(ret + 10);
    }

    /**
     * Returns the list of layers.
     * @return the list of layers
     */
    public JTable getTable() {
        return list;
    }
}
