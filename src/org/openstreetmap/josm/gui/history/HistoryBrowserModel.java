// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.HashSet;
import java.util.Set;

import javax.swing.JTable;
import javax.swing.table.TableModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.UserIdentityManager;
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
import org.openstreetmap.josm.data.osm.visitor.OsmPrimitiveVisitor;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.ChangeNotifier;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;

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
public class HistoryBrowserModel extends ChangeNotifier implements ActiveLayerChangeListener, DataSetListener {
    /** the history of an OsmPrimitive */
    private History history;
    private HistoryOsmPrimitive reference;
    private HistoryOsmPrimitive current;
    /**
     * latest isn't a reference of history. It's a clone of the currently edited
     * {@link OsmPrimitive} in the current edit layer.
     */
    private HistoryOsmPrimitive latest;

    private final VersionTableModel versionTableModel;
    private final TagTableModel currentTagTableModel;
    private final TagTableModel referenceTagTableModel;
    private final DiffTableModel currentRelationMemberTableModel;
    private final DiffTableModel referenceRelationMemberTableModel;
    private final DiffTableModel referenceNodeListTableModel;
    private final DiffTableModel currentNodeListTableModel;

    /**
     * constructor
     */
    public HistoryBrowserModel() {
        versionTableModel = new VersionTableModel(this);
        currentTagTableModel = new TagTableModel(this, PointInTimeType.CURRENT_POINT_IN_TIME);
        referenceTagTableModel = new TagTableModel(this, PointInTimeType.REFERENCE_POINT_IN_TIME);
        referenceNodeListTableModel = new DiffTableModel();
        currentNodeListTableModel = new DiffTableModel();
        currentRelationMemberTableModel = new DiffTableModel();
        referenceRelationMemberTableModel = new DiffTableModel();

        if (Main.main != null) {
            OsmDataLayer editLayer = MainApplication.getLayerManager().getEditLayer();
            if (editLayer != null) {
                editLayer.data.addDataSetListener(this);
            }
        }
        MainApplication.getLayerManager().addActiveLayerChangeListener(this);
    }

    /**
     * Creates a new history browser model for a given history.
     *
     * @param history the history. Must not be null.
     * @throws IllegalArgumentException if history is null
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

    private boolean canShowAsLatest(OsmPrimitive primitive) {
        if (primitive == null)
            return false;
        if (primitive.isNew() || !primitive.isUsable())
            return false;

        //try creating a history primitive. if that fails, the primitive cannot be used.
        try {
            HistoryOsmPrimitive.forOsmPrimitive(primitive);
        } catch (IllegalArgumentException ign) {
            Logging.trace(ign);
            return false;
        }

        if (history == null)
            return false;
        // only show latest of the same version if it is modified
        if (history.getByVersion(primitive.getVersion()) != null)
            return primitive.isModified();

        // if latest version from history is higher than a non existing primitive version,
        // that means this version has been redacted and the primitive cannot be used.
        return history.getLatest().getVersion() <= primitive.getVersion();

        // latest has a higher version than one of the primitives
        // in the history (probably because the history got out of sync
        // with uploaded data) -> show the primitive as latest
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
            OsmDataLayer editLayer = MainApplication.getLayerManager().getEditLayer();
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

    private void fireModelChange() {
        initNodeListTableModels();
        initMemberListTableModels();
        fireStateChanged();
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

    private void initTagTableModels() {
        currentTagTableModel.initKeyList();
        referenceTagTableModel.initKeyList();
    }

    /**
     * Should be called everytime either reference of current changes to update the diff.
     * TODO: Maybe rename to reflect this? eg. updateNodeListTableModels
     */
    private void initNodeListTableModels() {
        if (current == null || current.getType() != OsmPrimitiveType.WAY
         || reference == null || reference.getType() != OsmPrimitiveType.WAY)
            return;
        TwoColumnDiff diff = new TwoColumnDiff(
                ((HistoryWay) reference).getNodes().toArray(),
                ((HistoryWay) current).getNodes().toArray());
        referenceNodeListTableModel.setRows(diff.referenceDiff, diff.referenceReversed);
        currentNodeListTableModel.setRows(diff.currentDiff, false);
    }

