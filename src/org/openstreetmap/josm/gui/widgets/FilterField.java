// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.PatternSyntaxException;

import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Text field allowing to filter contents.
 * @since 15116
 */
public class FilterField extends JosmTextField {

    /**
     * Constructs a new {@code TableFilterField}.
     */
    public FilterField() {
        setToolTipText(tr("Enter a search expression"));
        SelectAllOnFocusGainedDecorator.decorate(this);
    }

    /**
     * Defines the filter behaviour.
     */
    @FunctionalInterface
    public interface FilterBehaviour {
        /**
         * Filters a component according to the given filter expression.
         * @param expr filter expression
         */
        void filter(String expr);
    }

    /**
     * Enables filtering of given table/model.
     * @param table table to filter
     * @param model table model
     * @return {@code this} for easy chaining
     */
    public FilterField filter(JTable table, AbstractTableModel model) {
        return filter(new TableFilterBehaviour(table, model));
    }

    /**
     * Enables generic filtering.
     * @param behaviour filter behaviour
     * @return {@code this} for easy chaining
     */
    public FilterField filter(FilterBehaviour behaviour) {
        getDocument().addDocumentListener(new FilterFieldAdapter(behaviour));
        return this;
    }

    private static class TableFilterBehaviour implements FilterBehaviour {
        private final JTable table;
        private final AbstractTableModel model;

        TableFilterBehaviour(JTable table, AbstractTableModel model) {
            this.table = Objects.requireNonNull(table, "table");
            this.model = Objects.requireNonNull(model, "model");
            Objects.requireNonNull(table.getRowSorter(), "table.rowSorter");
        }

        @Override
        public void filter(String expr) {
            try {
                final TableRowSorter<? extends TableModel> sorter =
                    (TableRowSorter<? extends TableModel>) table.getRowSorter();
                if (expr == null || expr.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    expr = expr.replace("+", "\\+");
                    // split search string on whitespace, do case-insensitive AND search
                    List<RowFilter<Object, Object>> andFilters = new ArrayList<>();
                    for (String word : expr.split("\\s+")) {
                        andFilters.add(RowFilter.regexFilter("(?i)" + word));
                    }
                    sorter.setRowFilter(RowFilter.andFilter(andFilters));
                }
                model.fireTableDataChanged();
            } catch (PatternSyntaxException | ClassCastException ex) {
                Logging.warn(ex);
            }
        }
    }

    private class FilterFieldAdapter implements DocumentListener {
        private final FilterBehaviour behaviour;

        FilterFieldAdapter(FilterBehaviour behaviour) {
            this.behaviour = Objects.requireNonNull(behaviour);
        }

        private void filter() {
            behaviour.filter(Utils.strip(getText()));
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            filter();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            filter();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            filter();
        }
    }
}
