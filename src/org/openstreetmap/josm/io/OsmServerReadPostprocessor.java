// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * Interface for plugins to process osm data after it has been downloaded or read
 * from file.
 * @see OsmReader#registerPostprocessor(OsmServerReadPostprocessor)
 */
@FunctionalInterface
public interface OsmServerReadPostprocessor {
    /**
     * Execute the post processor.
     * @param ds the dataset to read or modify
     * @param progress the progress monitor
     */
    void postprocessDataSet(DataSet ds, ProgressMonitor progress);
}
