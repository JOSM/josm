// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.MergeSourceBuildingVisitor;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * Merge the currently selected objects into another layer.
 * @since 1890
 */
public class MergeSelectionAction extends AbstractMergeAction {

    /**
     * Constructs a new {@code MergeSelectionAction}.
     */
    public MergeSelectionAction() {
        super(tr("Merge selection"), "dialogs/mergedown", tr("Merge the currently selected objects into another layer"),
            Shortcut.registerShortcut("system:mergeselection", tr("Edit: {0}", tr("Merge selection")),
            KeyEvent.VK_M, Shortcut.CTRL_SHIFT),
            true /* register */
        );
        setHelpId(ht("/Action/MergeSelection"));
    }

    /**
     * Merge the currently selected objects into another layer.
     */
    public void mergeSelected() {
        OsmDataLayer editLayer = getLayerManager().getEditLayer();
        List<Layer> targetLayers = LayerListDialog.getInstance().getModel().getPossibleMergeTargets(editLayer);
        if (targetLayers.isEmpty()) {
            warnNoTargetLayersForSourceLayer(editLayer);
            return;
        }
        Layer targetLayer = askTargetLayer(targetLayers, false, null, false, tr("Merge selection")).selectedTargetLayer;
        if (targetLayer == null)
            return;
        if (editLayer.isUploadDiscouraged() && targetLayer instanceof OsmDataLayer
                && !((OsmDataLayer) targetLayer).isUploadDiscouraged()
                && editLayer.data.getAllSelected().size() > 1
                && warnMergingUploadDiscouragedObjects(targetLayer)) {
            return;
        }
        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(editLayer.getDataSet());
        ((OsmDataLayer) targetLayer).mergeFrom(builder.build());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        OsmDataLayer editLayer = getLayerManager().getEditLayer();
        if (editLayer == null || editLayer.data.selectionEmpty())
            return;
        mergeSelected();
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        updateEnabledStateOnModifiableSelection(selection);
    }

    /**
     * Warns the user about merging too many objects with different upload policies.
     * @param targetLayer Target layer
     * @return true if the user wants to cancel, false if they want to continue
     */
    public final boolean warnMergingUploadDiscouragedObjects(Layer targetLayer) {
        return GuiHelper.warnUser(tr("Merging too many objects with different upload policies"),
                "<html>" +
                tr("You are about to merge more than 1 object between layers ''{0}'' and ''{1}''.<br /><br />"+
                        "<b>This is not the recommended way of merging such data</b>.<br />"+
                        "You should instead check and merge each object, <b>one by one</b>.<br /><br />"+
                        "Are you sure you want to continue?",
                        Utils.escapeReservedCharactersHTML(getLayerManager().getEditLayer().getName()),
                        Utils.escapeReservedCharactersHTML(targetLayer.getName()),
                        Utils.escapeReservedCharactersHTML(targetLayer.getName()))+
                "</html>",
                ImageProvider.get("dialogs", "mergedown"), tr("Ignore this hint and merge anyway"));
    }
}
