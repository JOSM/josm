// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.MergeSourceBuildingVisitor;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Shortcut;

public class MergeSelectionAction extends AbstractMergeAction {
    public MergeSelectionAction() {
        super(tr("Merge selection"), "dialogs/mergedown", tr("Merge the currently selected objects into another layer"),
            Shortcut.registerShortcut("system:mergeselection", tr("Edit: {0}", tr("Merge selection")),
            KeyEvent.VK_M, Shortcut.CTRL_SHIFT),
            true /* register */
        );
        putValue("help", ht("/Action/MergeSelection"));
    }

    public void mergeSelected(DataSet source) {
        List<Layer> targetLayers = LayerListDialog.getInstance().getModel().getPossibleMergeTargets(getEditLayer());
        if (targetLayers.isEmpty()) {
            warnNoTargetLayersForSourceLayer(getEditLayer());
            return;
        }
        Layer targetLayer = askTargetLayer(targetLayers);
        if (targetLayer == null)
            return;
        if (getEditLayer().isUploadDiscouraged() && targetLayer instanceof OsmDataLayer && !((OsmDataLayer)targetLayer).isUploadDiscouraged()
                && getEditLayer().data.getAllSelected().size() > 1) {
            if (warnMergingUploadDiscouragedObjects(targetLayer)) {
                return;
            }
        }
        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(getEditLayer().data);
        ((OsmDataLayer)targetLayer).mergeFrom(builder.build());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (getEditLayer() == null || getEditLayer().data.getAllSelected().isEmpty())
            return;
        mergeSelected(getEditLayer().data);
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
        } else {
            updateEnabledState(getCurrentDataSet().getAllSelected());
        }
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }

    /**
     * returns true if the user wants to cancel, false if they want to continue
     */
    public static final boolean warnMergingUploadDiscouragedObjects(Layer targetLayer) {
        return GuiHelper.warnUser(tr("Merging too many objects with different upload policies"),
                "<html>" +
                tr("You are about to merge more than 1 object between layers ''{0}'' and ''{1}''.<br /><br />"+
                        "<b>This is not the recommended way of merging such data</b>.<br />"+
                        "You should instead check and merge each object, <b>one by one</b>.<br /><br />"+
                        "Are you sure you want to continue?", getEditLayer().getName(), targetLayer.getName(), targetLayer.getName())+
                "</html>",
                ImageProvider.get("dialogs", "mergedown"), tr("Ignore this hint and merge anyway"));
    }
}
