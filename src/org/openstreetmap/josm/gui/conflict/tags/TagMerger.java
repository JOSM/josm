// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

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
import java.net.URL;
import java.util.ArrayList;

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

public class TagMerger extends JPanel {

    private JTable mineTable;
    private JTable mergedTable;
    private JTable theirTable;
    private final TagMergeModel model;
    private JButton btnKeepMine;
    private JButton btnKeepTheir;
    AdjustmentSynchronizer adjustmentSynchronizer;

    protected JScrollPane embeddInScrollPane(JTable table) {
        JScrollPane pane = new JScrollPane(table);
        pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        adjustmentSynchronizer.synchronizeAdjustment(pane.getVerticalScrollBar());
        return pane;
    }

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

    protected JScrollPane buildUndecidedTable() {
        mergedTable  = new JTable(
                model,
                new TagMergeColumnModel(
                        new UndecidedTableCellRenderer()
                )
        );
        mergedTable.setName("table.merged");
        return embeddInScrollPane(mergedTable);
    }

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
        btnKeepMine = new JButton(keepMineAction);
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
        add(buildUndecidedTable(), gc);

        gc.gridx = 3;
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.CENTER;
        gc.weightx = 0.0;
        gc.weighty = 0.0;
        KeepTheirAction keepTheirAction = new KeepTheirAction();
        btnKeepTheir = new JButton(keepTheirAction);
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

    public TagMerger() {
        model = new TagMergeModel();
        build();
    }

    public TagMergeModel getModel() {
        return model;
    }

    protected ImageIcon loadIcon(String name) {
        String path = "/images/dialogs/conflict/" + name;
        URL url = this.getClass().getResource(path);
        if (url == null) {
            System.out.println(tr("WARNING: failed to load resource {0}", path));
            return null;
        }
        return new ImageIcon(url);
    }

    class KeepMineAction extends AbstractAction implements ListSelectionListener {


        public KeepMineAction() {
            ImageIcon icon = loadIcon("tagkeepmine.png");
            if (icon != null) {
                putValue(Action.SMALL_ICON, icon);
                putValue(Action.NAME, "");
            } else {
                putValue(Action.NAME, tr(">"));
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Keep the selected key/value pairs from the local dataset"));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent arg0) {
            int rows[] = mineTable.getSelectedRows();
            if (rows == null || rows.length == 0)
                return;
            model.decide(rows, MergeDecisionType.KEEP_MINE);
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(mineTable.getSelectedRowCount() > 0);
        }
    }

    class KeepTheirAction extends AbstractAction implements ListSelectionListener {

        public KeepTheirAction() {
            ImageIcon icon = loadIcon("tagkeeptheir.png");
            if (icon != null) {
                putValue(Action.SMALL_ICON, icon);
                putValue(Action.NAME, "");
            } else {
                putValue(Action.NAME, tr(">"));
            }
            putValue(Action.SHORT_DESCRIPTION, tr("Keep the selected key/value pairs from the server dataset"));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent arg0) {
            int rows[] = theirTable.getSelectedRows();
            if (rows == null || rows.length == 0)
                return;
            model.decide(rows, MergeDecisionType.KEEP_THEIR);
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(theirTable.getSelectedRowCount() > 0);
        }
    }

    class AdjustmentSynchronizer implements AdjustmentListener {
        private final ArrayList<Adjustable> synchronizedAdjustables;

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

        public void adjustmentValueChanged(AdjustmentEvent e) {
            for (Adjustable a : synchronizedAdjustables) {
                if (a != e.getAdjustable()) {
                    a.setValue(e.getValue());
                }
            }
        }
    }

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

    class UndecideAction extends AbstractAction implements ListSelectionListener  {

        public UndecideAction() {
            ImageIcon icon = loadIcon("tagundecide.png");
            if (icon != null) {
                putValue(Action.SMALL_ICON, icon);
                putValue(Action.NAME, "");
            } else {
                putValue(Action.NAME, tr("Undecide"));
            }
            putValue(SHORT_DESCRIPTION, tr("Mark the selected tags as undecided"));
            setEnabled(false);
        }

        public void actionPerformed(ActionEvent arg0) {
            int rows[] = mergedTable.getSelectedRows();
            if (rows == null || rows.length == 0)
                return;
            model.decide(rows, MergeDecisionType.UNDECIDED);
        }

        public void valueChanged(ListSelectionEvent e) {
            setEnabled(mergedTable.getSelectedRowCount() > 0);
        }
    }
}
