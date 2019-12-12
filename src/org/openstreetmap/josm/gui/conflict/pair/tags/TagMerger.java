// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Adjustable;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.conflict.pair.AbstractMergePanel;
import org.openstreetmap.josm.gui.conflict.pair.IConflictResolver;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.gui.tagging.TagTableColumnModelBuilder;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageResource;

/**
 * UI component for resolving conflicts in the tag sets of two {@link OsmPrimitive}s.
 * @since 1622
 */
public class TagMerger extends AbstractMergePanel implements IConflictResolver {
    private static final String[] KEY_VALUE = {tr("Key"), tr("Value")};

    private final TagMergeModel model = new TagMergeModel();

    /**
     * the table for my tag set
     */
    private final JTable mineTable = generateTable(new MineTableCellRenderer());
    /**
     * the table for the merged tag set
     */
    private final JTable mergedTable = generateTable(new MergedTableCellRenderer());
    /**
     * the table for their tag set
     */
    private final JTable theirTable = generateTable(new TheirTableCellRenderer());

    /**
     * Constructs a new {@code TagMerger}.
     */
    public TagMerger() {
        mineTable.setName("table.my");
        theirTable.setName("table.their");
        mergedTable.setName("table.merged");

        DoubleClickAdapter dblClickAdapter = new DoubleClickAdapter();
        mineTable.addMouseListener(dblClickAdapter);
        theirTable.addMouseListener(dblClickAdapter);

        buildRows();
    }

    private JTable generateTable(TagMergeTableCellRenderer renderer) {
        return new JTable(model, new TagTableColumnModelBuilder(renderer, KEY_VALUE).build());
    }

    @Override
    protected List<? extends MergeRow> getRows() {
        return Arrays.asList(new TitleRow(), new TagTableRow(), new UndecidedRow());
    }

    /**
     * replies the model used by this tag merger
     *
     * @return the model
     */
    public TagMergeModel getModel() {
        return model;
    }

    private void selectNextConflict(int... rows) {
        int max = rows[0];
        for (int row: rows) {
            if (row > max) {
                max = row;
            }
        }
        int index = model.getFirstUndecided(max+1);
        if (index == -1) {
            index = model.getFirstUndecided(0);
        }
        mineTable.getSelectionModel().setSelectionInterval(index, index);
        theirTable.getSelectionModel().setSelectionInterval(index, index);
    }

    private final class TagTableRow extends MergeRow {
        private final AdjustmentSynchronizer adjustmentSynchronizer = new AdjustmentSynchronizer();

        /**
         * embeds table in a new {@link JScrollPane} and returns th scroll pane
         *
         * @param table the table
         * @return the scroll pane embedding the table
         */
        JScrollPane embeddInScrollPane(JTable table) {
            JScrollPane pane = new JScrollPane(table);
            adjustmentSynchronizer.synchronizeAdjustment(pane.getVerticalScrollBar());
            return pane;
        }

        @Override
        protected JComponent mineField() {
            return embeddInScrollPane(mineTable);
        }

        @Override
        protected JComponent mineButton() {
            KeepMineAction keepMineAction = new KeepMineAction();
            mineTable.getSelectionModel().addListSelectionListener(keepMineAction);
            JButton btnKeepMine = new JButton(keepMineAction);
            btnKeepMine.setName("button.keepmine");
            return btnKeepMine;
        }

        @Override
        protected JComponent merged() {
            return embeddInScrollPane(mergedTable);
        }

        @Override
        protected JComponent theirsButton() {
            KeepTheirAction keepTheirAction = new KeepTheirAction();
            theirTable.getSelectionModel().addListSelectionListener(keepTheirAction);
            JButton btnKeepTheir = new JButton(keepTheirAction);
            btnKeepTheir.setName("button.keeptheir");
            return btnKeepTheir;
        }

        @Override
        protected JComponent theirsField() {
            return embeddInScrollPane(theirTable);
        }

        @Override
        protected void addConstraints(GBC constraints, int columnIndex) {
            super.addConstraints(constraints, columnIndex);
            // Fill to bottom
            constraints.weighty = 1;
        }
    }

    private final class UndecidedRow extends AbstractUndecideRow {
        @Override
        protected AbstractAction createAction() {
            UndecideAction undecidedAction = new UndecideAction();
            mergedTable.getSelectionModel().addListSelectionListener(undecidedAction);
            return undecidedAction;
        }

        @Override
        protected String getButtonName() {
            return "button.undecide";
        }
    }

