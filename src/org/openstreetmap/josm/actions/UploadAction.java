// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.upload.ApiPreconditionCheckerHook;
import org.openstreetmap.josm.actions.upload.DiscardTagsHook;
import org.openstreetmap.josm.actions.upload.FixDataHook;
import org.openstreetmap.josm.actions.upload.RelationUploadOrderHook;
import org.openstreetmap.josm.actions.upload.UploadHook;
import org.openstreetmap.josm.actions.upload.ValidateUploadHook;
import org.openstreetmap.josm.data.APIDataSet;
import org.openstreetmap.josm.data.conflict.ConflictCollection;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.gui.HelpAwareOptionPane;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.io.UploadDialog;
import org.openstreetmap.josm.gui.io.UploadPrimitivesTask;
import org.openstreetmap.josm.gui.layer.AbstractModifiableLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * Action that opens a connection to the osm server and uploads all changes.
 *
 * An dialog is displayed asking the user to specify a rectangle to grab.
 * The url and account settings from the preferences are used.
 *
 * If the upload fails this action offers various options to resolve conflicts.
 *
 * @author imi
 */
public class UploadAction extends JosmAction {
    /**
     * The list of upload hooks. These hooks will be called one after the other
     * when the user wants to upload data. Plugins can insert their own hooks here
     * if they want to be able to veto an upload.
     *
     * Be default, the standard upload dialog is the only element in the list.
     * Plugins should normally insert their code before that, so that the upload
     * dialog is the last thing shown before upload really starts; on occasion
     * however, a plugin might also want to insert something after that.
     */
    private static final List<UploadHook> UPLOAD_HOOKS = new LinkedList<>();
    private static final List<UploadHook> LATE_UPLOAD_HOOKS = new LinkedList<>();

    static {
        /**
         * Calls validator before upload.
         */
        UPLOAD_HOOKS.add(new ValidateUploadHook());

        /**
         * Fixes database errors
         */
        UPLOAD_HOOKS.add(new FixDataHook());

        /**
         * Checks server capabilities before upload.
         */
        UPLOAD_HOOKS.add(new ApiPreconditionCheckerHook());

        /**
         * Adjusts the upload order of new relations
         */
        UPLOAD_HOOKS.add(new RelationUploadOrderHook());

        /**
         * Removes discardable tags like created_by on modified objects
         */
        LATE_UPLOAD_HOOKS.add(new DiscardTagsHook());
    }

    /**
     * Registers an upload hook. Adds the hook at the first position of the upload hooks.
     *
     * @param hook the upload hook. Ignored if null.
     */
    public static void registerUploadHook(UploadHook hook) {
        registerUploadHook(hook, false);
    }

    /**
     * Registers an upload hook. Adds the hook at the first position of the upload hooks.
     *
     * @param hook the upload hook. Ignored if null.
     * @param late true, if the hook should be executed after the upload dialog
     * has been confirmed. Late upload hooks should in general succeed and not
     * abort the upload.
     */
    public static void registerUploadHook(UploadHook hook, boolean late) {
        if (hook == null) return;
        if (late) {
            if (!LATE_UPLOAD_HOOKS.contains(hook)) {
                LATE_UPLOAD_HOOKS.add(0, hook);
            }
        } else {
            if (!UPLOAD_HOOKS.contains(hook)) {
                UPLOAD_HOOKS.add(0, hook);
            }
        }
    }

    /**
     * Unregisters an upload hook. Removes the hook from the list of upload hooks.
     *
     * @param hook the upload hook. Ignored if null.
     */
    public static void unregisterUploadHook(UploadHook hook) {
        if (hook == null) return;
        if (UPLOAD_HOOKS.contains(hook)) {
            UPLOAD_HOOKS.remove(hook);
        }
        if (LATE_UPLOAD_HOOKS.contains(hook)) {
            LATE_UPLOAD_HOOKS.remove(hook);
        }
    }

    /**
     * Constructs a new {@code UploadAction}.
     */
    public UploadAction() {
        super(tr("Upload data"), "upload", tr("Upload all changes in the active data layer to the OSM server"),
                Shortcut.registerShortcut("file:upload", tr("File: {0}", tr("Upload data")), KeyEvent.VK_UP, Shortcut.CTRL_SHIFT), true);
        putValue("help", ht("/Action/Upload"));
    }

    @Override
    protected void updateEnabledState() {
        OsmDataLayer editLayer = getLayerManager().getEditLayer();
        setEnabled(editLayer != null && editLayer.isUploadable());
    }

