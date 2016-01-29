// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.session;

import java.util.Collection;
import java.util.Collections;

import javax.swing.JCheckBox;

import org.openstreetmap.josm.gui.layer.Layer;

/**
 * Abstract superclass of all session layer exporters.
 * @param <T> Type of exported layer
 * @since 9455
 */
public abstract class AbstractSessionExporter<T extends Layer> implements SessionLayerExporter {

    protected final T layer;
    protected final JCheckBox export = new JCheckBox();

    /**
     * Constructs a new {@code AbstractSessionExporter}.
     * @param layer layer to export
     */
    protected AbstractSessionExporter(T layer) {
        this.layer = layer;
    }

    @Override
    public Collection<Layer> getDependencies() {
        return Collections.emptySet();
    }

    @Override
    public boolean shallExport() {
        return export.isSelected();
    }

    @Override
    public boolean requiresZip() {
        return false;
    }
}
