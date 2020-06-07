// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io.importexport;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.IOException;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeEvent;
import org.openstreetmap.josm.gui.layer.MainLayerManager.ActiveLayerChangeListener;

/**
 * Abstract base class for file exporters - IO classes that save layers to a file.
 */
public abstract class FileExporter implements ActiveLayerChangeListener {

    /** the  ExtensionFileFilter filter used by this exporter */
    public final ExtensionFileFilter filter;

    private boolean enabled;
    private boolean canceled;

    /**
     * Constructs a new {@code FileExporter}.
     * @param filter The extension file filter
     */
    protected FileExporter(ExtensionFileFilter filter) {
        this.filter = filter;
        this.enabled = true;
    }

    /**
     * Check if this exporter can export a certain layer to a certain file.
     *
     * Most exporters support just a single layer type.
     * @param pathname the target file name (check file extension using the {@link #filter}
     * @param layer the layer requested for export
     * @return true, if the exporter can handle the layer and filename is okay
     */
    public boolean acceptFile(File pathname, Layer layer) {
        return filter.acceptName(pathname.getName());
    }

    /**
     * Execute the data export. (To be overridden by subclasses.)
     *
     * @param file target file
     * @param layer the layer to export
     * @throws IOException in case of an IO error
     */
    public void exportData(File file, Layer layer) throws IOException {
        throw new IOException(tr("Could not export ''{0}''.", file.getName()));
    }

    /**
     * Execute the data export without prompting the user. (To be overridden by subclasses.)
     *
     * @param file target file
     * @param layer the layer to export
     * @throws IOException in case of an IO error
     * @since 15496
     */
    public void exportDataQuiet(File file, Layer layer) throws IOException {
        exportData(file, layer); //backwards compatibility
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
    public void activeOrEditLayerChanged(ActiveLayerChangeEvent e) {
        // To be overridden by subclasses if their enabled state depends of the active layer nature
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
