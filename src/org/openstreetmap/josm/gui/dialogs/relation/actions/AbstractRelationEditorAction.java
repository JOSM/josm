// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation.actions;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DownloadPolicy;
import org.openstreetmap.josm.gui.dialogs.relation.IRelationEditor;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTableModel;
import org.openstreetmap.josm.gui.dialogs.relation.SelectionTable;
import org.openstreetmap.josm.gui.dialogs.relation.SelectionTableModel;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.tagging.TagEditorModel;
import org.openstreetmap.josm.io.NetworkManager;
import org.openstreetmap.josm.io.OnlineResource;

/**
 * Abstract superclass of relation editor actions.
 *
 * @since 9496
 */
public abstract class AbstractRelationEditorAction extends AbstractAction
        implements TableModelListener, ListSelectionListener, PropertyChangeListener {
    private static final long serialVersionUID = 1L;
    protected final transient IRelationEditorActionAccess editorAccess;

    /**
     * Create a new relation editor action
     *
     * @param editorAccess
     *            The editor this action is for
     * @param updateOn
     *            The events that may cause the enabled state of this button to
     *            change.
     */
    protected AbstractRelationEditorAction(IRelationEditorActionAccess editorAccess,
            IRelationEditorUpdateOn... updateOn) {
        Objects.requireNonNull(editorAccess, "editorAccess");
        Objects.requireNonNull(updateOn, "updateOn");
        this.editorAccess = editorAccess;
        for (IRelationEditorUpdateOn u : updateOn) {
            u.register(editorAccess, this);
        }
    }

    /**
     * Create a new relation editor action
     *
     * @param editorAccess
     *            The editor this action is for
     * @param actionMapKey
     *            The key for the member table action map.
     * @param updateOn
     *            The events that may cause the enabled state of this button to
     *            change.
     */
    protected AbstractRelationEditorAction(IRelationEditorActionAccess editorAccess, String actionMapKey,
            IRelationEditorUpdateOn... updateOn) {
        this(editorAccess, updateOn);
        Objects.requireNonNull(actionMapKey, "actionMapKey");

        this.editorAccess.addMemberTableAction(actionMapKey, this);
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        updateEnabledState();
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        updateEnabledState();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        updateEnabledState();
    }

    protected abstract void updateEnabledState();

    protected final boolean canDownload() {
        DataSet ds = editorAccess.getEditor().getLayer().getDataSet();
        return !NetworkManager.isOffline(OnlineResource.OSM_API) && ds != null && !ds.isLocked()
                && DownloadPolicy.BLOCKED != ds.getDownloadPolicy();
    }

    protected MemberTable getMemberTable() {
        return editorAccess.getMemberTable();
    }

    protected MemberTableModel getMemberTableModel() {
        return editorAccess.getMemberTableModel();
    }

    protected SelectionTable getSelectionTable() {
        return editorAccess.getSelectionTable();
    }

    protected SelectionTableModel getSelectionTableModel() {
        return editorAccess.getSelectionTableModel();
    }

    protected IRelationEditor getEditor() {
        return editorAccess.getEditor();
    }

    protected TagEditorModel getTagModel() {
        return editorAccess.getTagModel();
    }

    protected OsmDataLayer getLayer() {
        return editorAccess.getEditor().getLayer();
    }

    /**
     * Indicates that this action only visible in expert mode
     * @return <code>true</code> for expert mode actions.
     * @since 14027
     */
    public boolean isExpertOnly() {
        return false;
    }
}
