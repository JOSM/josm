// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.conflict;

public interface IConflictListener {
    public void onConflictsAdded(ConflictCollection conflicts);
    public void onConflictsRemoved(ConflictCollection conflicts);
}
