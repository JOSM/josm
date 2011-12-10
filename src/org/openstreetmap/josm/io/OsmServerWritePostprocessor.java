// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import java.util.Collection;

import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

public interface OsmServerWritePostprocessor {

    public void postprocessUploadedPrimitives(Collection<IPrimitive> p, ProgressMonitor progress);

}
