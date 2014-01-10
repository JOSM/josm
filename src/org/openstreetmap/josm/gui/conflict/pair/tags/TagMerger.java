// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair.tags;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Adjustable;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.conflict.pair.IConflictResolver;
import org.openstreetmap.josm.gui.conflict.pair.MergeDecisionType;
import org.openstreetmap.josm.tools.ImageProvider;
/**
 * UI component for resolving conflicts in the tag sets of two {@link OsmPrimitive}s.
 *
 */
public class TagMerger extends JPanel implements IConflictResolver {

    private JTable mineTable;
    private JTable mergedTable;
    private JTable theirTable;
    private final TagMergeModel model;
    AdjustmentSynchronizer adjustmentSynchronizer;

    /**
     * embeds table in a new {@link JScrollPane} and returns th scroll pane
     *
     * @param table the table
     * @return the scroll pane embedding the table
     */
    protected JScrollPane embeddInScrollPane(JTable table) {
        JScrollPane pane = new JScrollPane(table);
        adjustmentSynchronizer.synchronizeAdjustment(pane.getVerticalScrollBar());
        return pane;
    }

    /**
     * builds the table for my tag set (table already embedded in a scroll pane)
     *
     * @return the table (embedded in a scroll pane)
     */
    protected JScrollPane buildMineTagTable() {
        mineTable  = new JTable(
                model,
                new TagMergeColumnModel(
                        new MineTableCellRenderer()
                )
        );
        mineTable.setName("table.my");
        return embeddInScrollPane(mineTable);
    }

    /**
     * builds the table for their tag set (table already embedded in a scroll pane)
     *
     * @return the table (embedded in a scroll pane)
     */
    protected JScrollPane buildTheirTable() {
        theirTable  = new JTable(
                model,
                new TagMergeColumnModel(
                        new TheirTableCellRenderer()
                )
        );
        theirTable.setName("table.their");
        return embeddInScrollPane(theirTable);
    }

    /**
     * builds the table for the merged tag set (table already embedded in a scroll pane)
     *
     * @return the table (embedded in a scroll pane)
     */

    protected JScrollPane buildMergedTable() {
        mergedTable  = new JTable(
                model,
                new TagMergeColumnModel(
                        new MergedTableCellRenderer()
                )
        );
        mergedTable.setName("table.merged");
        return embeddInScrollPane(mergedTable);
    }

    /**
     * build the user interface
     */
    protected void build() {
        GridBagConstraints gc = new GridBagConstraints();
        setLayout(new GridBagLayout());

        adjustmentSynchronizer = new AdjustmentSynchronizer();

        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.insets = new Insets(10,0,10,0);
        JLabel lbl = new JLabel(tr("My version (local dataset)"));
        add(lbl, gc);

        gc.gridx = 2;
        gc.gridy = 0;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        lbl = new JLabel(tr("Merged version"));
        add(lbl, gc);

        gc.gridx = 4;
        gc.gridy = 0;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        gc.insets = new Insets(0,0,0,0);
        lbl = new JLabel(tr("Their version (server dataset)"));
        add(lbl, gc);

        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.weightx = 0.3;
        gc.weighty = 1.0;
        add(buildMineTagTable(), gc);

        gc.gridx = 1;
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        KeepMineAction keepMineAction = new KeepMineAction();
        mineTable.getSelectionModel().addListSelectionListener(keepMineAction);
        JButton btnKeepMine = new JButton(keepMineAction);
        btnKeepMine.setName("button.keepmine");
        add(btnKeepMine, gc);

        gc.gridx = 2;
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.weightx = 0.3;
        gc.weighty = 1.0;
        add(buildMergedTable(), gc);

        gc.gridx = 3;
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        KeepTheirAction keepTheirAction = new KeepTheirAction();
        JButton btnKeepTheir = new JButton(keepTheirAction);
        btnKeepTheir.setName("button.keeptheir");
        add(btnKeepTheir, gc);

        gc.gridx = 4;
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        gc.weightx = 0.3;
        gc.weighty = 1.0;
        add(buildTheirTable(), gc);
        theirTable.getSelectionModel().addListSelectionListener(keepTheirAction);

        DoubleClickAdapter dblClickAdapter = new DoubleClickAdapter();
        mineTable.addMouseListener(dblClickAdapter);
        theirTable.addMouseListener(dblClickAdapter);

        gc.gridx = 2;
        gc.gridy = 2;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        UndecideAction undecidedAction = new UndecideAction();
        mergedTable.getSelectionModel().addListSelectionListener(undecidedAction);
        JButton btnUndecide = new JButton(undecidedAction);
        btnUndecide.setName("button.undecide");
        add(btnUndecide, gc);

    }

    /**
     * Constructs a new {@code TagMerger}.
     */
    public TagMerger() {
        model = new TagMergeModel();
        build();
    }

    /**
     * replies the model used by this tag merger
     *
     * @return the model
     */
    public TagMergeModel getModel() {
        return model;
    }

    private void selectNextConflict(int[] rows) {
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

    /**
     * Keeps the currently selected tags in my table in the list of merged tags.
     *
     */
    class KeepMineAction extends AbstractAction implements ListSelectionListener {
        public KeepMineAction() {
            ImageIcon icon = ImageProvider.get("dialogs/conflict", "tagkeepmine.png");
            if (icon != null) {
                putValue(Action.SMALL_ICON, icon);
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
            if (rows == null || rows.length == 0)
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
        public KeepTheirAction() {
            ImageIcon icon = ImageProvider.get("dialogs/conflict", "tagkeeptheir.png");
            if (icon != null) {
                putValue(Action.SMALL_ICON, icon);
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
            if (rows == null || rows.length == 0)
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
        private final List<Adjustable> synchronizedAdjustables;

        public AdjustmentSynchronizer() {
            synchronizedAdjustables = new ArrayList<Adjustable>();
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
            JTable table = null;
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
    class UndecideAction extends AbstractAction implements ListSelectionListener  {

        public UndecideAction() {
            ImageIcon icon = ImageProvider.get("dialogs/conflict", "tagundecide.png");
            if (icon != null) {
                putValue(Action.SMALL_ICON, icon);
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
            if (rows == null || rows.length == 0)
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
        MergeDecisionType decision = deleted?MergeDecisionType.KEEP_MINE:MergeDecisionType.UNDECIDED;
        for (int i=0; i<model.getRowCount(); i++) {
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
}
