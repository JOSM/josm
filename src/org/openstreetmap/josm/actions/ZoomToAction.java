// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.conflict.pair.nodes.NodeListTable;
import org.openstreetmap.josm.gui.conflict.pair.relation.RelationMemberTable;
import org.openstreetmap.josm.gui.dialogs.relation.MemberTable;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.widgets.OsmPrimitivesTable;
import org.openstreetmap.josm.tools.CheckParameterUtil;

public class ZoomToAction extends AbstractAction implements LayerChangeListener, ListSelectionListener {

    private final OsmPrimitivesTable table;
    
    private final String descriptionNominal;
    private final String descriptionInactiveLayer;
    private final String descriptionNoSelection;
    
    public ZoomToAction(OsmPrimitivesTable table, String descriptionNominal, String descriptionInactiveLayer, String descriptionNoSelection) {
        CheckParameterUtil.ensureParameterNotNull(table);
        this.table = table;
        this.descriptionNominal = descriptionNominal;
        this.descriptionInactiveLayer = descriptionInactiveLayer;
        this.descriptionNoSelection = descriptionNoSelection;
        putValue(NAME, tr("Zoom to"));
        putValue(SHORT_DESCRIPTION, descriptionNominal);
        updateEnabledState();
    }
    
    public ZoomToAction(MemberTable table) {
        this(table, 
                tr("Zoom to the object the first selected member refers to"),
                tr("Zooming disabled because layer of this relation is not active"),
                tr("Zooming disabled because there is no selected member"));
    }
    
    public ZoomToAction(RelationMemberTable table) {
        this(table, 
                tr("Zoom to the object the first selected member refers to"),
                tr("Zooming disabled because layer of this relation is not active"),
                tr("Zooming disabled because there is no selected member"));
    }
    
    public ZoomToAction(NodeListTable table) {
        this(table, 
                tr("Zoom to the first selected node"),
                tr("Zooming disabled because layer of this way is not active"),
                tr("Zooming disabled because there is no selected node"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (! isEnabled())
            return;
        int[] rows = this.table.getSelectedRows();
        if (rows == null || rows.length == 0)
            return;
        int row = rows[0];
        OsmDataLayer layer = this.table.getLayer();
        OsmPrimitive primitive = this.table.getPrimitiveInLayer(row, layer);
        if (layer != null && primitive != null) {
            layer.data.setSelected(primitive);
            AutoScaleAction.autoScale("selection");
        }
    }

    protected void updateEnabledState() {
        if (Main.main == null || Main.main.getEditLayer() != this.table.getLayer()) {
            setEnabled(false);
            putValue(SHORT_DESCRIPTION, descriptionInactiveLayer);
            return;
        }
        if (this.table.getSelectedRowCount() == 0) {
            setEnabled(false);
            putValue(SHORT_DESCRIPTION, descriptionNoSelection);
            return;
        }
        setEnabled(true);
        putValue(SHORT_DESCRIPTION, descriptionNominal);
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        updateEnabledState();
    }

    @Override
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        updateEnabledState();
    }

    @Override
    public void layerAdded(Layer newLayer) {
        updateEnabledState();
    }

    @Override
    public void layerRemoved(Layer oldLayer) {
        updateEnabledState();
    }
}