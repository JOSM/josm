// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import java.util.Collection;

public interface SourceProvider {

    public Collection<SourceEntry> getSources();
}
