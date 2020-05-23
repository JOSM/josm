// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.AutoScaleAction.AutoScaleMode;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.util.AdjustmentSynchronizer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Base class of {@link TagInfoViewer} and {@link RelationMemberListViewer}.
 * @since 6207
 */
public abstract class HistoryViewerPanel extends HistoryBrowserPanel {

    protected transient AdjustmentSynchronizer adjustmentSynchronizer;
    protected transient SelectionSynchronizer selectionSynchronizer;

    protected HistoryViewerPanel(HistoryBrowserModel model) {
        setModel(model);
        build();
    }

    private JScrollPane embedInScrollPane(JTable table) {
        JScrollPane pane = new JScrollPane(table);
        adjustmentSynchronizer.participateInSynchronizedScrolling(pane.getVerticalScrollBar());
        return pane;
    }

    protected abstract JTable buildTable(PointInTimeType pointInTimeType);

    private void build() {
        GridBagConstraints gc = new GridBagConstraints();

        // ---------------------------
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.weightx = 0.5;
        gc.weighty = 0.0;
        gc.insets = new Insets(5, 5, 5, 0);
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        referenceInfoPanel = new VersionInfoPanel(model, PointInTimeType.REFERENCE_POINT_IN_TIME);
        add(referenceInfoPanel, gc);

        gc.gridx = 1;
        gc.gridy = 0;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 0.5;
        gc.weighty = 0.0;
        gc.anchor = GridBagConstraints.FIRST_LINE_START;
        currentInfoPanel = new VersionInfoPanel(model, PointInTimeType.CURRENT_POINT_IN_TIME);
        add(currentInfoPanel, gc);

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
        add(embedInScrollPane(buildTable(PointInTimeType.REFERENCE_POINT_IN_TIME)), gc);

        gc.gridx = 1;
        gc.gridy = 1;
        gc.gridwidth = 1;
        gc.gridheight = 1;
        gc.weightx = 0.5;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        gc.anchor = GridBagConstraints.NORTHWEST;
        add(embedInScrollPane(buildTable(PointInTimeType.CURRENT_POINT_IN_TIME)), gc);
    }

    static class ListPopupMenu extends JPopupMenu {
        private final ZoomToObjectAction zoomToObjectAction;
        private final ShowHistoryAction showHistoryAction;

        ListPopupMenu(String name, String shortDescription) {
            zoomToObjectAction = new ZoomToObjectAction(name, shortDescription);
            add(zoomToObjectAction);
            showHistoryAction = new ShowHistoryAction();
            add(showHistoryAction);
        }

        void prepare(PrimitiveId pid) {
            zoomToObjectAction.setPrimitiveId(pid);
            zoomToObjectAction.updateEnabledState();

            showHistoryAction.setPrimitiveId(pid);
            showHistoryAction.updateEnabledState();
        }
    }

    static class ZoomToObjectAction extends AbstractAction {
        private transient PrimitiveId primitiveId;

        /**
         * Constructs a new {@code ZoomToObjectAction}.
         * @param name  name for the action
         * @param shortDescription The key used for storing a short <code>String</code> description for the action, used for tooltip text.
         */
        ZoomToObjectAction(String name, String shortDescription) {
            putValue(NAME, name);
            putValue(SHORT_DESCRIPTION, shortDescription);
            new ImageProvider("dialogs", "zoomin").getResource().attachImageIcon(this, true);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (!isEnabled())
                return;
            IPrimitive p = getPrimitiveToZoom();
            if (p != null && p.isSelectable()) {
                p.getDataSet().setSelected(p);
                AutoScaleAction.autoScale(AutoScaleMode.SELECTION);
            }
        }

        public void setPrimitiveId(PrimitiveId pid) {
            this.primitiveId = pid;
            updateEnabledState();
        }

        protected IPrimitive getPrimitiveToZoom() {
            if (primitiveId == null)
                return null;
            OsmData<?, ?, ?, ?> ds = MainApplication.getLayerManager().getActiveData();
            if (ds == null)
                return null;
            return ds.getPrimitiveById(primitiveId);
        }

        public void updateEnabledState() {
            setEnabled(MainApplication.getLayerManager().getActiveData() != null && getPrimitiveToZoom() != null);
        }
    }
}
