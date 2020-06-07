// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.tools.GBC;

/**
 * A panel used as tab in the merge dialog.
 * Contains helper methods for creating merge dialog columns.
 *
 * @author Michael Zangl
 * @since 12044
 */
public abstract class AbstractMergePanel extends JPanel {

    /**
     * A helper class to add a row to the layout. Each row has 6 columns.
     * @author Michael Zangl
     */
    protected static class MergeRow {
        protected int marginTop = 5;

        public MergeRow() {
            // allow access from subclasses
        }

        protected JComponent rowTitle() {
            return null;
        }

        protected JComponent mineField() {
            return null;
        }

        protected JComponent mineButton() {
            return null;
        }

        protected JComponent merged() {
            return null;
        }

        protected JComponent theirsButton() {
            return null;
        }

        protected JComponent theirsField() {
            return null;
        }

        void addTo(AbstractMergePanel panel) {
            JComponent[] buttons = getColumns();
            for (int columnIndex = 0; columnIndex < buttons.length; columnIndex++) {
                if (buttons[columnIndex] != null) {
                    GBC constraints = GBC.std(columnIndex, panel.currentRow);
                    addConstraints(constraints, columnIndex);
                    panel.add(buttons[columnIndex], constraints);
                }
            }
            panel.currentRow++;
        }

        protected JComponent[] getColumns() {
            return new JComponent[] {
                    rowTitle(),
                    mineField(),
                    mineButton(),
                    merged(),
                    theirsButton(),
                    theirsField()
            };
        }

        protected void addConstraints(GBC constraints, int columnIndex) {
            constraints.anchor(GBC.CENTER);
            constraints.fill = GBC.BOTH;
            constraints.weight(0, 0);
            constraints.insets(3, marginTop, 3, 0);
            if (columnIndex == 1 || columnIndex == 3 || columnIndex == 5) {
                // resize those rows
                constraints.weightx = 1;
            }
        }
    }

    /**
     * A row that does not contain the merge buttons. Fields in this row fill both the button and filed area.
     */
    protected static class MergeRowWithoutButton extends MergeRow {
        @Override
        protected JComponent[] getColumns() {
            return new JComponent[] {
                    rowTitle(),
                    mineField(), // width: 2
                    null,
                    merged(),
                    theirsField(), // width: 2
                    null,
            };
        }

        @Override
        protected void addConstraints(GBC constraints, int columnIndex) {
            super.addConstraints(constraints, columnIndex);

            if (columnIndex == 1 || columnIndex == 4) {
                constraints.gridwidth = 2;
            }
        }
    }

    /**
     * The title for the rows (mine, merged, theirs)
     */
    protected static class TitleRow extends MergeRow {
        public TitleRow() {
            // allow access from subclasses
        }

        @Override
        protected JComponent mineField() {
            JLabel label = new JLabel(tr("My version (local dataset)"));
            label.setToolTipText(tr("Properties in my dataset, i.e. the local dataset"));
            label.setHorizontalAlignment(JLabel.CENTER);
            return label;
        }

        @Override
        protected JComponent merged() {
            JLabel label = new JLabel(tr("Merged version"));
            label.setToolTipText(
                    tr("Properties in the merged element. They will replace properties in my elements when merge decisions are applied."));
            label.setHorizontalAlignment(JLabel.CENTER);
            return label;
        }

        @Override
        protected JComponent theirsField() {
            JLabel label = new JLabel(tr("Their version (server dataset)"));
            label.setToolTipText(tr("Properties in their dataset, i.e. the server dataset"));
            label.setHorizontalAlignment(JLabel.CENTER);
            return label;
        }
    }

    /**
     * Add the undecide button to the middle of the merged row.
     */
    protected abstract static class AbstractUndecideRow extends AbstractMergePanel.MergeRow {
        @Override
        protected JComponent merged() {
            AbstractAction actUndecide = createAction();
            JButton button = new JButton(actUndecide);
            button.setName(getButtonName());
            return button;
        }

        protected abstract AbstractAction createAction();

        protected abstract String getButtonName();

        @Override
        protected void addConstraints(GBC constraints, int columnIndex) {
            super.addConstraints(constraints, columnIndex);
            constraints.fill(GBC.NONE);
        }
    }

    /**
     * The current row counter. Used when adding new rows.
     */
    protected int currentRow;

    /**
     * Create a new merge panel.
     */
    protected AbstractMergePanel() {
        super(new GridBagLayout());
    }

    /**
     * Add the rows to this component.
     * This needs to be called in the constructor of the child class. That way, all it's fields are initialized.
     */
    protected void buildRows() {
        getRows().forEach(row -> row.addTo(this));
    }

    /**
     * Gets the rows.
     * @return A list of rows that should be displayed in this dialog.
     */
    protected abstract List<? extends MergeRow> getRows();

}