    /**
     * Check whether the preconditions are met to upload data from a given layer, if applicable.
     * @param layer layer to check
     * @return {@code true} if the preconditions are met, or not applicable
     * @see #checkPreUploadConditions(AbstractModifiableLayer, APIDataSet)
     */
    public static boolean checkPreUploadConditions(AbstractModifiableLayer layer) {
        return checkPreUploadConditions(layer,
                layer instanceof OsmDataLayer ? new APIDataSet(((OsmDataLayer) layer).data) : null);
    }

    protected static void alertUnresolvedConflicts(OsmDataLayer layer) {
        HelpAwareOptionPane.showOptionDialog(
                Main.parent,
                tr("<html>The data to be uploaded participates in unresolved conflicts of layer ''{0}''.<br>"
                        + "You have to resolve them first.</html>", Utils.escapeReservedCharactersHTML(layer.getName())
                ),
                tr("Warning"),
                JOptionPane.WARNING_MESSAGE,
                ht("/Action/Upload#PrimitivesParticipateInConflicts")
        );
    }

    /**
     * Warn user about discouraged upload, propose to cancel operation.
     * @param layer incriminated layer
     * @return true if the user wants to cancel, false if they want to continue
     */
    public static boolean warnUploadDiscouraged(AbstractModifiableLayer layer) {
        return GuiHelper.warnUser(tr("Upload discouraged"),
                "<html>" +
                tr("You are about to upload data from the layer ''{0}''.<br /><br />"+
                    "Sending data from this layer is <b>strongly discouraged</b>. If you continue,<br />"+
                    "it may require you subsequently have to revert your changes, or force other contributors to.<br /><br />"+
                    "Are you sure you want to continue?", Utils.escapeReservedCharactersHTML(layer.getName()))+
                "</html>",
                ImageProvider.get("upload"), tr("Ignore this hint and upload anyway"));
    }

    /**
     * Check whether the preconditions are met to upload data in <code>apiData</code>.
     * Makes sure upload is allowed, primitives in <code>apiData</code> don't participate in conflicts and
     * runs the installed {@link UploadHook}s.
     *
     * @param layer the source layer of the data to be uploaded
     * @param apiData the data to be uploaded
     * @return true, if the preconditions are met; false, otherwise
     */
    public static boolean checkPreUploadConditions(AbstractModifiableLayer layer, APIDataSet apiData) {
        if (layer.isUploadDiscouraged() && warnUploadDiscouraged(layer)) {
            return false;
        }
        if (layer instanceof OsmDataLayer) {
            OsmDataLayer osmLayer = (OsmDataLayer) layer;
            ConflictCollection conflicts = osmLayer.getConflicts();
            if (apiData.participatesInConflict(conflicts)) {
                alertUnresolvedConflicts(osmLayer);
                return false;
            }
        }
        // Call all upload hooks in sequence.
        // FIXME: this should become an asynchronous task
        //
        if (apiData != null) {
            for (UploadHook hook : UPLOAD_HOOKS) {
                if (!hook.checkUpload(apiData))
                    return false;
            }
        }

        return true;
    }

    /**
     * Uploads data to the OSM API.
     *
     * @param layer the source layer for the data to upload
     * @param apiData the primitives to be added, updated, or deleted
     */
    public void uploadData(final OsmDataLayer layer, APIDataSet apiData) {
        if (apiData.isEmpty()) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("No changes to upload."),
                    tr("Warning"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        if (!checkPreUploadConditions(layer, apiData))
            return;

        final UploadDialog dialog = UploadDialog.getUploadDialog();
        dialog.setChangesetTags(layer.data);
        dialog.setUploadedPrimitives(apiData);
        dialog.setVisible(true);
        dialog.rememberUserInput();
        if (dialog.isCanceled())
            return;

        for (UploadHook hook : LATE_UPLOAD_HOOKS) {
            if (!hook.checkUpload(apiData))
                return;
        }

        // Any hooks want to change the changeset tags?
        Changeset cs = UploadDialog.getUploadDialog().getChangeset();
        Map<String, String> changesetTags = cs.getKeys();
        for (UploadHook hook : UPLOAD_HOOKS) {
            hook.modifyChangesetTags(changesetTags);
        }
        for (UploadHook hook : LATE_UPLOAD_HOOKS) {
            hook.modifyChangesetTags(changesetTags);
        }

        MainApplication.worker.execute(
                new UploadPrimitivesTask(
                        UploadDialog.getUploadDialog().getUploadStrategySpecification(),
                        layer,
                        apiData,
                        cs
                )
        );
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        if (MainApplication.getMap() == null) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Nothing to upload. Get some data first."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        APIDataSet apiData = new APIDataSet(getLayerManager().getEditDataSet());
        uploadData(getLayerManager().getEditLayer(), apiData);
    }
}
