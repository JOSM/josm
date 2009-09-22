// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Observable;
import java.util.logging.Logger;

import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.data.osm.history.HistoryRelation;
import org.openstreetmap.josm.data.osm.history.HistoryWay;

/**
 * This is the model used by the history browser.
 * 
 * The state this model manages consists of the following elements:
 * <ul>
 *   <li>the {@see History} of a specific {@see OsmPrimitive}</li>
 *   <li>a dedicated version in this {@see History} called the {@see PointInTimeType#REFERENCE_POINT_IN_TIME}</li>
 *   <li>another version in this {@see History} called the {@see PointInTimeType#CURRENT_POINT_IN_TIME}</li>
 * <ul>
 * {@see HistoryBrowser} always compares the {@see PointInTimeType#REFERENCE_POINT_IN_TIME} with the
 * {@see PointInTimeType#CURRENT_POINT_IN_TIME}.

 * This model provides various {@see TableModel}s for {@see JTable}s used in {@see HistoryBrowser}, for
 * instance:
 * <ul>
 *  <li>{@see #getTagTableModel(PointInTimeType)} replies a {@see TableModel} for the tags of either of
 *   the two selected versions</li>
 *  <li>{@see #getNodeListTableModel(PointInTimeType)} replies a {@see TableModel} for the list of nodes of
 *   the two selected versions (if the current history provides information about a {@see Way}</li>
 *  <li> {@see #getRelationMemberTableModel(PointInTimeType)} replies a {@see TableModel} for the list of relation
 *  members  of the two selected versions (if the current history provides information about a {@see Relation}</li>
 *  </ul>
 * 
 * @see HistoryBrowser
 */
public class HistoryBrowserModel extends Observable {

    private static Logger logger = Logger.getLogger(HistoryBrowserModel.class.getName());

    /** the history of an OsmPrimitive */
    private History history;
    private HistoryOsmPrimitive reference;
    private HistoryOsmPrimitive current;

    private VersionTableModel versionTableModel;
    private TagTableModel currentTagTableModel;
    private TagTableModel referenceTagTableModel;
    private NodeListTableModel currentNodeListTableModel;
    private NodeListTableModel referenceNodeListTableModel;
    private RelationMemberTableModel currentRelationMemberTableModel;
    private RelationMemberTableModel referenceRelationMemberTableModel;

    public HistoryBrowserModel() {
        versionTableModel = new VersionTableModel();
        currentTagTableModel = new TagTableModel(PointInTimeType.CURRENT_POINT_IN_TIME);
        referenceTagTableModel = new TagTableModel(PointInTimeType.REFERENCE_POINT_IN_TIME);
        currentNodeListTableModel = new NodeListTableModel(PointInTimeType.CURRENT_POINT_IN_TIME);
        referenceNodeListTableModel = new NodeListTableModel(PointInTimeType.REFERENCE_POINT_IN_TIME);
        currentRelationMemberTableModel = new RelationMemberTableModel(PointInTimeType.CURRENT_POINT_IN_TIME);
        referenceRelationMemberTableModel = new RelationMemberTableModel(PointInTimeType.REFERENCE_POINT_IN_TIME);
    }

    public HistoryBrowserModel(History history) {
        this();
        setHistory(history);
    }

    /**
     * replies the history managed by this model
     * @return the history
     */
    public History getHistory() {
        return history;
    }

    /**
     * sets the history to be managed by this model
     * 
     * @param history the history
     * 
     */
    public void setHistory(History history) {
        this.history = history;
        if (history.getNumVersions() > 0) {
            current = history.getEarliest();
            reference = history.getEarliest();
        }
        initTagTableModels();
        fireModelChange();
    }


    protected void fireModelChange() {
        setChanged();
        notifyObservers();
        versionTableModel.fireTableDataChanged();
    }

    /**
     * Replies the table model to be used in a {@see JTable} which
     * shows the list of versions in this history.
     * 
     * @return the table model
     */
    public VersionTableModel getVersionTableModel() {
        return versionTableModel;
    }

    protected void initTagTableModels() {
        currentTagTableModel.initKeyList();
        referenceTagTableModel.initKeyList();
    }

    protected void initNodeListTabeModel() {
        currentNodeListTableModel.fireTableDataChanged();
        referenceNodeListTableModel.fireTableDataChanged();
    }

    protected void initMemberListTableModel() {
        currentRelationMemberTableModel.fireTableDataChanged();
        referenceRelationMemberTableModel.fireTableDataChanged();
    }

