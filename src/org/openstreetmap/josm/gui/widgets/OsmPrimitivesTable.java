// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumnModel;

import org.openstreetmap.josm.actions.ZoomToAction;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;

public abstract class OsmPrimitivesTable extends JTable {
    
    /**
     * the data layer in whose context primitives are edited in this table
     */
    private OsmDataLayer layer;

    /** the popup menu */
    private JPopupMenu popupMenu;
    private ZoomToAction zoomToAction;

    public final OsmDataLayer getLayer() {
        return layer;
    }

    public final void setLayer(OsmDataLayer layer) {
        this.layer = layer;
    }

    public OsmPrimitivesTable(OsmPrimitivesTableModel dm, TableColumnModel cm, ListSelectionModel sm) {
        super(dm, cm, sm);
        addMouseListener(new PopupMenuLauncher(getPopUpMenu()));
        addMouseListener(new DblClickHandler());
    }
    
    public OsmPrimitivesTableModel getOsmPrimitivesTableModel() {
        return (OsmPrimitivesTableModel) getModel();
    }

    /**
     * Replies the popup menu for this table
     *
     * @return the popup menu
     */
    protected final JPopupMenu getPopUpMenu() {
        if (popupMenu == null) {
            popupMenu = buildPopupMenu();
        }
        return popupMenu;
    }
    
    protected abstract ZoomToAction buildZoomToAction();

    protected JPopupMenu buildPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        zoomToAction = buildZoomToAction();
        MapView.addLayerChangeListener(zoomToAction);
        getSelectionModel().addListSelectionListener(zoomToAction);
        menu.add(zoomToAction);
        return menu;
    }
    
    public void unlinkAsListener() {
        MapView.removeLayerChangeListener(zoomToAction);
    }
        
    public OsmPrimitive getPrimitiveInLayer(int row, OsmDataLayer layer) {
        return getOsmPrimitivesTableModel().getReferredPrimitive(row);
    }

    protected class DblClickHandler extends MouseAdapter {

        protected void setSelection(MouseEvent e) {
            int row = rowAtPoint(e.getPoint());
            if (row < 0) return;
            OsmPrimitive primitive = getPrimitiveInLayer(row, layer);
            if (layer != null && primitive != null) {
                layer.data.setSelected(primitive.getPrimitiveId());
            }
        }

        protected void addSelection(MouseEvent e) {
            int row = rowAtPoint(e.getPoint());
            if (row < 0) return;
            OsmPrimitive primitive = getPrimitiveInLayer(row, layer);
            getSelectionModel().addSelectionInterval(row, row);
            if (layer != null && primitive != null) {
                layer.data.addSelected(primitive.getPrimitiveId());
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() > 1) {
                if (e.isControlDown()) {
                    addSelection(e);
                } else {
                    setSelection(e);
                }
            }
        }
    }
}
