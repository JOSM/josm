// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
/**
 * RelationMemberListViewer is a UI component which displays the  list of relation members of two
 * version of a {@see Relation} in a {@see History}.
 *
 * <ul>
 *   <li>on the left, it displays the list of relation members for the version at {@see PointInTimeType#REFERENCE_POINT_IN_TIME}</li>
 *   <li>on the right, it displays the list of relation members for the version at {@see PointInTimeType#CURRENT_POINT_IN_TIME}</li>
 * </ul>
 *
 */

public class RelationMemberListViewer extends JPanel{

    private HistoryBrowserModel model;
    private VersionInfoPanel referenceInfoPanel;
    private VersionInfoPanel currentInfoPanel;
    private AdjustmentSynchronizer adjustmentSynchronizer;
    private SelectionSynchronizer selectionSynchronizer;

    protected JScrollPane embeddInScrollPane(JTable table) {
        JScrollPane pane = new JScrollPane(table);
        pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        adjustmentSynchronizer.participateInSynchronizedScrolling(pane.getVerticalScrollBar());
        return pane;
    }

    protected JTable buildReferenceMemberListTable() {
        JTable table = new JTable(
                model.getRelationMemberTableModel(PointInTimeType.REFERENCE_POINT_IN_TIME),
                new RelationMemberTableColumnModel()
        );
        table.setName("table.referencememberlisttable");
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionSynchronizer.participateInSynchronizedSelection(table.getSelectionModel());
        return table;
    }

    protected JTable buildCurrentMemberListTable() {
        JTable table = new JTable(
                model.getRelationMemberTableModel(PointInTimeType.CURRENT_POINT_IN_TIME),
                new RelationMemberTableColumnModel()
        );
        table.setName("table.currentmemberlisttable");
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectionSynchronizer.participateInSynchronizedSelection(table.getSelectionModel());
        return table;
    }

    protected void build() {
        setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();

        // ---------------------------
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.weightx = 0.5;
        gc.weighty = 0.0;
        gc.insets = new Insets(5,5,5,0);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        referenceInfoPanel = new VersionInfoPanel(model, PointInTimeType.REFERENCE_POINT_IN_TIME);
        add(referenceInfoPanel,gc);

        gc.gridx = 1;
        gc.gridy = 0;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.5;
        gc.weighty = 0.0;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        currentInfoPanel = new VersionInfoPanel(model, PointInTimeType.CURRENT_POINT_IN_TIME);
        add(currentInfoPanel,gc);

        adjustmentSynchronizer = new AdjustmentSynchronizer();
        selectionSynchronizer = new SelectionSynchronizer();

        // ---------------------------
        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.weightx = 0.5;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.NORTHWEST;
        add(embeddInScrollPane(buildReferenceMemberListTable()),gc);

        gc.gridx = 1;
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.weightx = 0.5;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.NORTHWEST;
        add(embeddInScrollPane(buildCurrentMemberListTable()),gc);
    }

    public RelationMemberListViewer(HistoryBrowserModel model) {
        setModel(model);
        build();
    }

    protected void unregisterAsObserver(HistoryBrowserModel model) {
        if (currentInfoPanel != null) {
            model.deleteObserver(currentInfoPanel);
        }
        if (referenceInfoPanel != null) {
            model.deleteObserver(referenceInfoPanel);
        }
    }
    protected void registerAsObserver(HistoryBrowserModel model) {
        if (currentInfoPanel != null) {
            model.addObserver(currentInfoPanel);
        }
        if (referenceInfoPanel != null) {
            model.addObserver(referenceInfoPanel);
        }
    }

    public void setModel(HistoryBrowserModel model) {
        if (this.model != null) {
            unregisterAsObserver(model);
        }
        this.model = model;
        if (this.model != null) {
            registerAsObserver(model);
        }
    }
}
