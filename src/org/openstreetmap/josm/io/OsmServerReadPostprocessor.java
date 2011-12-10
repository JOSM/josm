// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

public interface OsmServerReadPostprocessor {

    public void postprocessDataSet(DataSet ds, ProgressMonitor progress);

}
