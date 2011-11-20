// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;

import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.event.AbstractDatasetChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataChangedEvent;
import org.openstreetmap.josm.data.osm.event.DataSetListener;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesAddedEvent;
import org.openstreetmap.josm.data.osm.event.PrimitivesRemovedEvent;
import org.openstreetmap.josm.data.osm.event.RelationMembersChangedEvent;
import org.openstreetmap.josm.data.osm.event.TagsChangedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.data.osm.history.History;
import org.openstreetmap.josm.data.osm.history.HistoryNode;
import org.openstreetmap.josm.data.osm.history.HistoryOsmPrimitive;
import org.openstreetmap.josm.data.osm.history.HistoryRelation;
import org.openstreetmap.josm.data.osm.history.HistoryWay;
import org.openstreetmap.josm.data.osm.visitor.AbstractVisitor;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.dialogs.UserListDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Diff;

/**
 * This is the model used by the history browser.
 *
 * The model state consists of the following elements:
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
public class HistoryBrowserModel extends Observable implements LayerChangeListener, DataSetListener {
    /** the history of an OsmPrimitive */
    private History history;
    private HistoryOsmPrimitive reference;
    private HistoryOsmPrimitive current;
    /**
     * latest isn't a reference of history. It's a clone of the currently edited
     * {@see OsmPrimitive} in the current edit layer.
     */
    private HistoryOsmPrimitive latest;

    private VersionTableModel versionTableModel;
    private TagTableModel currentTagTableModel;
    private TagTableModel referenceTagTableModel;
    private RelationMemberTableModel currentRelationMemberTableModel;
    private RelationMemberTableModel referenceRelationMemberTableModel;
    private DiffTableModel referenceNodeListTableModel;
    private DiffTableModel currentNodeListTableModel;

    /**
     * constructor
     */
    public HistoryBrowserModel() {
        versionTableModel = new VersionTableModel();
        currentTagTableModel = new TagTableModel(PointInTimeType.CURRENT_POINT_IN_TIME);
        referenceTagTableModel = new TagTableModel(PointInTimeType.REFERENCE_POINT_IN_TIME);
        referenceNodeListTableModel = new DiffTableModel();
        currentNodeListTableModel = new DiffTableModel();
        currentRelationMemberTableModel = new RelationMemberTableModel(PointInTimeType.CURRENT_POINT_IN_TIME);
        referenceRelationMemberTableModel = new RelationMemberTableModel(PointInTimeType.REFERENCE_POINT_IN_TIME);

        if (getEditLayer() != null) {
            getEditLayer().data.addDataSetListener(this);
        }
        MapView.addLayerChangeListener(this);
    }

    /**
     * Creates a new history browser model for a given history.
     *
     * @param history the history. Must not be null.
     * @throws IllegalArgumentException thrown if history is null
     */
    public HistoryBrowserModel(History history) {
        this();
        CheckParameterUtil.ensureParameterNotNull(history, "history");
        setHistory(history);
    }

    /**
     * Replies the current edit layer; null, if there isn't a current edit layer
     * of type {@see OsmDataLayer}.
     *
     * @return the current edit layer
     */
    protected OsmDataLayer getEditLayer() {
        try {
            return Main.map.mapView.getEditLayer();
        } catch(NullPointerException e) {
            return null;
        }
    }

    /**
     * replies the history managed by this model
     * @return the history
     */
    public History getHistory() {
        return history;
    }

    protected boolean hasNewNodes(Way way) {
        for (Node n: way.getNodes()) {
            if (n.isNew()) return true;
        }
        return false;
    }
    protected boolean canShowAsLatest(OsmPrimitive primitive) {
        if (primitive == null) return false;
        if (primitive.isNew() || !primitive.isUsable()) return false;

        //try creating a history primitive. if that fails, the primitive cannot be used.
        try {
            HistoryOsmPrimitive.forOsmPrimitive(primitive);
        } catch (Exception ign) {
            return false;
        }

        if (history == null) return false;
        // only show latest of the same version if it is modified
        if (history.getByVersion(primitive.getVersion()) != null)
            return primitive.isModified();

        // latest has a higher version than one of the primitives
        // in the history (probably because the history got out of sync
        // with uploaded data) -> show the primitive as latest
        return true;
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
            HistoryOsmPrimitive newLatest = null;
            if (getEditLayer() != null) {
                OsmPrimitive p = getEditLayer().data.getPrimitiveById(history.getId(), history.getType());
                if (canShowAsLatest(p)) {
                    newLatest = new HistoryPrimitiveBuilder().build(p);
                }
            }
            if (newLatest == null) {
                current = history.getLatest();
                int prevIndex = history.getNumVersions() - 2;
                reference = prevIndex < 0 ? history.getEarliest() : history.get(prevIndex);
            } else {
                reference = history.getLatest();
                current = newLatest;
            }
            setLatest(newLatest);
        }
        initTagTableModels();
        fireModelChange();
    }

    protected void fireModelChange() {
        initNodeListTableModels();
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

    /**
     * Should be called everytime either reference of current changes to update the diff.
     * TODO: Maybe rename to reflect this? eg. updateNodeListTableModels
     */
    protected void initNodeListTableModels() {

        if(current.getType() != OsmPrimitiveType.WAY || reference.getType() != OsmPrimitiveType.WAY)
            return;
        TwoColumnDiff diff = new TwoColumnDiff(
                ((HistoryWay)reference).getNodes().toArray(),
                ((HistoryWay)current).getNodes().toArray());
        referenceNodeListTableModel.setRows(diff.referenceDiff);
        currentNodeListTableModel.setRows(diff.currentDiff);

        referenceNodeListTableModel.fireTableDataChanged();
        currentNodeListTableModel.fireTableDataChanged();
    }

    protected void initMemberListTableModels() {
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
        CheckParameterUtil.ensureParameterNotNull(pointInTimeType, "pointInTimeType");
        if (pointInTimeType.equals(PointInTimeType.CURRENT_POINT_IN_TIME))
            return currentTagTableModel;
        else if (pointInTimeType.equals(PointInTimeType.REFERENCE_POINT_IN_TIME))
            return referenceTagTableModel;

        // should not happen
        return null;
    }

    public DiffTableModel getNodeListTableModel(PointInTimeType pointInTimeType) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(pointInTimeType, "pointInTimeType");
        if (pointInTimeType.equals(PointInTimeType.CURRENT_POINT_IN_TIME))
            return currentNodeListTableModel;
        else if (pointInTimeType.equals(PointInTimeType.REFERENCE_POINT_IN_TIME))
            return referenceNodeListTableModel;

        // should not happen
        return null;
    }

    public RelationMemberTableModel getRelationMemberTableModel(PointInTimeType pointInTimeType) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(pointInTimeType, "pointInTimeType");
        if (pointInTimeType.equals(PointInTimeType.CURRENT_POINT_IN_TIME))
            return currentRelationMemberTableModel;
        else if (pointInTimeType.equals(PointInTimeType.REFERENCE_POINT_IN_TIME))
            return referenceRelationMemberTableModel;

        // should not happen
        return null;
    }

    /**
     * Sets the {@see HistoryOsmPrimitive} which plays the role of a reference point
     * in time (see {@see PointInTimeType}).
     *
     * @param reference the reference history primitive. Must not be null.
     * @throws IllegalArgumentException thrown if reference is null
     * @throws IllegalStateException thrown if this model isn't a assigned a history yet
     * @throws IllegalArgumentException if reference isn't an history primitive for the history managed by this mode
     *
     * @see #setHistory(History)
     * @see PointInTimeType
     */
    public void setReferencePointInTime(HistoryOsmPrimitive reference) throws IllegalArgumentException, IllegalStateException{
        CheckParameterUtil.ensureParameterNotNull(reference, "reference");
        if (history == null)
            throw new IllegalStateException(tr("History not initialized yet. Failed to set reference primitive."));
        if (reference.getId() != history.getId())
            throw new IllegalArgumentException(tr("Failed to set reference. Reference ID {0} does not match history ID {1}.", reference.getId(),  history.getId()));
        HistoryOsmPrimitive primitive = history.getByVersion(reference.getVersion());
        if (primitive == null)
            throw new IllegalArgumentException(tr("Failed to set reference. Reference version {0} not available in history.", reference.getVersion()));

        this.reference = reference;
        initTagTableModels();
        initNodeListTableModels();
        initMemberListTableModels();
        setChanged();
        notifyObservers();
    }

    /**
     * Sets the {@see HistoryOsmPrimitive} which plays the role of the current point
     * in time (see {@see PointInTimeType}).
     *
     * @param reference the reference history primitive. Must not be null.
     * @throws IllegalArgumentException thrown if reference is null
     * @throws IllegalStateException thrown if this model isn't a assigned a history yet
     * @throws IllegalArgumentException if reference isn't an history primitive for the history managed by this mode
     *
     * @see #setHistory(History)
     * @see PointInTimeType
     */
    public void setCurrentPointInTime(HistoryOsmPrimitive current) throws IllegalArgumentException, IllegalStateException{
        CheckParameterUtil.ensureParameterNotNull(current, "current");
        if (history == null)
            throw new IllegalStateException(tr("History not initialized yet. Failed to set current primitive."));
        if (current.getId() != history.getId())
            throw new IllegalArgumentException(tr("Failed to set reference. Reference ID {0} does not match history ID {1}.", current.getId(),  history.getId()));
        HistoryOsmPrimitive primitive = history.getByVersion(current.getVersion());
        if (primitive == null)
            throw new IllegalArgumentException(tr("Failed to set current primitive. Current version {0} not available in history.", current.getVersion()));
        this.current = current;
        initTagTableModels();
        initNodeListTableModels();
        initMemberListTableModels();
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
        CheckParameterUtil.ensureParameterNotNull(type, "type");
        if (type.equals(PointInTimeType.CURRENT_POINT_IN_TIME))
            return current;
        else if (type.equals(PointInTimeType.REFERENCE_POINT_IN_TIME))
            return reference;

        // should not happen
        return null;
    }

    /**
     * Returns true if <code>primitive</code> is the latest primitive
     * representing the version currently edited in the current data
     * layer.
     *
     * @param primitive the primitive to check
     * @return true if <code>primitive</code> is the latest primitive
     */
    public boolean isLatest(HistoryOsmPrimitive primitive) {
        if (primitive == null) return false;
        return primitive == latest;
    }

    /**
     * The table model for the list of versions in the current history
     *
     */
    public class VersionTableModel extends AbstractTableModel {

        private VersionTableModel() {
        }

        @Override
        public int getRowCount() {
            if (history == null)
                return 0;
            int ret = history.getNumVersions();
            if (latest != null) {
                ret++;
            }
            return ret;
        }

        @Override
        public Object getValueAt(int row, int column) {
            switch (column) {
            case 0:
                return Long.toString(getPrimitive(row).getVersion());
            case 1:
                return isReferencePointInTime(row);
            case 2:
                return isCurrentPointInTime(row);
            case 3:
                long uId = getPrimitive(row).getUid();
                User user = User.getById(uId);
                int status;
                if (user == null) {
                    status = User.STATUS_UNKNOWN;
                } else {
                    status = user.getRelicensingStatus();
                }
                return UserListDialog.getRelicensingStatusIcon(status);
            case 4: {
                    HistoryOsmPrimitive p = getPrimitive(row);
                    if (p != null)
                        return new SimpleDateFormat().format(p.getTimestamp());
                    return null;
                }
            case 5: {
                    HistoryOsmPrimitive p = getPrimitive(row);
                    if (p != null)
                        return "<html>"+p.getUser() + " <font color=gray>(" + p.getUid() + ")</font></html>";
                    return null;
                }
            }
            return null;
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            if (!((Boolean) aValue)) return;
            switch (column) {
            case 1:
                setReferencePointInTime(row);
                break;
            case 2:
                setCurrentPointInTime(row);
                break;
            default:
                return;
            }
            fireTableDataChanged();
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column >= 1 && column <= 2;
        }

        public void setReferencePointInTime(int row) {
            if (history == null) return;
            if (row == history.getNumVersions()) {
                if (latest != null) {
                    HistoryBrowserModel.this.setReferencePointInTime(latest);
                }
                return;
            }
            if (row < 0 || row > history.getNumVersions()) return;
            HistoryOsmPrimitive reference = history.get(row);
            HistoryBrowserModel.this.setReferencePointInTime(reference);
        }

        public void setCurrentPointInTime(int row) {
            if (history == null) return;
            if (row == history.getNumVersions()) {
                if (latest != null) {
                    HistoryBrowserModel.this.setCurrentPointInTime(latest);
                }
                return;
            }
            if (row < 0 || row > history.getNumVersions()) return;
            HistoryOsmPrimitive current = history.get(row);
            HistoryBrowserModel.this.setCurrentPointInTime(current);
        }

        public boolean isReferencePointInTime(int row) {
            if (history == null) return false;
            if (row == history.getNumVersions())
                return latest == reference;
            if (row < 0 || row > history.getNumVersions()) return false;
            HistoryOsmPrimitive p = history.get(row);
            return p == reference;
        }

        public boolean isCurrentPointInTime(int row) {
            if (history == null) return false;
            if (row == history.getNumVersions())
                return latest == current;
            if (row < 0 || row > history.getNumVersions()) return false;
            HistoryOsmPrimitive p = history.get(row);
            return p == current;
        }

        public HistoryOsmPrimitive getPrimitive(int row) {
            if (history == null)
                return null;
            return isLatest(row) ? latest : history.get(row);
        }

        public boolean isLatest(int row) {
            return row >= history.getNumVersions();
        }

        public OsmPrimitive getLatest() {
            if (latest == null) return null;
            if (getEditLayer() == null) return null;
            OsmPrimitive p = getEditLayer().data.getPrimitiveById(latest.getId(), latest.getType());
            return p;
        }

        @Override
        public int getColumnCount() {
            return 6;
        }
    }

    /**
     * The table model for the tags of the version at {@see PointInTimeType#REFERENCE_POINT_IN_TIME}
     * or {@see PointInTimeType#CURRENT_POINT_IN_TIME}
     *
     */
    public class TagTableModel extends AbstractTableModel {

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

        @Override
        public int getColumnCount() {
            return 1;
        }
    }

    /**
     * The table model for the relation members of the version at {@see PointInTimeType#REFERENCE_POINT_IN_TIME}
     * or {@see PointInTimeType#CURRENT_POINT_IN_TIME}
     *
     */

    public class RelationMemberTableModel extends AbstractTableModel {

        private PointInTimeType pointInTimeType;

        private RelationMemberTableModel(PointInTimeType pointInTimeType) {
            this.pointInTimeType = pointInTimeType;
        }

        @Override
        public int getRowCount() {
            // Match the size of the opposite table so comparison is less confusing.
            // (scroll bars lines up properly, etc.)
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
            if (row >= relation.getNumMembers()) // see getRowCount
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

        @Override
        public int getColumnCount() {
            return 1;
        }
    }

    protected void setLatest(HistoryOsmPrimitive latest) {
        if (latest == null) {
            if (this.current == this.latest) {
                this.current = history.getLatest();
            }
            if (this.reference == this.latest) {
                this.current = history.getLatest();
            }
            this.latest = null;
        } else {
            if (this.current == this.latest) {
                this.current = latest;
            }
            if (this.reference == this.latest) {
                this.reference = latest;
            }
            this.latest = latest;
        }
        fireModelChange();
    }

    /**
     * Removes this model as listener for data change and layer change
     * events.
     *
     */
    public void unlinkAsListener() {
        if (getEditLayer() != null) {
            getEditLayer().data.removeDataSetListener(this);
        }
        MapView.removeLayerChangeListener(this);
    }

    /* ---------------------------------------------------------------------- */
    /* DataSetListener                                                        */
    /* ---------------------------------------------------------------------- */
    public void nodeMoved(NodeMovedEvent event) {
        Node node = event.getNode();
        if (!node.isNew() && node.getId() == history.getId()) {
            setLatest(new HistoryPrimitiveBuilder().build(node));
        }
    }

    public void primitivesAdded(PrimitivesAddedEvent event) {
        for (OsmPrimitive p: event.getPrimitives()) {
            if (canShowAsLatest(p)) {
                setLatest(new HistoryPrimitiveBuilder().build(p));
            }
        }
    }

    public void primitivesRemoved(PrimitivesRemovedEvent event) {
        for (OsmPrimitive p: event.getPrimitives()) {
            if (!p.isNew() && p.getId() == history.getId()) {
                setLatest(null);
            }
        }
    }

    public void relationMembersChanged(RelationMembersChangedEvent event) {
        Relation r = event.getRelation();
        if (!r.isNew() && r.getId() == history.getId()) {
            setLatest(new HistoryPrimitiveBuilder().build(r));
        }
    }

    public void tagsChanged(TagsChangedEvent event) {
        OsmPrimitive prim = event.getPrimitive();
        if (!prim.isNew() && prim.getId() == history.getId()) {
            setLatest(new HistoryPrimitiveBuilder().build(prim));
        }
    }

    public void wayNodesChanged(WayNodesChangedEvent event) {
        Way way = event.getChangedWay();
        if (!way.isNew() && way.getId() == history.getId()) {
            setLatest(new HistoryPrimitiveBuilder().build(way));
        }
    }

    public void dataChanged(DataChangedEvent event) {
        OsmPrimitive primitive = event.getDataset().getPrimitiveById(history.getId(), history.getType());
        HistoryOsmPrimitive latest;
        if (canShowAsLatest(primitive)) {
            latest = new HistoryPrimitiveBuilder().build(primitive);
        } else {
            latest = null;
        }
        setLatest(latest);
        fireModelChange();
    }

    public void otherDatasetChange(AbstractDatasetChangedEvent event) {
        // Irrelevant
    }

    /* ---------------------------------------------------------------------- */
    /* LayerChangeListener                                                    */
    /* ---------------------------------------------------------------------- */
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        if (oldLayer != null && oldLayer instanceof OsmDataLayer) {
            OsmDataLayer l = (OsmDataLayer)oldLayer;
            l.data.removeDataSetListener(this);
        }
        if (newLayer == null || ! (newLayer instanceof OsmDataLayer)) {
            latest = null;
            fireModelChange();
            return;
        }
        OsmDataLayer l = (OsmDataLayer)newLayer;
        l.data.addDataSetListener(this);
        OsmPrimitive primitive = l.data.getPrimitiveById(history.getId(), history.getType());
        HistoryOsmPrimitive latest;
        if (canShowAsLatest(primitive)) {
            latest = new HistoryPrimitiveBuilder().build(primitive);
        } else {
            latest = null;
        }
        setLatest(latest);
        fireModelChange();
    }

    public void layerAdded(Layer newLayer) {}
    public void layerRemoved(Layer oldLayer) {}

    /**
     * Creates a {@see HistoryOsmPrimitive} from a {@see OsmPrimitive}
     *
     */
    static class HistoryPrimitiveBuilder extends AbstractVisitor {
        private HistoryOsmPrimitive clone;

        private String getUserName(OsmPrimitive primitive) {
            return primitive.getUser() == null?null:primitive.getUser().getName();
        }

        private long getUserId(OsmPrimitive primitive) {
            return primitive.getUser() == null?0:primitive.getUser().getId();
        }

        public void visit(Node n) {
            clone = new HistoryNode(n.getId(), n.getVersion(), n.isVisible(), getUserName(n), getUserId(n), 0, n.getTimestamp(), n.getCoor());
            clone.setTags(n.getKeys());
        }

        public void visit(Relation r) {
            clone = new HistoryRelation(r.getId(), r.getVersion(), r.isVisible(), getUserName(r), getUserId(r), 0, r.getTimestamp());
            clone.setTags(r.getKeys());
            HistoryRelation hr = (HistoryRelation)clone;
            for (RelationMember rm : r.getMembers()) {
                hr.addMember(new org.openstreetmap.josm.data.osm.history.RelationMember(rm.getRole(), rm.getType(), rm.getUniqueId()));
            }
        }

        public void visit(Way w) {
            clone = new HistoryWay(w.getId(), w.getVersion(), w.isVisible(), getUserName(w), getUserId(w), 0, w.getTimestamp());
            clone.setTags(w.getKeys());
            for (Node n: w.getNodes()) {
                ((HistoryWay)clone).addNode(n.getUniqueId());
            }
        }

        public HistoryOsmPrimitive build(OsmPrimitive primitive) {
            primitive.visit(this);
            return clone;
        }
    }
}

