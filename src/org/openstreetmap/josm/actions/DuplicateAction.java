// License: GPL. Copyright 2007 by Immanuel Scholz and others
// Author: David Earl
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.Layer.LayerChangeListener;
import org.openstreetmap.josm.tools.Shortcut;

public final class DuplicateAction extends JosmAction implements SelectionChangedListener, LayerChangeListener {

    public DuplicateAction() {
        super(tr("Duplicate"), "duplicate",
                tr("Duplicate selection by copy and immediate paste."),
                Shortcut.registerShortcut("system:duplicate", tr("Edit: {0}", tr("Duplicate")), KeyEvent.VK_D, Shortcut.GROUP_MENU), true);
        setEnabled(false);
        DataSet.selListeners.add(this);
        Layer.listeners.add(this);
    }

    public void actionPerformed(ActionEvent e) {
        new PasteAction().pasteData(new CopyAction().copyData(), getEditLayer(), e);
    }


    protected void refreshEnabled() {
        setEnabled(getCurrentDataSet() != null
                && ! getCurrentDataSet().getSelected().isEmpty()
        );
    }

    /* ---------------------------------------------------------------------------------- */
    /* Interface SelectionChangeListener                                                  */
    /* ---------------------------------------------------------------------------------- */
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        refreshEnabled();
    }

    /* ---------------------------------------------------------------------------------- */
    /* Interface LayerChangeListener                                                      */
    /* ---------------------------------------------------------------------------------- */
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        refreshEnabled();
    }

    public void layerAdded(Layer newLayer) {
        refreshEnabled();
    }

    public void layerRemoved(Layer oldLayer) {
        refreshEnabled();
    }
}
