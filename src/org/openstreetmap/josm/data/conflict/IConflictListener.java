// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.conflict;

public interface IConflictListener {
    void onConflictsAdded(ConflictCollection conflicts);

    void onConflictsRemoved(ConflictCollection conflicts);
}