/**
 * Simple model storing "diff cells" in a list. Could probably have used a DefaultTableModel instead..
 *
 * {@see NodeListDiffTableCellRenderer}
 */
class DiffTableModel extends AbstractTableModel {
    private List<TwoColumnDiff.Item> rows;

    public void setRows(List<TwoColumnDiff.Item> rows) {
        this.rows = rows;
    }

    public DiffTableModel(List<TwoColumnDiff.Item> rows) {
        this.rows = rows;
    }
    public DiffTableModel() {
        this.rows = new ArrayList<TwoColumnDiff.Item>();
    }
    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public TwoColumnDiff.Item getValueAt(int rowIndex, int columnIndex) {
        return rows.get(rowIndex);
    }
}


/// Feel free to move me somewhere else. Maybe a bit specific for josm.tools?
/**
 * Produces a "two column diff" of two lists. (same as diff -y)
 *
 * Each list is annotated with the changes relative to the other, and "empty" cells are inserted so the lists are comparable item by item.
 *
 * diff on [1 2 3 4] [1 a 4 5] yields:
 *
 * item(SAME, 1)    item(SAME, 1)
 * item(CHANGED, 2) item(CHANGED, 2)
 * item(DELETED, 3) item(EMPTY)
 * item(SAME, 4)    item(SAME, 4)
 * item(EMPTY)      item(INSERTED, 5)
 *
 * @author olejorgenb
 */
