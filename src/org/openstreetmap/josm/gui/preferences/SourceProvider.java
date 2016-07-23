// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import java.util.Collection;

@FunctionalInterface
public interface SourceProvider {

    Collection<SourceEntry> getSources();
}
