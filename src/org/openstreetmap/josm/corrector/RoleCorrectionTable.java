// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.corrector;

import java.util.List;

public class RoleCorrectionTable extends
        CorrectionTable<RoleCorrectionTableModel> {

    public RoleCorrectionTable(List<RoleCorrection> roleCorrections) {
        super(new RoleCorrectionTableModel(roleCorrections));
    }

}