    private void initMemberListTableModels() {
        if (current == null || current.getType() != OsmPrimitiveType.RELATION
         || reference == null || reference.getType() != OsmPrimitiveType.RELATION)
            return;
        TwoColumnDiff diff = new TwoColumnDiff(
                ((HistoryRelation) reference).getMembers().toArray(),
                ((HistoryRelation) current).getMembers().toArray());
        referenceRelationMemberTableModel.setRows(diff.referenceDiff, diff.referenceReversed);
        currentRelationMemberTableModel.setRows(diff.currentDiff, false);
    }

    /**
     * Replies the tag table model for the respective point in time.
     *
     * @param pointInTimeType the type of the point in time (must not be null)
     * @return the tag table model
     * @throws IllegalArgumentException if pointInTimeType is null
     */
    public TagTableModel getTagTableModel(PointInTimeType pointInTimeType) {
        CheckParameterUtil.ensureParameterNotNull(pointInTimeType, "pointInTimeType");
        if (pointInTimeType.equals(PointInTimeType.CURRENT_POINT_IN_TIME))
            return currentTagTableModel;
        else // REFERENCE_POINT_IN_TIME
            return referenceTagTableModel;
    }

    /**
     * Replies the node list table model for the respective point in time.
     *
     * @param pointInTimeType the type of the point in time (must not be null)
     * @return the node list table model
     * @throws IllegalArgumentException if pointInTimeType is null
     */
    public DiffTableModel getNodeListTableModel(PointInTimeType pointInTimeType) {
        CheckParameterUtil.ensureParameterNotNull(pointInTimeType, "pointInTimeType");
        if (pointInTimeType.equals(PointInTimeType.CURRENT_POINT_IN_TIME))
            return currentNodeListTableModel;
        else // REFERENCE_POINT_IN_TIME
            return referenceNodeListTableModel;
    }

    /**
     * Replies the relation member table model for the respective point in time.
     *
     * @param pointInTimeType the type of the point in time (must not be null)
     * @return the relation member table model
     * @throws IllegalArgumentException if pointInTimeType is null
     */
    public DiffTableModel getRelationMemberTableModel(PointInTimeType pointInTimeType) {
        CheckParameterUtil.ensureParameterNotNull(pointInTimeType, "pointInTimeType");
        if (pointInTimeType.equals(PointInTimeType.CURRENT_POINT_IN_TIME))
            return currentRelationMemberTableModel;
        else // REFERENCE_POINT_IN_TIME
            return referenceRelationMemberTableModel;
    }

    /**
     * Sets the {@link HistoryOsmPrimitive} which plays the role of a reference point
     * in time (see {@link PointInTimeType}).
     *
     * @param reference the reference history primitive. Must not be null.
     * @throws IllegalArgumentException if reference is null
     * @throws IllegalStateException if this model isn't a assigned a history yet
     * @throws IllegalArgumentException if reference isn't an history primitive for the history managed by this mode
     *
     * @see #setHistory(History)
     * @see PointInTimeType
     */
    public void setReferencePointInTime(HistoryOsmPrimitive reference) {
        CheckParameterUtil.ensureParameterNotNull(reference, "reference");
        if (history == null)
            throw new IllegalStateException(tr("History not initialized yet. Failed to set reference primitive."));
        if (reference.getId() != history.getId())
            throw new IllegalArgumentException(
                    tr("Failed to set reference. Reference ID {0} does not match history ID {1}.", reference.getId(), history.getId()));
        if (history.getByVersion(reference.getVersion()) == null)
            throw new IllegalArgumentException(
                    tr("Failed to set reference. Reference version {0} not available in history.", reference.getVersion()));

        this.reference = reference;
        initTagTableModels();
        initNodeListTableModels();
        initMemberListTableModels();
        fireStateChanged();
    }

