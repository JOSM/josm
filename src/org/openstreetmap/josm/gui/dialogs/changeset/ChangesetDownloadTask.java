// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import java.util.Set;

import org.openstreetmap.josm.data.osm.Changeset;

public interface ChangesetDownloadTask extends Runnable{
    Set<Changeset> getDownloadedChangesets();
    boolean isCanceled();
    boolean isFailed();
}
