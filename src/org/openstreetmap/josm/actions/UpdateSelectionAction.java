// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.CheckParameterUtil.ensureParameterNotNull;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.DataSet.DownloadPolicy;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.UpdatePrimitivesTask;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.io.MultiFetchServerObjectReader;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * This action synchronizes a set of primitives with their state on the server.
 * @since 1670
 */
public class UpdateSelectionAction extends JosmAction {

    /**
     * handle an exception thrown because a primitive was deleted on the server
     *
     * @param id the primitive id
     * @param type The primitive type. Must be one of {@link OsmPrimitiveType#NODE NODE},
     * {@link OsmPrimitiveType#WAY WAY}, {@link OsmPrimitiveType#RELATION RELATION}
     */
    public static void handlePrimitiveGoneException(long id, OsmPrimitiveType type) {
        MultiFetchServerObjectReader reader = MultiFetchServerObjectReader.create();
        reader.append(MainApplication.getLayerManager().getEditDataSet(), id, type);
        try {
            DataSet ds = reader.parseOsm(NullProgressMonitor.INSTANCE);
            MainApplication.getLayerManager().getEditLayer().mergeFrom(ds);
        } catch (OsmTransferException e) {
            ExceptionDialogUtil.explainException(e);
        }
    }

    /**
     * Updates the data for for the {@link OsmPrimitive}s in <code>selection</code>
     * with the data currently kept on the server.
     *
     * @param selection a collection of {@link OsmPrimitive}s to update
     *
     */
    public static void updatePrimitives(final Collection<OsmPrimitive> selection) {
        MainApplication.worker.submit(new UpdatePrimitivesTask(MainApplication.getLayerManager().getEditLayer(), selection));
    }

    /**
     * Updates the data for  the {@link OsmPrimitive}s with id <code>id</code>
     * with the data currently kept on the server.
     *
     * @param id  the id of a primitive in the {@link DataSet} of the current edit layer. Must not be null.
     * @throws IllegalArgumentException if id is null
     * @throws IllegalStateException if there is no primitive with <code>id</code> in the current dataset
     * @throws IllegalStateException if there is no current dataset
     */
    public static void updatePrimitive(PrimitiveId id) {
        ensureParameterNotNull(id, "id");
        updatePrimitives(Collections.<OsmPrimitive>singleton(Optional.ofNullable(Optional.ofNullable(
                MainApplication.getLayerManager().getEditLayer()).orElseThrow(
                        () -> new IllegalStateException(tr("No current dataset found")))
                .data.getPrimitiveById(id)).orElseThrow(
                        () -> new IllegalStateException(tr("Did not find an object with id {0} in the current dataset", id)))));
    }

    /**
     * Constructs a new {@code UpdateSelectionAction}.
     */
    public UpdateSelectionAction() {
        super(tr("Update selection"), "updatedata",
                tr("Updates the currently selected objects from the server (re-downloads data)"),
                Shortcut.registerShortcut("file:updateselection",
                        tr("File: {0}", tr("Update selection")), KeyEvent.VK_U,
                        Shortcut.ALT_CTRL),
                true, "updateselection", true);
        putValue("help", ht("/Action/UpdateSelection"));
    }

    /**
     * Constructs a new {@code UpdateSelectionAction}.
     *
     * @param name the action's text as displayed on the menu (if it is added to a menu)
     * @param iconName the filename of the icon to use
     * @param tooltip  a longer description of the action that will be displayed in the tooltip. Please note
     *           that html is not supported for menu actions on some platforms.
     * @param shortcut a ready-created shortcut object or null if you don't want a shortcut. But you always
     *            do want a shortcut, remember you can always register it with group=none, so you
     *            won't be assigned a shortcut unless the user configures one. If you pass null here,
     *            the user CANNOT configure a shortcut for your action.
     * @param register register this action for the toolbar preferences?
     * @param toolbarId identifier for the toolbar preferences. The iconName is used, if this parameter is null
     */
    public UpdateSelectionAction(String name, String iconName, String tooltip, Shortcut shortcut, boolean register, String toolbarId) {
        super(name, iconName, tooltip, shortcut, register, toolbarId, true);
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        if (selection == null || selection.isEmpty()) {
            setEnabled(false);
        } else {
            DataSet ds = selection.iterator().next().getDataSet();
            setEnabled(!ds.isLocked() && !DownloadPolicy.BLOCKED.equals(ds.getDownloadPolicy())
                    && !Main.isOffline(OnlineResource.OSM_API));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        Collection<OsmPrimitive> toUpdate = getData();
        if (toUpdate.isEmpty()) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("There are no selected objects to update."),
                    tr("Selection empty"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        updatePrimitives(toUpdate);
    }

    /**
     * Returns the data on which this action operates. Override if needed.
     * @return the data on which this action operates
     */
    public Collection<OsmPrimitive> getData() {
        return getLayerManager().getEditDataSet().getAllSelected();
    }
}
