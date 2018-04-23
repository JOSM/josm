// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.PrimitiveRenderer;

/**
 * This panel displays a summary of the objects to upload. It is displayed in the upper part of the {@link UploadDialog}.
 * @since 2599
 */
public class UploadedObjectsSummaryPanel extends JPanel {
    /**
     * The swing property name for the number of objects to upload
     */
    public static final String NUM_OBJECTS_TO_UPLOAD_PROP = UploadedObjectsSummaryPanel.class.getName() + ".numObjectsToUpload";

    /** the list with the added primitives */
    private PrimitiveList lstAdd;
    private JLabel lblAdd;
    private JScrollPane spAdd;
    /** the list with the updated primitives */
    private PrimitiveList lstUpdate;
    private JLabel lblUpdate;
    private JScrollPane spUpdate;
    /** the list with the deleted primitives */
    private PrimitiveList lstDelete;
    private JLabel lblDelete;
    private JScrollPane spDelete;

    /**
     * Constructs a new {@code UploadedObjectsSummaryPanel}.
     */
    public UploadedObjectsSummaryPanel() {
        build();
    }

    protected void build() {
        setLayout(new GridBagLayout());
        PrimitiveRenderer renderer = new PrimitiveRenderer();
        MouseAdapter mouseListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                if (evt.getButton() == MouseEvent.BUTTON1 && evt.getClickCount() == 2) {
                    PrimitiveList list = (PrimitiveList) evt.getSource();
                    int index = list.locationToIndex(evt.getPoint());
                    AutoScaleAction.zoomTo(Collections.singleton(list.getModel().getElementAt(index)));
                }
            }
        };
        // initialize the three lists for uploaded primitives, but don't add them to the dialog yet, see setUploadedPrimitives()
        //
        lstAdd = new PrimitiveList();
        lstAdd.setCellRenderer(renderer);
        lstAdd.addMouseListener(mouseListener);
        lstAdd.setVisibleRowCount(Math.min(lstAdd.getModel().getSize(), 10));
        spAdd = new JScrollPane(lstAdd);
        lblAdd = new JLabel(tr("Objects to add:"));
        lblAdd.setLabelFor(lstAdd);

        lstUpdate = new PrimitiveList();
        lstUpdate.setCellRenderer(renderer);
        lstUpdate.addMouseListener(mouseListener);
        lstUpdate.setVisibleRowCount(Math.min(lstUpdate.getModel().getSize(), 10));
        spUpdate = new JScrollPane(lstUpdate);
        lblUpdate = new JLabel(tr("Objects to modify:"));
        lblUpdate.setLabelFor(lstUpdate);

        lstDelete = new PrimitiveList();
        lstDelete.setCellRenderer(renderer);
        lstDelete.addMouseListener(mouseListener);
        lstDelete.setVisibleRowCount(Math.min(lstDelete.getModel().getSize(), 10));
        spDelete = new JScrollPane(lstDelete);
        lblDelete = new JLabel(tr("Objects to delete:"));
        lblDelete.setLabelFor(lstDelete);
    }

    /**
     * Sets the collections of primitives which will be uploaded
     *
     * @param add  the collection of primitives to add
     * @param update the collection of primitives to update
     * @param delete the collection of primitives to delete
     */
    public void setUploadedPrimitives(List<OsmPrimitive> add, List<OsmPrimitive> update, List<OsmPrimitive> delete) {
        lstAdd.getPrimitiveListModel().setPrimitives(add);
        lstUpdate.getPrimitiveListModel().setPrimitives(update);
        lstDelete.getPrimitiveListModel().setPrimitives(delete);

        GridBagConstraints gcLabel = new GridBagConstraints();
        gcLabel.fill = GridBagConstraints.HORIZONTAL;
        gcLabel.weightx = 1.0;
        gcLabel.weighty = 0.0;
        gcLabel.anchor = GridBagConstraints.FIRST_LINE_START;

        GridBagConstraints gcList = new GridBagConstraints();
        gcList.fill = GridBagConstraints.BOTH;
        gcList.weightx = 1.0;
        gcList.weighty = 1.0;
        gcList.anchor = GridBagConstraints.CENTER;
        removeAll();
        int y = -1;
        if (!add.isEmpty()) {
            y++;
            gcLabel.gridy = y;
            lblAdd.setText(trn("{0} object to add:", "{0} objects to add:", add.size(), add.size()));
            add(lblAdd, gcLabel);
            y++;
            gcList.gridy = y;
            add(spAdd, gcList);
        }
        if (!update.isEmpty()) {
            y++;
            gcLabel.gridy = y;
            lblUpdate.setText(trn("{0} object to modify:", "{0} objects to modify:", update.size(), update.size()));
            add(lblUpdate, gcLabel);
            y++;
            gcList.gridy = y;
            add(spUpdate, gcList);
        }
        if (!delete.isEmpty()) {
            y++;
            gcLabel.gridy = y;
            lblDelete.setText(trn("{0} object to delete:", "{0} objects to delete:", delete.size(), delete.size()));
            add(lblDelete, gcLabel);
            y++;
            gcList.gridy = y;
            add(spDelete, gcList);
        }

        firePropertyChange(NUM_OBJECTS_TO_UPLOAD_PROP, 0, getNumObjectsToUpload());
    }

    /**
     * Replies the number of objects to upload
     *
     * @return the number of objects to upload
     */
    public int getNumObjectsToUpload() {
        return lstAdd.getModel().getSize()
        + lstUpdate.getModel().getSize()
        + lstDelete.getModel().getSize();
    }

    /**
     * A simple list of OSM primitives.
     */
    static class PrimitiveList extends JList<OsmPrimitive> {
        /**
         * Constructs a new {@code PrimitiveList}.
         */
        PrimitiveList() {
            super(new PrimitiveListModel());
        }

        public PrimitiveListModel getPrimitiveListModel() {
            return (PrimitiveListModel) getModel();
        }
    }

    /**
     * A list model for a list of OSM primitives.
     */
    static class PrimitiveListModel extends AbstractListModel<OsmPrimitive> {
        private transient List<OsmPrimitive> primitives;

        /**
         * Constructs a new {@code PrimitiveListModel}.
         */
        PrimitiveListModel() {
            primitives = new ArrayList<>();
        }

        PrimitiveListModel(List<OsmPrimitive> primitives) {
            setPrimitives(primitives);
        }

        public void setPrimitives(List<OsmPrimitive> primitives) {
            this.primitives = Optional.ofNullable(primitives).orElseGet(ArrayList::new);
            fireContentsChanged(this, 0, getSize());
        }

        @Override
        public OsmPrimitive getElementAt(int index) {
            if (primitives == null) return null;
            return primitives.get(index);
        }

        @Override
        public int getSize() {
            if (primitives == null) return 0;
            return primitives.size();
        }
    }
}
