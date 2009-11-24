// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.gui.help.HelpUtil.ht;

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
import org.openstreetmap.josm.tools.Shortcut;

public class MergeSelectionAction extends AbstractMergeAction {
    public MergeSelectionAction() {
        super(tr("Merge selection"), "dialogs/mergedown", tr("Merge the currently selected primitives into another layer"), Shortcut
                .registerShortcut("system:mergeselection", tr("Edit: {0}", tr("Merge selection")), KeyEvent.VK_M, Shortcut.GROUP_MENU, Shortcut.SHIFT_DEFAULT),
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
        MergeSourceBuildingVisitor builder = new MergeSourceBuildingVisitor(getEditLayer().data);
        ((OsmDataLayer)targetLayer).mergeFrom(builder.build());
    }

    public void actionPerformed(ActionEvent e) {
        if (getEditLayer() == null || getEditLayer().data.getSelected().isEmpty())
            return;
        mergeSelected(getEditLayer().data);
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
        } else {
            updateEnabledState(getCurrentDataSet().getSelected());
        }
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }
}
