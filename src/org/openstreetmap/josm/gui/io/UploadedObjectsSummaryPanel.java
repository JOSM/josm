// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.OsmPrimitivRenderer;

/**
 * This panel displays a summary of the objects to upload. It is displayed in
 * the upper part of the {@link UploadDialog}.
 *
 */
public class UploadedObjectsSummaryPanel extends JPanel {
    static public final String NUM_OBJECTS_TO_UPLOAD_PROP = UploadedObjectsSummaryPanel.class.getName() + ".numObjectsToUpload";

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

    protected void build() {
        setLayout(new GridBagLayout());
        OsmPrimitivRenderer renderer = new OsmPrimitivRenderer();
        // initialize the three lists for uploaded primitives, but don't add
        // them to the dialog yet,  see setUploadedPrimitives()
        //
        lstAdd = new PrimitiveList();
        lstAdd.setCellRenderer(renderer);
        lstAdd.setVisibleRowCount(Math.min(lstAdd.getModel().getSize(), 10));
        spAdd = new JScrollPane(lstAdd);
        lblAdd = new JLabel(tr("Objects to add:"));

        lstUpdate = new PrimitiveList();
        lstUpdate.setCellRenderer(renderer);
        lstUpdate.setVisibleRowCount(Math.min(lstUpdate.getModel().getSize(), 10));
        spUpdate = new JScrollPane(lstUpdate);
        lblUpdate = new JLabel(tr("Objects to modify:"));

        lstDelete = new PrimitiveList();
        lstDelete.setCellRenderer(renderer);
        lstDelete.setVisibleRowCount(Math.min(lstDelete.getModel().getSize(), 10));
        spDelete = new JScrollPane(lstDelete);
        lblDelete = new JLabel(tr("Objects to delete:"));
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
            lblAdd.setText(trn("{0} object to add:", "{0} objects to add:", add.size(),add.size()));
            add(lblAdd, gcLabel);
            y++;
            gcList.gridy = y;
            add(spAdd, gcList);
        }
        if (!update.isEmpty()) {
            y++;
            gcLabel.gridy = y;
            lblUpdate.setText(trn("{0} object to modify:", "{0} objects to modify:", update.size(),update.size()));
            add(lblUpdate, gcLabel);
            y++;
            gcList.gridy = y;
            add(spUpdate, gcList);
        }
        if (!delete.isEmpty()) {
            y++;
            gcLabel.gridy = y;
            lblDelete.setText(trn("{0} object to delete:", "{0} objects to delete:", delete.size(),delete.size()));
            add(lblDelete, gcLabel);
            y++;
            gcList.gridy = y;
            add(spDelete, gcList);
        }

        firePropertyChange(NUM_OBJECTS_TO_UPLOAD_PROP,0, getNumObjectsToUpload());
    }

    public UploadedObjectsSummaryPanel() {
        build();
    }

    /**
     * Replies the number of objects to upload
     *
     * @return the number of objects to upload
     */
    public int  getNumObjectsToUpload() {
        return lstAdd.getModel().getSize()
        + lstUpdate.getModel().getSize()
        + lstDelete.getModel().getSize();
    }

    /**
     * A simple list of OSM primitives.
     *
     */
    static class PrimitiveList extends JList {
        public PrimitiveList() {
            super(new PrimitiveListModel());
        }

        public PrimitiveListModel getPrimitiveListModel() {
            return (PrimitiveListModel)getModel();
        }
    }

    /**
     * A list model for a list of OSM primitives.
     *
     */
    static class PrimitiveListModel extends AbstractListModel{
        private List<OsmPrimitive> primitives;

        public PrimitiveListModel() {
            primitives = new ArrayList<OsmPrimitive>();
        }

        public PrimitiveListModel(List<OsmPrimitive> primitives) {
            setPrimitives(primitives);
        }

        public void setPrimitives(List<OsmPrimitive> primitives) {
            if (primitives == null) {
                this.primitives = new ArrayList<OsmPrimitive>();
            } else {
                this.primitives = primitives;
            }
            fireContentsChanged(this,0,getSize());
        }

        @Override
        public Object getElementAt(int index) {
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
