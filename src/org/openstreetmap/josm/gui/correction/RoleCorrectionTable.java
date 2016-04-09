// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.correction;

import java.util.List;

import org.openstreetmap.josm.data.correction.RoleCorrection;

/**
 * Role correction table.
 * @since 1001
 */
public class RoleCorrectionTable extends CorrectionTable<RoleCorrectionTableModel> {

    /**
     * Constructs a new {@code RoleCorrectionTable}.
     * @param roleCorrections role corrections
     */
    public RoleCorrectionTable(List<RoleCorrection> roleCorrections) {
        super(new RoleCorrectionTableModel(roleCorrections));
    }
}