    /**
     * replies the tag table model for the respective point in time
     * 
     * @param pointInTimeType the type of the point in time (must not be null)
     * @return the tag table model
     * @exception IllegalArgumentException thrown, if pointInTimeType is null
     */
    public TagTableModel getTagTableModel(PointInTimeType pointInTimeType) throws IllegalArgumentException {
        if (pointInTimeType == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "pointInTimeType"));
        if (pointInTimeType.equals(PointInTimeType.CURRENT_POINT_IN_TIME))
            return currentTagTableModel;
        else if (pointInTimeType.equals(PointInTimeType.REFERENCE_POINT_IN_TIME))
            return referenceTagTableModel;

        // should not happen
        return null;
    }

    public NodeListTableModel getNodeListTableModel(PointInTimeType pointInTimeType) throws IllegalArgumentException {
        if (pointInTimeType == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "pointInTimeType"));
        if (pointInTimeType.equals(PointInTimeType.CURRENT_POINT_IN_TIME))
            return currentNodeListTableModel;
        else if (pointInTimeType.equals(PointInTimeType.REFERENCE_POINT_IN_TIME))
            return referenceNodeListTableModel;

        // should not happen
        return null;
    }

    public RelationMemberTableModel getRelationMemberTableModel(PointInTimeType pointInTimeType) throws IllegalArgumentException {
        if (pointInTimeType == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "pointInTimeType"));
        if (pointInTimeType.equals(PointInTimeType.CURRENT_POINT_IN_TIME))
            return currentRelationMemberTableModel;
        else if (pointInTimeType.equals(PointInTimeType.REFERENCE_POINT_IN_TIME))
            return referenceRelationMemberTableModel;

        // should not happen
        return null;
    }

    public void setReferencePointInTime(HistoryOsmPrimitive reference) throws IllegalArgumentException, IllegalStateException{
        if (reference == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "reference"));
        if (history == null)
            throw new IllegalStateException(tr("History not initialized yet. Failed to set reference primitive."));
        if (reference.getId() != history.getId())
            throw new IllegalArgumentException(tr("Failed to set reference. Reference ID {0} does not match history ID {1}.", reference.getId(),  history.getId()));
        HistoryOsmPrimitive primitive = history.getByVersion(reference.getVersion());
        if (primitive == null)
            throw new IllegalArgumentException(tr("Failed to set reference. Reference version {0} not available in history.", reference.getVersion()));

        this.reference = reference;
        initTagTableModels();
        initNodeListTabeModel();
        initMemberListTableModel();
        setChanged();
        notifyObservers();
    }

    public void setCurrentPointInTime(HistoryOsmPrimitive current) throws IllegalArgumentException, IllegalStateException{
        if (current == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "current"));
        if (history == null)
            throw new IllegalStateException(tr("History not initialized yet. Failed to set current primitive."));
        if (current.getId() != history.getId())
            throw new IllegalArgumentException(tr("Failed to set reference. Reference ID {0} does not match history ID {1}.", current.getId(),  history.getId()));
        HistoryOsmPrimitive primitive = history.getByVersion(current.getVersion());
        if (primitive == null)
            throw new IllegalArgumentException(tr("Failed to set current primitive. Current version {0} not available in history.", current.getVersion()));
        this.current = current;
        initTagTableModels();
        initNodeListTabeModel();
        initMemberListTableModel();
        setChanged();
        notifyObservers();
    }

    /**
     * Replies the history OSM primitive for the {@see PointInTimeType#CURRENT_POINT_IN_TIME}
     * 
     * @return the history OSM primitive for the {@see PointInTimeType#CURRENT_POINT_IN_TIME} (may be null)
     */
    public HistoryOsmPrimitive getCurrentPointInTime() {
        return getPointInTime(PointInTimeType.CURRENT_POINT_IN_TIME);
    }

    /**
     * Replies the history OSM primitive for the {@see PointInTimeType#REFERENCE_POINT_IN_TIME}
     * 
     * @return the history OSM primitive for the {@see PointInTimeType#REFERENCE_POINT_IN_TIME} (may be null)
     */
    public HistoryOsmPrimitive getReferencePointInTime() {
        return getPointInTime(PointInTimeType.REFERENCE_POINT_IN_TIME);
    }

    /**
     * replies the history OSM primitive for a given point in time
     * 
     * @param type the type of the point in time (must not be null)
     * @return the respective primitive. Can be null.
     * @exception IllegalArgumentException thrown, if type is null
     */
    public HistoryOsmPrimitive getPointInTime(PointInTimeType type) throws IllegalArgumentException  {
        if (type == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null.", "type"));
        if (type.equals(PointInTimeType.CURRENT_POINT_IN_TIME))
            return current;
        else if (type.equals(PointInTimeType.REFERENCE_POINT_IN_TIME))
            return reference;

        // should not happen
        return null;
    }

    /**
     * The table model for the list of versions in the current history
     *
     */
    public class VersionTableModel extends DefaultTableModel {

        private VersionTableModel() {
        }

        @Override
        public int getRowCount() {
            if (history == null)
                return 0;
            return history.getNumVersions();
        }

        @Override
        public Object getValueAt(int row, int column) {
            if(history == null)
                return null;
            return history.get(row);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        public void setReferencePointInTime(int row) {
            if (history == null) return;
            if (row < 0 || row > history.getNumVersions()) return;
            HistoryOsmPrimitive reference = history.get(row);
            HistoryBrowserModel.this.setReferencePointInTime(reference);
        }

        public void setCurrentPointInTime(int row) {
            if (history == null) return;
            if (row < 0 || row > history.getNumVersions()) return;
            HistoryOsmPrimitive current = history.get(row);
            HistoryBrowserModel.this.setCurrentPointInTime(current);
        }

        public boolean isReferencePointInTime(int row) {
            if (history == null) return false;
            if (row < 0 || row > history.getNumVersions()) return false;
            HistoryOsmPrimitive p = history.get(row);
            return p.equals(reference);
        }

        public HistoryOsmPrimitive getPrimitive(int row) {
            return history.get(row);
        }
    }


    /**
     * The table model for the tags of the version at {@see PointInTimeType#REFERENCE_POINT_IN_TIME}
     * or {@see PointInTimeType#CURRENT_POINT_IN_TIME}
     * 
     */
    public class TagTableModel extends DefaultTableModel {

        private ArrayList<String> keys;
        private PointInTimeType pointInTimeType;

        protected void initKeyList() {
            HashSet<String> keySet = new HashSet<String>();
            if (current != null) {
                keySet.addAll(current.getTags().keySet());
            }
            if (reference != null) {
                keySet.addAll(reference.getTags().keySet());
            }
            keys = new ArrayList<String>(keySet);
            Collections.sort(keys);
            fireTableDataChanged();
        }

        protected TagTableModel(PointInTimeType type) {
            pointInTimeType = type;
            initKeyList();
        }

        @Override
        public int getRowCount() {
            if (keys == null) return 0;
            return keys.size();
        }

        @Override
        public Object getValueAt(int row, int column) {
            return keys.get(row);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        public boolean hasTag(String key) {
            HistoryOsmPrimitive primitive = getPointInTime(pointInTimeType);
            if (primitive == null)
                return false;
            return primitive.hasTag(key);
        }

        public String getValue(String key) {
            HistoryOsmPrimitive primitive = getPointInTime(pointInTimeType);
            if (primitive == null)
                return null;
            return primitive.get(key);
        }

        public boolean oppositeHasTag(String key) {
            PointInTimeType opposite = pointInTimeType.opposite();
            HistoryOsmPrimitive primitive = getPointInTime(opposite);
            if (primitive == null)
                return false;
            return primitive.hasTag(key);
        }

        public String getOppositeValue(String key) {
            PointInTimeType opposite = pointInTimeType.opposite();
            HistoryOsmPrimitive primitive = getPointInTime(opposite);
            if (primitive == null)
                return null;
            return primitive.get(key);
        }

        public boolean hasSameValueAsOpposite(String key) {
            String value = getValue(key);
            String oppositeValue = getOppositeValue(key);
            if (value == null || oppositeValue == null)
                return false;
            return value.equals(oppositeValue);
        }

        public PointInTimeType getPointInTimeType() {
            return pointInTimeType;
        }

        public boolean isCurrentPointInTime() {
            return pointInTimeType.equals(PointInTimeType.CURRENT_POINT_IN_TIME);
        }

        public boolean isReferencePointInTime() {
            return pointInTimeType.equals(PointInTimeType.REFERENCE_POINT_IN_TIME);
        }
    }

    /**
     * The table model for the nodes of the version at {@see PointInTimeType#REFERENCE_POINT_IN_TIME}
     * or {@see PointInTimeType#CURRENT_POINT_IN_TIME}
     * 
     */
    public class NodeListTableModel extends DefaultTableModel {

        private PointInTimeType pointInTimeType;

        private NodeListTableModel(PointInTimeType pointInTimeType) {
            this.pointInTimeType = pointInTimeType;
        }

        @Override
        public int getRowCount() {
            int n = 0;
            if (current != null && current.getType().equals(OsmPrimitiveType.WAY)) {
                n = ((HistoryWay)current).getNumNodes();
            }
            if (reference != null && reference.getType().equals(OsmPrimitiveType.WAY)) {
                n = Math.max(n,((HistoryWay)reference).getNumNodes());
            }
            return n;
        }

        protected HistoryWay getWay() {
            if (pointInTimeType.equals(PointInTimeType.CURRENT_POINT_IN_TIME)) {
                if (! current.getType().equals(OsmPrimitiveType.WAY))
                    return null;
                return (HistoryWay)current;
            }
            if (pointInTimeType.equals(PointInTimeType.REFERENCE_POINT_IN_TIME)) {
                if (! reference.getType().equals(OsmPrimitiveType.WAY))
                    return null;
                return (HistoryWay)reference;
            }

            // should not happen
            return null;
        }

        protected HistoryWay getOppositeWay() {
            PointInTimeType opposite = pointInTimeType.opposite();
            if (opposite.equals(PointInTimeType.CURRENT_POINT_IN_TIME)) {
                if (! current.getType().equals(OsmPrimitiveType.WAY))
                    return null;
                return (HistoryWay)current;
            }
            if (opposite.equals(PointInTimeType.REFERENCE_POINT_IN_TIME)) {
                if (! reference.getType().equals(OsmPrimitiveType.WAY))
                    return null;
                return (HistoryWay)reference;
            }

            // should not happen
            return null;
        }

        @Override
        public Object getValueAt(int row, int column) {
            HistoryWay way = getWay();
            if (way == null)
                return null;
            if (row >= way.getNumNodes())
                return null;
            return way.getNodes().get(row);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        public boolean isSameInOppositeWay(int row) {
            HistoryWay thisWay = getWay();
            HistoryWay oppositeWay = getOppositeWay();
            if (thisWay == null || oppositeWay == null)
                return false;
            if (row >= oppositeWay.getNumNodes())
                return false;
            return thisWay.getNodeId(row) == oppositeWay.getNodeId(row);
        }

        public boolean isInOppositeWay(int row) {
            HistoryWay thisWay = getWay();
            HistoryWay oppositeWay = getOppositeWay();
            if (thisWay == null || oppositeWay == null)
                return false;
            return oppositeWay.getNodes().contains(thisWay.getNodeId(row));
        }
    }

    /**
     * The table model for the relation members of the version at {@see PointInTimeType#REFERENCE_POINT_IN_TIME}
     * or {@see PointInTimeType#CURRENT_POINT_IN_TIME}
     * 
     */

    public class RelationMemberTableModel extends DefaultTableModel {

        private PointInTimeType pointInTimeType;

        private RelationMemberTableModel(PointInTimeType pointInTimeType) {
            this.pointInTimeType = pointInTimeType;
        }

        @Override
        public int getRowCount() {
            int n = 0;
            if (current != null && current.getType().equals(OsmPrimitiveType.RELATION)) {
                n = ((HistoryRelation)current).getNumMembers();
            }
            if (reference != null && reference.getType().equals(OsmPrimitiveType.RELATION)) {
                n = Math.max(n,((HistoryRelation)reference).getNumMembers());
            }
            return n;
        }

        protected HistoryRelation getRelation() {
            if (pointInTimeType.equals(PointInTimeType.CURRENT_POINT_IN_TIME)) {
                if (! current.getType().equals(OsmPrimitiveType.RELATION))
                    return null;
                return (HistoryRelation)current;
            }
            if (pointInTimeType.equals(PointInTimeType.REFERENCE_POINT_IN_TIME)) {
                if (! reference.getType().equals(OsmPrimitiveType.RELATION))
                    return null;
                return (HistoryRelation)reference;
            }

            // should not happen
            return null;
        }

        protected HistoryRelation getOppositeRelation() {
            PointInTimeType opposite = pointInTimeType.opposite();
            if (opposite.equals(PointInTimeType.CURRENT_POINT_IN_TIME)) {
                if (! current.getType().equals(OsmPrimitiveType.RELATION))
                    return null;
                return (HistoryRelation)current;
            }
            if (opposite.equals(PointInTimeType.REFERENCE_POINT_IN_TIME)) {
                if (! reference.getType().equals(OsmPrimitiveType.RELATION))
                    return null;
                return (HistoryRelation)reference;
            }

            // should not happen
            return null;
        }

        @Override
        public Object getValueAt(int row, int column) {
            HistoryRelation relation = getRelation();
            if (relation == null)
                return null;
            if (row >= relation.getNumMembers())
                return null;
            return relation.getMembers().get(row);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        public boolean isSameInOppositeWay(int row) {
            HistoryRelation thisRelation = getRelation();
            HistoryRelation oppositeRelation = getOppositeRelation();
            if (thisRelation == null || oppositeRelation == null)
                return false;
            if (row >= oppositeRelation.getNumMembers())
                return false;
            return
            thisRelation.getMembers().get(row).getPrimitiveId() == oppositeRelation.getMembers().get(row).getPrimitiveId()
            &&  thisRelation.getMembers().get(row).getRole().equals(oppositeRelation.getMembers().get(row).getRole());
        }

        public boolean isInOppositeWay(int row) {
            HistoryRelation thisRelation = getRelation();
            HistoryRelation oppositeRelation = getOppositeRelation();
            if (thisRelation == null || oppositeRelation == null)
                return false;
            return oppositeRelation.getMembers().contains(thisRelation.getMembers().get(row));
        }
    }
}