    /**
     * Sets the {@link HistoryOsmPrimitive} which plays the role of the current point
     * in time (see {@link PointInTimeType}).
     *
     * @param current the reference history primitive. Must not be {@code null}.
     * @throws IllegalArgumentException if reference is {@code null}
     * @throws IllegalStateException if this model isn't a assigned a history yet
     * @throws IllegalArgumentException if reference isn't an history primitive for the history managed by this mode
     *
     * @see #setHistory(History)
     * @see PointInTimeType
     */
    public void setCurrentPointInTime(HistoryOsmPrimitive current) {
        CheckParameterUtil.ensureParameterNotNull(current, "current");
        if (history == null)
            throw new IllegalStateException(tr("History not initialized yet. Failed to set current primitive."));
        if (current.getId() != history.getId())
            throw new IllegalArgumentException(
                    tr("Failed to set reference. Reference ID {0} does not match history ID {1}.", current.getId(), history.getId()));
        if (history.getByVersion(current.getVersion()) == null)
            throw new IllegalArgumentException(
                    tr("Failed to set current primitive. Current version {0} not available in history.", current.getVersion()));
        this.current = current;
        initTagTableModels();
        initNodeListTableModels();
        initMemberListTableModels();
        fireStateChanged();
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
     * @throws IllegalArgumentException if type is null
     */
    public HistoryOsmPrimitive getPointInTime(PointInTimeType type) {
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
     * representing the version currently edited in the current data layer.
     *
     * @param primitive the primitive to check
     * @return true if <code>primitive</code> is the latest primitive
     */
    public boolean isLatest(HistoryOsmPrimitive primitive) {
        return primitive != null && primitive == latest;
    }

    /**
     * Sets the reference point in time to the given row.
     * @param row row number
     */
    public void setReferencePointInTime(int row) {
        if (history == null)
            return;
        if (row == history.getNumVersions()) {
            if (latest != null) {
                setReferencePointInTime(latest);
            }
            return;
        }
        if (row < 0 || row > history.getNumVersions())
            return;
        setReferencePointInTime(history.get(row));
    }

    /**
     * Sets the current point in time to the given row.
     * @param row row number
     */
    public void setCurrentPointInTime(int row) {
        if (history == null)
            return;
        if (row == history.getNumVersions()) {
            if (latest != null) {
                setCurrentPointInTime(latest);
            }
            return;
        }
        if (row < 0 || row > history.getNumVersions())
            return;
        setCurrentPointInTime(history.get(row));
    }

    /**
     * Determines if the given row is the reference point in time.
     * @param row row number
     * @return {@code true} if the given row is the reference point in time
     */
    public boolean isReferencePointInTime(int row) {
        if (history == null)
            return false;
        if (row == history.getNumVersions())
            return latest == reference;
        if (row < 0 || row > history.getNumVersions())
            return false;
        return history.get(row) == reference;
    }

    /**
     * Determines if the given row is the current point in time.
     * @param row row number
     * @return {@code true} if the given row is the current point in time
     */
    public boolean isCurrentPointInTime(int row) {
        if (history == null)
            return false;
        if (row == history.getNumVersions())
            return latest == current;
        if (row < 0 || row > history.getNumVersions())
            return false;
        return history.get(row) == current;
    }

    /**
     * Returns the {@code HistoryPrimitive} at the given row.
     * @param row row number
     * @return the {@code HistoryPrimitive} at the given row
     */
    public HistoryOsmPrimitive getPrimitive(int row) {
        if (history == null)
            return null;
        return isLatest(row) ? latest : history.get(row);
    }

    /**
     * Determines if the given row is the latest.
     * @param row row number
     * @return {@code true} if the given row is the latest
     */
    public boolean isLatest(int row) {
        return row >= history.getNumVersions();
    }

    /**
     * Returns the latest {@code HistoryOsmPrimitive}.
     * @return the latest {@code HistoryOsmPrimitive}
     * @since 11646
     */
    public HistoryOsmPrimitive getLatest() {
        return latest;
    }

    /**
     * Returns the key set (union of current and reference point in type key sets).
     * @return the key set (union of current and reference point in type key sets)
     * @since 11647
     */
    public Set<String> getKeySet() {
        Set<String> keySet = new HashSet<>();
        if (current != null) {
            keySet.addAll(current.getTags().keySet());
        }
        if (reference != null) {
            keySet.addAll(reference.getTags().keySet());
        }
        return keySet;
    }

    /**
     * Sets the latest {@code HistoryOsmPrimitive}.
     * @param latest the latest {@code HistoryOsmPrimitive}
     */
    protected void setLatest(HistoryOsmPrimitive latest) {
        if (latest == null) {
            if (this.current == this.latest) {
                this.current = history != null ? history.getLatest() : null;
            }
            if (this.reference == this.latest) {
                this.reference = history != null ? history.getLatest() : null;
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
     * Removes this model as listener for data change and layer change events.
     *
     */
    public void unlinkAsListener() {
        OsmDataLayer editLayer = MainApplication.getLayerManager().getEditLayer();
        if (editLayer != null) {
            editLayer.data.removeDataSetListener(this);
        }
        MainApplication.getLayerManager().removeActiveLayerChangeListener(this);
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
        if (history == null)
            return;
        OsmPrimitive primitive = event.getDataset().getPrimitiveById(history.getId(), history.getType());
        HistoryOsmPrimitive newLatest;
        if (canShowAsLatest(primitive)) {
            newLatest = new HistoryPrimitiveBuilder().build(primitive);
        } else {
            newLatest = null;
        }
        setLatest(newLatest);
        fireModelChange();
    }

    @Override
    public void otherDatasetChange(AbstractDatasetChangedEvent event) {
        // Irrelevant
    }

    /* ---------------------------------------------------------------------- */
    /* ActiveLayerChangeListener                                              */
    /* ---------------------------------------------------------------------- */
    @Override
    public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
        Layer oldLayer = e.getPreviousActiveLayer();
        if (oldLayer instanceof OsmDataLayer) {
            OsmDataLayer l = (OsmDataLayer) oldLayer;
            l.data.removeDataSetListener(this);
        }
        Layer newLayer = e.getSource().getActiveLayer();
        if (!(newLayer instanceof OsmDataLayer)) {
            latest = null;
            fireModelChange();
            return;
        }
        OsmDataLayer l = (OsmDataLayer) newLayer;
        l.data.addDataSetListener(this);
        OsmPrimitive primitive = history != null ? l.data.getPrimitiveById(history.getId(), history.getType()) : null;
        HistoryOsmPrimitive newLatest;
        if (canShowAsLatest(primitive)) {
            newLatest = new HistoryPrimitiveBuilder().build(primitive);
        } else {
            newLatest = null;
        }
        setLatest(newLatest);
        fireModelChange();
    }

    /**
     * Creates a {@link HistoryOsmPrimitive} from a {@link OsmPrimitive}
     *
     */
    static class HistoryPrimitiveBuilder implements OsmPrimitiveVisitor {
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
            HistoryRelation hr = (HistoryRelation) clone;
            for (RelationMember rm : r.getMembers()) {
                hr.addMember(new RelationMemberData(rm.getRole(), rm.getType(), rm.getUniqueId()));
            }
        }

        @Override
        public void visit(Way w) {
            clone = new HistoryWay(w.getId(), w.getVersion(), w.isVisible(), getCurrentUser(), 0, null, false);
            clone.setTags(w.getKeys());
            for (Node n: w.getNodes()) {
                ((HistoryWay) clone).addNode(n.getUniqueId());
            }
        }

        private static User getCurrentUser() {
            UserInfo info = UserIdentityManager.getInstance().getUserInfo();
            return info == null ? User.getAnonymous() : User.createOsmUser(info.getId(), info.getDisplayName());
        }

        HistoryOsmPrimitive build(OsmPrimitive primitive) {
            primitive.accept(this);
            return clone;
        }
    }

}
