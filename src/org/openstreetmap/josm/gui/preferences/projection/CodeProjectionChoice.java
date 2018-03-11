// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.GBC;

/**
 * Projection choice that lists all known projects by code.
 * @since 5634
 */
public class CodeProjectionChoice extends AbstractProjectionChoice implements SubPrefsOptions {

    private String code;

    /**
     * Constructs a new {@code CodeProjectionChoice}.
     */
    public CodeProjectionChoice() {
        super(tr("By Code (EPSG)"), /* NO-ICON */ "core:code");
    }

    private static class CodeSelectionPanel extends JPanel implements ListSelectionListener, DocumentListener {

        private final JosmTextField filter = new JosmTextField(30);
        private final ProjectionCodeModel model = new ProjectionCodeModel();
        private JTable table;
        private final List<String> data;
        private final List<String> filteredData;
        private static final String DEFAULT_CODE = "EPSG:3857";
        private String lastCode = DEFAULT_CODE;
        private final transient ActionListener listener;

        CodeSelectionPanel(String initialCode, ActionListener listener) {
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
            table.setAutoCreateRowSorter(true);
            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(200, 214));

            this.setLayout(new GridBagLayout());
            this.add(filter, GBC.eol().weight(1.0, 0.0));
            this.add(scroll, GBC.eol().fill(GBC.HORIZONTAL));
        }

        public String getCode() {
            int idx = table.getSelectedRow();
            if (idx == -1)
                return lastCode;
            return filteredData.get(table.convertRowIndexToModel(table.getSelectedRow()));
        }

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

    /**
     * Comparator that compares the number part of the code numerically.
     */
    public static class CodeComparator implements Comparator<String>, Serializable {
        private static final long serialVersionUID = 1L;
        private final Pattern codePattern = Pattern.compile("([a-zA-Z]+):(\\d+)");

        @Override
        public int compare(String c1, String c2) {
            Matcher matcher1 = codePattern.matcher(c1);
            Matcher matcher2 = codePattern.matcher(c2);
            if (matcher1.matches()) {
                if (matcher2.matches()) {
                    int cmp1 = matcher1.group(1).compareTo(matcher2.group(1));
                    if (cmp1 != 0)
                        return cmp1;
                    int num1 = Integer.parseInt(matcher1.group(2));
                    int num2 = Integer.parseInt(matcher2.group(2));
                    return Integer.compare(num1, num2);
                } else
                    return -1;
            } else if (matcher2.matches())
                return 1;
            return c1.compareTo(c2);
        }
    }

    @Override
    public Projection getProjection() {
        return Projections.getProjectionByCode(code);
    }

    @Override
    public String getCurrentCode() {
        // not needed - getProjection() is overridden
        throw new UnsupportedOperationException();
    }

    @Override
    public String getProjectionName() {
        // not needed - getProjection() is overridden
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPreferences(Collection<String> args) {
        if (args != null && !args.isEmpty()) {
            code = args.iterator().next();
        }
    }

    @Override
    public JPanel getPreferencePanel(ActionListener listener) {
        return new CodeSelectionPanel(code, listener);
    }

    @Override
    public Collection<String> getPreferences(JPanel panel) {
        if (!(panel instanceof CodeSelectionPanel)) {
            throw new IllegalArgumentException("Unsupported panel: "+panel);
        }
        CodeSelectionPanel csPanel = (CodeSelectionPanel) panel;
        return Collections.singleton(csPanel.getCode());
    }

    /* don't return all possible codes - this projection choice it too generic */
    @Override
    public String[] allCodes() {
        return new String[0];
    }

    /* not needed since allCodes() returns empty array */
    @Override
    public Collection<String> getPreferencesFromCode(String code) {
        return null;
    }

    @Override
    public boolean showProjectionCode() {
        return true;
    }

    @Override
    public boolean showProjectionName() {
        return true;
    }

}