    /**
     * Keeps the currently selected tags in my table in the list of merged tags.
     *
     */
    class KeepMineAction extends AbstractAction implements ListSelectionListener {
        KeepMineAction() {
            ImageResource icon = new ImageProvider("dialogs/conflict", "tagkeepmine").getResource();
            if (icon != null) {
                icon.attachImageIcon(this, true);
                putValue(Action.NAME, "");
            } else {
                putValue(Action.NAME, ">");
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Keep the selected key/value pairs from the local dataset"));
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int[] rows = mineTable.getSelectedRows();
            if (rows.length == 0)
                return;
            model.decide(rows, MergeDecisionType.KEEP_MINE);
            selectNextConflict(rows);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            setEnabled(mineTable.getSelectedRowCount() > 0);
        }
    }

    /**
     * Keeps the currently selected tags in their table in the list of merged tags.
     *
     */
    class KeepTheirAction extends AbstractAction implements ListSelectionListener {
        KeepTheirAction() {
            ImageResource icon = new ImageProvider("dialogs/conflict", "tagkeeptheir").getResource();
            if (icon != null) {
                icon.attachImageIcon(this, true);
                putValue(Action.NAME, "");
            } else {
                putValue(Action.NAME, ">");
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Keep the selected key/value pairs from the server dataset"));
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int[] rows = theirTable.getSelectedRows();
            if (rows.length == 0)
                return;
            model.decide(rows, MergeDecisionType.KEEP_THEIR);
            selectNextConflict(rows);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            setEnabled(theirTable.getSelectedRowCount() > 0);
        }
    }

    /**
     * Synchronizes scrollbar adjustments between a set of
     * {@link Adjustable}s. Whenever the adjustment of one of
     * the registerd Adjustables is updated the adjustment of
     * the other registered Adjustables is adjusted too.
     *
     */
    static class AdjustmentSynchronizer implements AdjustmentListener {
        private final Set<Adjustable> synchronizedAdjustables;

        AdjustmentSynchronizer() {
            synchronizedAdjustables = new HashSet<>();
        }

        public void synchronizeAdjustment(Adjustable adjustable) {
            if (adjustable == null)
                return;
            if (synchronizedAdjustables.contains(adjustable))
                return;
            synchronizedAdjustables.add(adjustable);
            adjustable.addAdjustmentListener(this);
        }

        @Override
        public void adjustmentValueChanged(AdjustmentEvent e) {
            for (Adjustable a : synchronizedAdjustables) {
                if (a != e.getAdjustable()) {
                    a.setValue(e.getValue());
                }
            }
        }
    }

    /**
     * Handler for double clicks on entries in the three tag tables.
     *
     */
    class DoubleClickAdapter extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() != 2)
                return;
            JTable table;
            MergeDecisionType mergeDecision;

            if (e.getSource() == mineTable) {
                table = mineTable;
                mergeDecision = MergeDecisionType.KEEP_MINE;
            } else if (e.getSource() == theirTable) {
                table = theirTable;
                mergeDecision = MergeDecisionType.KEEP_THEIR;
            } else if (e.getSource() == mergedTable) {
                table = mergedTable;
                mergeDecision = MergeDecisionType.UNDECIDED;
            } else
                // double click in another component; shouldn't happen,
                // but just in case
                return;
            int row = table.rowAtPoint(e.getPoint());
            model.decide(row, mergeDecision);
        }
    }

    /**
     * Sets the currently selected tags in the table of merged tags to state
     * {@link MergeDecisionType#UNDECIDED}
     *
     */
    class UndecideAction extends AbstractAction implements ListSelectionListener {

        UndecideAction() {
            ImageResource icon = new ImageProvider("dialogs/conflict", "tagundecide").getResource();
            if (icon != null) {
                icon.attachImageIcon(this, true);
                putValue(Action.NAME, "");
            } else {
                putValue(Action.NAME, tr("Undecide"));
            }
            putValue(SHORT_DESCRIPTION, tr("Mark the selected tags as undecided"));
            setEnabled(false);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            int[] rows = mergedTable.getSelectedRows();
            if (rows.length == 0)
                return;
            model.decide(rows, MergeDecisionType.UNDECIDED);
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {
            setEnabled(mergedTable.getSelectedRowCount() > 0);
        }
    }

    @Override
    public void deletePrimitive(boolean deleted) {
        // Use my entries, as it doesn't really matter
        MergeDecisionType decision = deleted ? MergeDecisionType.KEEP_MINE : MergeDecisionType.UNDECIDED;
        for (int i = 0; i < model.getRowCount(); i++) {
            model.decide(i, decision);
        }
    }

    @Override
    public void populate(Conflict<? extends OsmPrimitive> conflict) {
        model.populate(conflict.getMy(), conflict.getTheir());
        for (JTable table : new JTable[]{mineTable, theirTable}) {
            int index = table.getRowCount() > 0 ? 0 : -1;
            table.getSelectionModel().setSelectionInterval(index, index);
        }
    }

    @Override
    public void decideRemaining(MergeDecisionType decision) {
        model.decideRemaining(decision);
    }
}
