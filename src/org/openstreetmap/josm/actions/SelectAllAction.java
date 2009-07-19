// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.Layer.LayerChangeListener;
import org.openstreetmap.josm.tools.Shortcut;

public class SelectAllAction extends JosmAction implements LayerChangeListener{

    public SelectAllAction() {
        super(tr("Select All"),"selectall", tr("Select all undeleted objects in the data layer. This selects incomplete objects too."),
                Shortcut.registerShortcut("system:selectall", tr("Edit: {0}", tr("Select All")), KeyEvent.VK_A, Shortcut.GROUP_MENU), true);
        Layer.listeners.add(this);
        refreshEnabled();
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        getCurrentDataSet().setSelected(getCurrentDataSet().allNonDeletedCompletePrimitives());
    }

    /**
     * Refreshes the enabled state
     * 
     */
    protected void refreshEnabled() {
        setEnabled(getEditLayer() != null);
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