class TwoColumnDiff {
    public static class Item {
        public static final int INSERTED = 1;
        public static final int DELETED = 2;
        public static final int CHANGED = 3;
        public static final int SAME = 4;
        public static final int EMPTY = 5; // value should be null
        public Item(int state, Object value) {
            this.state = state;
            this.value = state == EMPTY ? null : value;
        }

        public final Object value;
        public final int state;
    }

    public ArrayList<Item> referenceDiff;
    public ArrayList<Item> currentDiff;
    Object[] reference;
    Object[] current;

    /**
     * The arguments will _not_ be modified
     */
    public TwoColumnDiff(Object[] reference, Object[] current) {
        this.reference = reference;
        this.current = current;
        referenceDiff = new ArrayList<Item>();
        currentDiff = new ArrayList<Item>();
        diff();
    }
    private void diff() {
        Diff diff = new Diff(reference, current);
        Diff.change script = diff.diff_2(false);
        twoColumnDiffFromScript(script, reference, current);
    }

    /**
     * The result from the diff algorithm is a "script" (a compressed description of the changes)
     * This method expands this script into a full two column description.
     */
    private void twoColumnDiffFromScript(Diff.change script, Object[] a, Object[] b) {
        int ia = 0;
        int ib = 0;

        while(script != null) {
            int deleted = script.deleted;
            int inserted = script.inserted;
            while(ia < script.line0 && ib < script.line1){
                // System.out.println(" "+a[ia] + "\t "+b[ib]);
                Item cell = new Item(Item.SAME, a[ia]);
                referenceDiff.add(cell);
                currentDiff.add(cell);
                ia++;
                ib++;
            }

            while(inserted > 0 || deleted > 0) {
                if(inserted > 0 && deleted > 0) {
                    // System.out.println("="+a[ia] + "\t="+b[ib]);
                    referenceDiff.add(new Item(Item.CHANGED, a[ia++]));
                    currentDiff.add(new Item(Item.CHANGED, b[ib++]));
                } else if(inserted > 0) {
                    // System.out.println("\t+" + b[ib]);
                    referenceDiff.add(new Item(Item.EMPTY, null));
                    currentDiff.add(new Item(Item.INSERTED, b[ib++]));
                } else if(deleted > 0) {
                    // System.out.println("-"+a[ia]);
                    referenceDiff.add(new Item(Item.DELETED, a[ia++]));
                    currentDiff.add(new Item(Item.EMPTY, null));
                }
                inserted--;
                deleted--;
            }
            script = script.link;
        }
        while(ia < a.length && ib < b.length) {
            // System.out.println((ia < a.length ? " "+a[ia]+"\t" : "\t") + (ib < b.length ? " "+b[ib] : ""));
            referenceDiff.add(new Item(Item.SAME, a[ia++]));
            currentDiff.add(new Item(Item.SAME, b[ib++]));
        }
    }
}
