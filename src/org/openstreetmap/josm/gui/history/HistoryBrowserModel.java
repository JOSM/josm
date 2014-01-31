// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.RelationMemberData;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.UserInfo;
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
import org.openstreetmap.josm.gui.JosmUserIdentityManager;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * This is the model used by the history browser.
 *
 * The model state consists of the following elements:
 * <ul>
 *   <li>the {@link History} of a specific {@link OsmPrimitive}</li>
 *   <li>a dedicated version in this {@link History} called the {@link PointInTimeType#REFERENCE_POINT_IN_TIME}</li>
 *   <li>another version in this {@link History} called the {@link PointInTimeType#CURRENT_POINT_IN_TIME}</li>
 * </ul>
 * {@link HistoryBrowser} always compares the {@link PointInTimeType#REFERENCE_POINT_IN_TIME} with the
 * {@link PointInTimeType#CURRENT_POINT_IN_TIME}.

 * This model provides various {@link TableModel}s for {@link JTable}s used in {@link HistoryBrowser}, for
 * instance:
 * <ul>
 *  <li>{@link #getTagTableModel(PointInTimeType)} replies a {@link TableModel} for the tags of either of
 *   the two selected versions</li>
 *  <li>{@link #getNodeListTableModel(PointInTimeType)} replies a {@link TableModel} for the list of nodes of
 *   the two selected versions (if the current history provides information about a {@link Way}</li>
 *  <li> {@link #getRelationMemberTableModel(PointInTimeType)} replies a {@link TableModel} for the list of relation
 *  members  of the two selected versions (if the current history provides information about a {@link Relation}</li>
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
     * {@link OsmPrimitive} in the current edit layer.
     */
    private HistoryOsmPrimitive latest;

    private VersionTableModel versionTableModel;
    private TagTableModel currentTagTableModel;
    private TagTableModel referenceTagTableModel;
    private DiffTableModel currentRelationMemberTableModel;
    private DiffTableModel referenceRelationMemberTableModel;
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
        currentRelationMemberTableModel = new DiffTableModel();
        referenceRelationMemberTableModel = new DiffTableModel();

        OsmDataLayer editLayer = Main.main.getEditLayer();
        if (editLayer != null) {
            editLayer.data.addDataSetListener(this);
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

        // if latest version from history is higher than a non existing primitive version,
        // that means this version has been redacted and the primitive cannot be used.
        if (history.getLatest().getVersion() > primitive.getVersion())
            return false;

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
            OsmDataLayer editLayer = Main.main.getEditLayer();
            if (editLayer != null) {
                OsmPrimitive p = editLayer.data.getPrimitiveById(history.getId(), history.getType());
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
        initMemberListTableModels();
        setChanged();
        notifyObservers();
        versionTableModel.fireTableDataChanged();
    }

    /**
     * Replies the table model to be used in a {@link JTable} which
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
        if(current.getType() != OsmPrimitiveType.RELATION || reference.getType() != OsmPrimitiveType.RELATION)
            return;

        TwoColumnDiff diff = new TwoColumnDiff(
                ((HistoryRelation)reference).getMembers().toArray(),
                ((HistoryRelation)current).getMembers().toArray());

        referenceRelationMemberTableModel.setRows(diff.referenceDiff);
        currentRelationMemberTableModel.setRows(diff.currentDiff);

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

    public DiffTableModel getRelationMemberTableModel(PointInTimeType pointInTimeType) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(pointInTimeType, "pointInTimeType");
        if (pointInTimeType.equals(PointInTimeType.CURRENT_POINT_IN_TIME))
            return currentRelationMemberTableModel;
        else if (pointInTimeType.equals(PointInTimeType.REFERENCE_POINT_IN_TIME))
            return referenceRelationMemberTableModel;

        // should not happen
        return null;
    }

    /**
     * Sets the {@link HistoryOsmPrimitive} which plays the role of a reference point
     * in time (see {@link PointInTimeType}).
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
     * Sets the {@link HistoryOsmPrimitive} which plays the role of the current point
     * in time (see {@link PointInTimeType}).
     *
     * @param current the reference history primitive. Must not be {@code null}.
     * @throws IllegalArgumentException thrown if reference is {@code null}
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
     * Replies the history OSM primitive for the {@link PointInTimeType#CURRENT_POINT_IN_TIME}
     *
     * @return the history OSM primitive for the {@link PointInTimeType#CURRENT_POINT_IN_TIME} (may be null)
     */
    public HistoryOsmPrimitive getCurrentPointInTime() {
        return getPointInTime(PointInTimeType.CURRENT_POINT_IN_TIME);
    }

    /**
     * Replies the history OSM primitive for the {@link PointInTimeType#REFERENCE_POINT_IN_TIME}
     *
     * @return the history OSM primitive for the {@link PointInTimeType#REFERENCE_POINT_IN_TIME} (may be null)
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
    public final class VersionTableModel extends AbstractTableModel {

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
            case 3: {
                HistoryOsmPrimitive p = getPrimitive(row);
                if (p != null && p.getTimestamp() != null)
                    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(p.getTimestamp());
                return null;
            }
            case 4: {
                HistoryOsmPrimitive p = getPrimitive(row);
                if (p != null) {
                    User user = p.getUser();
                    if (user != null)
                        return user.getName();
                }
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
            OsmDataLayer editLayer = Main.main.getEditLayer();
            if (editLayer == null) return null;
            return editLayer.data.getPrimitiveById(latest.getId(), latest.getType());
        }

        @Override
        public int getColumnCount() {
            return 6;
        }
    }

    /**
     * The table model for the tags of the version at {@link PointInTimeType#REFERENCE_POINT_IN_TIME}
     * or {@link PointInTimeType#CURRENT_POINT_IN_TIME}
     *
     */
    public class TagTableModel extends AbstractTableModel {

        private List<String> keys;
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
        OsmDataLayer editLayer = Main.main.getEditLayer();
        if (editLayer != null) {
            editLayer.data.removeDataSetListener(this);
        }
        MapView.removeLayerChangeListener(this);
    }

    /* ---------------------------------------------------------------------- */
    /* DataSetListener                                                        */
    /* ---------------------------------------------------------------------- */
    @Override
    public void nodeMoved(NodeMovedEvent event) {
        Node node = event.getNode();
        if (!node.isNew() && node.getId() == history.getId()) {
            setLatest(new HistoryPrimitiveBuilder().build(node));
        }
    }

    @Override
    public void primitivesAdded(PrimitivesAddedEvent event) {
        for (OsmPrimitive p: event.getPrimitives()) {
            if (canShowAsLatest(p)) {
                setLatest(new HistoryPrimitiveBuilder().build(p));
            }
        }
    }

    @Override
    public void primitivesRemoved(PrimitivesRemovedEvent event) {
        for (OsmPrimitive p: event.getPrimitives()) {
            if (!p.isNew() && p.getId() == history.getId()) {
                setLatest(null);
            }
        }
    }

    @Override
    public void relationMembersChanged(RelationMembersChangedEvent event) {
        Relation r = event.getRelation();
        if (!r.isNew() && r.getId() == history.getId()) {
            setLatest(new HistoryPrimitiveBuilder().build(r));
        }
    }

    @Override
    public void tagsChanged(TagsChangedEvent event) {
        OsmPrimitive prim = event.getPrimitive();
        if (!prim.isNew() && prim.getId() == history.getId()) {
            setLatest(new HistoryPrimitiveBuilder().build(prim));
        }
    }

    @Override
    public void wayNodesChanged(WayNodesChangedEvent event) {
        Way way = event.getChangedWay();
        if (!way.isNew() && way.getId() == history.getId()) {
            setLatest(new HistoryPrimitiveBuilder().build(way));
        }
    }

    @Override
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

    @Override
    public void otherDatasetChange(AbstractDatasetChangedEvent event) {
        // Irrelevant
    }

    /* ---------------------------------------------------------------------- */
    /* LayerChangeListener                                                    */
    /* ---------------------------------------------------------------------- */
    @Override
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        if (oldLayer instanceof OsmDataLayer) {
            OsmDataLayer l = (OsmDataLayer)oldLayer;
            l.data.removeDataSetListener(this);
        }
        if (!(newLayer instanceof OsmDataLayer)) {
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

    @Override
    public void layerAdded(Layer newLayer) {}
    @Override
    public void layerRemoved(Layer oldLayer) {}

    /**
     * Creates a {@link HistoryOsmPrimitive} from a {@link OsmPrimitive}
     *
     */
    static class HistoryPrimitiveBuilder extends AbstractVisitor {
        private HistoryOsmPrimitive clone;

        @Override
        public void visit(Node n) {
            clone = new HistoryNode(n.getId(), n.getVersion(), n.isVisible(), getCurrentUser(), 0, null, n.getCoor(), false);
            clone.setTags(n.getKeys());
        }

        @Override
        public void visit(Relation r) {
            clone = new HistoryRelation(r.getId(), r.getVersion(), r.isVisible(), getCurrentUser(), 0, null, false);
            clone.setTags(r.getKeys());
            HistoryRelation hr = (HistoryRelation)clone;
            for (RelationMember rm : r.getMembers()) {
                hr.addMember(new RelationMemberData(rm.getRole(), rm.getType(), rm.getUniqueId()));
            }
        }

        @Override
        public void visit(Way w) {
            clone = new HistoryWay(w.getId(), w.getVersion(), w.isVisible(), getCurrentUser(), 0, null, false);
            clone.setTags(w.getKeys());
            for (Node n: w.getNodes()) {
                ((HistoryWay)clone).addNode(n.getUniqueId());
            }
        }

        private User getCurrentUser() {
            UserInfo info = JosmUserIdentityManager.getInstance().getUserInfo();
            return info == null ? User.getAnonymous() : User.createOsmUser(info.getId(), info.getDisplayName());
        }

        public HistoryOsmPrimitive build(OsmPrimitive primitive) {
            primitive.accept(this);
            return clone;
        }
    }
}
