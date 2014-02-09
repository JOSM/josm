// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.MapView.LayerChangeListener;
import org.openstreetmap.josm.gui.layer.Layer;

public abstract class FileExporter implements LayerChangeListener {

    public final ExtensionFileFilter filter;

    private boolean enabled;
    private boolean canceled;

    /**
     * Constructs a new {@code FileExporter}.
     * @param filter The extension file filter
     */
    public FileExporter(ExtensionFileFilter filter) {
        this.filter = filter;
        this.enabled = true;
    }

    public boolean acceptFile(File pathname, Layer layer) {
        return filter.acceptName(pathname.getName());
    }

    public void exportData(File file, Layer layer) throws IOException {
        throw new IOException(tr("Could not export ''{0}''.", file.getName()));
    }

    /**
     * Returns the enabled state of this {@code FileExporter}. When enabled, it is listed and usable in "File-&gt;Save" dialogs.
     * @return true if this {@code FileExporter} is enabled
     * @since 5459
     */
    public final boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the enabled state of the {@code FileExporter}. When enabled, it is listed and usable in "File-&gt;Save" dialogs.
     * @param enabled true to enable this {@code FileExporter}, false to disable it
     * @since 5459
     */
    public final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        // To be overriden by subclasses if their enabled state depends of the active layer nature
    }

    @Override
    public void layerAdded(Layer newLayer) {
        // To be overriden by subclasses if needed
    }

    @Override
    public void layerRemoved(Layer oldLayer) {
        // To be overriden by subclasses if needed
    }

    /**
     * Determines if this exporter has been canceled during export.
     * @return true if this {@code FileExporter} has been canceled
     * @since 6815
     */
    public final boolean isCanceled() {
        return canceled;
    }

    /**
     * Marks this exporter as canceled.
     * @param canceled true to mark this exporter as canceled, {@code false} otherwise
     * @since 6815
     */
    public final void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }
}
