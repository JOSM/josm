// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.preferences.projection.CodeProjectionChoice.CodeComparator;
import org.openstreetmap.josm.gui.util.TableHelper;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.GBC;

/**
 * Panel allowing to select a projection by code.
 * @since 13544 (extracted from {@link CodeProjectionChoice})
 */
public class CodeSelectionPanel extends JPanel implements ListSelectionListener, DocumentListener {

    private final JosmTextField filter = new JosmTextField(30);
    private final ProjectionCodeModel model = new ProjectionCodeModel();
    private JTable table;
    private final List<String> data;
    private final List<String> filteredData;
    private static final String DEFAULT_CODE = "EPSG:3857";
    private String lastCode = DEFAULT_CODE;
    private final transient ActionListener listener;

    /**
     * Constructs a new {@code CodeSelectionPanel}.
     * @param initialCode projection code initially selected
     * @param listener listener notified of selection change events
     */
    public CodeSelectionPanel(String initialCode, ActionListener listener) {
        this.listener = listener;
        data = new ArrayList<>(Projections.getAllProjectionCodes());
        data.sort(new CodeComparator());
        filteredData = new ArrayList<>(data);
        build();
        setCode(initialCode != null ? initialCode : DEFAULT_CODE);
        table.getSelectionModel().addListSelectionListener(this);
    }

    /**
     * List model for the filtered view on the list of all codes.
     */
    private class ProjectionCodeModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return filteredData.size();
        }

        @Override
        public String getValueAt(int index, int column) {
            if (index >= 0 && index < filteredData.size()) {
                String code = filteredData.get(index);
                switch (column) {
                    case 0: return code;
                    case 1: return Projections.getProjectionByCode(code).toString();
                    default: break;
                }
            }
            return null;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0: return tr("Projection code");
                case 1: return tr("Projection name");
                default: return super.getColumnName(column);
            }
        }
    }

    private void build() {
        filter.setColumns(40);
        filter.getDocument().addDocumentListener(this);

        table = new JTable(model);
        TableHelper.setFont(table, getClass());
        table.setAutoCreateRowSorter(true);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(200, 214));

        this.setLayout(new GridBagLayout());
        this.add(filter, GBC.eol().weight(1.0, 0.0));
        this.add(scroll, GBC.eol().fill(GBC.HORIZONTAL));
    }

    /**
     * Returns selected projection code.
     * @return selected projection code
     */
    public String getCode() {
        int idx = table.getSelectedRow();
        if (idx == -1)
            return lastCode;
        return filteredData.get(table.convertRowIndexToModel(table.getSelectedRow()));
    }

    /**
     * Sets selected projection code.
     * @param code projection code to select
     */
    public final void setCode(String code) {
        int idx = filteredData.indexOf(code);
        if (idx != -1) {
            selectRow(idx);
        }
    }

    private void selectRow(int idx) {
        table.setRowSelectionInterval(idx, idx);
        ensureRowIsVisible(idx);
    }

    private void ensureRowIsVisible(int idx) {
        table.scrollRectToVisible(table.getCellRect(idx, 0, true));
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        listener.actionPerformed(null);
        lastCode = getCode();
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        updateFilter();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        updateFilter();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        updateFilter();
    }

    private void updateFilter() {
        filteredData.clear();
        String filterTxt = filter.getText().trim().toLowerCase(Locale.ENGLISH);
        for (String code : data) {
            if (code.toLowerCase(Locale.ENGLISH).contains(filterTxt)
             || Projections.getProjectionByCode(code).toString().toLowerCase(Locale.ENGLISH).contains(filterTxt)) {
                filteredData.add(code);
            }
        }
        model.fireTableDataChanged();
        int idx = filteredData.indexOf(lastCode);
        if (idx == -1) {
            table.clearSelection();
            if (table.getModel().getRowCount() > 0) {
                ensureRowIsVisible(0);
            }
        } else {
            selectRow(idx);
        }
    }
}
