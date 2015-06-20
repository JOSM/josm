// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.util.Collection;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

public interface OsmServerWritePostprocessor {

    void postprocessUploadedPrimitives(Collection<OsmPrimitive> p, ProgressMonitor progress);
}
